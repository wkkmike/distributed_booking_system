package MiddlewareServer.TranscationManager;

import Server.Common.RMItem;

public class undoOperation {

    private undoCommandType operationType;
    private RMItem item = null;

    public enum undoCommandType{
        Set_Customer,
        Set_Item,
        Delete_Item,
        Delete_Customer,
        Unreserve
    };

    public undoOperation(undoCommandType cmd, RMItem item){
        this.item = item;
        this.operationType = cmd;
    }

    public undoCommandType getCmd(){
        return this.operationType;
    }

    public RMItem getItem(){
        return (RMItem) item.clone();
    }
}
