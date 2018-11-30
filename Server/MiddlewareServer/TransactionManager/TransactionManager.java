package MiddlewareServer.TransactionManager;

import MiddlewareServer.Common.ResourceManager;
import MiddlewareServer.LockManager.LockManager;


import java.io.*;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.*;

public class TransactionManager {
    private int xid;
    HashMap<Integer, Transaction> transactionList;
    private ResourceManager middleware;
    private LockManager lm;
    private String logFileName = "./middlewareLog";
    private String fileAName = "./mwA";
    private String fileBName = "./mwB";
    private String masterName = "./mwMaster";
    private boolean masterIsA;
    private boolean rmC = true;
    private boolean rmR = true;
    private boolean rmF = true;
    private boolean rmCus = true;
    private boolean alive = true;
    private List<Integer> transactionStatusList = new ArrayList<>();
    private int timeoutInSec = 5;
    private long timeoutForRetry = 45000;
    private boolean flag1 = true;
    private boolean flag2 = true;
    private boolean flag3 = true;
    private boolean flag4 = true;
    private boolean flag5 = true;
    private boolean flag6 = true;
    private boolean flag7 = true;
    private boolean flag8 = true;
    private final long TIMEOUT = 1;

    public TransactionManager(){
        xid = 1;
        transactionList = new HashMap<Integer, Transaction>();
    }

