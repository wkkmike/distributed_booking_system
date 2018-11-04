package MiddlewareServer.TranscationManager;

import java.rmi.RemoteException;
import java.util.*;

public class TransactionManager {
    private int xid;
    List<Transaction> transactionIdList;

    TransactionManager(){
        xid = 0;
        transactionIdList = new ArrayList<Transaction>();
    }

    //assign xid upon request
    public synchronized int start() throws RemoteException{
        Transaction t = new Transaction(xid);
        transactionIdList.add(t);
        xid ++;
        return (xid - 1);
    }

    public boolean commit(int transactionId) throws RemoteException,
            TranscationAbortedException, InvalidTransactionException{
        return false;
    }

    public void abort(int transactionId) throws RemoteException,
            InvalidTransactionException{

    }

    public boolean shutdown() throws RemoteException{
        return false;
    }
}
