import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Main {
  public static void main(String[] args) throws IOException{

       ServerSocket serverSocket = null;
       Socket clientSocket = null;
       BufferedReader in = null;
       BufferedWriter out = null;

       int port = 6379;

       try {
        System.out.println("**Server started**");
         serverSocket = new ServerSocket(port);
         // Since the tester restarts your program quite often, setting SO_REUSEADDR
         // ensures that we don't run into 'Address already in use' errors
         serverSocket.setReuseAddress(true);
         // Wait for connection from client.
         clientSocket = serverSocket.accept();
         System.out.println("Connection made: "+clientSocket.getInetAddress());

        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        System.out.println("in and out created");

        List<String> headers = new ArrayList<>();
        String header;
        while((header = in.readLine()) != ""){
          headers.add(header);
          System.out.println(headers);
        }

        clientSocket.getOutputStream().write("+PONG\r\n".getBytes());

        // System.out.println("[headers] "+headers);
        
        // String response = "+PONG\r\n";
        
        // System.out.println("[response] "+response);
        // out.write(response);

        // in.close();
        // out.close(); 
        // clientSocket.close();
        // serverSocket.close();
       } catch (IOException e) {
         System.out.println("IOException: " + e.getMessage());
       }
  }
}
