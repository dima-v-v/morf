/**
 * 
 */
package ubc.pavlab.morf.beans;

import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.LineChartSeries;

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

        if ( savedJob != null && savedJob.getComplete() && !savedJob.getFailed() ) {
            LineChartModel model = new LineChartModel();
            model.setAnimate( true );
            model.setExtender( "chartExtender" );

            Axis yAxis = model.getAxis( AxisType.Y );
            yAxis.setMin( 0 );
            yAxis.setMax( 1 );

            Axis xAxis = model.getAxis( AxisType.X );
            xAxis.setTickAngle( 75 );
            xAxis.setMin( 0 );

            LineChartSeries series = new LineChartSeries();
            series.setShowMarker( false );
            // series.setShowLine( false );

            String res = null;
            try {
                res = savedJob.getFuture().get( 1, TimeUnit.SECONDS );
            } catch ( InterruptedException | ExecutionException | TimeoutException e ) {
                log.error( e );
            }

            String textStr[] = res.split( "\\r?\\n" );
            int maxPos = 0;
            for ( int i = 0; i < textStr.length; i++ ) {
                String[] line = textStr[i].split( "\t" );
                if ( !line[0].startsWith( "#" ) ) {
                    try {
                        int pos = Integer.valueOf( textStr[i].split( "\t" )[0] );
                        if ( pos > maxPos ) {
                            maxPos = pos;
                        }
                        double val = Double.valueOf( textStr[i].split( "\t" )[2] );
                        series.set( pos, val );
                    } catch ( NumberFormatException e ) {
                        log.error( e );
                    }
                }

            }

            xAxis.setMax( maxPos );

            model.addSeries( series );

            if ( series.getData().size() > 0 ) {
                chartReady = true;
            } else {
                chartReady = false;
            }

            chart = model;

        } else {
            log.info( "Job contains no data" );
            chartReady = false;
        }

    }

    public long timeLeft() {
        return ( JobManager.PURGE_AFTER - ( System.currentTimeMillis() - savedJob.getSavedDate() ) ) / 1000 / 60 / 60;
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
