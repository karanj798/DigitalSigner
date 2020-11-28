package client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

public class Client {
    public static void main(String[] args) {
        Registry registry = null;
        client.CommonService obj;

        try {
            registry = LocateRegistry.getRegistry("localhost");
            obj = (client.CommonService) registry.lookup("MasterNode");
            System.out.println(Arrays.toString(registry.list()));
            handleInputs(obj);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            try {
                System.out.println(Arrays.toString(registry.list()));
                obj = (client.CommonService) registry.lookup("BackupNode");
                handleInputs(obj);
            } catch (RemoteException e1) {
                e1.printStackTrace();
            } catch (NotBoundException e1) {
                System.out.println("All servers are down");
            }
        }
    }
    public static void handleInputs(client.CommonService obj) throws RemoteException {
        System.out.println(obj.getMessage());
    }
}
