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
 
package com.jogamp.opengl.test.junit.jogl.acore;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import javax.media.opengl.*;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.nativewindow.NativeWindowVersion;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.DumpGLInfo;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.newt.opengl.*;
import com.jogamp.newt.*;

public class TestGLProfile01NEWT extends UITestCase {

    @Test
    public void test00Version() throws InterruptedException {
        System.err.println(VersionUtil.getPlatformInfo());
        System.err.println(GlueGenVersion.getInstance());
        System.err.println(NativeWindowVersion.getInstance());
        System.err.println(JoglVersion.getInstance());
        System.err.println(NewtVersion.getInstance());

        GLDrawableFactory factory = GLDrawableFactory.getFactory(GLProfile.getDefault());
        List/*<GLCapabilitiesImmutable>*/ availCaps = factory.getAvailableCapabilities(null);
        for(int i=0; i<availCaps.size(); i++) {
            System.err.println(availCaps.get(i));
        }
    }

    @Test
    public void test01GLProfileDefault() throws InterruptedException {
        System.out.println("GLProfile "+GLProfile.glAvailabilityToString());
        GLProfile glp = GLProfile.getDefault();
        System.out.println("GLProfile.getDefault(): "+glp);
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
    public void test02GL2() throws InterruptedException {
        GLProfile glp = GLProfile.get(GLProfile.GL2);
        dumpVersion(glp);
    }

    @Test
    public void test03GLProfileMaxProgrammable() throws InterruptedException {
        // Assuming at least one programmable profile is available
        GLProfile glp = GLProfile.getMaxProgrammable();
        System.out.println("GLProfile.getMaxProgrammable(): "+glp);
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
    public void test04GLProfileGL2ES1() throws InterruptedException {
        if(!GLProfile.isGL2ES1Available()) {
            System.out.println("GLProfile GL2ES1 n/a");
            return;
        }
        GLProfile glp = GLProfile.getGL2ES1();
        System.out.println("GLProfile GL2ES1: "+glp);
        dumpVersion(glp);
    }

    @Test
    public void test05GLProfileGL2ES2() throws InterruptedException {
        if(!GLProfile.isGL2ES2Available()) {
            System.out.println("GLProfile GL2ES2 n/a");
            return;
        }
        GLProfile glp = GLProfile.getGL2ES2();
        System.out.println("GLProfile GL2ES2: "+glp);
        dumpVersion(glp);
    }

    protected void dumpVersion(GLProfile glp) throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(glp);
        GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("TestGLProfile01NEWT");

        glWindow.addGLEventListener(new DumpGLInfo());

        glWindow.setSize(128, 128);
        glWindow.setVisible(true);

        glWindow.display();
        Thread.sleep(100);
        glWindow.destroy();
    }

    public static void main(String args[]) throws IOException {
        String tstname = TestGLProfile01NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
