import javax.swing.plaf.synth.SynthOptionPaneUI;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FTR implements Runnable {

    private DatagramSocket requestSocket;
    private DatagramSocket sendSocket;
    private String fileNamesReceived;



    private final static int MTU = 1500;


    public FTR(int requestPort){

        try{
            this.requestSocket = new DatagramSocket(requestPort);
            this.sendSocket = new DatagramSocket();
        }
        catch (SocketException e){
            e.printStackTrace();
        }
    }


    public void run(){

        boolean running = true;

        try {
            while (running) {                                           // inifnite loop - very bad pratice
                byte[] inBuffer = new byte[MTU];
                // create the packet to receive the data from client
                DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
                this.requestSocket.receive(inPacket);
                RequestHandler rh = new RequestHandler(inPacket);         // send received packet to new thread to be treated
                Thread t = new Thread(rh);
                t.start();
            }


            this.requestSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void sendSyn(String ip, int port, File[] files){


        try {


            final int identifier = 0;

            List<String> names = new ArrayList<>();

            for(File f : files){

                names.add(f.getName());

            }

            String fileNames = names.toString();

            ByteBuffer buff = ByteBuffer.allocate(4 + 2 * fileNames.length()).putInt(identifier);

            CharBuffer cbuff = buff.asCharBuffer();

            cbuff.put(fileNames);

            byte[] packet = buff.array();

            InetAddress ipServer = InetAddress.getByName(ip);
            System.out.println("Conecting to: " + ipServer.toString() +":" +port);

            DatagramPacket outPacket = new DatagramPacket(packet, packet.length, ipServer, port);
            this.sendSocket.send(outPacket);



        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void sendACK(String ip, int port, int seq, int block){


        try {


            final int identifier = 1;


            byte[] packet = ByteBuffer.
                          allocate(12).
                          putInt(identifier).
                          putInt(seq).
                          putInt(block).
                          array();


            InetAddress ipServer = InetAddress.getByName(ip);
            DatagramPacket outPacket = new DatagramPacket(packet, packet.length, ipServer, port);
            this.sendSocket.send(outPacket);



        }
        catch (Exception e){
            e.printStackTrace();
        }


    }


    public void sendWrite(String ip, int port,int seq,String filename){

        final int identifier = 2;

        try {


            ByteBuffer buff = ByteBuffer.allocate(8 + filename.length()).
                                        putInt(identifier).
                                        putInt(seq);

            CharBuffer cbuff = buff.asCharBuffer();

            cbuff.put(filename);


            byte[] packet = buff.array();


            InetAddress ipServer = InetAddress.getByName(ip);
            DatagramPacket outPacket = new DatagramPacket(packet, packet.length, ipServer, port);
            this.sendSocket.send(outPacket);
        }
        catch (Exception e){
            e.printStackTrace();
        }


    }


    public void sendData(String ip, int port,int seq, int block, byte[] data){

        final int identifier = 3;

        try {
            byte[] packet = ByteBuffer.
                    allocate(12 + data.length).
                    putInt(identifier).
                    putInt(seq).
                    putInt(block).
                    put(data).array();


            InetAddress ipServer = InetAddress.getByName(ip);
            DatagramPacket outPacket = new DatagramPacket(packet, packet.length, ipServer, port);
            this.sendSocket.send(outPacket);
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }


    public void sendFile(String ip, int port,int seq, File file){

        byte[] fileContent = null;
        try {
            fileContent = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

       // Slicing the data to 512 packets

        final int dataSize = 512;
        int start = 0;
        int end = 0;
        int k = 0;

        for(int i = 0; (i + 1) * dataSize < fileContent.length; i++){

            start = i * dataSize;
            end = (i + 1) * dataSize;

            byte[] data = new byte[dataSize];

            for(int j = start; j < end; j++){

                data[k] = fileContent[j];
                k++;
            }

            k = 0;

            sendData(ip,port,seq,i,data);

        }

    }

    public void sendFin(String ip, int port,int seq){

        try {


            final int identifier = 4;


            byte[] packet = ByteBuffer.
                    allocate(8).
                    putInt(identifier).
                    putInt(seq).
                    array();


            InetAddress ipServer = InetAddress.getByName(ip);
            DatagramPacket outPacket = new DatagramPacket(packet, packet.length, ipServer, port);
            this.sendSocket.send(outPacket);

        }
        catch (Exception e){
            e.printStackTrace();
        }





    }










}
