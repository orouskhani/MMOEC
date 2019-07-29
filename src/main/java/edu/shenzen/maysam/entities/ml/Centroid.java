package edu.shenzen.maysam.entities.ml;

import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import edu.shenzen.maysam.entities.math.Point;

import java.util.HashMap;
import java.util.Map;

public class Centroid extends MeanModel {
    Point features;
    Map<Integer, Point> trainingSamples;
    private double modularity;

    public double getModularity() {
        return modularity;
    }

    public void setModularity(double modularity) {
        this.modularity = modularity;
    }

    public Point getFeatures() {
        return features;
    }

    public void setFeatures(Point features) {
        this.features = features;
    }

    public Map<Integer, Point> getTrainingSamples()
    {
        return trainingSamples;
    }

    public void setTrainingSamples(Map<Integer, Point> trainingSamples)
    {
        this.trainingSamples = new HashMap<>(trainingSamples);
    }

    public void addTrainingSample(Integer index, Point trainingSample)
    {
        if (this.trainingSamples == null)
        {
            this.trainingSamples = new HashMap<>();
        }

        this.trainingSamples.put(index, trainingSample);
    }

    public Centroid(Point features, Map<Integer, Point> trainingSamples)
    {
        super(features);
        this.features = features;
        this.trainingSamples = new HashMap<>(trainingSamples);
    }

    public Centroid()
    {
        super(new Vector());
        this.trainingSamples = new HashMap<>();
    }
}
