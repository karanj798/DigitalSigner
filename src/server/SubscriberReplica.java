package server;

import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

public class SubscriberReplica {
    private ZContext context;
    private ZMQ.Socket subscriber;
    private String connection;
    private int i = 1;

    public SubscriberReplica(String connection){
        this.connection = connection;
        this.context = new ZContext();
    }

    public void start() {
        open();

        subscriber.subscribe("update".getBytes(ZMQ.CHARSET));

        // ZMQ.Poller poller = ZMQ.Poller(1);
        // poller.register(subscriber, ZMQ.Poller.POLLIN);
        while (!Thread.currentThread().isInterrupted()) {
            // poller.poll(100);
            // if (poller.pollin(0)) {
            //     String content = subscriber.recvStr();
            //     System.out.println(content);
            // }
        }

        close();
    }

    public void open() {
        subscriber = context.createSocket(SocketType.SUB);
        subscriber.connect(connection);
    }

    public void close() {
        subscriber.close();
        context.close();
    }
}
