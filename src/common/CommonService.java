package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CommonService extends Remote {
    String getMessage() throws RemoteException;
    void insertKey(String userName, String publicKey) throws RemoteException;
}
