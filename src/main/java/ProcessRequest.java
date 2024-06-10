import java.util.List;

public class ProcessRequest {
    
    public static String process(List<String> parsedElements){
        String response = "";
        String command = parsedElements.get(0);

        if(command.equalsIgnoreCase("PING")){
            response = "+PONG\r\n";
        } else if(command.equalsIgnoreCase("ECHO")){
            response = ResponseEncoder.BulkEncoder(parsedElements.get(1));
        }

        return response;
    }
}
