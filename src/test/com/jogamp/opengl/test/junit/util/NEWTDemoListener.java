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
package com.jogamp.opengl.test.junit.util;

import java.net.URLConnection;

import com.jogamp.common.util.IOUtil;
import com.jogamp.nativewindow.CapabilitiesImmutable;
import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.newt.Display;
import com.jogamp.newt.Display.PointerIcon;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Gamma;
import com.jogamp.opengl.util.PNGPixelRect;

import jogamp.newt.driver.PNGIcon;

public class NEWTDemoListener extends MouseAdapter implements KeyListener {
    protected final GLWindow glWindow;
    protected final QuitAdapter quitAdapter;
    final PointerIcon[] pointerIcons;
    int pointerIconIdx = 0;
    float gamma = 1f;
    float brightness = 0f;
    float contrast = 1f;
    boolean confinedFixedCenter = false;

    public NEWTDemoListener(final GLWindow glWin, final QuitAdapter quitAdapter, final PointerIcon[] pointerIcons) {
        this.glWindow = glWin;
        this.quitAdapter = quitAdapter;
        if( null != pointerIcons ) {
            this.pointerIcons = pointerIcons;
        } else {
            this.pointerIcons = createPointerIcons(glWindow);
        }
    }
    public NEWTDemoListener(final GLWindow glWin, final PointerIcon[] pointerIcons) {
        this(glWin, null, pointerIcons);
    }
    public NEWTDemoListener(final GLWindow glWin) {
        this(glWin, null, null);
    }

    protected void printlnState(final String prelude) {
        System.err.println(prelude+": "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets()+", state "+glWindow.getStateMaskString());
    }
    protected void printlnState(final String prelude, final String post) {
        System.err.println(prelude+": "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets()+", state "+glWindow.getStateMaskString()+", "+post);
    }
    protected void quitAdapterOff() {
        if( null != quitAdapter ) {
            quitAdapter.enable(false);
        }
    }
    protected void quitAdapterOn() {
        if( null != quitAdapter ) {
            quitAdapter.clear();
            quitAdapter.enable(true);
        }
    }

