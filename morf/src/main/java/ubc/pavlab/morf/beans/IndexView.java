package ubc.pavlab.morf.beans;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.apache.log4j.Logger;

import ubc.pavlab.morf.models.Job;

@ManagedBean
@ViewScoped
public class IndexView implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2438909388677798292L;

	private static final Logger log = Logger.getLogger(IndexView.class);

	@ManagedProperty(value = "#{userManager}")
	private UserManager userManager;

	// private String currentSelectedName;
	private String name;
	private String content;
	private Job selectedJob;
	private Job jobToRemove;
	private Integer exampleCnt = 1;

	public IndexView() {
		log.info("IndexView created");
	}

	@PostConstruct
	public void init() {
		log.info("IndexView init");
	}

	/*
	 * public void getResult(ActionEvent actionEvent) { String res = userManager.getResultIfReady(currentSelectedName);
	 * if (res != null) { addMessage(res); } else { addMessage("Something went wrong!"); } }
	 */

	public void cancelJob() {

		if (jobToRemove.getComplete()) {
			addMessage("Job (" + jobToRemove.getName() + ") is already complete, cannot cancel.",
					FacesMessage.SEVERITY_INFO);
		} else if (userManager.jobExists(jobToRemove)) {
			boolean canceled = userManager.cancelJob(jobToRemove);
			if (canceled) {
				addMessage("Job (" + jobToRemove.getName() + ") successfully cancelled.", FacesMessage.SEVERITY_INFO);
			} else {
				addMessage("Job (" + jobToRemove.getName() + ") failed to cancel.", FacesMessage.SEVERITY_WARN);
			}
		} else {
			addMessage("Cannot find job (" + jobToRemove.getName() + ")", FacesMessage.SEVERITY_ERROR);

		}
	}

	public void submitJob(ActionEvent actionEvent) {

		Job job = new Job(userManager.getSessionId(), name, content);
		if (userManager.jobExists(job)) {
			addMessage("Job already exists under name (" + name + ")", FacesMessage.SEVERITY_WARN);
		} else {
			addMessage("Job submitted for (" + name + ")", FacesMessage.SEVERITY_INFO);
			userManager.submitJob(job);
			// RequestContext.getCurrentInstance().addCallbackParam("stopPolling", false);
		}

	}

	public void submitMultipleJob(ActionEvent actionEvent) {
		for (int i = 0; i < 5; i++) {
			Job job = new Job(userManager.getSessionId(), exampleCnt.toString(), exampleCnt.toString());
			if (userManager.jobExists(job)) {
				addMessage("Job already exists under name (" + exampleCnt.toString() + ")", FacesMessage.SEVERITY_WARN);
				exampleCnt++;
			} else {
				addMessage("Job submitted for (" + exampleCnt.toString() + ")", FacesMessage.SEVERITY_INFO);
				userManager.submitJob(job);
				exampleCnt++;
			}

		}

	}

	private void addMessage(String summary, FacesMessage.Severity severity) {
		FacesMessage message = new FacesMessage(severity, summary, null);
		FacesContext.getCurrentInstance().addMessage(null, message);
	}

	/*
	 * public String getCurrentSelectedName() { return currentSelectedName; }
	 * 
	 * public void setCurrentSelectedName(String currentSelectedName) { this.currentSelectedName = currentSelectedName;
	 * }
	 */

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content.trim() + "\r\n";
	}

	public void setUserManager(UserManager userManager) {
		this.userManager = userManager;
	}

	public Job getSelectedJob() {
		return selectedJob;
	}

	public void setSelectedJob(Job selectedJob) {
		this.selectedJob = selectedJob;
	}

	public Job getJobToRemove() {
		return jobToRemove;
	}

	public void setJobToRemove(Job jobToRemove) {
		this.jobToRemove = jobToRemove;
	}

}
