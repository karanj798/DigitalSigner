package client;

import common.CommonService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

            System.out.println(Arrays.asList(registry.list()).toString());
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

            Scanner in = new Scanner(System.in);
            System.out.println("Upload file? [1=yes]");
            int num = in.nextInt();
            if(num == 1){
                File file = new File("resources\\test.txt");
                obj.uploadFile(file.getName(), Files.readAllBytes(file.toPath()));
            }

            handleInputs(obj);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
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
