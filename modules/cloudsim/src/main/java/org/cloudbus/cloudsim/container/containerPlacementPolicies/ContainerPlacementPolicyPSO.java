package org.cloudbus.cloudsim.container.containerPlacementPolicies;


import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerCloudlet;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.container.core.ContainerVm;

import net.sourceforge.jswarm_pso.ParticleUpdateSimple;
import net.sourceforge.jswarm_pso.Swarm;

import java.util.List;
import java.util.Set;



/**
 * Created by carlosandres.mendez on 2024.
 * For container placement PSO policy.
 */

public class ContainerPlacementPolicyPSO extends ContainerPlacementPolicy {
	public static final int NoOfParticles = 30;
	public static final int NoOfIterations = 100;

    Swarm swarm;
    PSO_FitnessFunction fitnessFunction;

    List<ContainerCloudlet> cloudletList;
    List<ContainerVm> vmList;
    List<ContainerHost> hostList;

    public ContainerPlacementPolicyPSO(List<ContainerCloudlet> cloudletList, List<ContainerVm> vmList, List<ContainerHost> hostList){
        this.cloudletList = cloudletList;
        this.vmList = vmList;
        this.hostList = hostList;

        //initialize particles
        PSO_Particle[] particles = new PSO_Particle[NoOfParticles];
        for(int i=0;i<NoOfParticles;i++) {
            particles[i]= new PSO_Particle(cloudletList.size(), vmList.size());
        }

        fitnessFunction = new PSO_FitnessFunction(cloudletList, vmList, hostList);
        swarm = new Swarm(cloudletList.size(), new PSO_Particle(cloudletList.size(), vmList.size()), fitnessFunction);
        swarm.setMinPosition(0);//minimum value is the minimum value of vm id
        swarm.setMaxPosition(vmList.size()-1);//maximum value of vm id
        swarm.setMaxMinVelocity(1.1);
        swarm.setParticles(particles);
        swarm.setParticleUpdate(new ParticleUpdateSimple(new PSO_Particle(cloudletList.size(), vmList.size())));
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
    public ContainerVm getContainerVm(List<ContainerVm> vmList, Object obj, Set<? extends ContainerVm> excludedVmList) {
        ContainerVm containerVm = null;
        Container container = (Container)obj;
        for (ContainerVm containerVm1 : vmList) {
            if (excludedVmList.contains(containerVm1)) {
                continue;
            }
            if(containerVm1.getId() == swarm.getBestPosition()[container.getId()-1]){
                containerVm = containerVm1;
                break;
            }
        }
        return containerVm;
    }


}
