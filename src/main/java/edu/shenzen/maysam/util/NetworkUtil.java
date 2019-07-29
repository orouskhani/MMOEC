package edu.shenzen.maysam.util;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import edu.shenzen.maysam.entities.enums.DistanceSet;
import edu.shenzen.maysam.entities.math.Point;
import edu.shenzen.maysam.entities.ml.Centroid;
import edu.shenzen.maysam.entities.solutions.ClusterSolution;
import edu.shenzen.maysam.util.ml.ClusteringUtil;
import org.jgrapht.Graph;
import org.jgrapht.alg.scoring.ClosenessCentrality;
import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import smile.math.Math;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class NetworkUtil {

    protected final static JMetalRandom randomGenerator = JMetalRandom.getInstance();

    public static List<Point> findInitPointAndTheirOBL(Map<Integer, Point> dataset, int topK, double upperbound, double lowerbound, DistanceSet distanceSet) {

        Map<Integer, Double> cc = findInitPointByPageRankCentrality(dataset, distanceSet);
        Map<Integer, Double> normCC = normalizeMap(cc);

        Map<Integer, Double> laplacian = findInitPointByLaplacianCentrality(dataset, distanceSet);
        Map<Integer, Double> normLaplacian = normalizeMap(laplacian);


        Map<Integer, Double> map = new HashMap<>();
        for (Integer index : dataset.keySet()) {
            map.put(index, normCC.get(index) + normLaplacian.get(index));
        }

        Map<Integer, Double> sorted = map
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                                LinkedHashMap::new));


        List<Integer> indices = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : sorted.entrySet()) {
            indices.add(entry.getKey());
        }

        List<Point> result = new ArrayList<>();
        List<Integer> integers = indices.subList(0, topK);
        for (Integer integer : integers) {
            result.add(dataset.get(integer));
        }
        //result.addAll(indices.subList(0,topK));
       /* int fromListSize = topK / 2;
        List<Point> originalPoints = new ArrayList<>();
        for(int i = 0 ; i < fromListSize ; i++){
            result.add(dataset.get(indices.get(i)));
            originalPoints.add(dataset.get(indices.get(i)));
        }

        for (Point originalPoint : originalPoints) {
            Point obl = findOBLOfThePoint(originalPoint, lowerbound, upperbound);
            result.add(obl);
        }
*/

        //   Collections.shuffle(result);
        return result;
    }

    public static Point findOBLOfThePoint(Point originalPoint, double lowerbound, double upperbound) {
        double[] newCoords = new double[originalPoint.getCoords().length];
        for (int i = 0; i < newCoords.length; i++) {
            newCoords[i] = upperbound + lowerbound - originalPoint.getCoords()[i];
        }
        return new Point(newCoords);
    }

    private static Map<Integer, Double> normalizeMap(Map<Integer, Double> values) {
        double min = Collections.min(values.values());
        double max = Collections.max(values.values());

        return values.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey, e -> ((e.getValue() - min)
                        / (max - min))));

    }


    public static List<Point> findRandomPoints(Map<Integer, Point> dataset, int heuristicPointsSize) {
        List<Point> result = new ArrayList<>(dataset.values());
        Collections.shuffle(result);
        return result.subList(0, heuristicPointsSize);
    }


    public static Map<Integer, Double> findInitPointByLaplacianCentrality(Map<Integer, Point> dataset, DistanceSet distanceSet) {
        double[][] graph = makeAdjacencyMatrix(dataset, distanceSet);

        double energy = calculateEnergy(graph);
        Map<Integer, Double> importance = new HashMap<>();
        for (int i = 0; i < graph.length; i++) {
            double[][] newGraph = removeNode(graph, i);
            importance.put(i, energy - calculateEnergy(newGraph));
        }

        return importance;
    }

    public static Map<Integer, Double> findInitPointByPageRankCentrality(Map<Integer, Point> dataset, DistanceSet distanceSet) {
        double[][] graph = makeAdjacencyMatrix(dataset, distanceSet);
        int size = graph.length;

        Graph<Integer, DefaultWeightedEdge> g = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        for (int i = 0; i < size; i++) {
            g.addVertex(i);
        }
        for (int i = 0; i < size; i++) {
            for (int j = i; j < size; j++) {
                g.addEdge(i, j, new DefaultWeightedEdge());
                g.setEdgeWeight(i, j, graph[i][j]);
            }
        }

        PageRank<Integer, DefaultWeightedEdge> cc = new PageRank<>(g);

        Map<Integer, Double> importance = new HashMap<>();
        for (int i = 0; i < size; i++) {
            importance.put(i, cc.getVertexScore(i));
        }
        return importance;
    }

    private static List<Point> findTopK(Map<Integer, Point> dataset, List<Double> importance, int topK) {
        ArrayIndexComparator comparator = new ArrayIndexComparator(importance);
        Integer[] indexes = comparator.createIndexArray();
        Arrays.sort(indexes, comparator);

        List<Point> result = new ArrayList<>();
        for (int i = 0; i < topK; i++) {
            result.add(dataset.get(indexes[i]));
        }
        return result;
    }

    private static double[][] makeAdjacencyMatrix(Map<Integer, Point> dataset, DistanceSet distanceSet) {
        int size = dataset.size();
        double[][] graph = new double[size][size];

        for (int i = 0; i < graph.length; i++) {
            for (int j = i; j < graph[i].length; j++) {
                graph[i][j] = (double) 1 / (1 + DistanceUtils.findDistance(dataset.get(i), dataset.get(j), distanceSet));
                graph[j][i] = graph[i][j];
            }
        }
        return graph;
    }


    private static double[][] removeNode(double[][] graph, int index) {
        double[][] newgraph = new double[graph.length - 1][graph.length - 1];
        for (int i = 0; i < graph.length; i++) {
            for (int j = 0; j < graph[i].length; j++) {
                if (i < index && j < index) {
                    newgraph[i][j] = graph[i][j];
                } else if (i < index && j > index) {
                    newgraph[i][j - 1] = graph[i][j];
                } else if (i > index && j < index) {
                    newgraph[i - 1][j] = graph[i][j];
                } else if (i > index && j > index) {
                    newgraph[i - 1][j - 1] = graph[i][j];
                }
            }

        }
        return newgraph;
    }

    private static double calculateEnergy(double[][] graph) {
        int size = graph.length;
        double[] X = new double[size];
        for (int i = 0; i < size; i++) {
            X[i] = Arrays.stream(graph[i]).sum();
        }

        double energy = Arrays.stream(X).map(d -> Math.pow(d, 2)).sum();
        for (int i = 0; i < graph.length; i++) {
            for (int j = i + 1; j < graph[i].length; j++) {
                energy += 2 * Math.pow(graph[i][j], 2);
            }
        }
        return energy;
    }

    public static Point findOBLOfThePoint(Point midBestPoints, Point initPoint) {
        double[] midCoords = midBestPoints.getCoords();
        double[] initPointCoords = initPoint.getCoords();

        double[] newCoords = new double[midCoords.length];
        for (int i = 0; i < newCoords.length; i++) {
            newCoords[i] = randomGenerator.nextDouble(midCoords[i], initPointCoords[i]);
        }

        return new Point(newCoords);
    }

    public static Point findNearestPoint(Point point, String idOfCluster, Map<String, Cluster<Centroid>> clusterMap, DistanceSet distanceSet) {
        double distance = 1000000;
        Point result = new Point();
        for (Map.Entry<String, Cluster<Centroid>> entry : clusterMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(idOfCluster))
                continue;
            for (Point p : entry.getValue().getModel().getTrainingSamples().values()) {
                double d = DistanceUtils.findDistance(point, p, distanceSet);
                if (d < distance) {
                    distance = d;
                    result = new Point(p.getCoords());
                }
            }
        }
        return result;
    }

    public static Point findNearestCentroid(Point point, ClusterSolution solution, Map<Integer, Point> dataset, DistanceSet distanceSet) {
        Map<String, Cluster<Centroid>> allClusters = ClusteringUtil.convertSolutionToClusters(solution, dataset, distanceSet)
                .getAllClusters().stream().collect(Collectors.toMap(Cluster<Centroid>::getName, cluster -> cluster));

        String index = "-1";
        double distance = Double.POSITIVE_INFINITY;
        for (Map.Entry<String, Cluster<Centroid>> cluster : allClusters.entrySet()) {
            if(cluster.getValue().getModel().getFeatures().equals(point)){
                continue;
            }
            double d = DistanceUtils.findDistance(cluster.getValue().getModel().getFeatures(), point, distanceSet);
            if(d < distance){
                distance = d;
                index = cluster.getKey();
            }
        }
        Collection<Point> points = allClusters.get(index).getModel().getTrainingSamples().values();
        distance = Double.POSITIVE_INFINITY;
        Point result = null;
        for (Point p : points) {
            double d = DistanceUtils.findDistance(p, point, distanceSet);
            if(d < distance){
                distance = d;
                result = new Point(p.getCoords());
            }
        }
        return result;
    }
}

