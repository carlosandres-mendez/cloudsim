package org.cloudbus.cloudsim.examples.pso;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.examples.power.random.RandomConstants;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.pso.PSODatacenterBroker;

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

	public static List<List<Allocation>> createInitPoblation(List<Cloudlet> cloudletList, List<PowerVm> vmList,
			List<PowerHost> hostList) {

		// Number of subsets of particles, ej. {idX}, {idY,idZ}, ..., {idX..N}
		int numSubsetParticles = (int) ((double) Constants.NUM_PARTICLES / (double) RandomConstants.NUMBER_OF_VMS);

		List<List<Allocation>> poblation = new ArrayList<>();
		for (int i = 1; i <= RandomConstants.NUMBER_OF_VMS; i++) { // number of different vms in each particle from 1 to
																	// N

			Set<Integer> lastNumbers = new HashSet<>();
			for (int j = 0; j < numSubsetParticles; j++) { // number of particles we are going to create for each number
															// of different vms

				List<Integer> idVmsList = new ArrayList<>();
				Set<Integer> uniqueIdVms = getUniqueRandomNumbers(i, RandomConstants.NUMBER_OF_VMS, lastNumbers, i - 1);
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

}
