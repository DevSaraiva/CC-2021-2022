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

        return this.ips.size() == this.syncronized.size(); //remove -2
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

    public static void main (String[] args){

            //parse arguments

            String folder = args[0];
            List<String> ips = new ArrayList<>();

            for(int i = 1; i < args.length; i++){
                ips.add(args[i]);
            }

            FFSync ffSync = new FFSync(folder,ips);


        try {

            int port = 8888;

            String ip = ffSync.ips.get(0);


            DatagramSocket requestSocket = new DatagramSocket(port);


            FTR ftr = new FTR(requestSocket,ffSync.folderPath,ffSync.allFiles,ffSync.syncronized,port);

            RequestHandler rq = new RequestHandler();

            Thread t = new Thread(ftr);

            t.start();

            rq.sendSyn(InetAddress.getByName(ip) ,port,ffSync.seq,ffSync.getFiles());

            ffSync.seq++;


            //waits for synchronization

            while(!ffSync.isSync()){
               Thread.sleep(10); //Try to remove the sleep with conditions
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
