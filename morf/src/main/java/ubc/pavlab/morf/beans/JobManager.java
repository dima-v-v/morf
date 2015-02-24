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

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

import org.apache.log4j.Logger;

import ubc.pavlab.morf.models.Job;
import ubc.pavlab.morf.runnables.MyCallable;

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

	private LinkedBlockingQueue<Job> queue = new LinkedBlockingQueue<Job>();
	private boolean running;

	private ExecutorService processJob;

	// private ScheduledExecutorService produceJobScheduler;

	@PostConstruct
	public void init() {
		processJob = Executors.newSingleThreadExecutor();

		// processJobScheduler.scheduleAtFixedRate(new ProcessJob(queue), 10, 5,
		// TimeUnit.SECONDS);

		// produceJobScheduler = Executors.newSingleThreadScheduledExecutor();
		// produceJobScheduler.scheduleAtFixedRate( new ProduceJob(
		// jobQueue.getQueue() ), 10, 10, TimeUnit.SECONDS );
	}

	@PreDestroy
	public void destroy() {
		log.info("JobManager destroyed");
		processJob.shutdownNow();
	}

	public boolean isRunning() {
		return running;
	}

	public void putJob(Job job) {
		try {
			this.queue.put(job);
			log.info("Adding Job: " + job.toString());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	public Job submit(String sessionId, String name, String content) {
		log.info("Submitting Job: " + name);
		MyCallable callable = new MyCallable(5000, name, content);
		Future<String> future = processJob.submit(callable);
		Job job = new Job(sessionId, name, content);
		job.setSubmitted(new Date());
		job.setFuture(future);
		return job;
	}

	public LinkedBlockingQueue<Job> getQueue() {
		return queue;
	}

}
