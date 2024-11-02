package org.cloudbus.cloudsim.examples.pso.original.planetlab;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.examples.power.random.RandomConstants;
import org.cloudbus.cloudsim.examples.pso.Allocation;
import org.cloudbus.cloudsim.examples.pso.Constants;
import org.cloudbus.cloudsim.examples.pso.Helper;
import org.cloudbus.cloudsim.examples.pso.PlanetLabRunner;
import org.cloudbus.cloudsim.examples.pso.original.PSO_FitnessFunction;
import org.cloudbus.cloudsim.examples.pso.original.PSO_Particle;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicyMigrationStaticThresholdPSO;

import net.sourceforge.jswarm_pso.Particle;
import net.sourceforge.jswarm_pso.ParticleUpdateSimple;
import net.sourceforge.jswarm_pso.Swarm;

public class Scheduler extends PlanetLabRunner {
    Swarm swarm;
    PSO_FitnessFunction fitnessFunction;

    /**
     * Instantiates a new planet lab runner.
     * 
     * @param enableOutput       the enable output
     * @param outputToFile       the output to file
     * @param inputFolder        the input folder
     * @param outputFolder       the output folder
     * @param workload           the workload
     * @param vmAllocationPolicy the vm allocation policy
     * @param vmSelectionPolicy  the vm selection policy
     * @param parameter          the parameter
     */
    public Scheduler(
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

    private void optimize() {

        /**
         * HOST UTILIZATION
         * Estimated percentage: total VMs mips / total host mips
         * This is a estimated value when all the vms are started (in this simulation
         * all the vms start at the same time)
         * However, during the simulation this value is going to be changed depending on
         * the finish cloudlets time or vm migrations
         **/
        for (PowerHost host : hostList) {
            double vmsMIPS = 0.0d;
            for (Vm vm : host.getVmList()) {
                vmsMIPS += vm.getMips() * vm.getNumberOfPes();
            }
            host.setUtilizationEstimation(vmsMIPS / (double) host.getTotalMips());
        }

        // *** Estimate power consumption from all hosts ***
        List<PowerHost> powerHostsOrderByPowerConsumption = new ArrayList<>(PlanetLabRunner.hostList); // asc, estimated
                                                                                                       // by
                                                                                                       // the host
                                                                                                       // utilization
                                                                                                       // fixed
                                                                                                       // in
                                                                                                       // Constants.UTILIZATION_THRESHOLD
        // host doest have power as an attribute -only the method-, but added for power
        // consumption estimation
        for (PowerHost h1 : powerHostsOrderByPowerConsumption) {
            h1.setPowerEstimation(h1.getPower(Constants.UTILIZATION_THRESHOLD));
        }

        // initialize particles
        PSO_Particle[] particles = new PSO_Particle[Constants.NUM_PARTICLES];
        int cont = 0;
        List<List<Allocation>> initPoblation = Helper.createInitPoblation(cloudletList,
                (List<PowerVm>) (Object) (PlanetLabRunner.vmList), PlanetLabRunner.hostList);

        for (List<Allocation> particle : initPoblation) {

            double[] position = new double[PlanetLabRunner.cloudletList.size()];
            double[] velocity = new double[PlanetLabRunner.cloudletList.size()];
            for (Allocation allocation : particle) {
                position[allocation.getCloudlet().getCloudletId()] = allocation.getVm().getId();
                velocity[allocation.getCloudlet()
                        .getCloudletId()] = (double) (int) (Math.random() * (double) RandomConstants.NUMBER_OF_VMS);
            }
            particles[cont++] = new PSO_Particle(PlanetLabRunner.cloudletList.size(), position, velocity);
        }

        System.out.println();
        for (PSO_Particle particle : particles)
            System.out.println(particle);
        System.out.println();

        // For stats and analysis
        double[] maeIteracion = new double[Constants.NUM_ITERATIONS];
        double[] maeIteracionGobalUpdate = new double[Constants.NUM_ITERATIONS];

        // adding particles that can represent especial situations, such as the real
        // state of a datacenter
        // particles[Constants.NUM_PARTICLES-1]= new PSO_Particle(cloudletList.size(),
        // PlanetLabRunner.vmList.size() , 0);

        fitnessFunction = new PSO_FitnessFunction(cloudletList, (List<PowerVm>) (Object) (PlanetLabRunner.vmList),
                PlanetLabRunner.hostList);
        swarm = new Swarm(cloudletList.size(), new PSO_Particle(cloudletList.size(), PlanetLabRunner.vmList.size()),
                fitnessFunction);
        /**
         * better performance with the default parameters
         * swarm.setGlobalIncrement(Constants.SOCIAL_COEFFICIENT);
         * swarm.setParticleIncrement(Constants.COGNIT_COEFFICIENT);
         * swarm.setInertia(Constants.INERTIA_WEIGHT);
         */
        swarm.setNumberOfParticles(Constants.NUM_PARTICLES);
        swarm.setMinPosition(0);// minimum value is the minimum value of vm id
        swarm.setMaxPosition(PlanetLabRunner.vmList.size() - 1);// maximum value of vm id
        swarm.setMaxMinVelocity(1.1);
        swarm.setParticles(particles);
        swarm.setParticleUpdate(
                new ParticleUpdateSimple(new PSO_Particle(cloudletList.size(), PlanetLabRunner.vmList.size())));
        for (int i = 0; i < Constants.NUM_ITERATIONS; i++) {
            swarm.evolve();
            if (i % 10 == 0) {
                System.out.println("Global best at iteration " + i + " :" + swarm.getBestFitness());
            }

            double sumMae = 0.0;
            double sumMaeGlobalUpdate = 0.0;
            for (Particle particle : particles) {
                sumMae += particle.mae;
                sumMaeGlobalUpdate += particle.maeGlobalUpdate;
            }
            double promedioMae = sumMae / (double) particles.length;
            double promedioMaeGlobalUpdate = sumMaeGlobalUpdate / (double) particles.length;
            maeIteracion[i] = promedioMae;
            maeIteracionGobalUpdate[i] = promedioMaeGlobalUpdate;
        }
        System.out.println("ORIGINAL PSO The best fitness value is " + swarm.getBestFitness());
        PSO_Particle bestparticle = (PSO_Particle) swarm.getBestParticle();
        System.out.println(bestparticle.toString());

        System.out.println("********* MAE Stat **************");
        for (int i = 0; i < Constants.NUM_ITERATIONS; i++) {
            System.out.print(String.format("%.2f", maeIteracion[i]) + " ");
        }
        System.out.println("********* MAE stat Global update **************");
        int cont2 = 0;
        for (int i = 0; i < Constants.NUM_ITERATIONS; i++) {
            if (maeIteracionGobalUpdate[i] != 0)
                cont2++;
            System.out.print(String.format("%.5f", maeIteracionGobalUpdate[i]) + " ");
        }
        System.out.println("\nTotal global changes: " + cont2);
        System.out.println("***** END Original PSO **********");
    }

    /**
     * Starts the simulation.
     * 
     * @param experimentName     the experiment name
     * @param outputFolder       the output folder
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
             * scheduler may know allocation policy, so it can figure out host assignment
             * (important for host power consideration in the scheduling process)
             */
            PowerVmAllocationPolicyMigrationStaticThresholdPSO vmAllocationMigrationMSPolicy = (PowerVmAllocationPolicyMigrationStaticThresholdPSO) vmAllocationPolicy;
            vmAllocationMigrationMSPolicy.setHostList(hostList);
            Set<? extends Host> excludedHosts = new HashSet<>();
            for (PowerVm vm : (List<PowerVm>) (Object) (PlanetLabRunner.vmList)) {
                PowerHost host = vmAllocationMigrationMSPolicy.findHostForVm(vm, excludedHosts);
                if (host != null) {
                    host.getVmList().add(vm);

                    List<Double> mips = new ArrayList<Double>();
                    for (int i = 0; i < vm.getNumberOfPes(); i++)
                        mips.add(vm.getMips());
                    host.getVmScheduler().allocatePesForVm(vm, mips);
                    vm.setHost(host);
                    vm.setBeingInstantiated(true);
                    System.out
                            .println(" Vm allocation in scheduling time: Vm: " + vm.getId() + " Host: " + host.getId());
                } else
                    throw new Exception(
                            "According to the allocation policy, all Vms cannot be allocated in the datacenter. You need to increase servers on them.");
            }

            optimize();

            /***
             * After optimization (see Before optimization)
             * Clear the hosts and vms in the datacenter
             */
            for (PowerVm vm : (List<PowerVm>) (Object) (PlanetLabRunner.vmList)) {
                vm.setHost(null);
            }

            for (PowerHost host : hostList) {
                host.getVmList().clear();
                host.getVmScheduler().deallocatePesForAllVms();
            }

            /***
             * After optimization (see Before optimization)
             * Bind cloudlets to vms in the datacenter
             */
            for (Cloudlet cloudlet : cloudletList) {
                Vm vm = PlanetLabRunner.vmList
                        .get((int) swarm.getBestParticle().getBestPosition()[cloudlet.getCloudletId()]);
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
