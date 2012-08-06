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
 
package com.jogamp.opengl.test.junit.util;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.media.opengl.GL;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLDrawable;

import com.jogamp.common.util.locks.SingletonInstance;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.texture.TextureIO;

import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.rules.TestName;


public abstract class UITestCase {
    @Rule public TestName _unitTestName = new TestName();

    public static final String SINGLE_INSTANCE_LOCK_FILE = "UITestCase.lock";
    public static final int SINGLE_INSTANCE_LOCK_PORT = 59999;
    
    public static final long SINGLE_INSTANCE_LOCK_TO   = 3*60*1000; // wait up to 3 min
    public static final long SINGLE_INSTANCE_LOCK_POLL =      1000; // poll every 1s

    static volatile SingletonInstance singletonInstance;
    
    static volatile boolean testSupported = true;

    private static final synchronized void initSingletonInstance() {
        if( null == singletonInstance )  {
            // singletonInstance = SingletonInstance.createFileLock(SINGLE_INSTANCE_LOCK_POLL, SINGLE_INSTANCE_LOCK_FILE);
            singletonInstance = SingletonInstance.createServerSocket(SINGLE_INSTANCE_LOCK_POLL, SINGLE_INSTANCE_LOCK_PORT);
            if(!singletonInstance.tryLock(SINGLE_INSTANCE_LOCK_TO)) {
                throw new RuntimeException("Fatal: Could not lock single instance: "+singletonInstance.getName());
            }
        }
    }
    
    public static void setTestSupported(boolean v) {
        System.err.println("setTestSupported: "+v);
        testSupported = v;
    }

    public final String getTestMethodName() {
        return _unitTestName.getMethodName();
    }
    
    public final String getSimpleTestName(String separator) {
        return getClass().getSimpleName()+separator+getTestMethodName();
    }

    public final String getFullTestName(String separator) {
        return getClass().getName()+separator+getTestMethodName();
    }
    
    @BeforeClass
    public static void oneTimeSetUp() {
        // one-time initialization code                
        initSingletonInstance();
    }

    @AfterClass
    public static void oneTimeTearDown() {
        // one-time cleanup code
        System.gc(); // force cleanup
        singletonInstance.unlock();
    }

    @Before
    public void setUp() {
        System.err.print("++++ UITestCase.setUp: "+getFullTestName(" - "));
        if(!testSupported) {
            System.err.println(" - "+unsupportedTestMsg);
            Assume.assumeTrue(testSupported);
        }
        System.err.println();      
    }

    @After
    public void tearDown() {
        System.err.println("++++ UITestCase.tearDown: "+getFullTestName(" - "));
    }
    
    static final String unsupportedTestMsg = "Test not supported on this platform.";
    
    /**
     * Takes a snapshot of the drawable's current framebuffer. Example filenames: 
     * <pre>
     * TestFBODrawableNEWT.test01-F_rgba-I_rgba-S0_default-GL2-n0004-0800x0600.png
     * TestFBODrawableNEWT.test01-F_rgba-I_rgba-S0_default-GL2-n0005-0800x0600.png
     * </pre>
     * 
     * @param simpleTestName will be used as the filename prefix
     * @param sn sequential number 
     * @param postSNDetail optional detail to be added to the filename after <code>sn</code>
     * @param gl the current GL context object. It's read drawable is being used as the pixel source and to gather some details which will end up in the filename.
     * @param readBufferUtil the {@link GLReadBufferUtil} to be used to read the pixels for the screenshot.
     * @param fileSuffix Optional file suffix without a <i>dot</i> defining the file type, i.e. <code>"png"</code>.
     *                   If <code>null</code> the <code>"png"</code> as defined in {@link TextureIO#PNG} is being used.
     * @param destPath Optional platform dependent file path. It shall use {@link File#separatorChar} as is directory separator.
     *                 It shall not end with a directory separator, {@link File#separatorChar}.
     *                 If <code>null</code> the current working directory is being used.  
     */
    public static void snapshot(String simpleTestName, int sn, String postSNDetail, GL gl, GLReadBufferUtil readBufferUtil, String fileSuffix, String destPath) {
        if(null == fileSuffix) {
            fileSuffix = TextureIO.PNG;
        }
        final StringWriter filenameSW = new StringWriter();
        {
            final GLDrawable drawable = gl.getContext().getGLReadDrawable();
            final GLCapabilitiesImmutable caps = drawable.getChosenGLCapabilities();
            final String F_pfmt = readBufferUtil.hasAlpha() ? "rgba" : "rgb_";
            final String pfmt = caps.getAlphaBits() > 0 ? "rgba" : "rgb_";
            final String aaext = caps.getSampleExtension();
            final int samples = caps.getNumSamples() ;
            postSNDetail = null != postSNDetail ? "-"+postSNDetail : "";
            final PrintWriter pw = new PrintWriter(filenameSW);
            pw.printf("%s-n%04d%s-F_%s-I_%s-S%d_%s-%s-%04dx%04d.%s", 
                    simpleTestName, sn, postSNDetail, F_pfmt, pfmt, samples, aaext, drawable.getGLProfile().getName(), 
                    drawable.getWidth(), drawable.getHeight(), fileSuffix);
        }
        final String filename = null != destPath ? destPath + File.separator + filenameSW.toString() : filenameSW.toString();
        System.err.println(Thread.currentThread().getName()+": ** screenshot: "+filename);
        gl.glFinish(); // just make sure rendering finished ..
        if(readBufferUtil.readPixels(gl, false)) {
            readBufferUtil.write(new File(filename));
        }                
    }    
}

