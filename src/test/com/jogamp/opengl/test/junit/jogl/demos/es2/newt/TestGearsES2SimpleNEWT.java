/**
 * Copyright 2015 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.test.junit.jogl.demos.es2.newt;

import java.io.IOException;
import java.net.URLConnection;

import com.jogamp.common.util.IOUtil;
import com.jogamp.newt.Display;
import com.jogamp.newt.Display.PointerIcon;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.opengl.util.NEWTDemoListener;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.PNGPixelRect;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLProfile;

import jogamp.newt.driver.PNGIcon;

import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGearsES2SimpleNEWT extends UITestCase {
    static final DimensionImmutable wsize = new Dimension(800, 600);
    static final float[] reqSurfacePixelScale = new float[] { ScalableSurface.AUTOMAX_PIXELSCALE, ScalableSurface.AUTOMAX_PIXELSCALE };

    static final int swapInterval = 1;

    static long duration = 500; // ms
    static boolean opaque = true;

    private void setTitle(final Window win, final GLCapabilitiesImmutable caps) {
        final float[] sDPI = win.getPixelsPerMM(new float[2]);
        sDPI[0] *= 25.4f;
        sDPI[1] *= 25.4f;
        win.setTitle("GLWindow: swapI "+swapInterval+", win: "+win.getBounds()+", pix: "+win.getSurfaceWidth()+"x"+win.getSurfaceHeight()+", sDPI "+sDPI[0]+" x "+sDPI[1]);
    }
    protected void runTestGL(final GLCapabilitiesImmutable caps, final boolean undecorated) throws InterruptedException {
        System.err.println("requested: vsync "+swapInterval+", "+caps);
        final Display dpy = NewtFactory.createDisplay(null);
        final Screen screen = NewtFactory.createScreen(dpy, 0);
        final GLWindow glWindow = GLWindow.create(screen, caps);
        glWindow.setSurfaceScale(reqSurfacePixelScale);
        final float[] valReqSurfacePixelScale = glWindow.getRequestedSurfaceScale(new float[2]);
        glWindow.setSize(wsize.getWidth(), wsize.getHeight());
        glWindow.setUndecorated(undecorated);

        final GearsES2 demo = new GearsES2(swapInterval);
        demo.setValidateBuffers(true);
        glWindow.addGLEventListener(demo);

        final SnapshotGLEventListener snap = new SnapshotGLEventListener();
        glWindow.addGLEventListener(snap);

        final Animator animator = new Animator();

        final QuitAdapter quitAdapter = new QuitAdapter();
        glWindow.addKeyListener(quitAdapter);
        glWindow.addWindowListener(quitAdapter);

        glWindow.addWindowListener(new WindowAdapter() {
            public void windowResized(final WindowEvent e) {
                System.err.println("window resized: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight());
                setTitle(glWindow, caps);
            }
            public void windowMoved(final WindowEvent e) {
                System.err.println("window moved:   "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight());
                setTitle(glWindow, caps);
            }
        });

        final PointerIcon[] pointerIcons = { null, null, null, null, null };
        {
            final Display disp = glWindow.getScreen().getDisplay();
            disp.createNative();
            int idx = 0;
            {
                PointerIcon _pointerIcon = null;
                final IOUtil.ClassResources res = new IOUtil.ClassResources(new String[] { "newt/data/cross-grey-alpha-16x16.png" }, glWindow.getClass().getClassLoader(), null);
                try {
                    _pointerIcon = disp.createPointerIcon(res, 8, 8);
                    System.err.printf("Create PointerIcon #%02d: %s%n", idx, _pointerIcon.toString());
                } catch (final Exception e) {
                    e.printStackTrace();
                }
                pointerIcons[idx] = _pointerIcon;
            }
            idx++;
            {
                PointerIcon _pointerIcon = null;
                final IOUtil.ClassResources res = new IOUtil.ClassResources(new String[] { "newt/data/pointer-grey-alpha-16x24.png" }, glWindow.getClass().getClassLoader(), null);
                try {
                    _pointerIcon = disp.createPointerIcon(res, 0, 0);
                    System.err.printf("Create PointerIcon #%02d: %s%n", idx, _pointerIcon.toString());
                } catch (final Exception e) {
                    e.printStackTrace();
                }
                pointerIcons[idx] = _pointerIcon;
            }
            idx++;
            {
                PointerIcon _pointerIcon = null;
                final IOUtil.ClassResources res = new IOUtil.ClassResources(new String[] { "arrow-red-alpha-64x64.png" }, glWindow.getClass().getClassLoader(), null);
                try {
                    _pointerIcon = disp.createPointerIcon(res, 0, 0);
                    System.err.printf("Create PointerIcon #%02d: %s%n", idx, _pointerIcon.toString());
                } catch (final Exception e) {
                    e.printStackTrace();
                }
                pointerIcons[idx] = _pointerIcon;
            }
            idx++;
            {
                PointerIcon _pointerIcon = null;
                final IOUtil.ClassResources res = new IOUtil.ClassResources(new String[] { "arrow-blue-alpha-64x64.png" }, glWindow.getClass().getClassLoader(), null);
                try {
                    _pointerIcon = disp.createPointerIcon(res, 0, 0);
                    System.err.printf("Create PointerIcon #%02d: %s%n", idx, _pointerIcon.toString());
                } catch (final Exception e) {
                    e.printStackTrace();
                }
                pointerIcons[idx] = _pointerIcon;
            }
            idx++;
            if( PNGIcon.isAvailable() ) {
                PointerIcon _pointerIcon = null;
                final IOUtil.ClassResources res = new IOUtil.ClassResources(new String[] { "jogamp-pointer-64x64.png" }, glWindow.getClass().getClassLoader(), null);
                try {
                    final URLConnection urlConn = res.resolve(0);
                    final PNGPixelRect image = PNGPixelRect.read(urlConn.getInputStream(), null, false /* directBuffer */, 0 /* destMinStrideInBytes */, false /* destIsGLOriented */);
                    System.err.printf("Create PointerIcon #%02d: %s%n", idx, image.toString());
                    _pointerIcon = disp.createPointerIcon(image, 32, 0);
                    System.err.printf("Create PointerIcon #%02d: %s%n", idx, _pointerIcon.toString());
                } catch (final Exception e) {
                    e.printStackTrace();
                }
                pointerIcons[idx] = _pointerIcon;
            }
            idx++;
        }

        final NEWTDemoListener newtDemoListener = new NEWTDemoListener(glWindow, pointerIcons);
        glWindow.addKeyListener(newtDemoListener);
        glWindow.addMouseListener(newtDemoListener);

        animator.add(glWindow);
        animator.start();

        glWindow.setVisible(true);
        animator.setUpdateFPSFrames(60, System.err);

        System.err.println("NW chosen: "+glWindow.getDelegatedWindow().getChosenCapabilities());
        System.err.println("GL chosen: "+glWindow.getChosenCapabilities());
        System.err.println("window pos/siz: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", "+glWindow.getInsets());

        final float[] hasSurfacePixelScale1 = glWindow.getCurrentSurfaceScale(new float[2]);
        System.err.println("HiDPI PixelScale: "+reqSurfacePixelScale[0]+"x"+reqSurfacePixelScale[1]+" (req) -> "+
                           valReqSurfacePixelScale[0]+"x"+valReqSurfacePixelScale[1]+" (val) -> "+
                           hasSurfacePixelScale1[0]+"x"+hasSurfacePixelScale1[1]+" (has)");
        setTitle(glWindow, caps);

        snap.setMakeSnapshot();

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while(!quitAdapter.shouldQuit() && t1-t0<duration) {
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
        }

        animator.stop();

        glWindow.destroy();
    }

    @Test
    public void test01_GL2ES2() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities( glp );
        caps.setBackgroundOpaque(opaque);
        runTestGL(caps, false);
    }

    public static void main(final String args[]) throws IOException {
        duration = 1000000; // ~16 min by default per main launch
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-translucent")) {
                opaque = false;
            }
        }
        org.junit.runner.JUnitCore.main(TestGearsES2SimpleNEWT.class.getName());
    }
}
