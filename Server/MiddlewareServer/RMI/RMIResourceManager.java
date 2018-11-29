// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package MiddlewareServer.RMI;

import MiddlewareServer.Interface.IMiddleware;
import MiddlewareServer.Common.*;
import Server.Common.Trace;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RMIResourceManager extends ResourceManager
{
	private static String s_serverName = "MiddlewareServer";
	private static String s_rmiPrefix = "group15";

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

	public static void main(String args[])
	{
		System.setSecurityManager(null);
		if(args.length > 0)
		{
			s_serverHost_Flight = args[0];
		}

		if(args.length > 1)
		{
			s_serverHost_Car = args[1];
		}

		if(args.length > 2)
		{
			s_serverHost_Room = args[2];
		}

		if(args.length > 3)
		{
			s_serverHost_Customer = args[3];
		}
		// Create the RMI server entry
		// Create a new Server object
		RMIResourceManager server = new RMIResourceManager(s_serverName);
		try {
			// Dynamically generate the stub (client proxy)
			IMiddleware resourceManager = (IMiddleware)UnicastRemoteObject.exportObject(server, 0);

			// Bind the remote object's stub in the registry
			Registry l_registry;
			try {
				l_registry = LocateRegistry.createRegistry(1099);
			} catch (RemoteException e) {
				l_registry = LocateRegistry.getRegistry(1099);
			}
			final Registry registry = l_registry;
			registry.rebind(s_rmiPrefix + s_serverName, resourceManager);

			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						registry.unbind(s_rmiPrefix + s_serverName);
						System.out.println("'" + s_serverName + "' resource manager unbound");
					}
					catch(Exception e) {
						System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
						e.printStackTrace();
					}
				}
			});
			System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_rmiPrefix + s_serverName + "'");
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}

		// Create and install a security manager
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new SecurityManager());
		}

		try {
			server.connect();
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public RMIResourceManager(String name)
	{
		super(name);
	}


	public boolean connect(){
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new SecurityManager());
		}
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

	public void rmFailuerHandler(){
	}

	public boolean shutdown() throws RemoteException{
		try {
			m_resourceManager_c.shutdown();
			m_resourceManager_cus.shutdown();
			m_resourceManager_f.shutdown();
			m_resourceManager_r.shutdown();
		}
		catch (RemoteException e){
			Trace.error("RM::Shutdown server failed." + e.getMessage());
		}

		Registry registry = LocateRegistry.getRegistry(1099);
		try{
			// Unregister ourself
			//registry.unbind(s_rmiPrefix + s_serverName);

			// Unexport; this will also remove us from the RMI run

			UnicastRemoteObject.unexportObject(this, true);
			ScheduledExecutorService scheduler =
					Executors.newScheduledThreadPool(1);
			scheduler.schedule(new Runnable() {
				public void run() {
					System.out.println("Server " + s_serverName + " exit");
					System.exit(1);}
			}, 500, TimeUnit.MILLISECONDS);
			Trace.info("RM::" + s_rmiPrefix + s_serverName + " shutdown");
		}
		catch(Exception e){
			Trace.info("RM::Problem during shutdown: " + e.getMessage());
		}
		return true;
	}
}