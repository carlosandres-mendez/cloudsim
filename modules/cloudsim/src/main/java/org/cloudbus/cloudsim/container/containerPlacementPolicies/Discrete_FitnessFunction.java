package org.cloudbus.cloudsim.container.containerPlacementPolicies;

import java.util.List;

import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.container.core.ContainerVm;
import org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled.Allocation;
import org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled.Discrete_Particle;


public class Discrete_FitnessFunction extends PSO_FitnessFunction{

    public Discrete_FitnessFunction(List<Container> containerList, List<ContainerVm> vmList, List<ContainerHost> hostList){
        super(containerList, vmList, hostList);
    }

    /**
	 * Evaluates a particles 
	 * @param particle : Particle to evaluate
	 * @return Fitness function for a particle
	 */
	public double evaluate(Discrete_Particle particle) {
        double[] positionArray = new double[particle.getPosition().size()];
        for(Allocation allocation : particle.getPosition()){
            positionArray[allocation.getContainer().getId()-1] = allocation.getVm().getId()-1;
        }

		double fit = evaluate(positionArray);
		particle.setFitness(fit, super.isMaximize());
		return fit;
	}

}

