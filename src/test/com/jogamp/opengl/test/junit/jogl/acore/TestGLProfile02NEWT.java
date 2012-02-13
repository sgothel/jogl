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

import org.junit.Assert;
import org.junit.Test;

import javax.media.opengl.*;

import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.DumpGLInfo;
import com.jogamp.newt.opengl.*;

public class TestGLProfile02NEWT extends UITestCase {

    @Test
    public void test00Version() throws InterruptedException {
        System.err.println(JoglVersion.getDefaultOpenGLInfo(null).toString()); 
    }

    @Test
    public void test01GLProfileGLES1() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GLES1)) {
            System.out.println("GLProfile GLES1 n/a");
            return;
        }
        GLProfile glp = GLProfile.get(GLProfile.GLES1);
        System.out.println("GLProfile GLES1: "+glp);
        dumpVersion(glp);
    }
    
    @Test
    public void test06GLProfileGLES2() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GLES2)) {
            System.out.println("GLProfile GLES2 n/a");
            return;
        }
        GLProfile glp = GLProfile.get(GLProfile.GLES2);
        System.out.println("GLProfile GLES2: "+glp);
        dumpVersion(glp);
    }
    
    protected void dumpVersion(GLProfile glp) throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(glp);        
        GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("TestGLProfile02NEWT");

        glWindow.addGLEventListener(new DumpGLInfo());

        glWindow.setSize(128, 128);
        glWindow.setVisible(true);

        glWindow.display();
        Thread.sleep(100);
        glWindow.destroy();
    }

    public static void main(String args[]) throws IOException {
        String tstname = TestGLProfile02NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
