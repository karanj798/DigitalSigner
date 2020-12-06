package server;

import common.CommonService;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * This class is a Main RMI Server.
 */
public class MasterNode {
    public static void main(String[] args) throws IOException {
        if (System.getSecurityManager() == null)
            System.setSecurityManager(new SecurityManager());

        String registryName = "MasterNode";
        CommonService obj = new RemoteObj("tcp://*:5516","tcp://*:5517", "Master");
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
    }

}
