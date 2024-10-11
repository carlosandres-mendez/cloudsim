package org.cloudbus.cloudsim.examples.pso.discrete;

import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.examples.pso.Allocation;
import org.cloudbus.cloudsim.examples.pso.original.PSO_FitnessFunction;


public class Discrete_FitnessFunction extends PSO_FitnessFunction{

    public Discrete_FitnessFunction(List<Cloudlet> cloudletList, List<PowerVm> vmList, List<PowerHost> hostList){
        super(cloudletList, vmList, hostList);
    }

    /**
	 * Evaluates a particles 
	 * @param particle : Particle to evaluate
	 * @return Fitness function for a particle
	 */
	public double evaluate(Discrete_Particle particle) {
        double[] positionArray = new double[particle.getPosition().size()];
		int i=0;
        for(Allocation allocation : particle.getPosition()){
            positionArray[i++] = allocation.getVm().getId();
        }

		double fit = evaluate(positionArray);
		particle.setFitness(fit, super.isMaximize());
		return fit;
	}

}
