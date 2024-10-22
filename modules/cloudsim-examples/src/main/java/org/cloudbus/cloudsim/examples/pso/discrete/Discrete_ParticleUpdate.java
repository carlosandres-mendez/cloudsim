package org.cloudbus.cloudsim.examples.pso.discrete;

import java.util.*;

import org.cloudbus.cloudsim.examples.pso.Allocation;

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

    boolean allowRepeatedCloudletsInVelocityFIFO = false; // default dehavior for velocity (this option needs to be studied)
    Discrete_PSO_Swarm swarm;
    Discrete_Particle particle;

    //A random weight r1.
    private final double WEIGHT_R1 = 0.3d;
    //The cognitive acceleration coefficient c1.
    private final double COGNIT_COEFFICIENT = 0.5d;

    //A random weight r2.
    private final double WEIGHT_R2 = 0.3d;
    //The social coefficient
    private final double SOCIAL_COEFFICIENT = 0.5d;

 
    public Discrete_ParticleUpdate() {
    }

    /** Update particle's velocity and position */
    public void update(Discrete_PSO_Swarm swarm, Discrete_Particle particle) {
        this.swarm = swarm;
        this.particle = particle;

        //***** Update velocity  ******/
        List<Allocation> personalPossibleCombinations = generatePossibleCombinations(WEIGHT_R1, COGNIT_COEFFICIENT, particle.getBestPosition(), particle.getPosition());
        List<Allocation> globalPossibleCombinations = generatePossibleCombinations(WEIGHT_R2, SOCIAL_COEFFICIENT, swarm.getBestPosition(), particle.getPosition());

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
                //remove if cloudlet exists in velocity queue
                for (Iterator<Allocation> iter = particle.getVelocity().iterator(); iter.hasNext();){
                    Allocation allocation = iter.next();
                        if(allocation.getCloudlet().equals(possibleCombination.getCloudlet())){
                            iter.remove();
                            break;
                        }
                
                }
                //add the cloudlet allocation to the velocity queue
                particle.getVelocity().add(possibleCombination);
                if(particle.getVelocity().size() > particle.getDimension())
                    particle.getVelocity().poll(); //if max is reach remove from velocity queue
            }

            //Add global possible combinations to velocity
            for (Allocation possibleCombination : globalPossibleCombinations){
                //remove if cloudlet exists in velocity queue
                for (Iterator<Allocation> iter = particle.getVelocity().iterator(); iter.hasNext();){
                    Allocation allocation = iter.next();
                        if(allocation.getCloudlet().equals(possibleCombination.getCloudlet())){
                            iter.remove();
                            break;
                        }
                
                }
                //add the cloudlet allocation to the velocity queue
                particle.getVelocity().add(possibleCombination);
                if(particle.getVelocity().size() > particle.getDimension())
                    particle.getVelocity().poll(); //if max is reach remove from velocity queue
            }
        }

        //***** Update position  ******/
        // Update position by replacing the current position with the velocity values  
        for (Allocation positionAllocation : particle.getPosition()) {
            for (Allocation velocityAllocation : particle.getVelocity()){
                if(positionAllocation.getCloudlet().equals(velocityAllocation.getCloudlet())){
                    positionAllocation.setVm(velocityAllocation.getVm());
                    positionAllocation.setHost(velocityAllocation.getHost());
                }
            }
        }
    }

    private List<Allocation> generatePossibleCombinations(double randomWeight, double coefficient, List<Allocation> bestPosition, List<Allocation> xPosition){
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


}