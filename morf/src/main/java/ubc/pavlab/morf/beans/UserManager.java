/**
 * 
 */
package ubc.pavlab.morf.beans;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import ubc.pavlab.morf.models.Job;

/**
 * @author mjacobson
 *
 */
@ManagedBean
@SessionScoped
public class UserManager implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3568877185808646254L;
	private static final Logger log = Logger.getLogger(UserManager.class);

	@ManagedProperty(value = "#{jobManager}")
	private JobManager jobManager;

	private Map<String, Job> results = new HashMap<String, Job>();
	private Set<Job> jobs = new HashSet<Job>();

	public UserManager() {
		log.info("UserManager created");
	}

	@PostConstruct
	public void init() {
		log.info("UserManager init");
	}

	private String getSessionId() {
		FacesContext fCtx = FacesContext.getCurrentInstance();
		HttpSession session = (HttpSession) fCtx.getExternalContext()
				.getSession(false);
		return session.getId();
	}

	public void createJob(String name, String contents) {
		Job job = new Job(getSessionId(), name, contents);
		jobManager.putJob(job);
	}

	void submitJob(String name, String content) {
		Job job = jobManager.submit(getSessionId(), name, content);
		jobs.add(job);
		results.put(name, job);
	}

	String getResultIfReady(String name) {
		Future<String> res = results.get(name).getFuture();
		if (res == null) {
			return "Cannot find job queued under name (" + name + ")";
		} else if (res.isDone()) {
			try {
				return res.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			return "Result isn't ready for (" + name + ")";
		}
	}

	public Set<Job> getJobs() {
		return jobs;
	}

	boolean jobExists(String name) {
		return this.results.containsKey(name);
	}

	public void setJobManager(JobManager jobManager) {
		this.jobManager = jobManager;
	}

}
