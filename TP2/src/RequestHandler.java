import jdk.swing.interop.SwingInterOpUtils;

import java.io.*;
import java.net.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.sql.SQLOutput;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RequestHandler implements Runnable {

    private Lock l = new ReentrantLock();
    private DatagramPacket inPacket;
    private DatagramSocket socket;
    private String folderPath;
    private Map<Integer, TranferState> tfs;
    private final int dataSize = 1400;
    private List<FileIP> allFiles;
    private List<Boolean> syncronized;
    private int port;
    private final static int MTU = 1500;
    private List<Boolean> receivedFiles;

    public RequestHandler(DatagramPacket inPacket, String folderPath, Map<Integer, TranferState> tfs,
            List<FileIP> allFiles, List<Boolean> syncronized, int port, List<Boolean> receivedFiles) {

        this.inPacket = inPacket;

        this.port = port;

        this.syncronized = syncronized;

        this.allFiles = allFiles;

        try {
            this.socket = new DatagramSocket();

        } catch (Exception e) {
            e.printStackTrace();
        }

        this.tfs = tfs;

        this.folderPath = folderPath;

        this.receivedFiles = receivedFiles;

    }

    public RequestHandler() {
        try {
            this.socket = new DatagramSocket();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getSyn(ByteBuffer bb) {

        int seq = bb.getInt();
        int length = bb.getInt();

        byte[] data = new byte[length];

        bb.get(data, 0, length);

        try {

            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data));
            File[] files = (File[]) in.readObject();
            in.close();

            this.l.lock();

            for (int i = 0; i < files.length; i++) {

                FileIP fi = new FileIP(files[i], this.inPacket.getAddress().toString().substring(1));
                this.allFiles.add(fi);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.l.unlock();
        }

        sendACK(this.inPacket.getAddress(), this.inPacket.getPort(), seq, 0);
        this.l.lock();
        try {
            this.syncronized.add(true);
        } finally {
            this.l.unlock();
        }

    }

    public void getAck(ByteBuffer bb) {

        int seq = bb.getInt();
        int block = bb.getInt();

    }

    public void getRead(ByteBuffer bb) throws IOException {

        int seq = bb.getInt();

        CharBuffer ch = bb.asCharBuffer();
        String fileName = ch.toString();

        sendACK(this.inPacket.getAddress(), this.inPacket.getPort(), seq, 0);

        File folder = new File(folderPath);

        String file = null;

        if (fileName.contains(folder.getName())) {

            file = folder.getParent() + "/" + fileName;
            File f = new File(file.trim());
            sendFile(this.inPacket.getAddress().toString().substring(1), this.port, seq, f, false);

        } else {

            file = folderPath + "/" + fileName;
            File f = new File(file.trim());
            sendFile(this.inPacket.getAddress().toString().substring(1), this.port, seq, f, true);
        }

    }

    public void getWrite(ByteBuffer bb) {

        int seq = bb.getInt();
        int blocks = bb.getInt();

        CharBuffer ch = bb.asCharBuffer();

        String fileName = ch.toString();

        TranferState tf = null;

        if (fileName.contains("/")) {
            String[] strings = fileName.split("/");
            tf = new TranferState(strings[1], blocks, strings[0]);
        } else {
            tf = new TranferState(fileName, blocks, null);
        }

        this.l.lock();
        try {
            this.tfs.put(seq, tf);
        } finally {
            this.l.unlock();
        }

        sendACK(this.inPacket.getAddress(), this.inPacket.getPort(), seq, blocks);

        // receive all packets from the file

        for (int i = 0; i <= blocks; i++) {

            byte[] inBuffer = new byte[MTU];
            DatagramPacket packet = new DatagramPacket(inBuffer, inBuffer.length);
            try {
                this.socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            ByteBuffer bb2 = ByteBuffer.wrap(inBuffer);
            int id = bb2.getInt();
            if (id == 3)
                getData(bb2, packet.getAddress(), packet.getPort());

        }

        this.receivedFiles.add(true);

    }

    public void getData(ByteBuffer bb, InetAddress ip, int port) {

        int length = bb.getInt();
        int seq = bb.getInt();
        int block = bb.getInt();
        byte[] data = bb.array();

        TranferState tf = null;

        byte[] dataFinal = new byte[length];

        for (int i = 0; i < length; i++) {
            dataFinal[i] = data[i + 16];
        }

        this.l.lock();

        try {
            tf = this.tfs.get(seq);
            tf.addBytes(dataFinal);
            tf.increaseBlocks();

        } finally {

            this.l.unlock();
        }

        if (tf.isFinished(block)) {

            String path = this.folderPath + '/' + tf.getFileName();

            try {

                if (tf.existSubfolder()) {

                    String pathFolder = this.folderPath + "/" + tf.getSubFolder();

                    File f = new File(pathFolder.trim());
                    try {
                        f.mkdir();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                File outputFile = new File(path.trim());
                FileOutputStream outputStream = new FileOutputStream(outputFile);
                outputStream.write(tf.getBytes());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        sendACK(ip, port, seq, block);

    }

    public void sendSyn(InetAddress ip, int port, int seq, File[] files) {

        try {

            final int identifier = 0;

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(files);
            oos.flush();
            byte[] data = bos.toByteArray();

            ByteBuffer buff = ByteBuffer.allocate(12 + data.length).putInt(identifier).putInt(seq).putInt(data.length)
                    .put(data);

            byte[] packet = buff.array();

            DatagramPacket outPacket = new DatagramPacket(packet, packet.length, ip, port);
            this.socket.send(outPacket);

            this.socket.setSoTimeout(1000);

            // Waits Ack

            byte[] inBuffer = new byte[20];
            DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
            this.socket.receive(inPacket);

        }
        // resend packet
        catch (SocketTimeoutException e) {

            sendSyn(ip, port, seq, files);
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendACK(InetAddress ip, int port, int seq, int block) {

        try {

            final int identifier = 1;

            byte[] packet = ByteBuffer.allocate(12).putInt(identifier).putInt(seq).putInt(block).array();

            DatagramPacket outPacket = new DatagramPacket(packet, packet.length, ip, port);
            this.socket.send(outPacket);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void sendRead(String ip, int port, int seq, String filename) {

        final int identifier = 4;

        try {

            ByteBuffer buff = ByteBuffer.allocate(8 + 2 * filename.length()).putInt(identifier).putInt(seq);

            CharBuffer cbuff = buff.asCharBuffer();

            cbuff.put(filename);

            byte[] packet = buff.array();

            InetAddress ipServer = InetAddress.getByName(ip);
            DatagramPacket outPacket = new DatagramPacket(packet, packet.length, ipServer, port);
            this.socket.send(outPacket);

            // receive ACk

            byte[] inBuffer = new byte[20];
            DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
            this.socket.receive(inPacket);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public int sendWrite(String ip, int port, int seq, int blocks, String filename) {

        final int identifier = 2;

        int portToReceive = 0;

        try {

            ByteBuffer buff = ByteBuffer.allocate(12 + 2 * filename.length()).putInt(identifier).putInt(seq)
                    .putInt(blocks);

            CharBuffer cbuff = buff.asCharBuffer();

            cbuff.put(filename);

            byte[] packet = buff.array();

            InetAddress ipServer = InetAddress.getByName(ip);
            DatagramPacket outPacket = new DatagramPacket(packet, packet.length, ipServer, port);
            this.socket.send(outPacket);

            this.socket.setSoTimeout(1000);

            // Waits Ack

            byte[] inBuffer = new byte[20];
            DatagramPacket packetAck = new DatagramPacket(inBuffer, inBuffer.length);
            this.socket.receive(packetAck);

            portToReceive = packetAck.getPort();

        } catch (SocketTimeoutException e) {
            System.out.println("resending Write");
            sendWrite(ip, port, seq, blocks, filename);
        }

        catch (Exception e) {
            e.printStackTrace();
        }

        return portToReceive;
    }

    public void sendData(String ip, int port, int seq, int block, byte[] data) {

        final int identifier = 3;

        try {
            byte[] packet = ByteBuffer.allocate(16 + data.length).putInt(identifier).putInt(data.length).putInt(seq)
                    .putInt(block).put(data).array();

            InetAddress ipServer = InetAddress.getByName(ip);
            DatagramPacket outPacket = new DatagramPacket(packet, packet.length, ipServer, port);
            this.socket.send(outPacket);

            // Waits Ack

            this.socket.setSoTimeout(1000);

            byte[] inBuffer = new byte[20];
            DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
            this.socket.receive(inPacket);

            // resend the packet
        } catch (SocketTimeoutException e) {
            System.out.println("Resending packet seq:" + seq + " block:" + block);
            sendData(ip, port, seq, block, data);
        }

        catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void sendFile(String ip, int port, int seq, File file, boolean existSub) throws IOException {

        byte[] fileContent = null;
        try {
            fileContent = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        int blocks = (int) Math.ceil(fileContent.length / this.dataSize);

        String filename = null;

        if (existSub) {
            filename = file.getParentFile().getName() + "/" + file.getName();
        } else {
            filename = file.getName();
        }

        int clientHandlerPort = sendWrite(ip, port, seq, blocks, filename.trim());

        // Slicing the data to datasize packets

        final int dataSize = this.dataSize;
        int start = 0;
        int end = 0;
        int k = 0;
        int i = 0;

        for (i = 0; (i + 1) * dataSize < fileContent.length; i++) {

            start = i * dataSize;
            end = (i + 1) * dataSize;

            byte[] data = new byte[dataSize];

            for (int j = start; j < end; j++) {

                data[k] = fileContent[j];
                k++;
            }

            k = 0;

            sendData(ip, clientHandlerPort, seq, i, data);

            String log = file + " was sent to " + ip;
            FileAppend("logs.txt", log);

        }

        start = i * dataSize;
        end = fileContent.length;

        byte[] data = new byte[end - start];

        for (int j = start; j < end; j++) {

            data[k] = fileContent[j];
            k++;
        }

        sendData(ip, clientHandlerPort, seq, i, data);

    }


    public void FileAppend (String fileName, String log) throws IOException {
        try (FileWriter f = new FileWriter(fileName, true); BufferedWriter bufferedWriter = new BufferedWriter(f); PrintWriter printWriter = new PrintWriter(bufferedWriter);){
            printWriter.println(log);
            printWriter.close();
        }
    }

    /*
    public void createLogFile () {
        try {
            File logsFile = new File("logs.txt");
            if (!logsFile.exists()) {
                logsFile.createNewFile();
                System.out.println("logs.txt created successfully!!!");
            }
            else {
                System.out.println("logs.txt cannot be created - File already exists...");
            }
        } catch (IOException e) {
            System.out.println("An error ocurred while creating LogFile :(");
            e.printStackTrace();
        }
    }
     */

    @Override
    public void run() {

        byte[] inBuffer = this.inPacket.getData();
        ByteBuffer bb = ByteBuffer.wrap(inBuffer);

        int identifier = bb.getInt();

        switch (identifier) {
            case 0:
                getSyn(bb);
                break;

            case 2:
                getWrite(bb);
                break;

            case 4:
                try {
                    getRead(bb);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                System.out.println("NO MATCH");
        }

    }

}