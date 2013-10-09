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

import javax.media.nativewindow.util.InsetsImmutable;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.QuitAdapter;

public class NewtAWTReparentingKeyAdapter extends KeyAdapter {
    final Frame frame;
    final NewtCanvasAWT newtCanvasAWT;
    final GLWindow glWindow;
    final QuitAdapter quitAdapter;
    
    public NewtAWTReparentingKeyAdapter(Frame frame, NewtCanvasAWT newtCanvasAWT, GLWindow glWindow, QuitAdapter quitAdapter) {
        this.frame = frame;
        this.newtCanvasAWT = newtCanvasAWT;
        this.glWindow = glWindow;
        this.quitAdapter = quitAdapter;
    }
    
    public void keyReleased(KeyEvent e) {
        if( !e.isPrintableKey() || e.isAutoRepeat() ) {
            return;
        }            
        if( e.getKeySymbol() == KeyEvent.VK_I ) {
            System.err.println(glWindow);
        } else if( e.getKeySymbol() == KeyEvent.VK_L ) {
            javax.media.nativewindow.util.Point p0 = newtCanvasAWT.getNativeWindow().getLocationOnScreen(null);
            javax.media.nativewindow.util.Point p1 = glWindow.getLocationOnScreen(null);
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
                    System.err.println("[set fullscreen  pre]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
                    glWindow.setFullscreen(!glWindow.isFullscreen());
                    System.err.println("[set fullscreen post]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
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
        } else if( e.getKeySymbol() == KeyEvent.VK_R ) {
            if( null != quitAdapter ) {
                quitAdapter.enable(false);
            }
            new Thread() {
                public void run() {
                    final Thread t = glWindow.setExclusiveContextThread(null);
                    if(glWindow.getParent()==null) {
                        System.err.println("XXX glWin to HOME");
                        glWindow.reparentWindow(newtCanvasAWT.getNativeWindow());
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
                            glWindow.reparentWindow(null, topLevelX, topLevelY, false);
                        } else {
                            System.err.println("XXX glWin to TOP.0");
                            glWindow.reparentWindow(null);
                        }
                    }
                    glWindow.requestFocus();
                    glWindow.setExclusiveContextThread(t);
                    if( null != quitAdapter ) {
                        quitAdapter.clear();
                        quitAdapter.enable(true);
                    }
            } }.start();
        }
    }
}