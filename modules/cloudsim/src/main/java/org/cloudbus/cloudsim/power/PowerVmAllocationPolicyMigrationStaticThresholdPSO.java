package org.cloudbus.cloudsim.power;

import java.util.List;

import org.cloudbus.cloudsim.Host;

public class PowerVmAllocationPolicyMigrationStaticThresholdPSO extends PowerVmAllocationPolicyMigrationStaticThreshold{

    public PowerVmAllocationPolicyMigrationStaticThresholdPSO(List<? extends Host> hostList,
            PowerVmSelectionPolicy vmSelectionPolicy, double utilizationThreshold) {
        super(hostList, vmSelectionPolicy, utilizationThreshold);

    }

    /**
	 * Sets the host list.
	 * 
	 * @param hostList the new host list
	 */
	public void setHostList(List<? extends Host> hostList) {
		super.setHostList(hostList);
	}

    
}
