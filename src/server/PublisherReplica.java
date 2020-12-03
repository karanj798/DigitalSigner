package server;

import org.zeromq.*;

public class PublisherReplica extends Thread{
    private ZContext context;
    private ZMQ.Socket publisher;
    private String connection;
    private String nodeType;
    private int i = 1;

    public PublisherReplica(String connection, String nodeType){
        this.connection = connection;
        this.nodeType = nodeType;
        context = new ZContext();
    }

    @Override
    public void run(){
        startSub();
    }

    public void startSub() {
        this.open();

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.out.println("PubReplica " + this.nodeType + ": " + e.getMessage());
            }
        }

        if(Thread.currentThread().isInterrupted()){
            this.close();
        }
    }

    public void publishNewFile(String fileName, byte[] fileContent) {
        ZMsg outMsg = new ZMsg();
        outMsg.add(new ZFrame("replicateFile"));
        outMsg.add(new ZFrame(fileName));
        outMsg.add(new ZFrame(fileContent));

        outMsg.send(publisher);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            System.out.println("PubReplica " + this.nodeType + ": " + e.getMessage());
        }
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
