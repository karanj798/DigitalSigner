package main;

import client.Profile;
import common.CommonService;

import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * @author Karan
 * This class tests if the client can rollover to BackupNode server when the
 * MainNode has faced a crash.
 */
public class FailTest {
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            CommonService obj = (CommonService) registry.lookup("MasterNode");
            Profile profile = new Profile("tester");
            profile.generatePrivatePublicKeysPair();
            Thread.sleep(1000);
            obj.insertKey("tester: 123" , profile.getPublicKeyAsString());
        } catch (ConnectException e) {
            try {
                Registry registry = LocateRegistry.getRegistry("localhost");
                CommonService obj = (CommonService) registry.lookup("BackupNode");
                Profile profile = new Profile("tester");
                profile.generatePrivatePublicKeysPair();
                obj.insertKey("tester: 123", profile.getPublicKeyAsString());
                System.out.println("got here");
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } catch (RemoteException | NotBoundException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
