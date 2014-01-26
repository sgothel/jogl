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

import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.os.Platform;

/**
 * Concurrent and lock-free initialization and rendering using exclusive NEWT Display EDT instances.
 * <p>
 * Rendering is always lock-free and independent of the EDT, however exclusive NEWT Display instances
 * perform lifecycle actions (window creation etc) w/o locking.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestInitConcurrent02NEWT extends InitConcurrentBaseNEWT {
    static boolean mainRun = false;
    
    @Test(timeout=180000) // TO 3 min
    public void test02TwoThreads() throws InterruptedException {
        if(!mainRun) {
            System.err.println("Disabled for auto unit test until further analysis - Windows/ATI driver crash");
            return;
        }
        runJOGLTasks(2, false);
    }
    
    @Test(timeout=180000) // TO 3 min
    public void test02FourThreads() throws InterruptedException {
        if(!mainRun) {
            System.err.println("Disabled for auto unit test until further analysis - Windows/ATI driver crash");
            return;
        }
        runJOGLTasks(4, false);
    }
    
    @Test(timeout=180000) // TO 3 min
    public void test16SixteenThreads() throws InterruptedException {
        if(!mainRun) {
            System.err.println("Disabled for auto unit test until further analysis - Windows/ATI driver crash");
            return;
        }
        if( Platform.getCPUFamily() != Platform.CPUFamily.ARM &&
            Platform.getOSType() != Platform.OSType.WINDOWS ) {
            runJOGLTasks(16, false);
        } else {
            runJOGLTasks( 6, false);
        }
    }
    
    public static void main(String args[]) throws IOException {
        mainRun = true;        
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-normalRun")) {
                mainRun = false;
            } else if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
        String tstname = TestInitConcurrent02NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
