package org.cloudbus.cloudsim.container.containerPlacementPolicies;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.container.core.ContainerVm;
import org.cloudbus.cloudsim.container.core.PowerContainerHost;
import org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled.Allocation;

import net.sourceforge.jswarm_pso.FitnessFunction;

public class PSO_FitnessFunction extends FitnessFunction{

    List<Container> containerList;
    List<ContainerVm> vmList;
    List<ContainerHost> hostList;

    double executionTime[]; //execution time for each task
    double hostUtilization[]; //utilization for each host
    double energy[]; //energy for each task

    public PSO_FitnessFunction(List<Container> containerList, List<ContainerVm> vmList, List<ContainerHost> hostList){
        this.containerList = containerList;
        this.vmList = vmList;
        this.hostList = hostList;

        executionTime = new double[containerList.size()];
        hostUtilization = new double[hostList.size()]; 
        energy = new double[containerList.size()];

        setMaximize(true);
    }

    public double evaluate(double[] position) {

        //cloudlet execution time = cloudlet length (total mips) / mv mips
        for(int i=0; i< position.length; i++) 
            executionTime[i] = containerList.get(i).getCloudlet().getCloudletLength() / vmList.get((int)position[i]).getMips();

        //host utilization
        for(int i=0; i< hostUtilization.length; i++) 
            hostUtilization[i]=0.0d;
        for(ContainerVm vm : vmList){
            double utilization = (double)vm.getTotalMips() / (double)vm.getHost().getTotalMips();
            hostUtilization[vm.getHost().getId()-1] +=  utilization;
        }
        //Utilization value must be between 0 and 1
        for(int i=0; i< hostUtilization.length; i++){
            if(hostUtilization[i]>1.0d)
                hostUtilization[i]=1.0d;
            else if(hostUtilization[i]<0.0d)
                hostUtilization[i]=0.0d;
        }

        //energy used for each cloulet 
        for(int i=0; i< position.length; i++){ 
            PowerContainerHost host = (PowerContainerHost) vmList.get((int)position[i]).getHost();
            double power = host.getPower(hostUtilization[host.getId()-1]);
            energy[i] = executionTime[i] * power;
        }

        //totals
        double totalEnergyCPU = 0.0d;
        double makespan = 0.0d;
        for(int i=0; i< position.length; i++){
            totalEnergyCPU += energy[i];
            makespan += executionTime[i];
        }

        //desbalancing degree calculated as the variance of host utilization
        double desbalancingDegree = calculateVariance(hostUtilization);

        //make comparable variables
        double minServerEnergy = Double.MAX_VALUE;
        double maxVmMIPS = 0.0d;

        for(ContainerHost host : hostList){
            PowerContainerHost powerHost = (PowerContainerHost)host;
            if(powerHost.getPower(0.3) < minServerEnergy)
                minServerEnergy = powerHost.getPower(0.3);
        }

        for(ContainerVm vm : vmList){
            if(vm.getTotalMips()>maxVmMIPS)
                maxVmMIPS = vm.getTotalMips();
        }

        double minPosibleExcecutionTime = 0.0d;
        double minPosibleEnergyUsed = 0.0d;

        double totalCloudletLength = 0.0d;
        for(Container container : containerList)
            totalCloudletLength += container.getCloudlet().getCloudletLength();
        minPosibleExcecutionTime = totalCloudletLength / (maxVmMIPS * vmList.size());

        //min posible energy used 
        for(int i=0; i< position.length; i++){ 
            minPosibleEnergyUsed += executionTime[i] * minServerEnergy;
        }

        makespan = minPosibleExcecutionTime / makespan;
        totalEnergyCPU = minPosibleEnergyUsed / totalEnergyCPU;

        //objetive function
        double weight1 = 0.3;
        double weight2 = 0.5;
        double weight3 = 0.1;
        double functOutput = (weight1 * makespan) + (weight2 * totalEnergyCPU) + (weight3 * desbalancingDegree);

        //We are going to remove invalid position setting functOutput to 0.
        //invalid position is when cpu capacity of a vm is bigger than the sum of cpu demanded of tasks
        //Required for the Original PSO

        //The next map is used for set the capacity MIPS of each VM
        Map<Integer, Double> vmCapacity = new HashMap<>();
        //The next map is used for set the demanded MIPS of each VM 
        Map<Integer, Double> vmDemanded = new HashMap<>();

        //fill the maps
        for(int i=0; i< position.length; i++){ 
            ContainerVm vm = vmList.get((int)position[i]);
            if(!vmCapacity.containsKey(vm.getId()))
                vmCapacity.put(vm.getId(), vm.getMips() * vm.getNumberOfPes());
            
            if(!vmDemanded.containsKey(vm.getId()))
                vmDemanded.put(vm.getId(), containerList.get(i).getMips());
            else
            vmDemanded.put(vm.getId(), vmDemanded.get(vm.getId()) + containerList.get(i).getMips());
        }

        //when invalid position set to 0
        for (Map.Entry<Integer, Double> entry : vmDemanded.entrySet()) {
            if(entry.getValue() > vmCapacity.get(entry.getKey()))
                functOutput = 0.0d;
        }


        //print results
        System.out.print("--------- evaluate ");
        for(int i=0;i<position.length;i++) {
            System.out.print(position[i]+" ");
        }
        System.out.println(functOutput);

        return functOutput;
	}

    // Método para calcular la varianza de un arreglo de doubles
    public double calculateVariance(double[] numeros) {
        double media = calculateMedia(numeros);
        double sumaDiferenciasCuadradas = 0.0;

        for (double num : numeros) {
            sumaDiferenciasCuadradas += Math.pow(num - media, 2);
        }

        return sumaDiferenciasCuadradas / numeros.length;
    }

    // Método para calcular la media de un arreglo de doubles
    public double calculateMedia(double[] numeros) {
        double suma = 0.0;
        for (double num : numeros) {
            suma += num;
        }
        return suma / numeros.length;
    }

}
