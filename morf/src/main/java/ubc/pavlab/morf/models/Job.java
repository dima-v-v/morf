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

package ubc.pavlab.morf.models;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class Job implements Callable<String> {

	private static final Logger log = Logger.getLogger(Job.class);
	private static String scriptName;
	private static String scriptBasePath;
	private static String pathToInput;
	private static String pathToOutput;

	private String sessionId;
	private String name;
	private String content;
	private Boolean complete = false;
	private Future<String> future;
	// private String result;
	private StreamedContent resultFile;
	private Date submittedDate;
	private String position;

	public Job() {

	}

	/**
	 * @param sessionId
	 * @param name
	 * @param contents
	 */
	public Job(String sessionId, String name, String content) {
		super();
		this.sessionId = sessionId;
		this.name = name;
		this.content = content;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

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

	public Future<String> getFuture() {
		return future;
	}

	public void setFuture(Future<String> future) {
		this.future = future;
	}

	public Boolean getComplete() {
		if (complete) {
			return true;
		} else {
			if (this.future == null) {
				return false;
			} else {
				return this.future.isDone();
			}
		}
	}

	public void setComplete(Boolean complete) {
		this.complete = complete;
	}

	// public String getResult() {
	// if ( result != null ) {
	// return result;
	// }
	// if ( getComplete() ) {
	// try {
	// this.result = this.future.get( 1, TimeUnit.SECONDS );
	// return result;
	// } catch ( InterruptedException | ExecutionException | TimeoutException e ) {
	// e.printStackTrace();
	// return null;
	// }
	// } else {
	// return null;
	// }
	//
	// }

	public StreamedContent getFile() {
		if (getComplete()) {
			try {
				InputStream in = IOUtils.toInputStream(this.future.get(1, TimeUnit.SECONDS), "UTF-8");
				this.resultFile = new DefaultStreamedContent(in, "text/plain", this.name + ".txt");
				return resultFile;
			} catch (InterruptedException | ExecutionException | TimeoutException | IOException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public String toString() {
		return "Job [sessionId=" + sessionId + ", name=" + name + ", complete=" + complete + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((sessionId == null) ? 0 : sessionId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Job other = (Job) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (sessionId == null) {
			if (other.sessionId != null)
				return false;
		} else if (!sessionId.equals(other.sessionId))
			return false;
		return true;
	}

	@Override
	public String call() throws Exception {
		log.info("Starting job " + name + " for session: " + sessionId);
		// Write content to input
		File file = new File(pathToInput);

		try (FileOutputStream fop = new FileOutputStream(file)) {

			// if file doesn't exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fop));
			writer.write(content);
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		// Execute script
		executeCommand("./" + scriptName, scriptBasePath);

		// Get output
		InputStream resultFile = new FileInputStream(pathToOutput);

		StringWriter writer = new StringWriter();
		IOUtils.copy(resultFile, writer, "UTF-8");
		log.info("Finished job " + name + " for session: " + sessionId);
		return writer.toString();
	}

	private String executeCommand(String command, String path) {

		StringBuffer output = new StringBuffer();

		Process p;
		try {
			p = Runtime.getRuntime().exec(command, null, new File(path));
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

			String line = "";
			while ((line = reader.readLine()) != null) {
				output.append(line + "\n");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return output.toString();
	}

	public Date getSubmittedDate() {
		return submittedDate;
	}

	public void setSubmittedDate(Date submittedDate) {
		this.submittedDate = submittedDate;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public static void setPaths(String scriptName, String scriptBasePath, String pathToInput, String pathToOutput) {
		Job.scriptName = scriptName;
		Job.scriptBasePath = scriptBasePath;
		Job.pathToInput = pathToInput;
		Job.pathToOutput = pathToOutput;
	}

}
