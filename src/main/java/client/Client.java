package client;

import interfaces.CentralServerService;
import interfaces.My2PCProtocol;
import utils.Utils;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class Client {

    private static String[] infoIdx = {"ustotal", "worldtotal", "catotal", "usdeath", "worlddeath"};

    public static void main(String[] args) throws RemoteException {
        Scanner sc = new Scanner(System.in);
        boolean hasSignUp = false;

        Registry registry;
        CentralServerService centralServer = null;
        try {
            registry = LocateRegistry.getRegistry("127.0.0.1", 8000);
            centralServer = (CentralServerService) registry.lookup("localhost/central");
        } catch (RemoteException | NotBoundException e) {
            System.out.println("Errors establishing RPC!");

        }

        int res = 0;
        while (true) {
            System.out.println("Do you want to sign up for an account?");
            System.out.println("1: Yes 2: No");
            try {
                res = sc.nextInt();
                if (res < 1 || res > 2) {
                    Utils.timedLog("Invalid Input!!!");
                    continue;
                }
                break;
            } catch (Exception e) {
                sc.nextLine();
                Utils.timedLog("Invalid Input!!!");
            }
        }
        sc.nextLine();
        boolean signUpResult = false;
        while (res == 1 && !signUpResult) {
            System.out.println("Enter the username:");
            String name = sc.nextLine();
            System.out.println("Enter the password:");
            String psw = sc.nextLine();
            if (centralServer != null && centralServer.signup(name, psw)) {
                hasSignUp = true;
                break;
            }
            System.out.println("Sign up failed!");
        }


        while (!hasSignUp) {
            System.out.println("Enter the username:");
            String name = sc.nextLine();
            System.out.println("Enter the password:");
            String psw = sc.nextLine();
            if (centralServer != null && centralServer.login(name, psw)) break;
            System.out.println("Login failed!");
        }

        while (true) {
            System.out.println("Enter an integer for services:");
            System.out.println("1: Add a server. 2: Remove a server. 3: Info query. 4: Add, Update or Query Customized Info");
            int num = 0;
            try {
                num = sc.nextInt();
                if (num < 1 || num > 4) {
                    Utils.timedLog("Invalid Input!!!");
                    continue;
                }
            } catch (Exception e) {
                sc.nextLine();
                Utils.timedLog("Invalid Input!!!");
                continue;
            }
            sc.nextLine();
            boolean result;
            switch(num) {
                case 1:
                    result = centralServer.addInfoServer();
                    if (result) Utils.timedLog("A new server initiated!");
                    break;
                case 2:
                    result = centralServer.removeInfoServer();
                    if (result) Utils.timedLog("A server removed!");
                    break;
                case 3:
                    System.out.println("COVID-19 General Info: 1: US Total Confirmed. 2: World Total Confirmed. 3: California Total Confirmed. 4: US Total Death. 5: World Total Death.");
                    System.out.println("Please input any number between 1~5. For example, 3.");
                    try {
                        int op1 = sc.nextInt();
                        if (op1 < 1 || op1 > 5) {
                            Utils.timedLog("Invalid Input!!!");
                            continue;
                        }
                        String query = infoIdx[op1-1];
                        System.out.println(centralServer.search("get,"+query+", "));
                    } catch (Exception e) {
                        Utils.timedLog("Invalid Input!!!");
                        continue;
                    }
                    break;
                case 4:
                    System.out.println("Enter your operation:<put|get|del> <key> <value>");
                    System.out.println("for example: put year 2017");
                    String op = sc.nextLine();
                    String[] ops = op.split(" ");
                    if (ops.length == 2) {
                        String cmd = ops[0], key = ops[1], value = " ";
                        Utils.timedLog(centralServer.search(cmd+","+key+","+value));
                    } else if (ops.length == 3) {
                        String cmd = ops[0], key = ops[1], value = ops[2];
                        Utils.timedLog(centralServer.search(cmd+","+key+","+value));
                    }
                    break;
                default: break;
            }
        }

    }
}
