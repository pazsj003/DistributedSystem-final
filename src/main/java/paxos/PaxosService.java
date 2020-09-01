package paxos;


import paxos.roles.Acceptor;
import paxos.roles.Proposer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PaxosService {

    private static final int PROPOSER_AMOUT = 4;
    private static final int ACCEPTOR_AMOUT = 7;

    private static final int CORE_SIZE = 5;
    private static final int MAX_SIZE = 7;
    private static final int KEEP_ALIVE = 3;

    public static CountDownLatch latch = new CountDownLatch(PROPOSER_AMOUT);

    // latch reference to the KVServer's latch
    private CountDownLatch externalLatch;

    private ThreadPoolExecutor threadPool;

    private List<Proposer> proposers;

    private List<Acceptor> acceptors;

    public PaxosService (CountDownLatch latch) {
        this.externalLatch = latch;
        threadPool = new ThreadPoolExecutor(CORE_SIZE, MAX_SIZE, KEEP_ALIVE, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(5), new ThreadPoolExecutor.DiscardOldestPolicy());
        proposers = new ArrayList<>();
        acceptors = new ArrayList<>();
    }

    public void initProposer(int id, String value) {
        proposers.add(new Proposer(id, "ProposalOfSever"+id, PROPOSER_AMOUT, value, acceptors));
    }

    public void runPaxos() {
        for	(int i = 0; i < ACCEPTOR_AMOUT; i++){
            acceptors.add(new Acceptor());
        }
        for	(int i = 0; i < PROPOSER_AMOUT; i++){
            threadPool.submit(proposers.get(i));
        }
        threadPool.shutdown();
        externalLatch.countDown();
    }
}
