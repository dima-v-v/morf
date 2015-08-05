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

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
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
    private Boolean authenticated = true;
    private Integer jobIdIncrementer = 0;

    @ManagedProperty(value = "#{jobManager}")
    private JobManager jobManager;

    @ManagedProperty(value = "#{settingsCache}")
    private SettingsCache settingsCache;

    @ManagedProperty(value = "#{security}")
    private Security security;

    // private Map<String, Job> results = new HashMap<String, Job>();
    private List<Job> jobs = new ArrayList<Job>(); // All jobs
    private Queue<Job> jobQueue = new LinkedList<Job>(); // All jobs yet to be submitted

    public UserManager() {
        log.info( "UserManager created" );
    }

    @PostConstruct
    public void init() {
        log.info( "UserManager init" );
        MAX_JOBS_IN_QUEUE = Integer.parseInt( settingsCache.getProperty( "morf.maxJobsInQueue" ) );
        authenticated = StringUtils.isBlank( settingsCache.getProperty( "morf.password" ) );
    }

    public String saveJob( Job job ) {
        return jobManager.saveJob( job );
    }

    public int getNewJobId() {
        synchronized ( jobIdIncrementer ) {
            return jobIdIncrementer++;
        }
    }

    String getSessionId() {
        FacesContext fCtx = FacesContext.getCurrentInstance();
        HttpSession session = ( HttpSession ) fCtx.getExternalContext().getSession( false );
        return session.getId();
    }

    void addFailedJob( Job job, String failedMessage ) {
        if ( !jobs.contains( job ) ) {
            job.setComplete( true );
            job.setFailed( true );
            job.setStatus( failedMessage );
            jobs.add( job );
        }
    }

    void submitJob( Job job ) {
        // log.info("Starting polling");
        // stopPolling = false;
        if ( !jobs.contains( job ) ) {
            jobs.add( job );
            jobQueue.add( job );
            job.setStatus( "Submission Pending..." );
        }
        submitJobFromQueue();

        // results.put(job.getName(), job);
    }

    public boolean cancelJob( Job job ) {
        boolean canceled = false;
        synchronized ( jobSubmitLock ) {
            if ( jobQueue.contains( job ) ) {
                // Not yet submitted, just remove it from queue and job list
                jobQueue.remove( job );
                jobs.remove( job );
                canceled = true;
            } else if ( jobs.contains( job ) ) {
                if ( job.getComplete() ) {
                    canceled = jobManager.removeJob( job );
                } else {
                    canceled = false;
                    //canceled = jobManager.cancelJob( job ); // Off for now because doesn't work
                }
                if ( canceled ) {
                    jobs.remove( job );
                }
                // already submitted must send cancel request
            }
        }
        return canceled;
    }

    private void submitJobFromQueue() {
        synchronized ( jobSubmitLock ) {
            if ( submittedAndIncompleteJobs() < MAX_JOBS_IN_QUEUE ) {
                Job job = jobQueue.poll();
                if ( job != null ) {
                    job.setSubmittedDate( new Date() );
                    jobManager.submit( job );
                }
            }
        }
    }

    private int submittedAndIncompleteJobs() {
        int cnt = 0;
        for ( Job job : jobs ) {
            if ( isSubmittedAndIncomplete( job ) ) cnt++;
        }
        return cnt;
    }

    private boolean isSubmittedAndIncomplete( Job job ) {
        return job.getSubmittedDate() != null && !job.getComplete();
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
            if ( !job.getComplete() ) {
                somethingIsRunning = true;
            }
        }
        if ( !somethingIsRunning ) {
            // log.info("Stopping polling");
            // stopPolling = true;
            RequestContext.getCurrentInstance().addCallbackParam( "stopPolling", true );
        }
    }

    public void authenticate( String password ) {
        synchronized ( authenticated ) {
            authenticated = security.checkPassword( password );
            log.info( "authenticated: " + authenticated );
            if ( !authenticated ) {
                FacesMessage message = new FacesMessage( FacesMessage.SEVERITY_WARN, "Incorrect Password!", null );
                FacesContext.getCurrentInstance().addMessage( null, message );
                // Brute force authentication delay
                try {
                    Thread.sleep( 3000 );
                } catch ( InterruptedException e ) {
                    log.error( "Authentication Delay Interrupted", e );
                }
            }
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

    public void setSecurity( Security security ) {
        this.security = security;
    }

    public Boolean getStopPolling() {
        return stopPolling;
    }

    public Boolean getAuthenticated() {
        return authenticated;
    }

}
