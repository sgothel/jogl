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

package com.jogamp.opengl.test.junit.jogl.demos.es2.swt;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.nativewindow.swt.SWTAccessor;
import com.jogamp.opengl.swt.GLCanvas;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;

import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.Point;
import javax.media.nativewindow.util.PointImmutable;
import javax.media.nativewindow.util.DimensionImmutable;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLProfile;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGearsES2SWT extends UITestCase {
    static int screenIdx = 0;
    static PointImmutable wpos;
    static DimensionImmutable wsize, rwsize=null;

    static long duration = 500; // ms
    static boolean opaque = true;
    static int forceAlpha = -1;
    static boolean fullscreen = false;
    static boolean pmvUseBackingArray = true;
    static int swapInterval = 1;
    static boolean showFPS = false;
    static int loops = 1;
    static boolean loop_shutdown = false;
    static boolean forceES2 = false;
    static boolean forceGL3 = false;
    static boolean mainRun = false;
    static boolean exclusiveContext = false;

    @BeforeClass
    public static void initClass() {
        if(null == wsize) {
            wsize = new Dimension(640, 480);
        }
    }

    @AfterClass
    public static void releaseClass() {
    }

    Display display = null;
    Shell shell = null;
    Composite composite = null;

    @Before
    public void init() {
        SWTAccessor.invoke(true, new Runnable() {
            public void run() {
                display = new Display();
                Assert.assertNotNull( display );
            }});
        display.syncExec(new Runnable() {
            public void run() {
                shell = new Shell( display );
                Assert.assertNotNull( shell );
                shell.setLayout( new FillLayout() );
                composite = new Composite( shell, SWT.NONE );
                composite.setLayout( new FillLayout() );
                Assert.assertNotNull( composite );
            }});
    }

    @After
    public void release() {
        Assert.assertNotNull( display );
        Assert.assertNotNull( shell );
        Assert.assertNotNull( composite );
        try {
            display.syncExec(new Runnable() {
               public void run() {
                composite.dispose();
                shell.dispose();
               }});
            SWTAccessor.invoke(true, new Runnable() {
               public void run() {
                display.dispose();
               }});
        }
        catch( Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        display = null;
        shell = null;
        composite = null;
    }

    protected void runTestGL(GLCapabilitiesImmutable caps) throws InterruptedException, InvocationTargetException {
        System.err.println("requested: vsync "+swapInterval+", "+caps);

        final GLCanvas canvas = GLCanvas.create( composite, 0, caps, null);
        Assert.assertNotNull( canvas );

        final GearsES2 demo = new GearsES2(swapInterval);
        demo.setPMVUseBackingArray(pmvUseBackingArray);
        canvas.addGLEventListener(demo);

        Animator animator = new Animator();
        animator.setModeBits(false, Animator.MODE_EXPECT_AWT_RENDERING_THREAD);
        animator.setExclusiveContext(exclusiveContext);

        animator.add(canvas);
        animator.start();
        Assert.assertTrue(animator.isStarted());
        Assert.assertTrue(animator.isAnimating());
        Assert.assertEquals(exclusiveContext ? animator.getThread() : null, canvas.getExclusiveContextThread());

        display.syncExec( new Runnable() {
           public void run() {
              shell.setText( getSimpleTestName(".") );
              shell.setSize( wsize.getWidth(), wsize.getHeight() );
              if( null != wpos ) {
                  shell.setLocation( wpos.getX(), wpos.getY() );
              }
              shell.open();
           }
        });

        animator.setUpdateFPSFrames(60, showFPS ? System.err : null);

        while(animator.isAnimating() && !canvas.isRealized() && animator.getTotalFPSDuration()<duration) {
            if( !display.readAndDispatch() ) {
                // blocks on linux .. display.sleep();
                Thread.sleep(10);
            }
        }
        System.err.println("NW chosen: "+canvas.getDelegatedDrawable().getChosenGLCapabilities());
        System.err.println("GL chosen: "+canvas.getChosenGLCapabilities());
        System.err.println("window pos/siz: "+canvas.getLocation()+" "+canvas.getWidth()+"x"+canvas.getHeight());

        if( null != rwsize ) {
            for(int i=0; i<50; i++) { // 500 ms dispatched delay
                if( !display.readAndDispatch() ) {
                    // blocks on linux .. display.sleep();
                    Thread.sleep(10);
                }
            }
            display.syncExec( new Runnable() {
               public void run() {
                  shell.setSize( rwsize.getWidth(), rwsize.getHeight() );
               }
            });
            System.err.println("window resize pos/siz: "+canvas.getLocation()+" "+canvas.getWidth()+"x"+canvas.getHeight());
        }

        while(animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            if( !display.readAndDispatch() ) {
                // blocks on linux .. display.sleep();
                Thread.sleep(10);
            }
        }

        Assert.assertEquals(exclusiveContext ? animator.getThread() : null, canvas.getExclusiveContextThread());
        animator.stop();
        Assert.assertFalse(animator.isAnimating());
        Assert.assertFalse(animator.isStarted());
        Assert.assertEquals(null, canvas.getExclusiveContextThread());

        display.syncExec(new Runnable() {
           public void run() {
               canvas.dispose();
           } } );
    }

    @Test
    public void test01GL2ES2() throws InterruptedException, InvocationTargetException {
        for(int i=1; i<=loops; i++) {
            System.err.println("Loop "+i+"/"+loops);
            final GLProfile glp;
            if(forceGL3) {
                glp = GLProfile.get(GLProfile.GL3);
            } else if(forceES2) {
                glp = GLProfile.get(GLProfile.GLES2);
            } else {
                glp = GLProfile.getGL2ES2();
            }
            final GLCapabilities caps = new GLCapabilities( glp );
            caps.setBackgroundOpaque(opaque);
            if(-1 < forceAlpha) {
                caps.setAlphaBits(forceAlpha);
            }
            runTestGL(caps);
            if(loop_shutdown) {
                GLProfile.shutdown();
            }
        }
    }

    @Test
    public void test02GL3() throws InterruptedException, InvocationTargetException {
        if(mainRun) return;

        if( !GLProfile.isAvailable(GLProfile.GL3) ) {
            System.err.println("GL3 n/a");
            return;
        }
        final GLProfile glp = GLProfile.get(GLProfile.GL3);
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps);
    }

    public static void main(String args[]) throws IOException {
        mainRun = true;

        int x=0, y=0, w=640, h=480, rw=-1, rh=-1;
        boolean usePos = false;

        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-translucent")) {
                opaque = false;
            } else if(args[i].equals("-forceAlpha")) {
                i++;
                forceAlpha = MiscUtils.atoi(args[i], 0);
            } else if(args[i].equals("-fullscreen")) {
                fullscreen = true;
            } else if(args[i].equals("-pmvDirect")) {
                pmvUseBackingArray = false;
            } else if(args[i].equals("-vsync")) {
                i++;
                swapInterval = MiscUtils.atoi(args[i], swapInterval);
            } else if(args[i].equals("-exclctx")) {
                exclusiveContext = true;
            } else if(args[i].equals("-es2")) {
                forceES2 = true;
            } else if(args[i].equals("-gl3")) {
                forceGL3 = true;
            } else if(args[i].equals("-showFPS")) {
                showFPS = true;
            } else if(args[i].equals("-width")) {
                i++;
                w = MiscUtils.atoi(args[i], w);
            } else if(args[i].equals("-height")) {
                i++;
                h = MiscUtils.atoi(args[i], h);
            } else if(args[i].equals("-x")) {
                i++;
                x = MiscUtils.atoi(args[i], x);
                usePos = true;
            } else if(args[i].equals("-y")) {
                i++;
                y = MiscUtils.atoi(args[i], y);
                usePos = true;
            } else if(args[i].equals("-rwidth")) {
                i++;
                rw = MiscUtils.atoi(args[i], rw);
            } else if(args[i].equals("-rheight")) {
                i++;
                rh = MiscUtils.atoi(args[i], rh);
            } else if(args[i].equals("-screen")) {
                i++;
                screenIdx = MiscUtils.atoi(args[i], 0);
            } else if(args[i].equals("-loops")) {
                i++;
                loops = MiscUtils.atoi(args[i], 1);
            } else if(args[i].equals("-loop-shutdown")) {
                loop_shutdown = true;
            }
        }
        wsize = new Dimension(w, h);
        if( 0 < rw && 0 < rh ) {
            rwsize = new Dimension(rw, rh);
        }

        if(usePos) {
            wpos = new Point(x, y);
        }
        System.err.println("position "+wpos);
        System.err.println("size "+wsize);
        System.err.println("resize "+rwsize);
        System.err.println("screen "+screenIdx);
        System.err.println("translucent "+(!opaque));
        System.err.println("forceAlpha "+forceAlpha);
        System.err.println("fullscreen "+fullscreen);
        System.err.println("pmvDirect "+(!pmvUseBackingArray));
        System.err.println("loops "+loops);
        System.err.println("loop shutdown "+loop_shutdown);
        System.err.println("forceES2 "+forceES2);
        System.err.println("forceGL3 "+forceGL3);
        System.err.println("swapInterval "+swapInterval);
        System.err.println("exclusiveContext "+exclusiveContext);

        org.junit.runner.JUnitCore.main(TestGearsES2SWT.class.getName());
    }
}
