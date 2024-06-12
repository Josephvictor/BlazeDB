public class ResponseEncoder {

    private static final String SimpleStrings = "+";
    private static final String BulkStrings = "$";
    private static final String Arrays = "*";
    private static final String Cr = "\r";
    private static final String Lf = "\n";

    public static String SimpleEncoder(String message){
        String response = String.format("%s%s%s%s", SimpleStrings,message,Cr,Lf);
        return response;
    }

    public static String BulkEncoder(String message){
        int length = message.length();
        String response = String.format("%s%d%s%s%s%s%s",BulkStrings,length,Cr,Lf,message,Cr,Lf);
        return response;
    }

    public static String NullBulkString(){
        return String.format("%s-1%s%s",BulkStrings,Cr,Lf);
    }

    public static String ArraysEncoder(String... message){
        
        int length = message.length;
        StringBuilder str = new StringBuilder();
        
        str.append(String.format("%s%d%s%s",Arrays,length,Cr,Lf));
        
        for(String msg : message){
            str.append(String.format("%s%d%s%s%s%s%s",BulkStrings,msg.length(),Cr,Lf,msg,Cr,Lf));
        }

        return str.toString();
    }

    public static String RdbFileEncoder(String message){
        return String.format("%s%d%s%s%s",BulkStrings,message.length(),Cr,Lf,message);
    }
}
