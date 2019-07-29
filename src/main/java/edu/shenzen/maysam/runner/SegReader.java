package edu.shenzen.maysam.runner;

import edu.shenzen.maysam.util.ImageUtils;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SegReader {
    public static void main(String[] args) throws Exception {
        String fileName = "C:\\Users\\Yasin\\Desktop\\BSDS300-human\\BSDS300\\human\\color\\1130\\108073.seg";

        List<String> lines = Files.lines(Paths.get(fileName)).collect(Collectors.toList());

        int height = Integer.parseInt(lines.get(5).split(" ")[1]);
        int width = Integer.parseInt(lines.get(4).split(" ")[1]);

        int[][] picture = new int[width][height];
        for(int i = 11 ; i < lines.size() ; i++){
            int x = Integer.parseInt(lines.get(i).split(" ")[1]);
            int y1 = Integer.parseInt(lines.get(i).split(" ")[2]);
            int y2 = Integer.parseInt(lines.get(i).split(" ")[3]);

            picture[y1][x] = Integer.parseInt(lines.get(i).split(" ")[0]);
            picture[y2][x] = Integer.parseInt(lines.get(i).split(" ")[0]);
        }

        int[][] modified_red_pixel = new int[width][height];
        int[][] modified_green_pixel = new int[width][height];
        int[][] modified_blue_pixel = new int[width][height];

        Color[] colors = new Color[]{Color.BLACK, Color.BLUE, Color.CYAN, Color.RED, Color.GREEN, Color.YELLOW,
                Color.MAGENTA, Color.ORANGE, Color.PINK, Color.WHITE, Color.GRAY, Color.LIGHT_GRAY, Color.DARK_GRAY};

        for (int i = 0 ; i < width ; i++) {
            for (int j = 0 ; j < height ; j++) {
                int cluster = picture[i][j];
                modified_red_pixel[i][j] = colors[cluster].getRed();//new Double(features.getCoords()[0]).intValue();
                modified_green_pixel[i][j] = colors[cluster].getGreen();//new Double(features.getCoords()[1]).intValue();
                modified_blue_pixel[i][j] = colors[cluster].getBlue();//new Double(features.getCoords()[2]).intValue();
            }
        }
        String newFileName = "C:\\Users\\Yasin\\Desktop\\33039_1.jpg";

        ImageUtils.writeRGB(modified_red_pixel, modified_green_pixel, modified_blue_pixel, newFileName);


    }
}
