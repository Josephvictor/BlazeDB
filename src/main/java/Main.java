import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Main {
  public static void main(String[] args) throws IOException{

    int port = 6379;
    if(args.length == 2){
      if(args[0].equalsIgnoreCase("--port"))  port = Integer.valueOf(args[1]);
    }

    Selector selector = Selector.open();
    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.bind(new InetSocketAddress("localhost", port));
    serverSocketChannel.configureBlocking(false);
    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

    System.out.println("***Server started***");
    Storage.addServerInfo("role", "master");

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
          ByteBuffer buffer = ByteBuffer.allocate(256);
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
            clientChannel.write(buffer);
            buffer.clear();
          }
        }
        iterator.remove();
      }
    }
  }
}
