package paxos.entity;

/**
 * The respond entity from an acceptor after PREPARE process
 */
public class PrepareRespond {

    private boolean isPromised;

    private AcceptorStep acceptorStep = AcceptorStep.INIT;

    private Proposal proposal;

    public boolean isPromised() {
        return isPromised;
    }

    public void setPromised(boolean promised) {
        isPromised = promised;
    }

    public AcceptorStep getAcceptorStep() {
        return acceptorStep;
    }

    public void setAcceptorStep(AcceptorStep acceptorStep) {
        this.acceptorStep = acceptorStep;
    }

    public Proposal getProposal() {
        return proposal;
    }

    public void setProposal(Proposal proposal) {
        this.proposal = proposal;
    }
}
