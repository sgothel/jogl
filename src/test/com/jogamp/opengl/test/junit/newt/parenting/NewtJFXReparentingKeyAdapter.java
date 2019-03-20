/**
 * Copyright 2019 JogAmp Community. All rights reserved.
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

import com.jogamp.nativewindow.CapabilitiesImmutable;
import com.jogamp.nativewindow.NativeWindow;
import com.jogamp.nativewindow.NativeWindowHolder;
import com.jogamp.nativewindow.util.Insets;
import com.jogamp.nativewindow.util.InsetsImmutable;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.opengl.util.NEWTDemoListener;

import javafx.geometry.Bounds;

/**
 * JavaFX specializing demo functionality of {@link NewtReparentingKeyAdapter}, includes {@link NEWTDemoListener}.
 */
public class NewtJFXReparentingKeyAdapter extends NewtReparentingKeyAdapter {
    final javafx.stage.Stage frame;

    public NewtJFXReparentingKeyAdapter(final javafx.stage.Stage frame, final NativeWindowHolder winHolder, final GLWindow glWindow) {
        super(winHolder, glWindow);
        this.frame = frame;
    }

    @Override
    public void keyPressed(final KeyEvent e) {
        if( e.isAutoRepeat() || e.isConsumed() ) {
            return;
        }
        if( 0 == e.getModifiers() ) { // all modifiers go to super class ..
          final int keySymbol = e.getKeySymbol();
          switch (keySymbol) {
            case KeyEvent.VK_R:
                e.setConsumed(true);
                quitAdapterOff();
                glWindow.invokeOnNewThread(null, false, new Runnable() {
                    public void run() {
                        final java.lang.Thread t = glWindow.setExclusiveContextThread(null);
                        if(glWindow.getParent()==null) {
                            printlnState("[reparent pre - glWin to HOME: child pos "+winHolder.getNativeWindow().getX()+"/"+winHolder.getNativeWindow().getY()+"]");
                            glWindow.reparentWindow(winHolder.getNativeWindow(), winHolder.getNativeWindow().getX(), winHolder.getNativeWindow().getY(), 0 /* hints */);
                            glWindow.setPosition(winHolder.getNativeWindow().getX(), winHolder.getNativeWindow().getY());
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
        setTitle(frame, winHolder.getNativeWindow(), glWindow);
    }
    public void setTitle(final javafx.stage.Stage frame, final NativeWindow nw, final Window win) {
        final CapabilitiesImmutable chosenCaps = win.getChosenCapabilities();
        final CapabilitiesImmutable reqCaps = win.getRequestedCapabilities();
        final CapabilitiesImmutable caps = null != chosenCaps ? chosenCaps : reqCaps;
        final String capsA = caps.isBackgroundOpaque() ? "opaque" : "transl";
        {
            frame.setTitle("Frame["+capsA+"], win: "+getNativeWinTitle(nw));
        }
        super.setTitle(nw, win);
    }
}
