import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;


public class FFSync {


    private int port = 8888;
    private String folderPath;
    private List<String> ips;
    private List<FileIP> allFiles;
    private int seq = 0;
    private List<Boolean> syncronized;


    public FFSync(String folderPath, List<String> ips){

        String currentPath = null;
        this.allFiles = new ArrayList<>();
        try {
            currentPath = new File(".").getCanonicalPath();
            this.folderPath = currentPath + "/" + folderPath;
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.seq = 0;

        this.ips = ips;

        this.syncronized = new ArrayList<>();

    }


    public File[] getFiles(){

        File folder = new File(this.folderPath);
        File[] files = folder.listFiles();

        return files;
    }


    public boolean isSync(){

        return this.ips.size()-3 == this.syncronized.size(); //remove -3
    }


    public List<FileIP> neededFilesCalculator(){
        List<File> files= Arrays.asList(getFiles());
        List<String> filesNames = files.stream().map(f -> f.getName()).collect(Collectors.toList());
        List<FileIP> needed = new ArrayList<>();
        List<String> neededAux = new ArrayList<>();

        for(FileIP fi : this.allFiles){
            if(!filesNames.contains(fi.getFile().getName()) && !neededAux.contains(fi.getFile().getName())){
                needed.add(fi);
                neededAux.add(fi.getFile().getName());
            }
        }

        return needed;
    }


    //passar a lista de ficheiros em formato de uma lista de strings para ser disposto no servidor HTTP

    public String[] fileArrayToStringArray(File[] files) {
        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            names[i] = "<b>" + files[i].getName() + "</b><br>";
        }
        return names;
    }


    public static void main (String[] args){

        //parse arguments
        String folder = args[0];
        List<String> ips = new ArrayList<>();

        for(int i = 1; i < args.length; i++){
            ips.add(args[i]);
        }
        FFSync ffSync = new FFSync(folder,ips);


        //HTTP
        try {
            System.out.println("Starting HTTP server connection on localhost:" + args[4]);

            String[] formattedLogs = ffSync.fileArrayToStringArray(ffSync.getFiles());      //tentar por o getFiles no HttpServer.java para dar load sempre que se faz um GET (por causa do watchfolder)
            HttpServer httpServer = new HttpServer(formattedLogs, Integer.parseInt(args[4]));
            Thread t1 = new Thread(httpServer);
            t1.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {

            int port = Integer.parseInt(args[3]);
            int requestPort = Integer.parseInt(args[2]);

            DatagramSocket requestSocket = new DatagramSocket(requestPort);

            FTR ftr = new FTR(requestSocket,ffSync.folderPath,ffSync.allFiles,ffSync.syncronized,port);

            RequestHandler rq = new RequestHandler();

            Thread t = new Thread(ftr);
            t.start();



            //Synchronize with all peers
            for(int i = 0; i < ffSync.ips.size()-3; i++){ //remove -3
                rq.sendSyn(InetAddress.getByName(ffSync.ips.get(i)) ,port,ffSync.seq,ffSync.getFiles());
                ffSync.seq++;
            }


            //waits for synchronization
            while(!ffSync.isSync()){
               Thread.sleep(100);
            }
            List<FileIP> neededFiles = ffSync.neededFilesCalculator();

            for(FileIP fi : neededFiles){
                rq.sendRead(fi.getIp(),port,ffSync.seq,fi.getFile().getPath());
                ffSync.seq++;
                System.out.println("Reading file:" + fi.getFile().getName());
            }




        }catch (Exception e){
            e.printStackTrace();
        }




        }
}
