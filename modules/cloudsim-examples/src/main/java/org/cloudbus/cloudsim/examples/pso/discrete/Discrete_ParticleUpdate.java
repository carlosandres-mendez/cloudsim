package org.cloudbus.cloudsim.examples.pso.discrete;

import java.util.*;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.examples.pso.Allocation;
import org.cloudbus.cloudsim.examples.pso.Constants;
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
 
    public Discrete_ParticleUpdate(List<PowerHost> powerHostsOrderByPowerConsumption, List<PowerVm> powerVmsOrderByPowerConsumption) {
        this.powerHostsOrderByPowerConsumption = powerHostsOrderByPowerConsumption;
        this.powerVmsOrderByPowerConsumption = powerVmsOrderByPowerConsumption;
    }

    /** Update particle's velocity and position */
    public void update(Discrete_PSO_Swarm swarm, Discrete_Particle particle) {
        this.swarm = swarm;
        this.particle = particle;

        //***** Update velocity  ******/
        List<Allocation> personalPossibleCombinations = generatePossibleCombinations(particle.getBestPosition(), particle.getPosition(), swarm.getParticleIncrement());
        List<Allocation> globalPossibleCombinations = generatePossibleCombinations(swarm.getBestPosition(), particle.getPosition(), swarm.getGlobalIncrement());

        //Inertia allocations
        List<Allocation> copyVelocity = new ArrayList<>(particle.getVelocity());
        Collections.shuffle(copyVelocity);
        ArrayList<Allocation> inertiaAllocations = new ArrayList<>(copyVelocity.subList(0, Constants.INERTIA_WEIGHT));

        List<Allocation> nextVelocity = new ArrayList<>(particle.getVelocity());
 
        //***** Next Velocity V(t+1) ******/
        int w = 0; // *** Weight
        int p = 0; // *** Personal
        int g = 0; // *** Global
        while(w < inertiaAllocations.size() || p < personalPossibleCombinations.size() || g < globalPossibleCombinations.size()){

            int numberList = (int)(Math.random() * 3) + 1; 

            if (numberList==1 && w < inertiaAllocations.size()) {
                nextVelocity.get(inertiaAllocations.get(w).getCloudlet().getCloudletId()).setVm(inertiaAllocations.get(w).getVm());
                w++;
            }
            else if (numberList==2 && p < personalPossibleCombinations.size()) {

                /**
                 * R1 independent random number uniquely
                 * generated from 0-1 at every update for each individual dimension d = 1 to D
                 */
                PowerVm vm = personalPossibleCombinations.get(p).getVm();
                if(Math.random()<0.5)
                    vm = swarm.getPowerVms().get((int)(Math.random() * swarm.getDimension()));
                
                nextVelocity.get(personalPossibleCombinations.get(p).getCloudlet().getCloudletId()).setVm(vm);
                p++;
            } else if (numberList==3 && g < globalPossibleCombinations.size()) {

                /**
                 * R2 independent random number uniquely
                 * generated from 0-1 at every update for each individual dimension d = 1 to D
                 */
                PowerVm vm = globalPossibleCombinations.get(g).getVm();
                if(Math.random()<0.5)
                    vm = swarm.getPowerVms().get((int)(Math.random() * swarm.getDimension()));

                nextVelocity.get(globalPossibleCombinations.get(g).getCloudlet().getCloudletId()).setVm(vm);
                g++;
            }
        }
        particle.setVelocity(nextVelocity);

        //***** Update position  ******/
        // Update position by replacing the current position with the velocity values  
        for (Allocation positionAllocation : particle.getPosition()) {
            for (Allocation velocityAllocation : nextVelocity){
                if(positionAllocation.getCloudlet().equals(velocityAllocation.getCloudlet())){

                    //replace the vm and host
                    positionAllocation.setVm(velocityAllocation.getVm());
                    positionAllocation.setHost(velocityAllocation.getHost());
                }
            }
        }

    }

    /**
     * 
     * @param bestPosition
     * @param xPosition
     * @param numMaxDifferences null or the maximum number of differences between the actual position and the best position to generate
     * @return
     */
    private List<Allocation> generatePossibleCombinations(List<Allocation> bestPosition, List<Allocation> xPosition, Double incrementCoefficient){
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

        //finally, if there are still more changes that need to be generated, then lets make them randomly
        //or if best position is in the the current position (in this case there are no different allocations)
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

    private List<Allocation> generatePossibleCombinations_random(List<Allocation> bestPosition, List<Allocation> xPosition, Double incrementCoefficient){
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