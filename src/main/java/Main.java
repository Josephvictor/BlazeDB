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

  private static int port = 6379;
  private static String role = "master";
  private static String master_replid = "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb";
  private static String master_repl_offset = "0";
  private static String[] mHostAndmPort = null;

  //Track the 3 way handshake process
  private enum State{
    INITIAL, SENT_PING, SENT_REPLCONF_PORT, SENT_REPLCONF_CAPA, COMPLETE
  }

  private static State state = State.INITIAL;

  public static void main(String[] args) throws IOException{

    processArguments(args);
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
      System.out.println("[main] Establishing connection to master");
      socketChannel.connect(new InetSocketAddress(mHostAndmPort[0], Integer.parseInt(mHostAndmPort[1])));
      while(!socketChannel.finishConnect()){
        System.out.println("[main] Waiting for connection to fully establish");
      }
      System.out.println("[main] Master connection established");
      //Register to listen for write
      socketChannel.configureBlocking(false);
      socketChannel.register(selector, SelectionKey.OP_WRITE);
      //Send the PING request
      String connRequest = ResponseEncoder.ArraysEncoder("PING");
      ByteBuffer buffer = ByteBuffer.allocate(254);
      buffer.put(connRequest.getBytes());
      
      socketChannel.keyFor(selector).attach(buffer);
      state = State.SENT_PING;
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
          SocketChannel socketChannel = (SocketChannel) key.channel();
          ByteBuffer buffer = ByteBuffer.allocate(254);
          int byteRead = socketChannel.read(buffer);
          if(byteRead == -1){
            socketChannel.close();
          }else{
            String message = new String(buffer.array()).trim();
            System.out.println("[Received message] "+ message);
            
            List<String> parsedElements = RequestParser.parse(message);
            System.out.println("[main] "+parsedElements);

            String response = ProcessRequest.process(parsedElements);
            System.out.println("[Response] "+response);
            
            ByteBuffer responseBuffer = ByteBuffer.allocate(254);
            responseBuffer.put(response.getBytes());

            //Change states for the 3 way handshake
            if(state == State.SENT_PING && response.equals("+PONG")){
              state = State.SENT_REPLCONF_PORT;
              // Attach the response buffer to the key and switch to write mode
              key.attach(responseBuffer);
              key.interestOps(SelectionKey.OP_WRITE);
            }else if(state == State.SENT_REPLCONF_PORT && response.equals("+OK")){
              state = State.SENT_REPLCONF_CAPA;
              // Attach the response buffer to the key and switch to write mode
              key.attach(responseBuffer);
              key.interestOps(SelectionKey.OP_WRITE);
            } else if(state == State.SENT_REPLCONF_CAPA && response.equals("+OK")){
              state = State.COMPLETE;
            }
            
          }
        }else if(key.isWritable()){
          SocketChannel socketChannel = (SocketChannel) key.channel();
          ByteBuffer buffer = (ByteBuffer) key.attachment();
          
          if(buffer != null){
            buffer.flip();
            while(buffer.hasRemaining()){
              socketChannel.write(buffer);
            }
          }
          buffer.clear();
          key.attach(null);
          key.interestOps(SelectionKey.OP_READ);
        }
        iterator.remove();
      }
    }
  }

  public static int getPort(){
    return port;
  }

  private static void processArguments(String[] args){
    if(args.length >= 2){
      for(int i = 0; i < args.length; i=i+2){
        if(args[i].equalsIgnoreCase("--port"))  port = Integer.valueOf(args[i+1]);
        else if(args[i].equalsIgnoreCase("--replicaof")){
          mHostAndmPort = args[i+1].split(" ");
          role = "slave";
        }
      }
    }
  }

}
