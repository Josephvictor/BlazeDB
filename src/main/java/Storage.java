import java.util.HashMap;
import java.util.Map;

public class Storage {
    
    private static Map<String, ValueWithTTL> keyValueStore = new HashMap<>();

    public static void addKeyValue(String key, String value, long expirationTime){
        keyValueStore.put(key, new ValueWithTTL(value, expirationTime));
    }

    public static String getValue(String key, long currentTimeMillis){
        if(!keyValueStore.containsKey(key))   return "-1";
        //return keyValueStore.get(key);
        ValueWithTTL obj = keyValueStore.get(key);
        long expirationTime = obj.getExpirationTime();
        String value = "";

        if(expirationTime == 0L){
            value =  (String) obj.getValue();
        }else if(expirationTime >= currentTimeMillis) {
            value =  (String) obj.getValue();
        } else{
            keyValueStore.remove(key);
            value = "-1";
        }

        return value;
    }
}
