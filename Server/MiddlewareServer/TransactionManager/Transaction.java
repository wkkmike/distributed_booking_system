package MiddlewareServer.TransactionManager;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.*;

import MiddlewareServer.Common.ResourceManager;
import MiddlewareServer.LockManager.LockManager;

public class Transaction {

    private final long TIMEOUT = 30000; // timeout by millisecond
    private int transcationID;
    List<RM> RMList = new ArrayList<RM>();
    LinkedList<undoOperation> undoOperationsList = new LinkedList<undoOperation>();
    private boolean aborted = false;
    private Date date = new Date();
    private long lastCall;
    private ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);
    private ResourceManager mw;
    private LockManager lm;

    public enum RM{
        RM_CUS,
        RM_C,
        RM_F,
        RM_R
    };

    public Transaction(int xid){
        transcationID = xid;
        lastCall = date.getTime();
    }

    public Transaction(int xid, ResourceManager mw, LockManager lm){
        transcationID = xid;
        this.mw = mw;
        this.lm = lm;
    }

    public Transaction(int xid, List<RM> RMList){
        transcationID = xid;
        this.RMList = new ArrayList<RM>(RMList);
    }

    public int getTransactionID(){
        return transcationID;
    }

    public boolean transactionInvoke() throws TranscationAbortedException{
        if(aborted)
            throw new TranscationAbortedException(transcationID, "This transaction has been aborted");
        scheduler.shutdownNow();
        return true;
    }

    public boolean commit() throws TranscationAbortedException{
        if(aborted)
            throw new TranscationAbortedException(transcationID, "This transaction has been aborted");
        return lm.UnlockAll(transcationID);
    }

    // commit prepare state
    private boolean prepare(){
        return false;
    }

    public void transactionSuspend(){
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(new Runnable() {
            public void run() {
                System.out.println("MW:: Transaction:<" + transcationID + "> timeout");
                abort();}
        }, TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public void addRMtoRMList(RM rm){
        if(RMList.contains(rm))
            return;
        RMList.add(rm);
    }

    public void removeRMfromRMList(RM rm){
        if(RMList.contains(rm))
            RMList.remove(rm);
        return;
    }

    public void addUndoOperation(undoOperation operation){
        undoOperationsList.addLast(operation);
    }

    public List<RM> getRMList(){
        List<RM> returnList = new ArrayList<RM>(RMList);
        return returnList;
    }

    public synchronized boolean abort(){
        scheduler.shutdownNow();
        if(aborted){
            System.out.println("MW::abort [xid:" + transcationID + "] has been aborted");
            return false;
        }
        undoOperation operation = undoOperationsList.pollLast();
        while (operation != null) {
            try {
                switch (operation.getCmd()) {
                    case Delete_Car: {
                        mw.undoAddCars(transcationID, operation.getKey());
                        break;
                    }
                    case Delete_Customer: {
                        mw.undoAddCustomers(transcationID, Integer.parseInt(operation.getKey()));
                        break;
                    }
                    case Delete_Room: {
                        mw.undoAddRooms(transcationID, operation.getKey());
                        break;
                    }
                    case Delete_Flight: {
                        mw.undoAddFlights(transcationID, Integer.parseInt(operation.getKey()));
                        break;
                    }
                    case Set_Customer: {
                        mw.setCustomer(transcationID, operation.getCustomer());
                        break;
                    }
                    case Set_Car: {
                        mw.setCar(transcationID, operation.getItem());
                        break;
                    }
                    case Set_Flight: {
                        mw.setFlight(transcationID, operation.getItem());
                        break;
                    }
                    case Set_Room: {
                        mw.setRoom(transcationID, operation.getItem());
                        break;
                    }
                    case Unreserve_Car: {
                        mw.setCar(transcationID, operation.getItem());
                        mw.setCustomer(transcationID, operation.getCustomer());
                        break;
                    }
                    case Unreserve_Flight:{
                        mw.setFlight(transcationID, operation.getItem());
                        mw.setCustomer(transcationID, operation.getCustomer());
                        break;
                    }
                    case Unreserve_Room:{
                        mw.setRoom(transcationID, operation.getItem());
                        mw.setCustomer(transcationID, operation.getCustomer());
                        break;
                    }
                }
            } catch (RemoteException e) {
                undoOperationsList.addLast(operation);
                System.out.println("RMI exception:" + e.getMessage());
                System.out.println("MW:UndoOperation for xid:" + transcationID + " failed");
                return false;
            }
            operation = undoOperationsList.pollLast();
        }
        System.out.println("MW_Transaction:UndoOperation for xid:" + transcationID + " finished");
        aborted = true;
        lm.UnlockAll(transcationID);
        System.out.println("MW_Transaction:UnlockAll for xid:" + transcationID + "success");
        return true;
    }
}
