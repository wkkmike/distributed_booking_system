// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.Common;

import MiddlewareServer.Interface.IMiddleware;
import Server.Interface.*;

import java.io.*;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.rmi.RemoteException;

public class ResourceManager implements IResourceManager
{
	protected String m_name = "";
	protected RMHashMap m_data = new RMHashMap();
	protected String fileAName;
	protected String fileBName;
	protected String logFileName;
	protected String masterRecordName;
	protected Boolean masterIsA;
	protected FileWriter masterWriter;
	protected FileWriter logWriter;
	protected HashMap<Integer, RMHashMap> dataHashMap = new HashMap<>();
	protected boolean alive = true;
	protected IMiddleware mw;
	private static String s_serverHost = "localhost";
	private static int s_serverPort = 1099;
	private static String s_serverName = "MiddlewareServer";
	private static String s_rmiPrefix = "group15";

	public ResourceManager(String p_name)
	{
		m_name = p_name;
		masterRecordName = "./" + p_name + "master";
		fileAName = "./" + p_name + "A";
		fileBName = "./" + p_name + "B";
		logFileName = "./" + p_name + "log";

		File masterRecord = new File(masterRecordName);
		// create file in disk
		if(!masterRecord.exists()){
			File fileA = new File(fileAName);
			File fileB = new File(fileBName);
			File logFile = new File(logFileName);
			try{
				masterRecord.createNewFile();
				fileA.delete();
				fileB.delete();
				logFile.delete();
				fileA.createNewFile();
				fileB.createNewFile();
				logFile.createNewFile();
				masterWriter = new FileWriter(masterRecordName, false);
				masterWriter.write("0 " + fileAName);
				logWriter = new FileWriter(logFileName);
				store(fileAName);
				store(fileBName);
				masterIsA = true;
				masterWriter.flush();
				//masterWriter.close();
			}
			catch(IOException e){
				System.out.println("Can't create file in disk for " + p_name);
			}
		}

		// Recover process
		else{
			alive = false;
			recover();
			alive = true;
		}
	}

	// Reads a data item
	protected RMItem readData(int xid, String key)
	{
		RMHashMap data = dataHashMap.get(xid);
		synchronized(data) {
			RMItem item = data.get(key);
			if (item != null) {
				return (RMItem)item.clone();
			}
			return null;
		}
	}

	// Writes a data item
	protected void writeData(int xid, String key, RMItem value)
	{
		RMHashMap data = dataHashMap.get(xid);
		synchronized(data) {
			data.put(key, value);
		}
	}

	// Remove the item out of storage
	protected void removeData(int xid, String key)
	{
		RMHashMap data = dataHashMap.get(xid);
		synchronized(data) {
			data.remove(key);
		}
	}

