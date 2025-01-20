package org.cloudbus.cloudsim.examples.pso.original.planetlab;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.examples.power.planetlab.NonPowerAware;
import org.cloudbus.cloudsim.examples.pso.Constants;

/**
 * A simulation of a heterogeneous power aware data center that applies the Static Threshold (THR)
 * VM allocation policy and Maximum Correlation (MC) VM selection policy.
 * 
 * The remaining configuration parameters are in the Constants and RandomConstants classes.
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
public class ThrMmt {

	/**
	 * The main method.
	 * 
	 * @param args the arguments
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void main(String[] args) throws IOException {
		boolean enableOutput = true;
		boolean outputToFile = true;
		String inputFolder = NonPowerAware.class.getClassLoader().getResource("workload/planetlab").getPath();
		String outputFolder = "output";
		String workload = "20110303"; // PlanetLab workload
		String vmAllocationPolicy = "thr"; // Static Threshold (THR) VM allocation policy
		String vmSelectionPolicy = "mmt"; // Minimum Migration Time (MMT) VM selection policy
		String parameter = String.valueOf(Constants.UTILIZATION_THRESHOLD); // the static utilization threshold

		double weight1 = 1;
		double weight2 = 0;
		double weight3 = 0;
		double weight4 = 0;
		boolean allObjetiveCombinations = true;
		int repeat = 10; //if this option is used, then allObjetiveCombinations need to be false

		if(allObjetiveCombinations){
			//double[] valores ={0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1}; // Generamos los valores individuales
			double[] valores ={0.2,0.3,0.4}; // Generamos los valores individuales
			List<double[]> todasLasCombinaciones = generarCombinaciones(valores);
	
			// Imprimir las combinaciones (opcional)
			for (double[] combinacion : todasLasCombinaciones) {
				for (double valor : combinacion) {
					System.out.print(valor + " ");
				}
	
				new Scheduler(
						enableOutput,
						outputToFile,
						inputFolder,
						outputFolder,
						workload,
						vmAllocationPolicy,
						vmSelectionPolicy,
						parameter,combinacion[0],combinacion[1],combinacion[2],combinacion[3]);
				System.out.println();
			}
		}
		else if(repeat > 0){
			for(int i=0;i<repeat; i++){
				new Scheduler(
						enableOutput,
						outputToFile,
						inputFolder,
						outputFolder,
						workload,
						vmAllocationPolicy,
						vmSelectionPolicy,
						parameter,weight1,weight2,weight3,weight4);
			}
		}
		else{
			new Scheduler(
					enableOutput,
					outputToFile,
					inputFolder,
					outputFolder,
					workload,
					vmAllocationPolicy,
					vmSelectionPolicy,
					parameter,weight1,weight2,weight3,weight4);
		}
	}

	public static List<double[]> generarCombinaciones(double[] valoresIndividuales) {
        List<double[]> combinaciones = new ArrayList<>();
        int n = valoresIndividuales.length;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    for (int l = 0; l < n; l++) {
						if((valoresIndividuales[i] + valoresIndividuales[j] +valoresIndividuales[k] +valoresIndividuales[l])==1){
							double[] combinacion = {valoresIndividuales[i], valoresIndividuales[j], valoresIndividuales[k], valoresIndividuales[l]};
							combinaciones.add(combinacion);
						}
                    }
                }
            }
        }
        return combinaciones;
    }

}
