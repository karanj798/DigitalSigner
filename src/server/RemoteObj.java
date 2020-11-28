package server;

import common.CommonService;

import java.rmi.RemoteException;

public class RemoteObj implements CommonService {

    @Override
    public String getMessage() throws RemoteException {
        return "Hello Client!";
    }
}