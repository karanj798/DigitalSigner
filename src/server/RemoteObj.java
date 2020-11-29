package server;

import common.CommonService;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.rmi.RemoteException;

public class RemoteObj implements CommonService {
    PublisherReplica pReplica;
    SubscriberReplica sReplica;

    public RemoteObj (String pubAddr, String subAddr){
        if(pubAddr != null){
            this.pReplica = new PublisherReplica(pubAddr);
            pReplica.start();
        }

        if(subAddr != null){
            this.sReplica = new SubscriberReplica(subAddr);
            sReplica.start();
        }
    }

    @Override
    public String getMessage() throws RemoteException {
        return "Hello Client!";
    }

    @Override
    public void uploadFile(String fileName, byte[] fileContent) throws RemoteException {
        try {
            System.out.println("File : " + fileName + " is being recieved...");

            File file = new File(fileName);
            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(fileName));
            output.write(fileContent, 0, fileContent.length);
            output.flush();
            output.close();

            System.out.println("Successfully downloaded the file " + fileName + " from the Client");

            pReplica.publishNewFile(fileName, fileContent);
        } catch (Exception e) {
            System.out.println("FileImpl: " + e.getMessage());
            e.printStackTrace();
        }
    }
}