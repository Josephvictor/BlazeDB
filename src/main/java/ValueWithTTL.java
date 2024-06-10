public class ValueWithTTL {

    private final Object value;
    private final long expirationTime;

    public ValueWithTTL(Object value, long expirationTime){
        this.value = value;
        this.expirationTime = expirationTime;
    }

    public Object getValue(){
        return value;
    }

    public long getExpirationTime(){
        return expirationTime;
    }
}
