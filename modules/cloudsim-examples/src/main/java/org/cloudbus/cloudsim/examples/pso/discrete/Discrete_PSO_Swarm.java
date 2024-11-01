package org.cloudbus.cloudsim.examples.pso.discrete;

import java.util.*;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;

import net.sourceforge.jswarm_pso.Particle;

import org.cloudbus.cloudsim.examples.pso.Allocation;
import org.cloudbus.cloudsim.examples.pso.Constants;
import org.cloudbus.cloudsim.examples.pso.Helper;

/**
 * 
 * @author carlosandres.mendez
 */
public class Discrete_PSO_Swarm {

    public static double DEFAULT_GLOBAL_INCREMENT = 0.9;
	public static int DEFAULT_INERTIA = 5;
	public static int DEFAULT_NUMBER_OF_PARTICLES = 25;
	public static double DEFAULT_PARTICLE_INCREMENT = 0.9;

    /** Number of particles in this swarm */
	int numberOfParticles;
	/** Particle's increment (for velocity update), usually called 'c1' constant */
	double particleIncrement;
    /** Global increment (for velocity update), usually called 'c2' constant */
	double globalIncrement;
	/** Inertia (for velocity update), usually called 'w' constant */
	int inertia;

    /** Best fitness so far (global best) */
    double bestFitness;
	/** Index of best particle so far */
	int bestParticleIndex;
    /** Best position so far (global best) */
    List<Allocation> bestPosition;
    /** Fitness function for this swarm */
	Discrete_FitnessFunction fitnessFunction;
    /** Particle update strategy */
	Discrete_ParticleUpdate particleUpdate;

    private ArrayList<Discrete_Particle> particles;

    List<PowerHost> powerHosts;
    List<PowerVm> powerVms;
    List<Cloudlet> cloudlets;

    /**
	 * Create a Swarm and set default values
	 * @param numberOfParticles : Number of particles in this swarm (should be greater than 0). 
	 * If unsure about this parameter, try Swarm.DEFAULT_NUMBER_OF_PARTICLES or greater
	 * @param fitnessFunction : Fitness function used to evaluate each particle
	 */
    public Discrete_PSO_Swarm(Discrete_FitnessFunction fitnessFunction, List<PowerHost> powerHosts, List<PowerVm> powerVms, List<Cloudlet> cloudlets) {
		globalIncrement = DEFAULT_GLOBAL_INCREMENT;
		inertia = DEFAULT_INERTIA;
		particleIncrement = DEFAULT_PARTICLE_INCREMENT;
        numberOfParticles = DEFAULT_NUMBER_OF_PARTICLES;

        bestFitness = Double.NaN; // important for setting the best fitness
		
		this.fitnessFunction = fitnessFunction;

        this.powerHosts = powerHosts;
        this.powerVms = powerVms;
        this.cloudlets = cloudlets;

        // Set up particle update strategy (default: ParticleUpdateSimple) 
		particleUpdate = new Discrete_ParticleUpdate(); //default update dehaviour
        bestParticleIndex = -1;
    }

	/**
	 * Initialize every particle
	 * Warning: maxPosition[], minPosition[], maxVelocity[], minVelocity[] must be initialized and setted
	 */
	public void init() {

		particles = new ArrayList<>();

	}

    public static Set<Integer> getUniqueRandomNumbers(int n, int upperBound) {
        Set<Integer> uniqueNumbers = new HashSet<>();
        Random random = new Random();

        while (uniqueNumbers.size() < n) {
            int number = random.nextInt(upperBound); // Genera un número aleatorio entre 0 y upperBound-1
            uniqueNumbers.add(number);
        }

        return uniqueNumbers;
    }

