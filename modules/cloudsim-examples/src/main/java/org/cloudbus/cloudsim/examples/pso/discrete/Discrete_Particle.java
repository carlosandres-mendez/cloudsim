package org.cloudbus.cloudsim.examples.pso.discrete;

import java.util.*;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.examples.power.planetlab.PlanetLabConstants;
import org.cloudbus.cloudsim.examples.power.random.RandomConstants;
import org.cloudbus.cloudsim.examples.pso.Allocation;
import org.cloudbus.cloudsim.examples.pso.PlanetLabRunner;

/**
 * 
 * @author carlosandres.mendez
 */
public class Discrete_Particle {

    //int NUMBER_OF_VMS = RandomConstants.NUMBER_OF_VMS;
    int NUMBER_OF_VMS = PlanetLabRunner.NUMBER_OF_VMS;
    //int NUMBER_OF_HOSTS = RandomConstants.NUMBER_OF_HOSTS;
    int NUMBER_OF_HOSTS = PlanetLabConstants.NUMBER_OF_HOSTS;

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

    public double mae; // for mae stat and analysis
    public double maeGlobalUpdate; // for mae stat and analysis
    
    /** Aditional info generated in the evaluation process */
    double vmTurnAroundTime[]; //execution time for each vm considering the tasks are going to process

    Map<Integer, List<Cloudlet>> vmCloudletsMap;

    double vmUtilization[]; //utilization for each vm
    double hostUtilization[];

    protected double powerConsumptionObjetive;
    protected double makespanObjetive;
    protected double desbalancingObjetive;
    protected double slaObjetive;

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
            positionCopy.add(new Allocation(alloc.getCloudlet(), alloc.getVm(), alloc.getHost()));
    }

    /** Copy position[] to bestPosition[] */
    public void copyPosition2Best() {
        bestPosition = new ArrayList<>();
        for(Allocation alloc : position)
            bestPosition.add(new Allocation(alloc.getCloudlet(), alloc.getVm(), alloc.getHost()));
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

    public void setVelocity(List<Allocation> velocity) {
        this.velocity = velocity;
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

    public double[] getVmTurnAroundTime() {
        return this.vmTurnAroundTime;
    }

	public void copyVmTurnAroundTime(double vmTurnAroundTime[]) {
        this.vmTurnAroundTime = new double[this.getDimension()];
		for (int i = 0; i < vmTurnAroundTime.length; i++)
            this.vmTurnAroundTime[i] = vmTurnAroundTime[i];
	}

    public Map<Integer, List<Cloudlet>> getVmCloudletsMap() {
        return vmCloudletsMap;
    }

    public void copyVmCloudletsMap(Map<Integer, List<Cloudlet>> vmCloudletsMap){
        this.vmCloudletsMap = new HashMap<>();
        for (Map.Entry<Integer, List<Cloudlet>> entry : vmCloudletsMap.entrySet()) 
            this.vmCloudletsMap.put(entry.getKey(), entry.getValue());
    }

    public double[] getVmUtilization(){
        return this.vmUtilization;
    }

    public void copyVmUtilization(double[] vmUtilization){
        this.vmUtilization = new double[this.getDimension()];
		for (int i = 0; i < vmUtilization.length; i++)
            this.vmUtilization[i] = vmUtilization[i];
    }  

    public double[] getHostUtilization(){
        return this.hostUtilization;
    }

    public void copyHostUtilization(double[] hostUtilization){
        this.hostUtilization = new double[NUMBER_OF_HOSTS];
		for (int i = 0; i < hostUtilization.length; i++)
            this.hostUtilization[i] = hostUtilization[i];
    }  

    public double getPowerConsumptionObjetive() {
        return powerConsumptionObjetive;
    }


    public void setPowerConsumptionObjetive(double powerConsumptionObjetive) {
        this.powerConsumptionObjetive = powerConsumptionObjetive;
    }


    public double getMakespanObjetive() {
        return makespanObjetive;
    }


    public void setMakespanObjetive(double makespanObjetive) {
        this.makespanObjetive = makespanObjetive;
    }


    public double getDesbalancingObjetive() {
        return desbalancingObjetive;
    }


    public void setDesbalancingObjetive(double desbalancingObjetive) {
        this.desbalancingObjetive = desbalancingObjetive;
    }


    public double getSlaObjetive() {
        return slaObjetive;
    }


    public void setSlaObjetive(double slaObjetive) {
        this.slaObjetive = slaObjetive;
    }


    /** Printable string */
    /** 
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
    }*/

    @Override
    public String toString() {
        String output = "\n***PARTICLE POSITION***\n";

        for(int i=0;i<NUMBER_OF_VMS;i++) {
            String tasks = "";
            int number_of_tasks = 0;
            for(int j=0;j<getPosition().size();j++) {
                if( i== getPosition().get(j).getVm().getId()) {
                    tasks +=(tasks.isEmpty() ? " " : " " ) + j;
                    ++number_of_tasks;
                }
            }
            // if(tasks.isEmpty())
            //     output += "NO Tasks is in VM "+ i+"\n";
            // else
            if(!tasks.isEmpty())
                output += number_of_tasks +" Tasks is in VM "+i +" Tasks id = " +tasks +"\n";
        }

        output += "\n***PARTICLE BEST POSITION***\n";
        if(getBestPosition()!=null) {
            for(int i=0;i<NUMBER_OF_VMS;i++) {
                String tasks = "";
                int number_of_tasks = 0;
                for(int j=0;j<getBestPosition().size();j++) {
                    if( i== getBestPosition().get(j).getVm().getId()) {
                        tasks +=(tasks.isEmpty() ? " " : " " ) + j;
                        ++number_of_tasks;
                    }
                }
                // if(tasks.isEmpty())
                //     output += "NO Tasks is in VM "+ i+"\n";
                // else
                if(!tasks.isEmpty())
                    output += number_of_tasks +" Tasks is in VM "+i +" Tasks id = " +tasks +"\n";
            }
        }

	    return output;
    }
}