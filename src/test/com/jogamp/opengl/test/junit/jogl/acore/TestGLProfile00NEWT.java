/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.*;

import com.jogamp.common.os.Platform;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.test.junit.util.UITestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLProfile00NEWT extends UITestCase {

    @Test
    public void test01InternedString() {
        final String s1 = "GL2";
        final String s2 = "GL2";
        Assert.assertEquals(s1, s2);
        Assert.assertTrue("s1-ref != s2-ref", s1 == s2);
        Assert.assertTrue("s1-ref != 'GL2'-ref", s1 == "GL2");

        Assert.assertEquals("GL2", GLProfile.GL2);
        Assert.assertTrue("GLProfile-ref != 'GL2'-ref", GLProfile.GL2 == "GL2");
    }

    @Test
    public void test02InitSingleton() throws InterruptedException {
        Assert.assertFalse("JOGL is initialized before usage", GLProfile.isInitialized());
        GLProfile.initSingleton();
        Assert.assertTrue("JOGL is not initialized after enforced initialization", GLProfile.isInitialized());
    }

    @Test
    public void test11DumpDesktopGLInfo() throws InterruptedException {
        Assert.assertTrue("JOGL is not initialized ...", GLProfile.isInitialized());
        System.err.println("Desktop");
        final GLDrawableFactory desktopFactory = GLDrawableFactory.getDesktopFactory();
        if( null != desktopFactory ) {
            System.err.println(JoglVersion.getDefaultOpenGLInfo(desktopFactory.getDefaultDevice(), null, false));
            System.err.println(Platform.getNewline()+Platform.getNewline()+Platform.getNewline());
        } else {
            System.err.println("\tNULL");
        }
    }

    @Test
    public void test12DumpEGLGLInfo() throws InterruptedException {
        Assert.assertTrue("JOGL is not initialized ...", GLProfile.isInitialized());
        System.err.println("EGL");
        final GLDrawableFactory eglFactory = GLDrawableFactory.getEGLFactory();
        if( null != eglFactory ) {
            System.err.println(JoglVersion.getDefaultOpenGLInfo(eglFactory.getDefaultDevice(), null, false));
        } else {
            System.err.println("\tNULL");
        }
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestGLProfile00NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
