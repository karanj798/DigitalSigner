package server;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.nio.charset.StandardCharsets;

public class SubscriberReplica extends Thread{
    private ZContext context;
    private ZMQ.Socket subscriber;
    private String connection;
    private int i = 1;

    public SubscriberReplica(String connection){
        this.connection = connection;
        this.context = new ZContext();
    }

    @Override
    public void run(){
        this.startPub();
    }

    public void startPub() {
        open();

        subscriber.subscribe(ZMQ.SUBSCRIPTION_ALL);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                ZMsg inMsg = ZMsg.recvMsg(subscriber);
                String fileName = inMsg.pop().toString();
                byte[] fileContent = inMsg.pop().getData();

                System.out.println("fileName: " + fileName);
                System.out.println("fileContent: " + new String(fileContent, StandardCharsets.UTF_8));

                // save file here
            } finally {
                //nothing
            }
        }

        close();
    }

    public void recvNewFile(){}

    public void open() {
        subscriber = context.createSocket(SocketType.SUB);
        subscriber.connect(connection);
    }

    public void close() {
        subscriber.close();
        context.close();
    }
}
