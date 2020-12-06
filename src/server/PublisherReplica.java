package server;

import org.zeromq.*;

/**
 * This class is a handles the PDF file Publication between MasterNode and BackupNode through ZeroMQ's Pub/Sub.
 */
public class PublisherReplica extends Thread{
    private ZContext context;
    private ZMQ.Socket publisher;
    private String connection;
    private String nodeType;
    private int i = 1;

    /**
     * Constructor of this class, initializes instance variables.
     * @param connection TCP and Port of the ZMQ Pub/Sub
     * @param nodeType Type of Server
     */
    public PublisherReplica(String connection, String nodeType){
        this.connection = connection;
        this.nodeType = nodeType;
        context = new ZContext();
    }

    /**
     * Run subscribing to publishers.
     */
    @Override
    public void run(){
        startSub();
    }

    /**
     * Handles ZeroMQ Subscriptions by waiting for Publications.
     */
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

    /**
     * Publishes {@code byte[]} representing the file's bytes.
     * @param fileName Name of the file.
     * @param fileContent {@code byte[]} content of the file.
     */
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

    /**
     * Opens ZeroMQ connection and binds the Socket to specific host and port.
     */
    public void open() {
        publisher = context.createSocket(SocketType.PUB);
        publisher.bind(connection);
    }

    /**
     * Closes ZeroMQ connection.
     */
    public void close() {
        publisher.close();
        context.close();
    }
}
