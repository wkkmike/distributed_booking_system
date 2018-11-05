package MiddlewareServer.TranscationManager;
import java.util.*;

import MiddlewareServer.Common.ResourceManager;
import MiddlewareServer.TranscationManager.undoOperation;

public class Transaction {


    private int transcationID;
    List<RM> RMList = new ArrayList<RM>();
    LinkedList<undoOperation> revertOperations = new LinkedList<undoOperation>();
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

    public List<RM> getRMList(){
        List<RM> returnList = new ArrayList<RM>(RMList);
        return returnList;
    }

    public boolean revert(ResourceManager mw){
        undoOperation operation = revertOperations.pollLast();
        while(operation != null) {
            switch (operation.getCmd()) {
                    case Delete_Item: {
                    break;
                }
                case Delete_Customer:{
                    break;
                }
                case Set_Customer:{
                    break;
                }
                case Set_Item:{
                    break;
                }
                case Unreserve:{
                    break;
                }
            }
        }
        return false;
    }
}
