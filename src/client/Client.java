package client;

import common.CommonService;
import common.model.Request;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Client {
    public static void main(String[] args) {
        // Run using: java -cp ".;out/production/DistributedSystemsProject/" client.Client
        try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            Registry backupRegistry = LocateRegistry.getRegistry("localhost", 45682);
            CommonService obj = null;
            String nodeName = null;

            //System.out.println(Arrays.asList(LocateRegistry.getRegistry("localhost").list()).toString());
            if (Arrays.asList(registry.list()).contains("MasterNode")) {
                System.out.println("[MESSAGE]: MasterNode is alive");
                obj = (CommonService) registry.lookup("MasterNode");
                nodeName = "MasterNode";
            } else if (Arrays.asList(backupRegistry.list()).contains("BackupNode")) {
                System.out.println("[MESSAGE]: BackupNode is alive");
                obj = (CommonService) backupRegistry.lookup("BackupNode");
                nodeName = "BackupNode";
            } else {
                System.out.println("[ERROR]: All servers are down...");
                System.exit(0);
            }
            handleInputs(obj, nodeName);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    public static void handleInputs(CommonService obj, String nodeName) throws RemoteException {
        Scanner in = new Scanner(System.in);
        System.out.print("Enter client name: ");
        String profileName = in.nextLine().trim();

        Profile profile = new Profile(profileName);
        profile.generatePrivatePublicKeysPair();
        try {
        obj.insertKey(profile.name, profile.getPublicKeyAsString());

        while (true) {
            System.out.print("Enter selection (1=Upload File | 2=Wait for Signing File | EXIT=to exit the prompt): ");
            String response = "";
            while (!response.equals("1") && !response.equals("2") && !response.equalsIgnoreCase("EXIT")) {
                response = in.nextLine().trim().trim();
                if (!response.equals("1") && !response.equals("2") && !response.equalsIgnoreCase("EXIT")) System.out.print("Enter either (1) or (2) or (EXIT)...");
            }

                if (response.equals("1")) {
                    handleFileInput(in, profileName, obj, nodeName);
                } else if (response.equals("2")) {
                    List<String> files = obj.getFilesForSigning(profileName);
                    String fileToSignInput = "";

                    if (!files.isEmpty()) {
                        System.out.println("\nFiles to be signed: " + files);
                        System.out.print("Which files to sign (* = all): ");

                        while ((!fileToSignInput.contains(".") && !fileToSignInput.equalsIgnoreCase("*")) || (fileToSignInput.contains(".") && !files.contains(fileToSignInput))) {
                            fileToSignInput = in.nextLine().trim();
                            if (!fileToSignInput.contains(".") && !fileToSignInput.equalsIgnoreCase("*"))
                                System.out.print("Please enter either (* = all) or a file name containing the extension...");
                            if (fileToSignInput.contains(".") && !files.contains(fileToSignInput))
                                System.out.print("File not found... please choose a file from list provided: ");
                        }

                        if (fileToSignInput.equalsIgnoreCase("*")) {
                            for (String f : files) {
                                handleFileToSign(obj, profile, profileName, f, in);
                            }
                        } else if (fileToSignInput.contains(".") && files.contains(fileToSignInput)) {
                            handleFileToSign(obj, profile, profileName, fileToSignInput, in);
                        }
                    } else {
                        System.out.println("No files available to sign... Going back to previous prompt.\n");
                    }
                } else {
                    System.out.println("Bye!");
                    break;
                }

            }
        } catch (ConnectException e) {
            try {
                if (nodeName.equals("MasterNode")) {
                    Registry registry = LocateRegistry.getRegistry("localhost", 45682);
                    // Failure on MasterNode
                    obj = (CommonService) registry.lookup("BackupNode");
                    handleInputs(obj, "BackupNode");
                } else if (nodeName.equals("BackupNode")) {
                    Registry registry = LocateRegistry.getRegistry("localhost");
                    obj = (CommonService) registry.lookup("MasterNode");
                    handleInputs(obj, "MasterNode");
                }
            } catch (NotBoundException notBoundException) {
                System.out.println("All servers are offline.");
            }
        }

        in.close();
    }

    private static void handleFileInput(Scanner in, String profileName, CommonService obj, String nodeName){
        try {
            System.out.print("Enter file name: ");
            String fileName = "";
            File file = new File("");
            while(!file.isFile()) {
                fileName = in.nextLine().trim();
                file = new File("resources/" + profileName + "/" + fileName);
                if(!file.isFile()) System.out.print("File does not exist... Please provide another file name: ");
            }

            System.out.print("\nWhich users need to sign (separated by comma): ");
            List<String> userNameList = Arrays.stream(in.nextLine().trim().split(",")).collect(Collectors.toList());

            String uuid = obj.request(new Request(fileName, userNameList), Files.readAllBytes(file.toPath()));
            System.out.print("Check status of your transaction (Y/N): ");
            String transactionInput = "";
            while(!transactionInput.equalsIgnoreCase("Y") && !transactionInput.equalsIgnoreCase("N")){
                transactionInput = in.nextLine().trim();
                if(!transactionInput.equalsIgnoreCase("Y") && !transactionInput.equalsIgnoreCase("N")) System.out.println("Please enter either (Y) or (N) for transaction...");
            }

            boolean waitForSignedFile = transactionInput.equalsIgnoreCase("Y");
            while(true){
                if (waitForSignedFile) {
                    if (obj.isFinished(uuid)) {
                        System.out.println("The File has been signed!");
                        byte[] fileData = obj.downloadFile(fileName.replaceAll(".pdf", "") + "_signed.pdf");
                        File signedFile = new File("resources/" + profileName + "/" + fileName.replaceAll(".pdf", "") + "_signed.pdf");
                        BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(signedFile));
                        output.write(fileData, 0, fileData.length);
                        output.flush();
                        output.close();
                        System.out.println(signedFile + " has been downloaded, opening it now ...\n");
                        Desktop.getDesktop().open(signedFile);

                        break;
                    } else {
                        System.out.print("Your file has not been signed yet... Do you still want to check the status of your transaction (Y/N): ");
                        transactionInput = "";
                        while(!transactionInput.equalsIgnoreCase("Y") && !transactionInput.equalsIgnoreCase("N")){
                            transactionInput = in.nextLine().trim();
                            if(!transactionInput.equalsIgnoreCase("Y") && !transactionInput.equalsIgnoreCase("N")) System.out.println("Please enter either (Y) or (N) for transaction...");
                        }

                        waitForSignedFile = transactionInput.equalsIgnoreCase("Y");
                    }
                } else { // if answer is N
                    System.out.println("ok bye...\n");
                    break;
                }
            }

        } catch (ConnectException e) {
            try {
                if (nodeName.equals("MasterNode")) {
                    Registry registry = LocateRegistry.getRegistry("localhost", 45682);
                    // Failure on MasterNode
                    obj = (CommonService) registry.lookup("BackupNode");
                    handleInputs(obj, "BackupNode");
                } else if (nodeName.equals("BackupNode")) {
                    Registry registry = LocateRegistry.getRegistry("localhost");
                    obj = (CommonService) registry.lookup("MasterNode");
                    handleInputs(obj, "MasterNode");
                }
            } catch (NotBoundException notBoundException) {
                System.out.println("All servers are offline.");
            } catch (RemoteException remoteException) {
                remoteException.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleFileToSign(CommonService obj, Profile profile, String profileName, String fileName, Scanner in) throws RemoteException {
        byte[] fileBytes = obj.downloadFile(fileName);
        File fileToSign = new File("resources/" + profileName + "/" + fileName.replaceAll(".pdf", "") + "_toSign.pdf");
        BufferedOutputStream output = null;
        try {
            output = new BufferedOutputStream(new FileOutputStream(fileToSign));
            output.write(fileBytes, 0, fileBytes.length);
            output.flush();
            output.close();

            System.out.println("\nOpening " + fileName + " to review...");
            Desktop.getDesktop().open(fileToSign);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.print("Do you want to sign this document? (Y/N): ");
        String toSignInput = "";
        while(!toSignInput.equalsIgnoreCase("Y") && !toSignInput.equalsIgnoreCase("N")){
            toSignInput = in.nextLine().trim();
            if(!toSignInput.equalsIgnoreCase("Y") && !toSignInput.equalsIgnoreCase("N")) System.out.println("Please enter either (Y) or (N) to sign the document...");
        }

        if(toSignInput.equalsIgnoreCase("Y")) {
            CryptoSign sign = new CryptoSign();
            sign.signDocument(fileBytes, profile.getPrivateKey());
            byte[] signedFileBytes = sign.getSignedDocument();
            obj.verifySignature(profileName, fileBytes, signedFileBytes, fileName);

        } // else delete and move on

        System.out.println("\nAttempting to remove the file " + fileToSign.getName());
        while(fileToSign.exists()){ // deleting local file copy
            if (fileToSign.delete()) {
                System.out.println("Removed file: " + fileToSign.getName() + "\n");
                break;
            } else {
                System.out.println("Unable to remove file: " + fileToSign.getName() + "... Try to exit out of the file viewer...");
            }

            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
