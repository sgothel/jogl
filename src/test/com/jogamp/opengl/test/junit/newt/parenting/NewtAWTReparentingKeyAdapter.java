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
package com.jogamp.opengl.test.junit.newt.parenting;

import java.awt.Frame;
import java.net.URLConnection;

import com.jogamp.nativewindow.util.InsetsImmutable;

import com.jogamp.common.util.IOUtil;
import com.jogamp.newt.Display;
import com.jogamp.newt.Display.PointerIcon;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.util.PNGPixelRect;

public class NewtAWTReparentingKeyAdapter extends KeyAdapter {
    final Frame frame;
    final NewtCanvasAWT newtCanvasAWT;
    final GLWindow glWindow;
    final QuitAdapter quitAdapter;
    PointerIcon[] pointerIcons = null;
    int pointerIconIdx = 0;

    public NewtAWTReparentingKeyAdapter(final Frame frame, final NewtCanvasAWT newtCanvasAWT, final GLWindow glWindow, final QuitAdapter quitAdapter) {
        this.frame = frame;
        this.newtCanvasAWT = newtCanvasAWT;
        this.glWindow = glWindow;
        this.quitAdapter = quitAdapter;
    }

    public void keyReleased(final KeyEvent e) {
        if( !e.isPrintableKey() || e.isAutoRepeat() ) {
            return;
        }
        if( e.getKeySymbol() == KeyEvent.VK_L ) {
            final com.jogamp.nativewindow.util.Point p0 = newtCanvasAWT.getNativeWindow().getLocationOnScreen(null);
            final com.jogamp.nativewindow.util.Point p1 = glWindow.getLocationOnScreen(null);
            System.err.println("NewtCanvasAWT position: "+p0+", "+p1);
        } else if( e.getKeySymbol() == KeyEvent.VK_D ) {
            glWindow.setUndecorated(!glWindow.isUndecorated());
        } else if( e.getKeySymbol() == KeyEvent.VK_S ) {
            if(glWindow.getParent()==null) {
                System.err.println("XXX glWin to 100/100");
                glWindow.setPosition(100, 100);
            } else {
                System.err.println("XXX glWin to 0/0");
                glWindow.setPosition(0, 0);
            }
        } else if( e.getKeySymbol() == KeyEvent.VK_F ) {
            if( null != quitAdapter ) {
                quitAdapter.enable(false);
            }
            new Thread() {
                public void run() {
                    final Thread t = glWindow.setExclusiveContextThread(null);
                    System.err.println("[set fullscreen  pre]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
                    glWindow.setFullscreen(!glWindow.isFullscreen());
                    System.err.println("[set fullscreen post]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
                    glWindow.setExclusiveContextThread(t);
                    if( null != quitAdapter ) {
                        quitAdapter.clear();
                        quitAdapter.enable(true);
                    }
            } }.start();
        } else if( e.getKeySymbol() == KeyEvent.VK_P ) {
            new Thread() {
                public void run() {
                    if(glWindow.getAnimator().isPaused()) {
                        glWindow.getAnimator().resume();
                    } else {
                        glWindow.getAnimator().pause();
                    }
                }
            }.run();
        } else if( e.getKeySymbol() == KeyEvent.VK_A ) {
            new Thread() {
                public void run() {
                    glWindow.setAlwaysOnTop(!glWindow.isAlwaysOnTop());
                }
            }.run();
        } else if( e.getKeySymbol() == KeyEvent.VK_R ) {
            if( null != quitAdapter ) {
                quitAdapter.enable(false);
            }
            new Thread() {
                public void run() {
                    final Thread t = glWindow.setExclusiveContextThread(null);
                    if(glWindow.getParent()==null) {
                        System.err.println("XXX glWin to HOME");
                        glWindow.reparentWindow(newtCanvasAWT.getNativeWindow(), -1, -1, 0 /* hints */);
                    } else {
                        if( null != frame ) {
                            final InsetsImmutable nInsets = glWindow.getInsets();
                            final java.awt.Insets aInsets = frame.getInsets();
                            int dx, dy;
                            if( nInsets.getTotalHeight()==0 ) {
                                dx = aInsets.left;
                                dy = aInsets.top;
                            } else {
                                dx = nInsets.getLeftWidth();
                                dy = nInsets.getTopHeight();
                            }
                            final int topLevelX = frame.getX()+frame.getWidth()+dx;
                            final int topLevelY = frame.getY()+dy;
                            System.err.println("XXX glWin to TOP.1 "+topLevelX+"/"+topLevelY+" - insets " + nInsets + ", " + aInsets);
                            glWindow.reparentWindow(null, topLevelX, topLevelY, 0 /* hint */);
                        } else {
                            System.err.println("XXX glWin to TOP.0");
                            glWindow.reparentWindow(null, -1, -1, 0 /* hints */);
                        }
                    }
                    glWindow.requestFocus();
                    glWindow.setExclusiveContextThread(t);
                    if( null != quitAdapter ) {
                        quitAdapter.clear();
                        quitAdapter.enable(true);
                    }
            } }.start();
        } else if(e.getKeySymbol() == KeyEvent.VK_C ) {
            if( null == pointerIcons ) {
                {
                    pointerIcons = new PointerIcon[3];
                    final Display disp = glWindow.getScreen().getDisplay();
                    {
                        PointerIcon _pointerIcon = null;
                        final IOUtil.ClassResources res = new IOUtil.ClassResources(glWindow.getClass(), new String[] { "newt/data/cross-grey-alpha-16x16.png" } );
                        try {
                            _pointerIcon = disp.createPointerIcon(res, 8, 8);
                            System.err.println("Create PointerIcon #01: "+_pointerIcon);
                        } catch (final Exception ex) {
                            ex.printStackTrace();
                        }
                        pointerIcons[0] = _pointerIcon;
                    }
                    {
                        PointerIcon _pointerIcon = null;
                        final IOUtil.ClassResources res = new IOUtil.ClassResources(glWindow.getClass(), new String[] { "newt/data/pointer-grey-alpha-16x24.png" } );
                        try {
                            _pointerIcon = disp.createPointerIcon(res, 0, 0);
                            System.err.println("Create PointerIcon #02: "+_pointerIcon);
                        } catch (final Exception ex) {
                            ex.printStackTrace();
                        }
                        pointerIcons[1] = _pointerIcon;
                    }
                    {
                        PointerIcon _pointerIcon = null;
                        final IOUtil.ClassResources res = new IOUtil.ClassResources(glWindow.getClass(), new String[] { "jogamp-pointer-64x64.png" } );
                        try {
                            final URLConnection urlConn = res.resolve(0);
                            final PNGPixelRect image = PNGPixelRect.read(urlConn.getInputStream(), null, false /* directBuffer */, 0 /* destMinStrideInBytes */, false /* destIsGLOriented */);
                            System.err.println("Create PointerIcon #03: "+image);
                            _pointerIcon = disp.createPointerIcon(image, 32, 0);
                            System.err.println("Create PointerIcon #03: "+_pointerIcon);
                        } catch (final Exception ex) {
                            ex.printStackTrace();
                        }
                        pointerIcons[2] = _pointerIcon;
                    }
                }
            }
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
        } else if( e.getKeySymbol() == KeyEvent.VK_I ) {
            new Thread() {
                public void run() {
                    final Thread t = glWindow.setExclusiveContextThread(null);
                    System.err.println("[set mouse visible pre]: "+glWindow.isPointerVisible());
                    glWindow.setPointerVisible(!glWindow.isPointerVisible());
                    System.err.println("[set mouse visible post]: "+glWindow.isPointerVisible());
                    glWindow.setExclusiveContextThread(t);
            } }.start();
        } else if(e.getKeySymbol() == KeyEvent.VK_J ) {
            new Thread() {
                public void run() {
                    final Thread t = glWindow.setExclusiveContextThread(null);
                    System.err.println("[set mouse confined pre]: "+glWindow.isPointerConfined());
                    glWindow.confinePointer(!glWindow.isPointerConfined());
                    System.err.println("[set mouse confined post]: "+glWindow.isPointerConfined());
                    glWindow.setExclusiveContextThread(t);
            } }.start();
        } else if(e.getKeySymbol() == KeyEvent.VK_W ) {
            new Thread() {
               public void run() {
                   System.err.println("[set mouse pos pre]");
                   glWindow.warpPointer(glWindow.getSurfaceWidth()/2, glWindow.getSurfaceHeight()/2);
                   System.err.println("[set mouse pos post]");
               } }.start();
        }
    }
}
