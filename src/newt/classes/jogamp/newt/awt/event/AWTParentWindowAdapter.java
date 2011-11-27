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

import jogamp.newt.driver.DriverUpdatePosition;

import com.jogamp.newt.event.awt.AWTAdapter;
import com.jogamp.newt.event.awt.AWTWindowAdapter;

/**
 * Specialized parent/client adapter,
 * where the NEWT child window really gets resized,
 * and the parent move window event gets discarded. */
public class AWTParentWindowAdapter 
    extends AWTWindowAdapter 
    implements java.awt.event.HierarchyListener
{
    public AWTParentWindowAdapter(com.jogamp.newt.Window downstream) {
        super(downstream);
    }

    public AWTAdapter addTo(java.awt.Component awtComponent) {
        awtComponent.addHierarchyListener(this);
        return super.addTo(awtComponent);
    }

    public AWTAdapter removeFrom(java.awt.Component awtComponent) {
        awtComponent.removeHierarchyListener(this);
        return super.removeFrom(awtComponent);
    }

    public void focusGained(java.awt.event.FocusEvent e) {
        // forward focus to NEWT child
        final com.jogamp.newt.Window newtChild = getNewtWindow();
        final boolean isOnscreen = newtChild.isNativeValid() && newtChild.getGraphicsConfiguration().getChosenCapabilities().isOnscreen();
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("AWT: focusGained: onscreen "+ isOnscreen+", "+e);
        }
        if(isOnscreen) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
        }
        newtChild.requestFocus(false);
    }

    public void focusLost(java.awt.event.FocusEvent e) {
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("AWT: focusLost: "+ e);
        }
    }

    public void componentResized(java.awt.event.ComponentEvent e) {
        // Need to resize the NEWT child window
        // the resized event will be send via the native window feedback.
        final java.awt.Component comp = e.getComponent();
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("AWT: componentResized: "+comp);
        }
        getNewtWindow().runOnEDTIfAvail(false, new Runnable() {
            public void run() {
                int cw = comp.getWidth();
                int ch = comp.getHeight();
                if( 0 < cw * ch ) {
                    if( getNewtWindow().getWidth() != cw || getNewtWindow().getHeight() != ch ) {
                        getNewtWindow().setSize(cw, ch);
                        if(comp.isVisible() != getNewtWindow().isVisible()) {
                            getNewtWindow().setVisible(comp.isVisible());
                        }
                    }
                } else if(getNewtWindow().isVisible()) {
                    getNewtWindow().setVisible(false);
                }
            }});
    }

    public void componentMoved(java.awt.event.ComponentEvent e) {
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("AWT: componentMoved: "+e);            
        }
        if(getNewtWindow().getDelegatedWindow() instanceof DriverUpdatePosition) {
            ((DriverUpdatePosition)getNewtWindow().getDelegatedWindow()).updatePosition();
        }            
    }

    public void windowActivated(java.awt.event.WindowEvent e) {
        // no propagation to NEWT child window
    }

    public void windowDeactivated(java.awt.event.WindowEvent e) {
        // no propagation to NEWT child window
    }

    public void hierarchyChanged(java.awt.event.HierarchyEvent e) {
        if( null == getNewtEventListener() ) {
            long bits = e.getChangeFlags();
            final java.awt.Component changed = e.getChanged();            
            if( 0 != ( java.awt.event.HierarchyEvent.SHOWING_CHANGED & bits ) ) {
                final boolean showing = changed.isShowing();
                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("AWT: hierarchyChanged SHOWING_CHANGED: showing "+showing+", "+changed+", source "+e.getComponent());
                }
                getNewtWindow().runOnEDTIfAvail(false, new Runnable() {
                    public void run() {
                        if(getNewtWindow().isVisible() != showing) {
                            getNewtWindow().setVisible(showing);
                        }
                    }});
            } 
            if(DEBUG_IMPLEMENTATION) {
                if( 0 != ( java.awt.event.HierarchyEvent.DISPLAYABILITY_CHANGED & bits ) ) {
                    final boolean displayability = changed.isDisplayable();
                    System.err.println("AWT: hierarchyChanged DISPLAYABILITY_CHANGED: displayability "+displayability+", "+changed);
                }
            }
        }
    }
}

