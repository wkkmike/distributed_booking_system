package MiddlewareServer.TransactionManager;

import MiddlewareServer.Common.ResourceManager;
import Server.Common.Trace;

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
        Transaction transaction = transactionList.get(transactionId);
        if(transaction == null)
            throw new InvalidTransactionException(xid, "no such transaction");
        return false;
    }

    public boolean abort(int transactionId) throws RemoteException,
            InvalidTransactionException {
        Transaction transaction = transactionList.get(transactionId);
        if (transaction == null)
            throw new InvalidTransactionException(xid, "no such transaction");
        if(transaction.abort(middleware)) {
            transactionList.remove(transactionId);
            return true;
        }
        return false;
    }

    public boolean shutdown() throws RemoteException{
        return false;
    }
}
