package org.cloudbus.cloudsim.examples.pso.original;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.examples.pso.Constants;
import org.cloudbus.cloudsim.examples.pso.RandomConstants;
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
 *      VM EXECUTION TIME
 *      Estimated by mips: total cloudlets mips / total vm mips
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
**/
        for(int i=0; i< hostUtilization.length; i++) 
            hostUtilization[i]=0.0d;
        for(PowerVm vm : vmList){
            //for each server in the position, sum all the vm mips
            for(int i=0; i< position.length; i++){
                if((int)position[i]==vm.getId()){
                    double vmMIPS = vm.getMips() * vm.getNumberOfPes();
                    hostUtilization[vm.getHost().getId()]+= vmMIPS;
                }
            }
        } 

        //set host utilization as a percentage
        for(int i=0; i< hostUtilization.length; i++){ 
            if(hostUtilization[i]!=0.0d)
                hostUtilization[i]=hostUtilization[i]/(double)hostList.get(i).getTotalMips();
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
 *      TOTAL AVAILABLE HOSTS MIPS of this solution (Estimated Resource SubUtilization)
 *      We want to minimize the number of available mips in the datacenter.
 *      This sum all the available mips of only the hosts of the especific solucion/allocation (particle position)
**/ 

        int numberOfHosts = 0;
        double hostsAvailableMips = 0.0d;
        double hostsTotalMips = 0.0d;
        for(int i=1; i< hostUtilization.length+1; i++){ 
            for(PowerHost host : hostList){
                if(host.getId()==i){
                    if(hostUtilization[i]!=0.0d){ //considering only hosts in the this especific solucion/allocation (particle position)
                        numberOfHosts++;
                        hostsTotalMips += (double)host.getTotalMips();
                        hostsAvailableMips += ((double)host.getTotalMips() * (1-hostUtilization[i]));
                    }
                }
            }
        }

        double resourceSubUtilization = hostsAvailableMips/hostsTotalMips;


/**
 *      MAKESPAN
 *      Estimated by the max vm execution time
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
        double maxPosibleExcecutionTime = maxClouletLenght / minVmMips;
        makespan = makespan / maxPosibleExcecutionTime;


/**
 *      LOAD BALANCING
 *      Estimated by the max vm execution time
**/ 

        //desbalancing degree calculated as the variance of host utilization
        double desbalancingDegree = calculateVariance(vmExecutionTime);



        //objetive function
        double weight1 = 0.6;
        double weight2 = 0.2;
        double weight3 = 0.4;
        double functOutput =  1/((weight1 * resourceSubUtilization) + (weight2 * desbalancingDegree) + (weight3 * numberHostOverUtilized==0?0:(numberHostOverUtilized/numberOfHosts)));

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
            sumaDiferenciasCuadradas += Math.pow((num - media)/media, 2);
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
