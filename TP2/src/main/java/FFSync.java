import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

public class FFSync {

    private int port = 12345;
    private String folderPath;
    private List<String> ips;



    public FFSync(String folderPath, List<String> ips){

        this.folderPath = folderPath;
        this.ips = ips;

    }



    public static void main (String[] args){




            String folderPath = args[0];
            List<String> ips = new ArrayList<>();

            for(int i = 1; i < args.length; i++){
                ips.add(args[i]);
            }

            FFSync ffSync = new FFSync(folderPath,ips);


        try {

            DatagramSocket receiveSocket = new DatagramSocket(Integer.parseInt(args[2])) ;


            FTR ftr = new FTR(receiveSocket);

            Thread t = new Thread(ftr);

            t.start();

            ftr.send(ffSync.ips.get(0),Integer.parseInt(args[3]));

        }catch (Exception e){
            e.printStackTrace();
        }



        }
}
