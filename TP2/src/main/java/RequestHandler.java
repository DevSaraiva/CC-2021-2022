import java.io.*;
import java.lang.instrument.Instrumentation;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class RequestHandler implements Runnable {

    private Lock l = new ReentrantLock();
    private DatagramPacket inPacket;
    private DatagramSocket socket;
    private String folderPath;
    private Map<Integer,TranferState> tfs;
    private  final int dataSize = 1400;
    private List<FileIP> allFiles;
    private List<Boolean> syncronized;





    public RequestHandler  (DatagramPacket inPacket, String folderPath, Map<Integer,TranferState> tfs,List<FileIP> allFiles,List<Boolean> syncronized) {
        this.inPacket = inPacket;

        this.syncronized = syncronized;

        this.allFiles = allFiles;


        try{
            this.socket = new DatagramSocket();

        } catch (Exception e){
            e.printStackTrace();
        }

        this.tfs = tfs;

        this.folderPath = folderPath;
       
    }


    public RequestHandler(){
        try{
            this.socket = new DatagramSocket();

        } catch (Exception e){
            e.printStackTrace();
        }
    }


    public void getSyn(ByteBuffer bb){

        int seq = bb.getInt();
        int length = bb.getInt();

        byte[] data = new byte[length];

        bb.get(data,0,length);

        try{

            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data));
            File[] files = (File[]) in.readObject();
            in.close();

            this.l.lock();

            for(int i = 0; i < files.length; i++){

                FileIP fi = new FileIP(files[i],this.inPacket.getAddress().toString());
                this.allFiles.add(fi);
            }

        }catch(Exception e){
            e.printStackTrace();
        }
        finally {
            this.l.unlock();
        }

        sendACK(this.inPacket.getAddress(),this.inPacket.getPort(),seq,0);
        this.l.lock();
        try{
            this.syncronized.add(true);
        }
        finally {
            this.l.unlock();
        }

    }


    public void getAck(ByteBuffer bb){

        int seq = bb.getInt();
        int block = bb.getInt();


    }

    public void getWrite(ByteBuffer bb){
        int seq = bb.getInt();
        int blocks = bb.getInt();



        CharBuffer ch = bb.asCharBuffer();
        String fileName = ch.toString();

        TranferState tf = new TranferState(fileName,blocks);

        this.tfs.put(seq,tf);

        sendACK(this.inPacket.getAddress(),this.inPacket.getPort(),seq,blocks);


    }


    public void getData(ByteBuffer bb) {

        int length = bb.getInt();
        int seq = bb.getInt();
        int block = bb.getInt();
        byte[] data = bb.array();



        TranferState tf = this.tfs.get(seq);


        byte[] dataFinal = new byte[length];

        for (int i = 0; i < length; i++) {
            dataFinal[i] = data[i + 16];
        }

        tf.addBytes(dataFinal);
        tf.increaseBlocks();


        if (tf.isFinished(block)) {

            System.out.println("finished");

            String path = this.folderPath + '/' + tf.getFileName();

            System.out.println(tf.getBytes().length);

                try {
                    File outputFile = new File(path.trim());
                    FileOutputStream outputStream = new FileOutputStream(outputFile);
                    outputStream.write(tf.getBytes());  // Write the bytes and you're done.

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            sendACK(this.inPacket.getAddress(),this.inPacket.getPort(),seq,block);

        }


    public void getFin(ByteBuffer bb){
        int seq = bb.getInt();
    }

    public void sendSyn(InetAddress ip, int port, int seq, File[] files){

        try {

            final int identifier = 0;

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(files);
            oos.flush();
            byte[] data = bos.toByteArray();


            ByteBuffer buff = ByteBuffer.allocate(12 + data.length).
                    putInt(identifier).
                    putInt(seq).
                    putInt(data.length).
                    put(data);


            byte[] packet = buff.array();

            DatagramPacket outPacket = new DatagramPacket(packet, packet.length, ip, port);
            this.socket.send(outPacket);

            this.socket.setSoTimeout(1000);


            //Waits Ack

            byte[] inBuffer = new byte[20];
            DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
            this.socket.receive(inPacket);


        }
        catch (SocketTimeoutException e) {

            sendSyn(ip,port,seq,files);
            System.out.println("timeout");
        }

         catch (Exception e){
            e.printStackTrace();
        }
    }

    public void sendACK(InetAddress ip, int port, int seq, int block){


        try {


            final int identifier = 1;


            byte[] packet = ByteBuffer.
                    allocate(12).
                    putInt(identifier).
                    putInt(seq).
                    putInt(block).
                    array();



            DatagramPacket outPacket = new DatagramPacket(packet, packet.length, ip, port);
            this.socket.send(outPacket);



        }
        catch (Exception e){
            e.printStackTrace();
        }

    }


    public void sendWrite(String ip, int port,int seq, int blocks, String filename){

        final int identifier = 2;

        try {


            ByteBuffer buff = ByteBuffer.allocate(12 + 2 * filename.length()).
                    putInt(identifier).
                    putInt(seq).
                    putInt(blocks);

            CharBuffer cbuff = buff.asCharBuffer();

            cbuff.put(filename);


            byte[] packet = buff.array();


            InetAddress ipServer = InetAddress.getByName(ip);
            DatagramPacket outPacket = new DatagramPacket(packet, packet.length, ipServer, port);
            this.socket.send(outPacket);

            //receive ack

            byte[] inBuffer = new byte[20];
            DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
            this.socket.receive(inPacket);


        }
        catch (Exception e){
            e.printStackTrace();
        }


    }


    public void sendData(String ip, int port,int seq, int block, byte[] data){

        final int identifier = 3;

        try {
            byte[] packet = ByteBuffer.
                    allocate(16 + data.length).
                    putInt(identifier).
                    putInt(data.length).
                    putInt(seq).
                    putInt(block).
                    put(data).array();


            InetAddress ipServer = InetAddress.getByName(ip);
            DatagramPacket outPacket = new DatagramPacket(packet, packet.length, ipServer, port);
            this.socket.send(outPacket);

            byte[] inBuffer = new byte[20];
            DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
            this.socket.receive(inPacket);



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


        int blocks = (int)Math.ceil(fileContent.length/this.dataSize);

        System.out.println("blocks :" + blocks);

        sendWrite(ip,port,seq,blocks,file.getName());

        // Slicing the data to datasize packets

        final int dataSize = this.dataSize;
        int start = 0;
        int end = 0;
        int k = 0;
        int i = 0;

        for(i = 0; (i + 1) * dataSize < fileContent.length; i++){

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

        start = i * dataSize;
        end = fileContent.length;

        byte[] data = new byte[end-start];

        for(int j = start; j < end; j++){

            data[k] = fileContent[j];
            k++;
        }

        sendData(ip,port,seq,i,data);


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
            this.socket.send(outPacket);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



        @Override
    public void run() {

        byte[] inBuffer = this.inPacket.getData();


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

    }

}