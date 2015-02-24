package ubc.pavlab.morf.beans;

import java.io.Serializable;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

import org.apache.log4j.Logger;

import ubc.pavlab.morf.models.Job;

@ManagedBean(eager = true)
@ApplicationScoped
public class JobQueue implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -3986488603159351855L;
    private static final Logger log = Logger.getLogger( JobQueue.class );

    private LinkedBlockingQueue<Job> queue = new LinkedBlockingQueue<Job>();
    private boolean running;

    public JobQueue() {
        log.info( "JobQueue created" );
    }

    @PostConstruct
    public void init() {
        log.info( "JobQueue init" );
        running = true;
        putJob( new Job( 1, "1" ) );
        putJob( new Job( 2, "2" ) );
        putJob( new Job( 3, "3" ) );
        putJob( new Job( 4, "4" ) );
        putJob( new Job( 5, "5" ) );
        putJob( new Job( 6, "6" ) );
    }

    public boolean isRunning() {
        return running;
    }

    public void putJob( Job job ) {
        try {
            this.queue.put( job );
            log.info( "Adding Job: " + job.toString() );
        } catch ( InterruptedException e ) {
            e.printStackTrace();
        }

    }

    public LinkedBlockingQueue<Job> getQueue() {
        return queue;
    }
}
