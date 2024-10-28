package org.cloudbus.cloudsim.examples.pso.original;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.examples.pso.Constants;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.power.PowerHost;

import net.sourceforge.jswarm_pso.FitnessFunction;

public class PSO_FitnessFunction extends FitnessFunction{

    List<Cloudlet> clouletList;
    List<PowerVm> vmList;
    List<PowerHost> hostList;

    double vmExecutionTime[]; //execution time for each vm considering the tasks are going to process
    double hostUtilization[]; //utilization for each host
    double vmUtilization[]; //utilization for each host
    double energy[]; //energy for each task

    public PSO_FitnessFunction(List<Cloudlet> clouletList, List<PowerVm> vmList, List<PowerHost> hostList){
        this.clouletList = clouletList;
        this.vmList = vmList;
        this.hostList = hostList;

        //executionTime = new double[clouletList.size()];
        vmExecutionTime = new double[vmList.size()];
        hostUtilization = new double[hostList.size()]; 
        vmUtilization = new double[vmList.size()]; 
        energy = new double[clouletList.size()];

        setMaximize(true);
    }

    public double evaluate(double[] position) {


        /**
         * NUMBER OF HOSTS in the position array
         * It is obtained from the vm, because each vm contains the host 
         */

         int numberOfHosts = 0;
        Set<Integer> hostIds = new HashSet<Integer>();
        for(int i=0; i<position.length; i++) {
            for(Vm vm : vmList) {
                if(vm.getId()==position[i])
                    hostIds.add(vm.getHost().getId());
            }
        }
        numberOfHosts = hostIds.size();

/**
 *      VM EXECUTION TIME (total execution time of a vm in the simulation considering the cloudlets it has to process)
 *      Estimated by mips: total cloudlets Length or size in Millions Instructions (MI) / total vm MIPS
 *           [mips refers to The total mips capacity of the PE of the VMs
 *              Pe (Processing Element) class represents a CPU core of a physical machine (PM), 
 *              defined in terms of Millions Instructions Per Second (MIPS) rating]
 *              see org.cloudbus.cloudsim.provisioners.PeProvisioner.Pe.java
**/

        //For each vm we are going to calculate the execution time using the cloudlets mips it has to process
        for(PowerVm vm : vmList){
            //sum all cloudlet the vm has to process
            for(int i=0; i< position.length; i++){
                if((int)position[i]==vm.getId())
                    vmExecutionTime[vm.getId()]+= clouletList.get(i).getCloudletLength();
            }
            //consider the vm capacity
            vmExecutionTime[vm.getId()] = vmExecutionTime[vm.getId()] / vm.getMips() * vm.getNumberOfPes();
        } 
        

/**
 *      HOST UTILIZATION
 *      Estimated percentage: total VMs mips / total host mips
 *      This is a estimated value when all the vms are started (in this simulation all the vms start at the same time)
 *      However, during the simulation this value is going to be changed depending on the finish cloudlets time or vm migrations
**/
        for(PowerHost host : hostList){
            double vmsMIPS = 0.0d;
            for(Vm vm : host.getVmList()){
                vmsMIPS += vm.getMips() * vm.getNumberOfPes();
            }
            hostUtilization[host.getId()]=vmsMIPS/(double)host.getTotalMips();
        }

   
/**
 *      NUMBER OF HOSTS WITH OVER UTILIZATION
 *      Estimated count the number of host over UTILIZATION_THRESHOLD
**/  

        int numberHostOverUtilized = 0;
        for(int i=0; i< hostUtilization.length; i++){ 
            if(hostUtilization[i]>Constants.UTILIZATION_THRESHOLD)
                numberHostOverUtilized++;
        }

/**
 *      TOTAL UTILIZATION HOSTS MIPS of this solution (Estimated Resource Utilization)
 *      We want to minimize the number of available mips in the datacenter.
 *      This sum all the available mips of only the hosts of the especific solucion/allocation (particle position)
**/ 

        
        double hostsUtiliMips = 0.0d;
        double hostsTotalMips = 0.0d;
        for(PowerHost host : hostList){
            if(hostIds.contains(host.getId())){ //considering only hosts in the this especific solucion/allocation (particle position)
                hostsUtiliMips += hostUtilization[host.getId()]*host.getTotalMips();
                hostsTotalMips += host.getTotalMips();
            }
        }

        double resourceUtilization = hostsUtiliMips/hostsTotalMips;


/**
 *      ENERGY
 *      Estimated by the estimated host utilization
**/ 
        double totalDatacenterPowerConsumption=0.0d;
        for(PowerHost host : hostList){
            if(hostIds.contains(host.getId())){ //considering only hosts in the this especific solucion/allocation (particle position)
                    totalDatacenterPowerConsumption += host.getPower(hostUtilization[host.getId()]); //Constants.UTILIZATION_THRESHOLD
            }
        }

        //normalize: make the value comparable by changing the value from 0 to 1
        double maxHostPowerConsumption=0.0d;
        double worstDatacenterPowerConsumption = 0.0d;
        for(PowerHost host : hostList){
            maxHostPowerConsumption = Math.max(maxHostPowerConsumption, host.getPower(1));
        }
        worstDatacenterPowerConsumption = maxHostPowerConsumption * hostList.size();
        totalDatacenterPowerConsumption = totalDatacenterPowerConsumption / worstDatacenterPowerConsumption;

/**
 *      MAKESPAN
 *      Estimated by the max vm execution time, since all vms start at the same time
**/ 
        double makespan = 0.0d;
        for(int i=0; i< vmList.size(); i++){
            makespan = Math.max(makespan, vmExecutionTime[i]);
        }

        //normalize: make the value comparable by changing the value from 0 to 1
        double maxClouletLenght = 0.0d;
        double minVmMips = Double.MAX_VALUE;
        for(int i=0; i< position.length; i++){ 
            maxClouletLenght = Math.max(maxClouletLenght, clouletList.get(i).getCloudletLength());
            minVmMips = Math.min(minVmMips, vmList.get(i).getMips() * vmList.get(i).getNumberOfPes());
        }
        double maxPosibleVmExcecutionTime = maxClouletLenght * (clouletList.size()) / minVmMips; // (clouletList.size() / 5) is a estimated num max tasks in a vm
        makespan = makespan / maxPosibleVmExcecutionTime;


/**
 *      LOAD BALANCING
 *      Estimated by the vm execution time variance
**/ 

        //desbalancing degree calculated as the variance of host utilization
        //double desbalancingDegree = calculateVariance(hostUtilization); //vmExecutionTime
        double balancingDegree = (double)numberOfHosts / hostList.size();

        //desbalancingDegree normalized
        //double maxVariance = maxPosibleVmExcecutionTime*maxPosibleVmExcecutionTime/4;
        //desbalancingDegree = desbalancingDegree/maxVariance;


        //objetive function
        double weight1 = 0.3;
        double weight2 = 0.3;
        double weight3 = 0.1;
        double weight4 = 0.3;
        double weight5 = 0;
        double functOutput =  1/((weight1 * totalDatacenterPowerConsumption) 
            + (weight2 * (1-resourceUtilization)) //this can be read resource sub utilization
            + (weight3 * makespan) 
            + (weight4 * (1-balancingDegree)) 
            + (weight5 * numberHostOverUtilized==0?0:(numberHostOverUtilized/numberOfHosts)));

/**
 *      This is when we need to validate, for example in containers
 * 

        //We are going to remove invalid position setting functOutput to 0.
        //invalid position is when cpu capacity of a vm is bigger than the sum of cpu demanded of tasks
        //Required for the Original PSO

        //The next map is used for set the capacity MIPS of each VM
        Map<Integer, Double> vmCapacity = new HashMap<>();
        //The next map is used for set the demanded MIPS of each VM 
        Map<Integer, Double> vmDemanded = new HashMap<>();

        //fill the maps
        for(int i=0; i< position.length; i++){ 
            PowerVm vm = vmList.get((int)position[i]);
            if(!vmCapacity.containsKey(vm.getId()))
                vmCapacity.put(vm.getId(), vm.getMips() * vm.getNumberOfPes());
            
            if(!vmDemanded.containsKey(vm.getId()))
                vmDemanded.put(vm.getId(), clouletList.get(i).getMips());
            else
            vmDemanded.put(vm.getId(), vmDemanded.get(vm.getId()) + clouletList.get(i).getMips());
        }

        //when invalid position set to 0
        for (Map.Entry<Integer, Double> entry : vmDemanded.entrySet()) {
            if(entry.getValue() > vmCapacity.get(entry.getKey()))
                functOutput = 0.0d;
        }
 */

        //print results
        System.out.print("--------- evaluate ");
        for(int i=0;i<position.length;i++) {
            System.out.print(position[i]+" ");
        }
        System.out.println(functOutput);

        return functOutput;
	}

