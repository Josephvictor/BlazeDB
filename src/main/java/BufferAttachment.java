import java.nio.ByteBuffer;
import java.util.List;

public class BufferAttachment {
    private final List<ByteBuffer> buffers;

    public BufferAttachment(List<ByteBuffer> buffers) {
        this.buffers = buffers;
    }

    public List<ByteBuffer> getBuffers() {
        return buffers;
    } 
    
}
