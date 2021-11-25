import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.List;

public class RequestHandler implements Runnable {

    private DatagramPacket inPacket;
    private DatagramSocket socket;


    public RequestHandler  (DatagramPacket inPacket) throws SocketException {
        this.inPacket = inPacket;
        System.out.println("Packet Received from: " + inPacket.getAddress().toString() +":" + inPacket.getPort());
        this.socket = new DatagramSocket();
       
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