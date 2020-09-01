package paxos.entity;

public class Proposal {

    // sequence number of a proposal
    int seq;

    String name;

    String value;

    public Proposal() { }

    public Proposal(int seq, String name, String value){
        this.seq = seq;
        this.name = name;
        this.value = value;
    }

    public void cloneProposal(Proposal proposal){
        this.seq = proposal.seq;
        this.name = proposal.name;
        this.value = proposal.value;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
