package edu.shenzen.maysam.runner;

import edu.shenzen.maysam.util.ml.ClusteringUtil;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public class SegCluster {

    public static void main(String[] args) throws Exception{
        String filename1 = "C:\\Users\\Yasin\\Desktop\\New folder (2)\\105053_1.seg";
        Map<Integer, Integer> cluster1 = readGroundTruth(filename1);

        String filename2 = "C:\\Users\\Yasin\\Desktop\\New folder (2)\\105053_2.seg";
        Map<Integer, Integer> cluster2 = readGroundTruth(filename2);

        Set<Integer> keys = new HashSet<>();
        keys.addAll(cluster1.keySet());

        keys.retainAll(cluster2.keySet());

        int[] y1 = new int[keys.size()];
        int[] y2 = new int[keys.size()];

        int index = 0;
        for (Integer key : keys) {
            y1[index] = cluster1.get(key);
            y2[index] = cluster2.get(key);
            index++;
        }

        System.out.println(ClusteringUtil.randIndex(y1, y2));

    }

    private static Map<Integer, Integer> readGroundTruth(String filename) throws Exception{
        List<String> lines = Files.lines(Paths.get(filename)).collect(Collectors.toList());

        int width = Integer.parseInt(lines.get(4).split(" ")[1]);
        int height = Integer.parseInt(lines.get(5).split(" ")[1]);
        int[][] picture = new int[height][width];
        for(int i = 0 ; i < picture.length ; i++)
            for(int j = 0 ; j < picture[i].length ; j++)
                picture[i][j] = -1;

        Map<Integer,Integer> result = new HashMap<>();

        for(int i = 11 ; i < lines.size() ; i++){
            int x = Integer.parseInt(lines.get(i).split(" ")[1]);
            int y1 = Integer.parseInt(lines.get(i).split(" ")[2]);
            int y2 = Integer.parseInt(lines.get(i).split(" ")[3]);

            picture[x][y1] = Integer.parseInt(lines.get(i).split(" ")[0]);
            picture[x][y2] = Integer.parseInt(lines.get(i).split(" ")[0]);
        }
        int index = 0;
        for(int i = 0 ; i < picture.length ; i++) {
            for (int j = 0; j < picture[i].length; j++) {
                if(picture[i][j] != -1){
                    result.put(index, picture[i][j]);
                }
                index++;
            }
        }
        return result;
    }
}
