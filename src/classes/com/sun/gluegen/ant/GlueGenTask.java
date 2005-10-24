package com.sun.gluegen.ant;

/*
 * GlueGenTask.java
 * Copyright (C) 2003 Rob Grzywinski (rgrzywinski@realityinteractive.com)
 *
 * Copying, distribution and use of this software in source and binary
 * forms, with or without modification, is permitted provided that the
 * following conditions are met:
 *
 * Distributions of source code must reproduce the copyright notice,
 * this list of conditions and the following disclaimer in the source
 * code header files; and Distributions of binary code must reproduce
 * the copyright notice, this list of conditions and the following
 * disclaimer in the documentation, Read me file, license file and/or
 * other materials provided with the software distribution.
 *
 * The names of Sun Microsystems, Inc. ("Sun") and/or the copyright
 * holder may not be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS," WITHOUT A WARRANTY OF ANY
 * KIND. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, NON-INTERFERENCE, ACCURACY OF
 * INFORMATIONAL CONTENT OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. THE
 * COPYRIGHT HOLDER, SUN AND SUN'S LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL THE
 * COPYRIGHT HOLDER, SUN OR SUN'S LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGES. YOU ACKNOWLEDGE THAT THIS SOFTWARE IS NOT
 * DESIGNED, LICENSED OR INTENDED FOR USE IN THE DESIGN, CONSTRUCTION,
 * OPERATION OR MAINTENANCE OF ANY NUCLEAR FACILITY. THE COPYRIGHT
 * HOLDER, SUN AND SUN'S LICENSORS DISCLAIM ANY EXPRESS OR IMPLIED
 * WARRANTY OF FITNESS FOR SUCH USES.
 */

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.DirSet;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * <p>An <a href="http://ant.apache.org">ANT</a> {@link org.apache.tools.ant.Task}
 * for using {@link com.sun.gluegen.GlueGen}.</p>
 * 
 * <p>Usage:</p>
 * <pre>
    &lt;gluegen src="[source C file]" 
                includes="[optional directory pattern of include files to include]"
                excludes="[optional directory pattern of include files to exclude]"
                includeRefid="[optional FileSet or DirSet for include files]"
                literalInclude="[optional hack to get around FileSet / DirSet issues with different drives]"
                emitter="[emitter class name]"
                config="[configuration file]" /&gt;
 * </pre> 
 *
 * @author Rob Grzywinski <a href="mailto:rgrzywinski@realityinteractive.com">rgrzywinski@yahoo.com</a>
 */
// FIXME:  blow out javadoc
// NOTE:  this has not been exhaustively tested
public class GlueGenTask extends Task
{
    /**
     * <p>The {@link com.sun.gluegen.GlueGen} classname.</p>
     */
    private static final String GLUE_GEN = "com.sun.gluegen.GlueGen";
    
    // =========================================================================
    /**
     * <p>The {@link org.apache.tools.ant.types.CommandlineJava} that is used
     * to execute {@link com.sun.gluegen.GlueGen}.</p>
     */
    private CommandlineJava gluegenCommandline;

    // =========================================================================
    /**
     * <p>The name of the emitter class.</p>
     */
    private String emitter;

    /**
     * <p>The configuration file name.</p>
     */
    private String configuration;
    
    /**
     * <p>The name of the source C file that is to be parsed.</p>
     */
    private String sourceFile;
    
    /**
     * <p>The {@link org.apache.tools.ant.types.FileSet} of includes.</p>
     */
    private FileSet includeSet = new FileSet();
    
    /**
     * <p>Because a {@link org.apache.tools.ant.types.FileSet} will include
     * everything in its base directory if it is left untouched, the <code>includeSet</code>
     * must only be added to the set of includes if it has been <i>explicitly</i>
     * set.</p>
     */
    private boolean usedIncludeSet = false; // by default it is not used 
    
    /**
     * <p>The set of include sets.  This allows includes to be added in multiple
     * fashions.</p>
     */
    // FIXME:  rename to listXXXX
    private List setOfIncludeSets = new LinkedList();

    /**
     * <p>A single literal directory to include.  This is to get around the
     * fact that neither {@link org.apache.tools.ant.types.FileSet} nor
     * {@link org.apache.tools.ant.types.DirSet} can handle multiple drives in
     * a sane manner.  If <code>null</code> then it has not been specified.</p>
     */
    private String literalInclude;
    
    // =========================================================================
    /**
     * <p>Create and add the VM and classname to {@link org.apache.tools.ant.types.CommandlineJava}.</p>
     */
    public GlueGenTask()
    {
        // create the CommandlineJava that will be used to call GlueGen
        gluegenCommandline = new CommandlineJava();
        
        // set the VM and classname in the commandline
        gluegenCommandline.setVm(JavaEnvUtils.getJreExecutable("java"));
        gluegenCommandline.setClassname(GLUE_GEN);
        // gluegenCommandline.createVmArgument().setValue("-verbose:class");
    }

