package edu.shenzen.maysam.algorithm.operators;

import de.lmu.ifi.dbs.elki.data.Cluster;
import edu.shenzen.maysam.entities.enums.DistanceSet;
import edu.shenzen.maysam.entities.math.Point;
import edu.shenzen.maysam.entities.ml.Centroid;
import edu.shenzen.maysam.entities.solutions.ClusterSolution;
import edu.shenzen.maysam.util.NetworkUtil;
import edu.shenzen.maysam.util.ml.ClusteringUtil;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.solution.util.RepairDoubleSolution;
import org.uma.jmetal.solution.util.RepairDoubleSolutionAtBounds;
import org.uma.jmetal.util.JMetalException;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.pseudorandom.RandomGenerator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MOMutation implements MutationOperator<DoubleSolution> {
    private RandomGenerator<Double> randomGenerator ;
    private int dimensionOfEachPoint;
    private Map<Integer, Point> dataset;
    private double mutationProbability;
    private double upperbound;
    private double lowerbound;
    private RepairDoubleSolution solutionRepair ;
    private Double probabilityOfParticle;
    private DistanceSet distanceSet;

    public MOMutation(int dimensionOfEachPoint, double probabilityOfParticle, double mutationProbability, double upperbound, double lowerbound, Map<Integer, Point> dataset, DistanceSet distanceSet) {
        this(() -> JMetalRandom.getInstance().nextDouble(), dimensionOfEachPoint, mutationProbability, upperbound, lowerbound, new RepairDoubleSolutionAtBounds() , dataset, distanceSet);
        this.probabilityOfParticle = probabilityOfParticle;
    }

    public MOMutation(RandomGenerator<Double> randomGenerator, int dimensionOfEachPoint, double mutationProbability,
                      double upperbound, double lowerbound, RepairDoubleSolution solutionRepair ,Map<Integer, Point> dataset, DistanceSet distanceSet) {
        this.randomGenerator = randomGenerator;
        this.dimensionOfEachPoint = dimensionOfEachPoint;
        this.dataset = dataset;
        this.mutationProbability = mutationProbability;
        this.upperbound = upperbound;
        this.lowerbound = lowerbound;
        this.solutionRepair = solutionRepair;
        this.distanceSet = distanceSet;
    }

    @Override
    public DoubleSolution execute(DoubleSolution solution) {
        if (null == solution) {
            throw new JMetalException("Null parameter") ;
        }

        doMutation(mutationProbability, solution);
        return solution;
    }

    private void doMutation(double probability, DoubleSolution solution) {
        if (randomGenerator.getRandomValue() < probabilityOfParticle) {
            Map<String, Cluster<Centroid>> clusterMap = ClusteringUtil.convertSolutionToClusters((ClusterSolution) solution, dataset, distanceSet).getAllClusters().stream()
                    .collect(Collectors.toMap(Cluster::getName, cluster -> cluster));

            double rnd, delta1, delta2, mutPow, deltaq;
            double y, yl, yu, val, xy;
            for (int i = 0; i < solution.getNumberOfVariables(); i += dimensionOfEachPoint) {
                if (randomGenerator.getRandomValue() <= probability) {
                    double plusOrMinus = randomGenerator.getRandomValue();
                    Cluster<Centroid> cluster = clusterMap.get(String.valueOf((int) (i / dimensionOfEachPoint)));
                    if (cluster == null) { // which means modularity is negative
                        double[] coords = new double[dimensionOfEachPoint];
                        for (int j = i; j < i + dimensionOfEachPoint; j++) {
                            coords[j-i] = solution.getVariableValue(j);
                        }
                        Point oblOfThePoint = NetworkUtil.findOBLOfThePoint(new Point(coords), lowerbound, upperbound);
                        for (int j = i; j < i + dimensionOfEachPoint; j++) {
                            solution.setVariableValue(j, oblOfThePoint.get(j-i));
                        }
                    } else { // which means modularity is positive
                        double[] coords = new double[dimensionOfEachPoint];
                        for (int j = i; j < i + dimensionOfEachPoint; j++) {
                            coords[j-i] = solution.getVariableValue(j);
                        }
                        Point point = new Point(coords);
                        Point p = NetworkUtil.findNearestCentroid(point, (ClusterSolution) solution, dataset, distanceSet);

                        double min;
                        double max;
                        for (int j = i; j < i + dimensionOfEachPoint; j++) {
                            min = Math.min(p.get(j-i), point.get(j-i));
                            max = Math.max(p.get(j-i), point.get(j-i));

                            solution.setVariableValue(j, min + randomGenerator.getRandomValue() * (max - min));
                        }
                    }
                }
            }
        }
    }
}

/*
Map<String, Cluster<Centroid>> clusterMap = ClusteringUtil.convertSolutionToClusters((ClusterSolution) solution, dataset).getAllClusters().stream()
                    .collect(Collectors.toMap(Cluster::getName, cluster -> cluster));

            double rnd, delta1, delta2, mutPow, deltaq;
            double y, yl, yu, val, xy;
            for (int i = 0; i < solution.getNumberOfVariables(); i += dimensionOfEachPoint) {
                if (randomGenerator.getRandomValue() <= probability) {
                    double plusOrMinus = randomGenerator.getRandomValue();
                    Cluster<Centroid> cluster = clusterMap.get(String.valueOf((int) (i / dimensionOfEachPoint)));
                    if (cluster == null) { // which means modularity is negative
                        double min = 0.5;
                        double max = 1;
                        for (int j = i; j < i + dimensionOfEachPoint; j++) {
                            rnd = min + randomGenerator.getRandomValue() * (max - min);

                            if (plusOrMinus > 0.5) {
                                val = Math.min(upperbound, (1 + rnd) * solution.getVariableValue(j));
                            } else {
                                val = -1 * Math.max(lowerbound, (1 - rnd) * solution.getVariableValue(j));
                            }
                            double v = solutionRepair.repairSolutionVariableValue(solution.getVariableValue(j) + val, lowerbound, upperbound);
                            solution.setVariableValue(j, v);
                        }
                        // double step = Math.min
                    } else { // which means modularity is positive
                        double[] coords = new double[dimensionOfEachPoint];
                        for (int j = i; j < i + dimensionOfEachPoint; j++) {
                            coords[j-i] = solution.getVariableValue(j);
                        }
                        Point point = new Point(coords);
                        Point p = NetworkUtil.findNearestPoint(point, String.valueOf((int) (i / dimensionOfEachPoint)),clusterMap);

                        double min;
                        double max;
                        for (int j = i; j < i + dimensionOfEachPoint; j++) {
                            min = Math.min(p.get(j-i), point.get(j-i));
                            max = Math.max(p.get(j-i), point.get(j-i));

                            solution.setVariableValue(j, min + randomGenerator.getRandomValue() * (max - min));
                        }
                    }
                }
            }
 */