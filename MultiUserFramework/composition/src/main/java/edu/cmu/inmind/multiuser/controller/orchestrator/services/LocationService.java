package edu.cmu.inmind.multiuser.controller.orchestrator.services;

import edu.cmu.inmind.multiuser.controller.orchestrator.bn.Behavior;

import java.util.Arrays;
import java.util.concurrent.ConcurrentSkipListSet;


/**
 * Created by oscarr on 5/22/18.
 */
public class LocationService extends Service {

    public LocationService(String deviceName, Behavior behavior, ConcurrentSkipListSet<String> state){
        super(deviceName, behavior, state);
    }

    @Override
    public boolean execute(int simulationStep) {
        network.triggerPostconditions(behavior);
        return true;
    }
}
