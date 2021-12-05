import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FFSync {

    private int port = 8888;
    private String folderPath;
    private List<String> ips;



    public FFSync(String folderPath, List<String> ips){

        String currentPath = null;
        try {
            currentPath = new File(".").getCanonicalPath();
            this.folderPath = currentPath + "/" + folderPath;
        } catch (IOException e) {
            e.printStackTrace();
        }


        this.ips = ips;

    }


    public File[] getFiles(){

        File folder = new File(this.folderPath);
        File[] files = folder.listFiles();


        return files;
    }


    public static void main (String[] args){




            String folder = args[0];
            List<String> ips = new ArrayList<>();

            for(int i = 1; i < args.length; i++){
                ips.add(args[i]);
            }


            FFSync ffSync = new FFSync(folder,ips);




        try {

            int requestPort = Integer.parseInt(args[2]);

            String ip = ffSync.ips.get(0);

            int port = Integer.parseInt(args[3]);

            DatagramSocket requestSocket = new DatagramSocket(requestPort);

            DatagramSocket sendSocket = new DatagramSocket();


            FTR ftr = new FTR(requestSocket,sendSocket,ffSync.folderPath);

            RequestHandler rq = new RequestHandler();

            Thread t = new Thread(ftr);

            t.start();

            rq.sendFile(ip ,port,0,ffSync.getFiles()[0]);




        }catch (Exception e){
            e.printStackTrace();
        }



        }
}
