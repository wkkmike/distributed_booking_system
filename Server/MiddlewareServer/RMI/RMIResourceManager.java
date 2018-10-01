// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package MiddlewareServer.RMI;

import Server.Interface.*;
import MiddlewareServer.Common.*;

import java.rmi.NotBoundException;
import java.util.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RMIResourceManager extends ResourceManager
{
	private static String s_serverName = "MiddlewareServer";
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
	private static String s_serverName_cus = "Customers";

	public static void main(String args[])
	{
		if (args.length > 0)
		{
			s_serverName = args[0];
		}

		// Create the RMI server entry
		// Create a new Server object
		RMIResourceManager server = new RMIResourceManager(s_serverName);
		try {
			// Dynamically generate the stub (client proxy)
			IResourceManager resourceManager = (IResourceManager)UnicastRemoteObject.exportObject(server, 0);

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
			m_resourceManager_f = connectServer(s_serverHost_f, s_serverPort_f, s_serverName_f);
			m_resourceManager_c = connectServer(s_serverHost_c, s_serverPort_c, s_serverName_c);
			m_resourceManager_r = connectServer(s_serverHost_r, s_serverPort_r, s_serverName_r);
			m_resourceManager_cus = connectServer(s_serverHost_cus, s_serverPort_cus, s_serverName_cus);
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}
		return true;
	}
}
