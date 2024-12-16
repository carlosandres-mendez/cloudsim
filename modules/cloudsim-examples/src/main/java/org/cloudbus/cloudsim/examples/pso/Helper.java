package org.cloudbus.cloudsim.examples.pso;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.examples.container.ConstantsExamples;
import org.cloudbus.cloudsim.examples.power.planetlab.PlanetLabConstants;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicyMigrationAbstract;
import org.cloudbus.cloudsim.pso.PSODatacenterBroker;
import org.cloudbus.cloudsim.util.MathUtil;

/**
 * The Class Helper.
 * 
 * @author Carlos Andres Mendez Rodriguez
 */
public class Helper extends org.cloudbus.cloudsim.examples.power.Helper {

	/**
	 * Creates the broker.
	 * 
	 * @return the datacenter broker
	 */
	public static DatacenterBroker createBroker() {
		DatacenterBroker broker = null;
		try {
			broker = new PSODatacenterBroker("PSOBroker");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		return broker;
	}

	/**
	 * Prints the Cloudlet objects.
	 * 
	 * @param list list of Cloudlets
	 */
	public static void printCloudletList(List<Cloudlet> list) {
		Log.enable();
		int size = list.size();
		Cloudlet cloudlet;

		String indent = "\t";
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("Cloudlet ID" + indent + "STATUS" + indent + "Resource ID" + indent + "VM ID" + indent
				+ "Time" + indent + "Start Time" + indent + "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			Log.print(indent + cloudlet.getCloudletId());

			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
				Log.printLine(indent + "SUCCESS" + indent + indent + cloudlet.getResourceId() + indent
						+ cloudlet.getVmId() + indent + dft.format(cloudlet.getActualCPUTime()) + indent
						+ dft.format(cloudlet.getExecStartTime()) + indent + indent
						+ dft.format(cloudlet.getFinishTime()));
			} else {
				Log.printLine(indent + "FAIL" + indent + indent + cloudlet.getResourceId() + indent
						+ cloudlet.getVmId() + indent + dft.format(cloudlet.getActualCPUTime()) + indent
						+ dft.format(cloudlet.getExecStartTime()) + indent + indent
						+ dft.format(cloudlet.getFinishTime()));
			}
		}
	}

	public static double calculateMAE(double[] array1, double[] array2) {
		// Verificar que ambos arreglos tengan la misma longitud
		if (array1.length != array2.length) {
			throw new IllegalArgumentException("Los arreglos deben tener la misma longitud");
		}

		// Calcular el Error Absoluto Medio
		double sum = 0.0;
		for (int i = 0; i < array1.length; i++) {
			sum += Math.abs(array1[i] - array2[i]);
		}

		return sum / array1.length;
	}

	//* Size Equal o Bigger To Number Of Cloulets */
	public static List<List<Allocation>> createInitBigPoblation(List<Cloudlet> cloudletList, List<PowerVm> vmList,
			List<PowerHost> hostList) {

		// Number of subsets of particles, ej. {idX}, {idY,idZ}, ..., {idX..N}
		int numSubsetParticles = (int) ((double) Constants.NUM_PARTICLES / (double) vmList.size());

		List<List<Allocation>> poblation = new ArrayList<>();
		for (int i = 1; i <= vmList.size(); i++) { // number of different vms in each particle from 1 to
																	// N

			Set<Integer> lastNumbers = new HashSet<>();
			for (int j = 0; j < numSubsetParticles; j++) { // number of particles we are going to create for each number
															// of different vms

				List<Integer> idVmsList = new ArrayList<>();
				Set<Integer> uniqueIdVms = getUniqueRandomNumbers(i, vmList.size(), lastNumbers, i - 1);
				lastNumbers.addAll(uniqueIdVms);
				List<Integer> uniqueIdVmsList = new ArrayList<>(uniqueIdVms);

				for (Cloudlet cloudlet : cloudletList) {
					idVmsList.add(uniqueIdVmsList.get(cloudlet.getCloudletId() % uniqueIdVmsList.size()));
				}
				Collections.shuffle(idVmsList);

				List<Allocation> particle = new ArrayList<>();
				for (Cloudlet cloudlet : cloudletList) {
					Allocation positionAllocation = new Allocation(
							cloudlet,
							vmList.get(idVmsList.get(cloudlet.getCloudletId())),
							hostList.get((int) (Math.random() * (double) hostList.size())));
					particle.add(positionAllocation);
				}
				poblation.add(particle);
			}
		}
		return poblation;
	}

