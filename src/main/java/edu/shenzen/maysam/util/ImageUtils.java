package edu.shenzen.maysam.util;

import edu.shenzen.maysam.entities.images.ImagePanel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class ImageUtils {
    public static void showImage(String filename) {
        try {
            ImagePanel frame = new ImagePanel(filename);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void writeRGB(int[][] red, int[][] green, int[][] blue, String filename) throws Exception {
        BufferedImage image = new BufferedImage(red.length/*Width*/, red[0].length/*height*/, BufferedImage.TYPE_INT_ARGB);

        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = 0; i < image.getWidth(); i++) {
                try {
                    Color c = new Color(red[i][j], green[i][j], blue[i][j]);
                    image.setRGB(i, j, c.getRGB());
                }catch(Exception ex){
                    System.out.println(i + " " + j);
                }
            }
        }
        ImageIO.write(image, "jpg", new File(filename));
    }
}
