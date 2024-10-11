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

        // Update velocity 
        List<Allocation> personalPossibleMigrations = generatePossibleCombinations(WEIGHT_R1, COGNIT_COEFFICIENT, particle.getBestPosition(), particle.getPosition());
        List<Allocation> globalPossibleMigrations = generatePossibleCombinations(WEIGHT_R2, SOCIAL_COEFFICIENT, swarm.getBestPosition(), particle.getPosition());

        for (Allocation possibleMigration : personalPossibleMigrations){
            boolean isFounded = false;
            for(Allocation allocation : particle.getVelocity()){
                if(allocation.getCloudlet().equals(possibleMigration.getCloudlet())){
                    isFounded = true;
                    allocation.setVm(possibleMigration.getVm());
                    allocation.setHost(possibleMigration.getHost());
                    break;
                }
            }
            if(!isFounded)
                particle.getVelocity().add(possibleMigration);
        }

        for (Allocation possibleMigration : globalPossibleMigrations){
            boolean isFounded = false;
            for(Allocation allocation : particle.getVelocity()){
                if(allocation.getCloudlet().equals(possibleMigration.getCloudlet())){
                    isFounded = true;
                    allocation.setVm(possibleMigration.getVm());
                    allocation.setHost(possibleMigration.getHost());
                    break;
                }
            }
            if(!isFounded)
                particle.getVelocity().add(possibleMigration);
        }

        // Update position
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
 *      This is when we need to validate, for example in containers
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