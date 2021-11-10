public class FFSync {

        public static void main (String[] args){
// 1 - request
// 2- send
// 3 - ip to send
//4 - port to send

            String ip = args[2];
            int port = Integer.parseInt(args[3]);

            FTR ftr = new FTR( Integer.parseInt(args[0]),Integer.parseInt(args[1]));

            System.out.println("listening on port: " + args[0] + "sending from port: " + args[1]);

            Thread t = new Thread(ftr);

            t.start();

            ftr.send(ip,port);





        }
}
