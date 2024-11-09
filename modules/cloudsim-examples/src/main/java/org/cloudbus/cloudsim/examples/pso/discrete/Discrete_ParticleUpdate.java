package org.cloudbus.cloudsim.examples.pso.discrete;

import java.util.*;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.examples.pso.Allocation;
import org.cloudbus.cloudsim.examples.pso.Constants;
import org.cloudbus.cloudsim.examples.pso.Helper;
import org.cloudbus.cloudsim.lists.VmList;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;

/**
 * Particle update strategy
 * 
 * Every Swarm.evolve() itereation the following methods are called
 * 		- begin(Swarm) : Once at the begining of each iteration
 * 		- update(Swarm,Particle) : Once for each particle
 * 		- end(Swarm) : Once at the end of each iteration
 * 
 * @author carlosandres.mendez
 */
public class Discrete_ParticleUpdate {

    Discrete_PSO_Swarm swarm;
    Discrete_Particle particle;

    //*** domain problem data ***
    List<PowerHost> powerHostsOrderByPowerConsumption; //asc, estimated by the host utilization fixed in Constants.UTILIZATION_THRESHOLD
    List<PowerVm> powerVmsOrderByPowerConsumption; //asc, according with the hosts power consumption and the initial policy allocation
    List<Cloudlet> cloudlets;
    

    public Discrete_ParticleUpdate() {
        //not implemented the default dehaviour
    }
 
    public Discrete_ParticleUpdate(List<PowerHost> powerHostsOrderByPowerConsumption, List<PowerVm> powerVmsOrderByPowerConsumption, List<Cloudlet> cloudlets) {
        this.powerHostsOrderByPowerConsumption = powerHostsOrderByPowerConsumption;
        this.powerVmsOrderByPowerConsumption = powerVmsOrderByPowerConsumption;
        this.cloudlets = cloudlets;
    }

