package MiddlewareServer.TransactionManager;

import MiddlewareServer.Common.ResourceManager;
import MiddlewareServer.LockManager.LockManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;

public class TransactionManager {
    private int xid;
    HashMap<Integer, Transaction> transactionList;
    private ResourceManager middleware;
    private LockManager lm;
    private String logFileName = "./middlewareLog";
    private FileWriter logWriter;
    private boolean rmC = true;
    private boolean rmR = true;
    private boolean rmF = true;
    private boolean rmCus = true;

    public TransactionManager(){
        xid = 1;
        transactionList = new HashMap<Integer, Transaction>();
    }

    public TransactionManager(ResourceManager middleware, LockManager lm){
        xid = 1;
        transactionList = new HashMap<Integer, Transaction>();
        this.middleware = middleware;
        this.lm = lm;


        File logFile = new File(logFileName);

        // create file in disk
        if(!logFile.exists()) {
            try {
                logFile.createNewFile();
                logWriter = new FileWriter(logFileName);
                logWriter.flush();
            } catch (IOException e) {
                System.out.println("Can't create file in disk for " + logFileName);
            }
        }
        else{
            recovery();
        }
    }

    //assign xid upon request
    public synchronized int start() throws RemoteException{
        Transaction t = new Transaction(xid, middleware, lm);
        transactionList.put(xid, t);
        write2log(xid + " I");
        xid ++;
        t.transactionSuspend();
        return (xid - 1);
    }

    public boolean commit(int transactionId) throws RemoteException, TranscationAbortedException,
            InvalidTransactionException, TransactionCommitFailException{
        Transaction transaction = transactionList.get(transactionId);
        if(transaction == null)
            throw new InvalidTransactionException(xid, "no such transaction");
        write2log(Integer.toString(transactionId) + " S");

        if(prepareCommit(xid)) {
            write2log(Integer.toString(transactionId) + " A");
            sendResult(xid, true);
            //TODO: Send yes to every.
        }
        else {
            write2log(Integer.toString(transactionId) + " N");
            sendResult(xid, false);
            return false;
            // TODO: Send no to every
        }
        if(transaction.commit()){
            transactionList.remove(transactionId);
            write2log(transactionId + " C");
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

    private void write2log(String msg){
        try {
            logWriter.write(msg + "\n");
        }
        catch (IOException e){
            System.out.println("Can't write to log");
        }
    }

    private boolean recovery(){
        return true;
    }

    // return true if all particpate vote yes.
    private boolean prepareCommit(int xid){
        List<Transaction.RM> rmList = transactionList.get(xid).getRMList();
        try {
            for (Transaction.RM rm : rmList) {
                if (rm == Transaction.RM.RM_CUS) {
                    if(!middleware.prepareCommit("costumers", xid)){
                        // Remove RM since it has discard this transaction.
                        transactionList.get(xid).removeRMfromRMList(rm);
                        System.out.println("TM::customer server votes no for <" + xid + "> commit");
                        return false;
                    }
                }
                if (rm == Transaction.RM.RM_C) {
                    if(!middleware.prepareCommit("cars", xid)){
                        transactionList.get(xid).removeRMfromRMList(rm);
                        System.out.println("TM::cars server votes no for <" + xid + "> commit");
                        return false;
                    }
                }
                if (rm == Transaction.RM.RM_R) {
                    if(!middleware.prepareCommit("rooms", xid)){
                        transactionList.get(xid).removeRMfromRMList(rm);
                        System.out.println("TM::rooms server votes no for <" + xid + "> commit");
                        return false;
                    }
                }
                if (rm == Transaction.RM.RM_F) {
                    if(!middleware.prepareCommit("flights", xid)){
                        transactionList.get(xid).removeRMfromRMList(rm);
                        System.out.println("TM::flights server votes no for <" + xid + "> commit");
                        return false;
                    }
                }
            }
        }
        catch (RemoteException e){
            System.out.println("TM::remote exception for prepareCommit <" + xid + ">");
        }
        System.out.println("TM::all participant RM vote yes for <" + xid + ">");
        return true;
    }


    private boolean sendResult(int xid, boolean result){
        List<Transaction.RM> rmList = transactionList.get(xid).getRMList();
        try {
            for (Transaction.RM rm : rmList) {
                if (rm == Transaction.RM.RM_CUS) {
                    if(!middleware.sendResult(xid, "costumers", result)){
                        System.out.println("TM::customer server votes no for <" + xid + "> commit");
                        return false;
                    }
                }
                if (rm == Transaction.RM.RM_C) {
                    if(!middleware.sendResult(xid, "cars", result)){
                        System.out.println("TM::cars server votes no for <" + xid + "> commit");
                        return false;
                    }
                }
                if (rm == Transaction.RM.RM_R) {
                    if(!middleware.sendResult(xid, "rooms", result)){
                        System.out.println("TM::rooms server votes no for <" + xid + "> commit");
                        return false;
                    }
                }
                if (rm == Transaction.RM.RM_F) {
                    if(!middleware.sendResult(xid, "flights", result)){
                        System.out.println("TM::flights server votes no for <" + xid + "> commit");
                        return false;
                    }
                }
            }
        }
        catch (RemoteException e){
            System.out.println("TM::remote exception for sendResult <" + xid + ">");
        }
        System.out.println("TM::all participant RM receive yes vote for <" + xid + ">");
        return true;
    }

    private boolean allRMAlive(){
        if(rmC && rmF && rmR && rmCus)
            return true;
        return false;
    }
}
