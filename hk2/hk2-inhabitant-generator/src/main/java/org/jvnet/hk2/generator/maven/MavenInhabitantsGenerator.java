/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.jvnet.hk2.generator.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.jvnet.hk2.generator.HabitatGenerator;

/**
 * Calls the generator with maven
 * 
 * @goal generateInhabitants
 * @phase process-classes 
 * @requiresDependencyResolution test
 */
public class MavenInhabitantsGenerator extends AbstractMojo {
    /**
     * The maven project.
     *
     * @parameter expression="${project}" @required @readonly
     */
    protected MavenProject project;
    
    /**
     * @parameter expression="${project.build.outputDirectory}"
     */
    private File outputDirectory;
    
    /**
     * @parameter expression="${project.build.testOutputDirectory}"
     */
    private File testOutputDirectory;
    
    /**
     * @parameter
     */
    private boolean verbose;
    
    /**
     * @parameter
     */
    private String locator;
    
    /**
     * @parameter
     */
    private boolean test = false;
    
    /**
     * @parameter
     */
    private boolean noswap;
    
    /**
     * @parameter expression="${supportedProjectTypes}" default-value="jar"
     */
    private String supportedProjectTypes;
    
    /**
     * This method will compile the inhabitants file based on
     * the classes just compiled
     */
    @Override
    public void execute() throws MojoFailureException {
        List<String> projectTypes = Arrays.asList(supportedProjectTypes.split(","));
        if(!projectTypes.contains(project.getPackaging())){
            return;
        }
        
        File output = (test)? testOutputDirectory : outputDirectory;
        output.mkdirs();
        
        if (!output.exists()) {
            getLog().info("Exiting hk2-inhabitant-generator because could not find output directory " +
                  output.getAbsolutePath());
            return;
        }
        
        if (verbose) {
            getLog().info("");
            getLog().info("hk2-inhabitant-generator generating into location " + output.getAbsolutePath());
            getLog().info("");
        }
        
        LinkedList<String> arguments = new LinkedList<String>();
        
        arguments.add(HabitatGenerator.FILE_ARG);
        arguments.add(output.getAbsolutePath());
        
        if (verbose) {
            arguments.add(HabitatGenerator.VERBOSE_ARG);
        }
        
        if (locator != null) {
            arguments.add(HabitatGenerator.LOCATOR_ARG);
            arguments.add(locator);
        }
        
        arguments.add(HabitatGenerator.SEARCHPATH_ARG);
        arguments.add(getBuildClasspath());
        
        if (noswap) {
            arguments.add(HabitatGenerator.NOSWAP_ARG);
        }
        
        String argv[] = arguments.toArray(new String[arguments.size()]);
        
        int result = HabitatGenerator.embeddedMain(argv);
        
        if (result != 0) {
            throw new MojoFailureException("Could not generate inhabitants file for " +
                (test ? testOutputDirectory : outputDirectory));
        }
    }
    
    private String getBuildClasspath() {
        StringBuilder sb = new StringBuilder();
        // Make sure to add in the directory that has been built
        if (test) {
            sb.append(outputDirectory.getAbsolutePath());
            sb.append(File.pathSeparator);
        }        
        
        List<Artifact> artList = new ArrayList<Artifact>(project.getArtifacts());
        Iterator<Artifact> i = artList.iterator();
        
        if (i.hasNext()) {
            sb.append(i.next().getFile().getPath());

            while (i.hasNext()) {
                sb.append(File.pathSeparator);
                sb.append(i.next().getFile().getPath());
            }
        }
        
        String classpath = sb.toString();
        if(verbose){
            getLog().info("");
            getLog().info("-- Classpath --");
            getLog().info("");
            getLog().info(classpath);
            getLog().info("");
        }
        return classpath;
    }      
}
