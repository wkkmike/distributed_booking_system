package MiddlewareServer.Interface;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ITransactionManager extends Remote {
    public boolean isCommit(int xid) throws RemoteException;

    public void abort(int xid) throws RemoteException;
}
