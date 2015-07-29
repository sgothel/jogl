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
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.Gamma;
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
                final IOUtil.ClassResources res = new IOUtil.ClassResources(glWindow.getClass(), new String[] { "newt/data/cross-grey-alpha-16x16.png" } );
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
                final IOUtil.ClassResources res = new IOUtil.ClassResources(glWindow.getClass(), new String[] { "newt/data/pointer-grey-alpha-16x24.png" } );
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
                final IOUtil.ClassResources res = new IOUtil.ClassResources(glWindow.getClass(), new String[] { "arrow-red-alpha-64x64.png" } );
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
                final IOUtil.ClassResources res = new IOUtil.ClassResources(glWindow.getClass(), new String[] { "arrow-blue-alpha-64x64.png" } );
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
                final IOUtil.ClassResources res = new IOUtil.ClassResources(glWindow.getClass(), new String[] { "jogamp-pointer-64x64.png" } );
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

        glWindow.addKeyListener(new KeyAdapter() {
            int pointerIconIdx = 0;
            float gamma = 1f;
            float brightness = 0f;
            float contrast = 1f;

            @Override
            public void keyPressed(final KeyEvent e) {
                if( e.isAutoRepeat() ) {
                    return;
                }
                if(e.getKeyChar()=='f') {
                    new Thread() {
                        public void run() {
                            final Thread t = glWindow.setExclusiveContextThread(null);
                            System.err.println("[set fullscreen  pre]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
                            if( glWindow.isFullscreen() ) {
                                glWindow.setFullscreen( false );
                            } else {
                                if( e.isAltDown() ) {
                                    glWindow.setFullscreen( null );
                                } else {
                                    glWindow.setFullscreen( true );
                                }
                            }
                            System.err.println("[set fullscreen post]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
                            glWindow.setExclusiveContextThread(t);
                    } }.start();
                } else if( e.getKeySymbol()== KeyEvent.VK_G ) {
                    new Thread() {
                        public void run() {
                            final float newGamma = gamma + ( e.isShiftDown() ? -0.1f : 0.1f );
                            System.err.println("[set gamma]: "+gamma+" -> "+newGamma);
                            if( Gamma.setDisplayGamma(glWindow, newGamma, brightness, contrast) ) {
                                gamma = newGamma;
                            }
                    } }.start();
                } else if(e.getKeyChar()=='a') {
                    new Thread() {
                        public void run() {
                            final Thread t = glWindow.setExclusiveContextThread(null);
                            System.err.println("[set alwaysontop pre]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
                            glWindow.setAlwaysOnTop(!glWindow.isAlwaysOnTop());
                            System.err.println("[set alwaysontop post]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
                            glWindow.setExclusiveContextThread(t);
                    } }.start();
                } else if(e.getKeyChar()=='d') {
                    new Thread() {
                        public void run() {
                            final Thread t = glWindow.setExclusiveContextThread(null);
                            // while( null != glWindow.getExclusiveContextThread() ) ;
                            System.err.println("[set undecorated  pre]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", d "+glWindow.isUndecorated()+", "+glWindow.getInsets());
                            glWindow.setUndecorated(!glWindow.isUndecorated());
                            System.err.println("[set undecorated post]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", d "+glWindow.isUndecorated()+", "+glWindow.getInsets());
                            glWindow.setExclusiveContextThread(t);
                    } }.start();
                } else if(e.getKeyChar()=='s') {
                    new Thread() {
                        public void run() {
                            final Thread t = glWindow.setExclusiveContextThread(null);
                            System.err.println("[set position  pre]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", "+glWindow.getInsets());
                            glWindow.setPosition(100, 100);
                            System.err.println("[set position post]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", "+glWindow.getInsets());
                            glWindow.setExclusiveContextThread(t);
                    } }.start();
                } else if(e.getKeyChar()=='c') {
                    new Thread() {
                        public void run() {
                            final Thread t = glWindow.setExclusiveContextThread(null);
                            System.err.println("[set pointer-icon pre]");
                            final PointerIcon currentPI = glWindow.getPointerIcon();
                            final PointerIcon newPI;
                            if( pointerIconIdx >= pointerIcons.length ) {
                                newPI=null;
                                pointerIconIdx=0;
                            } else {
                                newPI=pointerIcons[pointerIconIdx++];
                            }
                            glWindow.setPointerIcon( newPI );
                            System.err.println("[set pointer-icon post] "+currentPI+" -> "+glWindow.getPointerIcon());
                            glWindow.setExclusiveContextThread(t);
                    } }.start();
                } else if(e.getKeyChar()=='i') {
                    new Thread() {
                        public void run() {
                            final Thread t = glWindow.setExclusiveContextThread(null);
                            System.err.println("[set mouse visible pre]: "+glWindow.isPointerVisible());
                            glWindow.setPointerVisible(!glWindow.isPointerVisible());
                            System.err.println("[set mouse visible post]: "+glWindow.isPointerVisible());
                            glWindow.setExclusiveContextThread(t);
                    } }.start();
                } else if(e.getKeyChar()=='j') {
                    new Thread() {
                        public void run() {
                            final Thread t = glWindow.setExclusiveContextThread(null);
                            System.err.println("[set mouse confined pre]: "+glWindow.isPointerConfined());
                            glWindow.confinePointer(!glWindow.isPointerConfined());
                            System.err.println("[set mouse confined post]: "+glWindow.isPointerConfined());
                            if(!glWindow.isPointerConfined()) {
                                demo.setConfinedFixedCenter(false);
                            }
                            glWindow.setExclusiveContextThread(t);
                    } }.start();
                } else if(e.getKeyChar()=='J') {
                    new Thread() {
                        public void run() {
                            final Thread t = glWindow.setExclusiveContextThread(null);
                            System.err.println("[set mouse confined pre]: "+glWindow.isPointerConfined());
                            glWindow.confinePointer(!glWindow.isPointerConfined());
                            System.err.println("[set mouse confined post]: "+glWindow.isPointerConfined());
                            demo.setConfinedFixedCenter(glWindow.isPointerConfined());
                            glWindow.setExclusiveContextThread(t);
                    } }.start();
                } else if(e.getKeyChar()=='w') {
                    new Thread() {
                        public void run() {
                            final Thread t = glWindow.setExclusiveContextThread(null);
                            System.err.println("[set mouse pos pre]");
                            glWindow.warpPointer(glWindow.getSurfaceWidth()/2, glWindow.getSurfaceHeight()/2);
                            System.err.println("[set mouse pos post]");
                            glWindow.setExclusiveContextThread(t);
                    } }.start();
                } else if(e.getKeyChar()=='x') {
                    final float[] hadSurfacePixelScale = glWindow.getCurrentSurfaceScale(new float[2]);
                    final float[] reqSurfacePixelScale;
                    if( hadSurfacePixelScale[0] == ScalableSurface.IDENTITY_PIXELSCALE ) {
                        reqSurfacePixelScale = new float[] { ScalableSurface.AUTOMAX_PIXELSCALE, ScalableSurface.AUTOMAX_PIXELSCALE };
                    } else {
                        reqSurfacePixelScale = new float[] { ScalableSurface.IDENTITY_PIXELSCALE, ScalableSurface.IDENTITY_PIXELSCALE };
                    }
                    System.err.println("[set PixelScale pre]: had "+hadSurfacePixelScale[0]+"x"+hadSurfacePixelScale[1]+" -> req "+reqSurfacePixelScale[0]+"x"+reqSurfacePixelScale[1]);
                    glWindow.setSurfaceScale(reqSurfacePixelScale);
                    final float[] valReqSurfacePixelScale = glWindow.getRequestedSurfaceScale(new float[2]);
                    final float[] hasSurfacePixelScale1 = glWindow.getCurrentSurfaceScale(new float[2]);
                    System.err.println("[set PixelScale post]: "+hadSurfacePixelScale[0]+"x"+hadSurfacePixelScale[1]+" (had) -> "+
                            reqSurfacePixelScale[0]+"x"+reqSurfacePixelScale[1]+" (req) -> "+
                            valReqSurfacePixelScale[0]+"x"+valReqSurfacePixelScale[1]+" (val) -> "+
                            hasSurfacePixelScale1[0]+"x"+hasSurfacePixelScale1[1]+" (has)");
                    setTitle(glWindow, caps);
                }
            }
        });
        glWindow.addMouseListener(new MouseAdapter() {
            public void mouseClicked(final MouseEvent e) {
                if(e.getClickCount() == 2 && e.getPointerCount() == 1) {
                    glWindow.setFullscreen(!glWindow.isFullscreen());
                    System.err.println("setFullscreen: "+glWindow.isFullscreen());
                }
            }
         });

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
        runTestGL(caps, false);
    }

    public static void main(final String args[]) throws IOException {
        duration = 1000000; // ~16 min by default per main launch
        org.junit.runner.JUnitCore.main(TestGearsES2SimpleNEWT.class.getName());
    }
}
