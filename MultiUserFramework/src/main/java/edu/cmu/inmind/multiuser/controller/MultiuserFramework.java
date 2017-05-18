package edu.cmu.inmind.multiuser.controller;

import edu.cmu.inmind.multiuser.controller.communication.ServiceInfo;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.controller.resources.Config;
import edu.cmu.inmind.multiuser.controller.session.SessionManager;

/**
 * Created by oscarr on 3/20/17.
 */
public class MultiuserFramework{
    private SessionManager sessionManager;
    private static MultiuserFramework instance;

    private MultiuserFramework( SessionManager sessionManager){
        this.sessionManager = sessionManager;
    }

    public static void start( PluginModule[] modules, Config config, ServiceInfo serviceInfo ){
        if( instance == null ){
            Runtime.getRuntime().addShutdownHook(new Thread("shutdown hook in Multiuser framework") {
                public void run() {
                    try {
                        MultiuserFramework.stop();
                    }catch (Exception e){
                        ExceptionHandler.handle(e);
                    }
                }
            });
            instance = new MultiuserFramework( new SessionManager( modules, config, serviceInfo ) );
        }
        instance.sessionManager.start();
    }

    public static void start( PluginModule[] modules, Config config ){
        start( modules, config, null);
    }

    public static void stop(){
        try {
            instance.sessionManager.stop();
        }catch (Exception e){
            ExceptionHandler.handle(e);
        }
    }
}
