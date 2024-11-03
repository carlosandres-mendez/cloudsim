package org.cloudbus.cloudsim.examples.pso.discrete.random;

import java.util.ArrayList;
import java.util.Comparator;
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
import org.cloudbus.cloudsim.examples.pso.RandomRunner;
import org.cloudbus.cloudsim.examples.pso.discrete.Discrete_FitnessFunction;
import org.cloudbus.cloudsim.examples.pso.discrete.Discrete_PSO_Swarm;
import org.cloudbus.cloudsim.examples.pso.discrete.Discrete_Particle;
import org.cloudbus.cloudsim.examples.pso.discrete.Discrete_ParticleUpdate;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicyMigrationStaticThresholdPSO;

public class Scheduler extends RandomRunner {

    Discrete_PSO_Swarm swarm;

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

        // *** domain problem data ***
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


         /**
         * POWER CONSUMPTION ESTIMATION
         * Estimated percentage using the power consumption model and the UTILIZATION_THRESHOLD
         * This is a estimated value when all the hosts are started (in this simulation
         * all the vms and hosts start at the same time)
         * However, during the simulation this value is going to be changed depending on
         * the finish cloudlets time or vm migrations
         **/

        // *** Estimate power consumption from all hosts ***
        List<PowerHost> powerHostsOrderByPowerConsumption = new ArrayList<>(RandomRunner.hostList); // asc, estimated by
                                                                                                    // the host
                                                                                                    // utilization fixed
                                                                                                    // in
                                                                                                    // Constants.UTILIZATION_THRESHOLD
        // host doest have power as an attribute -only the method-, but added for power
        // consumption estimation
        for (PowerHost h1 : powerHostsOrderByPowerConsumption) {
            h1.setPowerEstimation(h1.getPower(h1.getUtilizationEstimation()));
        }

        // *** Estimate power consumption from all hosts ***
        powerHostsOrderByPowerConsumption.sort(Comparator.comparingDouble((PowerHost p) -> p.getTotalMips()));

        // powerVmsOrderByPowerConsumption.sort(Comparator.comparingDouble((PowerVm p)
        // -> p.getMips()).reversed()
        // .thenComparing((PowerVm p) ->
        // ((PowerHost)p.getHost()).getPower(Constants.UTILIZATION_THRESHOLD) ));

        List<PowerVm> powerVmsOrderByPowerConsumption = new ArrayList<>((List<PowerVm>) (Object) (RandomRunner.vmList)); 
        powerVmsOrderByPowerConsumption.sort(
                Comparator
                        .comparingDouble(
                                (PowerVm p) -> ((PowerHost) p.getHost()).getPower(Constants.UTILIZATION_THRESHOLD))
                        .thenComparing(Comparator.comparingDouble(PowerVm::getMips).reversed()));

        for (PowerHost host : powerHostsOrderByPowerConsumption) {
            System.out.println(host.getId() + " " + host.getPowerEstimation());
        }


         /**
         * CLOUDLETS UTILIZATION CPU ESTIMATION
         * Estimated average using the cpu utilization model of the first 10 intervals
         * This is a estimated value when all the hosts are started (in this simulation
         * all the vms and hosts start at the same time)
         * However, during the simulation this value is going to be changed depending on
         * the finish cloudlets time or vm migrations
         **/
        for(Cloudlet cloudlet : cloudletList){
            double avg = 0.0d;
            for(int i = 1; i <= 10; i++)
                avg += cloudlet.getUtilizationOfCpu(((double)i)*Constants.SCHEDULING_INTERVAL);
            avg /= 10;
            cloudlet.setUtilizationOfCpuEstimation(avg);
        }



        System.out.println("Info Vms Ordered By VM mips and Host Power Consumption ************");
        for (PowerVm p : powerVmsOrderByPowerConsumption) {
            System.out.println(
                    "Host: " + p.getHost().getId() + " power: " + ((PowerHost) p.getHost()).getPowerEstimation()
                            + " mips: " + p.getHost().getTotalMips() + " vm: " + p.getId() + " vm mips:" + p.getMips());
        }

        // initialize particles
        ArrayList<Discrete_Particle> particles = new ArrayList<>();
        List<List<Allocation>> initPoblation = Helper.createInitPoblation(cloudletList,
                (List<PowerVm>) (Object) (RandomRunner.vmList), RandomRunner.hostList);

