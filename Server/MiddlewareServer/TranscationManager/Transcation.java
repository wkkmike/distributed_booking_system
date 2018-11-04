package MiddlewareServer.TranscationManager;
import java.util.*;

public class Transcation {

    int transcationID;
    List<RM> RMList = new ArrayList<RM>();

    public enum RM{
        RM_CUS,
        RM_C,
        RM_F,
        RM_R
    };

    public Transcation(int xid){
        transcationID = xid;
    }

    public Transcation(int xid, List<RM> RMList){
        transcationID = xid;
        this.RMList = new ArrayList<RM>(RMList);
    }

    public int getTranscationID(){
        return transcationID;
    }

    public List<RM> getRMList(){
        List<RM> returnList = new ArrayList<RM>(RMList);
        return returnList;
    }
}