    public TransactionManager(ResourceManager middleware, LockManager lm){
        xid = 1;
        transactionList = new HashMap<Integer, Transaction>();
        this.middleware = middleware;
        this.lm = lm;


        File masterRecord = new File(masterName);
        // create file in disk
        if(!masterRecord.exists()) {
            File fileA = new File(fileAName);
            File fileB = new File(fileBName);
            File logFile = new File(logFileName);
            try {
                FileWriter masterWriter = new FileWriter(masterRecord, false);
                FileWriter logWriter = new FileWriter(logFileName);
                masterRecord.createNewFile();
                fileA.delete();
                fileB.delete();
                logFile.delete();
                fileA.createNewFile();
                fileB.createNewFile();
                logFile.createNewFile();
                masterWriter.write("0 A");
                store(fileAName);
                store(fileBName);
                masterIsA = true;
                logWriter.flush();
                logWriter.close();
                masterWriter.close();
            } catch (IOException e) {
                System.out.println("Can't create file in disk for " + logFileName);
            }
        }
        else{
            middleware.reconnect();
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

    public void setAlive(boolean a){
        if(!a)
            System.out.println("TM:: Some RM is not alive now.");
        else
            System.out.println("TM:: All RM are alive.");
        alive = a;
    }

    public boolean isAlive(){
        return alive;
    }

    public boolean commit(int transactionId) throws RemoteException, TranscationAbortedException,
            InvalidTransactionException, RMNotAliveException{
        Transaction transaction = transactionList.get(transactionId);
        if(transaction == null)
            throw new InvalidTransactionException(transactionId, "no such transaction");

        // Write start 2PC
        write2log(Integer.toString(transactionId) + " S");

        if(!flag1)
            System.exit(1);

        // Send vote request to all participant
        boolean prepareCommitFlag = true;
        try{
            prepareCommitFlag = prepareCommit(transactionId);
        }
        catch(RMNotAliveException e){
            throw e;
        }

        if(prepareCommitFlag) {
            if(!flag4)
                System.exit(1);
            // receive all decision, than commit.
            if(masterIsA){
                if(!save(fileBName)){
                    // can't save to file, coordinator vote no, then abort this transaction
                    write2log(Integer.toString(transactionId) + " A");
                    sendResult(transactionId, false);
                    return false;
                }
            }
            else{
                if(!save(fileAName)){
                    write2log(Integer.toString(transactionId) + " A");
                    sendResult(transactionId, false);
                    return false;
                }
            }
        }
        // some participant vote no, abort the transaction
        else {
            if(!flag5)
                System.exit(1);
            // abort the transaction.
            transactionList.remove(transactionId);
            write2log(Integer.toString(transactionId) + " A");
            sendResult(transactionId, false);
            return false;
        }

        // All participant vote yes, commit the transaction
        if(masterIsA) {
            masterIsA = false;
            try {
                FileWriter masterWriter = new FileWriter(masterName, false);
                masterWriter.write(transactionId + " B");
                masterWriter.flush();
                masterWriter.close();
            }
            catch (IOException e){
                System.out.println("Can't write to " + masterName);
            }
        }
        else {
            masterIsA = true;
            try {
                FileWriter masterWriter = new FileWriter(masterName, false);
                masterWriter.write(transactionId + " A");
                masterWriter.flush();
                masterWriter.close();
            } catch (IOException e) {
                System.out.println("Can't write to " + masterName);
            }
        }
        if(!flag5)
            System.exit(1);
        transactionStatusList.add(new Integer(transactionId));
        write2log(transactionId + " C");

        if(transaction.commit()){
            // send result to all participant.
            try {
                sendResult(transactionId, true);
            }
            catch (RMNotAliveException e){

            }
            finally {
                transactionList.remove(transactionId);
            }

            return true;
        }

        return false;
    }

    public boolean transactionInvoke(int transactionId) throws InvalidTransactionException, TranscationAbortedException, RMNotAliveException{
        if(!alive)
        {
            if(middleware.allAlive()){
                alive = true;
            }
            else {
                throw new RMNotAliveException();
            }
        }
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
            throw new InvalidTransactionException(transactionId, "no such transaction");
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
            FileWriter logWriter = new FileWriter(logFileName, true);
            logWriter.write(msg + "\n");
            logWriter.flush();
            logWriter.close();
        }
        catch (IOException e){
            System.out.println("Can't write to log");
        }
    }

    private boolean recovery(){
        try {
            BufferedReader masterReader = new BufferedReader(new FileReader(masterName));
            String line = masterReader.readLine();
            if(line == null)
                return true;
            String[] masterLine = line.split(" ");
            String n = masterLine[1];
            if(n.equals("A")){
                load(fileAName);
                masterIsA = true;
            }
            else{
                load(fileBName);
                masterIsA = false;
            }
            BufferedReader logReader = new BufferedReader(new FileReader(logFileName));
            line = logReader.readLine();
            HashMap<String, String> logHashMap = new HashMap<>();
            while(line != null){
                String[] log = line.split(" ");
                logHashMap.put(log[0], log[1]);
                line = logReader.readLine();
            }
            Iterator it = logHashMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                int xid =  Integer.parseInt((String) pair.getKey());
                String status = (String) pair.getValue();
                if(status.equals("S")){
                    transactionStatusList.remove(new Integer(xid));
                    middleware.abortRequest(xid);
                    write2log(xid + " A");
                }
                if(status.equals("C")) {
                    while(true) {
                        try {
                            sendResult(xid, true);
                            break;
                        }
                        catch(RMNotAliveException e){
                            continue;
                        }
                    }
                }
                if(status.equals("I")){
                    transactionStatusList.remove(new Integer(xid));
                    middleware.abortRequest(xid);
                    write2log(xid + " A");
                }
                if(status.equals("A")){
                    while(true) {
                        try {
                            sendResult(xid, true);
                            break;
                        }
                        catch(RMNotAliveException e){
                            continue;
                        }
                    }
                }
                it.remove();
                File crash = new File("./crash");
                if(crash.exists())
                    System.exit(1);
            }
        }
        catch(IOException e){

        }
        return true;
    }

