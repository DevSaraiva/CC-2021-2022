import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RequestHandler implements Runnable {

    private DatagramPacket inPacket;
    private DatagramSocket sendSocket;


    public RequestHandler  (DatagramPacket inPacket, DatagramSocket SendSocket) {
        this.inPacket = inPacket;
        System.out.println("Packet Received from: " + inPacket.getAddress().toString() +":" + inPacket.getPort());
        this.sendSocket = SendSocket;
       
    }

    public RequestHandler(DatagramSocket sendSocket){
        this.sendSocket = sendSocket;
    }


    public void getSyn(ByteBuffer bb){

        CharBuffer ch = bb.asCharBuffer();

        System.out.println(ch.toString());

    }


    public void getAck(ByteBuffer bb){

        int seq = bb.getInt();
        int block = bb.getInt();


    }

    public void getWrite(ByteBuffer bb){
        int seq = bb.getInt();
        CharBuffer ch = bb.asCharBuffer();
        String fileName = ch.toString();
    }


    public void getData(ByteBuffer bb){

        int seq = bb.getInt();
        int block = bb.getInt();
        byte[] data = bb.array();
        System.out.println(Arrays.toString(data));
        System.out.println("3" + "\n" + seq + "\n" + block);
    }

    public void getFin(ByteBuffer bb){
        int seq = bb.getInt();
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

    public void sendFin(String ip, int port,int seq) {

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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



        @Override
    public void run() {

        byte[] inBuffer = this.inPacket.getData();                  // get client Data
        InetAddress clientIp = this.inPacket.getAddress();          // get client IP
        int port = this.inPacket.getPort();                         // get client port


        ByteBuffer bb = ByteBuffer.wrap(inBuffer);

        int identifier = bb.getInt();

        switch(identifier) {
            case 0:
                getSyn(bb);
                break;
            case 1:
                getAck(bb);
                break;

            case 2:
                getWrite(bb);
                break;

            case 3:
                getData(bb);
                break;

            case 4:
                getFin(bb);
                break;
            default:
                // code block
        }



        String receivedString = new String(inBuffer);
        receivedString = receivedString.toUpperCase();              // to Upper case




    }

}