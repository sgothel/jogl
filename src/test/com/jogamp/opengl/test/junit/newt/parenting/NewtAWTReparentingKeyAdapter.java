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

import com.jogamp.nativewindow.CapabilitiesImmutable;
import com.jogamp.nativewindow.util.InsetsImmutable;
import com.jogamp.newt.Window;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.opengl.util.NEWTDemoListener;

public class NewtAWTReparentingKeyAdapter extends NEWTDemoListener {
    final Frame frame;
    final NewtCanvasAWT newtCanvasAWT;

    public NewtAWTReparentingKeyAdapter(final Frame frame, final NewtCanvasAWT newtCanvasAWT, final GLWindow glWindow) {
        super(glWindow, null);
        this.frame = frame;
        this.newtCanvasAWT = newtCanvasAWT;
    }

    public void keyPressed(final KeyEvent e) {
        if( e.isAutoRepeat() || e.isConsumed() ) {
            return;
        }
        if( 0 == e.getModifiers() ) { // all modifiers go to super class ..
          final int keySymbol = e.getKeySymbol();
          switch (keySymbol) {
            case KeyEvent.VK_L:
                e.setConsumed(true);
                final com.jogamp.nativewindow.util.Point p0 = newtCanvasAWT.getNativeWindow().getLocationOnScreen(null);
                final com.jogamp.nativewindow.util.Point p1 = glWindow.getLocationOnScreen(null);
                printlnState("[location]", "AWT "+p0+", NEWT "+p1);
                break;
            case KeyEvent.VK_R:
                e.setConsumed(true);
                quitAdapterOff();
                glWindow.invokeOnNewThread(null, false, new Runnable() {
                    public void run() {
                        final java.lang.Thread t = glWindow.setExclusiveContextThread(null);
                        if(glWindow.getParent()==null) {
                            printlnState("[reparent pre - glWin to HOME]");
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
                                printlnState("[reparent pre - glWin to TOP.1]", topLevelX+"/"+topLevelY+" - insets " + nInsets + ", " + aInsets);
                                glWindow.reparentWindow(null, topLevelX, topLevelY, 0 /* hint */);
                            } else {
                                printlnState("[reparent pre - glWin to TOP.0]");
                                glWindow.reparentWindow(null, -1, -1, 0 /* hints */);
                            }
                        }
                        printlnState("[reparent post]");
                        glWindow.requestFocus();
                        glWindow.setExclusiveContextThread(t);
                        quitAdapterOn();
                } } );
                break;
          }
        }
        super.keyPressed(e);
    }

    @Override
    public void setTitle() {
        setTitle(frame, newtCanvasAWT, glWindow);
    }
    public static void setTitle(final Frame frame, final NewtCanvasAWT glc, final Window win) {
        final CapabilitiesImmutable chosenCaps = win.getChosenCapabilities();
        final CapabilitiesImmutable reqCaps = win.getRequestedCapabilities();
        final CapabilitiesImmutable caps = null != chosenCaps ? chosenCaps : reqCaps;
        final String capsA = caps.isBackgroundOpaque() ? "opaque" : "transl";
        {
            final java.awt.Rectangle b = glc.getBounds();
            frame.setTitle("NewtCanvasAWT["+capsA+"], win: ["+b.x+"/"+b.y+" "+b.width+"x"+b.height+"], pix: "+glc.getNativeWindow().getSurfaceWidth()+"x"+glc.getNativeWindow().getSurfaceHeight());
        }
        final float[] sDPI = win.getPixelsPerMM(new float[2]);
        sDPI[0] *= 25.4f;
        sDPI[1] *= 25.4f;
        win.setTitle("GLWindow["+capsA+"], win: "+win.getBounds()+", pix: "+win.getSurfaceWidth()+"x"+win.getSurfaceHeight()+", sDPI "+sDPI[0]+" x "+sDPI[1]);
    }
}
