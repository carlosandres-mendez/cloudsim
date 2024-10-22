package org.cloudbus.cloudsim.examples.pso.discrete;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cloudbus.cloudsim.Log;
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
import org.cloudbus.cloudsim.examples.pso.original.PSO_FitnessFunction;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicyMigrationStaticThresholdPSO;


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

	public static final int NoOfParticles = 25;
	public static final int NoOfIterations = 10;

    Discrete_PSO_Swarm swarm;
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

        swarm = new Discrete_PSO_Swarm(new Discrete_FitnessFunction(cloudletList, (List<PowerVm>)(Object)(RandomRunner.vmList), RandomRunner.hostList), 
			RandomRunner.hostList, (List<PowerVm>)(Object)(RandomRunner.vmList), cloudletList);
        swarm.init();

		for (int i = 0; i < NoOfIterations; i++){
			swarm.evolve();
			if(i%10 == 0) {
				System.out.println("Global best at iteration "+i+" :"+swarm.getBestFitness());
			}
			System.out.println("--------------------Global best------------------");
		}
		if(swarm.getBestPosition()!=null){
			for(Allocation allocation : swarm.getBestPosition()){
				System.out.println("host" + allocation.getHost() + "vm" + allocation.getVm()+ "cloudlet"+ allocation.getCloudlet());
			}
		}
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
					System.out.println(" CANDRES "+vm.getId() + ": " + host.getId());
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
			for (Allocation allocation : swarm.getBestPosition()){
				broker.bindCloudletToVm(allocation.getCloudlet().getCloudletId(), allocation.getVm().getId());
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

		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
			System.exit(0);
		}

		Log.printLine("Finished " + experimentName);
	}

}
