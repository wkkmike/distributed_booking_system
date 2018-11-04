package MiddlewareServer.TranscationManager;

import java.rmi.RemoteException;
import java.util.*;

public class TransactionManager {
    List<Integer> transactionIdList = new ArrayList<Integer>();
    public TransactionManager(){

    }

    public int start() throws RemoteException{
        return 0;
    }

    public boolean commit(int transactionId) throws RemoteException,
            TranscationAbortedException, InvalidTransactionException{
        return false;
    }

    public void abort(int transactionId) throws RemoteException,
            InvalidTransactionException{

    }

    public boolean shutdown() throws RemoteException{
        return false;
    }
}