    /** Update particle's velocity and position */
    public void update(Discrete_PSO_Swarm swarm, Discrete_Particle particle) {
        this.swarm = swarm;
        this.particle = particle;

        //*** For analysis and stats  ***/
        double[] positionCopy = new double[particle.getPosition().size()];
		int i=0;
        for(Allocation allocation : particle.getPosition()){
            positionCopy[i++] = allocation.getVm().getId();
        }

        //***** Update velocity  ******/
        List<Allocation> personalPossibleCombinations = new ArrayList<>();
        List<Allocation> globalPossibleCombinations =  new ArrayList<>();

        switch(Constants.INTELLIGENT_FUNCTION){
            case Constants.RANDOM:
                if(Math.random()<0.5){
                    personalPossibleCombinations = generatePossibleCombinationsRandom(particle, particle, swarm.getParticleIncrement());
                    globalPossibleCombinations = generatePossibleCombinationsRandom(swarm.getBestParticle(), particle, swarm.getGlobalIncrement());
                }
                else{
                    globalPossibleCombinations = generatePossibleCombinationsRandom(swarm.getBestParticle(), particle, swarm.getGlobalIncrement());  
                    personalPossibleCombinations = generatePossibleCombinationsRandom(particle, particle, swarm.getParticleIncrement());
                }
                break;
            case Constants.UTILIZATION:
                if(Math.random()<0.5){
                    personalPossibleCombinations = generatePossibleCombinationsUtilization(particle, particle, swarm.getParticleIncrement());
                    globalPossibleCombinations = generatePossibleCombinationsUtilization(swarm.getBestParticle(), particle, swarm.getGlobalIncrement());
                }
                else{
                    globalPossibleCombinations = generatePossibleCombinationsUtilization(swarm.getBestParticle(), particle, swarm.getGlobalIncrement());  
                    personalPossibleCombinations = generatePossibleCombinationsUtilization(particle, particle, swarm.getParticleIncrement()); 
                }
                break;
            case Constants.MAKESPAN:
                if(Math.random()<0.5){
                    personalPossibleCombinations = generatePossibleCombinationsMakeSpan(particle, particle, swarm.getParticleIncrement());
                    globalPossibleCombinations = generatePossibleCombinationsMakeSpan(swarm.getBestParticle(), particle, swarm.getGlobalIncrement());
                }
                else{
                    globalPossibleCombinations = generatePossibleCombinationsMakeSpan(swarm.getBestParticle(), particle, swarm.getGlobalIncrement());
                    personalPossibleCombinations = generatePossibleCombinationsMakeSpan(particle, particle, swarm.getParticleIncrement());
                }
                break; 
            default:        
        }  

        //Inertia allocations
        List<Allocation> currentVelocity = new ArrayList<>(particle.getVelocity()); //copy from particle velocity
        Collections.shuffle(currentVelocity);
        currentVelocity = new ArrayList<>(currentVelocity.subList(0, (int)(((double)currentVelocity.size()) * (1-Constants.INERTIA_WEIGHT)))); //according to inertia, create a subset 

        List<Allocation> nextVelocity = new ArrayList<>(particle.getVelocity());

        //***** Next Velocity V(t+1) ******/

        // *** Weight
        Map<Integer, Allocation> velocityInertiaMap = new HashMap<Integer, Allocation>();
        // *** Personal
        Map<Integer, Allocation> velocityBestPersonalMap = new HashMap<Integer, Allocation>();
        // *** Global
        Map<Integer, Allocation> velocityBestGlobalMap = new HashMap<Integer, Allocation>();

        for(Allocation allocation : currentVelocity)
            velocityInertiaMap.put(allocation.getCloudlet().getCloudletId(), allocation);
        
        for(Allocation allocation : personalPossibleCombinations)
            velocityBestPersonalMap.put(allocation.getCloudlet().getCloudletId(), allocation);
        
        for(Allocation allocation : globalPossibleCombinations)
            velocityBestGlobalMap.put(allocation.getCloudlet().getCloudletId(), allocation);
        
        for(Cloudlet cloudlet : cloudlets){
            int numberList = 0;
            if(velocityInertiaMap.containsKey(cloudlet.getCloudletId()) && velocityBestPersonalMap.containsKey(cloudlet.getCloudletId()) 
                && velocityBestGlobalMap.containsKey(cloudlet.getCloudletId())){
                    numberList = (int)(Math.random() * 3) + 1; 
                }
            else if(velocityInertiaMap.containsKey(cloudlet.getCloudletId()) && velocityBestPersonalMap.containsKey(cloudlet.getCloudletId())){
                numberList = (int)(Math.random() * 2) + 1; 
            }
            else if(velocityBestPersonalMap.containsKey(cloudlet.getCloudletId()) && velocityBestGlobalMap.containsKey(cloudlet.getCloudletId())){
                numberList = (int)(Math.random() * 2) + 1; 
                numberList++;
            }
            else if(velocityInertiaMap.containsKey(cloudlet.getCloudletId()))
                numberList=1;

            else if(velocityBestPersonalMap.containsKey(cloudlet.getCloudletId()))
                numberList=2;

            else if(velocityBestGlobalMap.containsKey(cloudlet.getCloudletId()))
                numberList=3;

            switch(numberList){
                case 1:
                    if(nextVelocity.contains(cloudlet.getCloudletId()))
                        nextVelocity.get(cloudlet.getCloudletId()).setVm(velocityInertiaMap.get(cloudlet.getCloudletId()).getVm());
                    break;
                case 2:
                     /**
                     * R1 independent random number uniquely
                     * generated from 0-1 at every update for each individual dimension d = 1 to D
                     */
                    if(Math.random()<0.5 && nextVelocity.contains(cloudlet.getCloudletId()))
                        nextVelocity.get(cloudlet.getCloudletId()).setVm(velocityBestPersonalMap.get(cloudlet.getCloudletId()).getVm());
                    // else
                    //     nextVelocity.get(cloudlet.getCloudletId()).setVm(swarm.getPowerVms().get((int)(Math.random() * (double)(swarm.getPowerVms().size()-1)) + 1)); 
                    break;
                case 3:
                    /**
                     * R2 independent random number uniquely
                     * generated from 0-1 at every update for each individual dimension d = 1 to D
                     */
                    if(Math.random()<0.5 && nextVelocity.contains(cloudlet.getCloudletId()))
                        nextVelocity.get(cloudlet.getCloudletId()).setVm(velocityBestGlobalMap.get(cloudlet.getCloudletId()).getVm());
                    // else
                    //     nextVelocity.get(cloudlet.getCloudletId()).setVm(swarm.getPowerVms().get((int)(Math.random() * (double)(swarm.getPowerVms().size()-1)) + 1)); 
                    // break;
                default:
                    
            }
        }

        particle.setVelocity(nextVelocity);

        //***** Update position  ******/
        //Update position by replacing the current position with the velocity values  
        for (Allocation positionAllocation : particle.getPosition()) {
            for (Allocation velocityAllocation : nextVelocity){
                if(positionAllocation.getCloudlet().equals(velocityAllocation.getCloudlet())){

                    //replace the vm and host
                    positionAllocation.setVm(velocityAllocation.getVm());
                    positionAllocation.setHost(velocityAllocation.getHost());
                }
            }
        }


        //*** For analysis and stats  ***/
        double[] newPositionCopy = new double[particle.getPosition().size()];
		int j=0;
        for(Allocation allocation : particle.getPosition()){
            newPositionCopy[j++] = allocation.getVm().getId();
        }
        particle.mae = Helper.calculateMAE(positionCopy, newPositionCopy);
    }

