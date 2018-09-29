// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package MiddlewareServer.Common;

import MiddlewareServer.Interface.*;
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

    private static String s_serverHost_f = "localhost";
    private static int s_serverPort_f = 1099;
    private static String s_serverName_f = "Flights";

    private static String s_serverHost_c = "localhost";
    private static int s_serverPort_c = 1099;
    private static String s_serverName_c = "Cars";

    private static String s_serverHost_r = "localhost";
    private static int s_serverPort_r = 1099;
    private static String s_serverName_r = "Rooms";

	private static String s_serverHost_cus = "localhost";
	private static int s_serverPort_cus = 1099;
	private static String s_serverName_cus = "customers";

	public ResourceManager(String p_name)
	{
		m_name = p_name;
	}

    IResourceManager m_resourceManager_f = null;
	IResourceManager m_resourceManager_c = null;
	IResourceManager m_resourceManager_r = null;
	IResourceManager m_resourceManager_cus = null;

	public boolean connect(){
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new SecurityManager());
		}
		try {
			if(m_resourceManager_f== null)
				connectServer(s_serverHost_f, s_serverPort_f, s_serverName_f, m_resourceManager_f);
			if(m_resourceManager_c== null)
				connectServer(s_serverHost_c, s_serverPort_c, s_serverName_c, m_resourceManager_c);
			if(m_resourceManager_r== null)
				connectServer(s_serverHost_r, s_serverPort_r, s_serverName_r, m_resourceManager_r);
			if(m_resourceManager_cus== null)
				connectServer(s_serverHost_cus, s_serverPort_cus, s_serverName_cus, m_resourceManager_cus);
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}
		return true;
	}


	public void connectServer(String server, int port, String name,IResourceManager m_resourceManager)
    {
        try {
            boolean first = true;
            while (true) {
                try {
                    Registry registry = LocateRegistry.getRegistry(server, port);
                    m_resourceManager = (IResourceManager)registry.lookup(s_rmiPrefix + name);
                    System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
                    break;
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
    }


	// Create a new flight, or add seats to existing flight
	// NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
	public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException
	{
		Trace.info("RM::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");
		connect();
		return m_resourceManager_f.addFlight(xid, flightNum, flightSeats, flightPrice);
	}

	// Create a new car location or add cars to an existing location
	// NOTE: if price <= 0 and the location already exists, it maintains its current price
	public boolean addCars(int xid, String location, int count, int price) throws RemoteException
	{
		Trace.info("RM::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ") called");

        connect();
		return m_resourceManager_c.addCars(xid, location, count, price);
	}

	// Create a new room location or add rooms to an existing location
	// NOTE: if price <= 0 and the room location already exists, it maintains its current price
	public boolean addRooms(int xid, String location, int count, int price) throws RemoteException
	{
		Trace.info("RM::addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
        connect();
		return m_resourceManager_r.addRooms(xid, location, count, price);
	}

	// Deletes flight
	public boolean deleteFlight(int xid, int flightNum) throws RemoteException
	{
        connect();
		return m_resourceManager_f.deleteFlight(xid, flightNum);
	}

	// Delete cars at a location
	public boolean deleteCars(int xid, String location) throws RemoteException
	{
		connect();
		return m_resourceManager_c.deleteCars(xid, location);
	}

	// Delete rooms at a location
	public boolean deleteRooms(int xid, String location) throws RemoteException
	{
		connect();
		return m_resourceManager_r.deleteRooms(xid, location);
	}

	// Returns the number of empty seats in this flight
	public int queryFlight(int xid, int flightNum) throws RemoteException
	{
		connect();
		return m_resourceManager_f.queryFlight(xid, flightNum);
	}

	// Returns the number of cars available at a location
	public int queryCars(int xid, String location) throws RemoteException
	{
		connect();
		return m_resourceManager_c.queryCars(xid, location);
	}

	// Returns the amount of rooms available at a location
	public int queryRooms(int xid, String location) throws RemoteException
	{
		connect();
		return m_resourceManager_r.queryRooms(xid, location);
	}

	// Returns price of a seat in this flight
	public int queryFlightPrice(int xid, int flightNum) throws RemoteException
	{
		connect();
		return m_resourceManager_f.queryFlightPrice(xid, flightNum);
	}

	// Returns price of cars at this location
	public int queryCarsPrice(int xid, String location) throws RemoteException
	{
		connect();
		return m_resourceManager_c.queryCarsPrice(xid, location);
	}

	// Returns room price at this location
	public int queryRoomsPrice(int xid, String location) throws RemoteException
	{
		connect();
		return m_resourceManager_r.queryRoomsPrice(xid, location);
	}

	public String queryCustomerInfo(int xid, int customerID) throws RemoteException
	{
		connect();
		return m_resourceManager_cus.queryCustomerInfo(xid, customerID);
	}

	public int newCustomer(int xid) throws RemoteException
	{
		connect();
		return m_resourceManager_cus.newCustomer(xid);
	}

	public boolean newCustomer(int xid, int customerID) throws RemoteException
	{
		connect();
		return m_resourceManager_cus.newCustomer(xid,customerID);
	}

	public boolean deleteCustomer(int xid, int customerID) throws RemoteException
	{
		Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");
		connect();
		return m_resourceManager_cus.deleteCustomer(xid,customerID);
	}

	// Adds flight reservation to this customer
	public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException
	{
		connect();
		return m_resourceManager_f.reserveFlight(xid, customerID, flightNum);
	}

	// Adds car reservation to this customer
	public boolean reserveCar(int xid, int customerID, String location) throws RemoteException
	{
		connect();
		return m_resourceManager_c.reserveCar(xid, customerID, location);
	}

	// Adds room reservation to this customer
	public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException
	{
		connect();
		return m_resourceManager_r.reserveRoom(xid, customerID, location);
	}

	// Reserve bundle 
	public boolean bundle(int xid, int customerId, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException
	{
		return false;
	}

	public String getName() throws RemoteException
	{
		return m_name;
	}
}
 
