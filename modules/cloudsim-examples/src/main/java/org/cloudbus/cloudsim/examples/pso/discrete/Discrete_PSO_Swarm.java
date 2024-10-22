package org.cloudbus.cloudsim.examples.pso.discrete;

import java.util.*;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.examples.pso.Allocation;
import org.cloudbus.cloudsim.examples.pso.Constants;

/**
 * 
 * @author carlosandres.mendez
 */
public class Discrete_PSO_Swarm {
    /** Best fitness so far (global best) */
    double bestFitness;
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
        bestFitness = Double.NaN; // important for setting the best fitness
		
		this.fitnessFunction = fitnessFunction;

        this.powerHosts = powerHosts;
        this.powerVms = powerVms;
        this.cloudlets = cloudlets;

        // Set up particle update strategy (default: ParticleUpdateSimple) 
		particleUpdate = new Discrete_ParticleUpdate();
    }

	/**
	 * Initialize every particle
	 * Warning: maxPosition[], minPosition[], maxVelocity[], minVelocity[] must be initialized and setted
	 */
	public void init() {

		particles = new ArrayList<>();

        //Creamos particulas aleatorias
        for (int i=0; i < Constants.NUM_PARTICLES; i++) {

            List<Allocation> position = new ArrayList<>();
            Queue<Allocation> velocity = new LinkedList<>();

            for(Cloudlet cloudlet : this.cloudlets){ //Para cada contenedor buscamos aleatoriamente un host y una vm
                Random randObj = new Random();
                Allocation allocation = new Allocation(
                    cloudlet, 
                    this.powerVms.get(randObj.nextInt(this.powerVms.size())), 
                    this.powerHosts.get((int)Math.random()*this.powerHosts.size()));
                position.add(allocation);
            }
            particles.add(new Discrete_Particle(position, velocity));
        }
    
        System.out.println();
        for(Discrete_Particle particle : particles)
            System.out.println(particle);
        System.out.println();
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
		}

		//---
		// Evaluate each particle (and find the 'best' one)
		//---
		for (Discrete_Particle particle : particles) {

			// Evaluate particle
			double fit = fitnessFunction.evaluate(particle);

			// Update 'best global' position
			if (fitnessFunction.isBetterThan(bestFitness, fit)) {
				bestFitness = fit; // Copy best fitness, index, and position vector
				if (bestPosition == null) bestPosition = new ArrayList<>();
				particle.copyPosition(bestPosition);
			}

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

    public void setParticles(ArrayList<Discrete_Particle> particles) {
        this.particles = particles;
    }

}