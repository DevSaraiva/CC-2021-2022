import javax.swing.plaf.synth.SynthOptionPaneUI;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;


public class FTR implements Runnable {

    private DatagramSocket requestSocket;
    private String folderPath;


    Map<String,Map<Integer,TranferState>> transfers ;


    private final static int MTU = 1500;


    public FTR(DatagramSocket requestSocket, String folderPath){

            this.requestSocket = requestSocket;
            this.folderPath = folderPath;
            this.transfers = new HashMap<>();

    }


    public void run(){

        boolean running = true;

        try {
            while (running) {
                byte[] inBuffer = new byte[MTU];
                DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
                this.requestSocket.receive(inPacket);

                Map<Integer,TranferState> tfs;

                if(transfers.containsKey(inPacket.getAddress().toString())){
                    tfs = this.transfers.get(inPacket.getAddress().toString());
                }else{
                    tfs = new HashMap<>();
                    this.transfers.put(inPacket.getAddress().toString(),tfs);
                }


                RequestHandler rh = new RequestHandler(inPacket,this.folderPath,tfs);         // send received packet to new thread to be treated
                Thread t = new Thread(rh);


                t.start();
            }


            this.requestSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
