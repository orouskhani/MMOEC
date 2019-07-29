package edu.shenzen.maysam.util.ml;

import edu.shenzen.maysam.entities.enums.DistanceSet;
import edu.shenzen.maysam.entities.math.Point;
import edu.shenzen.maysam.entities.ml.Centroid;
import edu.shenzen.maysam.util.DistanceUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SilhouetteCoefficient {

    public double silhouetteCoefficient(Map<Integer, Centroid> clusterPointsByID, DistanceSet distanceSet) {
        double totalSilhouetteCoefficient = 0.0;
        long sampleCount = 0L;

        for (Map.Entry<Integer, Centroid> entry : clusterPointsByID.entrySet()) {
            //List<double[]> clusteredPoints = centroidToList(entry.getValue());
            long clusterSize = entry.getValue().getTrainingSamples().size();
            // Increment the total sample count for computing silhouette coefficient
            sampleCount += clusterSize;
            // if there's only one element in a cluster, then assume the silhouetteCoefficient for
            // the cluster = 0, this is an arbitrary choice per Section 2: Construction of Silhouettes
            // in the referenced paper
            if (clusterSize > 1) {
                Map<Integer, Point> points = entry.getValue().getTrainingSamples();
                List<Point> clusteredPoints = new ArrayList<>(points.values());
                for (Point point : clusteredPoints) {
                    double pointIntraClusterDissimilarity = clusterDissimilarityForPoint(point, clusteredPoints, true, distanceSet);
                    double pointInterClusterDissimilarity =
                            minInterClusterDissimilarityForPoint(entry.getKey(), point, clusterPointsByID, distanceSet);
                    totalSilhouetteCoefficient += silhouetteCoefficientScore(
                            pointIntraClusterDissimilarity, pointInterClusterDissimilarity);
                }
            }

        }
        return sampleCount == 0 ? 0.0 : totalSilhouetteCoefficient / sampleCount;

    }

    private double minInterClusterDissimilarityForPoint(
            int otherClusterID,
            Point point,
            Map<Integer, Centroid> clusteredPointsMap,
            DistanceSet distanceSet) {
        return clusteredPointsMap.entrySet().stream().mapToDouble(entry -> {
            // only compute dissimilarities with other clusters
            if (entry.getKey().equals(otherClusterID)) {
                return Double.POSITIVE_INFINITY;
            }
            return clusterDissimilarityForPoint(point, new ArrayList<>(entry.getValue().getTrainingSamples().values()), false, distanceSet);
        }).min().orElse(Double.POSITIVE_INFINITY);
    }

    private double clusterDissimilarityForPoint(Point point,
                                                List<Point> clusterPoints,
                                                boolean ownCluster, DistanceSet distanceSet) {
        double totalDissimilarity = 0.0;
        for (Point clusterPoint : clusterPoints) {
            totalDissimilarity += DistanceUtils.findDistance(clusterPoint, point, distanceSet);
        }

        if (ownCluster) {
            // (points.size -1) because a point's dissimilarity is being measured with other
            // points in its own cluster, hence there would be (n - 1) dissimilarities computed
            return totalDissimilarity / (clusterPoints.size() - 1);
        } else {
            // point dissimilarity is being measured with all points of one of other clusters
            return totalDissimilarity / clusterPoints.size();
        }
    }

    private double silhouetteCoefficientScore(double ai, double bi) {
        if (ai < bi) {
            return 1.0 - (ai / bi);
        }
        if (ai > bi) {
            return (bi / ai) - 1.0;
        }
        return 0.0;
    }

}
