public class ResponseEncoder {

    private static final String SimpleStrings = "+";
    private static final String BulkStrings = "$";
    private static final String Cr = "\r";
    private static final String Lf = "\n";

    public static String BulkEncoder(String message){
        int length = message.length();
        String response = String.format("%s%d%s%s%s%s%s",BulkStrings,length,Cr,Lf,message,Cr,Lf);
        return response;
    }
}
