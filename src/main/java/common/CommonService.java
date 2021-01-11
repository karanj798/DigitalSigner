package common;

import common.model.Request;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.PublicKey;
import java.util.List;

/**
 * RMI interface that used by Client/Server.
 */
public interface CommonService extends Remote {
    void insertKey(String userName, String publicKey) throws RemoteException;
    String request(Request request, byte[] fileContent) throws RemoteException;
    List<String> getFilesForSigning(String userName) throws RemoteException;
    byte[] downloadFile(String fileName)  throws RemoteException;
    void verifySignature(String userName, byte[] originalDocument, byte[] signedDocument, String fileName) throws RemoteException;
    boolean isFinished(String uuid) throws RemoteException;
}
