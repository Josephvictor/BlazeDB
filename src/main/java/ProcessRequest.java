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
            
            if(parsedElements.size() < 3){
                //throw error
                System.err.println("[ProcessRequest] Syntax error");
                return "";
            }
            String key = parsedElements.get(1);
            String value = parsedElements.get(2);

            if(parsedElements.size() == 3){
                Storage.addKeyValue(key, value, 0L);
                response = ResponseEncoder.SimpleEncoder("OK");
            }else{
                String arg1 = parsedElements.get(4);
                long ttl = Long.parseLong(parsedElements.get(5));
                long expirationTime = System.currentTimeMillis() + ttl;
                Storage.addKeyValue(key, value, expirationTime);
                response = ResponseEncoder.SimpleEncoder("OK");
            }
        } else if(command.equalsIgnoreCase("GET")){
            if(parsedElements.size() != 2){
                //throw error
                System.err.println("[ProcessRequest] Wrong number of arguments for GET command");
                return "";
            }

            String key = parsedElements.get(1);
            long currentTimeMillis = System.currentTimeMillis();
            String value = Storage.getValue(key, currentTimeMillis);

            if(value.equals("-1"))  response = ResponseEncoder.NullBulkString();
            else response = ResponseEncoder.BulkEncoder(value);
        }

        return response;
    }
}
