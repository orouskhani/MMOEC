package edu.shenzen.maysam.entities.solutions;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import edu.shenzen.maysam.entities.enums.DistanceSet;
import edu.shenzen.maysam.entities.math.Point;
import edu.shenzen.maysam.entities.ml.Centroid;
import edu.shenzen.maysam.util.DistanceUtils;
import edu.shenzen.maysam.util.MORandomGenerator;
import edu.shenzen.maysam.util.ml.ClusteringUtil;
import org.uma.jmetal.problem.DoubleProblem;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.impl.AbstractGenericSolution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntToDoubleFunction;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ClusterSolution extends AbstractGenericSolution<Double, DoubleProblem> implements DoubleSolution {

    int numberOfClusters;
    int numberOfFeatures;
    double db;
    DistanceSet distanceSet;

    /**
     * Constructor
     *
     * @param problem
     */
    protected ClusterSolution(DoubleProblem problem, int numberOfClusters, int numberOfFeatures, DistanceSet distanceSet) {
        super(problem);

        setNumberOfClusters(numberOfClusters);
        setNumberOfFeatures(numberOfFeatures);

        List<Point> population = new ArrayList<>();
        initializeDoubleVariables(population);
        initializeObjectiveValues();

        this.distanceSet = distanceSet;

    }

    public ClusterSolution(DoubleProblem problem, List<Point> population, int numberOfClusters, int numberOfFeatures,
                           DistanceSet distanceSet) {
        super(problem);

        setNumberOfClusters(numberOfClusters);
        setNumberOfFeatures(numberOfFeatures);

        initializeDoubleVariables(population);
        initializeObjectiveValues();

        this.distanceSet = distanceSet;

    }


    /** Copy constructor */
    public ClusterSolution(ClusterSolution solution) {
        super(solution.problem) ;

        for (int i = 0; i < problem.getNumberOfVariables(); i++) {
            setVariableValue(i, solution.getVariableValue(i));
        }

        for (int i = 0; i < problem.getNumberOfObjectives(); i++) {
            setObjective(i, solution.getObjective(i)) ;
        }

        attributes = new HashMap<>(solution.attributes) ;
        setNumberOfClusters(solution.getNumberOfClusters());
        setNumberOfFeatures(solution.getNumberOfFeatures());

        this.distanceSet = solution.distanceSet;
    }

    public int getNumberOfClusters() {
        return numberOfClusters;
    }

    public void setNumberOfClusters(int numberOfClusters) {
        this.numberOfClusters = numberOfClusters;
    }

    public int getNumberOfFeatures() {
        return numberOfFeatures;
    }

    public void setNumberOfFeatures(int numberOfFeatures) {
        this.numberOfFeatures = numberOfFeatures;
    }

    @Override
    public Double getLowerBound(int index) {
        return problem.getLowerBound(index);
    }

    @Override
    public Double getUpperBound(int index) {
        return problem.getUpperBound(index);
    }

    @Override
    public String getVariableValueString(int index) {
        return getVariableValue(index).toString() ;
    }

    @Override
    public Solution<Double> copy() {
        return new ClusterSolution(this);
    }

    private void initializeDoubleVariables(List<Point> population) {
        int dimension = population.get(0).getDimensionality();

       /* List<Double> probs = IntStream.range(0, population.size()).mapToDouble(new IntToDoubleFunction() {
            @Override
            public double applyAsDouble(int value) {
                return (double) 1 / (value + 2);
            }
        }).boxed().collect(Collectors.toList());
*/
       int index;
        for (int i = 0; i < numberOfClusters; i++) {
            double prob = randomGenerator.nextDouble(0, 1);
            if(prob < 0.5){
               /* double probSelect = randomGenerator.nextDouble(0, 1);
                int index = 0;
                for(int j = 0 ; j < probs.size() ; j++){
                    if(probs.get(j) < probSelect){
                        index = j;
                        break;
                    }
                }
*/
                index = randomGenerator.nextInt(0, population.size() - 1);
                //index = MORandomGenerator.getNextRandomFromList(population.size());
                Point point = population.get(index);
                for (int j = 0; j < dimension; j++) {
                    setVariableValue(i * dimension + j , point.getCoords()[j]);
                }
            }
            else{
                for (int j = 0; j < dimension; j++) {
                    Double value = randomGenerator.nextDouble(getLowerBound(i), getUpperBound(i)) ;
                    setVariableValue(i * dimension + j , value);
                }

            }
        }
    }

    public double getDb() {
        return db;
    }

    public double calDBMeasure(Map<Integer, Point> dataset) {
        Clustering<Centroid> clustering = ClusteringUtil.convertSolutionToClusters(this, dataset, distanceSet);

        Map<Integer, Double> Si = new HashMap<>();
        for (Cluster<Centroid> entry : clustering.getAllClusters()) {
            double si = 0;
            Point center = entry.getModel().getFeatures();
            for (Point point : entry.getModel().getTrainingSamples().values()) {
                si += DistanceUtils.findDistance(point, center, distanceSet);
            }
            si /= entry.getModel().getTrainingSamples().size();

            Si.put(Integer.parseInt(entry.getName()), si);
        }

        Map<String, Double> distances = new HashMap<>();
        for (Cluster<Centroid> clusterAtI : clustering.getAllClusters()) {
            for (Cluster<Centroid> clusterAtJ : clustering.getAllClusters()) {
                String key1 = clusterAtI.getName() + ";" + clusterAtJ.getName();
                String key2 = clusterAtJ.getName() + ";" + clusterAtI.getName();
                if(distances.containsKey(key1) || distances.containsKey(key2)){
                    continue;
                }
                double distance = DistanceUtils.findDistance(clusterAtI.getModel().getFeatures(),
                        clusterAtJ.getModel().getFeatures(), distanceSet);

                distances.put(key1, distance);
                distances.put(key2, distance);

            }
        }

        Map<String , Double> rs = new HashMap<>();
        for (Cluster<Centroid> clusterAtI : clustering.getAllClusters()) {
            rs.put(clusterAtI.getName(),0.0);
            Double si = Si.get(Integer.valueOf(clusterAtI.getName()));

            for (Cluster<Centroid> clusterAtJ : clustering.getAllClusters()) {
                if(clusterAtI.getName().equalsIgnoreCase(clusterAtJ.getName())){
                    continue;
                }
                double temp = rs.get(clusterAtI.getName());
                Double sj = Si.get(Integer.valueOf(clusterAtJ.getName()));
                Double distance = distances.get(clusterAtI.getName() + ";" + clusterAtJ.getName());

                double value = (si + sj) / distance;
                if(temp < value){
                    rs.put(clusterAtI.getName(),value);
                }

            }
        }

        db = rs.values().stream().mapToDouble(f -> f.doubleValue()).sum() / clustering.getAllClusters().size();
        return db;
    }

    public ClusterSolution reset(int clusterIndex) {

        for (int i = clusterIndex * this.getNumberOfFeatures() ; i < (clusterIndex + 1) * this.getNumberOfFeatures(); i++) {
            Double value = randomGenerator.nextDouble(getLowerBound(i), getUpperBound(i)) ;
            setVariableValue(i, value) ;
        }

        initializeObjectiveValues();
        return this;
    }
}
