package server;

import org.zeromq.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class PublisherReplica extends Thread{
    private ZContext context;
    private ZMQ.Socket publisher;
    private String connection;
    private int i = 1;

    public PublisherReplica(String connection){
        this.connection = connection;
        context = new ZContext();
    }

    @Override
    public void run(){
        this.startSub();
    }

    public void startSub() {
        open();
        //while (!Thread.currentThread().isInterrupted()) {
        //publisher.send("update" + i++);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //}
        //close();
    }

    public void publishNewFile(String fileName, byte[] fileContent) throws IOException {
        ZMsg outMsg = new ZMsg();
        outMsg.add(new ZFrame(fileName));
        outMsg.add(new ZFrame(fileContent));

        outMsg.send(publisher);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
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
