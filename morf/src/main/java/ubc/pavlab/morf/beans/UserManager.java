/**
 * 
 */
package ubc.pavlab.morf.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.primefaces.context.RequestContext;

import ubc.pavlab.morf.models.Job;

/**
 * @author mjacobson
 */
@ManagedBean
@SessionScoped
public class UserManager implements Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = -3568877185808646254L;
    private static final Logger log = Logger.getLogger( UserManager.class );
    private int MAX_JOBS_IN_QUEUE = 2;
    private Object jobSubmitLock = new Object();
    private Boolean stopPolling = true;

    @ManagedProperty(value = "#{jobManager}")
    private JobManager jobManager;

    @ManagedProperty(value = "#{settingsCache}")
    private SettingsCache settingsCache;

    // private Map<String, Job> results = new HashMap<String, Job>();
    private List<Job> jobs = new ArrayList<Job>();
    private Queue<Job> jobQueue = new LinkedList<Job>();

    public UserManager() {
        log.info( "UserManager created" );
    }

    @PostConstruct
    public void init() {
        log.info( "UserManager init" );
        MAX_JOBS_IN_QUEUE = Integer.parseInt( settingsCache.getProperty( "morf.maxJobsInQueue" ) );
    }

    String getSessionId() {
        FacesContext fCtx = FacesContext.getCurrentInstance();
        HttpSession session = ( HttpSession ) fCtx.getExternalContext().getSession( false );
        return session.getId();
    }

    void submitJob( Job job ) {
        // log.info("Starting polling");
        // stopPolling = false;
        if ( !jobs.contains( job ) ) {
            jobs.add( job );
            jobQueue.add( job );
        }
        submitJobFromQueue();

        // results.put(job.getName(), job);
    }

    private void submitJobFromQueue() {
        synchronized ( jobSubmitLock ) {
            if ( runningJobs() < MAX_JOBS_IN_QUEUE ) {
                Job job = jobQueue.poll();
                if ( job != null ) {
                    job.setSubmittedDate( new Date() );
                    jobManager.submit( job );
                }
            }
        }
    }

    private int runningJobs() {
        int cnt = 0;
        for ( Job job : jobs ) {
            if ( job.getSubmittedDate() != null && job.getFuture() != null && !job.getFuture().isDone() ) cnt++;
        }
        return cnt;
    }

    /*
     * String getResultIfReady(String name) { Future<String> res = results.get(name).getFuture(); if (res == null) {
     * return "Cannot find job queued under name (" + name + ")"; } else if (res.isDone()) { try { return res.get(); }
     * catch (InterruptedException | ExecutionException e) { e.printStackTrace(); return null; } } else { return
     * "Result isn't ready for (" + name + ")"; } }
     */

    public synchronized void updateQueuePositions() {

        for ( int i = 0; i < MAX_JOBS_IN_QUEUE; i++ ) {
            submitJobFromQueue();
        }
        boolean somethingIsRunning = false;
        for ( Job job : jobs ) {
            Future<String> future = job.getFuture();
            if ( future == null ) {
                somethingIsRunning = true;
                job.setPosition( "Pending..." );
            } else if ( future.isDone() ) {
                job.setPosition( "Done" );
            } else {
                somethingIsRunning = true;
                job.setPosition( jobManager.queuePosition( job ).toString() );
            }

        }
        if ( !somethingIsRunning ) {
            // log.info("Stopping polling");
            // stopPolling = true;
            RequestContext.getCurrentInstance().addCallbackParam( "stopPolling", true );
        }
    }

    public List<Job> getJobs() {
        return jobs;
    }

    boolean jobExists( Job job ) {
        return this.jobs.contains( job );
    }

    public void setJobManager( JobManager jobManager ) {
        this.jobManager = jobManager;
    }

    public void setSettingsCache( SettingsCache settingsCache ) {
        this.settingsCache = settingsCache;
    }

    public Boolean getStopPolling() {
        return stopPolling;
    }

}