	// Deletes the encar item
	protected boolean deleteItem(int xid, String key)
	{
		Trace.info("RM::deleteItem(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem)readData(xid, key);
		// Check if there is such an item in the storage
		if (curObj == null)
		{
			Trace.warn("RM::deleteItem(" + xid + ", " + key + ") failed--item doesn't exist");
			return false;
		}
		else
		{
			if (curObj.getReserved() == 0)
			{
				removeData(xid, curObj.getKey());
				Trace.info("RM::deleteItem(" + xid + ", " + key + ") item deleted");
				return true;
			}
			else
			{
				Trace.info("RM::deleteItem(" + xid + ", " + key + ") item can't be deleted because some customers have reserved it");
				return false;
			}
		}
	}

	// Query the number of available seats/rooms/cars
	protected int queryNum(int xid, String key)
	{
		Trace.info("RM::queryNum(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem)readData(xid, key);
		int value = 0;  
		if (curObj != null)
		{
			value = curObj.getCount();
		}
		Trace.info("RM::queryNum(" + xid + ", " + key + ") returns count=" + value);
		return value;
	}    

	// Query the price of an item
	protected int queryPrice(int xid, String key)
	{
		Trace.info("RM::queryPrice(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem)readData(xid, key);
		int value = 0; 
		if (curObj != null)
		{
			value = curObj.getPrice();
		}
		Trace.info("RM::queryPrice(" + xid + ", " + key + ") returns cost=$" + value);
		return value;        
	}

	// Return a reservable item, used by TM for undo operation.
    // return null, if no such object.
	private ReservableItem getItem(int xid, String key){
	    Trace.info("RM:getItem(" + xid + ", " + key + ") called");
	    ReservableItem curObj = (ReservableItem)readData(xid, key);
	    if(curObj == null)
	    	return null;
	    return (ReservableItem) curObj.clone();
    }

    public ReservableItem getFlight(int xid, int flightNum) throws RemoteException{
	    return getItem(xid, Flight.getKey(flightNum));
    }

    public ReservableItem getCar(int xid, String location) throws RemoteException{
	    return getItem(xid, Car.getKey(location));
    }

    public ReservableItem getRoom(int xid, String location) throws RemoteException{
	    return getItem(xid, Room.getKey(location));
    }

    // Return a customer by key, used by TM for undo operation.
    public Customer getCustomer(int xid, int customerId) throws RemoteException{
	    Trace.info("RM:getCustomer(" + xid + ", " + customerId + ") called");
	    Customer customer = (Customer)readData(xid, Customer.getKey(customerId));
	    if(customer == null)
	    	return null;
	    return (Customer) customer.clone();
    }

    // For undo operation, reset customer.
    public void setCustomer(int xid, Customer customer) throws RemoteException{
	    Trace.info("RM:setCustomer(" + xid + ")called");
	    Trace.info("RM:" + customer);
	    writeData(xid, customer.getKey(), customer);
    }

	@Override
	public boolean shutdown() throws RemoteException {
		return false;
	}

	// For undo operation. reset the item.
    private void setItem(int xid, ReservableItem obj) throws RemoteException{
	    Trace.info("RM:setItem(" + xid + ")called");
	    Trace.info("RM: " + obj);
	    writeData(xid, obj.getKey(), obj);
    }

	@Override
	public void setFlight(int xid, ReservableItem obj) throws RemoteException {
		setItem(xid, obj);
	}

	@Override
	public void setCar(int xid, ReservableItem obj) throws RemoteException {
		setItem(xid, obj);
	}

	@Override
	public void setRoom(int xid, ReservableItem obj) throws RemoteException {
		setItem(xid, obj);
	}

	// Check the existens of a customer. Only used by customer server
	public boolean checkCustomer(int xid, int customerID) throws RemoteException{
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if(customer == null) return false;
		return true;
	}

	// Reserve an item for customer server.
	public boolean reserveItem_cus(int xid, int customerID, String key, String location, int price) throws RemoteException{
		Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
		if(customer == null)
			return false;
		customer.reserve(key, location, price);
		writeData(xid, customer.getKey(), customer);
		return true;
	}

	// Return key of a Flight
	public String getFlightKey(int xid, int number) throws RemoteException{
		return Flight.getKey(number);
	}

	// Return key of a car
	public String getCarKey(int xid, String location) throws RemoteException{
		return Car.getKey(location);
	}

	// Return key of a room
	public String getRoomKey(int xid, String location) throws RemoteException{
		return Room.getKey(location);
	}

	// Reserve an item
	protected boolean reserveItem(int xid, int customerID, String key, String location)
	{
		Trace.info("RM::reserveItem(" + xid + ", customer=" + customerID + ", " + key + ", " + location + ") called" );        
		// Read customer object if it exists (and read lock it)

		/*
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer == null)
		{
			Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ")  failed--customer doesn't exist");
			return false;
		} 
		*/
		// Check if the item is available
		ReservableItem item = (ReservableItem)readData(xid, key);
		if (item == null)
		{
			Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--item doesn't exist");
			return false;
		}
		else if (item.getCount() == 0)
		{
			Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--No more items");
			return false;
		}
		else
		{            
		/*
			customer.reserve(key, location, item.getPrice());
			writeData(xid, customer.getKey(), customer);
		*/
			// Decrease the number of available items in the storage
			item.setCount(item.getCount() - 1);
			item.setReserved(item.getReserved() + 1);
			writeData(xid, item.getKey(), item);

			Trace.info("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
			return true;
		}        
	}

	// Create a new flight, or add seats to existing flight
	// NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
	public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException
	{
		Trace.info("RM::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");
		Flight curObj = (Flight)readData(xid, Flight.getKey(flightNum));
		if (curObj == null)
		{
			// Doesn't exist yet, add it
			Flight newObj = new Flight(flightNum, flightSeats, flightPrice);
			writeData(xid, newObj.getKey(), newObj);
			Trace.info("RM::addFlight(" + xid + ") created new flight " + flightNum + ", seats=" + flightSeats + ", price=$" + flightPrice);
		}
		else
		{
			// Add seats to existing flight and update the price if greater than zero
			curObj.setCount(curObj.getCount() + flightSeats);
			if (flightPrice > 0)
			{
				curObj.setPrice(flightPrice);
			}
			writeData(xid, curObj.getKey(), curObj);
			Trace.info("RM::addFlight(" + xid + ") modified existing flight " + flightNum + ", seats=" + curObj.getCount() + ", price=$" + flightPrice);
		}
		return true;
	}

	// Undo add flight operation. Only for update operation.
	public boolean undoAddFlights(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException
	{
		Trace.info("RM::undoAddFlights(" + + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");
		Flight curObj = (Flight)readData(xid, Flight.getKey(flightNum));
		if(curObj == null){
			// Doesn't have this object, something wrong.
			Trace.error("RM::undoAddFlights(" + + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") failed, no such flight");
			return false;
		}
		else{
			curObj.setCount(flightSeats);
			curObj.setPrice(flightPrice);
			writeData(xid, curObj.getKey(), curObj);
			Trace.info("RM::undoAddFlights(" + + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") success");
		}
		return true;
	}

	// Create a new car location or add cars to an existing location
	// NOTE: if price <= 0 and the location already exists, it maintains its current price
	public boolean addCars(int xid, String location, int count, int price) throws RemoteException
	{
		Trace.info("RM::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
		Car curObj = (Car)readData(xid, Car.getKey(location));
		if (curObj == null)
		{
			// Car location doesn't exist yet, add it
			Car newObj = new Car(location, count, price);
			writeData(xid, newObj.getKey(), newObj);
			Trace.info("RM::addCars(" + xid + ") created new location " + location + ", count=" + count + ", price=$" + price);
		}
		else
		{
			// Add count to existing car location and update price if greater than zero
			curObj.setCount(curObj.getCount() + count);
			if (price > 0)
			{
				curObj.setPrice(price);
			}
			writeData(xid, curObj.getKey(), curObj);
			Trace.info("RM::addCars(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price);
		}
		return true;
	}

	// Undo add Cars operation. Only for update operation.
	public boolean undoAddCars(int xid, String location, int count, int price) throws RemoteException
	{
		Trace.info("RM::undoAddCars(" + + xid + ", " + location + ", " + count + ", $" + price + ") called");
		Car curObj = (Car)readData(xid, Car.getKey(location));
		if(curObj == null){
			// Doesn't have this object, something wrong.
			Trace.error("RM::undoAddCars(" + + xid + ", " + location + ", " + count + ", $" + price + ") failed, no such car");
			return false;
		}
		else{
			curObj.setCount(count);
			curObj.setPrice(price);
			writeData(xid, curObj.getKey(), curObj);
			Trace.info("RM::undoAddCars(" + + xid + ", " + location + ", " + count + ", $" + price + ") success");
		}
		return true;
	}

	// Create a new room location or add rooms to an existing location
	// NOTE: if price <= 0 and the room location already exists, it maintains its current price
	public boolean addRooms(int xid, String location, int count, int price) throws RemoteException
	{
		Trace.info("RM::addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
		Room curObj = (Room)readData(xid, Room.getKey(location));
		if (curObj == null)
		{
			// Room location doesn't exist yet, add it
			Room newObj = new Room(location, count, price);
			writeData(xid, newObj.getKey(), newObj);
			Trace.info("RM::addRooms(" + xid + ") created new room location " + location + ", count=" + count + ", price=$" + price);
		} else {
			// Add count to existing object and update price if greater than zero
			curObj.setCount(curObj.getCount() + count);
			if (price > 0)
			{
				curObj.setPrice(price);
			}
			writeData(xid, curObj.getKey(), curObj);
			Trace.info("RM::addRooms(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price);
		}
		return true;
	}

	// Undo add Rooms operation. Only for update operation.
	public boolean undoAddRooms(int xid, String location, int count, int price) throws RemoteException
	{
		Trace.info("RM::undoAddRooms(" + + xid + ", " + location + ", " + count + ", $" + price + ") called");
		Room curObj = (Room)readData(xid, Room.getKey(location));
		if(curObj == null){
			// Doesn't have this object, something wrong.
			Trace.error("RM::undoAddRooms(" + + xid + ", " + location + ", " + count + ", $" + price + ") failed, no such room");
			return false;
		}
		else{
			curObj.setCount(count);
			curObj.setPrice(price);
			writeData(xid, curObj.getKey(), curObj);
			Trace.info("RM::undoAddRooms(" + + xid + ", " + location + ", " + count + ", $" + price + ") success");
		}
		return true;
	}

	// Deletes flight
	public boolean deleteFlight(int xid, int flightNum) throws RemoteException
	{
		return deleteItem(xid, Flight.getKey(flightNum));
	}

	// Delete cars at a location
	public boolean deleteCars(int xid, String location) throws RemoteException
	{
		return deleteItem(xid, Car.getKey(location));
	}

	// Delete rooms at a location
	public boolean deleteRooms(int xid, String location) throws RemoteException
	{
		return deleteItem(xid, Room.getKey(location));
	}

	// Returns the number of empty seats in this flight
	public int queryFlight(int xid, int flightNum) throws RemoteException
	{
		return queryNum(xid, Flight.getKey(flightNum));
	}

	// Returns the number of cars available at a location
	public int queryCars(int xid, String location) throws RemoteException
	{
		return queryNum(xid, Car.getKey(location));
	}

	// Returns the amount of rooms available at a location
	public int queryRooms(int xid, String location) throws RemoteException
	{
		return queryNum(xid, Room.getKey(location));
	}

	// Returns price of a seat in this flight
	public int queryFlightPrice(int xid, int flightNum) throws RemoteException
	{
		return queryPrice(xid, Flight.getKey(flightNum));
	}

	// Returns price of cars at this location
	public int queryCarsPrice(int xid, String location) throws RemoteException
	{
		return queryPrice(xid, Car.getKey(location));
	}

	// Returns room price at this location
	public int queryRoomsPrice(int xid, String location) throws RemoteException
	{
		return queryPrice(xid, Room.getKey(location));
	}

	public String queryCustomerInfo(int xid, int customerID) throws RemoteException
	{
		Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer == null)
		{
			Trace.warn("RM::queryCustomerInfo(" + xid + ", " + customerID + ") failed--customer doesn't exist");
			// NOTE: don't change this--WC counts on this value indicating a customer does not exist...
			return "";
		}
		else
		{
			Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ")");
			System.out.println(customer.getBill());
			return customer.getBill();
		}
	}

	public int newCustomer(int xid) throws RemoteException
	{
        	Trace.info("RM::newCustomer(" + xid + ") called");
		// Generate a globally unique ID for the new customer
		int cid = Integer.parseInt(String.valueOf(xid) +
			String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
			String.valueOf(Math.round(Math.random() * 100 + 1)));
		Customer customer = new Customer(cid);
		writeData(xid, customer.getKey(), customer);
		Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
		return cid;
	}

	public boolean newCustomer(int xid, int customerID) throws RemoteException
	{
		Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer == null)
		{
			customer = new Customer(customerID);
			writeData(xid, customer.getKey(), customer);
			Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") created a new customer");
			return true;
		}
		else
		{
			Trace.info("INFO: RM::newCustomer(" + xid + ", " + customerID + ") failed--customer already exists");
			return false;
		}
	}

	public HashMap<String, Integer> getCustomerReservations(int xid, int customerID) throws RemoteException{
		Trace.info("RM::getCustomerReservations(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if(customer == null){
			Trace.warn("RM::getCustomerReservations(" + xid + ", " + customerID + ") failed--customer doesn't exist");
			return null;
		}
		Trace.info("RM::getCustomerReservations(" + xid + ", " + customerID + ") success");
		RMHashMap reservations = customer.getReservations();
		HashMap<String, Integer> reservationList = new HashMap<String, Integer>();
		for(String reservedKey: reservations.keySet()){
			ReservedItem reserveditem = customer.getReservedItem(reservedKey);
			Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " " +  reserveditem.getCount() +  " times");
			reservationList.put(reserveditem.getKey(), reserveditem.getCount());
		}
		Trace.info("RM::getCustomerReservations(" + xid + ", " + customerID + ") success");
		return reservationList;
	}

	public void reduceReservations(int xid, String key, int num) throws RemoteException{
		Trace.info("RM::reduceReservations(" + xid + ") called");
		ReservableItem item = (ReservableItem) readData(xid, key);
		item.setReserved(item.getReserved() - num);
		item.setCount(item.getCount() + num);
		Trace.info("RM::" + key + " decrease " + num + " reservations.");
		writeData(xid, item.getKey(), item);
		Trace.info("RM::reduceReservations(" + xid + ") success");
	}

	public boolean deleteCustomer(int xid, int customerID) throws RemoteException
	{
		Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer == null)
		{
			Trace.warn("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--customer doesn't exist");
			return false;
		}
		else
		{            
			// Increase the reserved numbers of all reservable items which the customer reserved. 
 			/*
			RMHashMap reservations = customer.getReservations();
			for (String reservedKey : reservations.keySet())
			{        
				ReservedItem reserveditem = customer.getReservedItem(reservedKey);
				Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " " +  reserveditem.getCount() +  " times");
				ReservableItem item  = (ReservableItem)readData(xid, reserveditem.getKey());
				Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " which is reserved " +  item.getReserved() +  " times and is still available " + item.getCount() + " times");
				item.setReserved(item.getReserved() - reserveditem.getCount());
				item.setCount(item.getCount() + reserveditem.getCount());
				writeData(xid, item.getKey(), item);
			}
			*/
			// Remove the customer from the storage
			removeData(xid, customer.getKey());
			Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") succeeded");
			return true;
		}
	}

	// Adds flight reservation to this customer
	public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException
	{
		return reserveItem(xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
	}

	// Adds car reservation to this customer
	public boolean reserveCar(int xid, int customerID, String location) throws RemoteException
	{
		return reserveItem(xid, customerID, Car.getKey(location), location);
	}

	// Adds room reservation to this customer
	public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException
	{
		return reserveItem(xid, customerID, Room.getKey(location), location);
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

	public boolean store(String name){
		File file = new File(name);
		try {
			FileOutputStream fileOut = new FileOutputStream(name, false);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(m_data);
			out.close();
			fileOut.close();
			Trace.info("RM::store database at: " + name);
		}
		catch (IOException e){
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean save(String name, int xid){
		try {
			File outFile = new File(name);
			outFile.delete();
			outFile.createNewFile();
			FileOutputStream fileOut = new FileOutputStream(name);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(dataHashMap.get(xid));
			out.close();
			fileOut.close();
			System.out.printf("Serialized data is saved in " + name + " .\n");
		}catch(IOException i) {
			i.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean load(String name){
		try {
			FileInputStream fileIn = new FileInputStream(name);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			m_data = (RMHashMap) in.readObject();
			in.close();
			fileIn.close();
			System.out.printf("Serialized data is load from " + name + " .\n");
		}catch(IOException | ClassNotFoundException i) {
			i.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean alive() throws RemoteException{
		return alive;
	}

	private void recover() {
		try {
			BufferedReader masterReader = new BufferedReader(new FileReader(masterRecordName));
			String line = masterReader.readLine();
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
			connect();
			Iterator it = logHashMap.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pair = (Map.Entry)it.next();
				int xid =  Integer.parseInt((String) pair.getKey());
				String status = (String) pair.getValue();
				if(status.equals("S")){
					mw.abortRequest(xid);
					write2log(xid + " A");
				}
				if(status.equals("C"))
					continue;
				if(status.equals("Y")){
					boolean result = mw.isAbort(xid);
					if(result){
						write2log(xid + " A");
					}
					else{
						if(masterIsA) {
							load(fileAName);
						}
						else {
							load(fileBName);
						}
						write2log(Integer.toString(xid) + " C");
					}
				}
				if(status.equals("A"))
					continue;
				it.remove();
			}
		}
		catch(IOException e){
			System.out.println("RM::Recover process have IO exception, retry.");
			recover();
		}
		System.out.println("RM::Recover finish");
	}

	private void write2log(String msg){
		try {
			logWriter.write(msg + "\n");
			logWriter.flush();
		}
		catch (IOException e){
			System.out.println("Can't write to log");
		}
	}


	public boolean prepareCommit(int xid) throws RemoteException{
		if(masterIsA){
			if(!save(fileBName, xid)){
				System.out.println("RM:: Can't save data to disk, abort transaction <" + xid +">");
				write2log(Integer.toString(xid) + " A");
				return false;
			}
		}
		else{
			if(!save(fileAName, xid)){
				System.out.println("RM:: Can't save data to disk, abort the transaction <" + xid +">");
				write2log(Integer.toString(xid) + " A");
				return true;
			}
		}
		System.out.println("RM:: Save the data to disk, vote yes for transaction <" + xid + ">");
		write2log(Integer.toString(xid) + " Y");
		return true;
	}

	public boolean receiveResult(int xid, boolean result) throws RemoteException{
		if(result){
			// copy local data to main memory
			RMHashMap oldm_data = (RMHashMap) m_data.clone();
			m_data = (RMHashMap) dataHashMap.get(xid).clone();
			dataHashMap.remove(xid);
			if(masterIsA) {
				masterIsA = false;
				try {
					masterWriter.write(Integer.toString(xid) + " B");
				}
				catch (IOException e){
					System.out.println("Can't write to " + masterRecordName);
					receiveResult(xid, result);
				}
			}
			else {
				masterIsA = true;
				try {
					masterWriter.write(Integer.toString(xid) + " A");
				} catch (IOException e) {
					System.out.println("Can't write to " + masterRecordName);
					receiveResult(xid, result);
				}
			}
			System.out.println("RM::Receive commit, commit transaction <" + xid + ">");
			write2log(Integer.toString(xid) + " C");
		}

		else{
			dataHashMap.remove(xid);
			System.out.println("RM::Receive abort, abort transaction <" + xid + ">");
			write2log(xid + " A");
		}
		return true;
	}

	public boolean startTransaction(int xid) throws RemoteException{
		System.out.println("RM::start transaction <" + xid + ">");
		dataHashMap.put(xid, (RMHashMap) m_data.clone());
		return true;
	}

	// abort because some RM fail, and lost the record of this transaction.
	public void removeTransactionFromHashmap(int xid) throws RemoteException{
		if(!dataHashMap.containsKey(xid))
			return;
		dataHashMap.remove(xid);
		System.out.println("RM:: receive remove transaction request, abort transaction <" + xid + ">");
		write2log(xid + " A");
		return;
	}

	public void connect(){
		// Set the security policy
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new SecurityManager());
		}
		try {
			boolean first = true;
			while (true) {
				try {
					Registry registry = LocateRegistry.getRegistry(s_serverHost, s_serverPort);
					mw = (IMiddleware)registry.lookup(s_rmiPrefix + s_serverName);
					System.out.println("Connected to '" + s_serverName + "' server [" + s_serverHost + ":" + s_serverPort + "/"
							+ s_rmiPrefix + s_serverName + "]");
					break;
				}
				catch (NotBoundException|RemoteException e) {
					if (first) {
						e.printStackTrace();
						System.out.println("Waiting for '" + s_serverName + "' server [" + s_serverHost + ":" + s_serverPort
								+ "/" + s_rmiPrefix + s_serverName + "]");
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
}
 
