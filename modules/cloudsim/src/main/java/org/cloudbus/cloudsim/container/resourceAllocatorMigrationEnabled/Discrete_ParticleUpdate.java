package org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled;

import java.util.*;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.container.core.ContainerVm;
import org.cloudbus.cloudsim.container.core.PowerContainer;
import org.cloudbus.cloudsim.container.core.PowerContainerHost;
import org.cloudbus.cloudsim.container.core.PowerContainerHostUtilizationHistory;
import org.cloudbus.cloudsim.container.core.PowerContainerVm;
import org.cloudbus.cloudsim.container.resourceAllocators.PowerContainerAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;

/**
 * Particle update strategy
 * 
 * Every Swarm.evolve() itereation the following methods are called
 * 		- begin(Swarm) : Once at the begining of each iteration
 * 		- update(Swarm,Particle) : Once for each particle
 * 		- end(Swarm) : Once at the end of each iteration
 * 
 * @author carlosandres.mendez
 */
public class Discrete_ParticleUpdate {

    Discrete_PSO_Swarm swarm;
    Discrete_Particle particle;

    //A random weight r1.
    private final double WEIGHT_R1 = 0.2d;
    //The cognitive acceleration coefficient c1.
    private final double COGNIT_COEFFICIENT = 0.5d;

    //A random weight r2.
    private final double WEIGHT_R2 = 0.2d;
    //The social coefficient
    private final double SOCIAL_COEFFICIENT = 0.5d;

    
    public Discrete_ParticleUpdate() {
    }

    /** Update particle's velocity and position */
    public void update(Discrete_PSO_Swarm swarm, Discrete_Particle particle) {
        this.swarm = swarm;
        this.particle = particle;

        restoreParticleAllocation();

        // Update velocity 
        List<Allocation> personalPossibleMigrations = generatePossibleMigrations(WEIGHT_R1, COGNIT_COEFFICIENT, particle.getBestPosition(), particle.getPosition());
        //List<Allocation> globalPossibleMigrations = generatePossibleMigrations(WEIGHT_R2, SOCIAL_COEFFICIENT, swarm.getBestPosition(), particle.getPosition());

        for (Allocation possibleMigration : personalPossibleMigrations){
            boolean isFounded = false;
            for(Allocation allocation : particle.getVelocity()){
                if(allocation.getContainer().equals(possibleMigration.getContainer())){
                    isFounded = true;
                    allocation.setVm(possibleMigration.getVm());
                    allocation.setHost(possibleMigration.getHost());
                    break;
                }
            }
            if(!isFounded)
                particle.getVelocity().add(possibleMigration);
        }

        // for (Allocation possibleMigration : globalPossibleMigrations){
        //     boolean isFounded = false;
        //     for(Allocation allocation : particle.getVelocity()){
        //         if(allocation.getContainer().equals(possibleMigration.getContainer())){
        //             isFounded = true;
        //             allocation.setVm(possibleMigration.getVm());
        //             allocation.setHost(possibleMigration.getHost());
        //             break;
        //         }
        //     }
        //     if(!isFounded)
        //         particle.getVelocity().add(possibleMigration);
        // }

        // Update position
        for (Allocation positionAllocation : particle.getPosition()) {
            for (Allocation velocityAllocation : particle.getVelocity()){
                if(positionAllocation.getContainer().equals(velocityAllocation.getContainer())){
                    positionAllocation.setVm(velocityAllocation.getVm());
                    positionAllocation.setHost(velocityAllocation.getHost());
                }
            }
        }
    }

    private List<Allocation> generatePossibleMigrations(double randomWeight, double coefficient, List<Allocation> bestPosition, List<Allocation> xPosition){
        List<Allocation> possibleMigrations = new ArrayList<Allocation>();

        List<PowerContainerHostUtilizationHistory> overUtilizedHosts = this.swarm.allocationPolicy.getOverUtilizedHosts();

        //if(overUtilizedHosts.size()>0) {
            List<? extends Container> containersToMigrate = this.swarm.allocationPolicy.getContainersToMigrateFromHosts(overUtilizedHosts);

            List<Map<String, Object>> migrationMap = this.swarm.allocationPolicy.getPlacementForLeftContainers(containersToMigrate, new HashSet<ContainerHost>(overUtilizedHosts));
            migrationMap.addAll(this.swarm.allocationPolicy.getContainerMigrationMapFromUnderUtilizedHosts(overUtilizedHosts, migrationMap));

            for(Map<String, Object> migration : migrationMap){
                possibleMigrations.add(
                    new Allocation((Container)migration.get("container"), (ContainerVm)migration.get("vm"), (ContainerHost)migration.get("host")));
            }
        //}

        // for(Allocation xAllocation : xPosition){
        //     if(xPosition.get(xAllocation.getContainer().getId()) )
        // }

        return possibleMigrations;
    }

