import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class FTR implements Runnable {

    private DatagramSocket requestSocket;
    private DatagramSocket sendSocket;


    private final static int MTU = 1500;


    public FTR(int requestPort, int sendPort){
        try {
            this.requestSocket = new DatagramSocket(requestPort);
            this.sendSocket = new DatagramSocket(sendPort);
        } catch (SocketException e) {
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
                RequestHandler rh = new RequestHandler(inPacket);         // send receiverd packet to new thread to be treated
                Thread t = new Thread(rh);
                t.start();
            }


            this.requestSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void send(String ip, int port) { // receives the IP of the server // recieves the port of the server
        try {
            InetAddress ipServer = InetAddress.getByName(ip);
            System.out.println("Conecting to: " + ipServer.toString() +":" +port);


            DatagramSocket clientSocket = new DatagramSocket();                         // creates a socket - port not define - system gives an available port

            // buffer to read from the console
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String lineFromConsole = reader.readLine();                                 // reading from the console

            while (!lineFromConsole.equalsIgnoreCase("quit")) {
                byte[] inBuffer = new byte[MTU];
                byte[] outBuffer = new byte[MTU];

                // from the console to the socket - sending a message
                outBuffer = lineFromConsole.getBytes();
                DatagramPacket outPacket = new DatagramPacket(outBuffer, outBuffer.length, ipServer, port);
                clientSocket.send(outPacket);

                // from the socket to the console - reading a message
                DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
                clientSocket.receive(inPacket);
                System.out.println(new String(inPacket.getData()));

                lineFromConsole = reader.readLine();                                    // reading from console
            }
            clientSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }







}
