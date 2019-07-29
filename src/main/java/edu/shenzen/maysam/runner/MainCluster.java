package edu.shenzen.maysam.runner;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.evaluation.clustering.ClusterContingencyTable;
import de.lmu.ifi.dbs.elki.utilities.referencepoints.RandomGeneratedReferencePoints;
import edu.shenzen.maysam.algorithm.operators.MOCrossover;
import edu.shenzen.maysam.algorithm.operators.MOMutation;
import edu.shenzen.maysam.entities.enums.DistanceSet;
import edu.shenzen.maysam.entities.math.Point;
import edu.shenzen.maysam.entities.ml.Centroid;
import edu.shenzen.maysam.entities.problems.ClusterProblem;
import edu.shenzen.maysam.entities.solutions.ClusterSolution;
import edu.shenzen.maysam.initial.DataInitialization;
import edu.shenzen.maysam.util.DistanceUtils;
import edu.shenzen.maysam.util.ImageUtils;
import edu.shenzen.maysam.util.NetworkUtil;
import edu.shenzen.maysam.util.ml.ClusteringUtil;
import edu.shenzen.maysam.util.ml.KMeans;
import org.apache.spark.SparkConf;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAII;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.SelectionOperator;
import org.uma.jmetal.operator.impl.crossover.SBXCrossover;
import org.uma.jmetal.operator.impl.mutation.PolynomialMutation;
import org.uma.jmetal.operator.impl.selection.BinaryTournamentSelection;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.util.AbstractAlgorithmRunner;
import org.uma.jmetal.util.AlgorithmRunner;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import org.uma.jmetal.util.evaluator.impl.MultithreadedSolutionListEvaluator;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MainCluster extends AbstractAlgorithmRunner {

    protected final static JMetalRandom randomGenerator = JMetalRandom.getInstance();

    public static void main(String[] args) throws Exception {
        int repetition = 1;
        DistanceSet distanceSet = DistanceSet.EUCLIDEAN;
        String filename = "resources/unbalance.data";
        List<Point> dataset = DataInitialization.createDataset(filename);

        int numberOfFeatures = 2;
        int numberOfTrueClusters = 8;
        int numberOfClusters = 2 * numberOfTrueClusters;
        int numberOfVariables = numberOfClusters * numberOfFeatures;
        int numberOfCores = 20;
        double probabilityOfParticle = 1.1;

        double lowerBound = 139779;
        double upperBound = 575805;


        double sumFMeasure = 0;
        double sumPrecision = 0;
        double sumRecall = 0;
        double sumRandIndex = 0;
        double sumAdjusted = 0;

        List<Double> numberOfFinalClusters = new ArrayList<>();


        for(int repCounter = 0 ; repCounter < repetition ; repCounter++) {

            System.out.println("At Repetition : " + repCounter);

            Problem<DoubleSolution> problem;
            //Algorithm<List<DoubleSolution>> algorithm;
            CrossoverOperator<DoubleSolution> crossover;
            MutationOperator<DoubleSolution> mutation;
            SelectionOperator<List<DoubleSolution>, DoubleSolution> selection;

            List<Double> parameterLowerLimit = new ArrayList<>();
            List<Double> parameterUpperLimit = new ArrayList<>();

            for (int i = 0; i < numberOfVariables; i++) {
                parameterLowerLimit.add(lowerBound);
                parameterUpperLimit.add(upperBound);
            }

            Map<Integer, Point> collectedDS = IntStream.range(0, dataset.size())
                    .boxed()
                    .collect(Collectors.toMap(i -> i, dataset::get));

            int heuristicPointsSize = (int) (0.3 * dataset.size());
         //   List<Point> tempInitPoints = NetworkUtil.findInitPointAndTheirOBL(collectedDS, heuristicPointsSize, upperBound, lowerBound, distanceSet);

            List<Point> tempInitPoints = NetworkUtil.findRandomPoints(collectedDS, heuristicPointsSize);

            List<Point> initPoints = new ArrayList<>();
            for (int i = 0; i < heuristicPointsSize; i++) {
                initPoints.add(tempInitPoints.get(i));
            }

            problem = new ClusterProblem(numberOfClusters, numberOfFeatures, numberOfVariables, initPoints, collectedDS,
                    parameterLowerLimit, parameterUpperLimit, distanceSet);

            double crossoverProbability = 0.7;
            double crossoverDistributionIndex = 20.0;
            crossover = new MOCrossover(crossoverProbability, crossoverDistributionIndex, collectedDS, distanceSet) ;
            //crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

            double mutationProbability = 1.0 / problem.getNumberOfVariables();
            double mutationDistributionIndex = 20.0;

            mutation = new MOMutation(numberOfFeatures, probabilityOfParticle , mutationProbability, upperBound, lowerBound, collectedDS, distanceSet);
          //  mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);


            selection = new BinaryTournamentSelection<DoubleSolution>(new RankingAndCrowdingDistanceComparator<DoubleSolution>());

            //algorithm = new SmartNSGAII<>(problem, 1000, 20, crossover, mutation,
            //      selection, new SequentialSolutionListEvaluator<DoubleSolution>()) ;


            NSGAII<DoubleSolution> algorithm = new NSGAIIBuilder<DoubleSolution>(problem, crossover, mutation)
                    .setPopulationSize(20)
                    .setMaxEvaluations(500)
                    .setSelectionOperator(selection)
                    .setSolutionListEvaluator(
                            new MultithreadedSolutionListEvaluator<DoubleSolution>(
                                    numberOfCores, problem)).build();


            AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm)
                    .execute();

            long computingTime = algorithmRunner.getComputingTime();

            JMetalLogger.logger.info("Total execution time: " + computingTime + "ms");

            List<DoubleSolution> tempPopulation = algorithm.getResult();
            printFinalSolutionSet(tempPopulation);

            List<ClusterSolution> population = new ArrayList<>();
            for (DoubleSolution doubleSolution : tempPopulation) {
                population.add(new ClusterSolution((ClusterSolution) doubleSolution));
            }

            List<ClusterSolution> nomalizedPopulation = normalizePopulation(population);

            Map<Integer, Cluster<Centroid>> toplevelclustersmap = new HashMap<>();
            for (Map.Entry<Integer, Point> entry : collectedDS.entrySet()) {
                if (toplevelclustersmap.containsKey(entry.getValue().getLabel() - 1)) {
                    Cluster<Centroid> centroidCluster = toplevelclustersmap.get(entry.getValue().getLabel() - 1);

                    DBIDs iDs = centroidCluster.getIDs();
                    ArrayModifiableDBIDs newIds = DBIDFactory.FACTORY.newArray(iDs);
                    newIds.add(DBIDFactory.FACTORY.importInteger(entry.getKey()));
                    centroidCluster.setIDs(newIds);

                    Centroid model = centroidCluster.getModel();
                    model.addTrainingSample(entry.getKey(), entry.getValue());
                    centroidCluster.setModel(model);

                    toplevelclustersmap.put(entry.getValue().getLabel() - 1, centroidCluster);

                } else {
                    ArrayModifiableDBIDs dbs = DBIDFactory.FACTORY.newArray();

                    DBID dbid = DBIDFactory.FACTORY.importInteger(entry.getKey());
                    dbs.add(dbid);

                    Centroid centroid = new Centroid();
                    centroid.addTrainingSample(entry.getKey(), entry.getValue());

                    Cluster<Centroid> c = new Cluster<Centroid>(String.valueOf(entry.getValue().getLabel()), dbs, centroid);
                    toplevelclustersmap.put(entry.getValue().getLabel() - 1, c);
                }

            }

            Clustering<Centroid> groundTruthClustering = new Clustering<Centroid>("Best Clustering", "SCluster",
                    new ArrayList<>(toplevelclustersmap.values()));


            //  System.out.println("For All Solutions");
            int populationindex = 0;
            ClusterSolution finalSolution = null;
            double maxRandIndex = -1.0;
            for (ClusterSolution kMeansSolution : nomalizedPopulation) {
            //    System.out.println("For Solution at index : " + populationindex);
                populationindex++;
                Clustering<Centroid> clustering = ClusteringUtil.convertSolutionToClusters(kMeansSolution, collectedDS, distanceSet);


                ClusterContingencyTable contingencyTable = new ClusterContingencyTable(true, true);
                contingencyTable.process(groundTruthClustering, clustering);
                double rndIndex = contingencyTable.getPaircount().adjustedRandIndex();
          //      System.out.println("Rand Index : " + rndIndex);
                if (rndIndex > maxRandIndex) {
                    finalSolution = new ClusterSolution(kMeansSolution);
                    maxRandIndex = rndIndex;
                }
                System.out.println(Math.round(kMeansSolution.getObjective(0) * 100) / 100.0 + "," + Math.round(kMeansSolution.getObjective(1) * 100)/100.0);
            //    System.out.println("DB : " + kMeansSolution.getDb());

            }

            ClusterSolution solution = new ClusterSolution(finalSolution);//findBestSolution(nomalizedPopulation, new SimpleSolutionComparator());
            Clustering<Centroid> clustering = ClusteringUtil.convertSolutionToClusters(solution, collectedDS, distanceSet);
            numberOfFinalClusters.add((double)(clustering.getAllClusters().size()));

            ClusterContingencyTable contmat = new ClusterContingencyTable(true, true);
            contmat.process(groundTruthClustering, clustering);


            sumFMeasure += contmat.getPaircount().f1Measure();
            sumPrecision += contmat.getPaircount().precision();
            sumRecall += contmat.getPaircount().recall();
            sumRandIndex += contmat.getPaircount().randIndex();
            sumAdjusted += contmat.getPaircount().adjustedRandIndex();

            String writtenFilename = "resources/D31.csv";
            writeResult(solution, filename, writtenFilename, clustering);
        }
        System.out.println("F Measure : " + sumFMeasure / repetition );
        System.out.println("Precision : " + sumPrecision / repetition);
        System.out.println("Recall : " + sumRecall / repetition);
        System.out.println("rand index : " + sumRandIndex / repetition);
        System.out.println("Adjusted Rand Index : " + sumAdjusted / repetition);

        double avg = numberOfFinalClusters.stream().mapToDouble(a -> a).average().orElse(0.0);
        System.out.println("Average of Clusters : " + avg);

        double minus = numberOfFinalClusters.stream().mapToDouble(a -> Math.pow(a - avg , 2)).sum();
        System.out.println("Variance of Clusters : " + minus / repetition);

        for (Double numberOfFinalCluster : numberOfFinalClusters) {
            System.out.println(numberOfFinalCluster);
        }

    }

    private static void savePointsToFile(List<Point> initPoints, String savedFilename) throws Exception {
        PrintWriter pw = new PrintWriter(new FileOutputStream(savedFilename));
        for (Point point : initPoints){
            double[] coords = point.getCoords();
            for(int i = 0 ;i < coords.length ;i++){
                pw.print(coords[i]);
                if(i != coords.length -1)
                    pw.print(",");
                else
                    pw.println();

            }
        }

        pw.close();
    }

    private static void writeResult(ClusterSolution solution, String filename, String writtenFilename, Clustering<Centroid> fCentroids) throws Exception {
        if(writtenFilename.endsWith("jpg")){
            writeImage(solution, filename, writtenFilename, fCentroids.getAllClusters());
        }
        else if(writtenFilename.endsWith("csv")){
            writeData(solution, filename, writtenFilename, fCentroids.getAllClusters());
        }
    }

    private static void writeData(ClusterSolution solution, String filename, String writtenFilename, List<Cluster<Centroid>> fCentroids) throws Exception {
        FileWriter writer = new FileWriter(writtenFilename);
        for(Cluster<Centroid> cluster : fCentroids) {
            for (Point point : cluster.getModel().getTrainingSamples().values()) {
                String line = "";
                for (double v : point.getCoords()) {
                    line += v + " ";
                }
                line += cluster.getName();
                writer.write(line + "\n");
            }

        }
        writer.close();
    }


    private static void writeImage(ClusterSolution solution, String filename, String writtenFilename, List<Cluster<Centroid>> fCentroids) throws Exception {
        BufferedImage bi = ImageIO.read(new File(filename));

        List<ClusterSolution> bestSol = new ArrayList<>();
        bestSol.add(solution);
        new SolutionListOutput(bestSol)
                .setSeparator("\t")
                .setVarFileOutputContext(new DefaultFileOutputContext("bestVAR.tsv"))
                .setFunFileOutputContext(new DefaultFileOutputContext("bestFUN.tsv"))
                .print();


        int[][] modified_red_pixel = new int[bi.getWidth()][bi.getHeight()];
        int[][] modified_green_pixel = new int[bi.getWidth()][bi.getHeight()];
        int[][] modified_blue_pixel = new int[bi.getWidth()][bi.getHeight()];

        Color[] colors = new Color[fCentroids.size()];
        Random rand = new Random();

        for(int i = 0 ; i < colors.length ; i++){
            colors[i] = new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
        }

        int index = 0;
        for (Cluster<Centroid> centroid : fCentroids) {
            Map<Integer, Point> trainingSamples = centroid.getModel().getTrainingSamples();
            for (Map.Entry<Integer, Point> e : trainingSamples.entrySet()) {
                int x = e.getKey() % bi.getWidth();
                int y = e.getKey() / bi.getWidth();
                modified_red_pixel[x][y] = colors[index].getRed();//new Double(features.getCoords()[0]).intValue();
                modified_green_pixel[x][y] = colors[index].getGreen();//new Double(features.getCoords()[1]).intValue();
                modified_blue_pixel[x][y] = colors[index].getBlue();//new Double(features.getCoords()[2]).intValue();
            }
            index++;
        }

        ImageUtils.writeRGB(modified_red_pixel, modified_green_pixel, modified_blue_pixel, writtenFilename);
    }

    private static List<ClusterSolution> normalizePopulation(List<ClusterSolution> population) {
        List<ClusterSolution> result = new ArrayList<>();

        double minFirstObjective = population.stream().mapToDouble(new ToDoubleFunction<DoubleSolution>() {
            @Override
            public double applyAsDouble(DoubleSolution value) {
                return value.getObjective(0);
            }
        }).min().getAsDouble();

        double maxFirstObjective = population.stream().mapToDouble(new ToDoubleFunction<DoubleSolution>() {
            @Override
            public double applyAsDouble(DoubleSolution value) {
                return value.getObjective(0);
            }
        }).max().getAsDouble();

        double minSecondObjective = population.stream().mapToDouble(new ToDoubleFunction<DoubleSolution>() {
            @Override
            public double applyAsDouble(DoubleSolution value) {
                return value.getObjective(1);
            }
        }).min().getAsDouble();

        double maxSecondObjective = population.stream().mapToDouble(new ToDoubleFunction<DoubleSolution>() {
            @Override
            public double applyAsDouble(DoubleSolution value) {
                return value.getObjective(1);
            }
        }).max().getAsDouble();

        for (ClusterSolution doubleSolution : population) {
            ClusterSolution sol = doubleSolution;

            double first = (doubleSolution.getObjective(0)-minFirstObjective) / (maxFirstObjective - minFirstObjective);
            sol.setObjective(0 , first);

            double second = (doubleSolution.getObjective(1)-minSecondObjective) / (maxSecondObjective - minSecondObjective);
            sol.setObjective(1 , second);

            result.add(sol);

        }
        return result;
    }
}


