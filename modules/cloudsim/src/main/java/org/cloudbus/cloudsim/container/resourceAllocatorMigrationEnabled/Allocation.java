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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((container == null) ? 0 : container.hashCode());
        result = prime * result + ((vm == null) ? 0 : vm.hashCode());
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Allocation other = (Allocation) obj;
        if (container == null) {
            if (other.container != null)
                return false;
        } else if (!container.equals(other.container))
            return false;
        if (vm == null) {
            if (other.vm != null)
                return false;
        } else if (!vm.equals(other.vm))
            return false;
        if (host == null) {
            if (other.host != null)
                return false;
        } else if (!host.equals(other.host))
            return false;
        return true;
    }

}