    /**
     * **** CPU intelligent process **** 
     * @param bestPosition
     * @param xPosition
     * @param numMaxDifferences null or the maximum number of differences between the actual position and the best position to generate
     * @return
     */
    private List<Allocation> generatePossibleCombinationsMakeSpan(Discrete_Particle bestParticle, Discrete_Particle particle, Double incrementCoefficient){
        List<Allocation> possibleCombinations = new ArrayList<Allocation>();

        //Find different allocations between best position and the current position
        List<Allocation> difAllocPositiontions = new ArrayList<>(); 
        Set<Integer> changedCloulets = new HashSet<>();
        Map<Integer, Allocation> difAllocBestPositionsMap = new HashMap<>();
        for(int j=0; j< particle.getPosition().size(); j++){
            for(int k=0; k< bestParticle.getBestPosition().size(); k++){

                //if vm not has the same characteristic than the vm in the best position then take the vm from the bestPosition or a vm from the vms ordered list (usign random to decide)
                if(particle.getPosition().get(j).getCloudlet().getCloudletId()==bestParticle.getBestPosition().get(k).getCloudlet().getCloudletId() 
                    && particle.getPosition().get(j).getVm().getId()!=bestParticle.getBestPosition().get(k).getVm().getId()){
                    difAllocPositiontions.add(particle.getPosition().get(j));
                    difAllocBestPositionsMap.put(particle.getPosition().get(j).getCloudlet().getCloudletId(), bestParticle.getBestPosition().get(k));
                }
            }
        }

        //Find a limited number of differences between the actual position and the best position
        //We are going to generate numPossibleCombinations new combinations acording with the w coefficient
        int numPossibleCombinations =  (int) Math.floor(((double)particle.getPosition().size()) * incrementCoefficient ); 
        

        //select random allocations
        Collections.shuffle(difAllocPositiontions);

        //Apply any heuristic or intelligent process to change allocations that can be considered better options
        for(Allocation allocation : difAllocPositiontions){
            if(changedCloulets.size() < numPossibleCombinations){
                if(!changedCloulets.contains(allocation.getCloudlet().getCloudletId())){
                    if((((PowerHost)allocation.getVm().getHost()).getTotalMips() * ((PowerHost)allocation.getVm().getHost()).getNumberOfPes())
                        < (((PowerHost)difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm().getHost()).getTotalMips() 
                            * ((PowerHost)difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm().getHost()).getNumberOfPes()) ){

                            //The vm to change the cloudlet is going to be the same used in the best position
                            PowerVm vm = difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm();

                            possibleCombinations.add(
                                    new Allocation(
                                            allocation.getCloudlet(),
                                            vm, 
                                            (PowerHost)vm.getHost())
                            );
                            changedCloulets.add(allocation.getCloudlet().getCloudletId());
                        
                    }
                }
            }
            else 
                break;
        }

        //Apply a second or all necesary heuristics or intelligent processes if necessary
        //and complete changes in the different allocations
        for(Allocation allocation : difAllocPositiontions){
            if(changedCloulets.size() < numPossibleCombinations){
                if(!changedCloulets.contains(allocation.getCloudlet().getCloudletId())){
                    //if the utilization of the host in the best position is not lower than the utilization threshold then it is not going to be changed
                    if(allocation.getVm().getMips() < (difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm()).getMips()){

                            //The vm to change the cloudlet is going to be the same used in the best position
                            PowerVm vm = difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm();

                            possibleCombinations.add(
                                    new Allocation(
                                            allocation.getCloudlet(),
                                            vm, 
                                            (PowerHost)vm.getHost())
                            );
                            changedCloulets.add(allocation.getCloudlet().getCloudletId());
                    }
                }
            }
            else 
                break;
        }

        //finally, if there are still more changes that need to be generated, then lets make them randomly
        //or if best position is in the the current position (in this case there are no different allocations)
        // int cont=0;
        // while(changedCloulets.size() < (int)((double)numPossibleCombinations/(double)2)){

        //     Random random = new Random();
        //     int number = random.nextInt(swarm.getDimension());

        //     if(!changedCloulets.contains(number)){

        //         PowerVm vm = powerVmsOrderByPowerConsumption.get(cont);

        //         possibleCombinations.add(
        //                 new Allocation(
        //                         swarm.getCloudlets().get(number),
        //                         vm, 
        //                         (PowerHost)vm.getHost())
        //         );
        //         changedCloulets.add(number);
        //         cont++;
        //     }
        // }

        return possibleCombinations;
    }

    /**
     * **** No heuristic or intelligent process, just random seleccion ****
     * @param bestPosition
     * @param xPosition
     * @param numMaxDifferences null or the maximum number of differences between the actual position and the best position to generate
     * @return
     */
    private List<Allocation> generatePossibleCombinationsRandom(Discrete_Particle bestParticle, Discrete_Particle particle, Double incrementCoefficient){
        List<Allocation> possibleCombinations = new ArrayList<Allocation>();

        //Find different allocations between best position and the current position
        List<Allocation> difAllocPositiontions = new ArrayList<>(); 
        Set<Integer> changedCloulets = new HashSet<>();
        Map<Integer, Allocation> difAllocBestPositionsMap = new HashMap<>();
        for(int j=0; j< particle.getPosition().size(); j++){
            for(int k=0; k< bestParticle.getBestPosition().size(); k++){

                //if vm not has the same characteristic than the vm in the best position then take the vm from the bestPosition or a vm from the vms ordered list (usign random to decide)
                if(particle.getPosition().get(j).getCloudlet().getCloudletId()==bestParticle.getBestPosition().get(k).getCloudlet().getCloudletId() 
                    && particle.getPosition().get(j).getVm().getId()!=bestParticle.getBestPosition().get(k).getVm().getId()){
                    difAllocPositiontions.add(particle.getPosition().get(j));
                    difAllocBestPositionsMap.put(particle.getPosition().get(j).getCloudlet().getCloudletId(), bestParticle.getBestPosition().get(k));
                }
                    //&& (((PowerHost)xPositionShuffled.get(j).getVm().getHost()).getPowerEstimation() > ((PowerHost)bestPosition.get(j).getVm().getHost()).getPowerEstimation() )
                        /*|| ((xPositionShuffled.get(j).getVm()).getMips() < (bestPosition.get(j).getVm()).getMips() ) */
            }
        }

        //Find a limited number of differences between the actual position and the best position
        //We are going to generate numPossibleCombinations new combinations acording with the w coefficient
        int numPossibleCombinations =  (int) Math.floor(((double)particle.getPosition().size()) * incrementCoefficient ); 
        

        //select random allocations
        Collections.shuffle(difAllocPositiontions);

        //Apply any heuristic or intelligent process to change allocations that can be considered better options
        for(Allocation allocation : difAllocPositiontions){
            if(changedCloulets.size() < numPossibleCombinations){
                if(!changedCloulets.contains(allocation.getCloudlet().getCloudletId())){

                        //The vm to change the cloudlet is going to be the same used in the best position
                        PowerVm vm = difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm();

                        possibleCombinations.add(
                                new Allocation(
                                        allocation.getCloudlet(),
                                        vm, 
                                        (PowerHost)vm.getHost())
                        );
                        changedCloulets.add(allocation.getCloudlet().getCloudletId());    
                }
            }
            else 
                break;
        }

        return possibleCombinations;
    }
    