    // return true if all particpate vote yes.
    private boolean prepareCommit(int xid) throws RMNotAliveException{
        List<Transaction.RM> rmList = new ArrayList<>(4);
        if(!transactionList.containsKey(xid)) {
            rmList.add(Transaction.RM.RM_C);
            rmList.add(Transaction.RM.RM_CUS);
            rmList.add(Transaction.RM.RM_F);
            rmList.add(Transaction.RM.RM_R);
        }
        else{
            rmList = transactionList.get(xid).getRMList();
        }
        long startTime = new Date().getTime();
        try {
            for (Transaction.RM rm : rmList) {
                if (rm == Transaction.RM.RM_CUS) {
                    if(!timeoutPrepareCommit(xid, "customers", startTime)){
                        // Remove RM since it has discard this transaction.
                        transactionList.get(xid).removeRMfromRMList(rm);
                        System.out.println("TM::customer server votes no for <" + xid + "> commit");
                        return false;
                    }
                }
                if (rm == Transaction.RM.RM_C) {
                    if(!timeoutPrepareCommit(xid, "cars", startTime)){
                        transactionList.get(xid).removeRMfromRMList(rm);
                        System.out.println("TM::cars server votes no for <" + xid + "> commit");
                        return false;
                    }
                }
                if (rm == Transaction.RM.RM_R) {
                    if(!timeoutPrepareCommit(xid, "rooms", startTime)){
                        transactionList.get(xid).removeRMfromRMList(rm);
                        System.out.println("TM::rooms server votes no for <" + xid + "> commit");
                        return false;
                    }
                }
                if (rm == Transaction.RM.RM_F) {
                    if(!timeoutPrepareCommit(xid, "flights", startTime)){
                        transactionList.get(xid).removeRMfromRMList(rm);
                        System.out.println("TM::flights server votes no for <" + xid + "> commit");
                        return false;
                    }
                }
                if(!flag3)
                    System.exit(1);
            }
        }
        catch (RMNotAliveException e){
            throw e;
        }
        catch (Exception e){
            System.out.println("TM::remote exception for prepareCommit <" + xid + ">");
        }
        System.out.println("TM::all participant RM vote yes for <" + xid + ">");
        return true;
    }

    private boolean timeoutSendResult(int transactionId, String rm, boolean result, long startTime)throws RMNotAliveException{
        try {
            return middleware.sendResult(transactionId, rm, result);
        } catch (Exception e) {
            alive = false;
            throw new RMNotAliveException();
        }
    }


    /*
    private boolean timeoutPrepareCommit(int transactionId, String rm, long startTime)throws RMNotAliveException{
        ExecutorService executor = Executors.newSingleThreadExecutor();

        //TODO: RM crash before sending the request.
        Future<Boolean> handler = executor.submit(() -> middleware.prepareCommit(rm, transactionId));

        while(true) {
            try {
                if(!flag2){
                    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                    scheduler.schedule(new Runnable() {
                        public void run() {
                            System.out.println("here");
                            System.exit(1);
                        }
                    }, TIMEOUT, TimeUnit.MILLISECONDS);
                }
                return handler.get(timeoutInSec * 1000, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                handler.cancel(true);
                long nowTime = new Date().getTime();
                if (nowTime - startTime > timeoutForRetry) {
                    alive = false;
                    throw new RMNotAliveException();
                }
            } catch (ExecutionException e) {
                long nowTime = new Date().getTime();
                if (nowTime - startTime > timeoutForRetry) {
                    alive = false;
                    throw new RMNotAliveException();
                }
                try {
                    Thread.sleep(5000);
                }catch (Exception ee){

                }
//                e.printStackTrace();
//                System.out.println(e.getCause());
            } catch (Exception e) {
                long nowTime = new Date().getTime();
                if (nowTime - startTime > timeoutForRetry) {
                    alive = false;
                    throw new RMNotAliveException();
                }
                try {
                    Thread.sleep(5000);
                }catch (Exception ee){

                }
                System.out.println("Concurrent Exception");
            }finally {
                executor.shutdownNow();
            }
        }
    }
*/

    private boolean timeoutPrepareCommit(int transactionId, String rm, long startTime)throws RMNotAliveException{
        while(true) {
            try {
                if(!flag2){
                    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                    scheduler.schedule(new Runnable() {
                        public void run() {
                            System.exit(1);
                        }
                    }, TIMEOUT, TimeUnit.MILLISECONDS);
                }
                return middleware.prepareCommit(rm, transactionId);
            } catch (Exception e) {
                long nowTime = new Date().getTime();
                if (nowTime - startTime > timeoutForRetry) {
                    alive = false;
                    throw new RMNotAliveException();
                }
                try {
                    Thread.sleep(5000);
                }catch (Exception ee){

                }
                System.out.println("TM:: Retry send perpare commit");
            }
        }
    }

