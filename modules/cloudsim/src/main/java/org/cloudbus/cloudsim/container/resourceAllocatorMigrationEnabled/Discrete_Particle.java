package org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled;

import java.util.*;

/**
 * 
 * @author carlosandres.mendez
 */
public class Discrete_Particle {

    /** Best fitness function so far */
    double bestFitness;
    /** Best particles's position so far */
    List<Allocation> bestPosition;
    /** current fitness */
    double fitness;
    /** Position */
    List<Allocation> position;
    /** Velocity */
    List<Allocation> velocity;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Constructor 
     */
    public Discrete_Particle() {
        position = new ArrayList<>();
        bestPosition = new ArrayList<>();
        velocity = new ArrayList<>();
        bestFitness = Double.NaN;
        fitness = Double.NaN;
    }

    

    //-------------------------------------------------------------------------
    // Methods
    //-------------------------------------------------------------------------

    public Discrete_Particle(List<Allocation> position, List<Allocation> velocity) {
        this.position = position;
        this.velocity = velocity;
    }



    /** Copy position[] to positionCopy[] */
    public void copyPosition(List<Allocation> positionCopy) {
        positionCopy.clear();
        for(Allocation alloc : position)
            positionCopy.add(new Allocation(alloc.getContainer(), alloc.getVm(), alloc.getHost()));
    }

    /** Copy position[] to bestPosition[] */
    public void copyPosition2Best() {
        bestPosition = new ArrayList<>();
        for(Allocation alloc : position)
            bestPosition.add(new Allocation(alloc.getContainer(), alloc.getVm(), alloc.getHost()));
    }

    public double getBestFitness() {
        return bestFitness;
    }

    public List<Allocation> getBestPosition() {
        return bestPosition;
    }

    public int getDimension() {
        return position.size();
    }

    public double getFitness() {
        return fitness;
    }

    public List<Allocation> getPosition() {
        return position;
    }

    public List<Allocation> getVelocity() {
        return velocity;
    }

    /**
     * Initialize a particles's position and velocity vectors 
     * @param maxPosition : Vector stating maximum position for each dimension
     * @param minPosition : Vector stating minimum position for each dimension
     * @param maxVelocity : Vector stating maximum velocity for each dimension
     * @param minVelocity : Vector stating minimum velocity for each dimension
     */
    // public void init(double maxPosition[], double minPosition[], double maxVelocity[], double minVelocity[]) {
    //     for (int i = 0; i < position.length; i++) {
    //         if (Double.isNaN(maxPosition[i])) throw new RuntimeException("maxPosition[" + i + "] is NaN!");
    //         if (Double.isInfinite(maxPosition[i])) throw new RuntimeException("maxPosition[" + i + "] is Infinite!");

    //         if (Double.isNaN(minPosition[i])) throw new RuntimeException("minPosition[" + i + "] is NaN!");
    //         if (Double.isInfinite(minPosition[i])) throw new RuntimeException("minPosition[" + i + "] is Infinite!");

    //         if (Double.isNaN(maxVelocity[i])) throw new RuntimeException("maxVelocity[" + i + "] is NaN!");
    //         if (Double.isInfinite(maxVelocity[i])) throw new RuntimeException("maxVelocity[" + i + "] is Infinite!");

    //         if (Double.isNaN(minVelocity[i])) throw new RuntimeException("minVelocity[" + i + "] is NaN!");
    //         if (Double.isInfinite(minVelocity[i])) throw new RuntimeException("minVelocity[" + i + "] is Infinite!");

    //         // Initialize using uniform distribution
    //         position[i] = (maxPosition[i] - minPosition[i]) * Math.random() + minPosition[i];
    //         velocity[i] = (maxVelocity[i] - minVelocity[i]) * Math.random() + minVelocity[i];

    //         bestPosition[i] = Double.NaN;
    //     }
    // }

    public void setBestFitness(double bestFitness) {
        this.bestFitness = bestFitness;
    }

    public void setBestPosition(List<Allocation> bestPosition) {
        this.bestPosition = bestPosition;
    }

    /**
     * Set fitness and best fitness accordingly.
     * If it's the best fitness so far, copy data to bestFitness[]
     * @param fitness : New fitness value
     * @param maximize : Are we maximizing or minimizing fitness function?
     */
    public void setFitness(double fitness, boolean maximize) {
        this.fitness = fitness;
        if ((maximize && (fitness > bestFitness)) // Maximize and bigger? => store data
                || (!maximize && (fitness < bestFitness)) // Minimize and smaller? => store data too
                || Double.isNaN(bestFitness)) {
            copyPosition2Best();
            bestFitness = fitness;
        }
    }

    public void setPosition(List<Allocation> position) {
        this.position = position;
    }

    public void setVelocity(List<Allocation> velocity) {
        this.velocity = velocity;
    }

    /** Printable string */
    @Override
    public String toString() {
        String str = "fitness: " + fitness + "\tbest fitness: " + bestFitness;

        if (position != null) {
            str += "\n\tPosition:\n";
            for (Allocation allocation : position)
                str += allocation + "\n";
            str += "\n";   
        }

        if (velocity != null) {
            str += "\n\tVelocity:\n";
            for (Allocation allocation : velocity)
                str += allocation + "\n";
            str += "\n";
        }

        if (bestPosition != null) {
            str += "\n\tBest:\n";
            for (Allocation allocation : bestPosition)
                str += allocation + "\n";
            str += "\n";
        }

        str += "\n";
        return str;
    }
}