    /**
     * **** Energy intelligent process ****
     * @param bestPosition
     * @param xPosition
     * @param numMaxDifferences null or the maximum number of differences between the actual position and the best position to generate
     * @return
     */
    private List<Allocation> generatePossibleCombinationsUtilization(Discrete_Particle bestParticle, Discrete_Particle particle, Double incrementCoefficient){
        // System.out.println("--------- Position--------->");
        // for(int i=0;i<particle.getPosition().size();i++) {
        //     System.out.print(particle.getPosition().get(i).getVm().getId()+" ");
        // }

        List<Allocation> possibleCombinations = new ArrayList<Allocation>();

        //Find different allocations between best position and the current position
        List<Allocation> difAllocPositiontions = new ArrayList<>(); 
        Set<Integer> changedCloulets = new HashSet<>();
        Map<Integer, Allocation> difAllocBestPositionsMap = new HashMap<>();
        for(int j=0; j< particle.getPosition().size(); j++){
            for(int k=0; k< bestParticle.getBestPosition().size(); k++){

                //if vm not has the same characteristic than the vm in the best position then take the vm from the bestPosition or a vm from the vms ordered list (usign random to decide)
                if(particle.getPosition().get(j).getCloudlet().getCloudletId()==bestParticle.getBestPosition().get(k).getCloudlet().getCloudletId() 
                    && particle.getPosition().get(j).getVm().getId()!=bestParticle.getBestPosition().get(k).getVm().getId()){
                    difAllocPositiontions.add(particle.getPosition().get(j));
                    difAllocBestPositionsMap.put(particle.getPosition().get(j).getCloudlet().getCloudletId(), bestParticle.getBestPosition().get(k));
                }
            }
        }

        //Find a limited number of differences between the actual position and the best position
        //We are going to generate numPossibleCombinations new combinations acording with the w coefficient
        int numPossibleCombinations =  (int) Math.floor(((double)particle.getPosition().size()) * incrementCoefficient ); 
        

        //select random allocations but now lets make a selection process.
        Collections.shuffle(difAllocPositiontions);

        //Apply any heuristic or intelligent process to change allocations that can be considered better options
        for(Allocation allocation : difAllocPositiontions){
            if(changedCloulets.size() < numPossibleCombinations){
                if(!changedCloulets.contains(allocation.getCloudlet().getCloudletId())){
                    if( particle.getVmUtilization()[allocation.getVm().getId()] > Constants.VM_UTILIZATION_THRESHOLD){ //if we need to take off the cloudlet from the vm 

                        List<Allocation> prueba = new ArrayList<>(particle.getPosition());
                        Collections.shuffle(prueba);
                        
                        //if there is a vm in the same particle that fits (consolidation)
                        PowerVm vm = null;
                        double minPower = Double.MAX_VALUE;
                        for(Allocation all : prueba){
                            double vmUtilization = particle.getVmUtilization()[all.getVm().getId()];
                            double cloudletUtilizationCPU = allocation.getCloudlet().getUtilizationOfCpuEstimation();
                            if( vmUtilization + cloudletUtilizationCPU < Constants.VM_UTILIZATION_THRESHOLD){ 
                                // double utilization  = particle.getHostUtilization()[all.vm.getHost().getId()];
                                // double power = ((PowerHost)all.vm.getHost()).getPower(utilization>1?1:utilization);
                                // if(power<minPower){
                                //     minPower = power;
                                //     vm = all.getVm();
                                // }
                                vm = all.getVm();
                                break;
                            }
                        }

                        //if there is no vm that fits, lets try vms in the global best position
                        if(vm==null){ 
                            double vmUtilizationBest = particle.getVmUtilization()[(difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm().getId())];
                            double cloudletUtilizationCPU = allocation.getCloudlet().getUtilizationOfCpuEstimation();
                            if(((cloudletUtilizationCPU + vmUtilizationBest < Constants.VM_UTILIZATION_THRESHOLD))){

                                    //The vm to change the cloudlet is going to be the same used in the best position
                                    vm = difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm();

                                }
                        }

                        if(vm==null){
                            List<Allocation> prueba2 = new ArrayList<>(bestParticle.getBestPosition());
                            Collections.shuffle(prueba2);

                            for(Allocation all : prueba2){
                                if(particle.getVmUtilization()[all.vm.getId()] + allocation.getCloudlet().getUtilizationOfCpuEstimation() < Constants.VM_UTILIZATION_THRESHOLD){
                                    // double utilization  = particle.getHostUtilization()[all.vm.getHost().getId()];
                                    // double power = ((PowerHost)all.vm.getHost()).getPower(utilization>1?1:utilization);
                                    // if(power<minPower){
                                    //     minPower = power;
                                    //     vm = all.getVm();
                                    // }
                                    vm = all.getVm();
                                    break;
                                }
                            }
                        }

                        if(vm!=null){
                                allocation.setVm(vm);
                                particle.getVmUtilization()[vm.getId()] += allocation.getCloudlet().getUtilizationOfCpuEstimation();
                                particle.getHostUtilization()[vm.getHost().getId()] += allocation.getCloudlet().getUtilizationOfCpuEstimation();
                                possibleCombinations.add(
                                        new Allocation(
                                                allocation.getCloudlet(),
                                                vm, 
                                                (PowerHost)vm.getHost())
                                );
                                changedCloulets.add(allocation.getCloudlet().getCloudletId());
                        }

                        
                    }
                }
            }
            else 
                break;
        }


        //Apply any heuristic or intelligent process to change allocations that can be considered better options
        for(Allocation allocation : difAllocPositiontions){
            if(changedCloulets.size() < numPossibleCombinations){
                if(!changedCloulets.contains(allocation.getCloudlet().getCloudletId())){
                    if( particle.getVmUtilization()[allocation.getVm().getId()] > Constants.VM_UTILIZATION_THRESHOLD){ //if we need to take off the cloudlet from the vm 
                         //System.out.println("ALERTA");
                     }
                     else if((((PowerHost)allocation.getVm().getHost()).getPowerEstimation() //or vm is not overutilized but energy can improve
                                  > ((PowerHost)difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm().getHost()).getPowerEstimation())
                                  && particle.getVmUtilization()[difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm().getId()] + allocation.getCloudlet().getUtilizationOfCpuEstimation() < Constants.VM_UTILIZATION_THRESHOLD){
                        
                         //The vm to change the cloudlet is going to be the same used in the best position
                        PowerVm vm = difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm();



                        if(vm!=null){
                                allocation.setVm(vm);
                                particle.getVmUtilization()[vm.getId()] += allocation.getCloudlet().getUtilizationOfCpuEstimation();
                                particle.getHostUtilization()[vm.getHost().getId()] += allocation.getCloudlet().getUtilizationOfCpuEstimation();
                                possibleCombinations.add(
                                        new Allocation(
                                                allocation.getCloudlet(),
                                                vm, 
                                                (PowerHost)vm.getHost())
                                );
                                changedCloulets.add(allocation.getCloudlet().getCloudletId());
                        }

                        
                    }
                }
            }
            else 
                break;
        }

        for(Allocation allocation : difAllocPositiontions){
            if(changedCloulets.size() < numPossibleCombinations){
                if(!changedCloulets.contains(allocation.getCloudlet().getCloudletId())){
                    if( particle.getVmUtilization()[allocation.getVm().getId()] > Constants.VM_UTILIZATION_THRESHOLD){ //if we need to take off the cloudlet from the vm 
                        //System.out.println("ALERTA");
                     }
                      //or vm is not overutilized but energy can improve
                     else{
                        PowerHost hostAllocated = (PowerHost)allocation.getVm().getHost();
                        double hostUtilization = particle.getHostUtilization()[hostAllocated.getId()];
                        double energyHostAllocated = hostAllocated.getPower(hostUtilization>1?1:hostUtilization);

                        PowerHost hostAllocatedBest = (PowerHost)difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm().getHost();
                        double hostUtilizationBest = particle.getHostUtilization()[hostAllocatedBest.getId()];
                        double energyHostAllocatedBest =  hostAllocatedBest.getPower(hostUtilizationBest>1?1:hostUtilizationBest);

                        if(energyHostAllocated > energyHostAllocatedBest){
                        
                            PowerVm vm = null;


                            for(PowerVm vmOrdered :  powerVmsOrderByPowerConsumption){
                                if(particle.getVmUtilization()[difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm().getId()] + allocation.getCloudlet().getUtilizationOfCpuEstimation() < Constants.VM_UTILIZATION_THRESHOLD){
                                    vm = vmOrdered;
                                    break;
                                }
                            }


                            if(vm!=null){
                                    allocation.setVm(vm);
                                    particle.getVmUtilization()[vm.getId()] += allocation.getCloudlet().getUtilizationOfCpuEstimation();
                                    particle.getHostUtilization()[vm.getHost().getId()] += allocation.getCloudlet().getUtilizationOfCpuEstimation();
                                    possibleCombinations.add(
                                            new Allocation(
                                                    allocation.getCloudlet(),
                                                    vm, 
                                                    (PowerHost)vm.getHost())
                                    );
                                    changedCloulets.add(allocation.getCloudlet().getCloudletId());
                            }

                        }
                    }
                }
            }
            else 
                break;
        }



        //Apply any heuristic or intelligent process to change allocations that can be considered better options
        for(Allocation allocation : difAllocPositiontions){
            if(changedCloulets.size() < numPossibleCombinations){
                if(!changedCloulets.contains(allocation.getCloudlet().getCloudletId())){
                    //if( particle.getVmUtilization()[allocation.getVm().getId()] > Constants.VM_UTILIZATION_THRESHOLD){ //if we need to take off the cloudlet from the vm 
                        
                    // List<Allocation> prueba = new ArrayList<>(particle.getPosition());
                    // Collections.shuffle(prueba);

                        //if there is a vm in the same particle that fits (consolidation)
                        PowerVm vm = null;
                        double minPower = Double.MAX_VALUE;
                        for(Allocation all : particle.getPosition()){
                            double vmUtilization = particle.getVmUtilization()[all.getVm().getId()];
                            double cloudletUtilizationCPU = allocation.getCloudlet().getUtilizationOfCpuEstimation();
                            if( vmUtilization + cloudletUtilizationCPU < Constants.VM_UTILIZATION_THRESHOLD){ 
                                // double utilization  = particle.getHostUtilization()[all.vm.getHost().getId()];
                                // double power = ((PowerHost)all.vm.getHost()).getPower(utilization>1?1:utilization);
                                // if(power<minPower){
                                //     minPower = power;
                                //     vm = all.getVm();
                                // }
                                vm = all.getVm();
                                break;
                            }
                        }

                        //if there is no vm that fits, lets try vms in the global best position
                        if(vm==null){ 
                            double vmUtilizationBest = particle.getVmUtilization()[(difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm().getId())];
                            double cloudletUtilizationCPU = allocation.getCloudlet().getUtilizationOfCpuEstimation();
                            if(((cloudletUtilizationCPU + vmUtilizationBest < Constants.VM_UTILIZATION_THRESHOLD))){

                                    //The vm to change the cloudlet is going to be the same used in the best position
                                    vm = difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm();

                                }
                        }

                        if(vm==null){
                            // List<Allocation> prueba2 = new ArrayList<>(bestParticle.getBestPosition());
                            // Collections.shuffle(prueba2);

                            for(Allocation all : bestParticle.getBestPosition()){
                                if(particle.getVmUtilization()[all.vm.getId()] + allocation.getCloudlet().getUtilizationOfCpuEstimation() < Constants.VM_UTILIZATION_THRESHOLD){
                                    // double utilization  = particle.getHostUtilization()[all.vm.getHost().getId()];
                                    // double power = ((PowerHost)all.vm.getHost()).getPower(utilization>1?1:utilization);
                                    // if(power<minPower){
                                    //     minPower = power;
                                    //     vm = all.getVm();
                                    // }
                                    vm = all.getVm();
                                    break;
                                }
                            }
                        }

                        if(vm!=null){
                                allocation.setVm(vm);
                                particle.getVmUtilization()[vm.getId()] += allocation.getCloudlet().getUtilizationOfCpuEstimation();
                                particle.getHostUtilization()[vm.getHost().getId()] += allocation.getCloudlet().getUtilizationOfCpuEstimation();
                                possibleCombinations.add(
                                        new Allocation(
                                                allocation.getCloudlet(),
                                                vm, 
                                                (PowerHost)vm.getHost())
                                );
                                changedCloulets.add(allocation.getCloudlet().getCloudletId());
                        }

                        
                    //}
                }
            }
            else 
                break;
        }



        // System.out.print("\nBEst> ");
        // for(int i=0;i<bestParticle.getBestPosition().size();i++) {
        //     System.out.print(bestParticle.getBestPosition().get(i).getVm().getId()+" ");
        // }
        // System.out.println();
        // System.out.print("New P>");
        // for(int i=0;i<particle.getPosition().size();i++) {
        //     System.out.print(particle.getPosition().get(i).getVm().getId()+" ");
        // }
        // System.out.println(" diffs> "+difAllocPositiontions.size() + "  " +changedCloulets.size());


        //Apply any heuristic or intelligent process to change allocations that can be considered better options
        // for(Allocation allocation : difAllocPositiontions){
        //     if(changedCloulets.size() < numPossibleCombinations){
        //         if(!changedCloulets.contains(allocation.getCloudlet().getCloudletId())){
        //             if( particle.getVmUtilization()[allocation.getVm().getId()] > Constants.VM_UTILIZATION_THRESHOLD &&
        //                 ((allocation.getCloudlet().getUtilizationOfCpuEstimation()
        //                 + particle.getVmUtilization()[(difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm().getId())] < Constants.VM_UTILIZATION_THRESHOLD))
        //                 && (((PowerHost)allocation.getVm().getHost()).getPowerEstimation() 
        //                 >= ((PowerHost)difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm().getHost()).getPowerEstimation()) ){

        //                     //The vm to change the cloudlet is going to be the same used in the best position
        //                     PowerVm vm = difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm();
        //                     allocation.setVm(vm);
        //                     // possibleCombinations.add(
        //                     //         new Allocation(
        //                     //                 allocation.getCloudlet(),
        //                     //                 vm, 
        //                     //                 (PowerHost)vm.getHost())
        //                     // );
        //                     changedCloulets.add(allocation.getCloudlet().getCloudletId());
                        
        //             }
        //         }
        //     }
        //     else 
        //         break;
        // }

        //Apply any heuristic or intelligent process to change allocations that can be considered better options
        // for(Allocation allocation : difAllocPositiontions){
        //     if(changedCloulets.size() < numPossibleCombinations){
        //         if(!changedCloulets.contains(allocation.getCloudlet().getCloudletId())){
        //             if( particle.getVmUtilization()[allocation.getVm().getId()] > Constants.VM_UTILIZATION_THRESHOLD &&
        //             (allocation.getCloudlet().getUtilizationOfCpuEstimation()
        //                 + particle.getVmUtilization()[(difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm().getId())] < Constants.VM_UTILIZATION_THRESHOLD)){

        //                     //The vm to change the cloudlet is going to be the same used in the best position
        //                     PowerVm vm = difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm();
        //                     allocation.setVm(vm);
        //                     // possibleCombinations.add(
        //                     //         new Allocation(
        //                     //                 allocation.getCloudlet(),
        //                     //                 vm, 
        //                     //                 (PowerHost)vm.getHost())
        //                     // );
        //                     changedCloulets.add(allocation.getCloudlet().getCloudletId());
                        
        //             }
        //         }
        //     }
        //     else 
        //         break;
        // }

        //Apply any heuristic or intelligent process to change allocations that can be considered better options
        // for(Allocation allocation : difAllocPositiontions){
        //     if(changedCloulets.size() < numPossibleCombinations){
        //         if(!changedCloulets.contains(allocation.getCloudlet().getCloudletId())){
        //             if( particle.getVmUtilization()[allocation.getVm().getId()] > Constants.VM_UTILIZATION_THRESHOLD &&
        //             (((PowerHost)allocation.getVm().getHost()).getPowerEstimation() 
        //             > ((PowerHost)difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm().getHost()).getPowerEstimation())){

        //                     //The vm to change the cloudlet is going to be the same used in the best position
        //                     PowerVm vm = difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm();
        //                     allocation.setVm(vm);
        //                     // possibleCombinations.add(
        //                     //         new Allocation(
        //                     //                 allocation.getCloudlet(),
        //                     //                 vm, 
        //                     //                 (PowerHost)vm.getHost())
        //                     // );
        //                     changedCloulets.add(allocation.getCloudlet().getCloudletId());
                        
        //             }
        //         }
        //     }
        //     else 
        //         break;
        // }

        //Apply a second or all necesary heuristics or intelligent processes if necessary
        //and complete changes in the different allocations
        // for(Allocation allocation : difAllocPositiontions){
        //     if(changedCloulets.size() < numPossibleCombinations){
        //         if(!changedCloulets.contains(allocation.getCloudlet().getCloudletId())){
        //             //if the utilization of the host in the best position is not lower than the utilization threshold then it is not going to be changed
        //             if(((PowerHost)allocation.getVm().getHost()).getUtilizationEstimation() > Constants.UTILIZATION_THRESHOLD
        //                 && ((PowerHost)difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm().getHost()).getUtilizationEstimation() < Constants.UTILIZATION_THRESHOLD){

        //                     //The vm to change the cloudlet is going to be the same used in the best position
        //                     PowerVm vm = difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm();

        //                     possibleCombinations.add(
        //                             new Allocation(
        //                                     allocation.getCloudlet(),
        //                                     vm, 
        //                                     (PowerHost)vm.getHost())
        //                     );
        //                     changedCloulets.add(allocation.getCloudlet().getCloudletId());
        //             }
        //         }
        //     }
        //     else 
        //         break;
        // }

        //Apply any heuristic or intelligent process to change allocations that can be considered better options
        // for(Allocation allocation : difAllocPositiontions){
        //     if(changedCloulets.size() < numPossibleCombinations){
        //         if(!changedCloulets.contains(allocation.getCloudlet().getCloudletId())){

        //                 //The vm to change the cloudlet is going to be the same used in the best position
        //                 PowerVm vm = difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm();

        //                 possibleCombinations.add(
        //                         new Allocation(
        //                                 allocation.getCloudlet(),
        //                                 vm, 
        //                                 (PowerHost)vm.getHost())
        //                 );
        //                 changedCloulets.add(allocation.getCloudlet().getCloudletId());    
        //         }
        //     }
        //     else 
        //         break;
        // }

        return possibleCombinations;
    }    