    /**
	 * Evaluate fitness function for every particle 
	 * Warning: particles[] must be initialized and fitnessFunction must be set
	 */
	public void evaluate() {
		if (particles == null) throw new RuntimeException("No particles in this swarm! May be you need to call Swarm.init() method");
		if (fitnessFunction == null) throw new RuntimeException("No fitness function in this swarm! May be you need to call Discrete_PSO_Swarm.setFitnessFunction() method");

		// Initialize
		if (Double.isNaN(bestFitness)) {
			bestFitness = (fitnessFunction.isMaximize() ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
			bestPosition = null;
            bestParticleIndex = -1;
		}

		//---
		// Evaluate each particle (and find the 'best' one)
		//---
        int cont=0;
		for (Discrete_Particle particle : particles) {

			// Evaluate particle
			double fit = fitnessFunction.evaluate(particle);

            // For analysis and stats
            particle.maeGlobalUpdate = 0.0d;

			// Update 'best global' position
			if (fitnessFunction.isBetterThan(bestFitness, fit)) {

                //*** For analysis and stats  ***/
                if(bestPosition!=null){
                    double[] particlePositionToArray = new double[particle.getPosition().size()];
                    int j=0;
                    for(Allocation allocation : particle.getPosition()){
                        particlePositionToArray[j++] = allocation.getVm().getId();
                    }
                    double[] bestGlobalPositionToArray = new double[particle.getPosition().size()];
                    j=0;
                    for(Allocation allocation : bestPosition){
                        bestGlobalPositionToArray[j++] = allocation.getVm().getId();
                    }
                    particle.maeGlobalUpdate = Helper.calculateMAE(particlePositionToArray, bestGlobalPositionToArray);
                }

                //*** Copy best fitness, index, and position vector  ***/ 
                bestParticleIndex = cont;
				bestFitness = fit; 
				if (bestPosition == null) bestPosition = new ArrayList<>();
				particle.copyPosition(bestPosition);
			}
            cont++;
		}
	}

    /**
	 * Make an iteration: 
	 * 	- evaluates the swarm 
	 * 	- updates positions and velocities
	 * 	- applies positions and velocities constraints 
	 */
	public void evolve() {
		// Initialize (if not already done)
		if (particles == null) init();

		evaluate(); // Evaluate particles
		update(); // Update positions and velocities
	}

    	/**
	 * Update every particle's position and velocity, also apply position and velocity constraints (if any)
	 * Warning: Particles must be already evaluated
	 */
	public void update() {

        // For each particle...
        for (Discrete_Particle particle : particles) {
            // Update particle's position and speed
            // Apply position and velocity constraints
            particleUpdate.update(this, particle);
        }

	}

    public int getNumberOfParticles() {
        return numberOfParticles;
    }

    public void setNumberOfParticles(int numberOfParticles) {
        this.numberOfParticles = numberOfParticles;
    }

    public double getParticleIncrement() {
        return particleIncrement;
    }

    public void setParticleIncrement(double particleIncrement) {
        this.particleIncrement = particleIncrement;
    }

    public double getGlobalIncrement() {
        return globalIncrement;
    }

    public void setGlobalIncrement(double globalIncrement) {
        this.globalIncrement = globalIncrement;
    }

    public double getBestFitness() {
        return bestFitness;
    }

    public void setBestFitness(double bestFitness) {
        this.bestFitness = bestFitness;
    }

    public List<Allocation> getBestPosition() {
        return bestPosition;
    }

    public void setBestPosition(List<Allocation> bestPosition) {
        this.bestPosition = bestPosition;
    }

    public Discrete_FitnessFunction getFitnessFunction() {
        return fitnessFunction;
    }

    public void setFitnessFunction(Discrete_FitnessFunction fitnessFunction) {
        this.fitnessFunction = fitnessFunction;
    }

    public Discrete_ParticleUpdate getParticleUpdate() {
        return particleUpdate;
    }

    public void setParticleUpdate(Discrete_ParticleUpdate particleUpdate) {
        this.particleUpdate = particleUpdate;
    }

    public ArrayList<Discrete_Particle> getParticles() {
        return particles;
    }

    public List<PowerHost> getPowerHosts() {
        return powerHosts;
    }

    public void setPowerHosts(List<PowerHost> powerHosts) {
        this.powerHosts = powerHosts;
    }

    public List<PowerVm> getPowerVms() {
        return powerVms;
    }

    public void setPowerVms(List<PowerVm> powerVms) {
        this.powerVms = powerVms;
    }

    public List<Cloudlet> getCloudlets() {
        return cloudlets;
    }

    public void setCloudlets(List<Cloudlet> cloudlets) {
        this.cloudlets = cloudlets;
    }

    public void setParticles(ArrayList<Discrete_Particle> particles) {
        this.particles = particles;
    }

    public int getDimension(){
        return cloudlets.size();
    }

    public int getInertia() {
        return inertia;
    }

    public void setInertia(int inertia) {
        this.inertia = inertia;
    }

    public Discrete_Particle getBestParticle() {
		return particles.get(bestParticleIndex);
	}
    
}