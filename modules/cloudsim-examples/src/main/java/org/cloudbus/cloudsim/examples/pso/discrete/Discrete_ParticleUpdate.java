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

    boolean allowRepeatedCloudletsInVelocityFIFO = false; // dehavior for velocity to allow repeated cloudlets (this option needs to be studied)
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

        //if inertia 0 then velocity can be full with personal and social bests.
        //if inertia > 1 then velocity trend to keep the same. So, this variable is going to count the cloudlets updated. 
        int countUpdatedCloudlets = 0; 
        int inertia =  (int) Math.floor(((double)swarm.getDimension()) * swarm.getInertia());

        //***** Update velocity  ******/
        List<Allocation> personalPossibleCombinations = generatePossibleCombinations(swarm.getParticleIncrement(), particle.getBestPosition(), particle.getPosition());
        List<Allocation> globalPossibleCombinations = generatePossibleCombinations(swarm.getGlobalIncrement(), swarm.getBestPosition(), particle.getPosition());

        //if the option allow repeated cloudlets in th Velocity queue is enabled (this option needs to be studied)
        if(allowRepeatedCloudletsInVelocityFIFO){
            //Add the personal possible combinations to velocity  
            for (Allocation possibleCombination : personalPossibleCombinations){
                particle.getVelocity().add(possibleCombination);
                if(particle.getVelocity().size() > particle.getDimension())
                    particle.getVelocity().poll(); //if max is reach remove from velocity queue
            }
            //Add the global possible combinations to velocity
            for (Allocation possibleCombination : globalPossibleCombinations){
                particle.getVelocity().add(possibleCombination);
                if(particle.getVelocity().size() > particle.getDimension())
                    particle.getVelocity().poll(); //if max is reach remove from velocity queue
            }
        }
        else{
            //Add personal possible combinations to velocity
            for (Allocation possibleCombination : personalPossibleCombinations){
                if(countUpdatedCloudlets < inertia){
                    countUpdatedCloudlets++;

                    //remove if cloudlet exists in velocity queue
                    for (Iterator<Allocation> iter = particle.getVelocity().iterator(); iter.hasNext();){
                        Allocation allocation = iter.next();
                            if(allocation.getCloudlet().equals(possibleCombination.getCloudlet())){
                                //this is just for analyse the velocity queue
                                if(allocation.getVm().getId()!= possibleCombination.getVm().getId())
                                    swarm.cantNewVelocidad++;
                                //remove allocation
                                iter.remove();
                                break;
                            }
                    
                    }
                    //add the cloudlet allocation to the velocity queue
                    particle.getVelocity().add(possibleCombination);
                    swarm.cantAddVelocidad++;
                    if(particle.getVelocity().size() > particle.getDimension()){
                        particle.getVelocity().poll(); //if max is reach remove from velocity queue
                        swarm.cantPollVelocidad++;
                    }
                }
            }

            //Add global possible combinations to velocity
            for (Allocation possibleCombination : globalPossibleCombinations){
                if(countUpdatedCloudlets < inertia){
                    countUpdatedCloudlets++;

                    //remove if cloudlet exists in velocity queue
                    for (Iterator<Allocation> iter = particle.getVelocity().iterator(); iter.hasNext();){
                        Allocation allocation = iter.next();
                            if(allocation.getCloudlet().equals(possibleCombination.getCloudlet())){
                                //this is just for analyse the velocity queue
                                if(allocation.getVm().getId()!= possibleCombination.getVm().getId())
                                    swarm.cantNewVelocidad++;                           
                                //remove allocation
                                iter.remove();
                                break;
                            }
                    
                    }
                    //add the cloudlet allocation to the velocity queue
                    particle.getVelocity().add(possibleCombination);
                    swarm.cantAddVelocidad++;
                    if(particle.getVelocity().size() > particle.getDimension()){
                        particle.getVelocity().poll(); //if max is reach remove from velocity queue
                        swarm.cantPollVelocidad++;
                    }
                }
            }
        }

        //***** Update position  ******/
        // Update position by replacing the current position with the velocity values  
        for (Allocation positionAllocation : particle.getPosition()) {
            for (Allocation velocityAllocation : particle.getVelocity()){
                if(positionAllocation.getCloudlet().equals(velocityAllocation.getCloudlet())){
                    //this is just for analyse the velocity queue
                    if(positionAllocation.getVm().getId()!=velocityAllocation.getVm().getId())
                        swarm.cantGetVelocidad++;
                    //replace the vm and host
                    positionAllocation.setVm(velocityAllocation.getVm());
                    positionAllocation.setHost(velocityAllocation.getHost());
                }
            }
        }

        // for(Allocation alloc : particle.getVelocity())
        //     System.out.print("("+alloc.getCloudlet().getCloudletId() +"-"+ alloc.getVm().getId()+"-"+ alloc.getHost().getId()+") ");
        // System.out.println(); 
        System.out.println("Velocity size: "+particle.getVelocity().size()+" adds: "+ swarm.cantAddVelocidad+ " news: " +swarm.cantNewVelocidad+ " gets: " +swarm.cantGetVelocidad+ " polls: " +swarm.cantPollVelocidad);
    }

    private List<Allocation> generatePossibleCombinationsRandom(double randomWeight, double coefficient, List<Allocation> bestPosition, List<Allocation> xPosition){
        List<Allocation> possibleCombinations = new ArrayList<Allocation>();

        Collections.shuffle(bestPosition);

        //Random generated
        int numPossibleCombinations =  (int) Math.floor(((double)bestPosition.size()) * coefficient); 
        for(int i = 0; i < numPossibleCombinations; i++){
/**
 *      This is when we need to validate, for example in containers. We validate the new posible allocation in the position of the particle.
 * 
            // Calcular el total de MIPS de la VM
            PowerVm vm = bestPosition.get(i).getVm(); //Vm we are going to allocate the container
            double totalVmCapacity = vm.getMips() * vm.getNumberOfPes(); //total cpu vm 
            double totalMipsUsed = 0.0d; //total vm cpu already is using
            for(Allocation allocation : xPosition){
                if(allocation.getVm().getId()==vm.getId())
                    totalMipsUsed += allocation.getCloudlet().getMips();
            }
            
            // if is valid (if vm cpu already is using + the container we are trying to allocate is less or equal to total vm cpu capacity)
            if(totalMipsUsed + bestPosition.get(i).getCloudlet().getMips() <= totalVmCapacity){
                possibleCombinations.add(
                        new Allocation(bestPosition.get(i).getCloudlet(), bestPosition.get(i).getVm(), bestPosition.get(i).getHost())
                    );
            }
            else 
                continue;
**/
            possibleCombinations.add(
                        new Allocation(bestPosition.get(i).getCloudlet(), bestPosition.get(i).getVm(), bestPosition.get(i).getHost())
                    );
        }
        return possibleCombinations;
    }

    private List<Allocation> generatePossibleCombinations0(double randomWeight, double coefficient, List<Allocation> bestPosition, List<Allocation> xPosition){
        List<Allocation> possibleCombinations = new ArrayList<Allocation>();

        //Collections.shuffle(bestPosition);
        List<Allocation> xPositionShuffled = new ArrayList<>(bestPosition);
        Collections.shuffle(xPositionShuffled);

        //Random generated
        int numPossibleCombinations =  (int) Math.floor(((double)bestPosition.size()) * coefficient); 
        for(int i = 0; i < numPossibleCombinations; i++){

            for(int j=0; j< xPosition.size(); j++){

                //if vm not has the same characteristic than the vm in the best position then 
                if(xPosition.get(j).getVm().getMips()!= bestPosition.get(j).getVm().getMips()){

                    possibleCombinations.add(
                                new Allocation(xPositionShuffled.get(i).getCloudlet(), powerVmsOrderByPowerConsumption.get(i), xPositionShuffled.get(i).getHost())
                            );
                            break;
                }
            }

        }
        return possibleCombinations;
    }

    private List<Allocation> generatePossibleCombinations(double wCoefficient, List<Allocation> bestPosition, List<Allocation> xPosition){
        List<Allocation> possibleCombinations = new ArrayList<Allocation>();

        //Collections.shuffle(bestPosition);
        List<Allocation> xPositionShuffled = new ArrayList<>(xPosition);
        Collections.shuffle(xPositionShuffled);

        //We are going to generate numPossibleCombinations new combinations acording with the w coefficient
        int numPossibleCombinations =  (int) Math.floor(((double)bestPosition.size()) * wCoefficient ); 
        for(int i = 0; i < numPossibleCombinations; i++){

            numPossibleCombinationsLoop:
            for(int j=0; j< xPositionShuffled.size(); j++){
                for(int k=0; k< bestPosition.size(); k++){

                    //if vm not has the same characteristic than the vm in the best position then take the vm from the bestPosition or a vm from the vms ordered list (usign random to decide)
                    if(xPositionShuffled.get(j).getCloudlet()==bestPosition.get(k).getCloudlet() 
                        && (((PowerHost)xPositionShuffled.get(j).getVm().getHost()).getPowerEstimation() > ((PowerHost)bestPosition.get(j).getVm().getHost()).getPowerEstimation() )
                            || ((xPositionShuffled.get(j).getVm()).getMips() < (bestPosition.get(j).getVm()).getMips() )){


                        /**
                         * independent random number uniquely
                         * generated from 0-1 at every update for each individual dimension d = 1 to D
                         */
                        double random = Math.random(); 
                        PowerVm vm = random<0.5?bestPosition.get(j).getVm():powerVmsOrderByPowerConsumption.get(i);

                        possibleCombinations.add(
                                    new Allocation(xPositionShuffled.get(i).getCloudlet(), vm , xPositionShuffled.get(i).getHost())
                                );
                                break numPossibleCombinationsLoop;
                    }
                }
            }

        }
        return possibleCombinations;
    }
    

    private List<Allocation> generatePossibleCombinationsInDeveloping1(double randomWeight, double coefficient, List<Allocation> bestPosition, List<Allocation> xPosition){
        List<Allocation> possibleCombinations = new ArrayList<Allocation>();

        //buscamos evitar se tengan luego que migrar vms y las vms mas costosas en energia apagarlas lo mas pronto posible, mejor si no le damos tareas
        //si es una vm muy eficiente en uso energia y muy rapida (muchos mips)

        //detectamos mvs de xPosition cuyo servidor es sobreutilizado
        //de la lista ordenada de vms vamos buscando la que pueda albergar la vm
        //bestPosition se puede usar para buscar interseccion y agregarlas a la velocidad
        //la interseccion puede ser coincidencias en el numero de vm o en la caracteristica (si nuevas vms -con tareas- generan utilizacion similar )
        for(Allocation allocation : xPosition){

        }


        Collections.shuffle(bestPosition);

        //Random generated
        int numPossibleCombinations =  (int) Math.floor(((double)bestPosition.size()) * coefficient); 
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