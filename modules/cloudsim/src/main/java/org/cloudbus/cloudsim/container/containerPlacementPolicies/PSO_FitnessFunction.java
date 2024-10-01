package org.cloudbus.cloudsim.container.containerPlacementPolicies;

import java.util.List;

import org.cloudbus.cloudsim.container.core.ContainerCloudlet;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.container.core.ContainerVm;
import org.cloudbus.cloudsim.container.core.PowerContainerHost;

import net.sourceforge.jswarm_pso.FitnessFunction;

public class PSO_FitnessFunction extends FitnessFunction{

    List<ContainerCloudlet> cloudletList;
    List<ContainerVm> vmList;
    List<ContainerHost> hostList;

    double executionTime[]; //execution time for each task
    double vmUtilization[]; //utilization for each vm
    double energy[]; //energy for each task

    public PSO_FitnessFunction(List<ContainerCloudlet> cloudletList, List<ContainerVm> vmList, List<ContainerHost> hostList){
        this.cloudletList = cloudletList;
        this.vmList = vmList;
        this.hostList = hostList;

        executionTime = new double[cloudletList.size()];
        vmUtilization = new double[cloudletList.size()]; 
        energy = new double[cloudletList.size()];
    }

    public double evaluate(double[] position) {

        //execution time = cloudlet length (total mips) / mv mips
        for(int i=0; i< position.length; i++) 
            executionTime[i] = cloudletList.get(i).getCloudletLength() / vmList.get((int)position[i]).getMips();

        //utilization
        //for()

        //energy 
        // for(int i=0; i< position.length; i++) 
        //     energy[i] = executionTime[i] 
        //         * ((PowerContainerHost) vmList.get((int)position[i]).getHost()).getPower(90);


        double ponderado = 0.0d;
        for(int i=0; i< position.length; i++)
            ponderado += executionTime[i]; // * energy[i];

		return ponderado;
	}

}
