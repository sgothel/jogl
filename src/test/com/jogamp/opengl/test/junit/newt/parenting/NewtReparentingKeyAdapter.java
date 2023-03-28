/**
 * Copyright 2011, 2019 JogAmp Community. All rights reserved.
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

import com.jogamp.graph.font.FontScale;
import com.jogamp.nativewindow.CapabilitiesImmutable;
import com.jogamp.nativewindow.NativeWindow;
import com.jogamp.nativewindow.NativeWindowHolder;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.opengl.util.NEWTDemoListener;
import com.jogamp.opengl.GLAnimatorControl;

/**
 * Extending demo functionality of {@link NEWTDemoListener}
 * <ul>
 *   <li>L: Print parent and (child) {@link GLWindow} location</li>
 *   <li>R: Toggel parenting (top-level/child)</li>
 * </ul>
 */
public class NewtReparentingKeyAdapter extends NEWTDemoListener {
    final NativeWindowHolder winHolder;

    public NewtReparentingKeyAdapter(final NativeWindowHolder winHolder, final GLWindow glWindow) {
        super(glWindow);
        this.winHolder = winHolder;
    }

    @Override
    public void keyPressed(final KeyEvent e) {
        if( e.isAutoRepeat() || e.isConsumed() ) {
            return;
        }
        if( 0 == e.getModifiers() ) { // all modifiers go to super class ..
          final int keySymbol = e.getKeySymbol();
          switch (keySymbol) {
            case KeyEvent.VK_L:
                e.setConsumed(true);
                final com.jogamp.nativewindow.util.Point p0 = winHolder.getNativeWindow().getLocationOnScreen(null);
                final com.jogamp.nativewindow.util.Point p1 = glWindow.getLocationOnScreen(null);
                printlnState("[location]", "Parent "+p0+", NEWT "+p1);
                break;
            case KeyEvent.VK_R:
                e.setConsumed(true);
                quitAdapterOff();
                glWindow.invokeOnNewThread(null, false, new Runnable() {
                    @Override
                    public void run() {
                        final java.lang.Thread t = glWindow.setExclusiveContextThread(null);
                        if(glWindow.getParent()==null) {
                            printlnState("[reparent pre - glWin to HOME: child pos "+winHolder.getNativeWindow().getX()+"/"+winHolder.getNativeWindow().getY()+"]");
                            glWindow.reparentWindow(winHolder.getNativeWindow(), -1, -1, 0 /* hints */);
                        } else {
                            final com.jogamp.nativewindow.util.Point p0 = winHolder.getNativeWindow().getLocationOnScreen(null);
                            final com.jogamp.nativewindow.util.Point p1 = glWindow.getLocationOnScreen(null);
                            printlnState("[reparent pre - glWin to TOP.1] frame ", p0+", glWindow "+p1);
                            glWindow.reparentWindow(null, p1.getX(), p1.getY(), 0 /* hint */);
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
        setTitle(winHolder.getNativeWindow(), glWindow);
    }
    String getNativeWinTitle(final NativeWindow nw) {
        return "["+nw.getX()+"/"+nw.getY()+" "+nw.getWidth()+"x"+nw.getHeight()+"], pix: "+nw.getSurfaceWidth()+"x"+nw.getSurfaceHeight();
    }
    public void setTitle(final NativeWindow nw, final Window win) {
        final CapabilitiesImmutable chosenCaps = win.getChosenCapabilities();
        final CapabilitiesImmutable reqCaps = win.getRequestedCapabilities();
        final CapabilitiesImmutable caps = null != chosenCaps ? chosenCaps : reqCaps;
        final String capsA = caps.isBackgroundOpaque() ? "opaque" : "transl";
        final float[] sDPI = FontScale.ppmmToPPI( win.getPixelsPerMM(new float[2]) );
        win.setTitle("GLWindow["+capsA+"], win: "+win.getBounds()+", pix: "+win.getSurfaceWidth()+"x"+win.getSurfaceHeight()+", sDPI "+sDPI[0]+" x "+sDPI[1]);
    }
}