        for (List<Allocation> particle_position : initPoblation) {

            List<Allocation> velocity = new ArrayList<>();
            for (Allocation allocation : particle_position) {
                Allocation velocityAllocation = new Allocation(
                        allocation.getCloudlet(),
                        ((List<PowerVm>) (Object) (RandomRunner.vmList))
                                .get((int) (Math.random() * (double) RandomConstants.NUMBER_OF_VMS)),
                        RandomRunner.hostList.get((int) (Math.random() * (double) RandomRunner.hostList.size())));
                velocity.add(velocityAllocation);
            }
            particles.add(new Discrete_Particle(particle_position, velocity));
        }

        System.out.println();
        for (Discrete_Particle particle : particles)
            System.out.println(particle);
        System.out.println();

        // For stats and analysis
        double[] maeIteracion = new double[Constants.NUM_ITERATIONS];
        double[] maeIteracionGobalUpdate = new double[Constants.NUM_ITERATIONS];

        swarm = new Discrete_PSO_Swarm(
                new Discrete_FitnessFunction(cloudletList, (List<PowerVm>) (Object) (RandomRunner.vmList),
                        RandomRunner.hostList),
                RandomRunner.hostList, (List<PowerVm>) (Object) (RandomRunner.vmList), cloudletList);
        swarm.setParticleUpdate(
                new Discrete_ParticleUpdate(powerHostsOrderByPowerConsumption, powerVmsOrderByPowerConsumption, cloudletList));
        swarm.setGlobalIncrement(Constants.SOCIAL_COEFFICIENT);
        swarm.setParticleIncrement(Constants.COGNIT_COEFFICIENT);
        swarm.setInertia(Constants.INERTIA_WEIGHT);
        swarm.setNumberOfParticles(Constants.NUM_PARTICLES);
        swarm.setParticles(particles);

        for (int i = 0; i < Constants.NUM_ITERATIONS; i++) {
            swarm.evolve();
            if (i % 10 == 0) {
                System.out.println("Global best at iteration " + i + " :" + swarm.getBestFitness());
            }

            double sumMae = 0.0;
            double sumMaeGlobalUpdate = 0.0;
            for (Discrete_Particle particle : swarm.getParticles()) {
                sumMae += particle.mae;
                sumMaeGlobalUpdate += particle.maeGlobalUpdate;
            }
            double promedioMae = sumMae / (double) swarm.getParticles().size();
            double promedioMaeGlobalUpdate = sumMaeGlobalUpdate / (double) swarm.getParticles().size();
            maeIteracion[i] = promedioMae;
            maeIteracionGobalUpdate[i] = promedioMaeGlobalUpdate;
        }

        System.out.println("DISCRETE PSO The best fitness value is " + swarm.getBestFitness());
        Discrete_Particle bestparticle = (Discrete_Particle) swarm.getBestParticle();
        System.out.println(bestparticle.toString());

        // System.out.println("--------Global best---------------");
        // if(swarm.getBestPosition()!=null){
        // for(Allocation allocation : swarm.getBestPosition()){
        // System.out.println("host" + allocation.getHost() + "vm" + allocation.getVm()+
        // "cloudlet"+ allocation.getCloudlet());
        // }
        // }

        System.out.println("********* MAE stat **************");
        for (int i = 0; i < Constants.NUM_ITERATIONS; i++) {
            System.out.print(String.format("%.2f", maeIteracion[i]) + " ");
        }
        System.out.println("********* MAE stat Global update **************");
        int cont = 0;
        for (int i = 0; i < Constants.NUM_ITERATIONS; i++) {
            if (maeIteracionGobalUpdate[i] != 0)
                cont++;
            System.out.print(String.format("%.5f", maeIteracionGobalUpdate[i]) + " ");
        }
        System.out.println("\nTotal global changes: " + cont);

