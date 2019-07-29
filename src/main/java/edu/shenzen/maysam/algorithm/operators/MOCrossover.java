package edu.shenzen.maysam.algorithm.operators;

import com.google.common.base.Functions;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import edu.shenzen.maysam.entities.enums.DistanceSet;
import edu.shenzen.maysam.entities.math.Point;
import edu.shenzen.maysam.entities.ml.Centroid;
import edu.shenzen.maysam.entities.solutions.ClusterSolution;
import edu.shenzen.maysam.util.ml.ClusteringUtil;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.solution.util.RepairDoubleSolution;
import org.uma.jmetal.solution.util.RepairDoubleSolutionAtBounds;
import org.uma.jmetal.util.JMetalException;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.pseudorandom.RandomGenerator;

import java.util.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public class MOCrossover implements CrossoverOperator<DoubleSolution> {

    private static final double EPS = 1.0e-14;
    private final double distributionIndex;

    private RandomGenerator<Double> randomGenerator ;
    private Map<Integer, Point> dataset;
    private double crossoverProbability ;
    private RepairDoubleSolution solutionRepair ;
    private DistanceSet distanceSet;


    @Override
    public int getNumberOfRequiredParents() {
        return 2;
    }

    @Override
    public int getNumberOfGeneratedChildren() {
        return 2;
    }

    public MOCrossover(double crossoverProbability, double distributionIndex, Map<Integer, Point> dataset, DistanceSet distanceSet) {
        this(() -> JMetalRandom.getInstance().nextDouble(), crossoverProbability, distributionIndex,
                new RepairDoubleSolutionAtBounds(), dataset, distanceSet);
    }

    public MOCrossover(RandomGenerator<Double> randomGenerator, double crossoverProbability, double distributionIndex,
                       RepairDoubleSolution solutionRepair, Map<Integer, Point> dataset, DistanceSet distanceSet) {
        if (crossoverProbability < 0) {
            throw new JMetalException("Crossover probability is negative: " + crossoverProbability) ;
        }
        this.dataset = new HashMap<>(dataset);

        this.randomGenerator = randomGenerator;
        this.distributionIndex = distributionIndex;
        this.crossoverProbability = crossoverProbability;
        this.solutionRepair = solutionRepair ;
        this.distanceSet = distanceSet;

    }

    @Override
    public List<DoubleSolution> execute(List<DoubleSolution> clusterSolutions) {
        if (null == clusterSolutions) {
            throw new JMetalException("Null parameter") ;
        } else if (clusterSolutions.size() != 2) {
            throw new JMetalException("There must be two parents instead of " + clusterSolutions.size()) ;
        }

        return doCrossover(crossoverProbability, clusterSolutions.get(0), clusterSolutions.get(1)) ;
    }

    private List<DoubleSolution> doCrossover(double probability, DoubleSolution parent1, DoubleSolution parent2) {
        List<DoubleSolution> offspring = new ArrayList<DoubleSolution>(2);

        offspring.add((ClusterSolution) parent1.copy()) ;
        offspring.add((ClusterSolution) parent2.copy()) ;


        offspring.set(0 , changeOffSpring(parent1, parent2, probability, offspring, 0));
        offspring.set(1 , changeOffSpring(parent2, parent1, probability, offspring, 1));

        return offspring;

    }

    private ClusterSolution changeOffSpring(DoubleSolution parent1, DoubleSolution parent2, double probability, List<DoubleSolution> offspring , int index) {
        Map<String, Cluster<Centroid>> clusters1 = ClusteringUtil.convertSolutionToClusters((ClusterSolution) parent1, dataset, distanceSet).getAllClusters().stream()
                .collect(Collectors.toMap(Cluster::getName, cluster -> cluster));

        DoubleSolution doubleSolution = offspring.get(index);
        int numberOfFeatures = ((ClusterSolution) parent1).getNumberOfFeatures();
        for(int i = 0 ; i < ((ClusterSolution) parent1).getNumberOfClusters() ; i++){
            int clusterIndex = i * numberOfFeatures;
            if(clusters1.containsKey(String.valueOf(clusterIndex))){
                for(int j = clusterIndex ; j < clusterIndex + numberOfFeatures ; j++){
                    Double randomValue = randomGenerator.getRandomValue();
                    if(randomValue > probability){
                        doubleSolution.setVariableValue(j, findAValFromSolutions(parent1, parent2));
                    }
                }
            }
            else{
                for(int j = clusterIndex ; j < clusterIndex + numberOfFeatures ; j++){
                    Double randomValue = randomGenerator.getRandomValue();
                    if(randomValue < probability){
                        doubleSolution.setVariableValue(j, findAValFromSolutions(parent1, parent2));
                    }
                }
            }
        }
        return (ClusterSolution) doubleSolution;
    }

    private Double findAValFromSolutions(DoubleSolution parent1, DoubleSolution parent2) {
        int i = new Random().nextInt(parent2.getNumberOfVariables());
        return parent1.getVariableValue(i);

    }
}

