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

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class Job {

    @Override
    public String toString() {
        return "Job [uid=" + uid + ", contents=" + contents.substring( 0, Math.min( contents.length(), 20 ) ) + "]";
    }

    private final int uid;
    private final String contents;

    public Job( final int uid, final String contents ) {
        super();
        this.uid = uid;
        this.contents = contents;
    }

    public int getUid() {
        return uid;
    }

    public String getContents() {
        return contents;
    }

}
