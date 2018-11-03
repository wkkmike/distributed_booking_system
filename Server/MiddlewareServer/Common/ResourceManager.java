// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package MiddlewareServer.Common;

import Server.Interface.*;
import Server.Common.*;


import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;

public class ResourceManager implements IResourceManager
{
	protected String m_name = "";
	protected RMHashMap m_data = new RMHashMap();
    private static String s_rmiPrefix = "group15";

	public IResourceManager m_resourceManager_f = null;
	public IResourceManager m_resourceManager_c = null;
	public IResourceManager m_resourceManager_r = null;
	public IResourceManager m_resourceManager_cus = null;

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
//		connect();
		return m_resourceManager_f.addFlight(xid, flightNum, flightSeats, flightPrice);
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

	@Override
	public boolean checkCustomer(int xid, int customerID) throws RemoteException {
		return false;
	}

	@Override
	public boolean reserveItem_cus(int xid, int customerID, String key, String location, int price) throws RemoteException {
		return false;
	}

	@Override
	public String getFlightKey(int xid, int number) throws RemoteException {
		return null;
	}

	@Override
	public String getCarKey(int xid, String location) throws RemoteException {
		return null;
	}

	@Override
	public String getRoomKey(int xid, String location) throws RemoteException {
		return null;
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
}
 