    /**
     * **** Energy intelligent process ****
     * @param bestPosition
     * @param xPosition
     * @param numMaxDifferences null or the maximum number of differences between the actual position and the best position to generate
     * @return
     */
    private List<Allocation> generatePossibleCombinationsBack(List<Allocation> bestPosition, List<Allocation> xPosition, Double incrementCoefficient){
        List<Allocation> possibleCombinations = new ArrayList<Allocation>();

        //Find different allocations between best position and the current position
        List<Allocation> difAllocPositiontions = new ArrayList<>(); 
        Set<Integer> changedCloulets = new HashSet<>();
        Map<Integer, Allocation> difAllocBestPositionsMap = new HashMap<>();
        for(int j=0; j< xPosition.size(); j++){
            for(int k=0; k< bestPosition.size(); k++){

                //if vm not has the same characteristic than the vm in the best position then take the vm from the bestPosition or a vm from the vms ordered list (usign random to decide)
                if(xPosition.get(j).getCloudlet().getCloudletId()==bestPosition.get(k).getCloudlet().getCloudletId() 
                    && xPosition.get(j).getVm().getId()!=bestPosition.get(k).getVm().getId()){
                    difAllocPositiontions.add(xPosition.get(j));
                    difAllocBestPositionsMap.put(xPosition.get(j).getCloudlet().getCloudletId(), bestPosition.get(k));
                }
                    //&& (((PowerHost)xPositionShuffled.get(j).getVm().getHost()).getPowerEstimation() > ((PowerHost)bestPosition.get(j).getVm().getHost()).getPowerEstimation() )
                        /*|| ((xPositionShuffled.get(j).getVm()).getMips() < (bestPosition.get(j).getVm()).getMips() ) */
            }
        }

        //Find a limited number of differences between the actual position and the best position
        //We are going to generate numPossibleCombinations new combinations acording with the w coefficient
        int numPossibleCombinations =  (int) Math.floor(((double)xPosition.size()) * incrementCoefficient ); 
        

        //select random allocations
        Collections.shuffle(difAllocPositiontions);

        //Apply any heuristic or intelligent process to change allocations that can be considered better options
        for(Allocation allocation : difAllocPositiontions){
            if(changedCloulets.size() < numPossibleCombinations){
                if(!changedCloulets.contains(allocation.getCloudlet().getCloudletId())){
                    if(((PowerHost)allocation.getVm().getHost()).getPowerEstimation() 
                        > ((PowerHost)difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm().getHost()).getPowerEstimation() ){

                            //The vm to change the cloudlet is going to be the same used in the best position
                            PowerVm vm = difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm();

                            possibleCombinations.add(
                                    new Allocation(
                                            allocation.getCloudlet(),
                                            vm, 
                                            (PowerHost)vm.getHost())
                            );
                            changedCloulets.add(allocation.getCloudlet().getCloudletId());
                        
                    }
                }
            }
            else 
                break;
        }

        //Apply a second or all necesary heuristics or intelligent processes if necessary
        //and complete changes in the different allocations
        for(Allocation allocation : difAllocPositiontions){
            if(changedCloulets.size() < numPossibleCombinations){
                if(!changedCloulets.contains(allocation.getCloudlet().getCloudletId())){
                    //if the utilization of the host in the best position is not lower than the utilization threshold then it is not going to be changed
                    if(((PowerHost)allocation.getVm().getHost()).getUtilizationEstimation() > Constants.UTILIZATION_THRESHOLD
                        && ((PowerHost)difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm().getHost()).getUtilizationEstimation() < Constants.UTILIZATION_THRESHOLD){

                            //The vm to change the cloudlet is going to be the same used in the best position
                            PowerVm vm = difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm();

                            possibleCombinations.add(
                                    new Allocation(
                                            allocation.getCloudlet(),
                                            vm, 
                                            (PowerHost)vm.getHost())
                            );
                            changedCloulets.add(allocation.getCloudlet().getCloudletId());
                    }
                }
            }
            else 
                break;
        }

        // finally, if there are still more changes that need to be generated, then lets make them randomly
        // or if best position is in the the current position (in this case there are no different allocations)
        int cont=0;
        while(changedCloulets.size() < (int)((double)numPossibleCombinations/(double)2)){

            Random random = new Random();
            int number = random.nextInt(swarm.getDimension());

            if(!changedCloulets.contains(number)){

                PowerVm vm = powerVmsOrderByPowerConsumption.get(cont);

                possibleCombinations.add(
                        new Allocation(
                                swarm.getCloudlets().get(number),
                                vm, 
                                (PowerHost)vm.getHost())
                );
                changedCloulets.add(number);
                cont++;
            }
        }

        return possibleCombinations;
    }

