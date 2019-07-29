package edu.shenzen.maysam.entities.math;

public class Matrix {

    private double[][] matrix;

    public double[][] getMatrix() {
        return matrix;
    }

    public Matrix(double[][] matrix){
        this.matrix = matrix;
    }

    public void printPretty() {
        for(int i = 0 ; i < matrix.length ; i++){
            for(int j = 0 ; j < matrix[i].length ; j++){
                System.out.print(matrix[i][j]);
                if(j == matrix[i].length - 1){
                    System.out.print("\n");
                }
                else{
                    System.out.print("\t");
                }
            }
        }
    }
}
