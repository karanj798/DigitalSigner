package client;

import common.CommonService;
import common.model.Request;

import java.awt.*;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Client {
    public static void main(String[] args) {
        // Run using: java -cp ".;out/production/DistributedSystemsProject/" client.Client
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
                    System.out.print("Which users need to sign (separated by comma): ");
                    List<String> userNameList = Arrays.stream(in.nextLine().split(",")).collect(Collectors.toList());
                    File file = new File("resources/" + profileName + "/" + fileName);
                    String uuid = obj.request(new Request(fileName, userNameList), Files.readAllBytes(file.toPath()));
                    System.out.print("Check status of your transaction (Y/N): ");
                    if (in.nextLine().equals("Y")) {
                        if (obj.isFinished(uuid)) {
                            byte[] fileData  = obj.downloadFile(fileName.replaceAll(".pdf", "") + "_signed.pdf");
                            File signedFile = new File("resources/" + profileName + "/" + fileName.replaceAll(".pdf", "") + "_signed.pdf");
                            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(signedFile));
                            output.write(fileData,0,fileData.length);
                            output.flush();
                            output.close();
                            System.out.println(signedFile + " has been downloaded, opening it now ...");
                            Desktop.getDesktop().open(signedFile);
                        }
                    }

                } catch (IOException e) { e.printStackTrace(); }
            } else if (response.equals("2")) {
                List<String> files = obj.getFilesForSigning(profileName);
                System.out.println("Files to be signed: " + files);
                System.out.print("Which files to sign (* = all): ");
                String fileToSignInput = in.nextLine();
                if (fileToSignInput.equals("*")) {
                    for (String f : files) {
                        byte[] fileBytes = obj.downloadFile(f);
                        CryptoSign sign = new CryptoSign();
                        sign.signDocument(fileBytes, profile.getPrivateKey());
                        byte[] signedFileBytes = sign.getSignedDocument();
                        obj.verifySignature(profileName, fileBytes, signedFileBytes, f);
                    }
                }
            }
        }
    }
}
