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
    private List<FileIP> allFiles;
    private List<Boolean> syncronized;
    private int port;
    private Map<String, Map<Integer, TranferState>> transfers;
    private List<Boolean> receivedFiles;
    private RecentlyUpdated recentlyUpdated;
    private List<Boolean> watching;

    private final static int MTU = 5000;

    public FTR(DatagramSocket requestSocket, String folderPath, List<FileIP> allFiles, List<Boolean> syncronized,
            int port, List<Boolean> receivedFiles, RecentlyUpdated recentlyUpdated, List<Boolean> watching) {

        this.requestSocket = requestSocket;
        this.folderPath = folderPath;
        this.transfers = new HashMap<>();
        this.allFiles = allFiles;
        this.syncronized = syncronized;
        this.port = port;
        this.receivedFiles = receivedFiles;
        this.recentlyUpdated = recentlyUpdated;
        this.watching = watching;

    }

    public void run() {

        boolean running = true;

        try {
            while (running) {
                byte[] inBuffer = new byte[MTU];
                DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
                this.requestSocket.receive(inPacket);

                Map<Integer, TranferState> tfs;

                if (transfers.containsKey(inPacket.getAddress().toString())) {
                    tfs = this.transfers.get(inPacket.getAddress().toString());
                } else {
                    tfs = new HashMap<>();
                    String key = inPacket.getAddress().toString() + inPacket.getPort(); // could be removed later
                    this.transfers.put(key, tfs);
                }

                Boolean watch = false;

                if (this.watching.size() == 1)
                    watch = true;

                RequestHandler rh = new RequestHandler(inPacket, this.folderPath, tfs, this.allFiles, this.syncronized,
                        this.port, this.receivedFiles, this.recentlyUpdated, watch); // send received packet to new
                                                                                     // thread to
                // be treated
                Thread t = new Thread(rh);

                t.start();
            }

            this.requestSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
