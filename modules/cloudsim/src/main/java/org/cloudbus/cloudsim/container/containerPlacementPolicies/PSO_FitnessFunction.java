package org.cloudbus.cloudsim.container.containerPlacementPolicies;

import java.util.List;

import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerCloudlet;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.container.core.ContainerVm;
import org.cloudbus.cloudsim.container.core.PowerContainerHost;

import net.sourceforge.jswarm_pso.FitnessFunction;

public class PSO_FitnessFunction extends FitnessFunction{

    List<Container> containerList;
    List<ContainerVm> vmList;
    List<ContainerHost> hostList;

    double executionTime[]; //execution time for each task
    double vmUtilization[]; //utilization for each vm
    double energy[]; //energy for each task

    public PSO_FitnessFunction(List<Container> containerList, List<ContainerVm> vmList, List<ContainerHost> hostList){
        this.containerList = containerList;
        this.vmList = vmList;
        this.hostList = hostList;

        executionTime = new double[containerList.size()];
        vmUtilization = new double[containerList.size()]; 
        energy = new double[containerList.size()];
    }

    public double evaluate(double[] position) {

        //execution time = cloudlet length (total mips) / mv mips
        for(int i=0; i< position.length; i++) 
            executionTime[i] = containerList.get(i).getContainerCloudletScheduler().getCloudletExecList().get(0).getCloudletLength() / vmList.get((int)position[i]).getMips();

        //utilization
        //for()

        //energy 
         for(int i=0; i< position.length; i++) 
             energy[i] = executionTime[i] 
                 * ((PowerContainerHost) vmList.get((int)position[i]).getHost()).getPower(90);


        double ponderado = 0.0d;
        for(int i=0; i< position.length; i++)
            ponderado += executionTime[i] * energy[i];

		return ponderado;
	}

}
