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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

/**
 * Handles collecting and formatting data from the results of a job to be handled in the front-end by HighCharts
 * 
 * @author mjacobson
 * @version $Id$
 */
public class Chart {

    private static final Logger log = Logger.getLogger( Chart.class );

    private final Map<Integer, Double> seriesValues = new LinkedHashMap<>();
    private final Map<Integer, String> seriesLabels = new LinkedHashMap<>();
    private final String name;
    private final boolean ready;

    public Chart( Job job ) {

        if ( job != null ) {

            String name = job.getName();
            name = name.startsWith( ">" ) ? name.substring( 1 ) : name;
            this.name = name;

            if ( job.getComplete() && !job.getFailed() ) {

                String res = null;
                try {
                    res = job.getFuture().get( 1, TimeUnit.SECONDS );
                } catch ( InterruptedException | ExecutionException | TimeoutException e ) {
                    log.error( e );
                }

                String textStr[] = res.split( "\\r?\\n" );
                for ( int i = 0; i < textStr.length; i++ ) {
                    String[] line = textStr[i].split( "\t" );
                    if ( !line[0].startsWith( "#" ) && !line[0].startsWith( ">" ) ) {
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
                    ready = true;
                } else {
                    ready = false;
                }

            } else {
                log.info( "Job contains no data" );
                ready = false;
            }
        } else {
            this.name = "";
            this.ready = false;
        }

    }

    public Map<Integer, Double> getSeriesValues() {
        return seriesValues;
    }

    public Map<Integer, String> getSeriesLabels() {
        return seriesLabels;
    }

    public String getName() {
        return name;
    }

    public boolean isReady() {
        return ready;
    }

}
