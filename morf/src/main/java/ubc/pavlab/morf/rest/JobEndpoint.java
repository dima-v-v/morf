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

package ubc.pavlab.morf.rest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

import ubc.pavlab.morf.beans.JobManager;
import ubc.pavlab.morf.beans.SettingsCache;
import ubc.pavlab.morf.models.Job;
import ubc.pavlab.morf.models.ValidationResult;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@Path("/job")
@Singleton
public class JobEndpoint {

    private static final Logger log = Logger.getLogger( JobEndpoint.class );

    private int MAX_JOBS_IN_QUEUE = 2;

    @Inject
    private JobManager jobManager;

    @Inject
    private SettingsCache settingsCache;

    private Integer jobIdIncrementer = 0;
    private Map<String, Queue<Job>> outerJobQueue = new ConcurrentHashMap<>();

    public JobEndpoint() {
        log.info( "Job REST created" );
    }

    public int getNewJobId() {
        synchronized ( jobIdIncrementer ) {
            return jobIdIncrementer++;
        }
    }

    @GET
    @Path("/total")
    public Response getMsg() {

        String output = "Total Jobs in Queue: " + jobManager.getJobsInQueue();

        return Response.status( 200 ).entity( output ).build();

    }

    @GET
    @Path("/{param}")
    public Response getMsg( @PathParam("param" ) String msg) {

        Job savedJob = jobManager.fetchSavedJob( msg, false );

        String output = savedJob == null ? "Job not Found" : String.valueOf( savedJob.getStatus() );

        return Response.status( 200 ).entity( output ).build();

    }

    @POST
    @Path("/post")
    //@Consumes(MediaType.TEXT_XML)
    public Response postStrMsg( @Context HttpServletRequest request, String msg ) {

        JSONObject json;
        String content;
        try {
            json = new JSONObject( msg );
            content = json.getString( "fasta" );
        } catch ( JSONException e ) {
            log.warn( "Malformed JSON", e );
            return Response.status( 200 ).entity( "Malformed JSON" ).build();
        }

        if ( StringUtils.isBlank( content ) ) {
            return Response.status( 200 ).entity( "Blank FASTA" ).build();
        }

        String ipAddress = request.getHeader( "X-FORWARDED-FOR" );
        if ( ipAddress == null ) {
            ipAddress = request.getRemoteAddr();
        }

        if ( StringUtils.isBlank( ipAddress ) ) {
            return Response.status( 200 ).entity( "Could not parse remote address" ).build();
        }

        ValidationResult vr = validate( content );

        String label = "unknown";
        int sequenceSize = 0;

        int id = getNewJobId();

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

            // Using ipAddress as session Id

            Job job = new Job( ipAddress, label, id, content, sequenceSize, ipAddress, true, null );

            jobManager.submitToWaitingList( job );

            //            userManager.submitJob( job );
            //            saveJob( job, false );

            return Response.status( 200 ).entity( job.getSavedKey() ).build();

        } else {
            return Response.status( 200 ).entity( "Malformed FASTA Format" ).build();

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
}
