/*
 * Copyright (c) 2010 Sven Gothel. All Rights Reserved.
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
 * Neither the name Sven Gothel or the names of
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
 * SVEN GOTHEL HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */

package com.jogamp.test.junit.jogl.acore;


import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import javax.media.opengl.*;
import com.jogamp.newt.*;
import com.jogamp.newt.opengl.*;

import com.jogamp.newt.*;
import java.io.IOException;

public class TestGLProfile01CORE {
    static GLProfile glp;

    @BeforeClass
    public static void initClass() {
        glp = GLProfile.getDefault();
        Assert.assertNotNull(glp);
    }

    @AfterClass
    public static void releaseClass() {
    }

    @Test
    public void test01GLProfileDefault() {
        System.out.println("GLProfile <static> "+GLProfile.glAvailabilityToString());
        GLProfile glp = GLProfile.getDefault();
        dumpVersion(glp);
    }

    @Test
    public void test02GLProfileMaxFixedFunc() {
        // Assuming at least one fixed profile is available
        GLProfile glp = GLProfile.getMaxFixedFunc();
        System.out.println("GLProfile <static> getMaxFixedFunc(): "+glp);
        if(glp.getName().equals(GLProfile.GL4bc)) {
            Assert.assertTrue(GLProfile.isGL4bcAvailable());
            Assert.assertTrue(GLProfile.isGL3bcAvailable());
            Assert.assertTrue(GLProfile.isGL2Available());
            Assert.assertTrue(GLProfile.isGL2ES1Available());
            Assert.assertTrue(GLProfile.isGL2ES2Available());
        } else if(glp.getName().equals(GLProfile.GL3bc)) {
            Assert.assertTrue(GLProfile.isGL3bcAvailable());
            Assert.assertTrue(GLProfile.isGL2Available());
            Assert.assertTrue(GLProfile.isGL2ES1Available());
            Assert.assertTrue(GLProfile.isGL2ES2Available());
        } else if(glp.getName().equals(GLProfile.GL2)) {
            Assert.assertTrue(GLProfile.isGL2Available());
            Assert.assertTrue(GLProfile.isGL2ES1Available());
            Assert.assertTrue(GLProfile.isGL2ES2Available());
        } else if(glp.getName().equals(GLProfile.GL2ES1)) {
            Assert.assertTrue(GLProfile.isGL2ES1Available());
        }
        dumpVersion(glp);
    }

    @Test
    public void test02GLProfileMaxProgrammable() {
        // Assuming at least one programmable profile is available
        GLProfile glp = GLProfile.getMaxProgrammable();
        System.out.println("GLProfile <static> getMaxProgrammable(): "+glp);
        if(glp.getName().equals(GLProfile.GL4)) {
            Assert.assertTrue(GLProfile.isGL4Available());
            Assert.assertTrue(GLProfile.isGL3Available());
            Assert.assertTrue(GLProfile.isGL2Available());
            Assert.assertTrue(GLProfile.isGL2ES1Available());
            Assert.assertTrue(GLProfile.isGL2ES2Available());
        } else if(glp.getName().equals(GLProfile.GL3)) {
            Assert.assertTrue(GLProfile.isGL3Available());
            Assert.assertTrue(GLProfile.isGL2Available());
            Assert.assertTrue(GLProfile.isGL2ES1Available());
            Assert.assertTrue(GLProfile.isGL2ES2Available());
        } else if(glp.getName().equals(GLProfile.GL2)) {
            Assert.assertTrue(GLProfile.isGL2Available());
            Assert.assertTrue(GLProfile.isGL2ES1Available());
            Assert.assertTrue(GLProfile.isGL2ES2Available());
        } else if(glp.getName().equals(GLProfile.GL2ES2)) {
            Assert.assertTrue(GLProfile.isGL2ES2Available());
        }
        dumpVersion(glp);
    }

    protected void dumpVersion(GLProfile glp) {
        GLCapabilities caps = new GLCapabilities(glp);
        GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("TestGLProfile01CORE");

        glWindow.addGLEventListener(new DumpVersion());

        glWindow.setSize(128, 128);
        glWindow.setVisible(true);
        glWindow.display();
        glWindow.destroy(true);
    }

    public static void main(String args[]) throws IOException {
        String tstname = TestGLProfile01CORE.class.getName();
        org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner.main(new String[] {
            tstname,
            "filtertrace=true",
            "haltOnError=false",
            "haltOnFailure=false",
            "showoutput=true",
            "outputtoformatters=true",
            "logfailedtests=true",
            "logtestlistenerevents=true",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter,TEST-"+tstname+".xml" } );
    }

}
