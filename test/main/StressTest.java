package main;

import client.Profile;
import common.CommonService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Timestamp;

/**
 * @author Karan
 * @version 1.0
 * This class tests how many requests the RMI Server can handle.
 */
public class StressTest extends Thread {
    public static void main(String[] args) {
        // Create 1000 threads for requesting Remote method concurrently.
        for (int i = 0; i < 1000; i++) {
            new StressTest().start();
            if (i % 100 == 0) {
                System.out.println("[" + new Timestamp(System.currentTimeMillis())  + "] Made: " + i + " request so far");
            }
        }
    }

    /**
     * This method invokes Remote object's method on a separate thread.
     */
    @Override
    public void run() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            CommonService obj = (CommonService) registry.lookup("MasterNode");
            Profile profile = new Profile("tester");
            profile.generatePrivatePublicKeysPair();
            obj.insertKey("tester: " + getId(), profile.getPublicKeyAsString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
