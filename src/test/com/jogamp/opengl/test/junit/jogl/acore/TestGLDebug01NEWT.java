/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDebugListener;
import javax.media.opengl.GLDebugMessage;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLRunnable;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.UITestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLDebug01NEWT extends UITestCase {

    static String dbgTstMsg0 = "Hello World";
    static int dbgTstId0 = 42;

    GLWindow createWindow(final GLProfile glp, final boolean debugGL) {
        final GLCapabilities caps = new GLCapabilities(glp);
        //
        // Create native windowing resources .. X11/Win/OSX
        //
        final GLWindow window = GLWindow.create(caps);
        Assert.assertNotNull(window);
        window.setContextCreationFlags(debugGL?GLContext.CTX_OPTION_DEBUG:0);
        window.setSize(128, 128);
        window.setVisible(true);

        Assert.assertNotNull(window.getContext());
        Assert.assertNotNull(window.getContext().isCreated());

        return window;
    }

    void destroyWindow(final GLWindow window) {
        window.destroy();
    }


    void test01GLDebug01EnableDisable(final boolean enable, final String dbgTstMsg, final int dbgTstId) throws InterruptedException {
        final GLProfile glp = GLProfile.getDefault();

        final GLWindow window = createWindow(glp, enable);
        final GLContext ctx = window.getContext();
        final MyGLDebugListener myGLDebugListener = new MyGLDebugListener(dbgTstMsg, dbgTstId);
        if(enable) {
            ctx.addGLDebugListener(myGLDebugListener);
        }
        final String glDebugExt = ctx.getGLDebugMessageExtension();
        System.err.println("glDebug extension: "+glDebugExt);
        System.err.println("glDebug enabled: "+ctx.isGLDebugMessageEnabled());
        System.err.println("glDebug sync: "+ ctx.isGLDebugSynchronous());
        System.err.println("context version: "+ctx.getGLVersion());

        Assert.assertEquals((null == glDebugExt) ? false : enable, ctx.isGLDebugMessageEnabled());
        if(ctx.isGLDebugMessageEnabled() && null != dbgTstMsg && 0 <= dbgTstId) {
            window.invoke(true, new GLRunnable() {
                public boolean run(final GLAutoDrawable drawable) {
                    drawable.getContext().glDebugMessageInsert(GL2ES2.GL_DEBUG_SOURCE_APPLICATION,
                                                               GL2ES2.GL_DEBUG_TYPE_OTHER,
                                                               dbgTstId,
                                                               GL2ES2.GL_DEBUG_SEVERITY_MEDIUM, dbgTstMsg);
                    return true;
                }
            });
            Assert.assertEquals(true, myGLDebugListener.received());
        }

        destroyWindow(window);
    }

    @Test
    public void test01GLDebug01Disabled() throws InterruptedException {
        test01GLDebug01EnableDisable(false, null, -1);
    }

    @Test
    public void test01GLDebug01Enabled() throws InterruptedException {
        test01GLDebug01EnableDisable(true, dbgTstMsg0, dbgTstId0);
    }

    @Test
    public void test02GLDebug01Error() throws InterruptedException {
        final GLProfile glp = GLProfile.getDefault();

        final GLWindow window = createWindow(glp, true);

        final MyGLDebugListener myGLDebugListener = new MyGLDebugListener(
                GL2ES2.GL_DEBUG_SOURCE_API,
                GL2ES2.GL_DEBUG_TYPE_ERROR,
                GL2ES2.GL_DEBUG_SEVERITY_HIGH);
        window.getContext().addGLDebugListener(myGLDebugListener);

        window.invoke(true, new GLRunnable() {
            public boolean run(final GLAutoDrawable drawable) {
                drawable.getGL().glBindFramebuffer(-1, -1); // ERROR !
                return true;
            }
        } );

        if( window.getContext().isGLDebugMessageEnabled() ) {
            Assert.assertEquals(true, myGLDebugListener.received());
        }

        destroyWindow(window);
    }


    public static void main(final String args[]) throws IOException {
        final String tstname = TestGLDebug01NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

    public static class MyGLDebugListener implements GLDebugListener {
        int recSource;
        int recType;
        int recSeverity;

        String recMsg;
        int recId;
        boolean received = false;

        public MyGLDebugListener(final int recSource, final int recType, final int recSeverity) {
            this.recSource = recSource;
            this.recType = recType;
            this.recSeverity = recSeverity;
            this.recMsg = null;
            this.recId = -1;

        }
        public MyGLDebugListener(final String recMsg, final int recId) {
            this.recSource = -1;
            this.recType = -1;
            this.recSeverity = -1;
            this.recMsg = recMsg;
            this.recId = recId;
        }

        public boolean received() { return received; }

        public void messageSent(final GLDebugMessage event) {
            System.err.println("XXX: "+event);
            if(null != recMsg && recMsg.equals(event.getDbgMsg()) && recId == event.getDbgId()) {
                received = true;
            } else if(0 <= recSource && recSource == event.getDbgSource() &&
                                        recType == event.getDbgType() &&
                                        recSeverity== event.getDbgSeverity() ) {
                received = true;
            }
            // Thread.dumpStack();
        }
    }
}

