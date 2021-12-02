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




    private final static int MTU = 1500;


    public FTR(DatagramSocket requestSocket, DatagramSocket sendSocket){

            this.requestSocket = requestSocket;
            this.sendSocket = sendSocket;

    }


    public void run(){

        boolean running = true;

        try {
            while (running) {                                           // inifnite loop - very bad pratice
                byte[] inBuffer = new byte[MTU];
                // create the packet to receive the data from client
                DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
                this.requestSocket.receive(inPacket);
                RequestHandler rh = new RequestHandler(inPacket,sendSocket);         // send received packet to new thread to be treated
                Thread t = new Thread(rh);
                t.start();
            }


            this.requestSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
