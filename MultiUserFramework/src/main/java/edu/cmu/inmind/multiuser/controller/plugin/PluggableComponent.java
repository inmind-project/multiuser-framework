package edu.cmu.inmind.multiuser.controller.plugin;

import com.google.common.util.concurrent.AbstractIdleService;
import edu.cmu.inmind.multiuser.common.DestroyableCallback;
import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.ErrorMessages;
import edu.cmu.inmind.multiuser.controller.blackboard.Blackboard;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardListener;
import edu.cmu.inmind.multiuser.controller.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.communication.ResponseListener;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.log.MessageLog;
import edu.cmu.inmind.multiuser.controller.session.Session;
import edu.cmu.inmind.multiuser.controller.sync.SynchronizableEvent;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by oscarr on 3/16/17.
 */
@StateType( state = Constants.STATEFULL )
public abstract class PluggableComponent extends AbstractIdleService implements BlackboardListener, Pluggable,
        DestroyableCallback {
    private ConcurrentHashMap<String, Blackboard> blackboards;
    protected ConcurrentHashMap<String, MessageLog> messageLoggers;
    protected ConcurrentHashMap<String, Session> sessions;
    private Session activeSession;
    private boolean isShutDown;
    private ClientCommController clientCommController;
    private CopyOnWriteArrayList<DestroyableCallback> callbacks;

    public PluggableComponent(){
        blackboards = new ConcurrentHashMap<>();
        messageLoggers = new ConcurrentHashMap<>();
        sessions = new ConcurrentHashMap<>();//a component may be shared by several sessions (Stateless)
        callbacks = new CopyOnWriteArrayList();
        isShutDown = false;
    }


    public void addBlackboard(String sessionId, Blackboard blackboard) {
        if( blackboards == null || sessionId == null || blackboard == null ){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "blackboards: "+blackboards,
                    "sessionId: " + sessionId, "blackboard: " + blackboard));
        }
        blackboards.put(sessionId, blackboard);
    }

    public Session getSession() throws Throwable{
        checkActiveSession();
        return activeSession;
    }

    public void postCreate(){
        // do something after creation of this component
    }


    /** ================================================ START OVERRIDE ============================================ **/

    /**
     * Super: Pluggable interface
     */
    @Override
    public void execute(){
        //do nothing
    }

    /**
     * Super: BlackboardListener interface
     */
    @Override
    public abstract void onEvent(BlackboardEvent event) throws Throwable;


    /**
     * Super: AbstractExecutionThreadService class (GUAVA)
     */
    @Override
    protected void startUp() {
        Log4J.info(this, "Starting up component: " + this.getClass().getSimpleName() +
                " on session: " + checkActiveSession().getId() );
    }

    /**
     * Super: AbstractExecutionThreadService class (GUAVA)
     */
    @Override
    public void shutDown() {
        Log4J.info(this, "Shutting down component: " + this.getClass().getSimpleName() +
                " instantiation " + this.hashCode());
        isShutDown = true;
        if(blackboards != null) blackboards.clear();
        blackboards = null;
        if(messageLoggers != null) messageLoggers.clear();
        messageLoggers = null;
    }

    /**
     * Super: BlackboardListener interface
     */
    @Override
    public String getSessionId(){
        checkActiveSession();
        return activeSession.getId();
    }

    /** ================================================ END OVERRIDE ============================================ **/


    public Blackboard blackboard(){
        Blackboard bb = null;
        if( activeSession != null && activeSession.getId() != null && blackboards != null ) {
            bb = blackboards.get(activeSession.getId());
        }else{
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL,
                    "blackboards: " + blackboards, "activeSession: " + activeSession));
        }
        //TODO: why blackboard is null?
        if( bb == null ){
            bb = new Blackboard( getMessageLogger() );
        }
        return bb;
    }

    public void setClientCommController(ClientCommController clientCommController) {
        this.clientCommController = clientCommController;
    }

    public ClientCommController getClientCommController() {
        return clientCommController;
    }


    public void send( SessionMessage sessionMessage ){
        send( sessionMessage, true );
    }

    public void send( SessionMessage sessionMessage , boolean shouldProcessReply ){
        try {
            if (clientCommController == null) {
                throw new MultiuserException( ErrorMessages.NO_REMOTE_ANNOTATION, getClass().getSimpleName() );
            }
            Log4J.debug(this, "4.1. shouldProcessRequest: " + shouldProcessReply);
            clientCommController.setShouldProcessReply( shouldProcessReply );
            clientCommController.send( getSessionId(), sessionMessage);
        }catch (Throwable e){
            ExceptionHandler.handle( e );
        }
    }

    public void receive(ResponseListener responseListener){
        try {
            if (clientCommController == null) {
                throw new MultiuserException(ErrorMessages.NO_REMOTE_ANNOTATION, getClass().getSimpleName());
            }
            clientCommController.setResponseListener(responseListener);
        }catch (Throwable e){
            ExceptionHandler.handle( e );
        }
    }

    public void setActiveSession(Session activeSession){
        this.activeSession = activeSession;
    }

    public void setActiveSession(String sessionId){
        this.activeSession = sessions.get( sessionId );
    }

    public MessageLog getMessageLogger(){
        try {
            checkActiveSession();
            return messageLoggers.get( activeSession.getId() );
        }catch (Throwable e){
            ExceptionHandler.handle(e);
        }
        return null;
    }

    private Session checkActiveSession(){
        if (activeSession == null){
            if( sessions != null && sessions.size() > 0 ){
                activeSession = new ArrayList<>( sessions.values() ).get( sessions.size() - 1 );
            }else {
                ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "activeSession: "
                        + activeSession, "sessions: " + sessions) );
            }
        }
        return activeSession;
    }

    public void addMessageLogger(String sessionId, MessageLog messageLogger) {
        if( messageLoggers == null || sessionId == null || sessionId.isEmpty() || messageLogger == null ){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL,
                    "messageLoggers: " + messageLoggers, "sessionId: " + sessionId, "messagaLogger: " + messageLogger) );
        }
        messageLoggers.put(sessionId, messageLogger);
    }

    public void addSession(Session session){
        if( session == null || sessions == null ){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL,
                    "session: " + session, "sessions: " + sessions) );
        }
        sessions.put(session.getId(), session);
    }

    public void close(String sessionId, DestroyableCallback callback) throws Throwable{
        callbacks.add(callback);
        Session currentSession = sessions.remove( sessionId );
        if( clientCommController != null ) {
            clientCommController.send(currentSession.getId(), new SessionMessage(Constants.SESSION_CLOSED));
            // if this is a stateless component, we can only close it if all sessions have stopped
            if (sessions.isEmpty()) {
                clientCommController.close(this);
            }
        }else{
            destroyInCascade(this);
        }
    }

    @Override
    public void destroyInCascade(Object destroyedObj) throws Throwable{
        for(DestroyableCallback callback : callbacks){
            callback.destroyInCascade( this );
        }
    }

    public void notifyNext(PluggableComponent component){
        try {
            if( blackboards == null || component == null || component.getSessionId() == null ){
                ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL,
                        "blackboards: " + blackboards, "component: " + component, "sessionId: " +
                        component != null? component.getSessionId() : null) );
            }
            SynchronizableEvent next = blackboards.get(component.getSessionId()).getSyncEvent(component);
            if (next != null) {
                next.notifyNext();
            }
        }catch (Throwable e){
            ExceptionHandler.handle( e );
        }
    }
}