// String savedFilename = "resources/d31InitPoints_sorted_with_value.csv";
// savePointsToFile(initPoints, savedFilename);

           /* try (BufferedReader br = new BufferedReader(new FileReader(savedFilename))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] tokens = line.split(",");
                    Point p = new Point(new double[]{Double.parseDouble(tokens[0]),
                            Double.parseDouble(tokens[1])});

                    tempInitPoints.add(p);
                }
            }*/


// List<Point> normalOBLs = new ArrayList<>();
        /*for(int i = tempInitPoints.size() - 600 ; i < tempInitPoints.size() ; i++){
            normalOBLs.add(NetworkUtil.findOBLOfThePoint(tempInitPoints.get(i), lowerBound, upperBound));
        }*/
//  initPoints.addAll(normalOBLs);
        /*double[] coords = new double[initPoints.get(0).getCoords().length];
        for (Point initPoint : initPoints) {
            for (int i = 0 ; i < initPoint.getCoords().length ;i++) {
                coords[i] += initPoint.getCoords()[i];
            }
        }
        for (int i = 0; i < coords.length; i++) {
            coords[i] /= initPoints.size();
        }

        Point midBestPoints = new Point(coords);
        List<Point> finalPoints = new ArrayList<>();
        finalPoints.addAll(initPoints);
        for (Point normalOBL : normalOBLs) {
            finalPoints.add(NetworkUtil.findOBLOfThePoint(midBestPoints, normalOBL));
        }*/

        /*for (ClusterSolution clusterSolution : nomalizedPopulation) {
            clusterSolution.calDBMeasure(collectedDS);
        }*/
