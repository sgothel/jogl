/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
 
package com.jogamp.test.junit.jogl.acore;

import com.jogamp.test.junit.util.UITestCase;

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

public class TestGLProfile01NEWT extends UITestCase {
    static GLProfile glp;

    @BeforeClass
    public static void initClass() {
        GLProfile.initSingleton(true);
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
    public void test03GLProfileMaxProgrammable() {
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

    @Test
    public void test04GLProfileGL2ES1() {
        if(!GLProfile.isGL2ES1Available()) {
            System.out.println("GLProfile GL2ES1 n/a");
            return;
        }
        GLProfile glp = GLProfile.getGL2ES1();
        System.out.println("GLProfile <static> GL2ES1: "+glp);
        dumpVersion(glp);
    }

    @Test
    public void test05GLProfileGL2ES2() {
        if(!GLProfile.isGL2ES2Available()) {
            System.out.println("GLProfile GL2ES2 n/a");
            return;
        }
        GLProfile glp = GLProfile.getGL2ES2();
        System.out.println("GLProfile <static> GL2ES2: "+glp);
        dumpVersion(glp);
    }

    protected void dumpVersion(GLProfile glp) {
        GLCapabilities caps = new GLCapabilities(glp);
        GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("TestGLProfile01NEWT");

        glWindow.addGLEventListener(new DumpVersion());

        glWindow.setSize(128, 128);
        glWindow.setVisible(true);
        glWindow.display();
        glWindow.destroy(true);
    }

    public static void main(String args[]) throws IOException {
        String tstname = TestGLProfile01NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
