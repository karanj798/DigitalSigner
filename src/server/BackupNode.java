package server;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;

public class BackupNode {
    public static void main(String[] args) throws RemoteException, NotBoundException {
        Scanner in = new Scanner(System.in);

        String registryName = "BackupNode";
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

    public static void startReplicaService() {
        Thread pubReplicaThread = new Thread(new Runnable() {
            @Override
            public void run() {
                PublisherReplica pReplica = new PublisherReplica("tcp://*:5517");
                pReplica.start();
            }
        });

        Thread subReplicaThread = new Thread(new Runnable() {
            @Override
            public void run() {
                SubscriberReplica sReplica = new SubscriberReplica("tcp://*:5516");
                sReplica.start();
            }
        });

        pubReplicaThread.start();
        subReplicaThread.start();
    }
}
