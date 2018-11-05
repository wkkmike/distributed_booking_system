package MiddlewareServer.TransactionManager;
import java.rmi.RemoteException;
import java.util.*;

import MiddlewareServer.Common.ResourceManager;

public class Transaction {


    private int transcationID;
    List<RM> RMList = new ArrayList<RM>();
    LinkedList<undoOperation> undoOperationsList = new LinkedList<undoOperation>();
    private boolean aborted = false;

    public enum RM{
        RM_CUS,
        RM_C,
        RM_F,
        RM_R
    };

    public Transaction(int xid){
        transcationID = xid;
    }

    public Transaction(int xid, List<RM> RMList){
        transcationID = xid;
        this.RMList = new ArrayList<RM>(RMList);
    }

    public int getTranscationID(){
        return transcationID;
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

    public boolean abort(ResourceManager mw){
        undoOperation operation = undoOperationsList.pollLast();
        while (operation != null) {
            try {
                switch (operation.getCmd()) {
                    case Delete_Car: {
                        mw.deleteCars(transcationID, operation.getKey());
                        break;
                    }
                    case Delete_Customer: {
                        mw.deleteCustomer(transcationID, Integer.parseInt(operation.getKey()));
                        break;
                    }
                    case Delete_Room: {
                        mw.deleteRooms(transcationID, operation.getKey());
                        break;
                    }
                    case Delete_Flight: {
                        mw.deleteFlight(transcationID, Integer.parseInt(operation.getKey()));
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
        }
        System.out.println("MW_Transaction:UndoOperation for xid:" + transcationID + " finished");
        return true;
    }
}
