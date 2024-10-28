package org.cloudbus.cloudsim.pso;


import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.VmList;
import org.cloudbus.cloudsim.power.PowerDatacenterBroker;

/**
* 
* @author Carlos A. Mendez R.
* @since 2024
*/
public class PSODatacenterBroker extends PowerDatacenterBroker {

    /**
     * Instantiates a new PowerDatacenterBroker.
    * 
    * @param name the name of the broker
    * @throws Exception the exception
    */
    public PSODatacenterBroker(String name) throws Exception {
        super(name);
    }

}

