package MiddlewareServer.TransactionManager;

public class RMNotAliveException extends Exception{
    public RMNotAliveException()
    {
        super("Some Resource Manager is not touchable now. We will fix it soon.");
    };
}
