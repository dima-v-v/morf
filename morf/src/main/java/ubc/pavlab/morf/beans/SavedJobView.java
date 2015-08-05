/**
 * 
 */
package ubc.pavlab.morf.beans;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;
import org.primefaces.context.RequestContext;
import org.primefaces.model.chart.LineChartModel;

import com.google.gson.Gson;

import ubc.pavlab.morf.models.Job;

@ManagedBean
@ViewScoped
public class SavedJobView implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 7286831685089743671L;

    private static final Logger log = Logger.getLogger( SavedJobView.class );

    @ManagedProperty(value = "#{jobManager}")
    private JobManager jobManager;

    private String key;

    private Job savedJob;

    private LineChartModel chart;
    private boolean chartReady = false;

    public SavedJobView() {
        log.info( "SavedJobView created" );
    }

    public void init() {
        log.info( "SavedJobView init" );
        savedJob = jobManager.fetchSavedJob( key, false );
    }

    public void createChart() {

        if ( savedJob.getComplete() && !savedJob.getFailed() ) {

            String res = null;
            try {
                res = savedJob.getFuture().get( 1, TimeUnit.SECONDS );
            } catch ( InterruptedException | ExecutionException | TimeoutException e ) {
                log.error( e );
            }

            Map<Integer, Double> seriesValues = new HashMap<>();
            Map<Integer, String> seriesLabels = new HashMap<>();

            String textStr[] = res.split( "\\r?\\n" );
            for ( int i = 0; i < textStr.length; i++ ) {
                String[] line = textStr[i].split( "\t" );
                if ( !line[0].startsWith( "#" ) ) {
                    try {
                        String[] split = textStr[i].split( "\t" );
                        int pos = Integer.valueOf( split[0] );
                        double val = Double.valueOf( split[2] );
                        seriesValues.put( pos, val );
                        seriesLabels.put( pos, split[1] );
                    } catch ( NumberFormatException e ) {
                        log.error( e );
                    }
                }

            }

            if ( seriesValues.size() > 0 ) {
                chartReady = true;
            } else {
                chartReady = false;
            }

            RequestContext.getCurrentInstance().addCallbackParam( "hc_values", new Gson().toJson( seriesValues ) );
            RequestContext.getCurrentInstance().addCallbackParam( "hc_labels", new Gson().toJson( seriesLabels ) );
            RequestContext.getCurrentInstance().addCallbackParam( "hc_title", savedJob.getName() );

        } else {
            log.info( "Job contains no data" );
            chartReady = false;
        }

    }

    public Job getSavedJob() {
        return savedJob;
    }

    public void setSavedJob( Job savedJob ) {
        this.savedJob = savedJob;
    }

    public String getKey() {
        return key;
    }

    public void setKey( String key ) {
        this.key = key;
    }

    public boolean isChartReady() {
        return chartReady;
    }

    public LineChartModel getChart() {
        return chart;
    }

    public void setJobManager( JobManager jobManager ) {
        this.jobManager = jobManager;
    }

}
