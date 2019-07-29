package edu.shenzen.maysam.runner;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.evaluation.clustering.ClusterContingencyTable;
import edu.shenzen.maysam.entities.math.Point;
import edu.shenzen.maysam.entities.ml.Centroid;
import edu.shenzen.maysam.initial.DataInitialization;
import edu.shenzen.maysam.util.ImageUtils;
import edu.shenzen.maysam.util.ml.ClusteringUtil;
import edu.shenzen.maysam.util.ml.KMeans;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ImageSegmentationRunner {
//    private static final String FILE_PATH_BASE="C:\\Users\\Yasin\\Desktop\\108073\\";
//
//    private static final String FILENAME_READ = "105053.jpg";
//    private static final String FILENAME_WRITE = "105053_1.jpg";
//    private static final String FILENAME_GROUNDTRUTH = "105053.seg";
//
    public static void main(String[] args) throws Exception{

        String filename = "resources/spiral.data";

        List<Point> dataset = DataInitialization.createDataset(filename);
        Map<Integer, Point> collectedDS = IntStream.range(0, dataset.size())
                .boxed()
                .collect(Collectors.toMap(i -> i, dataset::get));


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


        KMeans kMeans = new KMeans(dataset);
        List<Centroid> centroids = kMeans.performClustering(3);

        List<Cluster<Centroid>> toplevelclusters = new ArrayList<>();
        int index = 0;
        for (Centroid entry : centroids) {
            ArrayModifiableDBIDs dbs = DBIDFactory.FACTORY.newArray();
            for (Integer id : entry.getTrainingSamples().keySet()) {
                DBID dbid = DBIDFactory.FACTORY.importInteger(id);
                dbs.add(dbid);
            }
            Cluster<Centroid> c = new Cluster<Centroid>(String.valueOf(index), dbs ,entry);
            toplevelclusters.add(c);
            index += 1;
        }
        Clustering<Centroid> clustering = new Clustering<Centroid>("Solution's Clustering" , "SCluster",
                toplevelclusters);


        ClusterContingencyTable contmat = new ClusterContingencyTable(false, false);
        contmat.process(groundTruthClustering, clustering);


        System.out.println(contmat.getPaircount().f1Measure());
        System.out.println(contmat.getPaircount().precision());
        System.out.println(contmat.getPaircount().recall());
        System.out.println(contmat.getPaircount().randIndex());
        System.out.println(contmat.getPaircount().adjustedRandIndex());

        /*int[][] modified_red_pixel = new int[bi.getWidth()][bi.getHeight()];
        int[][] modified_green_pixel = new int[bi.getWidth()][bi.getHeight()];
        int[][] modified_blue_pixel = new int[bi.getWidth()][bi.getHeight()];

        Color[] colors = new Color[]{Color.BLACK, Color.BLUE, Color.CYAN, Color.RED, Color.GREEN, Color.YELLOW,
        Color.MAGENTA, Color.ORANGE, Color.PINK, Color.WHITE};
        int index = 0;
        for (Centroid centroid : centroids) {
            Point features = centroid.getFeatures();
            Map<Integer, Point> trainingSamples = centroid.getTrainingSamples();
            for (Map.Entry<Integer, Point> e : trainingSamples.entrySet()) {
                int x = e.getKey() % bi.getWidth();
                int y = e.getKey() / bi.getWidth();
                modified_red_pixel[x][y] = colors[index].getRed();//new Double(features.getCoords()[0]).intValue();
                modified_green_pixel[x][y] = colors[index].getGreen();//new Double(features.getCoords()[1]).intValue();
                modified_blue_pixel[x][y] = colors[index].getBlue();//new Double(features.getCoords()[2]).intValue();
            }
            index++;
        }*/



//        String newFileName = "C:\\Users\\Yasin\\Desktop\\download_2.jpg";

  //      ImageUtils.writeRGB(modified_red_pixel, modified_green_pixel, modified_blue_pixel, newFileName);

    }
//
//    private static Map<Integer, Integer> readGroundTruth(String filename) throws Exception{
//        List<String> lines = Files.lines(Paths.get(filename)).collect(Collectors.toList());
//
//        int width = Integer.parseInt(lines.get(4).split(" ")[1]);
//        int height = Integer.parseInt(lines.get(5).split(" ")[1]);
//        int[][] picture = new int[height][width];
//        for(int i = 0 ; i < picture.length ; i++)
//            for(int j = 0 ; j < picture[i].length ; j++)
//                picture[i][j] = -1;
//
//        Map<Integer,Integer> result = new HashMap<>();
//
//        for(int i = 11 ; i < lines.size() ; i++){
//            int x = Integer.parseInt(lines.get(i).split(" ")[1]);
//            int y1 = Integer.parseInt(lines.get(i).split(" ")[2]);
//            int y2 = Integer.parseInt(lines.get(i).split(" ")[3]);
//
//            picture[x][y1] = Integer.parseInt(lines.get(i).split(" ")[0]);
//            picture[x][y2] = Integer.parseInt(lines.get(i).split(" ")[0]);
//        }
//        int index = 0;
//        for(int i = 0 ; i < picture.length ; i++) {
//            for (int j = 0; j < picture[i].length; j++) {
//                if(picture[i][j] != -1){
//                    result.put(index, picture[i][j]);
//                }
//                index++;
//            }
//        }
//        return result;
//    }

}
