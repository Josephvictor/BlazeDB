import java.util.HashMap;
import java.util.Map;

public class Storage {
    
    private static Map<String, String> strings = new HashMap<>();

    public static void addKeyValue(String key, String value){
        strings.put(key, value);
    }

    public static String getValue(String key){
        if(!strings.containsKey(key))   return "";
        return strings.get(key);
    }
}
