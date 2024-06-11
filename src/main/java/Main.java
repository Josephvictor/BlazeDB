import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Main {
  public static void main(String[] args) throws IOException{

    int port = 6379;
    String role = "master";
    String master_replid = "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb";
    String master_repl_offset = "0";
    String[] mHostAndmPort = null;

    if(args.length >= 2){
      for(int i = 0; i < args.length; i=i+2){
        if(args[i].equalsIgnoreCase("--port"))  port = Integer.valueOf(args[i+1]);
        else if(args[i].equalsIgnoreCase("--replicaof")){
          mHostAndmPort = args[i+1].split(" ");
          role = "slave";
        }
      }
    }
    System.out.println("[Master][host] and [port] "+Arrays.toString(mHostAndmPort));

    Selector selector = Selector.open();
    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.bind(new InetSocketAddress("localhost", port));
    serverSocketChannel.configureBlocking(false);
    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

    System.out.println("[main] ***Server started***"); 
    
    Storage.addServerInfo("role", role);
    Storage.addServerInfo("master_replid", master_replid);
    Storage.addServerInfo("master_repl_offset", master_repl_offset);
    System.out.println("[main] Server details added");

    if(role.equalsIgnoreCase("slave")){

      SocketChannel socketChannel = SocketChannel.open();
      socketChannel.configureBlocking(false);
      socketChannel.register(selector, SelectionKey.OP_WRITE);
      ByteBuffer buffer = ByteBuffer.allocate(254);
      
      System.out.println("[main] Establishing connection to master");
      socketChannel.connect(new InetSocketAddress(mHostAndmPort[0], Integer.parseInt(mHostAndmPort[1])));
      //Make handshake
      //1.PING
      String connRequest = ResponseEncoder.ArraysEncoder("PING");
      buffer.clear();
      buffer.put(connRequest.getBytes());
      buffer.flip();
      while(buffer.hasRemaining()){
        socketChannel.write(buffer);
      }
      buffer.clear();
      
      System.out.println("[main] Master connection established");

    }
      

    while(true){
      int conns = selector.select();
      if(conns == 0)  continue;

      Set<SelectionKey> selectedKeys = selector.selectedKeys();
      Iterator<SelectionKey> iterator = selectedKeys.iterator();

      while(iterator.hasNext()){
        SelectionKey key = iterator.next();
        
        if(key.isAcceptable()){
          ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
          SocketChannel clientChannel = serverChannel.accept();
          
          clientChannel.configureBlocking(false);
          clientChannel.register(selector, SelectionKey.OP_READ);
          System.out.println("Connection accepted: "+ clientChannel.getRemoteAddress());

        }else if(key.isReadable()){
          SocketChannel clientChannel = (SocketChannel) key.channel();
          ByteBuffer buffer = ByteBuffer.allocate(254);
          int byteRead = clientChannel.read(buffer);
          
          if(byteRead == -1){
            clientChannel.close();
          }else{
            buffer.flip();
            String message = new String(buffer.array()).trim();
            System.out.println("[Received message] "+ message);
            
            List<String> parsedElements = RequestParser.parse(message);
            System.out.println("[main] "+parsedElements);

            String response = ProcessRequest.process(parsedElements);
            System.out.println("[Response] "+response);
            
            buffer.clear();
            buffer.put(response.getBytes());
            buffer.flip();
            while(buffer.hasRemaining()){
              clientChannel.write(buffer);
            }
            buffer.clear();
          }
        }
        iterator.remove();
      }
    }
  }
}
