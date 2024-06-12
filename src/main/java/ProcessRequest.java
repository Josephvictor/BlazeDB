import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class ProcessRequest {
    
    public static List<byte[]> process(List<String> parsedElements){
        List<byte[]> responseList = new ArrayList<>();
        
        String command = parsedElements.get(0);

        if(command.equalsIgnoreCase("PING")){
            byte[] rBytes = ResponseEncoder.SimpleEncoder("PONG").getBytes();
            responseList.add(rBytes);

        } else if(command.equalsIgnoreCase("ECHO")){
            byte[] rBytes = ResponseEncoder.BulkEncoder(parsedElements.get(1)).getBytes();
            responseList.add(rBytes);

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
                byte[] rBytes = ResponseEncoder.SimpleEncoder("OK").getBytes();
                responseList.add(rBytes);
            }else{
                long ttl = Long.parseLong(parsedElements.get(4));
                long expirationTime = System.currentTimeMillis() + ttl;
                Storage.addKeyValue(key, value, expirationTime);
                byte[] rBytes = ResponseEncoder.SimpleEncoder("OK").getBytes();
                responseList.add(rBytes);
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

            if(value.equals("-1")){
                byte[] rBytes = (ResponseEncoder.NullBulkString()).getBytes();
                responseList.add(rBytes);
            }
            else {
                byte[] rBytes = ResponseEncoder.BulkEncoder(value).getBytes();
                responseList.add(rBytes);
            }
        } else if(command.equalsIgnoreCase("INFO")){
            StringBuilder str = new StringBuilder();
            Map<String, String> ServerInfo = Storage.getServerInfo();

            for(Map.Entry<String, String> entrySet : ServerInfo.entrySet()){
                String value = entrySet.getKey()+":"+entrySet.getValue();
                str.append(value);
            }
            byte[] rBytes = ResponseEncoder.BulkEncoder(str.toString()).getBytes();
            responseList.add(rBytes);
        } else if(command.equalsIgnoreCase("PONG")){
            byte[] rBytes = ResponseEncoder.ArraysEncoder("REPLCONF", "listening-port", 
            String.valueOf(Main.getPort())).getBytes();
            responseList.add(rBytes);
        } else if(command.equalsIgnoreCase("OK")){
            if(parsedElements.get(1).equals(State.SENT_REPLCONF_PORT.toString())){
                byte[] rBytes = ResponseEncoder.ArraysEncoder("REPLCONF", "capa", "psync2").getBytes();
                responseList.add(rBytes);
            }
            else if(parsedElements.get(1).equals(State.SENT_REPLCONF_CAPA.toString())){
                byte[] rBytes = ResponseEncoder.ArraysEncoder("PSYNC", "?", "-1").getBytes();
                responseList.add(rBytes);
            }
            else
                return null;   
        } else if(command.equalsIgnoreCase("REPLCONF") || command.equalsIgnoreCase("FULLRESYNC")){
            byte[] rBytes = ResponseEncoder.SimpleEncoder("OK").getBytes();
            responseList.add(rBytes);
        } else if(command.equalsIgnoreCase("PSYNC")){
            // String replID = parsedElements.get(1);
            // String replOffset = parsedElements.get(2);

            String masterReplId = Main.getMasterReplId();
            String masterReplOffset = Main.getMasterReplOffset();
            byte[] rBytes = ResponseEncoder.
            SimpleEncoder(String.format("FULLRESYNC %s %s", 
                                    masterReplId, masterReplOffset)).getBytes();
            responseList.add(rBytes);
            
            String rdbFile = "UkVESVMwMDEx+glyZWRpcy12ZXIFNy4yLjD6CnJlZGlzLWJpdHPAQPoFY3RpbWXCbQi8ZfoIdXNlZC1tZW3CsMQQAPoIYW9mLWJhc2XAAP/wbjv+wP9aog==";

            rBytes = Base64.getDecoder().decode(rdbFile);
            String fileLength = String.format("$%d\r\n", rBytes.length);   
            responseList.add(fileLength.getBytes());
            responseList.add(rBytes);
            
        }
        
        return responseList;
    }
}