    private List<Allocation> generatePossibleCombinationsInDevelop0(List<Allocation> bestPosition, List<Allocation> xPosition, Double incrementCoefficient){
        List<Allocation> possibleCombinations = new ArrayList<Allocation>();

        Collections.shuffle(bestPosition);

        //Random generated
        int numPossibleCombinations =  (int) Math.floor(((double)bestPosition.size()) * incrementCoefficient); 
        for(int i = 0; i < numPossibleCombinations; i++){

            possibleCombinations.add(
                        new Allocation(bestPosition.get(i).getCloudlet(), powerVmsOrderByPowerConsumption.get(i), bestPosition.get(i).getHost())
                    );
        }
        return possibleCombinations;
    }
    

    private List<Allocation> generatePossibleCombinationsInDevelop(double coefficient, List<Allocation> bestPosition, List<Allocation> xPosition){
        List<Allocation> possibleCombinations = new ArrayList<Allocation>();
        Set<Integer> vmsSelected = new HashSet<Integer>();
        Set<Integer> cloudletChanged = new HashSet<Integer>();

        //buscamos evitar se tengan luego que migrar vms y las vms mas costosas en energia apagarlas lo mas pronto posible, mejor si no le damos tareas
        //si es una vm muy eficiente en uso energia y muy rapida (muchos mips)


        List<Allocation> xPositionShuffled = new ArrayList<>(xPosition);
        Collections.shuffle(xPositionShuffled);

        //Random generated
        int numPossibleCombinations =  (int) Math.floor(((double)bestPosition.size()) * coefficient); 

        //for each task in the position 
        numPossibleCombinationsLoop:
        for(int i=0; i< xPosition.size(); i++){

            //if vm not has the same characteristic than the vm in the best position then 
            if(xPosition.get(i).getVm().getMips()!= bestPosition.get(i).getVm().getMips()){

                //find a vm from the vms ordered by power consumption which has the same mips as the best position
                outerloop:
                for(PowerVm vm : powerVmsOrderByPowerConsumption){
                    if(!vmsSelected.contains(vm.getId())){ //only different vms are selected


                        /** Trying an horizontal interchange with the same position */
                        //find a vm from the vms ordered by power consumption which has the same mips as the best position
                        for(Allocation allocation : xPositionShuffled){
                            if(vm.getId()==allocation.getVm().getId() //if it is the vm suggested by the vms ordered by power consumption
                                && !allocation.equals(xPosition.get(i)) //if this allocation is not the same we are iterating 
                                && allocation.getVm().getMips() == bestPosition.get(i).getVm().getMips() //if it has the same mips as the best position
                                && !cloudletChanged.contains(allocation.getCloudlet().getCloudletId())){ //if allocation is not already in the changes list

                                //if found, add it to the list of possible changes
                                possibleCombinations.add(
                                    new Allocation(xPosition.get(i).getCloudlet(), allocation.getVm(), xPosition.get(i).getHost())
                                );

                                //add to the vms already allocated
                                vmsSelected.add(allocation.getVm().getId());
                                cloudletChanged.add(allocation.getCloudlet().getCloudletId());

                                //if the number of possible combinations already reached the maximum number then finish
                                if(i==numPossibleCombinations)
                                    break numPossibleCombinationsLoop;
                                else
                                    break outerloop;
                            }
                        }
                    }
                }
            }
        }

        return possibleCombinations;
    } 
    
    	/**
	 * Gets the over utilized hosts.
	 * 
	 * @return the over utilized hosts
	 */
	protected List<PowerHost> getOverUtilizedHosts() {
		List<PowerHost> overUtilizedHosts = new LinkedList<PowerHost>();
		for (PowerHost host : swarm.powerHosts ) {
			if (isHostOverUtilized(host)) {
				overUtilizedHosts.add(host);
			}
		}
		return overUtilizedHosts;
	}

    /**
	 * Checks if a host is over utilized, based on CPU usage.
	 * 
	 * @param host the host
	 * @return true, if the host is over utilized; false otherwise
	 */
	protected boolean isHostOverUtilized(PowerHost host) {
		double totalRequestedMips = 0;
		for (Vm vm : host.getVmList()) {
			totalRequestedMips += vm.getCurrentRequestedTotalMips();
		}
		double utilization = totalRequestedMips / host.getTotalMips();
		return utilization > Constants.UTILIZATION_THRESHOLD;
	}


}