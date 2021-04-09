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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ubc.pavlab.morf.beans.JobManager;
import ubc.pavlab.morf.beans.SettingsCache;
import ubc.pavlab.morf.models.Chart;
import ubc.pavlab.morf.models.Job;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@Path("/job")
public class JobEndpoint {

    private static final Logger log = LogManager.getLogger( JobEndpoint.class );

    @Inject
    private JobManager jobManager;

    @Inject
    private SettingsCache settingsCache;

    public JobEndpoint() {
        log.info( "Job REST created" );
    }

    @GET
    @Path("/loadInfo")
    public Response getLoadInfo( @Context HttpServletRequest request ) {

        String ipAddress = request.getHeader( "X-FORWARDED-FOR" );
        if ( ipAddress == null ) {
            ipAddress = request.getRemoteAddr();
        }

        Map<String, Object> response = new LinkedHashMap<>();
        try {
            response.put( "httpstatus", 200 );
            response.put( "jobsInQueue", jobManager.getJobsInQueue() );
            response.put( "residuesInQueue", jobManager.getResiduesInQueue() );
            response.put( "jobsinClientQueue", jobManager.getJobsInClientQueue( ipAddress ) );
            response.put( "residuesInClientQueue", jobManager.getResiduesInClientQueue( ipAddress ) );

        } catch ( Exception e1 ) {
            log.error( "Malformed JSON", e1 );
        }
        return Response.ok( new Gson().toJson(response), MediaType.APPLICATION_JSON ).build();

    }

    @GET
    @Path("/{param}")
    public Response getMsg( @Context HttpServletRequest request, @PathParam("param" ) String msg) {

        Job job = jobManager.fetchSavedJob( msg, false );

        if ( job == null ) {
            Map<String, Object> deleted = fail( 404, "Job Not Found" );
            try {
                deleted.put( "complete", true );
            } catch ( Exception e ) {
                log.error( e );
            }
            return Response.status( 404 ).entity( new Gson().toJson(deleted) ).type( MediaType.APPLICATION_JSON ).build();
        }

        String ipAddress = request.getHeader( "X-FORWARDED-FOR" );
        if ( ipAddress == null ) {
            ipAddress = request.getRemoteAddr();
        }

        Map<String, Object> response = new LinkedHashMap<>();
        try {
            response.put( "httpstatus", 200 );
            response.put( "name", job.getName() );
            response.put( "size", job.getSequenceSize() );
            response.put( "status", job.getStatus() );
            response.put( "jobsInQueue", jobManager.getJobsInQueue() );
            response.put( "residuesInQueue", jobManager.getResiduesInQueue() );
            response.put( "jobsinClientQueue", jobManager.getJobsInClientQueue( ipAddress ) );
            response.put( "residuesInClientQueue", jobManager.getResiduesInClientQueue( ipAddress ) );
            response.put( "savedTimeLeft", job.getSaveTimeLeft() );
            response.put( "submitted", job.getSubmittedDate() );
            response.put( "success", true );

            // Race condition reasons
            boolean complete = job.getComplete();

            response.put( "complete", complete );
            if ( complete ) {
                Chart chart = new Chart( job );
                response.put( "labels", chart.getLabels() );
                response.put( "results", chart.getValues() );
                response.put( "titles", chart.getTitles() );
            } else {
                response.put( "eta", "Unknown" );
            }
        } catch ( Exception e1 ) {
            log.error( "Malformed JSON", e1 );
        }
        return Response.ok( new Gson().toJson(response), MediaType.APPLICATION_JSON ).build();

    }

    @GET
    @Path("/delete/{param}")
    public Response deleteStrMsg( @Context HttpServletRequest request, @PathParam("param" ) String msg) {
        Job job = jobManager.fetchSavedJob( msg, false );
        if ( job == null ) {
            return Response.status( 404 ).entity( new Gson().toJson(fail( 404, "Job Not Found" )) ).type( MediaType.APPLICATION_JSON ).build();
        }
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            boolean success = jobManager.requestStopJob( job );
            response.put( "httpstatus", 200 );
            response.put( "message", success ? "Job Deleted" : "Failed To Delete Job" );
            response.put( "success", success );

        } catch ( Exception e1 ) {
            log.error( "Malformed JSON", e1 );
        }
        return Response.ok( new Gson().toJson(response), MediaType.APPLICATION_JSON ).build();

    }

    @POST
    @Path("/post")
    public Response postStrMsg( @Context HttpServletRequest request, String msg ) {
        log.info( msg );
        String content;
        try {
            JsonObject json = new Gson().fromJson(msg, JsonObject.class);
            content = json.get( "fasta" ).getAsString();
            log.info( content );
        } catch ( Exception e ) {
            //log.warn( "Malformed JSON", e );
            return Response.status( 400 ).entity( new Gson().toJson(fail( 400, "Malformed JSON" )) ).type( MediaType.APPLICATION_JSON ).build();
        }

        if ( StringUtils.isBlank( content ) ) {
            return Response.status( 400 ).entity( new Gson().toJson(fail( 400, "Blank FASTA" )) ).type( MediaType.APPLICATION_JSON ).build();
        }

        String ipAddress = request.getHeader( "X-FORWARDED-FOR" );
        if ( ipAddress == null ) {
            ipAddress = request.getRemoteAddr();
        }

        //String sessionId = request.getSession( true ).getId();
        String sessionId = ipAddress;

        Job job = jobManager.createJob( sessionId, ipAddress, content, true, null );

        if ( job == null ) {
            return Response.status( 429 ).entity( new Gson().toJson(fail( 400, "Too Many Jobs In Queue" )) ).type( MediaType.APPLICATION_JSON ).build();
        }

        if ( !job.getFailed() ) {
            Map<String, Object> response = new LinkedHashMap<>();
            try {
                response.put( "httpstatus", 202 );
                response.put( "success", true );
                response.put( "message", "Job Accepted" );
                response.put( "name", job.getName() );
                response.put( "size", job.getSequenceSize() );
                response.put( "status", job.getStatus() );
                response.put( "jobsInQueue", jobManager.getJobsInQueue() );
                response.put( "residuesInQueue", jobManager.getResiduesInQueue() );
                response.put( "jobsInClientQueue", jobManager.getJobsInClientQueue( sessionId ) );
                response.put( "residuesInClientQueue", jobManager.getResiduesInClientQueue( sessionId ) );
                response.put( "location", settingsCache.getBaseUrl() + "rest/job/" + job.getSavedKey() );
            } catch ( Exception e1 ) {
                log.error( "Malformed JSON", e1 );
            }
            return Response.status( 202 ).entity( new Gson().toJson(response) ).type( MediaType.APPLICATION_JSON ).build();
        } else {
            return Response.status( 400 ).entity( new Gson().toJson(fail( 400, job.getStatus() )) ).type( MediaType.APPLICATION_JSON ).build();
        }

    }

    private static Map<String, Object> fail( int httpStatus, String message ) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            response.put( "httpstatus", httpStatus );
            response.put( "success", false );
            response.put( "message", message );
        } catch ( Exception e1 ) {
            log.error( "Malformed JSON", e1 );
        }
        return response;
    }

}
