package org.cloudbus.cloudsim.examples.pso.original;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.examples.pso.Allocation;
import org.cloudbus.cloudsim.examples.pso.Constants;
import org.cloudbus.cloudsim.examples.pso.Helper;
import org.cloudbus.cloudsim.examples.pso.RandomConstants;
import org.cloudbus.cloudsim.examples.pso.RandomHelper;
import org.cloudbus.cloudsim.examples.pso.RunnerAbstract;
import org.cloudbus.cloudsim.examples.pso.discrete.Discrete_Particle;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicyMigrationStaticThresholdPSO;

import net.sourceforge.jswarm_pso.Particle;
import net.sourceforge.jswarm_pso.ParticleUpdateSimple;
import net.sourceforge.jswarm_pso.Swarm;

/**
 * The example runner for the random workload.
 * 
 * If you are using any algorithms, policies or workload included in the power package please cite
 * the following paper:
 * 
 * Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012
 * 
 * @author Anton Beloglazov
 * @since Jan 5, 2012
 */
public class RandomRunner extends RunnerAbstract {

    Swarm swarm;
    PSO_FitnessFunction fitnessFunction;

	/**
	 * @param enableOutput
	 * @param outputToFile
	 * @param inputFolder
	 * @param outputFolder
	 * @param workload
	 * @param vmAllocationPolicy
	 * @param vmSelectionPolicy
	 * @param parameter
	 */
	public RandomRunner(
			boolean enableOutput,
			boolean outputToFile,
			String inputFolder,
			String outputFolder,
			String workload,
			String vmAllocationPolicy,
			String vmSelectionPolicy,
			String parameter) {
		super(
				enableOutput,
				outputToFile,
				inputFolder,
				outputFolder,
				workload,
				vmAllocationPolicy,
				vmSelectionPolicy,
				parameter);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cloudbus.cloudsim.examples.power.RunnerAbstract#init(java.lang.String)
	 */
	@Override
	protected void init(String inputFolder) {
		try {
			CloudSim.init(1, Calendar.getInstance(), false);

			broker = Helper.createBroker();
			int brokerId = broker.getId();

			cloudletList = RandomHelper.createCloudletList(brokerId, RandomConstants.NUMBER_OF_VMS);
			vmList = Helper.createVmList(brokerId, cloudletList.size());
			hostList = Helper.createHostList(RandomConstants.NUMBER_OF_HOSTS);

		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
			System.exit(0);
		}
	}

	private void optimize(){

		/**
		 *      HOST UTILIZATION
		 *      Estimated percentage: total VMs mips / total host mips
		 *      This is a estimated value when all the vms are started (in this simulation all the vms start at the same time)
		 *      However, during the simulation this value is going to be changed depending on the finish cloudlets time or vm migrations
		**/
		for(PowerHost host : hostList){
			double vmsMIPS = 0.0d;
			for(Vm vm : host.getVmList()){
				vmsMIPS += vm.getMips() * vm.getNumberOfPes();
			}
			host.setUtilizationEstimation(vmsMIPS/(double)host.getTotalMips());
		}

		//*** Estimate power consumption from all hosts ***
		List<PowerHost> powerHostsOrderByPowerConsumption = new ArrayList<>(RandomRunner.hostList); //asc, estimated by the host utilization fixed in Constants.UTILIZATION_THRESHOLD
		//host doest have power as an attribute -only the method-, but added for power consumption estimation
		for(PowerHost h1 : powerHostsOrderByPowerConsumption){
			h1.setPowerEstimation(h1.getPower(Constants.UTILIZATION_THRESHOLD));
		}

        //initialize particles
        // PSO_Particle[] particles = new PSO_Particle[Constants.NUM_PARTICLES];
        // for(int i=0;i<Constants.NUM_PARTICLES-1;i++) {
        //     particles[i]= new PSO_Particle(cloudletList.size(),  RandomRunner.vmList.size());
        //     System.out.println(particles[i]);
        // }

		//initialize particles
        PSO_Particle[] particles = new PSO_Particle[Constants.NUM_PARTICLES];
		int cont =0;
		for (int i=1; i <=  RandomRunner.vmList.size(); i++) { //number of different vms in each particle from 1 to N 
			int subset = (int)((double)Constants.NUM_PARTICLES/ (double)RandomRunner.vmList.size());
            for (int j=0; j < subset; j++) { //number of particles we are going to create for each number of different vms

                List<Integer> idVmsList = new ArrayList<>();
                Set<Integer> uniqueIdVms = getUniqueRandomNumbers(i,  RandomRunner.vmList.size());
                List<Integer> uniqueIdVmsList = new ArrayList<>(uniqueIdVms);
                for(Cloudlet cloudlet : RandomRunner.cloudletList){ 
                    idVmsList.add(uniqueIdVmsList.get(cloudlet.getCloudletId() % uniqueIdVmsList.size()));
                }
                Collections.shuffle(idVmsList);

				double[] position = new double[RandomRunner.cloudletList.size()];
				double[] velocity = new double[RandomRunner.cloudletList.size()];

				for (int h = 0; h < RandomRunner.cloudletList.size(); h++) {
					position[h] = ((PowerVm)RandomRunner.vmList.get(idVmsList.get(h))).getId();
					velocity[h] = Math.random()*RandomRunner.vmList.size();
				}
                particles[cont++] =new PSO_Particle(RandomRunner.cloudletList.size(), position, velocity);
				System.out.println(particles[cont-1]);
            }
        }

		//For stats and analysis
		double[] maeIteracion = new double[Constants.NUM_ITERATIONS];

		//adding particles that can represent especial situations, such as the real state of a datacenter
		//particles[Constants.NUM_PARTICLES-1]= new PSO_Particle(cloudletList.size(),  RandomRunner.vmList.size() , 0);

        fitnessFunction = new PSO_FitnessFunction(cloudletList, (List<PowerVm>)(Object)(RandomRunner.vmList), RandomRunner.hostList);
        swarm = new Swarm(cloudletList.size(), new PSO_Particle(cloudletList.size(), RandomRunner.vmList.size()), fitnessFunction);
		/**
		 * better performance with the default parameters	
		 * swarm.setGlobalIncrement(Constants.SOCIAL_COEFFICIENT); 
		 * swarm.setParticleIncrement(Constants.COGNIT_COEFFICIENT); 
		 * swarm.setInertia(Constants.INERTIA_WEIGHT);
		 */
		swarm.setNumberOfParticles(Constants.NUM_PARTICLES);
        swarm.setMinPosition(0);//minimum value is the minimum value of vm id
        swarm.setMaxPosition(RandomRunner.vmList.size()-1);//maximum value of vm id
        swarm.setMaxMinVelocity(1.1);
        swarm.setParticles(particles);
        swarm.setParticleUpdate(new ParticleUpdateSimple(new PSO_Particle(cloudletList.size(),  RandomRunner.vmList.size())));
        for(int i=0;i<Constants.NUM_ITERATIONS;i++) {
            swarm.evolve();
            if(i%10 == 0) {
                System.out.println("Global best at iteration "+i+" :"+swarm.getBestFitness());
            }

			double sumMae = 0.0;
			for(Particle particle : particles){
				sumMae += particle.mae;
			}
			double promedioMae = sumMae / (double)particles.length;
			maeIteracion[i] = promedioMae;
        }
        System.out.println("The best fitness value is "+swarm.getBestFitness());
        PSO_Particle bestparticle = (PSO_Particle)swarm.getBestParticle();
        System.out.println(bestparticle.toString());

		System.out.println("********* MAE Stat **************");
		for(int i=0; i<Constants.NUM_ITERATIONS; i++) { 
			System.out.print(String.format("%.2f", maeIteracion[i]) +" ");
		}
		System.out.println("***** END Original PSO **********");
    }

	public static Set<Integer> getUniqueRandomNumbers(int n, int upperBound) {
        Set<Integer> uniqueNumbers = new HashSet<>();
        Random random = new Random();

        while (uniqueNumbers.size() < n) {
            int number = random.nextInt(upperBound); // Genera un número aleatorio entre 0 y upperBound-1
            uniqueNumbers.add(number);
        }

        return uniqueNumbers;
    }

	/**
	 * Starts the simulation.
	 * 
	 * @param experimentName the experiment name
	 * @param outputFolder the output folder
	 * @param vmAllocationPolicy the vm allocation policy
	 */
	@Override
	protected void start(String experimentName, String outputFolder, VmAllocationPolicy vmAllocationPolicy) {
		System.out.println("Starting " + experimentName);

		try {
			PowerDatacenter datacenter = (PowerDatacenter) Helper.createDatacenter(
					"Datacenter",
					PowerDatacenter.class,
					hostList,
					vmAllocationPolicy);

			datacenter.setDisableMigrations(false);

			broker.submitVmList(vmList);
			broker.submitCloudletList(cloudletList);

			/*** 
			 * Before optimization (see After optimization)
			 * scheduler may know allocation policy, so it can figure out host assignment (important for host power consideration in the scheduling process)
			 * */
			PowerVmAllocationPolicyMigrationStaticThresholdPSO vmAllocationMigrationMSPolicy = (PowerVmAllocationPolicyMigrationStaticThresholdPSO)vmAllocationPolicy;
			vmAllocationMigrationMSPolicy.setHostList(hostList);
			Set<? extends Host> excludedHosts = new HashSet<>();
			for(PowerVm vm : (List<PowerVm>)(Object)(RandomRunner.vmList)){
				PowerHost host = vmAllocationMigrationMSPolicy.findHostForVm(vm, excludedHosts);
					if(host != null){
					host.getVmList().add(vm);

					List<Double> mips = new ArrayList<Double>();
					for(int i=0; i < vm.getNumberOfPes(); i++) 
						mips.add(vm.getMips());
					host.getVmScheduler().allocatePesForVm(vm, mips);
					vm.setHost(host);
					vm.setBeingInstantiated(true);
					System.out.println(" Vm allocation in scheduling time: Vm: "+vm.getId() + " Host: " + host.getId());
				}
				else 
					throw new Exception("According to the allocation policy, all Vms cannot be allocated in the datacenter. You need to increase servers on them.");
			}

			optimize();

			/*** 
			 * After optimization (see Before optimization)
			 * Clear the hosts and vms in the datacenter
			 * */
			for(PowerVm vm : (List<PowerVm>)(Object)(RandomRunner.vmList)){
				vm.setHost(null);
			}

			for(PowerHost host : hostList){
				host.getVmList().clear();
				host.getVmScheduler().deallocatePesForAllVms();
			}

			/*** 
			 * After optimization (see Before optimization)
			 * Bind cloudlets to vms in the datacenter
			 * */
			for (Cloudlet cloudlet : cloudletList){
				Vm vm = RandomRunner.vmList.get((int)swarm.getBestParticle().getBestPosition()[cloudlet.getCloudletId()]);
				broker.bindCloudletToVm(cloudlet.getCloudletId(), vm.getId());
			}


			CloudSim.terminateSimulation(Constants.SIMULATION_LIMIT);
			double lastClock = CloudSim.startSimulation();

			List<Cloudlet> newList = broker.getCloudletReceivedList();
			Log.printLine("Received " + newList.size() + " cloudlets");

			CloudSim.stopSimulation();

			Helper.printResults(
					datacenter,
					vmList,
					lastClock,
					experimentName,
					Constants.OUTPUT_CSV,
					outputFolder);

			Helper.printCloudletList(cloudletList);

		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
			System.exit(0);
		}

		Log.printLine("Finished " + experimentName);
	}

}