    // =========================================================================
    // ANT getters and setters
    /**
     * <p>Set the emitter class name.  This is called by ANT.</p>
     * 
     * @param  emitter the name of the emitter class
     */
    public void setEmitter(String emitter)
    {
        log( ("Setting emitter class name to: " + emitter), Project.MSG_VERBOSE);
        this.emitter = emitter;
    }

    /**
     * <p>Set the configuration file name.  This is called by ANT.</p>
     * 
     * @param  configuration the name of the configuration file
     */
    public void setConfig(String configuration)
    {
        log( ("Setting configuration file name to: " + configuration), 
              Project.MSG_VERBOSE);
        this.configuration = configuration;
    }

    /**
     * <p>Set the source C file that is to be parsed.  This is called by ANT.</p>
     * 
     * @param  sourceFile the name of the source file
     */
    public void setSrc(String sourceFile)
    {
        log( ("Setting source file name to: " + sourceFile), Project.MSG_VERBOSE);
        this.sourceFile = sourceFile;
    }

    /**
     * <p>Set a single literal include directory.  See the <code>literalInclude</code>
     * javadoc for more information.</p>
     * 
     * @param  directory the directory to include
     */
    public void setLiteralInclude(String directory)
    {
        this.literalInclude = directory;
    }

    /**
     * <p>Add an include file to the list.  This is called by ANT for a nested
     * element.</p>
     * 
     * @return {@link org.apache.tools.ant.types.PatternSet.NameEntry}
     */
    public PatternSet.NameEntry createInclude()
    {
        usedIncludeSet = true;
        return includeSet.createInclude();
    }

    /**
     * <p>Add an include file to the list.  This is called by ANT for a nested
     * element.</p>
     * 
     * @return {@link org.apache.tools.ant.types.PatternSet.NameEntry}
     */
    public PatternSet.NameEntry createIncludesFile()
    {
        usedIncludeSet = true;
        return includeSet.createIncludesFile();
    }

    /**
     * <p>Set the set of include patterns.  Patterns may be separated by a comma
     * or a space.  This is called by ANT.</p>
     *
     * @param  includes the string containing the include patterns
     */
    public void setIncludes(String includes)
    {
        usedIncludeSet = true;
        includeSet.setIncludes(includes);
    }

    /**
     * <p>Add an include file to the list that is to be exluded.  This is called 
     * by ANT for a nested element.</p>
     * 
     * @return {@link org.apache.tools.ant.types.PatternSet.NameEntry}
     */
    public PatternSet.NameEntry createExclude()
    {
        usedIncludeSet = true;
        return includeSet.createExclude();
    }

    /**
     * <p>Add an exclude file to the list.  This is called by ANT for a nested
     * element.</p>
     * 
     * @return {@link org.apache.tools.ant.types.PatternSet.NameEntry}
     */
    public PatternSet.NameEntry createExcludesFile()
    {
        usedIncludeSet = true;
        return includeSet.createExcludesFile();
    }

    /**
     * <p>Set the set of exclude patterns.  Patterns may be separated by a comma
     * or a space.  This is called by ANT.</p>
     *
     * @param  includes the string containing the exclude patterns
     */
    public void setExcludes(String excludes)
    {
        usedIncludeSet = true;
        includeSet.setExcludes(excludes);
    }

    /**
     * <p>Set a {@link org.apache.tools.ant.types.Reference} to simplify adding
     * of complex sets of files to include.  This is called by ANT.</p>?
     * 
     * @param  reference a <code>Reference</code> to a {@link org.apache.tools.ant.types.FileSet}
     *         or {@link org.apache.tools.ant.types.DirSet}
     * @throws BuildException if the specified <code>Reference</code> is not
     *         either a <code>FileSet</code> or <code>DirSet</code>
     */
    public void setIncludeRefid(Reference reference) 
    {
        // ensure that the referenced object is either a FileSet or DirSet
        final Object referencedObject = reference.getReferencedObject(getProject());
        if( !( (referencedObject instanceof FileSet) || 
               (referencedObject instanceof DirSet)) )
        {               
            throw new BuildException("Only FileSets or DirSets are allowed as an include refid.");
        }

        // add the referenced object to the set of include sets
        setOfIncludeSets.add(referencedObject); 
    }

    /**
     * <p>Add a nested {@link org.apache.tools.ant.types.DirSet} to specify
     * the files to include.  This is called by ANT.</p>
     * 
     * @param  dirset the <code>DirSet</code> to be added
     */
    public void addDirset(DirSet dirset)
    {
        setOfIncludeSets.add(dirset);
    }

    /**
     * <p>Add an optional classpath that defines the location of {@link com.sun.gluegen.GlueGen}
     * and <code>GlueGen</code>'s dependencies.</p>
     * 
     * @returns {@link org.apache.tools.ant.types.Path}
     */
     public Path createClasspath()
     {
         return gluegenCommandline.createClasspath(project).createPath();
     }

