import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProcessRequest {
    
    public static List<String> process(List<String> parsedElements){
        List<String> responseList = new ArrayList<>();
        
        String command = parsedElements.get(0);

        if(command.equalsIgnoreCase("PING")){
            responseList.add(ResponseEncoder.SimpleEncoder("PONG"));

        } else if(command.equalsIgnoreCase("ECHO")){
            responseList.add(ResponseEncoder.BulkEncoder(parsedElements.get(1)));

        } else if(command.equalsIgnoreCase("SET")){
            
            if(parsedElements.size() < 3){
                //throw error
                System.err.println("[ProcessRequest] Syntax error");
                return null;
            }
            String key = parsedElements.get(1);
            String value = parsedElements.get(2);

            if(parsedElements.size() == 3){
                Storage.addKeyValue(key, value, 0L);
                responseList.add(ResponseEncoder.SimpleEncoder("OK"));
            }else{
                //String arg1 = parsedElements.get(3);
                long ttl = Long.parseLong(parsedElements.get(4));
                long expirationTime = System.currentTimeMillis() + ttl;
                Storage.addKeyValue(key, value, expirationTime);
                responseList.add(ResponseEncoder.SimpleEncoder("OK"));
            }
        } else if(command.equalsIgnoreCase("GET")){
            if(parsedElements.size() != 2){
                //throw error
                System.err.println("[ProcessRequest] Wrong number of arguments for GET command");
                return null;
            }

            String key = parsedElements.get(1);
            long currentTimeMillis = System.currentTimeMillis();
            String value = Storage.getValue(key, currentTimeMillis);

            if(value.equals("-1"))  
                responseList.add(ResponseEncoder.NullBulkString());
            else 
                responseList.add(ResponseEncoder.BulkEncoder(value));
        } else if(command.equalsIgnoreCase("INFO")){
            StringBuilder string = new StringBuilder();
            Map<String, String> ServerInfo = Storage.getServerInfo();

            for(Map.Entry<String, String> entrySet : ServerInfo.entrySet()){
                String value = entrySet.getKey()+":"+entrySet.getValue();
                string.append(value);
                responseList.add(ResponseEncoder.BulkEncoder(string.toString()));
            }
        } else if(command.equalsIgnoreCase("PONG")){
            responseList.add(ResponseEncoder.ArraysEncoder("REPLCONF", "listening-port", 
                                String.valueOf(Main.getPort())));
        } else if(command.equalsIgnoreCase("OK")){
            if(parsedElements.get(1).equals(State.SENT_REPLCONF_PORT.toString()))
            responseList.add(ResponseEncoder.ArraysEncoder("REPLCONF", "capa", "psync2"));
            else if(parsedElements.get(1).equals(State.SENT_REPLCONF_CAPA.toString()))
            responseList.add(ResponseEncoder.ArraysEncoder("PSYNC", "?", "-1"));
            else
                return null;   
        } else if(command.equalsIgnoreCase("REPLCONF") || command.equalsIgnoreCase("FULLRESYNC")){
            responseList.add(ResponseEncoder.SimpleEncoder("OK"));
        } else if(command.equalsIgnoreCase("PSYNC")){
            String replID = parsedElements.get(1);
            String replOffset = parsedElements.get(2);

            String masterReplId = Main.getMasterReplId();
            String masterReplOffset = Main.getMasterReplOffset();
            responseList.add(ResponseEncoder.
                SimpleEncoder(String.format("FULLRESYNC %s %s", 
                                        masterReplId, masterReplOffset)));
            
            responseList.add("524544495330303131fa0972656469732d76657205372e322e30fa0a72656469732d62697473c040fa056374696d65c26d08bc65fa08757365642d6d656dc2b0c41000fa08616f662d62617365c000fff06e3bfec0ff5aa2");

        }
        
        return responseList;
    }
}
