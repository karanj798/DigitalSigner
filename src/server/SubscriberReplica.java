package server;

import common.FileUtils;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class is a handles the PDF file Subscription between MasterNode and BackupNode through ZeroMQ's Pub/Sub.
 */
public class SubscriberReplica extends Thread{
    private ZContext context;
    private ZMQ.Socket subscriber;
    private String connection;
    private String nodeType;
    private FileUtils fileUtils;

    /**
     * Constructor of this class, initializes ZeroMQ connection.
     * @param connection TCP and Port of the ZMQ Pub/Sub
     * @param nodeType Type of Server
     */
    public SubscriberReplica(String connection, String nodeType){
        this.connection = connection;
        this.nodeType = nodeType;
        this.context = new ZContext();
        this.fileUtils = new FileUtils();
    }

    /**
     * Begins publishing to subscribers.
     */
    @Override
    public void run(){
        this.startPub();
    }

    /**
     * Handles ZeroMQ publications by constantly replicating the file every 100ms.
     */
    public void startPub() {
        this.open();

        subscriber.subscribe(ZMQ.SUBSCRIPTION_ALL);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                ZMsg inMsg = ZMsg.recvMsg(subscriber);
                if(inMsg.pop().toString().equals("replicateFile")){
                    String fileName = inMsg.pop().toString();
                    byte[] fileContent = inMsg.pop().getData();

                    System.out.println("File received... saving the file.");
                    saveRcvFile(fileName, fileContent); // save file to a physical location
                }

                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.out.println("PubReplica " + this.nodeType + ": " + e.getMessage());
            }
        }
        this.close();
    }

    /**
     * This method saves the file that was received.
     * @param fileName Name of the file.
     * @param fileContent {@code byte[]} representing file's content.
     */
    private void saveRcvFile(String fileName, byte[] fileContent){
        String directoryPath = fileUtils.getResourcesPath() + this.nodeType;

        File directory = new File(directoryPath);
        if (! directory.exists()) directory.mkdir();  //Create a directory if it does not exist

        try {
            File file = new File(directoryPath + "/" + fileName);
            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file.getAbsoluteFile()));
            output.write(fileContent, 0, fileContent.length);

            output.flush();
            output.close();
        } catch (IOException e) {
            System.out.println("SubReplica " + this.nodeType + ": " + e.getMessage());
        }
    }

    /**
     * Opens ZeroMQ connection for Pub/Sub.
     */
    public void open() {
        subscriber = context.createSocket(SocketType.SUB);
        subscriber.connect(connection);
    }

    /**
     * Closes ZeroMQ connection.
     */
    public void close() {
        subscriber.close();
        context.close();
    }
}
