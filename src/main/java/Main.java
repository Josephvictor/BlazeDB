import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
  public static void main(String[] args) throws IOException{

       ServerSocket serverSocket = null;
       Socket clientSocket = null;
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

        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

        String headers = in.readLine();
        System.out.println(headers);
        
        String response = "";
        if(headers.contains("PING")){
          response = "+PONG\r\n";
        }
        
        System.out.println(response);
        out.write(response);
         
       } catch (IOException e) {
         System.out.println("IOException: " + e.getMessage());
       } finally {
         clientSocket.close();
         serverSocket.close();
       }
  }
}
