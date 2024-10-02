package org.cloudbus.cloudsim.container.containerPlacementPolicies;


import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerVm;

import java.util.List;
import java.util.Set;



/**
 * Created by carlosandres.mendez on 2024.
 * For container placement PSO policy.
 */

public class ContainerPlacementPolicyPSO extends ContainerPlacementPolicy {

    @Override
    public ContainerVm getContainerVm(List<ContainerVm> vmList, Object obj, Set<? extends ContainerVm> excludedVmList) {
        Container container = (Container)obj;
        return container.getVm();
    }
}
