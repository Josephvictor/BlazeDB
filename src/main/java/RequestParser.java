import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RequestParser {
    
    public static List<String> parse(String request){
        String[] parts = request.split("\r\n");
        System.out.println("[parts] "+Arrays.toString(parts));

        List<String> parsedElements = new ArrayList<>();

        int index = 0;
        if(parts[index].charAt(0) == '*'){
            int numOfElements = Integer.parseInt(parts[index].substring(1));
            index++;
            
            for(int i = 0; i < numOfElements; i++){
                if(parts[index].charAt(0) == '$'){
                    System.out.println(parts[index]);

                    int length = Integer.parseInt(parts[index].substring(1));
                    index++;
                    String element = parts[index];
                    System.out.println(element);
                    if(element.length() == length){
                        parsedElements.add(element);
                    }
                } else {
                    System.err.println("[RequestParser] Redis protocol not followed");
                    return null;
                }
                index++;
            }
        }else if(parts[0].charAt(0) == '+'){
        //     parsedElements.add(parts[index].substring(1));
            
        //     for(int i = 1; i < parts.length; i++){
        //         parsedElements.add(parts[i]);
        //         if(i==2)    break;
        //     }
            String simpleStr = parts[0].substring(1);
            parts = simpleStr.split(" ");
            System.out.println("[parts] "+Arrays.toString(parts));

            for(String s : parts){
                parsedElements.add(s);
            }
         }
        else{
            parsedElements.add(parts[index].substring(1));
        }

        return parsedElements;
    }
}
