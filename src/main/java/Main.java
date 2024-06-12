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
  private static State state = State.INITIAL;

  public static void main(String[] args) throws IOException{

    processArguments(args);
    
    Storage.addServerInfo("role", role);
    Storage.addServerInfo("master_replid", master_replid);
    Storage.addServerInfo("master_repl_offset", master_repl_offset);
    System.out.println("[main] Server details added");

    Selector selector = Selector.open();
    //if(role.equalsIgnoreCase("master")){
      ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
      serverSocketChannel.bind(new InetSocketAddress("localhost", port));
      serverSocketChannel.configureBlocking(false);
      serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
      System.out.println("[main] ***Server started***"); 
    //}
    
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
      System.out.println("[main] ***Replica started***"); 
    }

    eventLoop(selector);
  }

  private static void eventLoop(Selector selector) throws IOException{
    
    while(true){
      int conns = selector.select();
      if(conns == 0)  continue;

      Set<SelectionKey> selectedKeys = selector.selectedKeys();
      Iterator<SelectionKey> iterator = selectedKeys.iterator();

      while(iterator.hasNext()){
        SelectionKey key = iterator.next();
        if(key.isAcceptable()){
          handleConnection(key, selector);
        }else if(key.isReadable()){
          handleRead(key);
        }else if(key.isWritable()){
          handleWrite(key);
        }
        iterator.remove();
      }
    }
  }

  public static void handleConnection(SelectionKey key, Selector selector) throws IOException{
    ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
          SocketChannel clientChannel = serverChannel.accept();
          
          clientChannel.configureBlocking(false);
          clientChannel.register(selector, SelectionKey.OP_READ);
          System.out.println("Connection accepted: "+ clientChannel.getRemoteAddress());
  }

  public static void handleRead(SelectionKey key) throws IOException{

    SocketChannel socketChannel = (SocketChannel) key.channel();
    ByteBuffer buffer = ByteBuffer.allocate(254);
    buffer.clear();
    int byteRead = socketChannel.read(buffer);
    if(byteRead == -1){
      socketChannel.close();
    }else {
      buffer.flip(); // Flip the buffer before reading
      byte[] data = new byte[buffer.remaining()];
      buffer.get(data);

      String message = new String(data).trim();
      System.out.println("[main][Received message] "+ message);
      
      List<String> parsedElements = RequestParser.parse(message);
      System.out.println("[main][parsedElements] "+parsedElements);
      if(parsedElements.get(0).equals("OK")){
        parsedElements.add(state.toString());
      }

      String response = ProcessRequest.process(parsedElements);
      
      ByteBuffer responseBuffer = ByteBuffer.allocate(254);
      responseBuffer.put(response.getBytes());

      // System.out.println("currentState: "+state);
      // System.out.println("[command] "+parsedElements.get(0));

      if(state == State.SENT_PSYNC){
        state = State.FULLRESYNC;
      } else{
        // Attach the response buffer to the key and switch to write mode
        key.attach(responseBuffer);
        key.interestOps(SelectionKey.OP_WRITE);
        System.out.println("[main][Response] "+response+" --State: "+state);

        if(state == State.SENT_PING && parsedElements.get(0).equals("PONG")){
          state = State.SENT_REPLCONF_PORT;
        }else if(state == State.SENT_REPLCONF_PORT && parsedElements.get(0).equals("OK")){
          state = State.SENT_REPLCONF_CAPA;
        } else if(state == State.SENT_REPLCONF_CAPA && parsedElements.get(0).equals("OK")){
          state = State.SENT_PSYNC;
        }
        System.err.println("Changed to state: "+state);
      }
    }
  }

  public static void handleWrite(SelectionKey key) throws IOException{
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

  public static int getPort(){
    return port;
  }

  public static String getMasterReplId(){
    return master_replid;
  }

  public static String getMasterReplOffset(){
    return master_repl_offset;
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
      System.out.println("[Master][host] and [port] "+Arrays.toString(mHostAndmPort));
    }
  }

}
