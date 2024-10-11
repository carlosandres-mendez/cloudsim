package org.cloudbus.cloudsim.examples.pso;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;

public class Allocation {
    
    public Cloudlet cloudlet;
    public PowerVm vm;
    public PowerHost host;

    public Allocation(Cloudlet cloudlet, PowerVm vm, PowerHost host) {
        this.cloudlet = cloudlet;
        this.vm = vm;
        this.host = host;
    }
    
    public Cloudlet getCloudlet() {
        return cloudlet;
    }
    public void setCloudlet(Cloudlet cloudlet) {
        this.cloudlet = cloudlet;
    }
    public PowerVm getVm() {
        return vm;
    }
    public void setVm(PowerVm vm) {
        this.vm = vm;
    }
    public PowerHost getHost() {
        return host;
    }
    public void setHost(PowerHost host) {
        this.host = host;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((cloudlet == null) ? 0 : cloudlet.hashCode());
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
        if (cloudlet == null) {
            if (other.cloudlet != null)
                return false;
        } else if (!cloudlet.equals(other.cloudlet))
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