        System.out.println("********* END Discrete PSO **************");
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
            for (PowerVm vm : (List<PowerVm>) (Object) (RandomRunner.vmList)) {
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

            System.out.println(" PRE VALIDATION TO OPTMIZATION ");
            for (PowerHost host : hostList) {
                double totalVmMIPS = 0.0d;
                for (PowerVm vm : (List<PowerVm>) (Object) (host.getVmList())) {
                    totalVmMIPS += vm.getMips();
                }
                if (totalVmMIPS > host.getTotalMips())
                    System.out.println(" totalVmMIPS > host.getTotalMips() in host: " + host.getId());
            }

            optimize();

            System.out.println(" POS VALIDATION TO OPTMIZATION ");
            for (PowerHost host : hostList) {
                double totalVmMIPS = 0.0d;
                for (PowerVm vm : (List<PowerVm>) (Object) (host.getVmList())) {
                    totalVmMIPS += vm.getMips();
                }
                if (totalVmMIPS > host.getTotalMips())
                    System.out.println(" totalVmMIPS > host.getTotalMips() in host: " + host.getId());
            }

            /***
             * After optimization (see Before optimization)
             * Clear the hosts and vms in the datacenter
             */
            for (PowerVm vm : (List<PowerVm>) (Object) (RandomRunner.vmList)) {
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
            for (Allocation allocation : swarm.getBestPosition()) {
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

            Helper.printCloudletList(cloudletList);

        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
            System.exit(0);
        }

        Log.printLine("Finished " + experimentName);
    }

    // @Override
    protected void start2(String experimentName, String outputFolder, VmAllocationPolicy vmAllocationPolicy) {
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
            for (PowerVm vm : (List<PowerVm>) (Object) (RandomRunner.vmList)) {
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

            // *** domain problem data ***
            List<PowerHost> powerHostsOrderByPowerConsumption = new ArrayList<>(RandomRunner.hostList); // asc,
                                                                                                        // estimated by
                                                                                                        // the host
                                                                                                        // utilization
                                                                                                        // fixed in
                                                                                                        // Constants.UTILIZATION_THRESHOLD
            List<PowerVm> powerVmsOrderByPowerConsumption = new ArrayList<>(
                    (List<PowerVm>) (Object) (RandomRunner.vmList)); // asc, according with the hosts power consumption
                                                                     // and the initial policy allocation

            // host doest have power as an attribute -only the method-, but added for power
            // consumption estimation
            for (PowerHost h1 : powerHostsOrderByPowerConsumption) {
                h1.setPowerEstimation(h1.getPower(Constants.UTILIZATION_THRESHOLD));
            }

            powerHostsOrderByPowerConsumption.sort(Comparator.comparingDouble((PowerHost p) -> p.getTotalMips()));

            // powerVmsOrderByPowerConsumption.sort(
            // Comparator.comparingDouble((PowerVm p) -> p.getMips()).reversed()
            // .thenComparing((PowerVm p) ->
            // ((PowerHost)p.getHost()).getPower(Constants.UTILIZATION_THRESHOLD))
            // );

            powerVmsOrderByPowerConsumption.sort(
                    Comparator
                            .comparingDouble(
                                    (PowerVm p) -> ((PowerHost) p.getHost()).getPower(Constants.UTILIZATION_THRESHOLD))
                            .thenComparing(Comparator.comparingDouble(PowerVm::getMips).reversed()));

            for (PowerHost host : powerHostsOrderByPowerConsumption) {
                System.out.println(host.getId() + " " + host.getPowerEstimation());
            }

            System.out.println("Info Vms Ordered By VM mips and Host Power Consumption ************");
            int count = 0;
            for (PowerVm p : powerVmsOrderByPowerConsumption) {
                System.out.println("Host: " + p.getHost().getId() + " power: "
                        + ((PowerHost) p.getHost()).getPowerEstimation() + " mips: " + p.getHost().getTotalMips()
                        + " vm: " + p.getId() + " vm mips:" + p.getMips());

                broker.bindCloudletToVm(count, p.getId());
                count++;
                if (count == 50)
                    break;
                broker.bindCloudletToVm(count, p.getId());
                count++;
                if (count == 50)
                    break;
                broker.bindCloudletToVm(count, p.getId());
                count++;
                if (count == 50)
                    break;
            }

            /***
             * After optimization (see Before optimization)
             * Clear the hosts and vms in the datacenter
             */
            for (PowerVm vm : (List<PowerVm>) (Object) (RandomRunner.vmList)) {
                vm.setHost(null);
            }

            for (PowerHost host : hostList) {
                host.getVmList().clear();
                host.getVmScheduler().deallocatePesForAllVms();
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
            System.out.println(e.getMessage());
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
            System.exit(0);
        }

        Log.printLine("Finished " + experimentName);
    }

}
