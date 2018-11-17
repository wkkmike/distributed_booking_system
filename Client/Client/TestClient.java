package Client;

import java.util.Date;
import java.util.Random;
import java.util.Vector;

public class TestClient extends RMIClient {
    private Date date = new Date();
    private final int testTimes = 1000;
    private final long transactionTime = 1000 / 250;
    private static String clientNum = "0";

    public static void main(String args[]){
        System.setSecurityManager(null);


        if(args.length > 0)
        {
            clientNum = args[0];
        }
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
        try {
            Random random = new Random(System.currentTimeMillis());
            Thread.sleep(random.nextInt(10));

        }
        catch (Exception e){

        }
        part2(client);
        System.exit(0);
    }

    public static void part2(TestClient client){
//        if(toInt(clientNum)%2==0) {
            System.out.println("add cars average response time is: " + client.addCars(toInt(clientNum)) + " ms\n");
//        }else {
//            System.out.print2s average response time is: " + client.addRooms(toInt(clientNum)) + " ms\n");
//        }

    }


    public static void part1(TestClient client){
        client.initialize();
        client.startCommit();
        System.out.println("only start and commit, average response time is: "+client.startCommit()+" ms\n");

        System.out.println("single add cars average response time is: "+client.addCars() + " ms\n");
        System.out.println("single add flight average response time is: "+client.addFlight() + " ms\n");
        System.out.println("single add rooms average response time is: "+client.addRooms() + " ms\n");
        System.out.println("single add customer average response time is: "+client.addCus() + " ms\n");

        System.out.println("addcar,addflight,addroom, response time is: " + client.addFlightCarRoom() +" ms\n");
        System.out.println("addcar,addcar,addcar, response time is: " + client.addCarCarCar() +" ms\n");

        System.out.println("single query flight average response time is: "+client.queryFlight() + " ms\n");
        System.out.println("single query car average response time is: "+client.queryCars() + " ms\n");
        System.out.println("single query room average response time is: "+client.queryRooms() + " ms\n");

        System.out.println("single reserve bundle average response time is: "+client.bundle() + " ms\n");
        System.out.println("single reserve flight average response time is: "+client.reserveFlight() + " ms\n");
        System.out.println("single reserve car average response time is: "+client.reserveCar() + " ms\n");
        System.out.println("single reserve room average response time is: "+client.reserveRoom() + " ms\n");
        System.out.println("single delete customer average response time is: "+client.deleteCus() + " ms\n");
        System.out.println("single delete flight average response time is: "+client.deleteFlight() + " ms\n");
        System.out.println("single delete car average response time is: "+client.deleteCar() + " ms\n");
        System.out.println("single delete room average response time is: "+client.deleteRoom() + " ms\n");
    }

