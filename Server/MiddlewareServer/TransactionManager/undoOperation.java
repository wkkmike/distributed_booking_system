package MiddlewareServer.TransactionManager;

import Server.Common.Customer;
import Server.Common.ReservableItem;

public class undoOperation {

    private undoCommandType operationType;
    private ReservableItem item = null;
    private String key = null;
    private Customer customer = null;

    public enum undoCommandType{
        Set_Customer,
        Set_Car,
        Set_Flight,
        Set_Room,
        Delete_Car,
        Delete_Flight,
        Delete_Room,
        Delete_Customer,
        Unreserve_Car,
        Unreserve_Flight,
        Unreserve_Room
    };

    public undoOperation(undoCommandType cmd, ReservableItem item){
        this.item = item;
        this.operationType = cmd;
    }

    public undoOperation(undoCommandType cmd, String key){
        this.operationType = cmd;
        this.key = key;
    }

    public undoOperation(undoCommandType cmd, Customer customer){
        this.operationType = cmd;
        this.customer = customer;
    }

    public undoOperation(undoCommandType cmd, ReservableItem item, Customer customer){
        this.operationType = cmd;
        this.item = item;
        this.customer = customer;
    }

    public undoCommandType getCmd(){
        return this.operationType;
    }

    public ReservableItem getItem(){
        return item;
    }

    public String getKey(){
        return this.key;
    }

    public Customer getCustomer(){
        return customer;
    }
}
