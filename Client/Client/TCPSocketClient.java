package Client;

import Server.Interface.*;
import java.io.*;
import java.net.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class TCPSocketClient extends Client
{
    private static String s_serverHost = "localhost";
    private static int s_serverPort = 8888;
    private static String s_serverName = "MiddlewareServer";
    private static String s_rmiPrefix = "group15";
    private Socket socket = null;

    public static void main(String args[]) {
        // get customerized hostip and host port
        if(args.length > 0){
            s_serverHost = args[0];
        }

        if(args.length > 1){
            s_serverName = args[1];
        }

        if(args.length > 2){
            s_serverPort = Integer.parseInt(args[2]);
        }

        if (args.length > 3) {
            System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0mUsage: java client.RMIClient [server_hostname [server_rmiobject]]");
            System.exit(1);
        }


    }

    public boolean connect(String ip, int port){
        try{
            socket = new Socket(ip, port);
            out = new PrintWriter(socket.getOutputStream(),
                    true);
            in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
        } catch (UnknownHostException e) {
            System.out.println("Unknown host");
            System.exit(1);
            return false;
        } catch  (IOException e) {
            System.out.println("No I/O");
            System.exit(1);
            return false;
        }
        return true;
    }

    public void connectServer(){

    }
}
