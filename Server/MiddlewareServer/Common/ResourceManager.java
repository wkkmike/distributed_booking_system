// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package MiddlewareServer.Common;

import MiddlewareServer.Interface.IMiddleware;
import MiddlewareServer.LockManager.DeadlockException;
import MiddlewareServer.LockManager.LockManager;
import MiddlewareServer.LockManager.TransactionLockObject;
import MiddlewareServer.TransactionManager.*;
import Server.Interface.*;
import Server.Common.*;


import java.io.Serializable;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ResourceManager implements IMiddleware
{
	protected String m_name = "";
    private static String s_rmiPrefix = "group15";

	public IResourceManager m_resourceManager_f = null;
	public IResourceManager m_resourceManager_c = null;
	public IResourceManager m_resourceManager_r = null;
	public IResourceManager m_resourceManager_cus = null;

	private static String s_serverHost_Flight = "localhost";
	private static int s_serverPort_Flight = 1099;
	private static String s_serverName_Flight = "Flights";

	private static String s_serverHost_Car = "localhost";
	private static int s_serverPort_Car = 1099;
	private static String s_serverName_Car = "Cars";

	private static String s_serverHost_Room = "localhost";
	private static int s_serverPort_Room = 1099;
	private static String s_serverName_Room = "Rooms";

	private static String s_serverHost_Customer = "localhost";
	private static int s_serverPort_Customer = 1099;
	private static String s_serverName_Customer = "Customers";

	//changes start
	private LockManager LM = new LockManager();
	private TransactionManager TM = new TransactionManager(this, LM);
	private ScheduledExecutorService scheduler =
			Executors.newScheduledThreadPool(1);
	private final long TIMEOUT = 10000;
	//changes end

	public ResourceManager(String p_name)
	{
		m_name = p_name;
		scheduler.scheduleAtFixedRate(new Runnable() {
			public void run() {
				if(!allAlive()){
					TM.setAlive(false);
					reconnect();
				}
			}
		}, TIMEOUT, TIMEOUT, TimeUnit.MILLISECONDS);
	}

	public boolean connect(){
		System.out.println("MW:: reconnect to RM");
		try {
			m_resourceManager_f = connectServer(s_serverHost_Flight, s_serverPort_Flight, s_serverName_Flight);
			m_resourceManager_c = connectServer(s_serverHost_Car, s_serverPort_Car, s_serverName_Car);
			m_resourceManager_r = connectServer(s_serverHost_Room, s_serverPort_Room, s_serverName_Room);
			m_resourceManager_cus = connectServer(s_serverHost_Customer, s_serverPort_Customer, s_serverName_Customer);
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}
		return true;
	}

	public void reconnect(){
		System.out.println("MW:: reconnect to RM");
		try {
			m_resourceManager_f = connectServer(s_serverHost_Flight, s_serverPort_Flight, s_serverName_Flight);
			m_resourceManager_c = connectServer(s_serverHost_Car, s_serverPort_Car, s_serverName_Car);
			m_resourceManager_r = connectServer(s_serverHost_Room, s_serverPort_Room, s_serverName_Room);
			m_resourceManager_cus = connectServer(s_serverHost_Customer, s_serverPort_Customer, s_serverName_Customer);
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}
		for (int id: TM.getTransactionIdList()){
			if(TM.hasRM(id, Transaction.RM.RM_CUS))
				startTransaction(id, "customers");
			if(TM.hasRM(id, Transaction.RM.RM_C))
				startTransaction(id, "cars");
			if(TM.hasRM(id, Transaction.RM.RM_R))
				startTransaction(id, "rooms");
			if(TM.hasRM(id, Transaction.RM.RM_F))
				startTransaction(id, "flights");
		}
		TM.setAlive(true);
		System.out.println("MW:: reconnect success");
		return;
	}

	public IResourceManager connectServer(String server, int port, String name)
    {
		IResourceManager m_resourceManager;
        try {
            boolean first = true;
            while (true) {
                try {
                    Registry registry = LocateRegistry.getRegistry(server, port);
                    m_resourceManager = (IResourceManager) registry.lookup(s_rmiPrefix + name);
                    System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
                    return m_resourceManager;
                }
                catch (NotBoundException|RemoteException e) {
                    if (first) {
                        System.out.println("Waiting for '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
                        first = false;
                    }
                }
                Thread.sleep(500);
            }
        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }


	// Create a new flight, or add seats to existing flight
	// NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
	public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException,
			InvalidTransactionException, DeadlockException, TranscationAbortedException, RMNotAliveException
	{
		Trace.info("RM::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");
		try{
			TM.transactionInvoke(xid);
			// get write lock
			if(LM.Lock(xid, "flight-" + flightNum, TransactionLockObject.LockType.LOCK_WRITE)) {
				// have write lock
				TM.addRM(xid, Transaction.RM.RM_F);
				ReservableItem flight = m_resourceManager_f.getFlight(xid, flightNum);
				// No such flight before, undo operation should delete this entry;
				if(flight == null) {
					TM.addUndoOperation(xid, new undoOperation(undoOperation.undoCommandType.Delete_Flight, Integer.toString(flightNum)));
				}
				// Have such flight, the add is actually a update operation. Undo operation reset the flight.
				else {
					TM.addUndoOperation(xid, new undoOperation(undoOperation.undoCommandType.Set_Flight, flight));
				}
				return m_resourceManager_f.addFlight(xid, flightNum, flightSeats, flightPrice);
			}
		}
		catch (RemoteException e){
			TM.setAlive(false);
//			reconnect();
			throw new RMNotAliveException();
		}
		catch(InvalidTransactionException e){
			Trace.info("RM::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") is an invalid transaction");
			throw e;
		}
		catch(RMNotAliveException rme){
			Trace.info("RM not alive exception");
			throw rme;
		}
		catch(DeadlockException de){
			Trace.info("RM::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") get deadlock, now going to abort this transaction");
			try {
				TM.abort(xid);
			}catch (InvalidTransactionException ie){
				Trace.info("RM::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") is an invalid transaction");
				throw ie;
			}
			throw de;
		} finally{
			TM.transactionSuspend(xid);
		}
		return false;
	}

	// Create a new car location or add cars to an existing location
	// NOTE: if price <= 0 and the location already exists, it maintains its current price
	public boolean addCars(int xid, String location, int count, int price) throws RemoteException,
			InvalidTransactionException, DeadlockException, TranscationAbortedException, RMNotAliveException {
		Trace.info("RM::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
		try{
			TM.transactionInvoke(xid);
			//get write lock
			if(LM.Lock(xid,"car-"+location,TransactionLockObject.LockType.LOCK_WRITE)){
				// have write lock
				TM.addRM(xid, Transaction.RM.RM_C);
				ReservableItem car = m_resourceManager_c.getCar(xid, location);
				// No such car before, undo operation should delete this entry;
				if(car == null)
					TM.addUndoOperation(xid, new undoOperation(undoOperation.undoCommandType.Delete_Car, location));
					// Have such car, the add is actually a update operation. Undo operation reset the car.
				else
					TM.addUndoOperation(xid, new undoOperation(undoOperation.undoCommandType.Set_Car, car));
				return m_resourceManager_c.addCars(xid, location, count, price);
			}
		}
		catch (RemoteException e){
			TM.setAlive(false);
//			reconnect();
			throw new RMNotAliveException();
		}
		catch(RMNotAliveException rme){
			Trace.info("RM not alive exception");
			throw rme;
		}
		catch(InvalidTransactionException e){
			Trace.info("RM::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ") is an invalid transaction");
			throw e;
		}
		catch(DeadlockException de){
			Trace.info("RM::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ")  get deadlock, now going to abort this transaction");
			try {
				TM.abort(xid);
			}catch (InvalidTransactionException ie){
				Trace.info("RM::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ")  is an invalid transaction");
				throw ie;
			}
			throw de;
		}finally{
			TM.transactionSuspend(xid);
		}

		return  false;
	}

	// Create a new room location or add rooms to an existing location
	// NOTE: if price <= 0 and the room location already exists, it maintains its current price
	public boolean addRooms(int xid, String location, int count, int price) throws RemoteException,
			InvalidTransactionException, DeadlockException, TranscationAbortedException, RMNotAliveException {
		Trace.info("RM::addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") called");

		try{
			TM.transactionInvoke(xid);

			//get write lock
			if(LM.Lock(xid,"room-"+location,TransactionLockObject.LockType.LOCK_WRITE)){
				// have write lock
				TM.addRM(xid, Transaction.RM.RM_R);
				ReservableItem room = m_resourceManager_r.getRoom(xid, location);
				// No such room before, undo operation should delete this entry;
				if(room == null)
					TM.addUndoOperation(xid, new undoOperation(undoOperation.undoCommandType.Delete_Room, location));
					// Have such room, the add is actually a update operation. Undo operation reset the room.
				else
					TM.addUndoOperation(xid, new undoOperation(undoOperation.undoCommandType.Set_Room, room));
				return m_resourceManager_r.addRooms(xid, location, count, price);
			}
		}
		catch (RemoteException e){
			TM.setAlive(false);
//			reconnect();
			throw new RMNotAliveException();
		}
		catch(RMNotAliveException rme){
			Trace.info("RM not alive exception");
			throw rme;
		}
		catch(InvalidTransactionException e){
			Trace.info("RM::addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") is an invalid transaction");
			throw e;
		}
		catch(DeadlockException de){
			Trace.info("RM::addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") get deadlock, now going to abort this transaction");
			try {
				TM.abort(xid);
			}catch (InvalidTransactionException ie){
				Trace.info("RM::addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") is an invalid transaction");
				throw ie;
			}
			throw de;
		}finally{
			TM.transactionSuspend(xid);
		}

		return false;
	}

	// Deletes flight
	public boolean deleteFlight(int xid, int flightNum) throws RemoteException,
			InvalidTransactionException, DeadlockException, TranscationAbortedException, RMNotAliveException {
		Trace.info("RM::deleteFlight(" + xid + ", " + flightNum + ") called");
		try{
			TM.transactionInvoke(xid);
			// get write lock
			if(LM.Lock(xid, "flight-" + flightNum, TransactionLockObject.LockType.LOCK_WRITE)) {
				// have write lock
				TM.addRM(xid, Transaction.RM.RM_F);
				ReservableItem flight = m_resourceManager_f.getFlight(xid, flightNum);
				// No such flight before, undo operation should delete this entry;
				if(flight != null)
					TM.addUndoOperation(xid, new undoOperation(undoOperation.undoCommandType.Set_Flight, flight));
				return m_resourceManager_f.deleteFlight(xid, flightNum);
			}
		}
		catch (RemoteException e){
			TM.setAlive(false);
//			reconnect();
			throw new RMNotAliveException();
		}
		catch(RMNotAliveException rme){
			Trace.info("RM not alive exception");
			throw rme;
		}
		catch(InvalidTransactionException e){
			Trace.info("RM::deleteFlight(" + xid + ", " + flightNum + ") is an invalid transaction");
			throw e;
		}
		catch(DeadlockException de){
			Trace.info("RM::deleteFlight(" + xid + ", " + flightNum +  ") get deadlock, now going to abort this transaction");
			try {
				TM.abort(xid);
			}catch (InvalidTransactionException ie){
				Trace.info("RM::deleteFlight(" + xid + ", " + flightNum +  ") is an invalid transaction");
				throw ie;
			}
			throw de;
		} finally{
			TM.transactionSuspend(xid);
		}
		return false;
	}

	// Delete cars at a location
	public boolean deleteCars(int xid, String location) throws RemoteException,
			InvalidTransactionException, DeadlockException, TranscationAbortedException, RMNotAliveException {
		Trace.info("RM::deleteCars(" + xid + ", " + location + ") called");
		try{
			TM.transactionInvoke(xid);
			//get write lock
			if(LM.Lock(xid,"car-"+location,TransactionLockObject.LockType.LOCK_WRITE)){
				// have write lock
				TM.addRM(xid, Transaction.RM.RM_C);
				ReservableItem car = m_resourceManager_c.getCar(xid, location);
				// No such car before, undo operation should delete this entry;
				if(car != null)
					TM.addUndoOperation(xid, new undoOperation(undoOperation.undoCommandType.Set_Car, car));
				return m_resourceManager_c.deleteCars(xid, location);
			}
		}
		catch (RemoteException e){
			TM.setAlive(false);
//			reconnect();
			throw new RMNotAliveException();
		}
		catch(RMNotAliveException rme){
			Trace.info("RM not alive exception");
			throw rme;
		}
		catch(InvalidTransactionException e){
			Trace.info("RM::deleteCars(" + xid + ", " + location + ") is an invalid transaction");
			throw e;
		}
		catch(DeadlockException de){
			Trace.info("RM::deleteCars(" + xid + ", " + location + ")  get deadlock, now going to abort this transaction");
			try {
				TM.abort(xid);
			}catch (InvalidTransactionException ie){
				Trace.info("RM::deleteCars(" + xid + ", " + location + ")  is an invalid transaction");
				throw ie;
			}
			throw de;
		}finally{
			TM.transactionSuspend(xid);
		}
		return false;
	}

	// Delete rooms at a location
	public boolean deleteRooms(int xid, String location) throws RemoteException,
			DeadlockException, InvalidTransactionException, TranscationAbortedException, RMNotAliveException {
		Trace.info("RM::deleteRooms(" + xid + ", " + location + ") called");

		try{
			TM.transactionInvoke(xid);
			//get write lock
			if(LM.Lock(xid,"room-"+location,TransactionLockObject.LockType.LOCK_WRITE)){
				// have write lock
				TM.addRM(xid, Transaction.RM.RM_R);
				ReservableItem room = m_resourceManager_r.getRoom(xid, location);
				// No such room before, undo operation should delete this entry;
				if(room != null)
					TM.addUndoOperation(xid, new undoOperation(undoOperation.undoCommandType.Set_Room, room));
				return m_resourceManager_r.deleteRooms(xid, location);
			}
		}
		catch (RemoteException e){
			TM.setAlive(false);
//			reconnect();
			throw new RMNotAliveException();
		}
		catch(RMNotAliveException rme){
			Trace.info("RM not alive exception");
			throw rme;
		}
		catch(InvalidTransactionException e){
			Trace.info("RM::deleteRooms(" + xid + ", " + location + ") is an invalid transaction");
			throw e;
		}
		catch(DeadlockException de){
			Trace.info("RM::deleteRooms(" + xid + ", " + location + ") get deadlock, now going to abort this transaction");
			try {
				TM.abort(xid);
			}catch (InvalidTransactionException ie){
				Trace.info("RM::deleteRooms(" + xid + ", " + location + ") is an invalid transaction");
				throw ie;
			}
			throw de;
		}finally{
			TM.transactionSuspend(xid);
		}
		return false;
	}

	// Returns the number of empty seats in this flight
	public int queryFlight(int xid, int flightNum) throws RemoteException,
			InvalidTransactionException, DeadlockException, TranscationAbortedException, RMNotAliveException {
		Trace.info("RM::queryFlight(" + xid + ", " + flightNum + ") called");
		try{
			TM.transactionInvoke(xid);
			// get read lock
			if(LM.Lock(xid, "flight-" + flightNum, TransactionLockObject.LockType.LOCK_READ)) {
				// have read lock
				TM.addRM(xid, Transaction.RM.RM_F);
				return m_resourceManager_f.queryFlight(xid, flightNum);
			}
		}
		catch (RemoteException e){
			TM.setAlive(false);
//			reconnect();
			throw new RMNotAliveException();
		}
		catch(RMNotAliveException rme){
			Trace.info("RM not alive exception");
			throw rme;
		}
		catch(InvalidTransactionException e){
			Trace.info("RM::queryFlight(" + xid + ", " + flightNum + ") is an invalid transaction");
			throw e;
		}
		catch(DeadlockException de){
			Trace.info("RM::queryFlight(" + xid + ", " + flightNum + ") get deadlock, now going to abort this transaction");
			try {
				TM.abort(xid);
			}catch (InvalidTransactionException ie){
				Trace.info("RM::queryFlight(" + xid + ", " + flightNum + ") is an invalid transaction");
				throw ie;
			}
			throw de;
		}finally{
			System.out.println("###################################");
			TM.transactionSuspend(xid);
		}
		return -1;
	}

	// Returns the number of cars available at a location
	public int queryCars(int xid, String location) throws RemoteException,
			InvalidTransactionException, DeadlockException, TranscationAbortedException, RMNotAliveException {

		Trace.info("RM::queryCars(" + xid + ", " + location +") called");
		try{
			TM.transactionInvoke(xid);
			//get write lock
			if(LM.Lock(xid,"car-"+location,TransactionLockObject.LockType.LOCK_READ)){
				// have write lock
				TM.addRM(xid, Transaction.RM.RM_C);
				return m_resourceManager_c.queryCars(xid, location);
			}
		}
		catch (RemoteException e){
			TM.setAlive(false);
//			reconnect();
			throw new RMNotAliveException();
		}
		catch(RMNotAliveException rme){
			Trace.info("RM not alive exception");
			throw rme;
		}
		catch(InvalidTransactionException e){
			Trace.info("RM::queryCars(" + xid + ", " + location  + ") is an invalid transaction");
			throw e;
		}
		catch(DeadlockException de){
			Trace.info("RM::queryCars(" + xid + ", " + location  + ")  get deadlock, now going to abort this transaction");
			try {
				TM.abort(xid);
			}catch (InvalidTransactionException ie){
				Trace.info("RM::queryCars(" + xid + ", " + location  + ")  is an invalid transaction");
				throw ie;
			}
			throw de;
		}finally{
			TM.transactionSuspend(xid);
		}

		return -1;
	}

	// Returns the amount of rooms available at a location
	public int queryRooms(int xid, String location) throws RemoteException,
			InvalidTransactionException, DeadlockException, TranscationAbortedException, RMNotAliveException {
		Trace.info("RM::queryRooms(" + xid + ", " + location +") called");

		try{
			TM.transactionInvoke(xid);
			//get write lock
			if(LM.Lock(xid,"room-"+location,TransactionLockObject.LockType.LOCK_READ)){
				// have write lock
				TM.addRM(xid, Transaction.RM.RM_R);
				return m_resourceManager_r.queryRooms(xid, location);
			}
		}
		catch(RMNotAliveException rme){
			Trace.info("RM not alive exception");
			throw rme;
		}
		catch (RemoteException e){
			TM.setAlive(false);
//			reconnect();
			throw new RMNotAliveException();
		}
		catch(InvalidTransactionException e){
			Trace.info("RM::queryRooms(" + xid + ", " + location + ") is an invalid transaction");
			throw e;
		}
		catch(DeadlockException de){
			Trace.info("RM::queryRooms(" + xid + ", " + location + ") get deadlock, now going to abort this transaction");
			try {
				TM.abort(xid);
			}catch (InvalidTransactionException ie){
				Trace.info("RM::queryRooms(" + xid + ", " + location + ") is an invalid transaction");
				throw ie;
			}
			throw de;
		}finally{
			TM.transactionSuspend(xid);
		}
		return -1;
	}

	// Returns price of a seat in this flight
	public int queryFlightPrice(int xid, int flightNum) throws RemoteException,
			InvalidTransactionException, DeadlockException, TranscationAbortedException, RMNotAliveException {
		Trace.info("RM::queryFlightPrice(" + xid + ", " + flightNum + ") called");
		try{
			TM.transactionInvoke(xid);
			// get read lock
			if(LM.Lock(xid, "flight-" + flightNum, TransactionLockObject.LockType.LOCK_READ)) {
				// have read lock
				TM.addRM(xid, Transaction.RM.RM_F);
				return m_resourceManager_f.queryFlightPrice(xid, flightNum);
			}
		}
		catch(RMNotAliveException rme){
			Trace.info("RM not alive exception");
			throw rme;
		}
		catch (RemoteException e){
			TM.setAlive(false);
//			reconnect();
			throw new RMNotAliveException();
		}
		catch(InvalidTransactionException e){
			Trace.info("RM::queryFlightPrice(" + xid + ", " + flightNum + ") is an invalid transaction");
			throw e;
		}
		catch(DeadlockException de){
			Trace.info("RM::queryFlightPrice(" + xid + ", " + flightNum + ") get deadlock, now going to abort this transaction");
			try {
				TM.abort(xid);
			}catch (InvalidTransactionException ie){
				Trace.info("RM::queryFlightPrice(" + xid + ", " + flightNum + ") is an invalid transaction");
				throw ie;
			}
			throw de;
		}finally{
			TM.transactionSuspend(xid);
		}

		return -1;
	}

	// Returns price of cars at this location
	public int queryCarsPrice(int xid, String location) throws RemoteException,
			InvalidTransactionException, DeadlockException, TranscationAbortedException, RMNotAliveException {
		Trace.info("RM::queryCarsPrice(" + xid + ", " + location +") called");
		try{
			TM.transactionInvoke(xid);
			//get read lock
			if(LM.Lock(xid,"car-"+location,TransactionLockObject.LockType.LOCK_READ)){
				// have read lock
				TM.addRM(xid, Transaction.RM.RM_C);
				return m_resourceManager_c.queryCarsPrice(xid, location);
			}
		}
		catch(RMNotAliveException rme){
			Trace.info("RM not alive exception");
			throw rme;
		}
		catch (RemoteException e){
			TM.setAlive(false);
//			reconnect();
			throw new RMNotAliveException();
		}
		catch(InvalidTransactionException e){
			Trace.info("RM::queryCarsPrice(" + xid + ", " + location  + ") is an invalid transaction");
			throw e;
		}
		catch(DeadlockException de){
			Trace.info("RM::queryCarsPrice(" + xid + ", " + location  + ")  get deadlock, now going to abort this transaction");
			try {
				TM.abort(xid);
			}catch (InvalidTransactionException ie){
				Trace.info("RM::queryCarsPrice(" + xid + ", " + location  + ")  is an invalid transaction");
				throw ie;
			}
			throw de;
		}finally{
			TM.transactionSuspend(xid);
		}
		return -1;
	}

	// Returns room price at this location
	public int queryRoomsPrice(int xid, String location) throws RemoteException,
			InvalidTransactionException, DeadlockException, TranscationAbortedException, RMNotAliveException {
		Trace.info("RM::queryRoomsPrice(" + xid + ", " + location +") called");
		try{
			TM.transactionInvoke(xid);
			//get read lock
			if(LM.Lock(xid,"room-"+location,TransactionLockObject.LockType.LOCK_READ)){
				// have read lock
				TM.addRM(xid, Transaction.RM.RM_R);
				return m_resourceManager_r.queryRoomsPrice(xid, location);
			}
		}
		catch(RMNotAliveException rme){
			Trace.info("RM not alive exception");
			throw rme;
		}
		catch (RemoteException e){
			TM.setAlive(false);
//			reconnect();
			throw new RMNotAliveException();
		}
		catch(InvalidTransactionException e){
			Trace.info("RM::queryRoomsPrice(" + xid + ", " + location + ") is an invalid transaction");
			throw e;
		}
		catch(DeadlockException de){
			Trace.info("RM::queryRoomsPrice(" + xid + ", " + location + ") get deadlock, now going to abort this transaction");
			try {
				TM.abort(xid);
			}catch (InvalidTransactionException ie){
				Trace.info("RM::queryRoomsPrice(" + xid + ", " + location + ") is an invalid transaction");
				throw ie;
			}
			throw de;
		}finally{
			TM.transactionSuspend(xid);
		}
		return -1;
	}

	public String queryCustomerInfo(int xid, int customerID) throws RemoteException,
			InvalidTransactionException, DeadlockException, TranscationAbortedException, RMNotAliveException {
		Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID +") called");

		try{
			TM.transactionInvoke(xid);
			//get read lock
			if(LM.Lock(xid,"customer-"+customerID,TransactionLockObject.LockType.LOCK_READ)){
				// have read lock
				TM.addRM(xid, Transaction.RM.RM_CUS);
				return m_resourceManager_cus.queryCustomerInfo(xid, customerID);
			}
		}
		catch(RMNotAliveException rme){
			Trace.info("RM not alive exception");
			throw rme;
		}
		catch (RemoteException e){
			TM.setAlive(false);
//			reconnect();
			throw new RMNotAliveException();
		}
		catch(InvalidTransactionException e){
			Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ") is an invalid transaction");
			throw e;
		}
		catch(DeadlockException de){
			Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ") get deadlock, now going to abort this transaction");
			try {
				TM.abort(xid);
			}catch (InvalidTransactionException ie){
				Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ") is an invalid transaction");
				throw ie;
			}
			throw de;
		}finally{
			TM.transactionSuspend(xid);
		}
		return null;
	}

	public int newCustomer(int xid) throws RemoteException,
			InvalidTransactionException, DeadlockException, TranscationAbortedException, RMNotAliveException {
		Trace.info("RM::newCustomer(" + xid + ") called");

		try{
			TM.transactionInvoke(xid);
			int customerID = m_resourceManager_cus.newCustomer(xid);
			//get write lock
			if(LM.Lock(xid,"customer-"+customerID,TransactionLockObject.LockType.LOCK_WRITE)){
				// have write lock
				TM.addRM(xid, Transaction.RM.RM_CUS);
				TM.addUndoOperation(xid, new undoOperation(undoOperation.undoCommandType.Delete_Customer, Integer.toString(customerID)));
				return customerID;
			}
		}
		catch(RMNotAliveException rme){
			Trace.info("RM not alive exception");
			throw rme;
		}
		catch (RemoteException e){
			TM.setAlive(false);
//			reconnect();
			throw new RMNotAliveException();
		}
		catch(InvalidTransactionException e){
			Trace.info("RM::newCustomer(" + xid + ") is an invalid transaction");
			throw e;
		}
		catch(DeadlockException de){
			Trace.info("RM::newCustomer(" + xid +  ") get deadlock, now going to abort this transaction");
			try {
				TM.abort(xid);
			}catch (InvalidTransactionException ie){
				Trace.info("RM::newCustomer(" + xid +") is an invalid transaction");
				throw ie;
			}
			throw de;
		}finally{
			TM.transactionSuspend(xid);
		}
		return -1;
	}

	public boolean newCustomer(int xid, int customerID) throws RemoteException,
			DeadlockException, InvalidTransactionException, TranscationAbortedException, RMNotAliveException {
		Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") called");

		try{
			TM.transactionInvoke(xid);
			//get write lock
			if(LM.Lock(xid,"customer-"+customerID,TransactionLockObject.LockType.LOCK_WRITE)){
				// have write lock
				TM.addRM(xid, Transaction.RM.RM_CUS);
				boolean success = m_resourceManager_cus.newCustomer(xid,customerID);
				//if add successful, add to undo list
				if(success)
					TM.addUndoOperation(xid, new undoOperation(undoOperation.undoCommandType.Delete_Customer, Integer.toString(customerID)));
				return success;
			}
		}
		catch(RMNotAliveException rme){
			Trace.info("RM not alive exception");
			throw rme;
		}
		catch (RemoteException e){
			TM.setAlive(false);
//			reconnect();
			throw new RMNotAliveException();
		}
		catch(InvalidTransactionException e){
			Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") is an invalid transaction");
			throw e;
		}
		catch(DeadlockException de){
			Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") get deadlock, now going to abort this transaction");
			try {
				TM.abort(xid);
			}catch (InvalidTransactionException ie){
				Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") is an invalid transaction");
				throw ie;
			}
			throw de;
		}finally{
			TM.transactionSuspend(xid);
		}

		return false;
	}

	public boolean deleteCustomer(int xid, int customerID) throws RemoteException,
			InvalidTransactionException, DeadlockException, TranscationAbortedException, RMNotAliveException {
		Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");
		try{
			TM.transactionInvoke(xid);
			//get write lock
			if(LM.Lock(xid,"customer-"+customerID,TransactionLockObject.LockType.LOCK_WRITE)){
				// have write lock
				TM.addRM(xid, Transaction.RM.RM_CUS);
				HashMap<String, Integer> reservations = m_resourceManager_cus.getCustomerReservations(xid, customerID);
				for(String reservationKey: reservations.keySet()){
					LM.Lock(xid, reservationKey, TransactionLockObject.LockType.LOCK_WRITE);
				}

				for(String reservationKey: reservations.keySet()){
					ReservableItem item;
					if(reservationKey.charAt(0) == 'c'){
						item = m_resourceManager_c.getCar(xid, reservationKey.substring(4));
						TM.addUndoOperation(xid, new undoOperation(undoOperation.undoCommandType.Set_Car, item));
						m_resourceManager_c.reduceReservations(xid, reservationKey, reservations.get(reservationKey));
					}
					else if(reservationKey.charAt(0) == 'f'){
						item = m_resourceManager_f.getFlight(xid, Integer.parseInt(reservationKey.substring(7)));
						TM.addUndoOperation(xid, new undoOperation(undoOperation.undoCommandType.Set_Flight, item));
						m_resourceManager_f.reduceReservations(xid, reservationKey, reservations.get(reservationKey));
					}
					else{
						item = m_resourceManager_r.getRoom(xid, reservationKey.substring(5));
						TM.addUndoOperation(xid, new undoOperation(undoOperation.undoCommandType.Set_Room, item));
						m_resourceManager_r.reduceReservations(xid, reservationKey, reservations.get(reservationKey));
					}
				}

				Customer customer = m_resourceManager_cus.getCustomer(xid,customerID);
				//if add successful, add to undo list
				if(customer != null)
					TM.addUndoOperation(xid, new undoOperation(undoOperation.undoCommandType.Set_Customer, customer));
				return m_resourceManager_cus.deleteCustomer(xid,customerID);
			}
		}
		catch(RMNotAliveException rme){
			Trace.info("RM not alive exception");
			throw rme;
		}
		catch (RemoteException e){
			TM.setAlive(false);
//			reconnect();
			throw new RMNotAliveException();
		}
		catch(InvalidTransactionException e){
			Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") is an invalid transaction");
			throw e;
		}
		catch(DeadlockException de){
			Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") get deadlock, now going to abort this transaction");
			try {
				TM.abort(xid);
			}catch (InvalidTransactionException ie){
				Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") is an invalid transaction");
				throw ie;
			}
			throw de;
		}finally{
			TM.transactionSuspend(xid);
		}
		return false;
	}

	// Adds flight reservation to this customer
	public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException,
			InvalidTransactionException, DeadlockException, TranscationAbortedException, RMNotAliveException{
		try{
			TM.transactionInvoke(xid);
			if(LM.Lock(xid,"flight-"+flightNum,TransactionLockObject.LockType.LOCK_WRITE) &&
					LM.Lock(xid,"customer-"+customerID,TransactionLockObject.LockType.LOCK_WRITE)) {
				TM.addRM(xid, Transaction.RM.RM_CUS);
				if (!m_resourceManager_cus.checkCustomer(xid, customerID)) {
					return false;
				}
				TM.addRM(xid, Transaction.RM.RM_F);
				ReservableItem flight = m_resourceManager_f.getFlight(xid, flightNum);
				if (!m_resourceManager_f.reserveFlight(xid, customerID, flightNum)) {
					return false;
				}
				TM.addUndoOperation(xid, new undoOperation(undoOperation.undoCommandType.Set_Flight, flight));
				Customer customer = m_resourceManager_cus.getCustomer(xid, customerID);
				TM.addRM(xid, Transaction.RM.RM_CUS);
				if (!m_resourceManager_cus.reserveItem_cus(xid, customerID, m_resourceManager_f.getFlightKey(xid, flightNum),
						String.valueOf(flightNum), m_resourceManager_f.queryFlightPrice(xid, flightNum))) {
					return false;
				}
				TM.addUndoOperation(xid, new undoOperation(undoOperation.undoCommandType.Set_Customer, customer));
				return true;
			}
		}
		catch(RMNotAliveException rme){
			Trace.info("RM not alive exception");
			throw rme;
		}
		catch (RemoteException e){
			TM.setAlive(false);
//			reconnect();
			throw new RMNotAliveException();
		}
		catch(InvalidTransactionException e){
			Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") is an invalid transaction");
			throw e;
		}
		catch(DeadlockException de){
			Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") get deadlock, now going to abort this transaction");
			try {
				TM.abort(xid);
			}catch (InvalidTransactionException ie){
				Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") is an invalid transaction");
				throw ie;
			}
			throw de;
		}finally{
			TM.transactionSuspend(xid);
		}
		return false;
	}

	// Adds car reservation to this customer
	public boolean reserveCar(int xid, int customerID, String location) throws RemoteException,
			InvalidTransactionException, DeadlockException, TranscationAbortedException , RMNotAliveException{
		try {
			TM.transactionInvoke(xid);
			if(LM.Lock(xid,"car-"+location,TransactionLockObject.LockType.LOCK_WRITE) &&
					LM.Lock(xid,"customer-"+customerID,TransactionLockObject.LockType.LOCK_WRITE)) {
				TM.addRM(xid, Transaction.RM.RM_CUS);
				if (!m_resourceManager_cus.checkCustomer(xid, customerID))
					return false;
				TM.addRM(xid, Transaction.RM.RM_C);
				ReservableItem car = m_resourceManager_c.getCar(xid, location);
				if (!m_resourceManager_c.reserveCar(xid, customerID, location))
					return false;
				TM.addUndoOperation(xid, new undoOperation(undoOperation.undoCommandType.Set_Car, car));
				Customer customer = m_resourceManager_cus.getCustomer(xid, customerID);
				TM.addRM(xid, Transaction.RM.RM_CUS);
				if (!m_resourceManager_cus.reserveItem_cus(xid, customerID, m_resourceManager_c.getCarKey(xid, location),
						location, m_resourceManager_c.queryCarsPrice(xid, location)))
					return false;
				TM.addUndoOperation(xid, new undoOperation(undoOperation.undoCommandType.Set_Customer, customer));
				return true;
			}
		}
		catch(RMNotAliveException rme){
			Trace.info("RM not alive exception");
			throw rme;
		}
		catch (RemoteException e){
			TM.setAlive(false);
//			reconnect();
			throw new RMNotAliveException();
		}
		catch(InvalidTransactionException e){
			Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") is an invalid transaction");
			throw e;
		}
		catch(DeadlockException de){
			Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") get deadlock, now going to abort this transaction");
			try {
				TM.abort(xid);
			}catch (InvalidTransactionException ie){
				Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") is an invalid transaction");
				throw ie;
			}
			throw de;
		}finally{
			TM.transactionSuspend(xid);
		}
		return false;
	}

	// Adds room reservation to this customer
	public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException,
			DeadlockException, InvalidTransactionException, TranscationAbortedException , RMNotAliveException{
		try {
			TM.transactionInvoke(xid);
			if(LM.Lock(xid,"room-"+location,TransactionLockObject.LockType.LOCK_WRITE) &&
					LM.Lock(xid,"customer-"+customerID,TransactionLockObject.LockType.LOCK_WRITE)) {
				TM.addRM(xid, Transaction.RM.RM_CUS);
				if (!m_resourceManager_cus.checkCustomer(xid, customerID))
					return false;
				TM.addRM(xid, Transaction.RM.RM_R);
				ReservableItem room = m_resourceManager_r.getRoom(xid, location);
				if (!m_resourceManager_r.reserveRoom(xid, customerID, location))
					return false;
				TM.addUndoOperation(xid, new undoOperation(undoOperation.undoCommandType.Set_Room, room));
				Customer customer = m_resourceManager_cus.getCustomer(xid, customerID);
				TM.addRM(xid, Transaction.RM.RM_CUS);
				if (!m_resourceManager_cus.reserveItem_cus(xid, customerID, m_resourceManager_r.getRoomKey(xid, location),
						location, m_resourceManager_r.queryRoomsPrice(xid, location)))
					return false;
				TM.addUndoOperation(xid, new undoOperation(undoOperation.undoCommandType.Set_Customer, customer));
				return true;
			}
		}
		catch(RMNotAliveException rme){
			Trace.info("RM not alive exception");
			throw rme;
		}
		catch (RemoteException e){
			TM.setAlive(false);
//			reconnect();
			throw new RMNotAliveException();
		}
		catch(InvalidTransactionException e){
			Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") is an invalid transaction");
			throw e;
		}
		catch(DeadlockException de){
			Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") get deadlock, now going to abort this transaction");
			try {
				TM.abort(xid);
			}catch (InvalidTransactionException ie){
				Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") is an invalid transaction");
				throw ie;
			}
			throw de;
		}finally{
			TM.transactionSuspend(xid);
		}
		return false;
	}

	// Reserve bundle 
	public boolean bundle(int xid, int customerId, Vector<String> flightNumbers, String location, boolean car, boolean room) throws
			RemoteException, InvalidTransactionException, DeadlockException, TranscationAbortedException, RMNotAliveException
	{
        Boolean flag = true;
		HashMap<String, Integer> flights = new HashMap<String, Integer>();

		for(String flightNumber: flightNumbers){
			if(flights.containsKey(flightNumber)){
				flights.put(flightNumber,flights.get(flightNumber)+1);
			}else{
				flights.put(flightNumber,1);
			}
		}

        for(String flightNumber: flightNumbers){
			if(queryFlight(xid, Integer.parseInt(flightNumber)) < flights.get(flightNumber) ||
					!LM.Lock(xid,"flight-"+flightNumber,TransactionLockObject.LockType.LOCK_WRITE)){
				return false;
			}
		}

		if(car){
			if(queryCars(xid, location) <= 0 ||
					!LM.Lock(xid, "car-" + location, TransactionLockObject.LockType.LOCK_WRITE)){
				return false;
			}
		}
		if(room){
			if(queryRooms(xid, location) <= 0 ||
					!LM.Lock(xid, "room-" + location, TransactionLockObject.LockType.LOCK_WRITE)){
				return false;
			}
		}

		for(String flightNumber: flightNumbers){
            if(!reserveFlight(xid, customerId, Integer.parseInt(flightNumber))){
            	flag = false;
			}
        }
        if(car){
            if(!reserveCar(xid, customerId, location)){
            	flag = false;
			}
        }
        if(room){
            if(!reserveRoom(xid, customerId, location)){
            	flag = false;
			}
        }
		return flag;

	}

	public String getName() throws RemoteException
	{
		return m_name;
	}

	@Override
	public int start() throws RemoteException {
		return TM.start();
	}

	@Override
	public boolean commit(int transactionId) throws RemoteException, TranscationAbortedException,
			InvalidTransactionException, TransactionCommitFailException , RMNotAliveException{
		Trace.info("MW:commit(xid:" + transactionId +") called");
		try {
			TM.transactionInvoke(transactionId);
			if (TM.commit(transactionId)) {
				Trace.info("MW:commit(xid:" + transactionId + ") success");
				return true;
			}
			Trace.info("MW:commit(xid:" + transactionId + ") fail");
			TM.transactionSuspend(transactionId);
		}
		catch (RemoteException|RMNotAliveException e){
			TM.setAlive(false);
			reconnect();
			throw new RMNotAliveException();
		}
		return false;
	}

	@Override
	public void abort(int transactionId) throws RemoteException, InvalidTransactionException, RMNotAliveException {
		try {
			TM.transactionInvoke(transactionId);
		}
		catch(RMNotAliveException rme){
			Trace.info("RM not alive exception");
			throw rme;
		}
		catch(TranscationAbortedException e){
			Trace.info("MW:abort(xid:" + transactionId + ") abort a transaction already aborted");
		}
		try {
			if (TM.abort(transactionId)) {
				Trace.info("MW:abort(xid:" + transactionId + ") success");
			} else {
				TM.transactionSuspend(transactionId);
				Trace.info("MW:abort(xid:" + transactionId + ") failed");
			}
		}
		catch (RemoteException e){
			TM.setAlive(false);
			reconnect();
			throw new RMNotAliveException();
		}
	}

	@Override
	public boolean shutdown() throws RemoteException{
		return false;
	}

	@Override
	public boolean isAbort(int xid) throws RemoteException{
		return TM.isAbort(xid);
	}

	@Override
	public void abortRequest(int xid){
		try {
			m_resourceManager_c.removeTransactionFromHashmap(xid);
		}
		catch (RemoteException e){
			System.out.println("MW:car server is not alive");
		}
		try {
			m_resourceManager_cus.removeTransactionFromHashmap(xid);
		}
		catch (RemoteException e){
			System.out.println("MW:customer server is not alive");
		}
		try {
			m_resourceManager_f.removeTransactionFromHashmap(xid);
		}
		catch (RemoteException e){
			System.out.println("MW:flight server is not alive");
		}
		try {
			m_resourceManager_r.removeTransactionFromHashmap(xid);
		}
		catch (RemoteException e){
			System.out.println("MW:room server is not alive");
		}
		TM.abortRequest(xid);
		return;
	}

	public void abortRequestFromTM(int xid){
		try {
			m_resourceManager_c.removeTransactionFromHashmap(xid);
		}
		catch (RemoteException e){
			System.out.println("MW:car server is not alive");
		}
		try {
			m_resourceManager_cus.removeTransactionFromHashmap(xid);
		}
		catch (RemoteException e){
			System.out.println("MW:customer server is not alive");
		}
		try {
			m_resourceManager_f.removeTransactionFromHashmap(xid);
		}
		catch (RemoteException e){
			System.out.println("MW:flight server is not alive");
		}
		try {
			m_resourceManager_r.removeTransactionFromHashmap(xid);
		}
		catch (RemoteException e){
			System.out.println("MW:room server is not alive");
		}
		return;
	}

	public ReservableItem getFlight(int xid, int flightNum) throws RemoteException{
		Trace.info("MW:getFlight (xid:" + xid + ", flightNum:" + flightNum + ") called");
		return (ReservableItem) m_resourceManager_f.getFlight(xid, flightNum).clone();
	}

	public ReservableItem getCar(int xid, String location) throws RemoteException{
		Trace.info("MW:getCar (xid:" + xid + ", location:" + location + ") called");
		return (ReservableItem) m_resourceManager_c.getCar(xid, location).clone();
	}

	public ReservableItem getRoom(int xid, String location) throws RemoteException{
		Trace.info("MW:getRoom (xid:" + xid + ", location:" + location + ") called");
		return (ReservableItem) m_resourceManager_r.getRoom(xid, location).clone();
	}

	public Customer getCustomer(int xid, int customerId) throws RemoteException{
		Trace.info("MW:getCustomer (xid:" + xid + ", customerId:" + customerId + ") called");
		return (Customer) m_resourceManager_cus.getCustomer(xid, customerId).clone();
	}

	public void setFlight(int xid, ReservableItem obj) throws RemoteException{
		Trace.info("MW:setFlight (xid:" + xid + ", flight:<" + obj + ">) called");
		m_resourceManager_f.setFlight(xid, obj);
	}

	public void setCar(int xid, ReservableItem obj) throws RemoteException{
		Trace.info("MW:setCar (xid:" + xid + ", car:<" + obj + ">) called");
		m_resourceManager_c.setCar(xid, obj);
	}

	public void setRoom(int xid, ReservableItem obj) throws RemoteException{
		Trace.info("MW:setRoom (xid:" + xid + ", room:<" + obj + ">) called");
		m_resourceManager_r.setRoom(xid, obj);
	}


	public void setCustomer(int xid, Customer customer) throws RemoteException{
		Trace.info("MW:setCustomer (xid:" + xid + ", customer:<" + customer + ">) called");
		m_resourceManager_cus.setCustomer(xid, customer);
	}

	public void undoAddFlights(int xid, int flightNum)throws RemoteException{
		Trace.info("MW:undoAddFlight (xid:" + xid + ", flight:" + flightNum +") called");
		m_resourceManager_f.deleteFlight(xid, flightNum);
	}

	public void undoAddCars(int xid, String location)throws RemoteException{
		Trace.info("MW:undoAddCars (xid:" + xid + ", location:" + location +") called");
		m_resourceManager_c.deleteCars(xid, location);
	}

	public void undoAddRooms(int xid, String location)throws RemoteException{
		Trace.info("MW:undoAddRooms (xid:" + xid + ", location:" + location +") called");
		m_resourceManager_r.deleteRooms(xid, location);
	}

	public void undoAddCustomers(int xid, int customerId)throws RemoteException{
		Trace.info("MW:undoAddCustomers (xid:" + xid + ", customer:" + customerId +") called");
		m_resourceManager_cus.deleteCustomer(xid, customerId);
	}

	public boolean allAlive() {
		boolean flag = true;
		if(!alive("customers"))
			flag = false;
		if(!alive("cars"))
			flag = false;
		if(!alive("flights"))
			flag = false;
		if(!alive("rooms"))
			flag = false;
		return flag;
	}

	public boolean alive(String rm){
		try {
			if (rm.equals("customers")) {
				if (m_resourceManager_cus.alive())
					return true;
			}
			if (rm.equals("cars")) {
				if (m_resourceManager_c.alive())
					return true;
			}
			if (rm.equals("flights")) {
				if (m_resourceManager_f.alive())
					return true;
			}
			if (rm.equals("rooms")) {
				if (m_resourceManager_r.alive())
					return true;
			}
		}
		catch (RemoteException e){
			Trace.info("MW: Resource Manager:" + rm + "is not available.");
		}
		return false;
	}

	public boolean prepareCommit(String rm, int xid) throws RemoteException{
		if(rm.equals("customers")){
			if(m_resourceManager_cus.prepareCommit(xid)) {
				Trace.info("MW: Resource Manager:" + rm + "vote yes for transaction <" + xid + ">");
				return true;
			}
		}
		if(rm.equals("cars")){
			if(m_resourceManager_c.prepareCommit(xid)) {
				Trace.info("MW: Resource Manager:" + rm + "vote yes for transaction <" + xid + ">");
				return true;
			}
		}
		if(rm.equals("flights")){
			if(m_resourceManager_f.prepareCommit(xid)) {
				Trace.info("MW: Resource Manager:" + rm + "vote yes for transaction <" + xid + ">");
				return true;
			}
		}
		if(rm.equals("rooms")){
			if(m_resourceManager_r.prepareCommit(xid)) {
				Trace.info("MW: Resource Manager:" + rm + "vote yes for transaction <" + xid + ">");
				return true;
			}
		}
		Trace.info("MW: Resource Manager:" + rm + "vote no for transaction <" + xid + ">");
		return false;
	}

	public boolean sendResult(int xid, String rm, boolean result) throws RemoteException{
		String info;
		if(result)
			info = "commit";
		else
			info = "abort";
		if(rm.equals("customers")){
			if(m_resourceManager_cus.receiveResult(xid, result))
				Trace.info("MW: Resource Manager:" + rm + " receive " + info + " for transaction <" + xid + ">");
			return true;
		}
		if(rm.equals("cars")){
			if(m_resourceManager_c.receiveResult(xid, result))
				Trace.info("MW: Resource Manager:" + rm + " receive " + info + " for transaction <" + xid + ">");
			return true;
		}
		if(rm.equals("flights")){
			if(m_resourceManager_f.receiveResult(xid, result))
				Trace.info("MW: Resource Manager:" + rm + " receive " + info + " for transaction <" + xid + ">");
			return true;
		}
		if(rm.equals("rooms")){
			if(m_resourceManager_r.receiveResult(xid, result))
				Trace.info("MW: Resource Manager:" + rm + " receive " + info + " for transaction <" + xid + ">");
			return true;
		}
		Trace.info("MW: Resource Manager:" + rm + " don't answer for result request <" + xid + ">");
		return false;
	}

	public void startTransaction(int xid, String rm){
		try {
			if (rm.equals("customers")) {
				m_resourceManager_cus.startTransaction(xid);
			}
			if (rm.equals("cars")) {
				m_resourceManager_c.startTransaction(xid);
			}
			if (rm.equals("flights")) {
				m_resourceManager_f.startTransaction(xid);
			}
			if (rm.equals("rooms")) {
				m_resourceManager_r.startTransaction(xid);
			}
			Trace.info("MW: Resource Manager:" + rm + "start transaction <" + xid + ">");
		}
		catch (RemoteException e){

		}
	}

	public boolean isAlive(){
		return TM.isAlive();
	}

	public void crashResourceManager(String rm, int mode) throws RemoteException{
		if (rm.equals("customers")) {
			m_resourceManager_cus.crashResourceManager(mode);
		}
		if (rm.equals("cars")) {
			m_resourceManager_c.crashResourceManager(mode);
		}
		if (rm.equals("flights")) {
			m_resourceManager_f.crashResourceManager(mode);
		}
		if (rm.equals("rooms")) {
			m_resourceManager_r.crashResourceManager(mode);
		}
		Trace.info("MW: Resource Manager: " + rm + " crash mode: " + mode + " set.");
	}

	public void crashMiddleware(int mode) throws RemoteException{
		TM.crashMiddleware(mode);
		return;
	}

	public void resetCrashes() throws RemoteException{
		TM.resetCrashes();
		m_resourceManager_r.resetCrashes();
		m_resourceManager_f.resetCrashes();
		m_resourceManager_r.resetCrashes();
		m_resourceManager_cus.resetCrashes();
		Trace.info("MW: TM and RM reset crashed");
		return;
	}
}
 
