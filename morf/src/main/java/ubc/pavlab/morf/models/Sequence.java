/*
 * The morf project
 * 
 * Copyright (c) 2016 University of British Columbia
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
public class Sequence {
    private final String label;
    private final String content;
    private final int size;

    public Sequence( String label, String content, int size ) {
        this.label = label.trim();
        this.content = content.trim();
        this.size = size;
    }

    public String getLabel() {
        return label;
    }

    public String getContent() {
        return content;
    }

    public int getSize() {
        return size;
    }

    public String getFASTA() {
        return label + "\r\n" + content + "\r\n";
    }

}