	public static List<List<Allocation>> createInitPoblationRandom(List<Cloudlet> cloudletList, List<PowerVm> vmList, List<PowerHost> hostList) {

		List<List<Allocation>> poblation = new ArrayList<>();

		for (int i = 1; i <= Constants.NUM_PARTICLES; i++) {

			List<PowerVm> vmRandomList = new ArrayList<>(vmList);
			Collections.shuffle(vmRandomList);

			List<Allocation> particle = new ArrayList<>();
			for (Cloudlet cloudlet : cloudletList) {
				Allocation positionAllocation = new Allocation(
						cloudlet,
						vmRandomList.get(cloudlet.getCloudletId() % vmRandomList.size()),
						hostList.get((int) (Math.random() * (double) hostList.size())));
				particle.add(positionAllocation);
			}
			poblation.add(particle);
		}		

		return poblation;
	}	

	public static List<List<Allocation>> createInitPoblationLinealDistribution(List<Cloudlet> cloudletList, List<PowerVm> vmList, List<PowerHost> hostList) {

		List<List<Allocation>> poblation = new ArrayList<>();

		for (int i = 1; i <= Constants.NUM_PARTICLES; i++) {

			List<PowerVm> vmRandomList = new ArrayList<>(vmList);
			Collections.shuffle(vmRandomList);
			
			List<Allocation> particle = new ArrayList<>();
			for (Cloudlet cloudlet : cloudletList) {
				Allocation positionAllocation = new Allocation(
						cloudlet,
						vmRandomList.get(((int)((double)cloudlet.getCloudletId()*((double)i/(double)Constants.NUM_PARTICLES) 
							+ ((double)vmRandomList.size()*(1.0d-((double)i/(double)Constants.NUM_PARTICLES)))) + i*5 ) % vmRandomList.size()),
						hostList.get((int) (Math.random() * (double) hostList.size())));
				particle.add(positionAllocation);
			}
			poblation.add(particle);
		}		

		return poblation;
	}

	public static Set<Integer> getUniqueRandomNumbers(int n, int upperBound, Set<Integer> excludedSet,
			int repetitionsAllowed) {
		Set<Integer> uniqueNumbers = new HashSet<>();
		Random random = new Random();

		// Map to keep track of how many times each number has been added
		int[] frequency = new int[upperBound];

		while (uniqueNumbers.size() < n) {
			int number = random.nextInt(upperBound); // Genera un número aleatorio entre 0 y upperBound-1
			if (!excludedSet.contains(number) || frequency[number] < repetitionsAllowed) {
				uniqueNumbers.add(number);
				frequency[number]++;
			}
		}

		return uniqueNumbers;
	}


