/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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

package jogamp.newt.awt.event;

import java.awt.KeyboardFocusManager;

import com.jogamp.nativewindow.NativeWindow;

import jogamp.newt.driver.DriverUpdatePosition;

import com.jogamp.newt.Window;
import com.jogamp.newt.event.awt.AWTAdapter;
import com.jogamp.newt.event.awt.AWTWindowAdapter;

/**
 * Specialized parent/client adapter,
 * where the NEWT child window really gets resized,
 * and the parent move window event gets discarded. */
public class AWTParentWindowAdapter extends AWTWindowAdapter implements java.awt.event.HierarchyListener
{
    NativeWindow downstreamParent;

    public AWTParentWindowAdapter(final NativeWindow downstreamParent, final com.jogamp.newt.Window downstream) {
        super(downstream);
        this.downstreamParent = downstreamParent;
    }
    public AWTParentWindowAdapter() {
        super();
    }
    public AWTParentWindowAdapter setDownstream(final NativeWindow downstreamParent, final com.jogamp.newt.Window downstream) {
        setDownstream(downstream);
        this.downstreamParent = downstreamParent;
        return this;
    }

    @Override
    public synchronized AWTAdapter clear() {
        super.clear();
        this.downstreamParent = null;
        return this;
    }

    @Override
    public synchronized AWTAdapter addTo(final java.awt.Component awtComponent) {
        awtComponent.addHierarchyListener(this);
        return super.addTo(awtComponent);
    }

    @Override
    public synchronized AWTAdapter removeFrom(final java.awt.Component awtComponent) {
        awtComponent.removeHierarchyListener(this);
        return super.removeFrom(awtComponent);
    }

    @Override
    public synchronized void focusGained(final java.awt.event.FocusEvent e) {
        if( !isSetup ) { return; }
        // forward focus to NEWT child
        final com.jogamp.newt.Window newtChild = getNewtWindow();
        if( null != newtChild ) {
            final boolean isOnscreen = newtChild.isNativeValid() && newtChild.getGraphicsConfiguration().getChosenCapabilities().isOnscreen();
            final boolean isParent = downstreamParent == newtChild.getParent();
            final boolean isFullscreen = newtChild.isFullscreen();
            if(DEBUG_IMPLEMENTATION) {
                System.err.println("AWT: focusGained: onscreen "+ isOnscreen+", "+e+", isParent: "+isParent+", isFS "+isFullscreen);
            }
            if(isParent) {
                if(isOnscreen && !isFullscreen) {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                }
                newtChild.requestFocus(false);
            }
        }
    }

    @Override
    public synchronized void focusLost(final java.awt.event.FocusEvent e) {
        if( !isSetup ) { return; }
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("AWT: focusLost: "+ e);
        }
    }

    @Override
    public synchronized void componentResized(final java.awt.event.ComponentEvent e) {
        if( !isSetup ) { return; }
        // Need to resize the NEWT child window
        // the resized event will be send via the native window feedback.
        final java.awt.Component comp = e.getComponent();
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("AWT: componentResized: "+comp);
        }
        final Window newtChild = getNewtWindow();
        if( null != newtChild ) {
            newtChild.runOnEDTIfAvail(false, new Runnable() {
                @Override
                public void run() {
                    final int cw = comp.getWidth();
                    final int ch = comp.getHeight();
                    if( 0 < cw && 0 < ch ) {
                        if( newtChild.getWidth() != cw || newtChild.getHeight() != ch ) {
                            newtChild.setSize(cw, ch);
                            final boolean v = comp.isShowing(); // compute showing-state throughout hierarchy
                            if(v != newtChild.isVisible()) {
                                newtChild.setVisible(v);
                            }
                        }
                    } else if(newtChild.isVisible()) {
                        newtChild.setVisible(false);
                    }
                }});
        }
    }

    @Override
    public synchronized void componentMoved(final java.awt.event.ComponentEvent e) {
        if( !isSetup ) { return; }
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("AWT: componentMoved: "+e);
        }
        final Window newtChild = getNewtWindow();
        if( null != newtChild && ( newtChild.getDelegatedWindow() instanceof DriverUpdatePosition ) ) {
            ((DriverUpdatePosition)newtChild.getDelegatedWindow()).updatePosition(0, 0);
        }
    }

    @Override
    public synchronized void windowActivated(final java.awt.event.WindowEvent e) {
        // no propagation to NEWT child window
    }

    @Override
    public synchronized void windowDeactivated(final java.awt.event.WindowEvent e) {
        // no propagation to NEWT child window
    }

    @Override
    public synchronized void hierarchyChanged(final java.awt.event.HierarchyEvent e) {
        if( !isSetup ) { return; }
        final Window newtChild = getNewtWindow();
        if( null != newtChild && null == getNewtEventListener() ) {
            final long bits = e.getChangeFlags();
            final java.awt.Component comp = e.getComponent();
            if( 0 != ( java.awt.event.HierarchyEvent.SHOWING_CHANGED & bits ) ) {
                final boolean showing = comp.isShowing(); // compute showing-state throughout hierarchy
                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("AWT: hierarchyChanged SHOWING_CHANGED: showing "+showing+", comp "+comp+", changed "+e.getChanged());
                }
                newtChild.runOnEDTIfAvail(false, new Runnable() {
                    @Override
                    public void run() {
                        if(newtChild.isVisible() != showing) {
                            newtChild.setVisible(showing);
                        }
                    }});
            }
            if(DEBUG_IMPLEMENTATION) {
                if( 0 != ( java.awt.event.HierarchyEvent.DISPLAYABILITY_CHANGED & bits ) ) {
                    System.err.println("AWT: hierarchyChanged DISPLAYABILITY_CHANGED: "+e.getChanged());
                }
            }
        }
    }
}

