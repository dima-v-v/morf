/*
 * The morf project
 * 
 * Copyright (c) 2015 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ubc.pavlab.morf.beans;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

import org.apache.log4j.Logger;

import ubc.pavlab.morf.models.Job;
import ubc.pavlab.morf.models.PurgeOldJobs;
import ubc.pavlab.morf.service.SessionIdentifierGenerator;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@ManagedBean(eager = true)
@ApplicationScoped
public class JobManager {

    private static final Logger log = Logger.getLogger( JobManager.class );

    // Contains map of random token to saved job for future viewing
    private Map<String, Job> savedJobs = new HashMap<>();

    // Used to create new save tokens
    private final SessionIdentifierGenerator sig = new SessionIdentifierGenerator();

    // Used to periodically purge the old saved jobs
    private ScheduledExecutorService scheduler;
    public static final long PURGE_AFTER = 86400000;

    // Contains a representation of the internal queue of jobs
    private LinkedList<Job> jobs = new LinkedList<Job>();

    // private ExecutorService processJob;
    // private ThreadPoolExecutor executor;
    private ExecutorService executor;

    // private ScheduledExecutorService produceJobScheduler;

    @PostConstruct
    public void init() {
        executor = Executors.newSingleThreadExecutor();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        // old after 1 day, checks every hour
        scheduler.scheduleAtFixedRate( new PurgeOldJobs( savedJobs ), 0, 1, TimeUnit.HOURS );
        // executor = (ThreadPoolExecutor) Executors.newSingleThreadExecutor();
    }

    @PreDestroy
    public void destroy() {
        log.info( "JobManager destroyed" );
        // processJob.shutdownNow();
        executor.shutdownNow();
        scheduler.shutdownNow();
    }

    public boolean cancelJob( Job job ) {
        boolean canceled = false;
        synchronized ( jobs ) {
            Future<String> future = job.getFuture();
            canceled = future.cancel( true );

            if ( canceled ) {
                jobs.remove( job );
            }
        }
        return canceled;
    }

    /**
     * Attempts to remove job from jobs list, if job is not yet complete this will fail. See @cancelJob
     * 
     * @param job
     * @return True if job is removed
     */
    public boolean removeJob( Job job ) {
        boolean removed = false;
        if ( job.getComplete() ) {
            synchronized ( jobs ) {
                jobs.remove( job );
                removed = true;
            }
        }
        return removed;
    }

    public Job submit( Job job ) {
        // TODO is synchronized necessary?
        synchronized ( jobs ) {
            log.info( "Submitting job (" + job.getId() + ") for session: (" + job.getSessionId() + ") and IP: ("
                    + job.getIpAddress() + ")" );
            job.setJobManager( this );
            Future<String> future = executor.submit( job );
            job.setFuture( future );
            jobs.add( job );
            job.setPosition( Integer.toString( jobs.size() ) );
        }
        return job;
    }

    public void updatePositions() {
        synchronized ( jobs ) {
            int idx = 1;

            for ( Iterator<Job> iterator = jobs.iterator(); iterator.hasNext(); ) {
                Job job = iterator.next();

                if ( job.getRunning() ) {
                    job.setPosition( "Running..." );
                    idx++;
                } else if ( job.getComplete() ) {
                    job.setPosition( "Done" );
                    iterator.remove();
                } else {
                    job.setPosition( Integer.toString( idx ) );
                    idx++;
                }

            }
            log.info( String.format( "Jobs in queue: %d", jobs.size() ) );
        }
    }

    public Job fetchSavedJob( String key, boolean remove ) {
        synchronized ( savedJobs ) {
            if ( remove ) {
                return savedJobs.remove( key );
            } else {
                return savedJobs.get( key );
            }
        }
    }

    public String saveJob( Job job ) {
        synchronized ( savedJobs ) {
            String key = sig.nextSessionId();
            job.setSavedKey( key );
            job.setSaved( true );
            job.setSaveExpiredDate( System.currentTimeMillis() + JobManager.PURGE_AFTER );
            savedJobs.put( key, job );
            return key;
        }
    }

}
