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

package ubc.pavlab.morf.runnables;

import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import ubc.pavlab.morf.models.Job;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class ProduceJob implements Runnable {

    private static final Logger log = Logger.getLogger( ProcessJob.class );

    private LinkedBlockingQueue<Job> queue;

    public ProduceJob( LinkedBlockingQueue<Job> queue ) {
        super();
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            int randomNum = 1 + ( int ) ( Math.random() * 100 );
            Job job = new Job( randomNum, String.valueOf( randomNum ) );
            this.queue.put( job );
            log.info( "Adding Job: " + job.toString() );
        } catch ( InterruptedException e ) {
            e.printStackTrace();
        }
    }
}