    /**
     * Gets random hosts.
     *
     * @return the random hosts
     */
    protected List<PowerContainerHostUtilizationHistory> getRandomHosts(int n) {
        List<PowerContainerHostUtilizationHistory> randomHosts = new LinkedList<PowerContainerHostUtilizationHistory>();
        Random rand = new Random();
        for(int i=0; i<n;i++){
            randomHosts.add(swarm.getAllocationPolicy().<PowerContainerHostUtilizationHistory>getContainerHostList().get(rand.nextInt(swarm.getAllocationPolicy().getContainerHostList().size())));
        }
        return randomHosts;
    }

    /**
     * Gets random vms.
     *
     * @return the random vms
     */
    protected List<PowerContainerVm> getRandomVms(PowerContainerHost host, int n, Set<? extends ContainerVm> excludedVmList) {
        List<PowerContainerVm> randomVms = new LinkedList<PowerContainerVm>();
        if (host.getVmList().size() > 0) {
            Random rand = new Random();
            for(int i=0; i<n;i++){
                for(int j=0; j<host.getVmList().size();j++){
                    
                    int randomNum = rand.nextInt(host.getVmList().size());
                    if (!excludedVmList.contains(host.<PowerContainerVm>getVmList().get(randomNum))){
                        randomVms.add(host.<PowerContainerVm>getVmList().get(randomNum));
                        break;
                    }
                } 
            }
        }
        else {

            Log.print(String.format("Error: The VM list Size is: %d", host.getVmList().size()));
        }
        return randomVms;
    }

    /**
     * Gets random containers.
     *
     * @return the random containers
     */
    protected List<PowerContainer> getRandomContainers(PowerContainerVm vm, int n) {
        List<PowerContainer> randomVms = new LinkedList<PowerContainer>();
        Random rand = new Random();
        for(int i=0; i<n;i++){
            randomVms.add(vm.<PowerContainer>getContainerList().get((rand.nextInt(vm.getContainerList().size()))));
        }
        return randomVms;
    }