		/**
	 * Prints the results.
	 * 
	 * @param datacenter the datacenter
	 * @param lastClock the last clock
	 * @param experimentName the experiment name
	 * @param outputInCsv the output in csv
	 * @param outputFolder the output folder
	 */
	public static void printResults(
			PowerDatacenter datacenter,
			List<Vm> vms,
			double lastClock,
			String experimentName,
			boolean outputInCsv,
			String outputFolder, int psoAlgorithm, double weight1, double weight2, double weight3, double weight4, double bestFitness, int lastIterGlobalChange, int countGlobalChange) {
		Log.enable();
		List<Host> hosts = datacenter.getHostList();

		int numberOfHosts = hosts.size();
		int numberOfVms = vms.size();

		double totalSimulationTime = lastClock;
		double energy = datacenter.getPower() / (3600 * 1000);
		int numberOfMigrations = datacenter.getMigrationCount();

		Map<String, Double> slaMetrics = getSlaMetrics(vms);

		double slaOverall = slaMetrics.get("overall");
		double slaAverage = slaMetrics.get("average");
		double slaDegradationDueToMigration = slaMetrics.get("underallocated_migration");
		// double slaTimePerVmWithMigration = slaMetrics.get("sla_time_per_vm_with_migration");
		// double slaTimePerVmWithoutMigration =
		// slaMetrics.get("sla_time_per_vm_without_migration");
		// double slaTimePerHost = getSlaTimePerHost(hosts);
		double slaTimePerActiveHost = getSlaTimePerActiveHost(hosts);

		double sla = slaTimePerActiveHost * slaDegradationDueToMigration;

		List<Double> timeBeforeHostShutdown = getTimesBeforeHostShutdown(hosts);

		int numberOfHostShutdowns = timeBeforeHostShutdown.size();

		double meanTimeBeforeHostShutdown = Double.NaN;
		double stDevTimeBeforeHostShutdown = Double.NaN;
		if (!timeBeforeHostShutdown.isEmpty()) {
			meanTimeBeforeHostShutdown = MathUtil.mean(timeBeforeHostShutdown);
			stDevTimeBeforeHostShutdown = MathUtil.stDev(timeBeforeHostShutdown);
		}

		List<Double> timeBeforeVmMigration = getTimesBeforeVmMigration(vms);
		double meanTimeBeforeVmMigration = Double.NaN;
		double stDevTimeBeforeVmMigration = Double.NaN;
		if (!timeBeforeVmMigration.isEmpty()) {
			meanTimeBeforeVmMigration = MathUtil.mean(timeBeforeVmMigration);
			stDevTimeBeforeVmMigration = MathUtil.stDev(timeBeforeVmMigration);
		}

		if (outputInCsv) {
			File folder = new File(outputFolder);
			if (!folder.exists()) {
				folder.mkdir();
			}
			File folder1 = new File(outputFolder + "/stats");
			if (!folder1.exists()) {
				folder1.mkdir();
			}
			File folder2 = new File(outputFolder + "/time_before_host_shutdown");
			if (!folder2.exists()) {
				folder2.mkdir();
			}
			File folder3 = new File(outputFolder + "/time_before_vm_migration");
			if (!folder3.exists()) {
				folder3.mkdir();
			}
			File folder4 = new File(outputFolder + "/metrics");
			if (!folder4.exists()) {
				folder4.mkdir();
			}

			StringBuilder data = new StringBuilder();
			String delimeter = ",";

			data.append(weight1+delimeter+weight2+delimeter+weight3+delimeter+weight4+delimeter+bestFitness+delimeter+lastIterGlobalChange+ delimeter+ countGlobalChange+delimeter);
			data.append(parseExperimentName(experimentName+ delimeter
				+(psoAlgorithm==Constants.ORIGINAL_PSO?"ORIGINAL":"DISCRETE") 
				+ delimeter + Constants.POPULATION_INIT_FUNCTION
				+ delimeter + (psoAlgorithm==Constants.ORIGINAL_PSO?"":Constants.INTELLIGENT_FUNCTION)
				+ delimeter + Constants.COGNIT_COEFFICIENT
				+ delimeter + Constants.SOCIAL_COEFFICIENT
				+ delimeter + Constants.INERTIA_WEIGHT
				+ delimeter + Constants.NUM_ITERATIONS 
				+ delimeter + Constants.NUM_PARTICLES
				+ delimeter + Constants.VM_UTILIZATION_THRESHOLD
				+ delimeter + Constants.UTILIZATION_THRESHOLD));
			data.append(String.format("%d", numberOfHosts) + delimeter);
			data.append(String.format("%d", numberOfVms) + delimeter);
			data.append(String.format("%.2f", totalSimulationTime) + delimeter);
			data.append(String.format("%.5f", energy) + delimeter);
			data.append(String.format("%d", numberOfMigrations) + delimeter);
			data.append(String.format("%.10f", sla) + delimeter);
			data.append(String.format("%.10f", slaTimePerActiveHost) + delimeter);
			data.append(String.format("%.10f", slaDegradationDueToMigration) + delimeter);
			data.append(String.format("%.10f", slaOverall) + delimeter);
			data.append(String.format("%.10f", slaAverage) + delimeter);
			// data.append(String.format("%.5f", slaTimePerVmWithMigration) + delimeter);
			// data.append(String.format("%.5f", slaTimePerVmWithoutMigration) + delimeter);
			// data.append(String.format("%.5f", slaTimePerHost) + delimeter);
			data.append(String.format("%d", numberOfHostShutdowns) + delimeter);
			data.append(String.format("%.2f", meanTimeBeforeHostShutdown) + delimeter);
			data.append(String.format("%.2f", stDevTimeBeforeHostShutdown) + delimeter);
			data.append(String.format("%.2f", meanTimeBeforeVmMigration) + delimeter);
			data.append(String.format("%.2f", stDevTimeBeforeVmMigration) + delimeter);

			if (datacenter.getVmAllocationPolicy() instanceof PowerVmAllocationPolicyMigrationAbstract) {
				PowerVmAllocationPolicyMigrationAbstract vmAllocationPolicy = (PowerVmAllocationPolicyMigrationAbstract) datacenter
						.getVmAllocationPolicy();

				double executionTimeVmSelectionMean = MathUtil.mean(vmAllocationPolicy
						.getExecutionTimeHistoryVmSelection());
				double executionTimeVmSelectionStDev = MathUtil.stDev(vmAllocationPolicy
						.getExecutionTimeHistoryVmSelection());
				double executionTimeHostSelectionMean = MathUtil.mean(vmAllocationPolicy
						.getExecutionTimeHistoryHostSelection());
				double executionTimeHostSelectionStDev = MathUtil.stDev(vmAllocationPolicy
						.getExecutionTimeHistoryHostSelection());
				double executionTimeVmReallocationMean = MathUtil.mean(vmAllocationPolicy
						.getExecutionTimeHistoryVmReallocation());
				double executionTimeVmReallocationStDev = MathUtil.stDev(vmAllocationPolicy
						.getExecutionTimeHistoryVmReallocation());
				double executionTimeTotalMean = MathUtil.mean(vmAllocationPolicy
						.getExecutionTimeHistoryTotal());
				double executionTimeTotalStDev = MathUtil.stDev(vmAllocationPolicy
						.getExecutionTimeHistoryTotal());

				data.append(String.format("%.5f", executionTimeVmSelectionMean) + delimeter);
				data.append(String.format("%.5f", executionTimeVmSelectionStDev) + delimeter);
				data.append(String.format("%.5f", executionTimeHostSelectionMean) + delimeter);
				data.append(String.format("%.5f", executionTimeHostSelectionStDev) + delimeter);
				data.append(String.format("%.5f", executionTimeVmReallocationMean) + delimeter);
				data.append(String.format("%.5f", executionTimeVmReallocationStDev) + delimeter);
				data.append(String.format("%.5f", executionTimeTotalMean) + delimeter);
				data.append(String.format("%.5f", executionTimeTotalStDev) + delimeter);

				writeMetricHistory(hosts, vmAllocationPolicy, outputFolder + "/metrics/" + experimentName
						+ "_metric");
			}

			data.append("\n");

			writeDataRow(data.toString(), outputFolder + "/stats/" + experimentName + "_stats.csv");
			writeDataColumn(timeBeforeHostShutdown, outputFolder + "/time_before_host_shutdown/"
					+ experimentName + "_time_before_host_shutdown.csv");
			writeDataColumn(timeBeforeVmMigration, outputFolder + "/time_before_vm_migration/"
					+ experimentName + "_time_before_vm_migration.csv");

		} else {
			Log.setDisabled(false);
			Log.printLine();
			Log.printLine(String.format("Experiment name: " + experimentName));
			Log.printLine(String.format("Number of hosts: " + numberOfHosts));
			Log.printLine(String.format("Number of VMs: " + numberOfVms));
			Log.printLine(String.format("Total simulation time: %.2f sec", totalSimulationTime));
			Log.printLine(String.format("Energy consumption: %.2f kWh", energy));
			Log.printLine(String.format("Number of VM migrations: %d", numberOfMigrations));
			Log.printLine(String.format("SLA: %.5f%%", sla * 100));
			Log.printLine(String.format(
					"SLA perf degradation due to migration: %.2f%%",
					slaDegradationDueToMigration * 100));
			Log.printLine(String.format("SLA time per active host: %.2f%%", slaTimePerActiveHost * 100));
			Log.printLine(String.format("Overall SLA violation: %.2f%%", slaOverall * 100));
			Log.printLine(String.format("Average SLA violation: %.2f%%", slaAverage * 100));
			// Log.printLine(String.format("SLA time per VM with migration: %.2f%%",
			// slaTimePerVmWithMigration * 100));
			// Log.printLine(String.format("SLA time per VM without migration: %.2f%%",
			// slaTimePerVmWithoutMigration * 100));
			// Log.printLine(String.format("SLA time per host: %.2f%%", slaTimePerHost * 100));
			Log.printLine(String.format("Number of host shutdowns: %d", numberOfHostShutdowns));
			Log.printLine(String.format(
					"Mean time before a host shutdown: %.2f sec",
					meanTimeBeforeHostShutdown));
			Log.printLine(String.format(
					"StDev time before a host shutdown: %.2f sec",
					stDevTimeBeforeHostShutdown));
			Log.printLine(String.format(
					"Mean time before a VM migration: %.2f sec",
					meanTimeBeforeVmMigration));
			Log.printLine(String.format(
					"StDev time before a VM migration: %.2f sec",
					stDevTimeBeforeVmMigration));

			if (datacenter.getVmAllocationPolicy() instanceof PowerVmAllocationPolicyMigrationAbstract) {
				PowerVmAllocationPolicyMigrationAbstract vmAllocationPolicy = (PowerVmAllocationPolicyMigrationAbstract) datacenter
						.getVmAllocationPolicy();

				double executionTimeVmSelectionMean = MathUtil.mean(vmAllocationPolicy
						.getExecutionTimeHistoryVmSelection());
				double executionTimeVmSelectionStDev = MathUtil.stDev(vmAllocationPolicy
						.getExecutionTimeHistoryVmSelection());
				double executionTimeHostSelectionMean = MathUtil.mean(vmAllocationPolicy
						.getExecutionTimeHistoryHostSelection());
				double executionTimeHostSelectionStDev = MathUtil.stDev(vmAllocationPolicy
						.getExecutionTimeHistoryHostSelection());
				double executionTimeVmReallocationMean = MathUtil.mean(vmAllocationPolicy
						.getExecutionTimeHistoryVmReallocation());
				double executionTimeVmReallocationStDev = MathUtil.stDev(vmAllocationPolicy
						.getExecutionTimeHistoryVmReallocation());
				double executionTimeTotalMean = MathUtil.mean(vmAllocationPolicy
						.getExecutionTimeHistoryTotal());
				double executionTimeTotalStDev = MathUtil.stDev(vmAllocationPolicy
						.getExecutionTimeHistoryTotal());

				Log.printLine(String.format(
						"Execution time - VM selection mean: %.5f sec",
						executionTimeVmSelectionMean));
				Log.printLine(String.format(
						"Execution time - VM selection stDev: %.5f sec",
						executionTimeVmSelectionStDev));
				Log.printLine(String.format(
						"Execution time - host selection mean: %.5f sec",
						executionTimeHostSelectionMean));
				Log.printLine(String.format(
						"Execution time - host selection stDev: %.5f sec",
						executionTimeHostSelectionStDev));
				Log.printLine(String.format(
						"Execution time - VM reallocation mean: %.5f sec",
						executionTimeVmReallocationMean));
				Log.printLine(String.format(
						"Execution time - VM reallocation stDev: %.5f sec",
						executionTimeVmReallocationStDev));
				Log.printLine(String.format("Execution time - total mean: %.5f sec", executionTimeTotalMean));
				Log.printLine(String
						.format("Execution time - total stDev: %.5f sec", executionTimeTotalStDev));
			}
			Log.printLine();
		}

		Log.setDisabled(true);
	}

}