    @Override
    public void keyPressed(final KeyEvent e) {
        if( e.isAutoRepeat() || e.isConsumed() ) {
            return;
        }
        final int keySymbol = e.getKeySymbol();
        switch(keySymbol) {
            case KeyEvent.VK_SPACE:
                e.setConsumed(true);
                new Thread() {
                    public void run() {
                        if(glWindow.getAnimator().isPaused()) {
                            glWindow.getAnimator().resume();
                        } else {
                            glWindow.getAnimator().pause();
                        }
                    }
                }.run();
                break;
            case KeyEvent.VK_A:
                e.setConsumed(true);
                new Thread() {
                    public void run() {
                        final Thread t = glWindow.setExclusiveContextThread(null);
                        printlnState("[set alwaysontop pre]");
                        glWindow.setAlwaysOnTop(!glWindow.isAlwaysOnTop());
                        printlnState("[set alwaysontop post]");
                        glWindow.setExclusiveContextThread(t);
                } }.start();
                break;
            case KeyEvent.VK_B:
                e.setConsumed(true);
                new Thread() {
                    public void run() {
                        final Thread t = glWindow.setExclusiveContextThread(null);
                        printlnState("[set alwaysonbottom pre]");
                        glWindow.setAlwaysOnBottom(!glWindow.isAlwaysOnBottom());
                        printlnState("[set alwaysonbottom post]");
                        glWindow.setExclusiveContextThread(t);
                } }.start();
                break;
            case KeyEvent.VK_C:
                e.setConsumed(true);
                new Thread() {
                    public void run() {
                        if( null != pointerIcons ) {
                            final Thread t = glWindow.setExclusiveContextThread(null);
                            printlnState("[set pointer-icon pre]");
                            final PointerIcon currentPI = glWindow.getPointerIcon();
                            final PointerIcon newPI;
                            if( pointerIconIdx >= pointerIcons.length ) {
                                newPI=null;
                                pointerIconIdx=0;
                            } else {
                                newPI=pointerIcons[pointerIconIdx++];
                            }
                            glWindow.setPointerIcon( newPI );
                            printlnState("[set pointer-icon post]", currentPI+" -> "+glWindow.getPointerIcon());
                            glWindow.setExclusiveContextThread(t);
                        }
                } }.start();
                break;
            case KeyEvent.VK_D:
                e.setConsumed(true);
                new Thread() {
                    public void run() {
                        final Thread t = glWindow.setExclusiveContextThread(null);
                        // while( null != glWindow.getExclusiveContextThread() ) ;
                        printlnState("[set undecorated pre]");
                        glWindow.setUndecorated(!glWindow.isUndecorated());
                        printlnState("[set undecorated post]");
                        glWindow.setExclusiveContextThread(t);
                } }.start();
                break;
            case KeyEvent.VK_F:
                e.setConsumed(true);
                quitAdapterOff();
                new Thread() {
                    public void run() {
                        final Thread t = glWindow.setExclusiveContextThread(null);
                        printlnState("[set fullscreen pre]");
                        if( glWindow.isFullscreen() ) {
                            glWindow.setFullscreen( false );
                        } else {
                            if( e.isAltDown() ) {
                                glWindow.setFullscreen( null );
                            } else {
                                glWindow.setFullscreen( true );
                            }
                        }
                        printlnState("[set fullscreen post]");
                        glWindow.setExclusiveContextThread(t);
                        quitAdapterOn();
                } }.start();
                break;
            case KeyEvent.VK_G:
                e.setConsumed(true);
                new Thread() {
                    public void run() {
                        final float newGamma = gamma + ( e.isShiftDown() ? -0.1f : 0.1f );
                        System.err.println("[set gamma]: "+gamma+" -> "+newGamma);
                        if( Gamma.setDisplayGamma(glWindow, newGamma, brightness, contrast) ) {
                            gamma = newGamma;
                        }
                } }.start();
                break;
            case KeyEvent.VK_I:
                e.setConsumed(true);
                new Thread() {
                    public void run() {
                        final Thread t = glWindow.setExclusiveContextThread(null);
                        printlnState("[set pointer-visible pre]");
                        glWindow.setPointerVisible(!glWindow.isPointerVisible());
                        printlnState("[set pointer-visible post]");
                        glWindow.setExclusiveContextThread(t);
                } }.start();
                break;
            case KeyEvent.VK_J:
                e.setConsumed(true);
                new Thread() {
                    public void run() {
                        final Thread t = glWindow.setExclusiveContextThread(null);
                        printlnState("[set pointer-confined pre]", "warp-center: "+e.isShiftDown());
                        final boolean confine = !glWindow.isPointerConfined();
                        glWindow.confinePointer(confine);
                        printlnState("[set pointer-confined post]", "warp-center: "+e.isShiftDown());
                        if( e.isShiftDown() ) {
                            setConfinedFixedCenter(confine);
                        } else if( !confine ) {
                            setConfinedFixedCenter(false);
                        }
                        glWindow.setExclusiveContextThread(t);
                } }.start();
                break;
            case KeyEvent.VK_M:
                e.setConsumed(true);
                new Thread() {
                    public void run() {
                        // none:  max-v
                        // alt:   max-h
                        // shift: max-hv
                        // ctrl:  max-off
                        final boolean horz, vert;
                        if( e.isControlDown() ) {
                            horz = false;
                            vert = false;
                        } else if( e.isShiftDown() ) {
                            final boolean anyMax = glWindow.isMaximizedHorz() || glWindow.isMaximizedVert();
                            horz = !anyMax;
                            vert = !anyMax;
                        } else if( !e.isAltDown() ) {
                            horz = glWindow.isMaximizedHorz();
                            vert = !glWindow.isMaximizedVert();
                        } else if( e.isAltDown() ) {
                            horz = !glWindow.isMaximizedHorz();
                            vert = glWindow.isMaximizedVert();
                        } else {
                            vert = glWindow.isMaximizedVert();
                            horz = glWindow.isMaximizedHorz();
                        }
                        final Thread t = glWindow.setExclusiveContextThread(null);
                        printlnState("[set maximize pre]", "max[vert "+vert+", horz "+horz+"]");
                        glWindow.setMaximized(horz, vert);
                        printlnState("[set maximize post]", "max[vert "+vert+", horz "+horz+"]");
                        glWindow.setExclusiveContextThread(t);
                } }.start();
                break;
            case KeyEvent.VK_P:
                e.setConsumed(true);
                new Thread() {
                    public void run() {
                        final Thread t = glWindow.setExclusiveContextThread(null);
                        printlnState("[set position pre]");
                        glWindow.setPosition(100, 100);
                        printlnState("[set position post]");
                        glWindow.setExclusiveContextThread(t);
                } }.start();
                break;
            case KeyEvent.VK_R:
                e.setConsumed(true);
                new Thread() {
                    public void run() {
                        final Thread t = glWindow.setExclusiveContextThread(null);
                        printlnState("[set resizable pre]");
                        glWindow.setResizable(!glWindow.isResizable());
                        printlnState("[set resizable post]");
                        glWindow.setExclusiveContextThread(t);
                } }.start();
                break;
            case KeyEvent.VK_S:
                e.setConsumed(true);
                new Thread() {
                    public void run() {
                        final Thread t = glWindow.setExclusiveContextThread(null);
                        printlnState("[set sticky pre]");
                        glWindow.setSticky(!glWindow.isSticky());
                        printlnState("[set sticky post]");
                        glWindow.setExclusiveContextThread(t);
                } }.start();
                break;
            case KeyEvent.VK_V:
                e.setConsumed(true);
                new Thread() {
                    public void run() {
                        final boolean wasVisible = glWindow.isVisible();
                        {
                            final Thread t = glWindow.setExclusiveContextThread(null);
                            printlnState("[set visible pre]");
                            glWindow.setVisible(!wasVisible);
                            printlnState("[set visible post]");
                            glWindow.setExclusiveContextThread(t);
                        }
                        if( wasVisible ) {
                            try {
                                Thread.sleep(5000);
                            } catch (final InterruptedException e) {
                                e.printStackTrace();
                            }
                            final Thread t = glWindow.setExclusiveContextThread(null);
                            printlnState("[reset visible pre]");
                            glWindow.setVisible(true);
                            printlnState("[reset visible post]");
                            glWindow.setExclusiveContextThread(t);
                        }
                } }.start();
                break;
            case KeyEvent.VK_W:
                e.setConsumed(true);
                new Thread() {
                    public void run() {
                        final Thread t = glWindow.setExclusiveContextThread(null);
                        printlnState("[set pointer-pos pre]");
                        glWindow.warpPointer(glWindow.getSurfaceWidth()/2, glWindow.getSurfaceHeight()/2);
                        printlnState("[set pointer-pos post]");
                        glWindow.setExclusiveContextThread(t);
                } }.start();
                break;
            case KeyEvent.VK_X:
                e.setConsumed(true);
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
                setTitle();
        }
    }
    @Override
    public void keyReleased(final KeyEvent e) { }

