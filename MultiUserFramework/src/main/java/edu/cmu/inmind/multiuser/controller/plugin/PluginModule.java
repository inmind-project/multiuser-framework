package edu.cmu.inmind.multiuser.controller.plugin;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;
import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.ErrorMessages;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.log.Loggable;
import edu.cmu.inmind.multiuser.controller.log.LoggerInterceptor;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestrator;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestratorFactory;
import edu.cmu.inmind.multiuser.controller.resources.ResourceLocator;

import java.util.ArrayList;
import java.util.List;

import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;

/**
 * Created by oscarr on 3/7/17.
 */
public class PluginModule extends AbstractModule {

    private Multibinder<PluggableComponent> pluginBinder;
    private List<ModuleComponent> plugableComponents;
    private Class<? extends ProcessOrchestrator> orchestrator;

    private PluginModule(Builder builder){
        this.plugableComponents = builder.plugableComponents;
        this.orchestrator = builder.orchestrator;
    }

    private void addPlugin( ModuleComponent moduleComponent ) throws Throwable{
        Class<? extends PluggableComponent> component = moduleComponent.component;
        String message = moduleComponent.message;
        int numOfInstances = moduleComponent.numOfInstances;
        int numBonds = 0;

        if (message != null && component != null) {
            ResourceLocator.addMsgMapping(message, component);
        }

        if (component.isAnnotationPresent(StateType.class)) {
            String state = component.getAnnotation(StateType.class).state();
            if ( state.equals(Constants.STATELESS) ){
                pluginBinder.addBinding().to(component).in(Singleton.class);
                numBonds++;
            }
            else if ( state.equals(Constants.STATEFULL ) ){
                pluginBinder.addBinding().to(component);
                numBonds++;
            }
            else if ( state.equals(Constants.POOL ) ){
                pluginBinder.addBinding().toProvider(new ComponentPoolProvider(component, numOfInstances));
                numBonds++;
            }
        }
        if( numBonds == 1 ){
            return;
        }else if( numBonds > 1 ){
            throw new MultiuserException(ErrorMessages.COMP_MUTUAL_EXCLUSIVE);
        }else {
            throw new MultiuserException(ErrorMessages.COMP_NO_ANNOTATIONS);
        }
    }

    private void addOrchestrator(Class<? extends ProcessOrchestrator> clazz){
        install(new FactoryModuleBuilder()
                .implement(ProcessOrchestrator.class, clazz)
                .build(ProcessOrchestratorFactory.class));
    }


    @Override
    protected void configure(){
        try {
            pluginBinder = Multibinder.newSetBinder(binder(), PluggableComponent.class);
            for (ModuleComponent component : plugableComponents) {
                addPlugin(component);
            }
            addOrchestrator(orchestrator);
            addInterceptors();
        }catch (Throwable e){
            ExceptionHandler.handle(e);
        }
    }

    private void addInterceptors() {
        bindInterceptor( any(), annotatedWith(Interceptable.class), new ExecutionInterceptor() );
        bindInterceptor( any(), annotatedWith(Loggable.class), new LoggerInterceptor() );
    }

    /****************************** BUILDER PATTERN ******************************************/

    public static class Builder {
        private List<ModuleComponent> plugableComponents;
        private int defaultNumOfInstances = 100;
        private Class<? extends ProcessOrchestrator> orchestrator;


        /**
         * You need to specify at least one plugabble component. Use Builder(ProcessOrchestrator, PluggableComponent,
         * String) instead
         * @param orchestrator
         */
        @Deprecated
        public Builder(Class<? extends ProcessOrchestrator> orchestrator) {
            this.orchestrator = orchestrator;
        }

        public Builder(Class<? extends ProcessOrchestrator> orchestrator, Class<? extends PluggableComponent> component,
                       String message) {
            this.orchestrator = orchestrator;
            addPlugin( component, message );
        }

        public Builder addPlugin(Class<? extends PluggableComponent> component, int numOfInstances, String message ){
            if( plugableComponents == null ){
                plugableComponents = new ArrayList<>();
            }
            plugableComponents.add( new ModuleComponent(component, numOfInstances, message) );
            return this;
        }

        public Builder addPlugin(Class<? extends PluggableComponent> component, String mapping ){
            return addPlugin( component, defaultNumOfInstances, mapping );
        }

        public Builder addPlugin( String className, int numOfInstances, String mapping ) throws Throwable{
            Class component = Class.forName(className);
            return addPlugin (component, numOfInstances, mapping );
        }

        public Builder addPlugin( String className, String mapping ){
            try {
                return addPlugin(className, defaultNumOfInstances, mapping);
            }catch (Throwable e){
                ExceptionHandler.handle(e);
            }
            return null;
        }

        public PluginModule build(){
            return new PluginModule( this );
        }
    }

    static class ModuleComponent{
        private Class<? extends PluggableComponent> component;
        private int numOfInstances;
        private String message;

        public ModuleComponent(Class<? extends PluggableComponent> component, int numOfInstances, String message) {
            this.component = component;
            this.numOfInstances = numOfInstances;
            this.message = message;
        }
    }
}
