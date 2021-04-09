package ubc.pavlab.morf.beans;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.primefaces.context.RequestContext;

import com.google.common.collect.Lists;
import com.google.gson.Gson;

import ubc.pavlab.morf.models.Chart;
import ubc.pavlab.morf.models.Job;
import ubc.pavlab.morf.models.Sequence;
import ubc.pavlab.morf.models.ValidationResult;

@Named
@ViewScoped
public class IndexView implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 2438909388677798292L;

    private static final Logger log = LogManager.getLogger( IndexView.class );

    private static final int MINIMUM_SEQUENCE_SIZE = 26;

    @Inject
    private UserManager userManager;

    @Inject
    private SettingsCache settingsCache;

    // private String currentSelectedName;
    private String content;
    private String trainOnDataset = "True";
    private boolean caseSensitive = true;

    private Job selectedJob;
    private Job submittedJob;
    private Job jobToRemove;
    private Job jobToSave;
    private Job jobToRenew;

    // Chart Stuff
    private boolean chartReady = false;

    public IndexView() {
        log.info( "IndexView created" );
    }

    @PostConstruct
    public void init() {
        log.info( "IndexView init" );
    }

    public void resendEmailSelected() {
        if ( userManager.resendEmail( selectedJob ) ) {
            addMessage( "Job (" + selectedJob.getId() + ") successfully emailed.", FacesMessage.SEVERITY_INFO );
        } else {
            addMessage( "Job (" + selectedJob.getId() + ") FAILED to email.", FacesMessage.SEVERITY_WARN );
        }
    }

    public void renewJob() {
        log.info( "renew " + jobToRenew.getId() );
        userManager.renewSaveJob( jobToRenew );
    }

    public void saveJob() {
        saveJob( jobToSave, true );
    }

    private void saveJob( Job job, boolean message ) {
        if ( !job.isSaved() ) {
            log.info( "saved" );
            userManager.saveJob( job );
            if ( message ) {
                addMessage( "Job (" + job.getId()
                        + ") successfully saved. The job will be available at the provided link for "
                        + JobManager.PURGE_AFTER / 60 / 60 / 1000 + " hours.", FacesMessage.SEVERITY_WARN );
            }
        }

    }

    public void applyExampleInput() {
        content = ">Example\n" + settingsCache.getProperty( "morf.exampleInput" );
    }

    public void createChart() {

        Chart chart = new Chart( selectedJob );

        chartReady = chart.isReady();

        RequestContext.getCurrentInstance()
                .addCallbackParam( "hc_values", new Gson().toJson( chart.getSeriesValues() ) );
        RequestContext.getCurrentInstance()
                .addCallbackParam( "hc_labels", new Gson().toJson( chart.getSeriesLabels() ) );
        RequestContext.getCurrentInstance().addCallbackParam( "hc_names", new Gson().toJson( chart.getSeriesNames() ) );
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
        // log.debug( "Validating: " + content );
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

    public Collection<Sequence> parseContent( String content ) {
        List<Sequence> sequences = Lists.newArrayList();

        String textStr[] = content.split( "\\r?\\n" );

        String currentSequenceContent = "";
        String currentSequenceLabel = "";
        int currentSequenceSize = 0;
        for ( int i = 0; i < textStr.length; i++ ) {
            String line = textStr[i];
            if ( line.startsWith( ">" ) ) {
                // Save previous sequence if exists
                if ( !StringUtils.isEmpty( currentSequenceContent ) ) {
                    sequences.add(
                            new Sequence( currentSequenceLabel, currentSequenceContent, currentSequenceSize ) );
                }
                // Start new sequence
                currentSequenceLabel = line;
                currentSequenceContent = "";
                currentSequenceSize = 0;
            } else {

                // If we don't have a label, skip
                if ( !StringUtils.isEmpty( currentSequenceLabel ) ) {
                    line = line.replaceAll( "\\s+", "" );

                    if ( !caseSensitive ) {
                        line = line.toUpperCase();
                        line = line.replaceAll( "\\d", "" );
                    }
                    // Skip empty lines
                    if ( !StringUtils.isEmpty( line ) ) {
                        currentSequenceSize += line.length();
                        currentSequenceContent += line + "\r\n";
                    }
                }

            }

        }

        // Last sequence
        if ( !StringUtils.isEmpty( currentSequenceContent ) ) {
            sequences.add(
                    new Sequence( currentSequenceLabel, currentSequenceContent, currentSequenceSize ) );
        }

        return sequences;

    }

    public void submitJob( ActionEvent actionEvent ) {

        HttpServletRequest request = ( HttpServletRequest ) FacesContext.getCurrentInstance().getExternalContext()
                .getRequest();
        String ipAddress = request.getHeader( "X-FORWARDED-FOR" );
        if ( ipAddress == null ) {
            ipAddress = request.getRemoteAddr();
        }

        Collection<Sequence> sequences = parseContent( content );

        for ( Sequence sequence : sequences ) {
            ValidationResult vr = validate( sequence.getFASTA() );

            int id = userManager.getNewJobId();
            String email = userManager.getEmail();

            if ( vr.isSuccess() ) {

                if ( sequence.getSize() < MINIMUM_SEQUENCE_SIZE ) {
                    Job job = new Job( userManager.getSessionId(), sequence.getLabel(), id, sequence.getFASTA(), 0,
                            ipAddress, trainOnDataset.equals( "True" ), StringUtils.isBlank( email ) ? null : email );
                    userManager.addFailedJob( job,
                            "Error report:\nError(s) found\n" + sequence.getLabel() + "\nError : "
                                    + "Sequence too small; must be at least " + MINIMUM_SEQUENCE_SIZE + " residues" );
                    addMessage( "Sequence too small; must be at least " + MINIMUM_SEQUENCE_SIZE + " residues",
                            FacesMessage.SEVERITY_INFO );
                    return;
                }

                if ( caseSensitive && sequence.getContent().matches( "(?s).*\\d.*" ) ) { // Need this because the validate executable isn't checking numbers
                    Job job = new Job( userManager.getSessionId(), sequence.getLabel(), id, sequence.getFASTA(), 0,
                            ipAddress, trainOnDataset.equals( "True" ), StringUtils.isBlank( email ) ? null : email );
                    userManager.addFailedJob( job,
                            "Error report:\nError(s) found\n" + sequence.getLabel() + "\nError : "
                                    + "Sequence contains numbers" );
                    addMessage( "Sequence contains numbers",
                            FacesMessage.SEVERITY_INFO );
                    return;
                }

                Job job = new Job( userManager.getSessionId(), sequence.getLabel(), id, sequence.getFASTA(),
                        sequence.getSize(), ipAddress, trainOnDataset.equals( "True" ),
                        StringUtils.isBlank( email ) ? null : email );

                if ( userManager.jobExists( job ) ) {
                    addMessage( "Job already exists under id (" + id + ")", FacesMessage.SEVERITY_WARN );
                } else {
                    userManager.submitJob( job );
                    saveJob( job, false );

                    // RequestContext.getCurrentInstance().addCallbackParam("stopPolling", false);

                    if ( StringUtils.isBlank( email ) ) {
                        RequestContext.getCurrentInstance().addCallbackParam( "confirm", true );
                    } else {
                        RequestContext.getCurrentInstance().addCallbackParam( "confirm", false );
                        addMessage(
                                "Job (" + id + ") submitted for (" + sequence.getLabel()
                                        + ") <br/> The job will be available at the provided <a href='http://"
                                        + settingsCache.getBaseUrl() + "savedJob.xhtml?key=" + job.getSavedKey()
                                        + "' target='_blank'>link</a> for " + JobManager.PURGE_AFTER / 60 / 60 / 1000
                                        + " hours.",
                                FacesMessage.SEVERITY_INFO );
                    }

                    submittedJob = job;

                }
            } else {
                Job job = new Job( userManager.getSessionId(), sequence.getLabel(), id, sequence.getFASTA(), 0,
                        ipAddress, trainOnDataset.equals( "True" ), StringUtils.isBlank( email ) ? null : email );
                userManager.addFailedJob( job, vr.getContent() );
                addMessage( "Malformed FASTA Format!", FacesMessage.SEVERITY_INFO );

            }

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

    public String getTrainOnDataset() {
        return trainOnDataset;
    }

    public void setTrainOnDataset( String trainOnDataset ) {
        this.trainOnDataset = trainOnDataset;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive( boolean caseSensitive ) {
        this.caseSensitive = caseSensitive;
    }

    public Job getSelectedJob() {
        return selectedJob;
    }

    public void setSelectedJob( Job selectedJob ) {
        this.selectedJob = selectedJob;
    }

    public Job getSubmittedJob() {
        return submittedJob;
    }

    public void setSubmittedJob( Job submittedJob ) {
        this.submittedJob = submittedJob;
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

    public void setJobToRenew( Job jobToRenew ) {
        this.jobToRenew = jobToRenew;
    }

    public boolean isChartReady() {
        return chartReady;
    }

}
