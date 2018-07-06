package edu.cmu.inmind.multiuser.controller.composer.services;

import edu.cmu.inmind.multiuser.controller.composer.bn.Behavior;

import java.util.Arrays;
import java.util.concurrent.ConcurrentSkipListSet;

import static edu.cmu.inmind.multiuser.controller.composer.simulation.SimuConstants.S12_ALICE_DO_GROCERY;


/**
 * Created by oscarr on 5/24/18.
 */
public class DoGroceryShoppingService extends Service {

    public DoGroceryShoppingService(String deviceName, Behavior behavior, ConcurrentSkipListSet<String> state){
        super(deviceName, behavior, state);
    }

    @Override
    public boolean execute(Object... params) {
        network.triggerPostconditions(behavior, Arrays.asList("grocery-shopping-done"));
        return true;
    }
}
