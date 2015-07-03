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

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLProfile;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLProfile03NEWTOffscreen extends GLProfile0XBase {

    @Test
    public void test01GLProfileDefault() throws InterruptedException {
        System.out.println("GLProfile "+GLProfile.glAvailabilityToString());
        System.out.println("GLProfile.getDefaultDevice(): "+GLProfile.getDefaultDevice());
        final GLProfile glp = GLProfile.getDefault();
        System.out.println("GLProfile.getDefault(): "+glp);
        final GLCapabilitiesImmutable caps = new GLCapabilities(glp);
        validateOnlineOffscreen("default", caps);
    }

    @Test
    public void test02GLProfileDefaultBitmap() throws InterruptedException {
        System.out.println("GLProfile "+GLProfile.glAvailabilityToString());
        System.out.println("GLProfile.getDefaultDevice(): "+GLProfile.getDefaultDevice());
        final GLProfile glp = GLProfile.getDefault();
        System.out.println("GLProfile.getDefault(): "+glp);
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBitmap(true);
        validateOnlineOffscreen("default", caps);
    }

    @Test
    public void test02GLProfileMaxProgrammable() throws InterruptedException {
        // Assuming at least one programmable profile is available
        final GLProfile glp = GLProfile.getMaxProgrammable(true);
        System.out.println("GLProfile.getMaxProgrammable(): "+glp);
        final GLCapabilitiesImmutable caps = new GLCapabilities(glp);
        validateOnlineOffscreen("maxProgrammable", caps);
    }

    @Test
    public void test03GLProfileMaxFixedFunc() throws InterruptedException {
        // Assuming at least one fixed function profile is available
        final GLProfile glp = GLProfile.getMaxFixedFunc(true);
        System.out.println("GLProfile.getMaxFixedFunc(): "+glp);
        final GLCapabilitiesImmutable caps = new GLCapabilities(glp);
        validateOnlineOffscreen("maxFixedFunc", caps);
    }

    @Test
    public void test04GLProfileGL2ES1() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GL2ES1)) {
            System.out.println("GLProfile GL2ES1 n/a");
            return;
        }
        final GLProfile glp = GLProfile.getGL2ES1();
        final GLCapabilitiesImmutable caps = new GLCapabilities(glp);
        validateOnlineOffscreen(GLProfile.GL2ES1, caps);
    }

    @Test
    public void test05GLProfileGL2ES2() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GL2ES2)) {
            System.out.println("GLProfile GL2ES2 n/a");
            return;
        }
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilitiesImmutable caps = new GLCapabilities(glp);
        validateOnlineOffscreen(GLProfile.GL2ES2, caps);
    }

    @Test
    public void test06GLProfileGL4ES3() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GL4ES3)) {
            System.out.println("GLProfile GL4ES3 n/a");
            return;
        }
        final GLProfile glp = GLProfile.getGL4ES3();
        final GLCapabilitiesImmutable caps = new GLCapabilities(glp);
        validateOnlineOffscreen(GLProfile.GL4ES3, caps);
    }

    @Test
    public void test07GLProfileGL2GL3() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GL2GL3)) {
            System.out.println("GLProfile GL2GL3 n/a");
            return;
        }
        final GLProfile glp = GLProfile.getGL2GL3();
        final GLCapabilitiesImmutable caps = new GLCapabilities(glp);
        validateOnlineOffscreen(GLProfile.GL2GL3, caps);
    }

    void testSpecificProfile(final String glps) throws InterruptedException {
        if(GLProfile.isAvailable(glps)) {
            final GLProfile glp = GLProfile.get(glps);
            final GLCapabilitiesImmutable caps = new GLCapabilities(glp);
            validateOnlineOffscreen(glps, caps);
        } else {
            System.err.println("Profile "+glps+" n/a");
        }
    }

    @Test
    public void test10_GL4bc() throws InterruptedException {
        testSpecificProfile(GLProfile.GL4bc);
    }

    @Test
    public void test11_GL3bc() throws InterruptedException {
        testSpecificProfile(GLProfile.GL3bc);
    }

    @Test
    public void test12_GL2() throws InterruptedException {
        testSpecificProfile(GLProfile.GL2);
    }

    @Test
    public void test13_GL4() throws InterruptedException {
        testSpecificProfile(GLProfile.GL4);
    }

    @Test
    public void test14_GL3() throws InterruptedException {
        testSpecificProfile(GLProfile.GL3);
    }

    @Test
    public void test15_GLES1() throws InterruptedException {
        testSpecificProfile(GLProfile.GLES1);
    }

    @Test
    public void test16_GLES2() throws InterruptedException {
        testSpecificProfile(GLProfile.GLES2);
    }

    @Test
    public void test17_GLES3() throws InterruptedException {
        testSpecificProfile(GLProfile.GLES3);
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestGLProfile03NEWTOffscreen.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
