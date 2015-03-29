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

package ubc.pavlab.morf.beans;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

import org.apache.log4j.Logger;

import ubc.pavlab.morf.models.Job;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@ManagedBean(eager = true)
@ApplicationScoped
public class SettingsCache implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 3280770517562168465L;

    private static final Logger log = Logger.getLogger( SettingsCache.class );

    private static final String PROPERTIES_FILE = "/home/nmalhis/MCW/morf.properties";

    private Properties prop = new Properties();

    public SettingsCache() {
        log.info( "SettingsCache created" );
    }

    @PostConstruct
    public void init() {
        log.info( "SettingsCache init" );

        InputStream input = null;

        try {

            try {
                // TODO possibly look at classpath first?
                input = new FileInputStream( PROPERTIES_FILE );
            } catch ( FileNotFoundException e ) {
                log.warn( "Could not find PROPERTIES_FILE : (" + PROPERTIES_FILE + ") looking in: ("
                        + System.getProperty( "user.dir" ) + ")" );
                input = new FileInputStream( "morf.properties" );
            }

            // load a properties file from class path, inside static method
            prop.load( input );

            for ( String property : prop.stringPropertyNames() ) {
                log.debug( property + ": " + prop.getProperty( property ) );
            }

            String scriptPath = prop.getProperty( "morf.script" );
            Path p = Paths.get( scriptPath );

            Job.setPaths( p.getFileName().toString(), p.getParent().toString(), prop.getProperty( "morf.input" ),
                    prop.getProperty( "morf.output" ) );

        } catch ( IOException ex ) {
            ex.printStackTrace();
        } finally {
            if ( input != null ) {
                try {
                    input.close();
                } catch ( IOException e ) {
                    e.printStackTrace();
                }
            }
        }

    }

    public String getProperty( String key ) {
        return prop.getProperty( key );
    }

    public boolean contains( String key ) {
        return prop.contains( key );
    }

    public void reload() {
        init();
    }
}
