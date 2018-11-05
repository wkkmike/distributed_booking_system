// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package MiddlewareServer.Common;

import MiddlewareServer.Interface.IMiddleware;
import MiddlewareServer.LockManager.DeadlockException;
import MiddlewareServer.LockManager.LockManager;
import MiddlewareServer.LockManager.TransactionLockObject;
import MiddlewareServer.TransactionManager.InvalidTransactionException;
import MiddlewareServer.TransactionManager.TransactionManager;
import MiddlewareServer.TransactionManager.TranscationAbortedException;
import Server.Interface.*;
import Server.Common.*;


import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;

public class ResourceManager implements IMiddleware
{
	protected String m_name = "";
    private static String s_rmiPrefix = "group15";

	public IResourceManager m_resourceManager_f = null;
	public IResourceManager m_resourceManager_c = null;
	public IResourceManager m_resourceManager_r = null;
	public IResourceManager m_resourceManager_cus = null;

	//changes start
	private LockManager LM = new LockManager();
	private TransactionManager TM = new TransactionManager();
	//changes end

	public ResourceManager(String p_name)
	{
		m_name = p_name;
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
	public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException
	{

		Trace.info("RM::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");
		try{
			if(LM.Lock(xid, "flight-" + flightNum, TransactionLockObject.LockType.LOCK_WRITE)) {
				return m_resourceManager_f.addFlight(xid, flightNum, flightSeats, flightPrice);
			}
		}catch(DeadlockException de){
			Trace.info("RM::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") get deadlock, now going to abort this transaction");
			try {
				TM.abort(xid);
			}catch (InvalidTransactionException ie){
				Trace.info("RM::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") is an invalid transaction");
			}
		}
		return false;
	}

	// Create a new car location or add cars to an existing location
	// NOTE: if price <= 0 and the location already exists, it maintains its current price
	public boolean addCars(int xid, String location, int count, int price) throws RemoteException
	{
		Trace.info("RM::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
		return  m_resourceManager_c.addCars(xid, location, count, price);
	}

	// Create a new room location or add rooms to an existing location
	// NOTE: if price <= 0 and the room location already exists, it maintains its current price
	public boolean addRooms(int xid, String location, int count, int price) throws RemoteException
	{
		Trace.info("RM::addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
		return m_resourceManager_r.addRooms(xid, location, count, price);
	}

	// Deletes flight
	public boolean deleteFlight(int xid, int flightNum) throws RemoteException
	{
		return m_resourceManager_f.deleteFlight(xid, flightNum);
	}

	// Delete cars at a location
	public boolean deleteCars(int xid, String location) throws RemoteException
	{
		return m_resourceManager_c.deleteCars(xid, location);
	}

	// Delete rooms at a location
	public boolean deleteRooms(int xid, String location) throws RemoteException
	{
		return m_resourceManager_r.deleteRooms(xid, location);
	}

	// Returns the number of empty seats in this flight
	public int queryFlight(int xid, int flightNum) throws RemoteException
	{
		return m_resourceManager_f.queryFlight(xid, flightNum);
	}

	// Returns the number of cars available at a location
	public int queryCars(int xid, String location) throws RemoteException
	{
//		connect();
		return m_resourceManager_c.queryCars(xid, location);
	}

	// Returns the amount of rooms available at a location
	public int queryRooms(int xid, String location) throws RemoteException
	{
		return m_resourceManager_r.queryRooms(xid, location);
	}

	// Returns price of a seat in this flight
	public int queryFlightPrice(int xid, int flightNum) throws RemoteException
	{
		return m_resourceManager_f.queryFlightPrice(xid, flightNum);
	}

	// Returns price of cars at this location
	public int queryCarsPrice(int xid, String location) throws RemoteException
	{
		return m_resourceManager_c.queryCarsPrice(xid, location);
	}

	// Returns room price at this location
	public int queryRoomsPrice(int xid, String location) throws RemoteException
	{
		return m_resourceManager_r.queryRoomsPrice(xid, location);
	}

	public String queryCustomerInfo(int xid, int customerID) throws RemoteException
	{
		return m_resourceManager_cus.queryCustomerInfo(xid, customerID);
	}

	public int newCustomer(int xid) throws RemoteException
	{
		return m_resourceManager_cus.newCustomer(xid);
	}

	public boolean newCustomer(int xid, int customerID) throws RemoteException
	{
		return m_resourceManager_cus.newCustomer(xid,customerID);
	}

	public boolean deleteCustomer(int xid, int customerID) throws RemoteException
	{
		Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");
		return m_resourceManager_cus.deleteCustomer(xid,customerID);
	}

	// Adds flight reservation to this customer
	public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException
	{
		if(!m_resourceManager_cus.checkCustomer(xid, customerID))
			return false;
		if(!m_resourceManager_f.reserveFlight(xid, customerID, flightNum))
			return false;
		if(!m_resourceManager_cus.reserveItem_cus(xid, customerID, m_resourceManager_f.getFlightKey(xid, flightNum),
				String.valueOf(flightNum), m_resourceManager_f.queryFlightPrice(xid, flightNum)))
			return false;
		return true;
	}

	// Adds car reservation to this customer
	public boolean reserveCar(int xid, int customerID, String location) throws RemoteException
	{
		if(!m_resourceManager_cus.checkCustomer(xid, customerID))
			return false;
		if(!m_resourceManager_c.reserveCar(xid, customerID, location))
			return false;
		if(!m_resourceManager_cus.reserveItem_cus(xid, customerID, m_resourceManager_c.getCarKey(xid, location),
				location, m_resourceManager_f.queryCarsPrice(xid, location)))
			return false;
		return true;
	}

	// Adds room reservation to this customer
	public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException
	{
		if(!m_resourceManager_cus.checkCustomer(xid, customerID))
			return false;
		if(!m_resourceManager_r.reserveRoom(xid, customerID, location))
			return false;
		if(!m_resourceManager_cus.reserveItem_cus(xid, customerID, m_resourceManager_r.getRoomKey(xid, location),
				location, m_resourceManager_r.queryRoomsPrice(xid, location)))
			return false;
		return true;
	}

	// Reserve bundle 
	public boolean bundle(int xid, int customerId, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException
	{
        Boolean flag = true;
		for(String flightNumber: flightNumbers){
			if(queryFlight(xid, Integer.parseInt(flightNumber)) <= 0){
				return false;
			}
		}
		if(car){
			if(queryCars(xid, location) <= 0){
				return false;
			}
		}
		if(room){
			if(queryRooms(xid, location) <= 0){
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
		if(flag) return true;
		return false;
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
	public boolean commit(int transactionId) throws RemoteException, TranscationAbortedException, InvalidTransactionException {
		return TM.commit(transactionId);
	}

	@Override
	public void abort(int transactionId) throws RemoteException, InvalidTransactionException {
		if (TM.abort(transactionId)) {
			Trace.info("MW:abort(xid:" + transactionId + ") success");
		}
		else{
			Trace.info("MW:abort(xid:" + transactionId + ") failed");
		}
		if(LM.UnlockAll(transactionId)){
			Trace.info("MW:Unlock all lock for xid:" + transactionId + " success");
		}
		else{
			Trace.info("MW:Unlock all lock for xid:" + transactionId + " failed");
		}
	}

	@Override
	public boolean shutdown() throws RemoteException {
		return TM.shutdown();
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
}
 
