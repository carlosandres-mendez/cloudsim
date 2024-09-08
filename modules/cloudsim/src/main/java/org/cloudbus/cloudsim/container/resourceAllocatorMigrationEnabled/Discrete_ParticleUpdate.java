package org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled;

import java.util.*;

import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerHost;

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

    //A random weight r1.
    private final double WEIGHT_R1 = 0d;
    //The cognitive acceleration coefficient c1.
    private final double COGNIT_COEFFICIENT = 0d;

    //A random weight r2.
    private final double WEIGHT_R2 = 0d;
    //The social coefficient
    private final double SOCIAL_COEFFICIENT = 0d;

    
    public Discrete_ParticleUpdate() {
    }

    /** Update particle's velocity and position */
    public void update(Discrete_PSO_Swarm swarm, Discrete_Particle particle) {
        // Update velocity 
        List<Allocation> possibleMigrationsPersonal = generatePossibleMigrations(WEIGHT_R1, COGNIT_COEFFICIENT, particle.getBestPosition(), particle.getPosition());
        List<Allocation> possibleMigrationsGlobal = generatePossibleMigrations(WEIGHT_R2, SOCIAL_COEFFICIENT, swarm.getBestPosition(), particle.getPosition());

        for (Allocation possibleMigration : possibleMigrationsPersonal){
            boolean isFounded = false;
            for(Allocation allocation : particle.getVelocity()){
                if(allocation.getContainer().equals(possibleMigration.getContainer())){
                    isFounded = true;
                    allocation.setVm(possibleMigration.getVm());
                    allocation.setHost(possibleMigration.getHost());
                    break;
                }
            }
            if(!isFounded)
                particle.getVelocity().add(possibleMigration);
        }

        for (Allocation possibleMigration : possibleMigrationsGlobal){
            boolean isFounded = false;
            for(Allocation allocation : particle.getVelocity()){
                if(allocation.getContainer().equals(possibleMigration.getContainer())){
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
                if(positionAllocation.getContainer().equals(velocityAllocation.getContainer())){
                    positionAllocation.setVm(velocityAllocation.getVm());
                    positionAllocation.setHost(velocityAllocation.getHost());
                }
            }
        }
    }

    private List<Allocation> generatePossibleMigrations(double randomWeight, double coefficient, List<Allocation> bestPosition, List<Allocation> xPosition){
        List<Allocation> possibleMigrations = new ArrayList<Allocation>();


        return possibleMigrations;
    }
    
}
