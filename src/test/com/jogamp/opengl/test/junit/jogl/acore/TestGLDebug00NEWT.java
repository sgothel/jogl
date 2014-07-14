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

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDebugListener;
import javax.media.opengl.GLDebugMessage;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLProfile;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.opengl.test.junit.util.UITestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLDebug00NEWT extends UITestCase {

    static String dbgTstMsg0 = "Hello World";
    static int dbgTstId0 = 42;

    public static class WindowContext {
        public final Window window;
        public final GLContext context;

        public WindowContext(final Window w, final GLContext c) {
            window = w;
            context = c;
        }
    }

    WindowContext createWindow(final GLProfile glp, final boolean debugGL) {
        final GLCapabilities caps = new GLCapabilities(glp);
        //
        // Create native windowing resources .. X11/Win/OSX
        //
        final Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);

        final Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);

        final Window window = NewtFactory.createWindow(screen, caps);
        Assert.assertNotNull(window);
        window.setSize(128, 128);
        window.setVisible(true);

        final GLDrawableFactory factory = GLDrawableFactory.getFactory(glp);
        final GLDrawable drawable = factory.createGLDrawable(window);
        Assert.assertNotNull(drawable);

        drawable.setRealized(true);

        final GLContext context = drawable.createContext(null);
        Assert.assertNotNull(context);

        context.enableGLDebugMessage(debugGL);

        final int res = context.makeCurrent();
        Assert.assertTrue(GLContext.CONTEXT_CURRENT_NEW==res || GLContext.CONTEXT_CURRENT==res);

        return new WindowContext(window, context);
    }

    void destroyWindow(final WindowContext winctx) {
        final GLDrawable drawable = winctx.context.getGLDrawable();

        Assert.assertNotNull(winctx.context);
        winctx.context.destroy();

        Assert.assertNotNull(drawable);
        drawable.setRealized(false);

        Assert.assertNotNull(winctx.window);
        winctx.window.destroy();
    }


    void test01GLDebug01EnableDisable(final boolean enable) throws InterruptedException {
        final GLProfile glp = GLProfile.getDefault();

        final WindowContext winctx = createWindow(glp, enable);
        final String glDebugExt = winctx.context.getGLDebugMessageExtension();
        System.err.println("glDebug extension: "+glDebugExt);
        System.err.println("glDebug enabled: "+winctx.context.isGLDebugMessageEnabled());
        System.err.println("glDebug sync: "+winctx.context.isGLDebugSynchronous());
        System.err.println("context version: "+winctx.context.getGLVersion());

        Assert.assertEquals((null == glDebugExt) ? false : enable, winctx.context.isGLDebugMessageEnabled());

        destroyWindow(winctx);
    }

    @Test
    public void test01GLDebugDisabled() throws InterruptedException {
        test01GLDebug01EnableDisable(false);
    }

    @Test
    public void test01GLDebugEnabled() throws InterruptedException {
        test01GLDebug01EnableDisable(true);
    }

    @Test
    public void test02GLDebugError() throws InterruptedException {
        final GLProfile glp = GLProfile.getDefault();

        final WindowContext winctx = createWindow(glp, true);

        final MyGLDebugListener myGLDebugListener = new MyGLDebugListener(
                GL2ES2.GL_DEBUG_SOURCE_API,
                GL2ES2.GL_DEBUG_TYPE_ERROR,
                GL2ES2.GL_DEBUG_SEVERITY_HIGH);
        winctx.context.addGLDebugListener(myGLDebugListener);

        final GL gl = winctx.context.getGL();

        gl.glBindFramebuffer(-1, -1); // ERROR !

        if( winctx.context.isGLDebugMessageEnabled() ) {
            Assert.assertEquals(true, myGLDebugListener.received());
        }

        destroyWindow(winctx);
    }

    @Test
    public void test03GLDebugInsert() throws InterruptedException {
        final GLProfile glp = GLProfile.getDefault();
        final WindowContext winctx = createWindow(glp, true);
        final MyGLDebugListener myGLDebugListener = new MyGLDebugListener(dbgTstMsg0, dbgTstId0);
        winctx.context.addGLDebugListener(myGLDebugListener);

        final String glDebugExt = winctx.context.getGLDebugMessageExtension();
        Assert.assertEquals((null == glDebugExt) ? false : true, winctx.context.isGLDebugMessageEnabled());

        if( winctx.context.isGLDebugMessageEnabled() ) {
            winctx.context.glDebugMessageInsert(GL2ES2.GL_DEBUG_SOURCE_APPLICATION,
                                                GL2ES2.GL_DEBUG_TYPE_OTHER,
                                                dbgTstId0,
                                                GL2ES2.GL_DEBUG_SEVERITY_MEDIUM, dbgTstMsg0);
            Assert.assertEquals(true, myGLDebugListener.received());
        }

        destroyWindow(winctx);
    }


    public static void main(final String args[]) throws IOException {
        final String tstname = TestGLDebug00NEWT.class.getName();
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

