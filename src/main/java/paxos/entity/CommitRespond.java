package paxos.entity;


/**
 * The respond entity from an acceptor after COMMIT process
 */
public class CommitRespond {

    private boolean isAccepted;

    private AcceptorStep acceptorStep = AcceptorStep.INIT;

    private Proposal proposal;

    public boolean isAccepted() {
        return isAccepted;
    }

    public void setAccepted(boolean accepted) {
        isAccepted = accepted;
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
