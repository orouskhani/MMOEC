package edu.shenzen.maysam.entities.problems;

import de.lmu.ifi.dbs.elki.data.Cluster;
import edu.shenzen.maysam.entities.enums.DistanceSet;
import edu.shenzen.maysam.entities.math.Point;
import edu.shenzen.maysam.entities.ml.Centroid;
import edu.shenzen.maysam.entities.solutions.ClusterSolution;
import edu.shenzen.maysam.util.DistanceUtils;
import edu.shenzen.maysam.util.ml.ClusteringUtil;
import org.uma.jmetal.problem.impl.AbstractDoubleProblem;
import org.uma.jmetal.solution.DoubleSolution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

public class ClusterProblem extends AbstractDoubleProblem{

    private Integer numberOfClusters;
    private Integer numberOfFeatures;
    private List<Point> population;
    private Map<Integer, Point> dataset;
    private Point referencePoint;
    private DistanceSet distanceSet;

    public ClusterProblem(int numberOfClusters , int numberOfFeatures, int numberOfVariables, List<Point> population,
                          Map<Integer, Point> dataset, List<Double> parameterLowerLimit, List<Double> parameterUpperLimit, DistanceSet distanceSet) {
        setNumberOfClusters(numberOfClusters);
        setNumberOfFeatures(numberOfFeatures);
        setNumberOfVariables(numberOfClusters*numberOfFeatures);
        setName("Image Segmentation");
        setNumberOfObjectives(2);
        setDataset(dataset);
        setReferencePoint(findReferencePoint(new ArrayList<Point>(dataset.values())));

       // System.out.println("Reference Point : " + getReferencePoint());

        this.population = new ArrayList<>(population);

        List<Double> lowerLimit = new ArrayList<>(parameterLowerLimit) ;
        List<Double> upperLimit = new ArrayList<>(parameterUpperLimit) ;

        setLowerLimit(lowerLimit);
        setUpperLimit(upperLimit);

        this.distanceSet = distanceSet;

    }

    public Map<Integer, Point> getDataset() {
        return dataset;
    }

    public void setDataset(Map<Integer, Point> dataset) {
        this.dataset = dataset;
    }

    private Point findReferencePoint(List<Point> dataset) {

        double length = dataset.size();
        double[] referencePoint = new double[dataset.get(0).getDimensionality()];
        for(int i = 0 ; i < dataset.size() ; i++){
            for(int j = 0 ; j < dataset.get(i).getCoords().length ; j++){
                referencePoint[j] += dataset.get(i).getCoords()[j];
            }
        }
        for(int j = 0 ; j < referencePoint.length ; j++){
            referencePoint[j] /= length;
        }

        return new Point(referencePoint);
    }

    public Integer getNumberOfClusters() {
        return numberOfClusters;
    }

    public void setNumberOfClusters(Integer numberOfClusters) {
        this.numberOfClusters = numberOfClusters;
    }

    public Integer getNumberOfFeatures() {
        return numberOfFeatures;
    }

    public void setNumberOfFeatures(Integer numberOfFeatures) {
        this.numberOfFeatures = numberOfFeatures;
    }

    public Point getReferencePoint() {
        return referencePoint;
    }

    public void setReferencePoint(Point referencePoint) {
        this.referencePoint = referencePoint;
    }

    @Override
    public void evaluate(DoubleSolution solution) {
        double[] f = new double[getNumberOfObjectives()];

        List<Centroid> centroids = ClusteringUtil.convertSolutionToClusters((ClusterSolution) (solution), getDataset(), distanceSet).getAllClusters().stream().map(new Function<Cluster<Centroid>, Centroid>() {
            @Override
            public Centroid apply(Cluster<Centroid> centroidCluster) {
                return centroidCluster.getModel();
            }
        }).collect(Collectors.toList());

        int clusterIndex = ClusteringUtil.checkValiditiy(centroids);
        while(clusterIndex >= 0){
            ((ClusterSolution)solution).reset(clusterIndex);

            centroids.clear();
            centroids = ClusteringUtil.convertSolutionToClusters((ClusterSolution) (solution), getDataset(), distanceSet).getAllClusters().stream().map(new Function<Cluster<Centroid>, Centroid>() {
                @Override
                public Centroid apply(Cluster<Centroid> centroidCluster) {
                    return centroidCluster.getModel();
                }
            }).collect(Collectors.toList());

            clusterIndex = ClusteringUtil.checkValiditiy(centroids);
        }
        double d = DistanceUtils.deviation(new ArrayList<Point>(dataset.values()), centroids.stream().map(new Function<Centroid, Point>() {
            @Override
            public Point apply(Centroid centroid) {
                return centroid.getFeatures();
            }
        }).collect(Collectors.toList()), distanceSet);

        f[0] = d;//((ClusterSolution) solution).calDBMeasure(dataset);
        solution.setObjective(0, f[0]);
        /*double dd = centroids.stream().mapToDouble(new ToDoubleFunction<Centroid>() {
            @Override
            public double applyAsDouble(Centroid value) {
                return value.getModularity();
            }
        }).sum();*/

        double dd = DistanceUtils.getDistanceOfCentroidFromReferencePoint(centroids, referencePoint, distanceSet);
        dd = dd / centroids.size();
        f[1] = 1 / 1 + dd ;

        solution.setObjective(1, f[1]);
    }

    @Override
    public DoubleSolution createSolution() {
        return new ClusterSolution(this, population, numberOfClusters, numberOfFeatures, distanceSet);
    }
}
