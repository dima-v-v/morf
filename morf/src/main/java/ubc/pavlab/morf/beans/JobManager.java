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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;

import org.apache.log4j.Logger;

import ubc.pavlab.morf.runnables.ProcessJob;
import ubc.pavlab.morf.runnables.ProduceJob;

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

    @ManagedProperty(value = "#{jobQueue}")
    JobQueue jobQueue;

    private ScheduledExecutorService processJobScheduler;
    private ScheduledExecutorService produceJobScheduler;

    @PostConstruct
    public void init() {
        processJobScheduler = Executors.newSingleThreadScheduledExecutor();
        processJobScheduler.scheduleAtFixedRate( new ProcessJob( jobQueue.getQueue() ), 10, 5, TimeUnit.SECONDS );

        produceJobScheduler = Executors.newSingleThreadScheduledExecutor();
        produceJobScheduler.scheduleAtFixedRate( new ProduceJob( jobQueue.getQueue() ), 10, 10, TimeUnit.SECONDS );
    }

    @PreDestroy
    public void destroy() {
        log.info( "JobManager destroyed" );
        processJobScheduler.shutdownNow();
    }

    public void setJobQueue( JobQueue jobQueue ) {
        this.jobQueue = jobQueue;
    }

}
