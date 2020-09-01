package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface My2PCProtocol extends Remote {

    String send(String op) throws RemoteException;

    boolean prepare() throws RemoteException;

    String commit(String op) throws RemoteException;
}
