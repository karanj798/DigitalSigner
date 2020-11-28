package server;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class BackupNode {
    public static void main(String[] args) throws RemoteException, NotBoundException {

        String registryName = "BackupNode";
        CommonService obj = new RemoteObj();
        CommonService stub = (CommonService) UnicastRemoteObject.exportObject(obj, 0);
        Registry registry = LocateRegistry.getRegistry();
        registry.rebind(registryName, stub);

        // Handle the shutdown of the program in a graceful manner.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                registry.unbind(registryName);
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        }));

        //startReplicaService();
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
