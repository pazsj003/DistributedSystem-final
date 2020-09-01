package paxos.roles;


import paxos.entity.AcceptorStep;
import paxos.entity.CommitRespond;
import paxos.entity.PrepareRespond;
import paxos.entity.Proposal;
import paxos.util.PaxosUtil;
import utils.Utils;

public class Acceptor {

    // current step of acceptor
    private AcceptorStep step = AcceptorStep.INIT;

    // most recent promised proposal, typically has the greatest sequence number
    private Proposal promisedProposal = new Proposal();

    // most recent accepted proposal, typically has the greatest sequence number
    private Proposal acceptedProposal = new Proposal();

    public synchronized PrepareRespond prepare(Proposal pendingProposal) {
        PrepareRespond prepareRespond = new PrepareRespond();

        // randomly crash this acceptor
        if (PaxosUtil.simulateCrashing()) {
            Utils.timedLog("Network crashed at proposal: " + pendingProposal.getName());
            return null;
        }

        switch (step) {
            case INIT: // new proposal
                prepareRespond.setAcceptorStep(step);
                prepareRespond.setPromised(true);
                prepareRespond.setProposal(null);
                step = AcceptorStep.PROMISED; // step to the promised status
                promisedProposal.cloneProposal(pendingProposal);
                return prepareRespond;
            case PROMISED: // this acceptor has promised a proposal before
                if (promisedProposal.getSeq() > pendingProposal.getSeq()) {
                    prepareRespond.setPromised(false);
                } else { // only promise when it comes a greater sequence number
                    promisedProposal.cloneProposal(pendingProposal);
                    prepareRespond.setPromised(true);
                }
                prepareRespond.setAcceptorStep(step);
                prepareRespond.setProposal(promisedProposal);
                return prepareRespond;
            case ACCEPTED: // this acceptor has accepted a proposal before
                // only update when it comes a greater sequence number and has the same value
                // because this acceptor has committed a proposal so it never changes
                if (promisedProposal.getSeq() < pendingProposal.getSeq()
                        && promisedProposal.getValue().equals(pendingProposal.getValue())) {
                    promisedProposal.setSeq(pendingProposal.getSeq());
                    prepareRespond.setPromised(true);
                } else { // refuse and return the latest promised proposal
                    prepareRespond.setPromised(false);
                }
                prepareRespond.setAcceptorStep(step);
                prepareRespond.setProposal(promisedProposal);
                return prepareRespond;
            default:
                break;
        }

        return null;
    }

    public synchronized CommitRespond commit(Proposal pendingProposal) {
        CommitRespond commitRespond = new CommitRespond();

        // randomly crash this acceptor
        if (PaxosUtil.simulateCrashing()) {
            Utils.timedLog("Network crashed at proposal: " + pendingProposal.getName());
            return null;
        }

        switch (step) {
            // this only happens when all acceptors have promised and moved to commit
            // then an acceptor crashes at that time
            // current step is INIT because we create a new thread with a new acceptor
            case INIT:
                commitRespond.setAcceptorStep(step);
                commitRespond.setAccepted(true);
                acceptedProposal.cloneProposal(pendingProposal);
                commitRespond.setProposal(acceptedProposal);
                step = AcceptorStep.ACCEPTED; // step to the promised status
                return commitRespond;
            case PROMISED:
                // only accept if it comes wit ha greater sequence number
                if (pendingProposal.getSeq() > promisedProposal.getSeq()) {
                    promisedProposal.cloneProposal(pendingProposal);
                    acceptedProposal.cloneProposal(pendingProposal);
                    step = AcceptorStep.ACCEPTED;
                    commitRespond.setAccepted(true);
                    return commitRespond;
                } else {
                    commitRespond.setAccepted(false);
                }
                commitRespond.setAcceptorStep(step);
                commitRespond.setProposal(promisedProposal);
                return commitRespond;
            case ACCEPTED:
                if (pendingProposal.getSeq() > acceptedProposal.getSeq()
                        && pendingProposal.getValue().equals(acceptedProposal.getValue())) {
                    acceptedProposal.setSeq(pendingProposal.getSeq());
                    commitRespond.setAccepted(true);
                    return commitRespond;
                } else { // refuse and return the latest accepted proposal
                    commitRespond.setAccepted(false);
                }
                commitRespond.setAcceptorStep(step);
                commitRespond.setProposal(acceptedProposal);
                return commitRespond;
        }
        return null;
    }
}
