package edu.shenzen.maysam.initial;

import edu.shenzen.maysam.entities.math.Matrix;
import edu.shenzen.maysam.entities.math.Point;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DataInitialization {

    public static List<Point> createDataset(String filename) throws Exception{
        String extension = FilenameUtils.getExtension(filename);
        if(extension.equalsIgnoreCase("jpg")){
            return createImageDataset(filename);
        }
        else if(extension.equalsIgnoreCase("data")){
            return createData(filename);
        }
        return new ArrayList<>();

    }

    public static List<Point> createImageDataset(String filename) throws Exception{

        String filename_gt = filename.replace(".jpg", ".csv");
        int[][] labels = readGroundTruthOfImage(filename_gt);

        BufferedImage bi = ImageIO.read(new File(filename));
        List<Point> points = new ArrayList<>();
        for (int x = 0; x < bi.getHeight(); x++) {
            for (int y = 0; y < bi.getWidth(); y++) {
                double[] coords = new double[3];
                int[] pixel = bi.getRaster().getPixel(y, x, new int[3]);
                coords[0] = pixel[0];
                coords[1] = pixel[1];
                coords[2] = pixel[2];

                Point p = new Point(coords);
                p.setLabel(labels[x][y]);
                points.add(p);
            }
        }
        return points;
    }

    private static int[][] readGroundTruthOfImage(String filename_gt) throws Exception {
        List<String> lines = Files.lines(Paths.get(filename_gt)).collect(Collectors.toList());

        int[][] gt = new int[lines.size()][lines.get(0).split(",").length];
        for (int i = 0 ; i < lines.size() ; i++) {
            String[] tokens = lines.get(i).split(",");
            for(int j = 0 ; j < tokens.length ; j++){
                gt[i][j] = Integer.parseInt(tokens[j]);
            }
        }
        return gt;
    }

    public static List<Point> createData(String filename) throws Exception{
        List<String> lines = Files.lines(Paths.get(filename)).collect(Collectors.toList());
        List<Point> result = new ArrayList<>();
        for (String line : lines) {
            String[] tokens = line.split(" ");
            double[] features = new double[tokens.length - 1];
            for(int i = 0 ; i < tokens.length - 1 ; i++){
                features[i] = Double.parseDouble(tokens[i]);
            }

            Point p = new Point(features);
            p.setLabel(Integer.parseInt(tokens[tokens.length-1]));
            result.add(p);
        }
        return result;

    }
}
