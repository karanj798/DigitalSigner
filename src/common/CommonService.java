package common;

import common.model.Request;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface CommonService extends Remote {
    String getMessage() throws RemoteException;
    void insertKey(String userName, String publicKey) throws RemoteException;
    void uploadFile(String fileName, byte[] fileContent) throws RemoteException;
    void signDocument(String fileName) throws  RemoteException;
    String request(Request request, byte[] fileContent) throws RemoteException;
    List<String> getFilesForSigning(String userName) throws RemoteException;
}
