/*
 * Copyright (c) 2010 Sven Gothel. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name Sven Gothel or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SVEN GOTHEL HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */
package com.jogamp.newt.event.awt;

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
        if(DEBUG_IMPLEMENTATION) {
            System.out.println("AWT: focusGained: START "+ e.getComponent());
        }
    }

    public void focusLost(java.awt.event.FocusEvent e) {
        if(DEBUG_IMPLEMENTATION) {
            System.out.println("AWT: focusLost: "+ e.getComponent());
        }
    }

    public void componentResized(java.awt.event.ComponentEvent e) {
        // Need to resize the NEWT child window
        // the resized event will be send via the native window feedback.
        final java.awt.Component comp = e.getComponent();
        if(DEBUG_IMPLEMENTATION) {
            System.out.println("AWT: componentResized: "+comp);
        }
        newtWindow.runOnEDTIfAvail(false, new Runnable() {
            public void run() {
                if( 0 < comp.getWidth() * comp.getHeight() ) {
                    newtWindow.setSize(comp.getWidth(), comp.getHeight());
                    newtWindow.setVisible(comp.isVisible());
                } else {
                    newtWindow.setVisible(false);
                }
            }});
    }

    public void componentMoved(java.awt.event.ComponentEvent e) {
        // no propagation to NEWT child window
    }

    public void windowActivated(java.awt.event.WindowEvent e) {
        // no propagation to NEWT child window
    }

    public void windowDeactivated(java.awt.event.WindowEvent e) {
        // no propagation to NEWT child window
    }

    public void hierarchyChanged(java.awt.event.HierarchyEvent e) {
        if( null == newtListener ) {
            long bits = e.getChangeFlags();
            final java.awt.Component changed = e.getChanged();
            if( 0 != ( java.awt.event.HierarchyEvent.SHOWING_CHANGED & bits ) ) {
                final boolean showing = changed.isShowing();
                if(DEBUG_IMPLEMENTATION) {
                    System.out.println("AWT: hierarchyChanged SHOWING_CHANGED: showing "+showing+", "+changed);
                }
                if(!newtWindow.isDestroyed()) {
                    newtWindow.runOnEDTIfAvail(false, new Runnable() {
                        public void run() {
                            newtWindow.setVisible(showing);
                        }
                    });
                }
            } 
            if( 0 != ( java.awt.event.HierarchyEvent.DISPLAYABILITY_CHANGED & bits ) ) {
                final boolean displayability = changed.isDisplayable();
                if(DEBUG_IMPLEMENTATION) {
                    System.out.println("AWT: hierarchyChanged DISPLAYABILITY_CHANGED: displayability "+displayability+", "+changed);
                }
            }
        }
    }
}

