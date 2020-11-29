package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CommonService extends Remote {
    String getMessage() throws RemoteException;

    void uploadFile(String fileName, byte[] fileContent) throws RemoteException;
}
