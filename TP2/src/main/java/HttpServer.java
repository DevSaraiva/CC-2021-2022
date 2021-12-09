import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;



// este protocolo ocorre na camada 7 do modelo OSI

public class HttpServer {
    public void startHTTP( String[] logs, int port) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(port)) {  //using a TCP socket to handle a TCP connection
            while (true) {
                try (Socket client = serverSocket.accept()) {   //espera ate poder ter uma conexao de um cliente para aceitar
                    handleClient(client, logs);
                }
            }
        }
    }


    private static void handleClient(Socket client, String[] logs) throws IOException {
        System.out.println("Debug: got new client " + client.toString() + "\n");
        BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream())); //ler a request para um buffered reader

        StringBuilder requestBuilder = new StringBuilder();
        String line;
        while (!(line = br.readLine()).isBlank()) {     //A request acaba sempre com uma linha vazia (\r\n), portanto terminamos de ler nesse caso
            requestBuilder.append(line + "\r\n");       ////ler a request a partir do socket do cliente.
        }

        //Parsing da request
        String request = requestBuilder.toString();
        String[] requestsLines = request.split("\r\n"); //\r\n indica new line
        String[] requestLine = requestsLines[0].split(" "); //separar em: {"GET", "/", "HTTP/1.1"}
        String method = requestLine[0]; //method = "GET"
        String path = requestLine[1];   //path = "/"
        String version = requestLine[2];    //version = "HTTP/1.1"
        String host = requestsLines[1].split(" ")[1];   //host = "localhost:8080"

        List<String> headers = new ArrayList<>();
        for (int h = 2; h < requestsLines.length; h++) {    //headers começam a partir da terceira linha (corresponde ao indice 2)
            String header = requestsLines[h];
            headers.add(header);
        }

        String accessLog = String.format("Client %s, method %s, path %s, version %s, host %s, headers %s",
                client.toString(), method, path, version, host, headers.toString());
        System.out.println(accessLog);
        System.out.println("\nRequest is: \n" + request + "\n");

        //enviar a resposta para o output stream do cliente
        OutputStream clientOutput = client.getOutputStream();
        clientOutput.write("HTTP/1.1 200 OK\r\n".getBytes());
        clientOutput.write(("ContentType: text/html\r\n").getBytes());
        clientOutput.write("\r\n".getBytes());
        //clientOutput.write("<b>Test String!</b>".getBytes());
        for (String s : logs) {
            clientOutput.write(s.getBytes());
        }
        clientOutput.write("\r\n\r\n".getBytes());
        clientOutput.flush();
        client.close();

        System.out.println("Response is: \n" + request + "\n");
    }

}
