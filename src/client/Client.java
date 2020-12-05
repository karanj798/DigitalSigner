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
            handleInputs(obj);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    public static void handleInputs(CommonService obj) throws RemoteException {
        Scanner in = new Scanner(System.in);
        System.out.print("Enter client name: ");
        String profileName = in.nextLine();

        Profile profile = new Profile(profileName);
        profile.generatePrivatePublicKeysPair();
        obj.insertKey(profile.name, profile.getPublicKeyAsString());
        System.out.print("Enter selection (1=Upload File | 2=Wait for Signing File): ");

        while (in.hasNextLine()) {
            String response = in.nextLine();
            if (response.equals("1")) {
                try {
                    System.out.print("Enter file name: ");
                    String fileName = in.nextLine();
                    System.out.print("Which users need to sign (seperated by comma): ");
                    String[] userNameList = in.nextLine().split(",");
                    File file = new File("resources/" + profileName + "/" + fileName);

                    obj.request(file.getName(), Files.readAllBytes(file.toPath()), userNameList);
                } catch (IOException e) { e.printStackTrace(); }
            } else if (response.equals("2")) {

            }
        }
    }
}
