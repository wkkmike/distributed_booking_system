package MiddlewareServer.TransactionManager;

import MiddlewareServer.Common.ResourceManager;
import MiddlewareServer.LockManager.LockManager;
import Server.Common.Trace;

import java.rmi.RemoteException;
import java.util.*;

public class TransactionManager {
    private int xid;
    HashMap<Integer, Transaction> transactionList;
    private ResourceManager middleware;
    private LockManager lm;

    public TransactionManager(){
        xid = 1;
        transactionList = new HashMap<Integer, Transaction>();
    }

    public TransactionManager(ResourceManager middleware, LockManager lm){
        xid = 1;
        transactionList = new HashMap<Integer, Transaction>();
        this.middleware = middleware;
        this.lm = lm;
    }

    //assign xid upon request
    public synchronized int start() throws RemoteException{
        Transaction t = new Transaction(xid, middleware, lm);
        transactionList.put(xid, t);
        xid ++;
        t.transactionSuspend();
        return (xid - 1);
    }

    public boolean commit(int transactionId) throws RemoteException,
            TranscationAbortedException, InvalidTransactionException{
        Transaction transaction = transactionList.get(transactionId);
        if(transaction == null)
            throw new InvalidTransactionException(xid, "no such transaction");
        if(transaction.commit()){
            transactionList.remove(transactionId);
            return true;
        }
        return false;
    }

    public boolean transactionInvoke(int transactionId) throws InvalidTransactionException, TranscationAbortedException{
        Transaction transaction = transactionList.get(transactionId);
        if(transaction == null)
            throw new InvalidTransactionException(transactionId, "no such transaction");
        if (!transaction.transactionInvoke()) {
            transactionList.remove(transactionId);
            return false;
        }
        return true;
    }

    public void transactionSuspend(int transactionId){
        Transaction transaction = transactionList.get(transactionId);
        if(transaction == null)
            return;
        transaction.transactionSuspend();
    }

    public boolean abort(int transactionId) throws RemoteException,
            InvalidTransactionException {
        Transaction transaction = transactionList.get(transactionId);
        if (transaction == null)
            throw new InvalidTransactionException(xid, "no such transaction");
        if(transaction.abort()) {
            transactionList.remove(transactionId);
            return true;
        }
        return false;
    }

    public boolean shutdown() throws RemoteException{
        return false;
    }

    public void addRM(int xid, Transaction.RM rm) throws InvalidTransactionException{
        Transaction transaction = transactionList.get(xid);
        if(transaction == null)
            throw new InvalidTransactionException(xid, "no such transaction");
        transaction.addRMtoRMList(rm);
        transactionList.put(xid, transaction);
    }

    public void deleteRM(int xid, Transaction.RM rm) throws InvalidTransactionException{
        Transaction transaction = transactionList.get(xid);
        if(transaction == null)
            throw new InvalidTransactionException(xid, "no such transaction");
        transaction.removeRMfromRMList(rm);
        transactionList.put(xid, transaction);
    }

    public void addUndoOperation(int xid, undoOperation ops) throws InvalidTransactionException{
        Transaction transaction = transactionList.get(xid);
        if(transaction == null)
            throw new InvalidTransactionException(xid, "no such transaction");
        transaction.addUndoOperation(ops);
        transactionList.put(xid, transaction);
    }
}