    public void setConfinedFixedCenter(final boolean v) {
        confinedFixedCenter = v;
    }
    @Override
    public void mouseMoved(final MouseEvent e) {
        if( e.isConfined() ) {
            mouseCenterWarp(e);
        }
    }
    @Override
    public void mouseDragged(final MouseEvent e) {
        if( e.isConfined() ) {
            mouseCenterWarp(e);
        }
    }
    @Override
    public void mouseClicked(final MouseEvent e) {
        if(e.getClickCount() == 2 && e.getPointerCount() == 1) {
            glWindow.setFullscreen(!glWindow.isFullscreen());
            System.err.println("setFullscreen: "+glWindow.isFullscreen());
        }
    }
    private void mouseCenterWarp(final MouseEvent e) {
        if(e.isConfined() && confinedFixedCenter ) {
            final int x=glWindow.getSurfaceWidth()/2;
            final int y=glWindow.getSurfaceHeight()/2;
            glWindow.warpPointer(x, y);
        }
    }

    public void setTitle() {
        setTitle(glWindow);
    }
    public static void setTitle(final GLWindow win) {
        final CapabilitiesImmutable chosenCaps = win.getChosenCapabilities();
        final CapabilitiesImmutable reqCaps = win.getRequestedCapabilities();
        final CapabilitiesImmutable caps = null != chosenCaps ? chosenCaps : reqCaps;
        final String capsA = caps.isBackgroundOpaque() ? "opaque" : "transl";
        final float[] sDPI = win.getPixelsPerMM(new float[2]);
        sDPI[0] *= 25.4f;
        sDPI[1] *= 25.4f;
        win.setTitle("GLWindow["+capsA+"], win: "+win.getBounds()+", pix: "+win.getSurfaceWidth()+"x"+win.getSurfaceHeight()+", sDPI "+sDPI[0]+" x "+sDPI[1]);
    }

    public static PointerIcon[] createPointerIcons(final GLWindow glWindow) {
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
        return pointerIcons;
    }

}