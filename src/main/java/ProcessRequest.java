import java.util.List;

public class ProcessRequest {
    
    public static String process(List<String> parsedElements){
        String response = "";
        String command = parsedElements.get(0);

        if(command.equalsIgnoreCase("PING")){
            response = ResponseEncoder.SimpleEncoder("PONG");

        } else if(command.equalsIgnoreCase("ECHO")){
            response = ResponseEncoder.BulkEncoder(parsedElements.get(1));

        } else if(command.equalsIgnoreCase("SET")){
            
            if(parsedElements.size() != 3){
                //throw error
                System.err.println("[ProcessRequest] Syntax error");
                return "";
            }

            Storage.addKeyValue(parsedElements.get(1), parsedElements.get(2));
            response = ResponseEncoder.SimpleEncoder("OK");

        } else if(command.equalsIgnoreCase("GET")){
            if(parsedElements.size() != 2){
                //throw error
                System.err.println("[ProcessRequest] Wrong number of arguments for GET command");
                return "";
            }

            String value = Storage.getValue(parsedElements.get(1));
            response = ResponseEncoder.BulkEncoder(value);
        }

        return response;
    }
}
