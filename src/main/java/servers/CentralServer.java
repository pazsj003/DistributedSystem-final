package servers;

import interfaces.CentralServerService;
import interfaces.My2PCProtocol;
import utils.Utils;

import java.io.*;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CentralServer implements CentralServerService {

    private static final int CORE_SIZE = 10;
    private static final int MAX_SIZE = 15;
    private static final int KEEP_ALIVE = 3;

    private static BufferedWriter out;

    private Map<String, String> accounts;

    private File accountsFile;

    private ThreadPoolExecutor threadPool;

    private Registry registry;

    // maximum info server count is 10, minimum is 5
    public static Queue<Future> infoServers;

    public CentralServer() throws RemoteException, AlreadyBoundException {
        accounts = new HashMap<>();
        threadPool = new ThreadPoolExecutor(CORE_SIZE, MAX_SIZE, KEEP_ALIVE, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(5), new ThreadPoolExecutor.DiscardOldestPolicy());
        infoServers = new LinkedList<>();
        registry = LocateRegistry.getRegistry("127.0.0.1", 8000);
        System.setProperty("java.rmi.server.hostname", "127.0.0.1");
        CentralServerService stub = (CentralServerService) UnicastRemoteObject.exportObject(this, 0);
        registry.bind("localhost/central", stub);
        setUpAccounts();
        initInfoServers();
    }

    private void initInfoServers() {
        // create 5 servers initially
        for (int i = 0; i < 5; i++) {
            Future future = threadPool.submit(new InfoServer("localhost/KV"+i));
            infoServers.offer(future);
        }
    }

    // FIXME: try to add more accounts here
    private void setUpAccounts() {
        readAccounts();
    }

    public static void main (String args[]) throws IOException, AlreadyBoundException {
        LocateRegistry.createRegistry(8000);
        Utils.timedLog("Registry server starts to listen to 127.0.0.1:8000!");
        File dataFile = new File("./manipulations.txt");
        dataFile.createNewFile();
        out = new BufferedWriter(new FileWriter(dataFile));

        CentralServer server = new CentralServer();
    }

    @Override
    public boolean signup(String name, String psw) throws RemoteException {
        if (accounts.containsKey(name)) {
            Utils.errorLog("User name already exists!");
            return false;
        }
        accounts.put(name, psw);
        storeAccounts(name, psw);
        return true;
    }

    @Override
    public boolean login(String name, String psw) throws RemoteException {
        if (!accounts.containsKey(name)) return false;
        return accounts.get(name).equals(psw);
    }

    @Override
    public String search(String keyword) throws RemoteException {
        // data persistence
        try {
            out.write(keyword + "\r\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // randomly pick a eligible server
        String name = "localhost/KV" + new Random().nextInt(infoServers.size());
        try {
            My2PCProtocol infoServer = (My2PCProtocol) registry.lookup(name);
            String respond = infoServer.send(keyword);
            Utils.timedLog(respond);
            return respond;
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean addInfoServer() throws RemoteException {
        if (infoServers.size() >= CORE_SIZE) return false;
        String name = "localhost/KV" + infoServers.size();
        Future future = threadPool.submit(new InfoServer(name));
        infoServers.offer(future);
        return true;
    }

    @Override
    public boolean removeInfoServer() throws RemoteException {
        if (infoServers.size() <= 5) return false;
        Future future = infoServers.poll();
        future.cancel(true);
        return true;
    }

    private void storeAccounts(String userName, String password) {
        try {
            if (accountsFile == null) {
                accountsFile = new File("src/main/resources/accounts");
                return;
            }
            FileWriter myWriter = new FileWriter(accountsFile, true);
            myWriter.write("user:" + userName + "\n");
            myWriter.write("password:" + password + "\n");
            myWriter.flush();
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readAccounts() {
        accountsFile = new File("src/main/resources/accounts");
        try {
            Scanner myReader = new Scanner(accountsFile);
            while (myReader.hasNextLine()) {
                String userName, password;
                // read user name
                String data = myReader.nextLine();
                String namePrefix = "user:";
                userName = data.substring(namePrefix.length());
                // read password
                data = myReader.nextLine();
                String pwdPrefix = "password:";
                password = data.substring(pwdPrefix.length());
                // put into accounts
                accounts.put(userName, password);
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            Utils.timedLog("Cannot read account file");
        }
    }

}