    private boolean sendResult(int xid, boolean result) throws RMNotAliveException{
        List<Transaction.RM> rmList = new ArrayList<>(4);
        if(!transactionList.containsKey(xid)) {
            rmList.add(Transaction.RM.RM_C);
            rmList.add(Transaction.RM.RM_CUS);
            rmList.add(Transaction.RM.RM_F);
            rmList.add(Transaction.RM.RM_R);
        }
        else{
            rmList = transactionList.get(xid).getRMList();
        }

            for (Transaction.RM rm : rmList) {
                long startTime = new Date().getTime();
                if (rm == Transaction.RM.RM_CUS) {
                    if(!timeoutSendResult(xid, "customers", result, startTime)){
                        System.out.println("TM::customer server don't answer for <" + xid + "> send result");
                        return false;
                    }
                }
                if (rm == Transaction.RM.RM_C) {
                    if(!timeoutSendResult(xid, "cars", result, startTime)){
                        System.out.println("TM::cars server don't answer for <" + xid + "> send result");
                        return false;
                    }
                }
                if (rm == Transaction.RM.RM_R) {
                    if(!timeoutSendResult(xid, "rooms", result, startTime)){
                        System.out.println("TM::rooms server don't answer for <" + xid + "> send result");
                        return false;
                    }
                }
                if (rm == Transaction.RM.RM_F) {
                    if(!timeoutSendResult(xid, "flights", result, startTime)){
                        System.out.println("TM::flights server don't anser for <" + xid + "> send result");
                        return false;
                    }
                }
                if (!flag6)
                    System.exit(1);
            }
            if(!flag7)
                System.exit(1);
        System.out.println("TM::all participant RM receive yes vote for <" + xid + ">");
        return true;
    }

    private boolean allRMAlive(){
        if(rmC && rmF && rmR && rmCus)
            return true;
        return false;
    }

    public boolean save(String name){
        try {
            File outFile = new File(name);
            outFile.delete();
            outFile.createNewFile();
            FileOutputStream fileOut = new FileOutputStream(name);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(transactionStatusList);
            out.close();
            fileOut.close();
            System.out.printf("Serialized data is saved in " + name + " .\n");
        }catch(IOException i) {
            i.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean store(String name){
        File file = new File(name);
        try {
            FileOutputStream fileOut = new FileOutputStream(name, false);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(transactionStatusList);
            out.close();
            fileOut.close();
        }
        catch (IOException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean isAbort(int xid){
        if(transactionStatusList.contains(new Integer(xid)))
            return false;
        return true;
    }

    public void abortRequest(int xid){
        if(!transactionList.containsKey(xid))
            return;
        transactionList.get(xid).unlockAll();
        transactionList.remove(xid);
        write2log(xid + " A");
        return;
    }

    public boolean load(String name){
        try {
            FileInputStream fileIn = new FileInputStream(name);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            transactionStatusList = (List<Integer>) in.readObject();
            in.close();
            fileIn.close();
            System.out.printf("Serialized data is load from " + name + " .\n");
        }catch(IOException | ClassNotFoundException i) {
            i.printStackTrace();
            return false;
        }
        return true;
    }

    public List<Integer> getTransactionIdList(){
        List<Integer> transactionIdList = new LinkedList<>();
        Iterator it = transactionList.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            transactionIdList.add((Integer) pair.getKey());
        }
        return transactionIdList;
    }

    public boolean hasRM(int transaxtionId, Transaction.RM rm){
        Transaction transaction = transactionList.get(transaxtionId);
        if(transaction == null)
            return false;
        return transaction.hasRm(rm);
    }

    public void crashMiddleware(int mode){
        switch (mode){
            case 1:{
                flag1 = false;
                break;
            }
            case 2:{
                flag2 = false;
                break;
            }
            case 3:{
                flag3 = false;
                break;
            }
            case 4:{
                flag4 = false;
                break;
            }
            case 5:{
                flag5 = false;
                break;
            }
            case 6:{
                flag6 = false;
                break;
            }
            case 7:{
                flag7 = false;
                break;
            }
            case 8:{
                flag8 = false;
                File crash = new File("./crash");
                if(!crash.exists()) {
                    try {
                        crash.createNewFile();
                    }
                    catch (IOException e){
                        System.out.println("Can't create crash file");
                    }
                }
                break;
            }
        }
        return;
    }

    public void resetCrashes(){
        flag1 = true;
        flag2 = true;
        flag3 = true;
        flag4 = true;
        flag5 = true;
        flag6 = true;
        flag7 = true;
        flag8 = true;
        File crash = new File("./crash");
        crash.deleteOnExit();
        return;
    }
}
