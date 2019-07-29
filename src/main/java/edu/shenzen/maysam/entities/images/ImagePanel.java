package edu.shenzen.maysam.entities.images;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class ImagePanel extends JFrame {

    public ImagePanel(String filename) throws Exception {
        super("Blahblahblah");

        JFrame frame = new JFrame();

        File imageFile = new File(filename);
        Image i = ImageIO.read(imageFile);
        ImageIcon image = new ImageIcon(i);
        JLabel imageLabel = new JLabel(image);
        frame.add(imageLabel);
        frame.setLayout(null);
        imageLabel.setLocation(0, 0);

        BufferedImage img = ImageIO.read(new File(filename));
        int height = img.getHeight();
        int width = img.getWidth();


        imageLabel.setSize(width, height);
        imageLabel.setVisible(true);
        frame.setVisible(true);
        frame.setSize(width, height);
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);

    }
}
