package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CommonService extends Remote {
    String getMessage() throws RemoteException;
    void insertKey(String userName, String publicKey) throws RemoteException;
    void uploadFile(String fileName, byte[] fileContent) throws RemoteException;
    void signDocument(String fileName) throws  RemoteException;
    void request(String fileName, byte[] fileContent, String[] signerList) throws RemoteException;
}
