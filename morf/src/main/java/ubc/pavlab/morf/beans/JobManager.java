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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

import org.apache.log4j.Logger;

import ubc.pavlab.morf.models.Job;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@ManagedBean(eager = true)
@ApplicationScoped
public class JobManager {

	private static final Logger log = Logger.getLogger(JobManager.class);

	private boolean running;
	private LinkedList<Job> jobs = new LinkedList<Job>();

	// private ExecutorService processJob;
	// private ThreadPoolExecutor executor;
	private ExecutorService executor;

	// private ScheduledExecutorService produceJobScheduler;

	@PostConstruct
	public void init() {
		executor = Executors.newSingleThreadExecutor();
		// executor = (ThreadPoolExecutor) Executors.newSingleThreadExecutor();
	}

	@PreDestroy
	public void destroy() {
		log.info("JobManager destroyed");
		// processJob.shutdownNow();
		executor.shutdownNow();
	}

	public boolean isRunning() {
		return running;
	}

	public Job submit(Job job) {

		// Future<String> future = processJob.submit(job);
		// TODO is synchronized necessary?
		synchronized (jobs) {
			log.info("Submitting Job: " + job.getName() + " for session: (" + job.getSessionId() + ")");
			Future<String> future = executor.submit(job);
			job.setFuture(future);
			jobs.add(job);
		}
		return job;
	}

	// This needs to be optimized
	public Integer queuePosition(Job job) {
		if (jobs.contains(job)) {
			int idx = 1;
			for (Iterator<Job> iterator = jobs.iterator(); iterator.hasNext();) {
				Job j = (Job) iterator.next();
				if (j.equals(job)) {
					// log.info("Position of (" + job.getName() + ") in queue: " + idx);
					return idx;
				} else if (!j.getFuture().isDone()) {
					// loop through queue increment counter for every not done job before the given job
					idx++;
				}

			}
		}

		log.info("Position of (" + job.getName() + ") in queue: NOT_FOUND");
		return null;
	}
}
