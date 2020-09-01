package paxos.roles;


import paxos.entity.Proposal;

import java.util.HashMap;
import java.util.Map;

public class Learner {

    private static Map<Integer, Proposal> acceptedHistory;

    public Learner() {
        acceptedHistory = new HashMap<>();
    }

    // invoked every time a proposal has eventually voted
    public static void updateLearner (Proposal proposal) {
        acceptedHistory.putIfAbsent(proposal.getSeq(), proposal);
    }

}
