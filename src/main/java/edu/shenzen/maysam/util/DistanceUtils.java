package edu.shenzen.maysam.util;

import edu.shenzen.maysam.entities.enums.DistanceSet;
import edu.shenzen.maysam.entities.math.Matrix;
import edu.shenzen.maysam.entities.math.Point;
import edu.shenzen.maysam.entities.ml.Centroid;
import org.uma.jmetal.solution.DoubleSolution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DistanceUtils {

    private static Point[] experts = null;
    private static double denominator = 0.0;
    public static void setExperts(List<Point> outerExperts){
        if(experts == null){
            experts = new Point[outerExperts.size()];
            for (int i = 0 ; i < outerExperts.size() ; i++) {
                experts[i] = new Point(outerExperts.get(i).getCoords());
            }
        }
    }

    public static void setDenominator(List<Point> dataset){
        double sum = 0;
        for (Point point : dataset) {
            sum += findPOE(point);
        }
        denominator = sum;
    }

    public static Matrix pdist2(List<Point> xList, List<Point> yList, DistanceSet distanceDif){
        double[][] resultArray = new double[xList.size()][yList.size()];

        for (int i = 0 ; i < xList.size() ; i++) {
            Point x = xList.get(i);
            for (int j = 0 ; j < yList.size() ; j++) {
                Point y = yList.get(j);
                resultArray[i][j] = findDistance(x, y, distanceDif);
            }
        }

        return new Matrix(resultArray);
    }

    public static double deviation(List<Point> dataset, List<Point> solution, DistanceSet distanceSet) {

        Matrix matrix1 = pdist2(dataset, solution, distanceSet);
        double[][] matrix2 = matrix1.getMatrix();

        double sum = 0;
        for(int i = 0 ; i < matrix2.length ; i++){
            sum+= Arrays.stream(matrix2[i])
                    .min()
                    .getAsDouble();
        }

        return sum;
    }

    public static double getDistanceOfCentroidFromReferencePoint(List<Centroid> centroids, Point referencePoint, DistanceSet distanceSet) {
        double distance = 0;
        for (Centroid centroid : centroids) {
           distance += centroid.getTrainingSamples().size() * findDistance(centroid.getFeatures(), referencePoint, distanceSet);
        }
        return distance;
    }

    public static Double findDistance(Point x, Point y, DistanceSet distanceDif) {
        if(distanceDif == DistanceSet.EUCLIDEAN){
            return findEuclideanDistance(x, y);
        }
        else if (distanceDif == DistanceSet.KERNEL){
            double temp = Math.exp(-1 * findEuclideanDistance(x, y) / 2);
            return 2 * ( 1 - temp);
        }
        else if(distanceDif == DistanceSet.POE){
            double xPOE = findPOE(x);
            double yPOE = findPOE(y);

            return Math.abs(xPOE - yPOE) / denominator;
        }
        return null;
    }

    private static double findEuclideanDistance(Point x, Point y){
        double sum = 0;
        double[] coords = y.getCoords();
        for(int j = 0 ; j < coords.length ; j++){
            sum += Math.pow(x.getCoords()[j] - coords[j], 2);
        }
        return Math.sqrt(sum);
    }
    private static double findPOE(Point p){
        double result = 1.0;
        for(int i = 0 ; i < experts.length ; i++){
            double sum = 0;
            double[] coords = p.getCoords();
            for(int j = 0 ; j < coords.length ; j++){
                sum += Math.pow(experts[i].getCoords()[j] - coords[j], 2);
            }
            return Math.sqrt(sum);
           // double temp = Math.exp(-1 * Math.sqrt(sum) / 2);
           // result *= temp;
        }
        return result;
    }

    /*double sum = 0;
            double[] coords = y.getCoords();
            for(int i = 0 ; i < coords.length ; i++){
                sum += Math.pow(x.getCoords()[i] - coords[i], 2);
            }
            //return Math.sqrt(sum);
            double temp = Math.exp(-1 * Math.sqrt(sum) / 2);*/

//    public static void main(String[] args) {
//        double[] rnd = new Random().doubles(12, 0, 1).toArray();
//        List<Point> xList = new ArrayList<>();
//        List<Point> yList = new ArrayList<>();
//
//        for(int i = 0 ; i < rnd.length ; ){
//            double[] point = new double[]{rnd[i], rnd[i+1]};
//            Point p = new Point(point);
//            i = i + 2;
//            if(i < 8) {
//                xList.add(p);
//            }
//            else{
//                yList.add(p);
//            }
//        }
//
//        DistanceUtils.pdist2(xList, yList, DistanceSet.EUCLIDEAN);
//    }

}
