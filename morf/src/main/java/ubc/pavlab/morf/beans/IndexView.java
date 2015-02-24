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

	public IndexView() {
		log.info("IndexView created");
	}

	@PostConstruct
	public void init() {
		log.info("IndexView init");
	}

	/*
	 * public void getResult(ActionEvent actionEvent) { String res =
	 * userManager.getResultIfReady(currentSelectedName); if (res != null) {
	 * addMessage(res); } else { addMessage("Something went wrong!"); } }
	 */

	public void submitJob(ActionEvent actionEvent) {
		if (userManager.jobExists(name)) {
			addMessage("Job already exists under name (" + name + ")");
		} else {
			addMessage("Job submitted for (" + name + ")");
			userManager.submitJob(name, content);
		}

	}

	private void addMessage(String summary) {
		FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
				summary, null);
		FacesContext.getCurrentInstance().addMessage(null, message);
	}

	/*
	 * public String getCurrentSelectedName() { return currentSelectedName; }
	 * 
	 * public void setCurrentSelectedName(String currentSelectedName) {
	 * this.currentSelectedName = currentSelectedName; }
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
		this.content = content;
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

}
