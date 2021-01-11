package server;

import common.CommonService;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * This class is a Backup RMI Server.
 */
public class BackupNode {
    public static void main(String[] args) throws RemoteException {

        String registryName = "BackupNode";
        CommonService obj = new RemoteObj("tcp://*:5517", "tcp://*:5516", "Backup");

        CommonService stub = (CommonService) UnicastRemoteObject.exportObject(obj, 0);
        Registry registry = LocateRegistry.getRegistry(45682);

        registry.rebind(registryName, stub);

        // Handle the shutdown of the program in a graceful manner.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                registry.unbind(registryName);
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        }));

    }

}
