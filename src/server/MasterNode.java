import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.nio.ByteBuffer;

import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

public class MasterNode {
    public static void main(String[] args) throws RemoteException, NotBoundException {
        Scanner in = new Scanner(System.in);

        String registryName = "MasterNode";
        CommonService obj = new RemoteObj();
        CommonService stub = (CommonService) UnicastRemoteObject.exportObject(obj, 0);
        Registry registry = LocateRegistry.getRegistry();
        registry.rebind(registryName, stub);

        if (in.nextLine().equals("q")) {
            registry.unbind(registryName);
            System.exit(0);
        }

        //startReplicaService();

        in.close();
    }

    public static void startReplicaService(){
        Thread pubReplicaThread = new Thread(new Runnable() {
            @Override
            public void run() {
                PublisherReplica pReplica = new PublisherReplica("tcp://*:5516");
                pReplica.start();
            }
        });
    
        Thread subReplicaThread = new Thread(new Runnable(){
            @Override
            public void run() {
                SubscriberReplica sReplica = new SubscriberReplica("tcp://*:5517");
                sReplica.start();
            }
        }); 

        pubReplicaThread.start();
        subReplicaThread.start();
    }
}
