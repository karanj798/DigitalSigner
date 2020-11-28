import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

public class PublisherReplica{
    private ZContext context;
    private ZMQ.Socket publisher;
    private String connection;
    private int i = 1;

    public PublisherReplica(String connection){
        this.connection = connection;
        context = new ZContext();
    }

    public void start() {
        open();
        while (!Thread.currentThread().isInterrupted()) {
            publisher.send("update" + i++);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        close();
    }

    public void open() {
        publisher = context.createSocket(SocketType.PUB);
        publisher.bind(connection);
    }

    public void close() {
        publisher.close();
        context.close();
    }
}
