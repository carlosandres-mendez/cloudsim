package org.cloudbus.cloudsim.container.core;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.container.containerPlacementPolicies.PSO_FitnessFunction;
import org.cloudbus.cloudsim.container.containerPlacementPolicies.PSO_Particle;
import org.cloudbus.cloudsim.container.resourceAllocators.ContainerAllocationPolicy;
import org.cloudbus.cloudsim.container.resourceAllocators.ContainerVmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;

import net.sourceforge.jswarm_pso.ParticleUpdateSimple;
import net.sourceforge.jswarm_pso.Swarm;

public class PowerContainerDatacenterPSO extends PowerContainerDatacenterCM {

    public static final int NoOfParticles = 30;
	public static final int NoOfIterations = 100;

    Swarm swarm;
    PSO_FitnessFunction fitnessFunction;

    public PowerContainerDatacenterPSO(String name, ContainerDatacenterCharacteristics characteristics,
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
        }

        fitnessFunction = new PSO_FitnessFunction(containerList, this.getContainerVmList(), this.getHostList());
        swarm = new Swarm(containerList.size(), new PSO_Particle(containerList.size(), this.getContainerVmList().size()), fitnessFunction);
        swarm.setMinPosition(0);//minimum value is the minimum value of vm id
        swarm.setMaxPosition(this.getContainerVmList().size()-1);//maximum value of vm id
        swarm.setMaxMinVelocity(1.1);
        swarm.setParticles(particles);
        swarm.setParticleUpdate(new ParticleUpdateSimple(new PSO_Particle(containerList.size(),  this.getContainerVmList().size())));
        for(int i=0;i<NoOfIterations;i++) {
            swarm.evolve();
            swarm.show(300, 300, 0, 4, false);
            if(i%10 == 0) {
                System.out.println("Global best at iteration "+i+" :"+swarm.getBestFitness());
            }
        }
        System.out.println("The best fitness value is "+swarm.getBestFitness());
        PSO_Particle bestparticle = (PSO_Particle)swarm.getBestParticle();
        System.out.println(bestparticle.toString());
    }

    @Override
    public void processContainerSubmit(SimEvent ev, boolean ack) {
        List<Container> containerList = (List<Container>) ev.getData();

        optimize(containerList);

        //allocate vm for each container according to the pso optimization
        for(int i=0; i < swarm.getBestParticle().getPosition().length; i++){
            for(ContainerVm vm : this.getContainerVmList()){
                if(vm.getId()-1==(int)swarm.getBestParticle().getPosition()[i]){
                    containerList.get(i).getCloudlet().setVmId(vm.getId());
                    containerList.get(i).setVm(vm);
                }
            }
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
