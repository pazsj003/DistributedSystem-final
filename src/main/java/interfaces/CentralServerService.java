package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CentralServerService extends Remote {

    boolean signup(String name, String psw) throws RemoteException;

    boolean login(String name, String psw) throws RemoteException;

    String search(String keyword) throws RemoteException;

    boolean addInfoServer() throws RemoteException;

    boolean removeInfoServer() throws RemoteException;
}
