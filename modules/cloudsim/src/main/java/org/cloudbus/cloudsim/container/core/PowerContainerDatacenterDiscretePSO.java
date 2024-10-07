package org.cloudbus.cloudsim.container.core;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.container.containerPlacementPolicies.Discrete_FitnessFunction;
import org.cloudbus.cloudsim.container.containerPlacementPolicies.Discrete_PSO_Swarm;
import org.cloudbus.cloudsim.container.containerPlacementPolicies.PSO_FitnessFunction;
import org.cloudbus.cloudsim.container.containerPlacementPolicies.PSO_Particle;
import org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled.Allocation;
import org.cloudbus.cloudsim.container.resourceAllocators.ContainerAllocationPolicy;
import org.cloudbus.cloudsim.container.resourceAllocators.ContainerVmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;


public class PowerContainerDatacenterDiscretePSO extends PowerContainerDatacenterCM {

    public static final int NoOfParticles = 25;
	public static final int NoOfIterations = 10;

    Discrete_PSO_Swarm swarm;

    public PowerContainerDatacenterDiscretePSO(String name, ContainerDatacenterCharacteristics characteristics,
            ContainerVmAllocationPolicy vmAllocationPolicy, ContainerAllocationPolicy containerAllocationPolicy,
            List<Storage> storageList, double schedulingInterval, String experimentName, String logAddress,
            double vmStartupDelay, double containerStartupDelay) throws Exception {
        super(name, characteristics, vmAllocationPolicy, containerAllocationPolicy, storageList, schedulingInterval,
                experimentName, logAddress, vmStartupDelay, containerStartupDelay);

    }

    private void optimize(List<Container> containerList){

        //initialize particles
        PSO_Particle[] particles = new PSO_Particle[NoOfParticles];
        for(int i=0;i<NoOfParticles;i++) {
            particles[i]= new PSO_Particle(containerList.size(),  this.getContainerVmList().size());
            System.out.println(particles[i]);
        }

        swarm = new Discrete_PSO_Swarm(new Discrete_FitnessFunction(containerList, this.getContainerVmList(), this.getHostList()), 
            this.getHostList(), this.getContainerVmList(), containerList);
        swarm.init();
        if(swarm.getParticles().size() > 0 && swarm.getParticles().get(0).getPosition().size() > 0){ //si aun hay tareas y servidores

            for (int i = 0; i < NoOfIterations; i++){
                swarm.evolve();
                if(i%10 == 0) {
                    System.out.println("Global best at iteration "+i+" :"+swarm.getBestFitness());
                }
                System.out.println("--------------------Global best------------------");
            }
            if(swarm.getBestPosition()!=null){
                for(Allocation allocation : swarm.getBestPosition()){
                    System.out.println("host" + allocation.getHost() + "vm" + allocation.getVm()+ "container"+ allocation.getContainer());
                }
            }
        }
    }

    @Override
    public void processContainerSubmit(SimEvent ev, boolean ack) {
        List<Container> containerList = (List<Container>) ev.getData();

        optimize(containerList);

        for (Container container : containerList) {
            ContainerVm vm = swarm.getBestPosition().get(container.getId()-1).getVm();
            container.setVm(vm);
            container.getCloudlet().setVmId(vm.getId());
            vm.getContainerList().add(container);
        }

        for (Container container : containerList) {
            boolean result = getContainerAllocationPolicy().allocateVmForContainer(container, getContainerVmList());
            if (ack) {
                int[] data = new int[3];
                data[1] = container.getId();
                if (result) {
                    data[2] = CloudSimTags.TRUE;
                } else {
                    data[2] = CloudSimTags.FALSE;
                }
                if (result) {
                    ContainerVm containerVm = getContainerAllocationPolicy().getContainerVm(container);
                    data[0] = containerVm.getId();
                    if(containerVm.getId() == -1){

                        Log.printConcatLine("The ContainerVM ID is not known (-1) !");
                    }
//                    Log.printConcatLine("Assigning the container#" + container.getUid() + "to VM #" + containerVm.getUid());
                    getContainerList().add(container);
                    if (container.isBeingInstantiated()) {
                        container.setBeingInstantiated(false);
                    }
                    container.updateContainerProcessing(CloudSim.clock(), getContainerAllocationPolicy().getContainerVm(container).getContainerScheduler().getAllocatedMipsForContainer(container));
                } else {
                    data[0] = -1;
                    //notAssigned.add(container);
                    Log.printLine(String.format("Couldn't find a vm to host the container #%s", container.getUid()));

                }
                send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), containerCloudSimTags.CONTAINER_CREATE_ACK, data);

                //in addition, we need to sumit the cloudlet events for process the cloudlets to the CloudResource and send ack to broker
                send(getId(), CloudSim.getMinTimeBetweenEvents(), CloudSimTags.CLOUDLET_SUBMIT, container.getCloudlet());
            }
        }

    }


    
}
