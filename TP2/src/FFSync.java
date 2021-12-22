import java.awt.event.InputEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
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
    private List<Boolean> receivedFiles;
    private List<Boolean> watching;

    public FFSync(String folderPath, List<String> ips) {

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

        this.receivedFiles = new ArrayList<>();

        this.watching = new ArrayList<>();

    }

    public File[] getFiles(String folderPath) {

        File folder = new File(folderPath);
        List<File> files = new ArrayList<>();

        for (File f : folder.listFiles()) {
            if (f.isDirectory()) {
                List<File> newFiles = Arrays.asList(getFiles(f.getPath()));
                files.addAll(newFiles);
            } else {
                files.add(f);
            }
        }

        File[] res = new File[files.size()];
        files.toArray(res);

        return res;
    }

    public boolean isSync() {

        return this.ips.size() - 3 == this.syncronized.size(); // remove -3
    }

    // calculate the files missing

    public List<FileIP> neededFilesCalculator(String folderPath) {

        List<File> myFiles = Arrays.asList(getFiles(folderPath));
        List<FileIP> needed = new ArrayList<>();

        for (FileIP fi : this.allFiles) {

            List<String> aux = needed.stream().map(f -> f.getFile().getName()).collect(Collectors.toList());

            if (!aux.contains(fi.getFile().getName())) {

                // get where is the newer file

                FileIP newer = fi;

                for (FileIP file : this.allFiles) {

                    if (file.getFile().getName().equals(fi.getFile().getName())) {

                        if (file.getFile().lastModified() < fi.getFile().lastModified()) {
                            newer = file;
                        }
                    }
                }

                needed.add(newer);
            }

        }

        return needed;
    }

    // passar a lista de ficheiros em formato de uma lista de strings para ser
    // disposto no servidor HTTP

    public String[] fileArrayToStringArray(File[] files) {
        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            names[i] = "<b>" + files[i].getName() + "</b><br>";
        }
        return names;
    }

    public void createLogFile() {
        try {
            File logsFile = new File("logs.txt");
            if (logsFile.exists()) {
                System.out.println("logs.txt already exists...");
                System.out.println("Do you want to delete the existing one and create a new logs.txt? [y/n]");
                Scanner sc = new Scanner(System.in);
                String input = sc.nextLine();

                if (input.equals("y")) {
                    logsFile.delete();
                    logsFile.createNewFile();
                } else {
                    System.out.println("logs.txt not created. Using the existing one (may cause problems)");
                }

            } else {
                logsFile.createNewFile();
                System.out.println("logs.txt created successfully!!!");
            }
        } catch (IOException e) {
            System.out.println("An error ocurred while creating LogFile :(");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        // parse arguments
        String folder = args[0];
        List<String> ips = new ArrayList<>();

        for (int i = 1; i < args.length; i++) {
            ips.add(args[i]);
        }
        FFSync ffSync = new FFSync(folder, ips);

        ffSync.createLogFile();

        // HTTP
        try {
            System.out.println("Starting HTTP server connection on localhost:" + args[4]);

            File folderHTTP = new File(ffSync.folderPath);
            HttpServer httpServer = new HttpServer(Integer.parseInt(args[4]), folderHTTP);
            Thread t1 = new Thread(httpServer);
            t1.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        RecentlyUpdated ru = new RecentlyUpdated();

        try {

            int port = Integer.parseInt(args[3]);
            int requestPort = Integer.parseInt(args[2]);

            DatagramSocket requestSocket = new DatagramSocket(requestPort);

            FTR ftr = new FTR(requestSocket, ffSync.folderPath, ffSync.allFiles, ffSync.syncronized, port,
                    ffSync.receivedFiles, ru, ffSync.watching);

            Thread t = new Thread(ftr);
            t.start();

            // Synchronize with all peers
            for (int i = 0; i < ffSync.ips.size() - 3; i++) { // remove -3

                SendHandler sh = new SendHandler(1, InetAddress.getByName(ffSync.ips.get(i)), port, ffSync.seq,
                        ffSync.getFiles(ffSync.folderPath));

                Thread syn = new Thread(sh);
                syn.start();
                ffSync.seq++;
            }

            // waits for synchronization
            while (!ffSync.isSync()) {
                Thread.sleep(100);
            }
            List<FileIP> neededFiles = ffSync.neededFilesCalculator(ffSync.folderPath);

            for (FileIP fi : neededFiles) {

                String file = fi.getFile().getParentFile().getName() + "/" + fi.getFile().getName();

                SendHandler sh = new SendHandler(2, fi.getIp(), port, ffSync.seq,
                        file.trim());

                Thread read = new Thread(sh);
                read.start();
                ffSync.seq++;
                System.out.println("Reading file:" + fi.getFile().getName());
            }

            // waits for the reception of all files

            while (ffSync.receivedFiles.size() != neededFiles.size()) { // alterar condição
                Thread.sleep(100);
            }

            ffSync.watching.add(true);

            System.out.println("\nInitial tranfer finished\n");

            // starts watching for modifications in the folder

            MainWatch mw = new MainWatch(ffSync.ips, ffSync.seq, port, ru);

            mw.wathcFolder(ffSync.folderPath);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
