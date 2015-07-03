package com.jogamp.gluegen.opengl.ant;

/*
 * Copyright (C) 2003 Rob Grzywinski (rgrzywinski@realityinteractive.com)
 * Copyright (c) 2003-2005 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 */

import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * <p>An <a href="http://ant.apache.org">ANT</a> {@link org.apache.tools.ant.Task}
 * for using {@link com.jogamp.gluegen.opengl.BuildStaticGLInfo}.</p>
 *
 * <p>Usage:</p>
 * <pre>
    &lt;staticglgen package="[generated files package]"
                    headers="[file pattern of GL headers]"
                    outputdir="[directory to output the generated files]" /&gt;
 * </pre>
 *
 * @author Rob Grzywinski <a href="mailto:rgrzywinski@realityinteractive.com">rgrzywinski@yahoo.com</a>
 */
// FIXME:  blow out javadoc
public class StaticGLGenTask extends Task
{
    /**
     * <p>The {@link com.jogamp.gluegen.opengl.BuildStaticGLInfo} classname.</p>
     */
    private static final String GL_GEN = "com.jogamp.gluegen.opengl.BuildStaticGLInfo";

    // =========================================================================
    /**
     * <p>The {@link org.apache.tools.ant.types.CommandlineJava} that is used
     * to execute {@link com.jogamp.gluegen.opengl.BuildStaticGLInfo}.</p>
     */
    private final CommandlineJava glgenCommandline;

    // =========================================================================
    /**
     * <p>The package name for the generated files.</p>
     */
    private String packageName;

    /**
     * <p>The output directory.</p>
     */
    private String outputDirectory;

    /**
     * <p>The {@link org.apache.tools.ant.types.FileSet} of GL headers.</p>
     */
    private final FileSet headerSet = new FileSet();

    // =========================================================================
    /**
     * <p>Create and add the VM and classname to {@link org.apache.tools.ant.types.CommandlineJava}.</p>
     */
    public StaticGLGenTask()
    {
        // create the CommandlineJava that will be used to call BuildStaticGLInfo
        glgenCommandline = new CommandlineJava();

        // set the VM and classname in the commandline
        glgenCommandline.setVm(JavaEnvUtils.getJreExecutable("java"));
        glgenCommandline.setClassname(GL_GEN);
    }

    // =========================================================================
    // ANT getters and setters
    /**
     * <p>Set the package name for the generated files.  This is called by ANT.</p>
     *
     * @param  packageName the name of the package for the generated files
     */
    public void setPackage(final String packageName)
    {
        log( ("Setting package name to: " + packageName), Project.MSG_VERBOSE);
        this.packageName = packageName;
    }

    /**
     * <p>Set the output directory.  This is called by ANT.</p>
     *
     * @param  directory the output directory
     */
    public void setOutputDir(final String directory)
    {
        log( ("Setting output directory to: " + directory),
              Project.MSG_VERBOSE);
        this.outputDirectory = directory;
    }

    /**
     * <p>Add a header file to the list.  This is called by ANT for a nested
     * element.</p>
     *
     * @return {@link org.apache.tools.ant.types.PatternSet.NameEntry}
     */
    public PatternSet.NameEntry createHeader()
    {
        return headerSet.createInclude();
    }

    /**
     * <p>Add a header file to the list.  This is called by ANT for a nested
     * element.</p>
     *
     * @return {@link org.apache.tools.ant.types.PatternSet.NameEntry}
     */
    public PatternSet.NameEntry createHeadersFile()
    {
        return headerSet.createIncludesFile();
    }

    /**
     * <p>Set the set of header patterns.  Patterns may be separated by a comma
     * or a space.  This is called by ANT.</p>
     *
     * @param  headers the string containing the header patterns
     */
    public void setHeaders(final String headers)
    {
        headerSet.setIncludes(headers);
    }

    /**
     * <p>Add an optional classpath that defines the location of {@link com.jogamp.gluegen.opengl.BuildStaticGLInfo}
     * and <code>BuildStaticGLInfo</code>'s dependencies.</p>
     *
     * @returns {@link org.apache.tools.ant.types.Path}
     */
     public Path createClasspath()
     {
         return glgenCommandline.createClasspath(project).createPath();
     }

    // =========================================================================
    /**
     * <p>Run the task.  This involves validating the set attributes, creating
     * the command line to be executed and finally executing the command.</p>
     *
     * @see  org.apache.tools.ant.Task#execute()
     */
    @Override
    public void execute()
        throws BuildException
    {
        // validate that all of the required attributes have been set
        validateAttributes();

        // TODO:  add logic to determine if the generated file needs to be
        //        regenerated

        // add the attributes to the CommandlineJava
        addAttributes();

        log(glgenCommandline.describeCommand(), Project.MSG_VERBOSE);

        // execute the command and throw on error
        final int error = execute(glgenCommandline.getCommandline());
        if(error == 1)
            throw new BuildException( ("BuildStaticGLInfo returned: " + error), location);
    }

    /**
     * <p>Ensure that the user specified all required arguments.</p>
     *
     * @throws BuildException if there are required arguments that are not
     *         present or not valid
     */
    private void validateAttributes()
        throws BuildException
    {
        // validate that the package name is set
        if(!isValid(packageName))
            throw new BuildException("Invalid package name: " + packageName);

        // validate that the output directory is set
        // TODO:  switch to file and ensure that it exists
        if(!isValid(outputDirectory))
            throw new BuildException("Invalid output directory name: " + outputDirectory);

        // TODO:  validate that there are headers set
    }

    /**
     * <p>Is the specified string valid?  A valid string is non-<code>null</code>
     * and has a non-zero length.</p>
     *
     * @param  string the string to be tested for validity
     * @return <code>true</code> if the string is valid.  <code>false</code>
     *         otherwise.
     */
    private boolean isValid(final String string)
    {
        // check for null
        if(string == null)
            return false;

        // ensure that the string has a non-zero length
        // NOTE:  must trim() to remove leading and trailing whitespace
        if(string.trim().length() < 1)
            return false;

        // the string is valid
        return true;
    }

    /**
     * <p>Add all of the attributes to the command line.  They have already
     * been validated.</p>
     */
    private void addAttributes()
    {
        // add the package name
        glgenCommandline.createArgument().setValue(packageName);

        // add the output directory name
        glgenCommandline.createArgument().setValue(outputDirectory);

        // add the header -files- from the FileSet
        headerSet.setDir(getProject().getBaseDir());
        final DirectoryScanner directoryScanner = headerSet.getDirectoryScanner(getProject());
        final String[] directoryFiles = directoryScanner.getIncludedFiles();
        for(int i=0; i<directoryFiles.length; i++)
        {
            glgenCommandline.createArgument().setValue(directoryFiles[i]);
        }
    }

    /**
     * <p>Execute {@link com.jogamp.gluegen.opengl.BuildStaticGLInfo} in a
     * forked JVM.</p>
     *
     * @throws BuildException
     */
    private int execute(final String[] command)
        throws BuildException
    {
        // create the object that will perform the command execution
        final Execute execute = new Execute(new LogStreamHandler(this, Project.MSG_INFO,
                                                           Project.MSG_WARN),
                                      null);

        // set the project and command line
        execute.setAntRun(project);
        execute.setCommandline(command);
        execute.setWorkingDirectory( project.getBaseDir() );

        // execute the command
        try
        {
            return execute.execute();
        } catch(final IOException ioe)
        {
            throw new BuildException(ioe, location);
        }
    }
}
