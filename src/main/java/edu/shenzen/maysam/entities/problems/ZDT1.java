package edu.shenzen.maysam.entities.problems;

import java.util.ArrayList;
import java.util.List;
import org.uma.jmetal.problem.impl.AbstractDoubleProblem;
import org.uma.jmetal.solution.DoubleSolution;

/** Class representing problem ZDT1 */
@SuppressWarnings("serial")
public class ZDT1 extends AbstractDoubleProblem {

    /** Constructor. Creates default instance of problem ZDT1 (30 decision variables) */
    public ZDT1() {
        this(30);
    }

    /**
     * Creates a new instance of problem ZDT1.
     *
     * @param numberOfVariables Number of variables.
     */
    public ZDT1(Integer numberOfVariables) {
        setNumberOfVariables(numberOfVariables);
        setNumberOfObjectives(2);
        setName("ZDT1");

        List<Double> lowerLimit = new ArrayList<>(getNumberOfVariables()) ;
        List<Double> upperLimit = new ArrayList<>(getNumberOfVariables()) ;

        for (int i = 0; i < getNumberOfVariables(); i++) {
            lowerLimit.add(0.0);
            upperLimit.add(1.0);
        }

        setLowerLimit(lowerLimit);
        setUpperLimit(upperLimit);
    }

    /** Evaluate() method */
    public void evaluate(DoubleSolution solution) {
        double[] f = new double[getNumberOfObjectives()];

        f[0] = solution.getVariableValue(0);
        double g = this.evalG(solution);
        double h = this.evalH(f[0], g);
        f[1] = h * g;

        solution.setObjective(0, f[0]);
        solution.setObjective(1, f[1]);
    }

    /**
     * Returns the value of the ZDT1 function G.
     *
     * @param solution Solution
     */
    protected double evalG(DoubleSolution solution) {
        double g = 0.0;
        for (int i = 1; i < solution.getNumberOfVariables(); i++) {
            g += solution.getVariableValue(i);
        }
        double constant = 9.0 / (solution.getNumberOfVariables() - 1);

        return constant * g + 1.0;
    }

    /**
     * Returns the value of the ZDT1 function H.
     *
     * @param f First argument of the function H.
     * @param g Second argument of the function H.
     */
    protected double evalH(double f, double g) {
        double h ;
        h = 1.0 - Math.sqrt(f / g);
        return h;
    }
}