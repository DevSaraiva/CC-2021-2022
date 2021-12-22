import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

public class SendHandler implements Runnable {

    private DatagramSocket socket;
    private final int dataSize = 1400;
    private int mode;
    private InetAddress ip;
    private int port;
    private int seq;
    private File[] files;
    private String fileS;
    private String ipS;
    private boolean subfolder;
    private File file;

    // 1-SendSyn
    // 2-SendRead
    // 3- SendFile

    public SendHandler(DatagramSocket socket) {
        this.socket = socket;
    }

    public SendHandler(int mode, InetAddress ip, int port, int seq, File[] files) {

        this.mode = mode;
        try {
            this.socket = new DatagramSocket();
        } catch (SocketException e) {

            e.printStackTrace();
        }

        this.ip = ip;
        this.port = port;
        this.seq = seq;
        this.files = files;

    }

    public SendHandler(int mode, String ip, int port, int seq, String file) {

        this.mode = mode;

        try {
            this.socket = new DatagramSocket();
        } catch (SocketException e) {

            e.printStackTrace();
        }

        this.ipS = ip;
        this.port = port;
        this.seq = seq;
        this.fileS = file;

    }

    public SendHandler(int mode, String ip, int port, int seq, File file, boolean subfolder) {

        this.mode = mode;

        try {
            this.socket = new DatagramSocket();
        } catch (SocketException e) {

            e.printStackTrace();
        }

        this.ipS = ip;
        this.port = port;
        this.seq = seq;
        this.file = file;
        this.subfolder = subfolder;

    }

    public void sendSyn(InetAddress ip, int port, int seq, File[] files) {

        try {

            final int identifier = 0;

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(files);
            oos.flush();
            byte[] data = bos.toByteArray();

            ByteBuffer buff = ByteBuffer.allocate(12 + data.length)
                    .putInt(identifier)
                    .putInt(seq)
                    .putInt(data.length)
                    .put(data);

            byte[] packet = Hmac.addHmac(buff);

            DatagramPacket outPacket = new DatagramPacket(packet, packet.length, ip, port);

            this.socket.send(outPacket);

            this.socket.setSoTimeout(1000);

            // Waits Ack

            byte[] inBuffer = new byte[32];
            DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
            this.socket.receive(inPacket);

            while (!verifyACK(seq, 0, inPacket)) {
                this.socket.receive(inPacket);
            }

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

            ByteBuffer buff = ByteBuffer.allocate(12)
                    .putInt(identifier)
                    .putInt(seq)
                    .putInt(block);

            byte[] packet = Hmac.addHmac(buff);

            DatagramPacket outPacket = new DatagramPacket(packet, packet.length, ip, port);
            this.socket.send(outPacket);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public boolean verifyACK(int seq, int block, DatagramPacket inPacket) {

        ByteBuffer bb = ByteBuffer.wrap(inPacket.getData());
        int id = bb.getInt();
        int rSeq = bb.getInt();
        int rBlock = bb.getInt();

        byte[] mac = new byte[20];
        bb.get(mac, 0, 20);

        boolean res = false;

        // refactoring the packet

        ByteBuffer receivedACK = ByteBuffer.allocate(12)
                .putInt(id)
                .putInt(rSeq)
                .putInt(rBlock);

        try {
            if (rSeq == seq && rBlock == block && Hmac.verifyHMAC(receivedACK.array(), mac))
                res = true;
        } catch (Exception e) {

            e.printStackTrace();
        }

        return res;

    }

    public void sendRead(String ip, int port, int seq, String filename) {

        final int identifier = 4;

        try {

            byte[] fileNameBytes = filename.getBytes();

            ByteBuffer buff = ByteBuffer.allocate(12 + filename.length()).putInt(identifier).putInt(seq)
                    .putInt(filename.length()).put(fileNameBytes);

            byte[] packet = Hmac.addHmac(buff);

            InetAddress ipServer = InetAddress.getByName(ip);
            DatagramPacket outPacket = new DatagramPacket(packet, packet.length, ipServer, port);
            this.socket.send(outPacket);

            // verify Ack

            byte[] inBuffer = new byte[32];
            DatagramPacket packetAck = new DatagramPacket(inBuffer, inBuffer.length);
            this.socket.setSoTimeout(1000);
            this.socket.receive(packetAck);

            while (!verifyACK(seq, 0, packetAck)) {
                // waits ACK
                this.socket.receive(packetAck);
            }

        } catch (SocketTimeoutException e) {
            System.out.println("resending Read");
            sendRead(ip, port, seq, filename);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public int sendWrite(String ip, int port, int seq, int blocks, String filename) {

        int portToReceive = 0;

        final int identifier = 2;

        byte[] filenameBytes = filename.getBytes();

        try {

            ByteBuffer buff = ByteBuffer.allocate(16 + filename.length())
                    .putInt(identifier)
                    .putInt(seq)
                    .putInt(blocks)
                    .putInt(filename.length())
                    .put(filenameBytes);

            byte[] packet = Hmac.addHmac(buff);

            InetAddress ipServer = InetAddress.getByName(ip);
            DatagramPacket outPacket = new DatagramPacket(packet, packet.length, ipServer, port);
            this.socket.send(outPacket);

            // Waits Ack

            this.socket.setSoTimeout(1000);

            byte[] inBuffer = new byte[32];
            DatagramPacket packetAck = new DatagramPacket(inBuffer, inBuffer.length);
            this.socket.receive(packetAck);

            while (!verifyACK(seq, 0, packetAck)) {
                this.socket.receive(packetAck);

            }

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

            ByteBuffer buff = ByteBuffer.allocate(16 + data.length).putInt(identifier).putInt(seq)
                    .putInt(block).putInt(data.length).put(data);

            byte[] packet = Hmac.addHmac(buff);

            InetAddress ipServer = InetAddress.getByName(ip);
            DatagramPacket outPacket = new DatagramPacket(packet, packet.length, ipServer, port);
            this.socket.send(outPacket);

            // Waits Ack

            this.socket.setSoTimeout(1000);

            byte[] inBuffer = new byte[32];
            DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
            this.socket.receive(inPacket);

            while (!verifyACK(seq, block, inPacket)) {
                this.socket.receive(inPacket);
            }

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

        long startTimer = System.currentTimeMillis();
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

        }

        start = i * dataSize;
        end = fileContent.length;

        byte[] data = new byte[end - start];

        for (int j = start; j < end; j++) {

            data[k] = fileContent[j];
            k++;
        }

        sendData(ip, clientHandlerPort, seq, i, data);

        long stopTimer = System.currentTimeMillis();
        long durationInMillis = stopTimer - startTimer;
        long durationInSeconds = durationInMillis*1000;

        long debit = (file.length()*8) / durationInSeconds;
        // regist into the log file

        String log = file.getName() + " was sent to " + ip + " | Duration= " + durationInMillis + "ms | Debit= " + debit + "bits/sec";
        FileAppend("logs.txt", log);

    }

    public void FileAppend(String fileName, String log) throws IOException {
        try (FileWriter f = new FileWriter(fileName, true);
                BufferedWriter bufferedWriter = new BufferedWriter(f);
                PrintWriter printWriter = new PrintWriter(bufferedWriter);) {
            printWriter.println(log);
            printWriter.close();
        }
    }

    @Override
    public void run() {

        switch (this.mode) {
            case 1:
                sendSyn(this.ip, this.port, this.seq, this.files);
                break;

            case 2:
                sendRead(this.ipS, this.port, this.seq, this.fileS);
                break;

            case 3:
                try {
                    sendFile(this.ipS, this.port, this.seq, this.file, this.subfolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

        }

    }

}
