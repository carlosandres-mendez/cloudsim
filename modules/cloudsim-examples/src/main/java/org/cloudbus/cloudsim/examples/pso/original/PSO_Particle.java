package org.cloudbus.cloudsim.examples.pso.original;

import java.util.Random;

import org.cloudbus.cloudsim.examples.power.random.RandomConstants;
import org.cloudbus.cloudsim.examples.pso.PlanetLabRunner;

import net.sourceforge.jswarm_pso.Particle;

public class PSO_Particle extends Particle{

    //int NUMBER_OF_VMS = RandomConstants.NUMBER_OF_VMS;
    int NUMBER_OF_VMS = PlanetLabRunner.NUMBER_OF_VMS;

    public PSO_Particle(int dimention, double[] position, double[] velocity){
        super(dimention);
        setPosition(position);
        setVelocity(velocity);
    }

    public PSO_Particle(int numberCloudlets, int numberVms){
        super(numberCloudlets);
        double[] position = new double[numberCloudlets];
        double[] velocity = new double[numberCloudlets];

        for (int i = 0; i < numberCloudlets; i++) {
            Random randObj = new Random();
            position[i] = randObj.nextInt(numberVms);
            velocity[i] = Math.random()*(double)numberVms;
        }
        setPosition(position);
        setVelocity(velocity);
    }

    public PSO_Particle(int numberCloudlets, int numberVms, int typeParticle){
        super(numberCloudlets);
        double[] position = new double[numberCloudlets];
        double[] velocity = new double[numberCloudlets];

        for (int i = 0; i < numberCloudlets; i++) {
            position[i] = i;
            velocity[i] = i;
        }
        setPosition(position);
        setVelocity(velocity);
    }

    public String toString() {
        String output = "\n***PARTICLE POSITION***\n";
        for(int i=0;i<NUMBER_OF_VMS;i++) {
            String tasks = "";
            int number_of_tasks = 0;
            for(int j=0;j<getPosition().length;j++) {
                if( i== (int)getPosition()[j]) {
                    tasks +=(tasks.isEmpty() ? " " : " " ) + j;
                    ++number_of_tasks;
                }
            }
            // if(tasks.isEmpty())
            //     output += "NO Tasks is in VM "+ i+"\n";
            // else
            if(!tasks.isEmpty())
                output += number_of_tasks +" Tasks is in VM "+i +" Tasks id = " +tasks +"\n";
        }

        output += "\n***PARTICLE BEST POSITION***\n";
        if(getBestPosition()!=null) {
            for(int i=0;i<NUMBER_OF_VMS;i++) {
                String tasks = "";
                int number_of_tasks = 0;
                for(int j=0;j<getBestPosition().length;j++) {
                    if( i== (int)getBestPosition()[j]) {
                        tasks +=(tasks.isEmpty() ? " " : " " ) + j;
                        ++number_of_tasks;
                    }
                }
                // if(tasks.isEmpty())
                //     output += "NO Tasks is in VM "+ i+"\n";
                // else
                if(!tasks.isEmpty())
                    output += number_of_tasks +" Tasks is in VM "+i +" Tasks id = " +tasks +"\n";
            }
        }

	    return output;
    }
}
