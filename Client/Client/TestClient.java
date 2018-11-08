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
                m_resourceManager.addCars(1,Integer.toString(i), 10, 10);
                m_resourceManager.addRooms(1,Integer.toString(i), 10, 10);
            }
            m_resourceManager.commit(xid);
        }
        catch (Exception e){

        }
    }

    public double test1(){
        Random random = new Random(date.getTime());
        long totalTime = 0;
        try{
            for(int i=1; i<= testTimes; i++){
                long startTime = System.currentTimeMillis();
                int xid = m_resourceManager.start();
                m_resourceManager.deleteFlight(xid, i);
                m_resourceManager.commit(xid);
                totalTime += System.currentTimeMillis() - startTime;
                Thread.sleep(transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5));
            }
        }
        catch (Exception e){

        }
        return (double) totalTime / testTimes;
    }

    public double test2(){
        Random random = new Random(date.getTime());
        long totalTime = 0;
        try{
            for(int i=1; i<= testTimes*3/5; i++){
                long startTime = System.currentTimeMillis();
                int xid = m_resourceManager.start();
                m_resourceManager.deleteFlight(xid, i);
                m_resourceManager.deleteCars(xid, Integer.toString(i));
                m_resourceManager.deleteRooms(xid, Integer.toString(i));
                m_resourceManager.commit(xid);
                totalTime += System.currentTimeMillis() - startTime;
                Thread.sleep(transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5));
            }
        }
        catch (Exception e){

        }
        return (double) totalTime / testTimes;
    }

    public double test3(){
        Random random = new Random(date.getTime());
        long totalTime = 0;
        try{
            for(int i=1; i<= testTimes*3/2; i++){
                long startTime = System.currentTimeMillis();
                int xid = m_resourceManager.start();
                m_resourceManager.commit(xid);
                totalTime += System.currentTimeMillis() - startTime;
                Thread.sleep(transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5));
            }
        }
        catch (Exception e){

        }
        return (double) totalTime / testTimes;
    }
}
