package edu.shenzen.maysam.entities.math;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import edu.shenzen.maysam.entities.enums.DistanceSet;

import java.util.Arrays;
import java.util.Objects;

public class Point extends Vector{ //implements FeatureVector<Double> {

    private double[] coords;
    private int label;

    public Point(double[] coords){
        super(coords);
        this.coords = coords;
    }

    public Point() {
    }

    public double[] getCoords() {
        return coords;
    }

    public void setCoords(double[] coords) {
        this.coords = coords;
    }

    /*public double findDistance(Point y, DistanceSet distanceDif) {
        if(distanceDif == DistanceSet.EUCLIDEAN){
            double sum = 0;
            double[] coords = y.getCoords();
            for(int i = 0 ; i < coords.length ; i++){
                sum += Math.pow(this.coords[i] - coords[i], 2);
            }
            //return Math.sqrt(sum);
            double temp = Math.exp(-1 * Math.sqrt(sum) / 2);
            return 2 * (1 - temp);
        }
        else{
            return 0;
        }
    }*/


    public void setLabel(int label) {
        this.label = label;
    }

    public int getLabel() {
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return label == point.label &&
                Arrays.equals(coords, point.coords);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), label);
        result = 31 * result + Arrays.hashCode(coords);
        return result;
    }

    /*  @Override
        public int getDimensionality() {
            return coords.length;
        }

        @Override
        public Double getValue(int dimension) {
            return coords[dimension];
        }
    */
    public String toFormattedString() {
        String line = "[";
        for(int i = 0 ; i < coords.length ; i++){
            line = line.concat(String.valueOf(coords[i]));
            if(i != coords.length - 1){
                line += ",";
            }
        }
        line = line.concat("]");
        return line;
    }
}