class ArrayIndexComparator implements Comparator<Integer>
{
    private final List<Double> array;

    public ArrayIndexComparator(List<Double> array)
    {
        this.array = new ArrayList<>(array);
    }

    public Integer[] createIndexArray()
    {
        Integer[] indexes = new Integer[array.size()];
        for (int i = 0; i < array.size(); i++)
        {
            indexes[i] = i; // Autoboxing
        }
        return indexes;
    }

    @Override
    public int compare(Integer index1, Integer index2)
    {
        // Autounbox from Integer to int to use as array indexes
        return array.get(index2).compareTo(array.get(index1));
    }
}

/*int index = -1;

        double minModularity = Double.POSITIVE_INFINITY;
        Point defCentroid = null;
        for (int i = 0; i < solution.getNumberOfClusters(); i++) {
            double[] modularity = modularities[i];
            double modularityWithoutI = Arrays.stream(modularity).sum() - modularity[i];

            double modularityAtI = (modularity[i] / sumOfModularities) -
                    Math.pow(modularityWithoutI / sumOfModularities, 2);

            if (modularityAtI < minModularity) {
                minModularity = modularityAtI;
                double[] values = new double[solution.getNumberOfFeatures()];
                for (int j = 0; j < solution.getNumberOfFeatures(); j++) {
                    values[j] = solution.getVariableValue(solution.getNumberOfFeatures() * i + j);
                }
                Point centroid = new Point();
                centroid.setCoords(values);

                if(centroid.equals(point)) {
                    continue;
                }
                defCentroid = new Point(centroid.getCoords());
                index = i;
            }
        }
        List<Cluster<Centroid>> allClusters = ClusteringUtil.convertSolutionToClusters(solution, dataset).getAllClusters();

        for (Cluster<Centroid> cluster : allClusters) {
            if(cluster.getName().equalsIgnoreCase(String.valueOf(index))){
                Point result = null;
                Collection<Point> values = cluster.getModel().getTrainingSamples().values();
                double distance = Double.POSITIVE_INFINITY;
                for (Point value : values) {
                    double d = value.findDistance(point, DistanceSet.EUCLIDEAN);
                    if( d < distance){
                        distance = d;
                        result = new Point(value.getCoords());
                    }
                }
                return result;
            }
        }
        return defCentroid;*/