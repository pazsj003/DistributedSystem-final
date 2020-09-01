package servers;

import interfaces.My2PCProtocol;
import paxos.PaxosService;
import utils.Utils;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InfoServer implements Runnable, My2PCProtocol {

    // The KV store of info server
    private Map<String, String> map;

    private ReadWriteLock lock;

    private String name;

    private Registry registry;

    private CountDownLatch latchForPaxos;

    public InfoServer(String hostName) {
        super();

        name = hostName;
        map = new HashMap<>();
        lock = new ReentrantReadWriteLock();

        initNews();
    }

    @Override
    public void run() {
        try {
            registry = LocateRegistry.getRegistry("127.0.0.1", 8000);
            System.setProperty("java.rmi.server.hostname", "127.0.0.1");
            My2PCProtocol stub = (My2PCProtocol) UnicastRemoteObject.exportObject(this, 0);
            registry.bind(name, stub);

            Utils.timedLog("Server: " + name + " is ready");
        } catch (RemoteException | AlreadyBoundException e) {
            Utils.errorLog(e.getMessage());
        }
    }

    @Override
    public String send(String op) throws RemoteException {
        AtomicReference<String> response = new AtomicReference<>();
        CountDownLatch count = new CountDownLatch(1);

        try {
            response.set(processOp(op));
        } catch (IOException e) {
            Utils.errorLog(e.getMessage());
        }
        count.countDown();

        try {
            count.await();
        } catch (InterruptedException e) {
            Utils.errorLog(e.getMessage());
        }

        // Then we begin to handle data consistency
        dataSynchronization(op);

        return String.valueOf(response);
    }

    @Override
    public boolean prepare() throws RemoteException {
        // assume no one fails at the prepare stage
        return true;
    }

    @Override
    public String commit(String op) throws RemoteException {
        try {
            processOp(op);
        } catch (IOException e) {
//            e.printStackTrace();
            return "Server: " + name + " commit failed!";
        }
        return "Server: " + name + " committed!";
    }

    private String processOp(String op) throws IOException {
        if (    !op.startsWith("put") &&
                !op.startsWith("get") &&
                !op.startsWith("del")) {
            return "Server: " + name + " Invalid operation!";
        }

        String[] ops = op.split(",");
        String cmd = ops[0], key = ops[1], value = ops[2];
        if ((cmd.equals("get") || cmd.equals("del")) && !map.containsKey(key)) {
            String errMsg = "Server: " + name + " Invalid key! Have you ever created it?";
            Utils.timedLog(errMsg);
            return errMsg;
        }

        switch (cmd) {
            case "get":
                lock.readLock().lock();
                String v = map.get(key);
                lock.readLock().unlock();
                return "Server: " + name + " The command is get, and the result is: " + v;
            case "put":
                lock.writeLock().lock();
                map.put(key, value);
                lock.writeLock().unlock();
                return "Server: " + name + " The command is put, and process is finished!";
            case "del":
                lock.writeLock().lock();
                map.remove(key);
                lock.writeLock().unlock();
                return "Server: " + name + " The command is delete, and process is finished!";
            default: break;
        }

        return "Server: " + name + " Invalid operation!";
    }

    private boolean dataSynchronization(String op) {
        if (    !op.startsWith("put") &&
                !op.startsWith("get") &&
                !op.startsWith("del")) {
            return false;
        }
        String[] ops = op.split(",");
        String cmd = ops[0], key = ops[1], value = ops[2];
        switch (cmd) {
            case "get": return true;
            case "put": // fall down to the case "del"
            case "del": return updateWith2PC(op, value);
            default: break;
        }

        return false;
    }

    private boolean updateWith2PC(String op, String value) {
        latchForPaxos = new CountDownLatch(1);
        PaxosService paxosService = new PaxosService(latchForPaxos);

        // Firstly, make all of other servers prepared
        // return false and cancel the update if any of servers is not ready
        for (int i = 0; i < CentralServer.infoServers.size(); i++) {
            String svrName = "localhost/KV"+i;
            if (svrName.equals(name)) continue; // skip on itself
            My2PCProtocol stub = null;
            try {
                stub = (My2PCProtocol) registry.lookup(svrName);
                // exit once if there is a false
                if (!stub.prepare()) return false;
                paxosService.initProposer(i, i+"#" + value);
            } catch (RemoteException | NotBoundException e) {
                Utils.errorLog("Not found this server when polling: " + svrName);
                return false;
            }
        }

        paxosService.runPaxos();
        try {
            latchForPaxos.await();
        } catch (InterruptedException e) {
            Utils.timedLog(e.getMessage());
        }

        // All servers stand by so we start to conduct the same update on each server
        // we assume that all the manipulation would success
        for (int i = 0; i < CentralServer.infoServers.size(); i++) {
            String svrName = "localhost/KV"+i;
            if (svrName.equals(name)) continue; // skip on itself
            My2PCProtocol stub = null;
            try {
                stub = (My2PCProtocol) registry.lookup(svrName);
                String resp = stub.commit(op);
                Utils.timedLog(resp);
            } catch (RemoteException | NotBoundException e) {
                Utils.errorLog("Not found this server when polling: " + svrName);
                return false;
            }
        }
        return true;
    }

    private void initNews() {
        map.put("ustotal", "761,964");
        map.put("worldtotal", "2,404,249");
        map.put("catotal", "31,528");
        map.put("usdeath", "35,314");
        map.put("worlddeath", "165,234");
    }

}
