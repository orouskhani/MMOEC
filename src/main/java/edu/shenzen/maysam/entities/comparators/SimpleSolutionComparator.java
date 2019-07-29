package edu.shenzen.maysam.entities.comparators;

import edu.shenzen.maysam.entities.solutions.ClusterSolution;

import java.util.Comparator;

public class SimpleSolutionComparator implements Comparator<ClusterSolution> {

    public SimpleSolutionComparator() {

    }

    @Override
    public int compare(ClusterSolution o1, ClusterSolution o2) {
     return new Double(o1.getDb()).compareTo(o2.getDb());
    }

}
