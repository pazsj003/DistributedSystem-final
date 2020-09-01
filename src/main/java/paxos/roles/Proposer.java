package paxos.roles;


import paxos.PaxosService;
import paxos.entity.AcceptorStep;
import paxos.entity.CommitRespond;
import paxos.entity.PrepareRespond;
import paxos.entity.Proposal;
import paxos.util.PaxosUtil;
import utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Proposer implements Runnable {

    int id = 0;

    private String name;

    private Proposal proposal;

    // round count for elections
    private int rounds = 0;

    // the threshold count for successful election
    private int passCount;

    // total proposer count, used for generate sequence number for proposals
    private int proposerCount;

    private List<Acceptor> acceptors;

    public Proposer (int id, String name, int proposerCount, String value, List<Acceptor> acceptors){
        this.id = id;
        this.name = name;
        this.acceptors = acceptors;
        this.proposerCount = proposerCount;
        rounds = 0;
        passCount = acceptors.size() / 2 + 1;
        proposal = new Proposal(PaxosUtil.generateSeq(id, rounds, proposerCount),
                name, value);
        rounds++;
    }

    public synchronized void prepare(){

        PrepareRespond respond = null;

        int promisedCount = 0; // how many acceptors has promised this proposal

        int restartCount = 0; // how many acceptors has crashed during one round

        while (true) {
            List<Proposal> promisedProposals = new ArrayList<>();
            List<Proposal> acceptedProposals = new ArrayList<>();
            promisedCount = 0;
            restartCount = 0;

            // iterate all acceptors
            for	(Acceptor acceptor : acceptors) {

                respond = acceptor.prepare(proposal);

                // simulate delay in network
                PaxosUtil.simulateDelay();

                // restart the crashed acceptors afterward
                if	(respond == null) {
                    restartCount++;
                    continue;
                }

                // acceptor promised this proposal
                if	(respond.isPromised()){
                    promisedCount++;
                } else {
                    // acceptor refuses because it has promised a proposal with greater sequence number
                    if	(respond.getAcceptorStep() == AcceptorStep.PROMISED){
                        promisedProposals.add(respond.getProposal());
                    }
                    // acceptor refuses because it has accepted a proposal with greater sequence number
                    if	(respond.getAcceptorStep() == AcceptorStep.ACCEPTED){
                        acceptedProposals.add(respond.getProposal());
                    }
                }
            }

            // create new threads and restart with new Acceptors to prepare the proposal
            AtomicInteger count = new AtomicInteger(); // newly promised proposals after restarting

            for (int i = 0; i < restartCount; i++) {
                new Thread(() -> {
                    Acceptor acceptor = new Acceptor();
                    PrepareRespond prepareRespond = acceptor.prepare(proposal);

                    if (prepareRespond == null) return;
                    // acceptor promised this proposal
                    if	(prepareRespond.isPromised()){
                        count.incrementAndGet();
                    } else {
                        // acceptor refuses because it has promised a proposal with greater sequence number
                        if	(prepareRespond.getAcceptorStep() == AcceptorStep.PROMISED){
                            promisedProposals.add(prepareRespond.getProposal());
                        }
                        // acceptor refuses because it has accepted a proposal with greater sequence number
                        if	(prepareRespond.getAcceptorStep() == AcceptorStep.ACCEPTED){
                            acceptedProposals.add(prepareRespond.getProposal());
                        }
                    }
                }).start();
            }
            promisedCount += count.get();

            // we have collected enough promises so break the loop to proceed commit
            if (promisedCount >= passCount) break;

            // replace current proposal of this proposer
            // with the accepted proposal with the greatest sequence number
            Proposal greatestProposal = getGreatestProposal(acceptedProposals);
            proposal.setSeq(PaxosUtil.generateSeq(id, rounds, proposerCount));
            if (greatestProposal != null)
                proposal.setValue(greatestProposal.getValue());

            rounds++;
        }
    }

    public synchronized void commit(){

        int acceptedCount = 0;
        int restartCount = 0;

        while (true) {
            List<Proposal> acceptedProposals = new ArrayList<>();
            acceptedCount = 0;
            restartCount = 0;
            for	(Acceptor acceptor : acceptors){
                CommitRespond respond = acceptor.commit(proposal);
                PaxosUtil.simulateDelay();

                if	(respond == null){
                    restartCount++;
                    continue;
                }

                if (respond.isAccepted()){
                    acceptedCount++;
                }else{
                    acceptedProposals.add(respond.getProposal());
                }
            }

            AtomicInteger count = new AtomicInteger();

            for (int i = 0; i < restartCount; i++) {
                new Thread(() -> {
                    Acceptor acceptor = new Acceptor();
                    CommitRespond commitRespond = acceptor.commit(proposal);

                    // acceptor accepted this proposal
                    if	(commitRespond != null && commitRespond.isAccepted()){
                        count.incrementAndGet();
                    } else {
                        // acceptor refuses because it has accepted a proposal with greater sequence number
                        if	(commitRespond != null && commitRespond.getAcceptorStep() == AcceptorStep.ACCEPTED){
                            acceptedProposals.add(commitRespond.getProposal());
                        }
                    }
                }).start();
            }
            acceptedCount += count.get();

            if (acceptedCount >= passCount){
                Utils.timedLog(name + " has voted the proposal result: " + proposal.getValue());
                Learner.updateLearner(proposal);
                return;
            } else {
                Proposal greatestProposal = getGreatestProposal(acceptedProposals);
                proposal.setSeq(PaxosUtil.generateSeq(id, rounds, proposerCount));
                if (greatestProposal != null)
                    proposal.setValue(greatestProposal.getValue());

                rounds++;
            }
        }
    }

    // find the proposal with greatest sequence number
    private Proposal getGreatestProposal(List<Proposal> proposals){
        int max = Integer.MIN_VALUE, idx = 0;
        for (int i = 0; i < proposals.size(); i++) {
            if (proposals.get(i).getSeq() > max) {
                max = proposals.get(i).getSeq();
                idx = i;
            }
        }

        return proposals.get(idx);
    }

    @Override
    public void run() {
        PaxosService.latch.countDown();
        try {
            PaxosService.latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        prepare();

        commit();
    }

}
