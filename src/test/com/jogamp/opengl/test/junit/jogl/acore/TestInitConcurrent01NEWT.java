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

import com.jogamp.opengl.GLProfile;

import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.os.Platform;

/**
 * Concurrent initialization and lock-free rendering using shared NEWT Display EDT instances.
 * <p>
 * Rendering is always lock-free and independent of the EDT, however shared NEWT Display instances
 * perform lifecycle actions (window creation etc) with locking.
 * </p>
 * <p>
 * Each test is decorated w/ {@link GLProfile#shutdown()} to ensure that
 * implicit {@link GLProfile#initSingleton()} is also being tested.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestInitConcurrent01NEWT extends InitConcurrentBaseNEWT {

    @Test(timeout=180000) // TO 3 min
    public void test02TwoThreads() throws InterruptedException {
        runJOGLTasks(2, true);
    }

    @Test(timeout=180000) // TO 3 min
    public void test04FourThreads() throws InterruptedException {
        runJOGLTasks(4, true);
    }

    @Test(timeout=300000) // TO 5 min
    public void test16SixteenThreads() throws InterruptedException {
        if( Platform.getCPUFamily() != Platform.CPUFamily.ARM &&
            Platform.getOSType() != Platform.OSType.WINDOWS ) {
            runJOGLTasks(16, true);
        } else {
            runJOGLTasks( 6, true);
        }
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (final Exception ex) { ex.printStackTrace(); }
            }
        }
        final String tstname = TestInitConcurrent01NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
