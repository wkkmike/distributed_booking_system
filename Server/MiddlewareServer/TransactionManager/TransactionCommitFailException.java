package MiddlewareServer.TransactionManager;

public class TransactionCommitFailException extends Exception {
    private int m_xid = 0;

    public TransactionCommitFailException(int xid, String msg)
    {
        super("Commit of transaction " + xid + " is failed:" + msg);
        m_xid = xid;
    }

    int getXId()
    {
        return m_xid;
    }
}