    public Allocation findRandomVmForContainer(Container container, Set<? extends ContainerHost> excludedHosts, boolean checkForVM) {
        Allocation allocation = null;
        double minPower = Double.MAX_VALUE;
        PowerContainerHost allocatedHost = null;
        ContainerVm allocatedVm = null;

        List<PowerContainerHost> containerHostList = new ArrayList<>();
        for (PowerContainerHost host : swarm.getAllocationPolicy().<PowerContainerHost>getContainerHostList()) {
            containerHostList.add(host);
        }
        Collections.shuffle(containerHostList);

        for (PowerContainerHost host : containerHostList) {
            if (excludedHosts.contains(host)) {
                continue;
            }

            List<ContainerVm> containerVmList = new ArrayList<>();
            for (ContainerVm vm : host.getVmList()) {
                containerVmList.add(vm);
            }
            Collections.shuffle(containerVmList);

            for (ContainerVm vm : containerVmList) {
                if (checkForVM) {
                    if (vm.isInWaiting()) {
                        continue;
                    }
                }
                if (vm.isSuitableForContainer(container)) {
                    // if vm is overutilized or host would be overutilized after the allocation, this host is not chosen!
                    if (!isVmOverUtilized(vm)) {
                        continue;
                    }
                    if (swarm.getAllocationPolicy().getUtilizationOfCpuMips(host) != 0 && isHostOverUtilizedAfterContainerAllocation(host, vm, container)) {
                        continue;
                    }

                    try {
                        double powerAfterAllocation = getPowerAfterContainerAllocation(host, container, vm);
                        if (powerAfterAllocation != -1) {
                            double powerDiff = powerAfterAllocation - host.getPower();
                            if (powerDiff < minPower) {
                                minPower = powerDiff;
                                allocatedHost = host;
                                allocatedVm = vm;
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }
        if(allocatedVm!=null)
            allocation = new Allocation(container,allocatedVm,allocatedHost);

        return allocation;
    }

        protected boolean isVmOverUtilized(ContainerVm vm) {
        boolean isOverUtilized = true;
        double util = 0;
//        Log.printConcatLine("Checking if the vm is over utilized or not!");
        for (Container container : vm.getContainerList()) {
            util += container.getTotalUtilizationOfCpuMips(CloudSim.clock());
        }
        if (util > vm.getHost().getTotalMips() / vm.getHost().getNumberOfPes() * vm.getNumberOfPes()) {
            return false;
        }


        return isOverUtilized;
    }

    /**
     * Gets the power after allocation.
     *
     * @param host      the host
     * @param container the vm
     * @return the power after allocation
     */
    protected double getPowerAfterContainerAllocation(PowerContainerHost host, Container container, ContainerVm vm) {
        double power = 0;
        try {
            power = host.getPowerModel().getPower(getMaxUtilizationAfterContainerAllocation(host, container, vm));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        return power;
    }

    /**
     * Gets the power after allocation. We assume that load is balanced between PEs. The only
     * restriction is: VM's max MIPS < PE's MIPS
     *
     * @param host      the host
     * @param container the vm
     * @return the power after allocation
     */
    protected double getMaxUtilizationAfterContainerAllocation(PowerContainerHost host, Container container, ContainerVm containerVm) {
        double requestedTotalMips = container.getCurrentRequestedTotalMips();
        if (requestedTotalMips > containerVm.getMips()) {
            requestedTotalMips = containerVm.getMips();
        }
        double hostUtilizationMips = swarm.getAllocationPolicy().getUtilizationOfCpuMips(host);
        double hostPotentialUtilizationMips = hostUtilizationMips + requestedTotalMips;
        double pePotentialUtilization = hostPotentialUtilizationMips / host.getTotalMips();
        return pePotentialUtilization;
    }

    /**
     * Checks if is host over utilized after allocation.
     *
     * @param host      the host
     * @param container the vm
     * @return true, if is host over utilized after allocation
     */
    protected boolean isHostOverUtilizedAfterContainerAllocation(PowerContainerHost host, ContainerVm vm, Container container) {
        boolean isHostOverUtilizedAfterAllocation = true;
        if (vm.containerCreate(container)) {
            isHostOverUtilizedAfterAllocation = swarm.getAllocationPolicy().isHostOverUtilized(host);
            vm.containerDestroy(container);
        }
        return isHostOverUtilizedAfterAllocation;
    }

    /**
     * Restore allocation.
     */
    public void restoreParticleAllocation() {
        for (ContainerHost host : this.swarm.allocationPolicy.getContainerHostList()) {
            for (ContainerVm vm : host.getVmList()) {
                vm.containerDestroyAll();
                vm.reallocateMigratingInContainers();
            }

            host.containerVmDestroyAll();
            host.reallocateMigratingInContainerVms();
        }
        for (Allocation allocation : this.particle.position) {
            PowerContainerVm vm = (PowerContainerVm) allocation.getVm();

            for (Allocation allocation2 : this.particle.position) {
                PowerContainerVm vm2 = (PowerContainerVm) allocation2.getVm();
                Container container2 = (Container) allocation2.getContainer();
                if (vm.getId() == vm2.getId()) {
                    if (!vm.getContainerList().contains(container2)) {
                        if (!vm.containerCreate(container2)) {
                            Log.printConcatLine("Couldn't restore Container #", container2.getId(), " on vm #", vm.getId());
                            System.exit(0);
                        }
                    } else {
    
                        Log.print("The Container is in the VM already");
                    }
                }
            }

            PowerContainerHost host = (PowerContainerHost) allocation.getHost();
            if (!host.getVmList().contains(vm)) {
                if (!host.containerVmCreate(vm)) {
                    Log.printConcatLine("Couldn't restore VM #", vm.getId(), " on host #", host.getId());
                    System.exit(0);
                }

                this.swarm.allocationPolicy.getVmTable().put(vm.getUid(), host);
            }
//            vm.containerDestroyAll();
//            vm.reallocateMigratingInContainers();
        }
//        List<ContainerVm > restoredVms = new ArrayList<>();
        for (Allocation allocation : this.particle.position) {
            PowerContainerVm vm = (PowerContainerVm) allocation.getVm();
            if (allocation.getContainer() != null) {
                Container container = (Container) allocation.getContainer();
//                Log.print(container);

                // if (!vm.getContainerList().contains(container)) {
                //     if (!vm.containerCreate(container)) {
                //         Log.printConcatLine("Couldn't restore Container #", container.getId(), " on vm #", vm.getId());
                //         System.exit(0);
                //     }
                // } else {

                //     Log.print("The Container is in the VM already");
                // }

                if (container.getVm() == null) {
                    Log.print("The Vm is null");

                }
                ((PowerContainerAllocationPolicy) this.swarm.allocationPolicy.getDatacenter().getContainerAllocationPolicy()).
                        getContainerTable().put(container.getUid(), vm);
//            container.setVm(vm);

            }
        }


    }
    
}
