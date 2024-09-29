package org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.containerSelectionPolicies.PowerContainerSelectionPolicy;
import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.container.core.ContainerVm;
import org.cloudbus.cloudsim.container.core.PowerContainerHostUtilizationHistory;
import org.cloudbus.cloudsim.container.hostSelectionPolicies.HostSelectionPolicy;
import org.cloudbus.cloudsim.container.vmSelectionPolicies.PowerContainerVmSelectionPolicy;
import org.cloudbus.cloudsim.util.ExecutionTimeMeasurer;

public class PowerContainerVmAllocationPolicyMigrationStaticThresholdMCUnderUtilizedPSO extends PowerContainerVmAllocationPolicyMigrationStaticThresholdMCUnderUtilized{
     
    List<PowerContainerHostUtilizationHistory> overUtilizedHosts;
    /**
     * Instantiates a new power vm allocation policy migration mad.
     *
     * @param hostList             the host list
     * @param vmSelectionPolicy    the vm selection policy
     * @param utilizationThreshold the utilization threshold
     */
    public PowerContainerVmAllocationPolicyMigrationStaticThresholdMCUnderUtilizedPSO(
            List<? extends ContainerHost> hostList,
            PowerContainerVmSelectionPolicy vmSelectionPolicy, PowerContainerSelectionPolicy containerSelectionPolicy,
            HostSelectionPolicy hostSelectionPolicy, double utilizationThreshold, double underUtilizationThresh,
            int numberOfVmTypes, int[] vmPes, float[] vmRam, long vmBw, long vmSize, double[] vmMips) {
        super(hostList, vmSelectionPolicy, containerSelectionPolicy, hostSelectionPolicy,utilizationThreshold, underUtilizationThresh,
        		 numberOfVmTypes, vmPes, vmRam, vmBw, vmSize, vmMips);
    }

    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends ContainerVm> vmList) {

        ExecutionTimeMeasurer.start("optimizeAllocationTotal");

        ExecutionTimeMeasurer.start("optimizeAllocationHostSelection");
        overUtilizedHosts = getOverUtilizedHosts();
        getExecutionTimeHistoryHostSelection().add(
                ExecutionTimeMeasurer.end("optimizeAllocationHostSelection"));

        printOverUtilizedHosts(overUtilizedHosts);

        saveAllocation();

        ExecutionTimeMeasurer.start("optimizeAllocationContainerSelection");
        List<? extends Container> containersToMigrate = getContainersToMigrateFromHosts(overUtilizedHosts);
        getExecutionTimeHistoryVmSelection().add(ExecutionTimeMeasurer.end("optimizeAllocationContainerSelection"));

        Log.printLine("Reallocation of Containers from the over-utilized hosts:");
        ExecutionTimeMeasurer.start("optimizeAllocationVmReallocation");
        List<Map<String, Object>> migrationMap = getPlacementForLeftContainers(containersToMigrate, new HashSet<ContainerHost>(overUtilizedHosts));


        getExecutionTimeHistoryVmReallocation().add(
                ExecutionTimeMeasurer.end("optimizeAllocationVmReallocation"));
        Log.printLine();

        System.out.println("--------------------savedAllocation------------------");
        for(Map<String, Object> savedAllocation : getSavedAllocation()){
            System.out.print(((Container)savedAllocation.get("container")).getId());
            System.out.print(((ContainerVm)savedAllocation.get("vm")).getId());
            System.out.println(((ContainerHost)savedAllocation.get("host")).getId());
        }

        if(this.getContainerHostList()!=null && this.getContainerHostList().size()>0){

            //Particle Swarm Optimization (PSO) 
            int numberOfIterations = 2;
            Discrete_PSO_Swarm swarm = new Discrete_PSO_Swarm(new Discrete_FitnessFunction(), this );
            swarm.init();
            if(swarm.getParticles().size() > 0 && swarm.getParticles().get(0).getPosition().size() > 0){ //si aun hay tareas y servidores

                for (int i = 0; i < numberOfIterations; i++){
                    swarm.evolve();
                    System.out.println("--------------------savedAllocation------------------");
                    for(Map<String, Object> savedAllocation : getSavedAllocation()){
                        System.out.print(((Container)savedAllocation.get("container")).getId());
                        System.out.print(((ContainerVm)savedAllocation.get("vm")).getId());
                        System.out.println(((ContainerHost)savedAllocation.get("host")).getId());
                    }
                }
                if(swarm.getBestPosition()!=null){
                    for(Allocation allocation : swarm.getBestPosition()){
                        if(!isAllocationInList(allocation)){
                            Map<String, Object> map = new HashMap<String, Object>();
                            map.put("host", allocation.getHost());
                            map.put("vm", allocation.getVm());
                            map.put("container", allocation.getContainer());
                            migrationMap.add(map);
                        }
                    }
                }
            }
        }

        restoreAllocation();

        getExecutionTimeHistoryTotal().add(ExecutionTimeMeasurer.end("optimizeAllocationTotal"));

        return migrationMap;


    }

    private boolean isAllocationInList(Allocation allocation){
        boolean isAllocationInList = false;
        for(Map<String, Object> savedAllocation : getSavedAllocation()){
            if(((Container)savedAllocation.get("container")).equals(allocation.getContainer())
                && ((ContainerVm)savedAllocation.get("vm")).equals(allocation.getVm())
                    && ((ContainerHost)savedAllocation.get("host")).equals(allocation.getHost()))
                isAllocationInList = true;
        }

        return isAllocationInList;
    }


}
