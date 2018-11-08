package Client;

import java.util.Date;
import java.util.Random;
import java.util.Vector;

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

        System.out.println("\nsingle add flight average response time is: "+client.addFlight() + "\n");
        System.out.println("\nsingle add cars average response time is: "+client.addCars() + "\n");
        System.out.println("\nsingle add rooms average response time is: "+client.addRooms() + "\n");
        System.out.println("\nsingle add customer average response time is: "+client.addCus() + "\n");

        System.out.println("\nsingle reserve bundle average response time is: "+client.bundle() + "\n");
        System.out.println("\nsingle reserve flight average response time is: "+client.reserveFlight() + "\n");
        System.out.println("\nsingle reserve car average response time is: "+client.reserveCar() + "\n");
        System.out.println("\nsingle reserve room average response time is: "+client.reserveRoom() + "\n");
        System.out.println("\nsingle delete customer average response time is: "+client.deleteCus() + "\n");
        System.out.println("\nsingle delete flight average response time is: "+client.deleteFlight() + "\n");
        System.out.println("\nsingle delete car average response time is: "+client.deleteCar() + "\n");
        System.out.println("\nsingle delete room average response time is: "+client.deleteRoom() + "\n");

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

    public double addFlight(){
        Random random = new Random(date.getTime());
        long totalTime = 0;
        try{
            for(int i=1; i<= testTimes; i++){
                long startTime = System.currentTimeMillis();
                int xid = m_resourceManager.start();
                m_resourceManager.addFlight(xid, i,10,10);
                m_resourceManager.commit(xid);
                totalTime += System.currentTimeMillis() - startTime;
                Thread.sleep(Math.min(0,Math.abs(transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5))));
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return (double) totalTime / testTimes;
    }

    public double addCars(){
        Random random = new Random(date.getTime());
        long totalTime = 0;
        try{
            for(int i=1; i<= testTimes; i++){
                long startTime = System.currentTimeMillis();
                int xid = m_resourceManager.start();
                m_resourceManager.addCars(xid, Integer.toString(i),10,10);
                m_resourceManager.commit(xid);
                totalTime += System.currentTimeMillis() - startTime;
                Thread.sleep(transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5));
            }
        }
        catch (Exception e){

        }
        return (double) totalTime / testTimes;
    }

    public double addRooms(){
        Random random = new Random(date.getTime());
        long totalTime = 0;
        try{
            for(int i=1; i<= testTimes; i++){
                long startTime = System.currentTimeMillis();
                int xid = m_resourceManager.start();
                m_resourceManager.addRooms(xid, Integer.toString(i),10,10);
                m_resourceManager.commit(xid);
                totalTime += System.currentTimeMillis() - startTime;
                Thread.sleep(transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5));
            }
        }
        catch (Exception e){

        }
        return (double) totalTime / testTimes;
    }

    public double addCus(){
        Random random = new Random(date.getTime());
        long totalTime = 0;
        try{
            for(int i=1; i<= testTimes; i++){
                long startTime = System.currentTimeMillis();
                int xid = m_resourceManager.start();
                m_resourceManager.newCustomer(xid, i);
                m_resourceManager.commit(xid);
                totalTime += System.currentTimeMillis() - startTime;
                Thread.sleep(transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5));
            }
        }
        catch (Exception e){

        }
        return (double) totalTime / testTimes;
    }

    public double bundle(){
        Random random = new Random(date.getTime());
        long totalTime = 0;
        try{
            for(int i=1; i<= testTimes; i++){
                Vector<String> v = new Vector<>();
                v.add(Integer.toString(i));
                long startTime = System.currentTimeMillis();
                int xid = m_resourceManager.start();
                m_resourceManager.bundle(xid, i, v, Integer.toString(i), true, true);
                m_resourceManager.commit(xid);
                totalTime += System.currentTimeMillis() - startTime;
                Thread.sleep(transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5));
            }
        }
        catch (Exception e){

        }
        return (double) totalTime / testTimes;
    }

    public double reserveFlight(){
        Random random = new Random(date.getTime());
        long totalTime = 0;
        try{
            for(int i=1; i<= testTimes; i++){
                long startTime = System.currentTimeMillis();
                int xid = m_resourceManager.start();
                m_resourceManager.reserveFlight(xid, i, i);
                m_resourceManager.commit(xid);
                totalTime += System.currentTimeMillis() - startTime;
                Thread.sleep(transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5));
            }
        }
        catch (Exception e){

        }
        return (double) totalTime / testTimes;
    }

    public double reserveCar(){
        Random random = new Random(date.getTime());
        long totalTime = 0;
        try{
            for(int i=1; i<= testTimes; i++){
                long startTime = System.currentTimeMillis();
                int xid = m_resourceManager.start();
                m_resourceManager.reserveCar(xid, i, Integer.toString(i));
                m_resourceManager.commit(xid);
                totalTime += System.currentTimeMillis() - startTime;
                Thread.sleep(transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5));
            }
        }
        catch (Exception e){

        }
        return (double) totalTime / testTimes;
    }

    public double reserveRoom(){
        Random random = new Random(date.getTime());
        long totalTime = 0;
        try{
            for(int i=1; i<= testTimes; i++){
                long startTime = System.currentTimeMillis();
                int xid = m_resourceManager.start();
                m_resourceManager.reserveRoom(xid, i, Integer.toString(i));
                m_resourceManager.commit(xid);
                totalTime += System.currentTimeMillis() - startTime;
                Thread.sleep(transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5));
            }
        }
        catch (Exception e){

        }
        return (double) totalTime / testTimes;
    }

    public double deleteCus(){
        Random random = new Random(date.getTime());
        long totalTime = 0;
        try{
            for(int i=1; i<= testTimes; i++){
                long startTime = System.currentTimeMillis();
                int xid = m_resourceManager.start();
                m_resourceManager.deleteCustomer(xid, i);
                m_resourceManager.commit(xid);
                totalTime += System.currentTimeMillis() - startTime;
                Thread.sleep(transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5));
            }
        }
        catch (Exception e){

        }
        return (double) totalTime / testTimes;
    }

    public double deleteFlight(){
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

    public double deleteCar(){
        Random random = new Random(date.getTime());
        long totalTime = 0;
        try{
            for(int i=1; i<= testTimes; i++){
                long startTime = System.currentTimeMillis();
                int xid = m_resourceManager.start();
                m_resourceManager.deleteCars(xid, Integer.toString(i));
                m_resourceManager.commit(xid);
                totalTime += System.currentTimeMillis() - startTime;
                Thread.sleep(transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5));
            }
        }
        catch (Exception e){

        }
        return (double) totalTime / testTimes;
    }

    public double deleteRoom(){
        Random random = new Random(date.getTime());
        long totalTime = 0;
        try{
            for(int i=1; i<= testTimes; i++){
                long startTime = System.currentTimeMillis();
                int xid = m_resourceManager.start();
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