    public double evaluate2(double[] position) {

        //cloudlet execution time = cloudlet length (total mips) / mv mips
        // for(int i=0; i< position.length; i++) 
        //     executionTime[i] = clouletList.get(i).getCloudletLength() / vmList.get((int)position[i]).getMips();

        //For each vm we are going to calculate the execution time using the cloudlets mips it has to process
        for(PowerVm vm : vmList){
            //sum all cloudlet the vm has to process
            for(int i=0; i< position.length; i++){
                if((int)position[i]==vm.getId())
                    vmExecutionTime[vm.getId()]+= clouletList.get(i).getCloudletLength();
            }
            //consider the vm capacity
            vmExecutionTime[vm.getId()] = vmExecutionTime[vm.getId()] / vm.getMips() * vm.getNumberOfPes();
        } 
        
/**
 *      This is when we have the host information
 * 

        //host utilization
        for(int i=0; i< hostUtilization.length; i++) 
            hostUtilization[i]=0.0d;
        for(PowerVm vm : vmList){
            double utilization = (double)vm.getMips() / (double)vm.getHost().getTotalMips();
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
            PowerHost host = (PowerHost) vmList.get((int)position[i]).getHost();
            double power = host.getPowerModel().getPower(hostUtilization[host.getId()-1]);
            energy[i] = executionTime[i] * power;
        }

        //totals
        double totalEnergyCPU = 0.0d;
        double makespan = 0.0d;
        for(int i=0; i< position.length; i++){
            totalEnergyCPU += energy[i];
            makespan += executionTime[i];
        }
**/

        //total
        double makespan = 0.0d;
        for(int i=0; i< vmList.size(); i++){
            makespan = Math.max(makespan, vmExecutionTime[i]);
        }

        //desbalancing degree calculated as the variance of host utilization
        double desbalancingDegree = calculateVariance(vmExecutionTime);

        //normalize: make comparable variables
        double minClouletLenght = Double.MAX_VALUE;
        double maxVmMips = Double.MAX_VALUE;
        for(int i=0; i< position.length; i++){ 
            minClouletLenght = Math.min(minClouletLenght, clouletList.get(i).getCloudletLength());
            maxVmMips = Math.max(maxVmMips, vmList.get(i).getMips() * vmList.get(i).getNumberOfPes());
        }
        double minPosibleExcecutionTime = minClouletLenght / maxVmMips;
        makespan = minPosibleExcecutionTime / makespan;

        //objetive function
        double weight1 = 0.5;
        double weight2 = 0.5;
        double functOutput = (weight1 * makespan) + (weight2 * desbalancingDegree);

/**
 *      This is when we need to validate, for example in containers
 * 

        //We are going to remove invalid position setting functOutput to 0.
        //invalid position is when cpu capacity of a vm is bigger than the sum of cpu demanded of tasks
        //Required for the Original PSO

        //The next map is used for set the capacity MIPS of each VM
        Map<Integer, Double> vmCapacity = new HashMap<>();
        //The next map is used for set the demanded MIPS of each VM 
        Map<Integer, Double> vmDemanded = new HashMap<>();

        //fill the maps
        for(int i=0; i< position.length; i++){ 
            PowerVm vm = vmList.get((int)position[i]);
            if(!vmCapacity.containsKey(vm.getId()))
                vmCapacity.put(vm.getId(), vm.getMips() * vm.getNumberOfPes());
            
            if(!vmDemanded.containsKey(vm.getId()))
                vmDemanded.put(vm.getId(), clouletList.get(i).getMips());
            else
            vmDemanded.put(vm.getId(), vmDemanded.get(vm.getId()) + clouletList.get(i).getMips());
        }

        //when invalid position set to 0
        for (Map.Entry<Integer, Double> entry : vmDemanded.entrySet()) {
            if(entry.getValue() > vmCapacity.get(entry.getKey()))
                functOutput = 0.0d;
        }
 */

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
            //sumaDiferenciasCuadradas += Math.pow((num - media)/media, 2);
            sumaDiferenciasCuadradas += Math.pow((num - media), 2);
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
