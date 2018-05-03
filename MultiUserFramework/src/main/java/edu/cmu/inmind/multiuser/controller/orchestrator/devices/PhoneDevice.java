package edu.cmu.inmind.multiuser.controller.orchestrator.devices;

import edu.cmu.inmind.multiuser.controller.orchestrator.bn.BehaviorNetwork;

/**
 * Created by oscarr on 4/26/18.
 */
public class PhoneDevice extends Device {
    public PhoneDevice(String name, BehaviorNetwork network){
        super(name, network);
    }

    @Override
    public synchronized void executeService(String serviceName){
        System.out.println(String.format("*** %s device is executing service: %s", name, serviceName));
        super.executeService(serviceName);
    }
}