    // =========================================================================
    /**
     * <p>Run the task.  This involves validating the set attributes, creating
     * the command line to be executed and finally executing the command.</p>
     * 
     * @see  org.apache.tools.ant.Task#execute()
     */
    public void execute() 
        throws BuildException 
    {
        // validate that all of the required attributes have been set
        validateAttributes();
        
        // TODO:  add logic to determine if the generated file needs to be
        //        regenerated
        
        // add the attributes to the CommandlineJava
        addAttributes();

        log(gluegenCommandline.describeCommand(), Project.MSG_VERBOSE);
        
        // execute the command and throw on error
        final int error = execute(gluegenCommandline.getCommandline());
        if(error == 1)
            throw new BuildException( ("GlueGen returned: " + error), location);
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
        // validate that the emitter class is set
        if(!isValid(emitter))
            throw new BuildException("Invalid emitter class name: " + emitter);

        // validate that the configuration file is set
        if(!isValid(configuration))
            throw new BuildException("Invalid configuration file name: " + configuration);
            
        // validate that the source file is set
        if(!isValid(sourceFile))
            throw new BuildException("Invalid source file name: " + sourceFile);
            
        // CHECK:  do there need to be includes to be valid?
    }

    /**
     * <p>Is the specified string valid?  A valid string is non-<code>null</code>
     * and has a non-zero length.</p>
     * 
     * @param  string the string to be tested for validity
     * @return <code>true</code> if the string is valid.  <code>false</code>
     *         otherwise. 
     */
    private boolean isValid(String string)
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
        throws BuildException
    {
        // NOTE:  GlueGen uses concatenated flag / value rather than two 
        //        separate arguments
        
        // add the emitter class name
        gluegenCommandline.createArgument().setValue("-E" + emitter);
        
        // add the configuration file name
        gluegenCommandline.createArgument().setValue("-C" + configuration);
        
        // add the includedSet to the setOfIncludeSets to simplify processing
        // all types of include sets ONLY if it has been set.
        // NOTE:  see the usedIncludeSet member javadoc for more info 
        // NOTE:  references and nested DirSets have already been added to the
        //        set of include sets
        if(usedIncludeSet)
        {
            includeSet.setDir(getProject().getBaseDir()); // NOTE:  the base dir must be set
            setOfIncludeSets.add(includeSet);
        }
        
        // iterate over all include sets and add their directories to the 
        // list of included directories.
        final List includedDirectories = new LinkedList();
        for(Iterator includes=setOfIncludeSets.iterator(); includes.hasNext(); )
        {
            // get the included set and based on its type add the directories
            // to includedDirectories
        	Object include = (Object)includes.next();
            final String[] directoryDirs;
            if(include instanceof FileSet)
            {
                final FileSet fileSet = (FileSet)include;
                DirectoryScanner directoryScanner = fileSet.getDirectoryScanner(getProject());
                directoryDirs = directoryScanner.getIncludedDirectories();
            } else if(include instanceof DirSet)
            {
                final DirSet dirSet = (DirSet)include;
                DirectoryScanner directoryScanner = dirSet.getDirectoryScanner(getProject());
                directoryDirs = directoryScanner.getIncludedDirectories();
            } else 
            {
                // NOTE:  this cannot occur as it is checked on setXXX() but
                //        just to be pedantic this is here
                throw new BuildException("Invalid included construct."); 
            }
        	
            // add the directoryDirs to the includedDirectories
            // TODO:  exclude any directory that is already in the list
            for(int i=0; i<directoryDirs.length; i++)
            {
            	includedDirectories.add(directoryDirs[i]);
            }
        }
        
        // if literalInclude is valid then add it to the list of included
        // directories
        if(isValid(literalInclude))
            includedDirectories.add(literalInclude); 
        
        // add the included directories to the command
        for(Iterator includes=includedDirectories.iterator(); includes.hasNext(); )
        {
        	String directory = (String)includes.next();
            gluegenCommandline.createArgument().setValue("-I" + directory);
        }

        // finally, add the source file
        gluegenCommandline.createArgument().setValue(sourceFile);
    }

    /** 
     * <p>Execute {@link com.sun.gluegen.GlueGen} in a forked JVM.</p>
     * 
     * @throws BuildException
     */
    private int execute(String[] command) 
        throws BuildException
    {
        // create the object that will perform the command execution
        Execute execute = new Execute(new LogStreamHandler(this, Project.MSG_INFO,
                                                           Project.MSG_WARN), 
                                      null);
                                      
        // set the project and command line
        execute.setAntRun(project);
        execute.setCommandline(command);
        
        // execute the command
        try
        {
            return execute.execute();
        } catch(IOException ioe)
        {
            throw new BuildException(ioe, location);
        }
    }
}
