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
import org.primefaces.context.RequestContext;

import com.google.gson.Gson;

import ubc.pavlab.morf.models.Chart;
import ubc.pavlab.morf.models.Job;
import ubc.pavlab.morf.models.ValidationResult;

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
    private String content;
    private Job selectedJob;
    private Job jobToRemove;
    private Job jobToSave;

    // Chart Stuff
    private boolean chartReady = false;

    public IndexView() {
        log.info( "IndexView created" );
    }

    @PostConstruct
    public void init() {
        log.info( "IndexView init" );

    }

    public void saveJob() {
        if ( !jobToSave.isSaved() ) {
            log.info( "saved" );
            userManager.saveJob( jobToSave );
        }

    }

    public void applyExampleInput() {
        content = ">Example\n" + settingsCache.getProperty( "morf.exampleInput" );
    }

    public void createChart() {

        Chart chart = new Chart( selectedJob );

        chartReady = chart.isReady();

        RequestContext.getCurrentInstance().addCallbackParam( "hc_values",
                new Gson().toJson( chart.getSeriesValues() ) );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_labels",
                new Gson().toJson( chart.getSeriesLabels() ) );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_title", chart.getName() );

    }

    public void cancelJob() {

        if ( userManager.jobExists( jobToRemove ) ) {
            boolean canceled = userManager.cancelJob( jobToRemove );
            if ( canceled ) {
                addMessage( "Job (" + jobToRemove.getId() + ") successfully cancelled.", FacesMessage.SEVERITY_INFO );
            } else {
                addMessage( "Job (" + jobToRemove.getId() + ") failed to cancel.", FacesMessage.SEVERITY_WARN );
            }
        } else {
            addMessage( "Cannot find job (" + jobToRemove.getId() + ")", FacesMessage.SEVERITY_ERROR );

        }
    }

    private ValidationResult validate( String content ) {
        log.debug( "Validating: " + content );
        ProcessBuilder pb = new ProcessBuilder( settingsCache.getProperty( "morf.validate" ), "/dev/stdin",
                "/dev/stdout" );
        pb.redirectErrorStream( true );

        Process process = null;
        try {
            process = pb.start();
        } catch ( IOException e ) {
            log.error( "Couldn't start the validation process.", e );
            return new ValidationResult( false, "ERROR: Something went wrong!" );
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
            return new ValidationResult( false, "ERROR: Something went wrong!" );
        }

        BufferedReader br = new BufferedReader( new InputStreamReader( process.getInputStream() ) );

        String currLine = null;
        boolean res = false;
        StringBuilder resultContent = new StringBuilder();
        try {
            currLine = br.readLine();
            if ( currLine != null ) {
                if ( currLine.startsWith( ">" ) ) {
                    res = true;
                } else {
                    res = false;
                }

                resultContent.append( currLine );
                resultContent.append( System.lineSeparator() );

                while ( ( currLine = br.readLine() ) != null ) {
                    resultContent.append( currLine );
                    resultContent.append( System.lineSeparator() );
                }

            }

            br.close();
        } catch ( IOException e ) {
            log.error( "Couldn't read the output.", e );
            return new ValidationResult( false, "ERROR: Something went wrong!" );
        }

        return new ValidationResult( res, resultContent.toString() );
    }

    public void submitJob( ActionEvent actionEvent ) {
        ValidationResult vr = validate( content );

        HttpServletRequest request = ( HttpServletRequest ) FacesContext.getCurrentInstance().getExternalContext()
                .getRequest();
        String ipAddress = request.getHeader( "X-FORWARDED-FOR" );
        if ( ipAddress == null ) {
            ipAddress = request.getRemoteAddr();
        }
        String label = "unknown";
        int sequenceSize = 0;

        int id = userManager.getNewJobId();

        if ( vr.isSuccess() ) {
            // TODO
            String textStr[] = content.split( "\\r?\\n" );

            if ( textStr.length > 1 ) {
                label = textStr[0];
                // if ( label.startsWith( ">" ) ) {
                // label = label.substring( 1 );
                // }

                for ( int i = 1; i < textStr.length; i++ ) {
                    sequenceSize += textStr[i].length();
                }

            }

            Job job = new Job( userManager.getSessionId(), label, id, content, sequenceSize, ipAddress );

            if ( userManager.jobExists( job ) ) {
                addMessage( "Job already exists under id (" + id + ")", FacesMessage.SEVERITY_WARN );
            } else {
                addMessage( "Job (" + id + ") submitted for (" + label + ")", FacesMessage.SEVERITY_INFO );
                userManager.submitJob( job );
                // RequestContext.getCurrentInstance().addCallbackParam("stopPolling", false);
            }
        } else {
            Job job = new Job( userManager.getSessionId(), label, id, content, 0, ipAddress );
            userManager.addFailedJob( job, vr.getContent() );
            addMessage( "Malformed FASTA Format!", FacesMessage.SEVERITY_ERROR );

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

    public Job getJobToSave() {
        return jobToSave;
    }

    public void setJobToSave( Job jobToSave ) {
        this.jobToSave = jobToSave;
    }

    public boolean isChartReady() {
        return chartReady;
    }

}