    public double addCars(int index){
        Random random = new Random(date.getTime());
        long totalTime = 0;
        try{
            for(int i=1; i<= testTimes; i++){
                long startTime = System.currentTimeMillis();
                int xid = m_resourceManager.start();
                m_resourceManager.addCars(xid,Integer.toString(i+index*1000), 10, 10);
                m_resourceManager.commit(xid);
                totalTime += System.currentTimeMillis() - startTime;
                Thread.sleep(Math.max(0,transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5)));
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return (double) totalTime / testTimes;
    }

    public double addRooms(int index){
        Random random = new Random(date.getTime());
        long totalTime = 0;
        try{
            for(int i=1; i<= testTimes; i++){
                long startTime = System.currentTimeMillis();
                int xid = m_resourceManager.start();
                m_resourceManager.addRooms(xid, Integer.toString(i+index*1000),10,10);
                m_resourceManager.commit(xid);
                totalTime += System.currentTimeMillis() - startTime;
                Thread.sleep(Math.max(0,transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5)));
            }
        }
        catch (Exception e){

        }
        return (double) totalTime / testTimes;
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

    public double addFlightCarRoom(){
        Random random = new Random(date.getTime());
        long totalTime = 0;
        try{
            for(int i=1; i<= testTimes; i++){
                long startTime = System.currentTimeMillis();
                int xid = m_resourceManager.start();
                m_resourceManager.addFlight(xid, i, 10, 10);
                m_resourceManager.addCars(xid,Integer.toString(i), 10, 10);
                m_resourceManager.addRooms(xid,Integer.toString(i), 10, 10);
                m_resourceManager.commit(xid);
                totalTime += System.currentTimeMillis() - startTime;
                Thread.sleep(Math.max(0,transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5)));
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return (double) totalTime / testTimes;
    }

    public double addCarCarCar(){
        Random random = new Random(date.getTime());
        long totalTime = 0;
        try{
            for(int i=1; i<= testTimes; i++){
                long startTime = System.currentTimeMillis();
                int xid = m_resourceManager.start();
                m_resourceManager.addCars(xid,Integer.toString(i), 10, 10);
                m_resourceManager.addCars(xid,Integer.toString(i), 10, 10);
                m_resourceManager.addCars(xid,Integer.toString(i), 10, 10);
                m_resourceManager.commit(xid);
                totalTime += System.currentTimeMillis() - startTime;
                Thread.sleep(Math.max(0,transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5)));
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return (double) totalTime / testTimes;
    }

    public double startCommit(){
        Random random = new Random(date.getTime());
        long totalTime = 0;
        try{
            for(int i=1; i<= testTimes; i++){
                long startTime = System.currentTimeMillis();
                int xid = m_resourceManager.start();
                m_resourceManager.commit(xid);
                totalTime += System.currentTimeMillis() - startTime;
                Thread.sleep(Math.max(0,transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5)));
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return (double) totalTime / testTimes;
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
                Thread.sleep(Math.max(0,transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5)));
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return (double) totalTime / testTimes;
    }

    public double queryFlight(){
        Random random = new Random(date.getTime());
        long totalTime = 0;
        try{
            for(int i=1; i<= testTimes; i++){
                long startTime = System.currentTimeMillis();
                int xid = m_resourceManager.start();
                m_resourceManager.queryFlight(xid, i);
                m_resourceManager.commit(xid);
                totalTime += System.currentTimeMillis() - startTime;
                Thread.sleep(Math.max(0,transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5)));
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
                Thread.sleep(Math.max(0,transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5)));
            }
        }
        catch (Exception e){

        }
        return (double) totalTime / testTimes;
    }


    public double queryCars(){
        Random random = new Random(date.getTime());
        long totalTime = 0;
        try{
            for(int i=1; i<= testTimes; i++){
                long startTime = System.currentTimeMillis();
                int xid = m_resourceManager.start();
                m_resourceManager.queryCars(xid, Integer.toString(i));
                m_resourceManager.commit(xid);
                totalTime += System.currentTimeMillis() - startTime;
                Thread.sleep(Math.max(0,transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5)));
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
                Thread.sleep(Math.max(0,transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5)));
            }
        }
        catch (Exception e){

        }
        return (double) totalTime / testTimes;
    }

    public double queryRooms(){
        Random random = new Random(date.getTime());
        long totalTime = 0;
        try{
            for(int i=1; i<= testTimes; i++){
                long startTime = System.currentTimeMillis();
                int xid = m_resourceManager.start();
                m_resourceManager.queryRooms(xid, Integer.toString(i));
                m_resourceManager.commit(xid);
                totalTime += System.currentTimeMillis() - startTime;
                Thread.sleep(Math.max(0,transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5)));
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
                Thread.sleep(Math.max(0,(transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5))));
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
                Thread.sleep(Math.max(0,(transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5))));
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
                Thread.sleep(Math.max(0,(transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5))));
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
                Thread.sleep(Math.max(0,(transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5))));
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
                Thread.sleep(Math.max(0,(transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5))));
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
                Thread.sleep(Math.max(0,(transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5))));
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
                Thread.sleep(Math.max(0,(transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5))));
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
                Thread.sleep(Math.max(0,(transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5))));
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
                Thread.sleep(Math.max(0,(transactionTime - (System.currentTimeMillis() - startTime) + (random.nextInt(11) - 5))));
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
