package paxos.util;

import utils.Utils;

import java.util.Random;

public class PaxosUtil {

    private static final int POSSIBILITY = 8;
    private static Random random = new Random();

    public static void simulateDelay(){
        try {
            Thread.currentThread().sleep(random.nextInt(200));
        } catch (InterruptedException e) {
            Utils.timedLog(e.getMessage());
        }
    }

    // Take a chance of 1/8
    // Make use of base thoughts from Reservoir Sampling
    public static boolean simulateCrashing(){
        return random.nextInt(POSSIBILITY) == 0;
    }

    // An algorithm from Google Chubby framework
    // Make use of proposers' id, current election round and the total number of proposers
    // to generate sequence number
    public static int generateSeq(int id, int rounds, int proposerCount){
        return id + rounds * proposerCount;
    }
}
