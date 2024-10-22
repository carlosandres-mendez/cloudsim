package org.cloudbus.cloudsim.examples.pso.original;

import java.util.Random;

import org.cloudbus.cloudsim.examples.pso.RandomConstants;

import net.sourceforge.jswarm_pso.Particle;

public class PSO_Particle extends Particle{

    public PSO_Particle(int numberCloudlets, int numberVms){
        super(numberCloudlets);
        double[] position = new double[numberCloudlets];
        double[] velocity = new double[numberCloudlets];

        for (int i = 0; i < numberCloudlets; i++) {
            Random randObj = new Random();
            position[i] = randObj.nextInt(numberVms);
            velocity[i] = Math.random()*numberVms;
        }
        setPosition(position);
        setVelocity(velocity);
    }

    public String toString() {
        String output = "";
        for(int i=0;i<RandomConstants.NUMBER_OF_VMS;i++) {
            String tasks = "";
            int number_of_tasks = 0;
            for(int j=0;j<getPosition().length;j++) {
                if( i== (int)getPosition()[j]) {
                    tasks +=(tasks.isEmpty() ? " " : " " ) + j;
                    ++number_of_tasks;
                }
            }
            if(tasks.isEmpty())
                output += "NO Tasks is in VM "+ i+"\n";
            else
                output += number_of_tasks +" Tasks is in VM "+i +" Tasks id = " +tasks +"\n";
        }
	    return output;
    }
}
