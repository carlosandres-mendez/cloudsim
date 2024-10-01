package org.cloudbus.cloudsim.examples.container;

import java.util.Random;

import net.sourceforge.jswarm_pso.Particle;

public class PSO_Particle extends Particle{
    public PSO_Particle(){
        super(ConstantsExamples.NUMBER_CLOUDLETS);
        double[] position = new double[ConstantsExamples.NUMBER_CLOUDLETS];
        double[] velocity = new double[ConstantsExamples.NUMBER_CLOUDLETS];

        for (int i = 0; i < ConstantsExamples.NUMBER_CLOUDLETS; i++) {
            Random randObj = new Random();
            position[i] = randObj.nextInt(ConstantsExamples.NUMBER_VMS);
            velocity[i] = Math.random()*ConstantsExamples.NUMBER_VMS;
        }
        setPosition(position);
        setVelocity(velocity);
    }

    public String toString() {
        String output = "";
        for(int i=0;i<ConstantsExamples.NUMBER_VMS;i++) {
            String tasks = "";
            int number_of_tasks = 0;
            for(int j=0;j<ConstantsExamples.NUMBER_CLOUDLETS;j++) {
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
