package org.cloudbus.cloudsim.examples.pso;


/**
 * If you are using any algorithms, policies or workload included in the power package, please cite
 * the following paper:
 *
 * Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012
 *
 * @author Anton Beloglazov
 * @since Jan 6, 2012
 */
public class Constants extends org.cloudbus.cloudsim.examples.power.Constants{


	/*
	 * 2024 Carlos A. Mendez Rodriguez
	 * Added for PSO Original and discrete
	 */

	public final static int NUM_PARTICLES	= 105; //min equal to number of vms, because of the init poblation generation, see pso.Helper.java 
	//public final static int NUM_PARTICLES	= 10; //min equal to number of vms, because of the init poblation generation, see pso.Helper.java 

	public final static int NUM_ITERATIONS	= 50;
    
	//The inertia Weight 
	/**
	 * The authors also
		suggested using w as a dynamic value over the optimization
		process, starting with a value greater than 1.0 to encourage
		early exploration, and decreasing eventually to a value less
		than 1.0 to focus the efforts of the swarm on the best area
		found in the exploration. 
		James Kennedy && Daniel Bratton, 2007. Defining a Standard for Particle Swarm Optimization 
		
		In this discrete PSO, inertia weight should be from 0 to D (dimension size), indicating the number of the current velocity is going to remain in the next velocity */
    public final static int INERTIA_WEIGHT = 0; //Planetlab
	//public final static int INERTIA_WEIGHT = 50; //Random

    //The cognitive acceleration coefficient c1.
    public final static double COGNIT_COEFFICIENT = 0.9d;

    //The social coefficient
    public final static double SOCIAL_COEFFICIENT = 0.9d;

	//Metrics related

	//Power consumption estimated by the host turn around time
	public final static boolean POWER_CONSUMPTION_ESTIMATED_BY_HTT = false; 

	//Added by Carlos A. Mendez Rodriguez for tesis
	public final static double VM_UTILIZATION_THRESHOLD = 0.7;

}
