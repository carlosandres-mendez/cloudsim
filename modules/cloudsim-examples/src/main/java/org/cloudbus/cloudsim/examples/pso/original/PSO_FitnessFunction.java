package org.cloudbus.cloudsim.examples.pso.original;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    protected double hostUtilization[];
    protected double hostTurnAroundTime[]; //execution time for each host considering the tasks are going to process if no SLA 
    protected double vmTurnAroundTime[]; //execution time for each vm considering the tasks are going to process
    protected double vmUtilization[]; //utilization for each vm
    protected Map<Integer, List<Cloudlet>> vmCloudletsMap;

    protected double powerConsumptionObjetive;
    protected double makespanObjetive;
    protected double desbalancingObjetive;
    protected double slaObjetive;

    double weight1 = 0.3d;
    double weight2 = 0.2d;
    double weight3 = 0.2d;
    double weight4 = 0.3d;

    public PSO_FitnessFunction(List<Cloudlet> clouletList, List<PowerVm> vmList, List<PowerHost> hostList){
        this.clouletList = clouletList;
        this.vmList = vmList;
        this.hostList = hostList;

        hostUtilization = new double[hostList.size()];
        hostTurnAroundTime = new double[hostList.size()];
        vmTurnAroundTime = new double[vmList.size()];
        vmUtilization = new double[vmList.size()]; 
        vmCloudletsMap = new HashMap<>();

        setMaximize(true);
    }

    public double evaluate(double[] position) {

        /**
         * CLEAN ARRAYS in this method invocation
         */
        for(PowerHost host : hostList){
            hostUtilization[host.getId()] = 0.0d;
            hostTurnAroundTime[host.getId()] = 0.0d;
        }

        for(PowerVm vm : vmList){
            vmTurnAroundTime[vm.getId()] = 0.0d;
            vmUtilization[vm.getId()] = 0.0d;
        }

        //asociate vms in this Particle position (in this solution) with the cloudlets
        vmCloudletsMap.clear();
        for(int i=0; i<position.length; i++) {
            if(!vmCloudletsMap.containsKey((int)position[i])) {
                List<Cloudlet> cloudlets = new ArrayList<Cloudlet>();
                cloudlets.add(clouletList.get(i));
                vmCloudletsMap.put((int)position[i], cloudlets);
            }
            else
                vmCloudletsMap.get((int)position[i]).add(clouletList.get(i));
        }

        /**
         * NUMBER OF HOSTS AND VMS in the position array
         * Host is obtained from the vm, because each vm was asociated with the host, according to the allocation policy
         */

        int numberOfHosts = 0;
        int numberOfVms = 0;
        Set<Integer> hostIds = new HashSet<Integer>();
        Set<Integer> vmIds = new HashSet<Integer>();
        for(int i=0; i<position.length; i++) {
            for(Vm vm : vmList) {
                if(vm.getId()==(int)position[i]){
                    hostIds.add(vm.getHost().getId());
                    vmIds.add(vm.getId());
                }
            }
        }
        numberOfHosts = hostIds.size();
        numberOfVms = vmIds.size();


/**
 *      HOST TURNAROUND TIME (total execution time of a host in the simulation considering the cloudlets it has to process if no SLA occurs)
 *      Estimated by mips: total cloudlets Length or size in Millions Instructions (MI) / total vm MIPS
 *           [mips refers to The total mips capacity of the PE of the VMs
 *              Pe (Processing Element) class represents a CPU core of a physical machine (PM), 
 *              defined in terms of Millions Instructions Per Second (MIPS) rating]
 *              see org.cloudbus.cloudsim.provisioners.PeProvisioner.Pe.java
**/

        //For each host we are going to calculate the execution time using the cloudlets mips it has to process
        for(PowerVm vm : vmList){
            //FOR EACH CLOUDLET the host has to process
            for(int i=0; i< position.length; i++){
                if((int)position[i]==vm.getId())
                    hostTurnAroundTime[vm.getHost().getId()]+= clouletList.get(i).getCloudletLength();
            }
        } 

        //consider the host capacity
        for(PowerHost host : hostList)
            hostTurnAroundTime[host.getId()] = hostTurnAroundTime[host.getId()] / host.getTotalMips();

        //MAX HOST TURNAROUND TIME 
        double sumAllCloudlets = ((double)Constants.CLOUDLET_LENGTH*(double)clouletList.size());
        double maxHostTurnAroundTime = sumAllCloudlets /((double)Constants.HOST_MIPS[0]*2.0d); 

/**
 *      VM TURNAROUND TIME (total execution time of a vm in the simulation considering the cloudlets it has to process)
 *      Estimated by mips: total cloudlets Length or size in Millions Instructions (MI) / total vm MIPS
 *           [mips refers to The total mips capacity of the PE of the VMs
 *              Pe (Processing Element) class represents a CPU core of a physical machine (PM), 
 *              defined in terms of Millions Instructions Per Second (MIPS) rating]
 *              see org.cloudbus.cloudsim.provisioners.PeProvisioner.Pe.java
**/

        //For each vm we are going to calculate the execution time using the cloudlets mips it has to process
        for(PowerVm vm : vmList){
            //FOR EACH CLOUDLET the vm has to process
            for(int i=0; i< position.length; i++){
                if((int)position[i]==vm.getId())
                    vmTurnAroundTime[vm.getId()]+= clouletList.get(i).getCloudletLength();
            }
            //consider the vm capacity
            vmTurnAroundTime[vm.getId()] = vmTurnAroundTime[vm.getId()] / vm.getMips() * (double)vm.getNumberOfPes();
        } 


/**
 *      VMS UTILIZATION In this Particle position (in this solution)
 *      Estimated by the sum of estimated cpu utilization of cloudlets
**/ 

        for(PowerVm vm : vmList){
            if(vmCloudletsMap.containsKey(vm.getId())){
                for (Cloudlet cloudlet : vmCloudletsMap.get(vm.getId())) {

                    vmUtilization[vm.getId()] += cloudlet.getUtilizationOfCpuEstimation();
                }
                
            }
        }

/**
 *      HOSTS UTILIZATION In this Particle position (in this solution)
 *      Estimated by the total of vm mips the host has to process in each cloudlet
 *      In other words, estimated by the utilization ratio of the host that considers the utilization ratio of the vms in each
**/        
        for (PowerHost host : hostList) {
            if(hostIds.contains(host.getId())){
                double vmsMIPS = 0.0d;
                for (Vm vm : host.getVmList()) {
                    if(vmCloudletsMap.containsKey(vm.getId())){

                        vmsMIPS += (vm.getMips() * (double)vm.getNumberOfPes() * (vmUtilization[vm.getId()]>1?1:vmUtilization[vm.getId()]));
                    }
                }
                hostUtilization[host.getId()] = vmsMIPS / (double) host.getTotalMips();
            }
        }

   
/**
 *      NUMBER OF HOSTS WITH OVER UTILIZATION
 *      Estimated count the number of host over UTILIZATION_THRESHOLD
**/  

        int numberHostOverUtilized = 0;
        for(PowerHost host : hostList){ 
            if(hostIds.contains(host.getId())){
                if(hostUtilization[host.getId()]>Constants.UTILIZATION_THRESHOLD)
                    numberHostOverUtilized++;
            }
        }

        double hostSLA = (double)numberHostOverUtilized / (double)numberOfHosts;

/**
 *      NUMBER OF VMs WITH OVER UTILIZATION
 *      Estimated count the number of vms over CPU > 100%
**/  

        int numberVmOverUtilized = 0;
        for(PowerVm vm : vmList){ 
            if(vmIds.contains(vm.getId())){
                if(vmUtilization[vm.getId()]>1)
                    numberVmOverUtilized++;
            }
        }


        double vmSLA = (double)numberVmOverUtilized / (double)numberOfVms;

/**
 *      CONSOLIDATION INDICATOR
 *      TOTAL UTILIZATION HOSTS MIPS In this Particle position (in this solution) (Estimated Resource Utilization)
 *      We want to minimize the number of available mips in the datacenter.
 *      This sum all the available mips of only the hosts of the especific solucion/allocation (particle position)
**/ 

        
        double hostsUtiliMips = 0.0d;
        double hostsTotalMips = 0.0d;
        for(PowerHost host : hostList){
            if(hostIds.contains(host.getId())){ //considering only hosts in the this especific solucion/allocation (particle position)
                hostsUtiliMips += (hostUtilization[host.getId()]>1?1:hostUtilization[host.getId()])*(double)host.getTotalMips();
                hostsTotalMips += host.getTotalMips();
            }
        }

        double consolidation = hostsUtiliMips/hostsTotalMips;

/**
 *      ENERGY In this Particle position (in this solution)
 *      Estimated by the estimated host utilization
**/ 
        double maxHostPower = 0.0d;
        for(PowerHost host : hostList)
            maxHostPower = Math.max(maxHostPower, host.getPower(1));

        double totalDatacenterPowerConsumption=0.0d;
        if(Constants.POWER_CONSUMPTION_ESTIMATED_BY_HTT){
            for(PowerHost host : hostList){
                if(hostIds.contains(host.getId())){ //considering only hosts in the this especific solucion/allocation (particle position)
                        totalDatacenterPowerConsumption += ((hostUtilization[host.getId()]>1?1:hostUtilization[host.getId()])*hostTurnAroundTime[host.getId()]); 
                }
            }

            //normalize: make the value comparable by changing the value from 0 to 1
            double worstDatacenterPowerConsumption = 0.0d;

            //worstDatacenterPowerConsumption is an estimation. what is higher? one host processing all tasks or all hosts processing all the tasks?
            worstDatacenterPowerConsumption = 
                Math.max(maxHostPower*maxHostTurnAroundTime,
                    maxHostPower*(double)hostList.size()*((double)Constants.CLOUDLET_LENGTH)/((double)Constants.HOST_MIPS[0])*2.0d);
            totalDatacenterPowerConsumption = totalDatacenterPowerConsumption / worstDatacenterPowerConsumption;
        }
        else{

            for(PowerHost host : hostList){
                if(hostIds.contains(host.getId())){ //considering only hosts in the this especific solucion/allocation (particle position)
                        totalDatacenterPowerConsumption += (host.getPower((hostUtilization[host.getId()]>1?1:hostUtilization[host.getId()]))); 
                }
            }
            totalDatacenterPowerConsumption = totalDatacenterPowerConsumption / (maxHostPower * (double)hostList.size());
        }


/**
 *      MAKESPAN
 *      Estimated by the max vm turn aroundTime time, since all vms start at the same time
**/ 
        double makespan = 0.0d;
        for(int i=0; i< vmList.size(); i++){
            makespan = Math.max(makespan, vmTurnAroundTime[i]);
        }

        //normalize: make the value comparable by changing the value from 0 to 1
        double totalClouletsMIPS = 0.0d;
        double minVmMips = Double.MAX_VALUE;
        for(int i=0; i< clouletList.size(); i++)
            totalClouletsMIPS += clouletList.get(i).getCloudletLength();
        
        for(int i=0; i< vmList.size(); i++) 
            minVmMips = Math.min(minVmMips, vmList.get(i).getMips() * (double)vmList.get(i).getNumberOfPes());

        //worst case is when the vm with less mips has to process all the cloudlets
        double maxPosibleVmExcecutionTime = totalClouletsMIPS / minVmMips; 
        makespan = makespan / maxPosibleVmExcecutionTime;

/**
 *      MIGRATION COST
 *      Estimated by the ram usage for each vm
**/ 
        //For each vm in the solution we are going to sum ram
        double ram = 0.0d;
        for(PowerVm vm : vmList){
            if(vmIds.contains(vm.getId())){
                ram += vm.getRam();
            }
        } 

        //normalize: make the value comparable by changing the value from 0 to 1
        double migrationCost = ram;
        double maxMigrationCost = Constants.VM_RAM[1] * (double)vmList.size();
        migrationCost = migrationCost / maxMigrationCost;


/**
 *      LOAD BALANCING
 *      Estimated by the vm execution time variance
**/ 

        //desbalancing degree calculated as the variance of vmTurnAroundTime
        double desbalancing = calcularDesviacionEstandar(normalizarDatos(hostTurnAroundTime,0,maxHostTurnAroundTime)); //vmExecutionTime
        desbalancing *= 2; //desviacion estandar goes from 0 to 0.5

        // double hostBalancingDegree = (double)numberOfHosts / (double)hostList.size();
        // double vmsBalancingDegree = (double)numberOfVms / (double)vmList.size();
        // desbalancing = 0.19*(1-hostBalancingDegree) + 0.19*(1-vmsBalancingDegree) + 0.62*desbalancing;


        //desbalancingDegree normalized
        //double maxVariance = maxHostTurnAroundTime*maxHostTurnAroundTime/4;
        //desbalancingDegree = desbalancingDegree/maxVariance;
        //desbalancingDegree = desbalancingDegree/maxHostTurnAroundTime;
        
        //desbalancingDegree normalized
        // double maxVariance = maxPosibleVmExcecutionTime*maxPosibleVmExcecutionTime/4;
        // desbalancingDegree = desbalancingDegree/maxVariance;
        // double balancingDegree = 1 - desbalancingDegree;

        // double weight1 = 0.3d;
        // double weight2 = 0.2d;
        // double weight3 = 0.1d;
        // double weight4 = 0.2d; 
        // double weight5 = 0.3d;

        //objetive function
        
        powerConsumptionObjetive = (0.4*totalDatacenterPowerConsumption + 0.2*migrationCost + 0.4*(1.0d-consolidation));
        makespanObjetive = makespan;
        desbalancingObjetive = desbalancing;
        slaObjetive = 0.5*vmSLA + 0.5*hostSLA;

        double functOutput =  1.0d/(
              (weight1 * powerConsumptionObjetive) 
            + (weight2 * makespanObjetive) 
            + (weight3 * desbalancingObjetive) 
            + (weight4 * slaObjetive));

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
                vmCapacity.put(vm.getId(), vm.getMips() * (double)vm.getNumberOfPes());
            
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
        // System.out.print("--------- evaluate ");
        // for(int i=0;i<position.length;i++) {
        //     System.out.print(position[i]+" ");
        // }
        //System.out.println(functOutput);

        return functOutput;
	}

        //cloudlet execution time = cloudlet length (total mips) / mv mips
        // for(int i=0; i< position.length; i++) 
        //     executionTime[i] = clouletList.get(i).getCloudletLength() / vmList.get((int)position[i]).getMips();


        
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

    // Método para calcular la desviación estándar
    private double calcularDesviacionEstandar(double[] cargas) {
        double media = calculateMedia(cargas);
        double sumaCuadrados = 0.0;

        // Calcular la suma de los cuadrados de las diferencias respecto a la media
        for (double carga : cargas) {
            sumaCuadrados += Math.pow(carga - media, 2);
        }

        // Dividir por n para la varianza muestral
        double varianza = sumaCuadrados / cargas.length;

        // Retornar la raíz cuadrada de la varianza para obtener la desviación estándar
        return Math.sqrt(varianza);
    }

    // Método para calcular la media de un arreglo de doubles
    public double calculateMedia(double[] numeros) {
        double suma = 0.0;
        for (double num : numeros) {
            suma += num;
        }
        return suma / numeros.length;
    }

    private static double[] normalizarDatos(double[] cargas, double min, double max) {

        double[] cargasNormalizadas = new double[cargas.length];

        // Aplicar la normalización Min-Max
        for (int i = 0; i < cargas.length; i++) {
            cargasNormalizadas[i] = (cargas[i] - min) / (max - min);
        }

        return cargasNormalizadas;
    }

    public double getWeight1() {
        return weight1;
    }

    public void setWeight1(double weight1) {
        this.weight1 = weight1;
    }

    public double getWeight2() {
        return weight2;
    }

    public void setWeight2(double weight2) {
        this.weight2 = weight2;
    }

    public double getWeight3() {
        return weight3;
    }

    public void setWeight3(double weight3) {
        this.weight3 = weight3;
    }

    public double getWeight4() {
        return weight4;
    }

    public void setWeight4(double weight4) {
        this.weight4 = weight4;
    }

    
}
