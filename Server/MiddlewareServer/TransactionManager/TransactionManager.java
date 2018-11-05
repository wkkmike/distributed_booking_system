package MiddlewareServer.TransactionManager;

import MiddlewareServer.Common.ResourceManager;

import java.rmi.RemoteException;
import java.util.*;

public class TransactionManager {
    private int xid;
    HashMap<Integer, Transaction> transactionList;
    private ResourceManager middleware;

    public TransactionManager(){
        xid = 1;
        transactionList = new HashMap<Integer, Transaction>();
    }

    public TransactionManager(ResourceManager middleware){
        xid = 1;
        transactionList = new HashMap<Integer, Transaction>();
        this.middleware = middleware;
    }

    //assign xid upon request
    public synchronized int start() throws RemoteException{
        Transaction t = new Transaction(xid);
        transactionList.put(xid, t);
        xid ++;
        return (xid - 1);
    }

    public boolean commit(int transactionId) throws RemoteException,
            TranscationAbortedException, InvalidTransactionException{
        return false;
    }

    public void abort(int transactionId) throws RemoteException,
            InvalidTransactionException {
        Transaction transaction = transactionList.get(transactionId);
        if (transaction == null)
            throw new InvalidTransactionException(xid, "no such transaction");

    }
    public boolean shutdown() throws RemoteException{
        return false;
    }
}