/*List<DoubleSolution> offspring = new ArrayList<DoubleSolution>(2);

        offspring.add((ClusterSolution) parent1.copy()) ;
        offspring.add((ClusterSolution) parent2.copy()) ;

        int i;
        double rand;
        double y1, y2, lowerBound, upperBound;
        double c1, c2;
        double alpha, beta, betaq;
        double valueX1, valueX2;

        double firstProb = 2 * probability / 3;

        Double generatorRandomValue = randomGenerator.getRandomValue();
        if (generatorRandomValue <= probability && generatorRandomValue > firstProb ) {
            List<Cluster<Centroid>> clusters1 = ClusteringUtil.convertSolutionToClusters((ClusterSolution) parent1, dataset, distanceSet).getAllClusters();
            List<Integer> clusters1Ids = clusters1.stream().mapToInt(new ToIntFunction<Cluster<Centroid>>() {
                @Override
                public int applyAsInt(Cluster<Centroid> value) {
                    return Integer.parseInt(value.getName());
                }
            }).boxed().collect(Collectors.toList());

            List<Cluster<Centroid>> clusters2 = ClusteringUtil.convertSolutionToClusters((ClusterSolution) parent2, dataset, distanceSet).getAllClusters();
            List<Integer> clusters2Ids = clusters2.stream().mapToInt(new ToIntFunction<Cluster<Centroid>>() {
                @Override
                public int applyAsInt(Cluster<Centroid> value) {
                    return Integer.parseInt(value.getName());
                }
            }).boxed().collect(Collectors.toList());


            List<Cluster<Centroid>> allCluster = new ArrayList<>();
            allCluster.addAll(clusters1);
            allCluster.addAll(clusters2);

            List<Centroid> centroids = new ArrayList<>();
            for (Cluster<Centroid> cluster : allCluster) {
                int index = 0;
                for(i = 0 ; i < centroids.size() ; i++){
                    if( cluster.getModel().getModularity() < centroids.get(i).getModularity()) {
                        index = i + 1;
                    }
                }
                centroids.add(index, cluster.getModel());
            }
            int index = 0;
            for (int j = 0; j < clusters1Ids.size(); j++) {
                Centroid centroid = centroids.get(j);
                Point features = centroid.getFeatures();

                index = clusters1Ids.get(j);
                for (int i1 = 0; i1 < features.getCoords().length; i1++) {
                    offspring.get(0).setVariableValue( index * features.getCoords().length + i1 , features.getCoords()[i1]);
                }
            }
            for (int j = 0; j < clusters2Ids.size(); j++) {
                Centroid centroid = centroids.get(j + clusters1Ids.size());
                Point features = centroid.getFeatures();

                index = clusters2Ids.get(j);
                for (int i1 = 0; i1 < features.getCoords().length; i1++) {
                    offspring.get(1).setVariableValue( index * features.getCoords().length + i1 , features.getCoords()[i1]);
                }
            }
        }
        else if (generatorRandomValue <= firstProb) {
            for (i = 0; i < parent1.getNumberOfVariables(); i++) {
                valueX1 = parent1.getVariableValue(i);
                valueX2 = parent2.getVariableValue(i);
                if (randomGenerator.getRandomValue() <= 0.5) {
                    if (Math.abs(valueX1 - valueX2) > EPS) {

                        if (valueX1 < valueX2) {
                            y1 = valueX1;
                            y2 = valueX2;
                        } else {
                            y1 = valueX2;
                            y2 = valueX1;
                        }

                        lowerBound = parent1.getLowerBound(i);
                        upperBound = parent1.getUpperBound(i);

                        rand = randomGenerator.getRandomValue();
                        beta = 1.0 + (2.0 * (y1 - lowerBound) / (y2 - y1));
                        alpha = 2.0 - Math.pow(beta, -(distributionIndex + 1.0));

                        if (rand <= (1.0 / alpha)) {
                            betaq = Math.pow(rand * alpha, (1.0 / (distributionIndex + 1.0)));
                        } else {
                            betaq = Math
                                    .pow(1.0 / (2.0 - rand * alpha), 1.0 / (distributionIndex + 1.0));
                        }
                        c1 = 0.5 * (y1 + y2 - betaq * (y2 - y1));

                        beta = 1.0 + (2.0 * (upperBound - y2) / (y2 - y1));
                        alpha = 2.0 - Math.pow(beta, -(distributionIndex + 1.0));

                        if (rand <= (1.0 / alpha)) {
                            betaq = Math.pow((rand * alpha), (1.0 / (distributionIndex + 1.0)));
                        } else {
                            betaq = Math
                                    .pow(1.0 / (2.0 - rand * alpha), 1.0 / (distributionIndex + 1.0));
                        }
                        c2 = 0.5 * (y1 + y2 + betaq * (y2 - y1));

                        c1 = solutionRepair.repairSolutionVariableValue(c1, lowerBound, upperBound) ;
                        c2 = solutionRepair.repairSolutionVariableValue(c2, lowerBound, upperBound) ;

                        if (randomGenerator.getRandomValue() <= 0.5) {
                            offspring.get(0).setVariableValue(i, c2);
                            offspring.get(1).setVariableValue(i, c1);
                        } else {
                            offspring.get(0).setVariableValue(i, c1);
                            offspring.get(1).setVariableValue(i, c2);
                        }
                    } else {
                        offspring.get(0).setVariableValue(i, valueX1);
                        offspring.get(1).setVariableValue(i, valueX2);
                    }
                } else {
                    offspring.get(0).setVariableValue(i, valueX1);
                    offspring.get(1).setVariableValue(i, valueX2);
                }
            }
        }

        return offspring;*/
