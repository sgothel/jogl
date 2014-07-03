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

import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import javax.media.opengl.*;

import com.jogamp.common.os.Platform;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.test.junit.util.UITestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLProfile00NEWT extends UITestCase {

    @Test
    public void testInitSingleton() throws InterruptedException {
        GLProfile.initSingleton();
        System.err.println("Desktop");
        final GLDrawableFactory desktopFactory = GLDrawableFactory.getDesktopFactory();
        if( null != desktopFactory ) {
            System.err.println(JoglVersion.getDefaultOpenGLInfo(desktopFactory.getDefaultDevice(), null, false));
            System.err.println(Platform.getNewline()+Platform.getNewline()+Platform.getNewline());
        } else {
            System.err.println("\tNULL");
        }

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
