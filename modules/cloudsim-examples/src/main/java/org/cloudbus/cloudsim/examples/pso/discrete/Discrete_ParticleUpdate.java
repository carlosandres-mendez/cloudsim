package org.cloudbus.cloudsim.examples.pso.discrete;

import java.util.*;
import java.util.concurrent.Callable;

import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.examples.pso.Allocation;
import org.cloudbus.cloudsim.examples.pso.Constants;
import org.cloudbus.cloudsim.examples.pso.Helper;
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
public class Discrete_ParticleUpdate implements Callable<Void>{

    Discrete_PSO_Swarm swarm;
    Discrete_Particle particle;

    public Discrete_ParticleUpdate() {
    }

    public Discrete_ParticleUpdate(Discrete_PSO_Swarm swarm, Discrete_Particle particle) {
        this.swarm = swarm;
        this.particle = particle;
    }
 

    public Void call(){
        update();
        return null;
    }

    /** Update particle's velocity and position */
    public void update() {

        //*** For analysis and stats  ***/
        double[] positionCopy = new double[particle.getPosition().size()];
		int i=0;
        for(Allocation allocation : particle.getPosition()){
            positionCopy[i++] = allocation.getVm().getId();
        }



        // Vms Ordered By VM mips and Host Power Consumption 
        List<Allocation> velocity = new ArrayList<>(particle.getPosition());
        Set<PowerVm> vmsInPosition = new HashSet<>();
        for(Allocation allocation : particle.getPosition())
            vmsInPosition.add(allocation.getVm());
        List<PowerVm> vmsInPositionList = new ArrayList<>(vmsInPosition);
        vmsInPositionList.sort(
                Comparator
                        .comparingDouble(
                                (PowerVm p) -> ((PowerHost) p.getHost()).getPower(Constants.UTILIZATION_THRESHOLD))
                        .thenComparing(Comparator.comparingDouble(PowerVm::getMips).reversed()));


        //balancing
        double[] partVmUtilization = particle.getVmUtilization().clone();
        for(PowerVm vm : vmsInPositionList){
            double vmUtilization = partVmUtilization[vm.getId()];
            break_vm:
            if(vmUtilization > Constants.VM_UTILIZATION_THRESHOLD){ //si la vm esta sobre utilizada
                for(Allocation allocation : velocity){ //para todas las tareas de la vm
                    if(allocation.getVm().getId()==vm.getId()){

                        boolean foundVm=false;
                        for (PowerVm vm2 : vmsInPositionList) {
                            if(vm.getId()!=vm2.getId()){
                                vmUtilization = partVmUtilization[vm2.getId()];
                                double cloudletUtilizationCPU = allocation.getCloudlet().getUtilizationOfCpuEstimation();
                                if( vmUtilization + cloudletUtilizationCPU < Constants.VM_UTILIZATION_THRESHOLD){ 
                                    partVmUtilization[allocation.getVm().getId()] -= cloudletUtilizationCPU;
                                    partVmUtilization[vm2.getId()] += cloudletUtilizationCPU;
                                    allocation.setVm(vm2);
                                    foundVm = true;
                                    break break_vm;
                                }
                            }
                        }
                        if(!foundVm){
                            for(PowerVm vm3 : swarm.getPowerVms() ){
                                if(!vmsInPosition.contains(vm3)){
                                    allocation.setVm(vm3);
                                    double cloudletUtilizationCPU = allocation.getCloudlet().getUtilizationOfCpuEstimation();
                                    partVmUtilization[vm3.getId()] += cloudletUtilizationCPU;
                                    foundVm = true;
                                    break break_vm;
                                } 
                            }
                        }
                    }
                }
            }
        }

        //consolidation
        for(Allocation allocation : velocity){ //for all cloudlets
            Iterator<PowerVm> itr = vmsInPositionList.iterator();
            while(itr.hasNext()) { // for all vms in the position
                PowerVm vm = itr.next();
                double vmUtilization = partVmUtilization[vm.getId()];
                double cloudletUtilizationCPU = allocation.getCloudlet().getUtilizationOfCpuEstimation();
                if( vmUtilization + cloudletUtilizationCPU < Constants.VM_UTILIZATION_THRESHOLD){ 
                    partVmUtilization[allocation.getVm().getId()] -= cloudletUtilizationCPU;
                    if(partVmUtilization[allocation.getVm().getId()]<=0)
                        itr.remove();
                    partVmUtilization[vm.getId()] += cloudletUtilizationCPU;
                    allocation.setVm(vm);
                    break;
                }
            } 
        }
        particle.setVelocity(velocity);



        //***** Update velocity  ******/
        List<Runnable> functions = new ArrayList<>();

        switch(Constants.INTELLIGENT_FUNCTION){
            case Constants.RANDOM:

                functions.add(()-> generatePossibleCombinationsRandom(particle.getVelocity(), particle.getPosition(), 1-Constants.INERTIA_WEIGHT));
                functions.add(()-> generatePossibleCombinationsRandom(particle.getBestPosition(), particle.getPosition(), swarm.getParticleIncrement()));
                functions.add(()-> generatePossibleCombinationsRandom(swarm.getBestParticle().getBestPosition(), particle.getPosition(), swarm.getGlobalIncrement()));

                Collections.shuffle(functions);

                for (Runnable function : functions) 
                    function.run();

                break;
            case Constants.UTILIZATION:

                functions.add(()-> generatePossibleCombinationsUtilization(particle.getVelocity(), particle.getPosition(), 1-Constants.INERTIA_WEIGHT, true));
                functions.add(()-> generatePossibleCombinationsUtilization(particle.getBestPosition(), particle.getPosition(), swarm.getParticleIncrement(), false));
                functions.add(()-> generatePossibleCombinationsUtilization(swarm.getBestParticle().getBestPosition(), particle.getPosition(), swarm.getGlobalIncrement(), false));

                Collections.shuffle(functions);

                for (Runnable function : functions) 
                     function.run();
                
                break;
            case Constants.MAKESPAN:

                functions.add(()-> generatePossibleCombinationsMakeSpan(particle.getVelocity(), particle.getPosition(), 1-Constants.INERTIA_WEIGHT));
                functions.add(()-> generatePossibleCombinationsMakeSpan(particle.getBestPosition(), particle.getPosition(), swarm.getParticleIncrement()));
                functions.add(()-> generatePossibleCombinationsMakeSpan(swarm.getBestParticle().getBestPosition(), particle.getPosition(), swarm.getGlobalIncrement()));

                Collections.shuffle(functions);

                for (Runnable function : functions) 
                    function.run();

                break; 
            default:        
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
    private List<Allocation> generatePossibleCombinationsMakeSpan(List<Allocation> bestPosition, List<Allocation> position, Double incrementCoefficient){
        List<Allocation> possibleCombinations = new ArrayList<Allocation>();

        //Find different allocations between best position and the current position
        List<Allocation> difAllocPositiontions = new ArrayList<>(); 
        Set<Integer> changedCloulets = new HashSet<>();
        Map<Integer, Allocation> difAllocBestPositionsMap = new HashMap<>();
        for(int j=0; j< position.size(); j++){
            for(int k=0; k< bestPosition.size(); k++){

                //if vm not has the same characteristic than the vm in the best position then take the vm from the bestPosition or a vm from the vms ordered list (usign random to decide)
                if(position.get(j).getCloudlet().getCloudletId()==bestPosition.get(k).getCloudlet().getCloudletId() 
                    && position.get(j).getVm().getId()!=bestPosition.get(k).getVm().getId()){
                    difAllocPositiontions.add(position.get(j));
                    difAllocBestPositionsMap.put(position.get(j).getCloudlet().getCloudletId(), bestPosition.get(k));
                }
            }
        }

        //Find a limited number of differences between the actual position and the best position
        //We are going to generate numPossibleCombinations new combinations acording with the w coefficient
        int numPossibleCombinations =  (int) Math.floor(((double)position.size()) * incrementCoefficient ); 
        

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
    private List<Allocation> generatePossibleCombinationsRandom(List<Allocation> bestPosition, List<Allocation> position, Double incrementCoefficient){
        List<Allocation> possibleCombinations = new ArrayList<Allocation>();

        //Find different allocations between best position and the current position
        List<Allocation> difAllocPositiontions = new ArrayList<>(); 
        Set<Integer> changedCloulets = new HashSet<>();
        Map<Integer, Allocation> difAllocBestPositionsMap = new HashMap<>();
        for(int j=0; j< position.size(); j++){
            for(int k=0; k< bestPosition.size(); k++){

                //if vm not has the same characteristic than the vm in the best position then take the vm from the bestPosition or a vm from the vms ordered list (usign random to decide)
                if(position.get(j).getCloudlet().getCloudletId()==bestPosition.get(k).getCloudlet().getCloudletId() 
                    && position.get(j).getVm().getId()!=bestPosition.get(k).getVm().getId()){
                    difAllocPositiontions.add(position.get(j));
                    difAllocBestPositionsMap.put(position.get(j).getCloudlet().getCloudletId(), bestPosition.get(k));
                }
                    //&& (((PowerHost)xPositionShuffled.get(j).getVm().getHost()).getPowerEstimation() > ((PowerHost)bestPosition.get(j).getVm().getHost()).getPowerEstimation() )
                        /*|| ((xPositionShuffled.get(j).getVm()).getMips() < (bestPosition.get(j).getVm()).getMips() ) */
            }
        }

        //Find a limited number of differences between the actual position and the best position
        //We are going to generate numPossibleCombinations new combinations acording with the w coefficient
        int numPossibleCombinations =  (int) Math.floor(((double)position.size()) * incrementCoefficient ); 
        

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
    private List<Allocation> generatePossibleCombinationsUtilization(List<Allocation> bestPosition,List<Allocation> position, Double incrementCoefficient, boolean isInertia){
        // System.out.println("--------- Position--------->");
        // for(int i=0;i<particle.getPosition().size();i++) {
        //     System.out.print(particle.getPosition().get(i).getVm().getId()+" ");
        // }

        List<Allocation> possibleCombinations = new ArrayList<Allocation>();

        //Find different allocations between best position and the current position
        List<Allocation> difAllocPositiontions = new ArrayList<>(); 
        Set<Integer> changedCloulets = new HashSet<>();
        Map<Integer, Allocation> difAllocBestPositionsMap = new HashMap<>();
        for(int j=0; j< position.size(); j++){
            for(int k=0; k< bestPosition.size(); k++){

                //if vm not has the same characteristic than the vm in the best position then take the vm from the bestPosition or a vm from the vms ordered list (usign random to decide)
                if(position.get(j).getCloudlet().getCloudletId()==bestPosition.get(k).getCloudlet().getCloudletId() 
                    && position.get(j).getVm().getId()!=bestPosition.get(k).getVm().getId()){
                    difAllocPositiontions.add(position.get(j));
                    difAllocBestPositionsMap.put(position.get(j).getCloudlet().getCloudletId(), bestPosition.get(k));
                }
            }
        }

        //Find a limited number of differences between the actual position and the best position
        //We are going to generate numPossibleCombinations new combinations acording with the w coefficient
        int numPossibleCombinations =  (int) Math.floor(((double)difAllocPositiontions.size()) * incrementCoefficient ); 
        

        //select random allocations but now lets make a selection process.
        Collections.shuffle(difAllocPositiontions);

        List<Allocation> randomPosition = new ArrayList<>(position);
        Collections.shuffle(randomPosition);

        List<Allocation> randomBestPosition = new ArrayList<>(bestPosition);
        Collections.shuffle(randomBestPosition);

        int cont = 0;
        //Apply any heuristic or intelligent process to change allocations that can be considered better options
        for(Allocation allocation : difAllocPositiontions){
            if(cont < numPossibleCombinations){
                if(!changedCloulets.contains(allocation.getCloudlet().getCloudletId())){
                    //if( particle.getVmUtilization()[allocation.getVm().getId()] > Constants.VM_UTILIZATION_THRESHOLD){ //if we need to take off the cloudlet from the vm 
                        
                        //if there is a vm in the same particle that fits (consolidation)
                        PowerVm vm = null; 
                        // if(Math.random()> Constants.INERTIA_WEIGHT){
                        // //if(swarm.getIteration()>5){
                        //     double minPower = Double.MAX_VALUE;
                        //     for(Allocation all : randomPosition){
                        //         if( allocation.getVm().getId()!=all.getVm().getId()){
                        //             double vmUtilization = particle.getVmUtilization()[all.getVm().getId()];
                        //             double cloudletUtilizationCPU = allocation.getCloudlet().getUtilizationOfCpuEstimation();
                        //             if( vmUtilization + cloudletUtilizationCPU < Constants.VM_UTILIZATION_THRESHOLD){ 
                        //                 // double utilization  = particle.getHostUtilization()[all.vm.getHost().getId()];
                        //                 // double power = ((PowerHost)all.vm.getHost()).getPower(utilization>1?1:utilization);
                        //                 // if(power<minPower){
                        //                 //     minPower = power;
                        //                 //     vm = all.getVm();
                        //                 // }
                        //                 vm = all.getVm();
                        //                 break;
                        //             }
                        //         }
                        //     }
                        // }

                        if(isInertia || Math.random()<0.5){

                            //if there is no vm that fits, lets try vms in the best position
                            if(vm==null){ 
                                double vmUtilizationBest = particle.getVmUtilization()[(difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm().getId())];
                                double cloudletUtilizationCPU = allocation.getCloudlet().getUtilizationOfCpuEstimation();
                                if(((cloudletUtilizationCPU + vmUtilizationBest < Constants.VM_UTILIZATION_THRESHOLD))){

                                        //The vm to change the cloudlet is going to be the same used in the best position
                                        vm = difAllocBestPositionsMap.get(allocation.getCloudlet().getCloudletId()).getVm();

                                    }
                            }

                            if(vm==null){

                                for(Allocation all : randomBestPosition){
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
                        }
                        else cont++;

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
                                cont++;
                        }

                        
                    //}
                }
            }
            else 
                break;
        }



        //Apply any heuristic or intelligent process to change allocations that can be considered better options
        // for(Allocation allocation : difAllocPositiontions){
        //     if(cont < numPossibleCombinations){
        //         if(!changedCloulets.contains(allocation.getCloudlet().getCloudletId())){
        //             //if( particle.getVmUtilization()[allocation.getVm().getId()] > Constants.VM_UTILIZATION_THRESHOLD){ //if we need to take off the cloudlet from the vm 

        //                 //if there is a vm in the same particle that fits (consolidation)
        //                 PowerVm vm = null;
        //                 double minPower = Double.MAX_VALUE;
        //                 if(Math.random()> Constants.INERTIA_WEIGHT){
        //                 //if(swarm.getIteration()>30){
        //                     for(Allocation all : randomPosition){
        //                         if(allocation.getVm().getId()!=all.getVm().getId()){
        //                             double vmUtilization = particle.getVmUtilization()[all.getVm().getId()];
        //                             double cloudletUtilizationCPU = allocation.getCloudlet().getUtilizationOfCpuEstimation();
        //                             if(vmUtilization + cloudletUtilizationCPU < Constants.VM_UTILIZATION_THRESHOLD){ 
        //                                 // double utilization  = particle.getHostUtilization()[all.vm.getHost().getId()];
        //                                 // double power = ((PowerHost)all.vm.getHost()).getPower(utilization>1?1:utilization);
        //                                 // if(power<minPower){
        //                                 //     minPower = power;
        //                                 //     vm = all.getVm();
        //                                 // }
        //                                 vm = all.getVm();
        //                                 break;
        //                             }
        //                         }
        //                     }
        //                 }



        //                 if(vm!=null){
        //                         allocation.setVm(vm);
        //                         particle.getVmUtilization()[vm.getId()] += allocation.getCloudlet().getUtilizationOfCpuEstimation();
        //                         particle.getHostUtilization()[vm.getHost().getId()] += allocation.getCloudlet().getUtilizationOfCpuEstimation();
        //                         possibleCombinations.add(
        //                                 new Allocation(
        //                                         allocation.getCloudlet(),
        //                                         vm, 
        //                                         (PowerHost)vm.getHost())
        //                         );
        //                         changedCloulets.add(allocation.getCloudlet().getCloudletId());
        //                         cont++;
        //                 }

                        
        //             //}
        //         }
        //     }
        //     else 
        //         break;
        // }

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


    public Discrete_PSO_Swarm getSwarm() {
        return swarm;
    }


    public void setSwarm(Discrete_PSO_Swarm swarm) {
        this.swarm = swarm;
    }


    public Discrete_Particle getParticle() {
        return particle;
    }


    public void setParticle(Discrete_Particle particle) {
        this.particle = particle;
    }


}