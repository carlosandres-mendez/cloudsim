package org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled;

import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.container.core.ContainerVm;

public class Allocation {
    Container container;
    ContainerVm vm;
    ContainerHost host;
    
    public Allocation(Container container, ContainerVm vm, ContainerHost host) {
        this.container = container;
        this.vm = vm;
        this.host = host;
    }

    public Container getContainer() {
        return container;
    }

    public void setContainer(Container container) {
        this.container = container;
    }

    public ContainerVm getVm() {
        return vm;
    }

    public void setVm(ContainerVm vm) {
        this.vm = vm;
    }

    public ContainerHost getHost() {
        return host;
    }

    public void setHost(ContainerHost host) {
        this.host = host;
    }

    @Override
    public String toString() {
        return "Allocation [container=" + container + ", vm=" + vm + ", host=" + host + "]";
    }

}
