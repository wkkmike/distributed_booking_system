package Client;

import java.util.Date;
import java.util.Random;

public class TestClient extends RMIClient {
    private Date date = new Date();
    private final int testTimes = 1000;
    private final long transactionTime = 500;

    public static void main(String args[]){
        System.setSecurityManager(null);

        // Set the security policy
        if (System.getSecurityManager() == null)
        {
            System.setSecurityManager(new SecurityManager());
        }
        // Get a reference to the RMIRegister
        TestClient client = new TestClient();
        try {
            client.connectServer();
        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
        client.initialize();
        System.out.println("average response time is :"+client.test1());
        System.exit(0);
    }

    public void initialize(){
        try {
            int xid = m_resourceManager.start();
            for(int i=1; i<10001; i++) {
                m_resourceManager.addFlight(1, i, 10, 10);
            }
            m_resourceManager.commit(xid);
        }
        catch (Exception e){

        }
    }

    public double test1(){
        Random random = new Random(date.getTime());
        long currentTime = date.getTime();
        try{
            for(int i=1; i<= testTimes; i++){
                int xid = m_resourceManager.start();
                m_resourceManager.deleteFlight(xid, i);
                m_resourceManager.commit(xid);
                Thread.sleep(transactionTime - (date.getTime() - currentTime) + (random.nextInt(11) - 5));
            }
        }
        catch (Exception e){

        }
        long time = date.getTime() - currentTime;
        return (double) time / testTimes;
    }
}