/* for (Cluster<Centroid> entry : clustering.getAllClusters()) {
                   System.out.println("Cluster Index : " + entry.getName());
                    System.out.println("Cluster Center : " + entry.getModel().getFeatures());
                    System.out.println("Cluster Size : " + entry.getIDs().size());
                }*/

/*System.out.println("F Measure : " + contmat.getPaircount().f1Measure());
            System.out.println("Precision : " + contmat.getPaircount().precision());
            System.out.println("Recall : " + contmat.getPaircount().recall());
            System.out.println("rand index : " + contmat.getPaircount().randIndex());
            System.out.println("Adjusted Rand Index : " + contmat.getPaircount().adjustedRandIndex());
            System.out.println("Fowlkes Mallows : " + contmat.getPaircount().fowlkesMallows());
            System.out.println("Jaccard : " + contmat.getPaircount().jaccard());
            System.out.println("mirkin : " + contmat.getPaircount().mirkin());
            System.out.println("Entropy-VI : " + contmat.getEntropy().variationOfInformation());
            System.out.println("Entropy-NormalizedVI : " + contmat.getEntropy().normalizedVariationOfInformation());
            System.out.println("Edit-F1 : " + contmat.getEdit().f1Measure());
            System.out.println("inverse purity : " + contmat.getSetMatching().inversePurity());
            System.out.println("purity : " + contmat.getSetMatching().purity());
            System.out.println("SM-F1 : " + contmat.getSetMatching().f1Measure());
            System.out.println("BCubed-Precision : " + contmat.getBCubed().precision());
            System.out.println("BCubed-Recall : " + contmat.getBCubed().recall());
            System.out.println("BCubed-F1 : " + contmat.getBCubed().f1Measure());
*/

//     System.out.println("For Final Solution With Size : " + clustering.getAllClusters().size());
//   for (Cluster<Centroid> entry : clustering.getAllClusters()) {
//     System.out.println("Cluster Index : " + entry.getName());
//   System.out.println("Cluster Center : " + entry.getModel().getFeatures().toFormattedString());
// System.out.println("Cluster Size : " + entry.getIDs().size());
//}
