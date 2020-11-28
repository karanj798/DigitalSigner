package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CommonService extends Remote {
    String getMessage() throws RemoteException;
}
