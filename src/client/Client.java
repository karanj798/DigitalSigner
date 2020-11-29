package client;

import common.CommonService;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {

        try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            CommonService obj = null;

            if (Arrays.asList(registry.list()).contains("MasterNode")) {
                System.out.println("[MESSAGE]: MasterNode is alive");
                obj = (CommonService) registry.lookup("MasterNode");
            } else if (Arrays.asList(registry.list()).contains("BackupNode")) {
                System.out.println("[MESSAGE]: BackupNode is alive");
                obj = (CommonService) registry.lookup("BackupNode");
            } else {
                System.out.println("[ERROR]: All servers are down...");
                System.exit(0);
            }

            handleInputs(obj);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    public static void handleInputs(CommonService obj) throws RemoteException {
        Scanner in = new Scanner(System.in);
        System.out.print("Enter client name: ");

        Profile profile = new Profile(in.nextLine());
        profile.generatePrivatePublicKeysPair();
        //System.out.println(profile.getPublicKeyAsString());
        obj.insertKey(profile.name, profile.getPublicKeyAsString());
    }
}
