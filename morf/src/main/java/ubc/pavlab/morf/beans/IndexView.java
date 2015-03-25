package ubc.pavlab.morf.beans;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import ubc.pavlab.morf.models.Job;

@ManagedBean
@ViewScoped
public class IndexView implements Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 2438909388677798292L;

    private static final Logger log = Logger.getLogger( IndexView.class );

    @ManagedProperty(value = "#{userManager}")
    private UserManager userManager;

    @ManagedProperty(value = "#{settingsCache}")
    private SettingsCache settingsCache;

    // private String currentSelectedName;
    private String name;
    private String content;
    private Job selectedJob;
    private Job jobToRemove;
    private Integer exampleCnt = 1;

    public IndexView() {
        log.info( "IndexView created" );
    }

    @PostConstruct
    public void init() {
        log.info( "IndexView init" );
    }

    /*
     * public void getResult(ActionEvent actionEvent) { String res = userManager.getResultIfReady(currentSelectedName);
     * if (res != null) { addMessage(res); } else { addMessage("Something went wrong!"); } }
     */

    public void cancelJob() {

        if ( userManager.jobExists( jobToRemove ) ) {
            boolean canceled = userManager.cancelJob( jobToRemove );
            if ( canceled ) {
                addMessage( "Job (" + jobToRemove.getName() + ") successfully cancelled.", FacesMessage.SEVERITY_INFO );
            } else {
                addMessage( "Job (" + jobToRemove.getName() + ") failed to cancel.", FacesMessage.SEVERITY_WARN );
            }
        } else {
            addMessage( "Cannot find job (" + jobToRemove.getName() + ")", FacesMessage.SEVERITY_ERROR );

        }
    }

    private boolean validate( String content ) {
        log.debug( "Validating: " + content );
        ProcessBuilder pb = new ProcessBuilder( settingsCache.getProperty( "morf.validate" ), "/dev/stdin",
                "/dev/stdout" );
        pb.redirectErrorStream( true );

        Process process = null;
        try {
            process = pb.start();
        } catch ( IOException e ) {
            log.error( "Couldn't start the validation process.", e );
            return false;
        }

        try {
            if ( process != null ) {
                BufferedWriter bw = new BufferedWriter( new OutputStreamWriter( process.getOutputStream() ) );

                // BufferedReader inputFile = new BufferedReader(new InputStreamReader(new FileInputStream(
                // "/home/mjacobson/morf/input.txt")));
                //
                // String currInputLine = null;
                // while ((currInputLine = inputFile.readLine()) != null) {
                // bw.write(currInputLine);
                // bw.newLine();
                // }
                // bw.close();
                // inputFile.close();
                bw.write( content );
                bw.close();
            }
        } catch ( IOException e ) {
            log.error( "Either couldn't read from the input file or couldn't write to the OutputStream.", e );
            return false;
        }

        BufferedReader br = new BufferedReader( new InputStreamReader( process.getInputStream() ) );

        // String currLine = null;
        boolean res = false;
        try {
            if ( br.readLine().startsWith( ">" ) ) {
                res = true;
            } else {
                res = false;
            }
            // while ((currLine = br.readLine()) != null) {
            // System.out.println(currLine);
            // }
            br.close();
        } catch ( IOException e ) {
            log.error( "Couldn't read the output.", e );
            return false;
        }

        return res;
    }

    public void submitJob( ActionEvent actionEvent ) {
        boolean res = validate( content );

        if ( res ) {

            HttpServletRequest request = ( HttpServletRequest ) FacesContext.getCurrentInstance().getExternalContext()
                    .getRequest();
            String ipAddress = request.getHeader( "X-FORWARDED-FOR" );
            if ( ipAddress == null ) {
                ipAddress = request.getRemoteAddr();
            }
            // System.out.println("ipAddress:" + ipAddress);

            Job job = new Job( userManager.getSessionId(), name, content, ipAddress );

            if ( userManager.jobExists( job ) ) {
                addMessage( "Job already exists under name (" + name + ")", FacesMessage.SEVERITY_WARN );
            } else {
                addMessage( "Job submitted for (" + name + ")", FacesMessage.SEVERITY_INFO );
                userManager.submitJob( job );
                // RequestContext.getCurrentInstance().addCallbackParam("stopPolling", false);
            }
        } else {
            addMessage( "Job (" + name + "): Malformed FASTA Format!", FacesMessage.SEVERITY_ERROR );
        }

    }

    public void submitMultipleJob( ActionEvent actionEvent ) {
        HttpServletRequest request = ( HttpServletRequest ) FacesContext.getCurrentInstance().getExternalContext()
                .getRequest();
        String ipAddress = request.getHeader( "X-FORWARDED-FOR" );
        if ( ipAddress == null ) {
            ipAddress = request.getRemoteAddr();
        }
        // System.out.println("ipAddress:" + ipAddress);
        for ( int i = 0; i < 5; i++ ) {
            content = exampleCnt.toString();
            name = exampleCnt.toString();
            submitJob( null );
            exampleCnt++;

        }

    }

    private void addMessage( String summary, FacesMessage.Severity severity ) {
        FacesMessage message = new FacesMessage( severity, summary, null );
        FacesContext.getCurrentInstance().addMessage( null, message );
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

    public void setName( String name ) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent( String content ) {
        this.content = content.trim() + "\r\n";
    }

    public void setUserManager( UserManager userManager ) {
        this.userManager = userManager;
    }

    public void setSettingsCache( SettingsCache settingsCache ) {
        this.settingsCache = settingsCache;
    }

    public Job getSelectedJob() {
        return selectedJob;
    }

    public void setSelectedJob( Job selectedJob ) {
        this.selectedJob = selectedJob;
    }

    public Job getJobToRemove() {
        return jobToRemove;
    }

    public void setJobToRemove( Job jobToRemove ) {
        this.jobToRemove = jobToRemove;
    }

}
