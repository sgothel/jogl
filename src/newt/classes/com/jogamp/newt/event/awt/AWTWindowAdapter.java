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

public class AWTWindowAdapter 
    extends AWTAdapter 
    implements java.awt.event.ComponentListener, java.awt.event.WindowListener
{
    WindowClosingListener windowClosingListener;

    public AWTWindowAdapter(com.jogamp.newt.event.WindowListener newtListener) {
        super(newtListener);
    }

    public AWTWindowAdapter(com.jogamp.newt.event.WindowListener newtListener, com.jogamp.newt.Window newtProxy) {
        super(newtListener, newtProxy);
    }

    public AWTWindowAdapter(com.jogamp.newt.Window downstream) {
        super(downstream);
    }

    public AWTAdapter addTo(java.awt.Component awtComponent) {
        java.awt.Window win = getWindow(awtComponent);
        awtComponent.addComponentListener(this);
        if( null == windowClosingListener ) {
            windowClosingListener = new WindowClosingListener();
        }
        if( null != win ) {
            win.addWindowListener(windowClosingListener);
        }
        if(awtComponent instanceof java.awt.Window) {
            ((java.awt.Window)awtComponent).addWindowListener(this);
        }
        return this;
    }

    public AWTAdapter removeFrom(java.awt.Component awtComponent) {
        awtComponent.removeComponentListener(this);
        java.awt.Window win = getWindow(awtComponent);
        if( null != win && null != windowClosingListener ) {
            win.removeWindowListener(windowClosingListener);
        }
        if(awtComponent instanceof java.awt.Window) {
            ((java.awt.Window)awtComponent).removeWindowListener(this);
        }
        return this;
    }

    static java.awt.Window getWindow(java.awt.Component comp) {
        while( null != comp && !(comp instanceof java.awt.Window) ) {
            comp = comp.getParent();
        }
        if(comp instanceof java.awt.Window) {
            return (java.awt.Window) comp;
        }
        return null;
    }

    public void componentResized(java.awt.event.ComponentEvent e) {
        com.jogamp.newt.event.WindowEvent event = AWTNewtEventFactory.createWindowEvent(e, newtWindow);
        if(null!=newtListener) {
            ((com.jogamp.newt.event.WindowListener)newtListener).windowResized(event);
        } else {
            enqueueEvent(event);
        }
    }

    public void componentMoved(java.awt.event.ComponentEvent e) {
        com.jogamp.newt.event.WindowEvent event = AWTNewtEventFactory.createWindowEvent(e, newtWindow);
        if(null!=newtListener) {
            ((com.jogamp.newt.event.WindowListener)newtListener).windowMoved(event);
        } else {
            enqueueEvent(event);
        }
    }

    public void componentShown(java.awt.event.ComponentEvent e) {
        /**
        if(null==newtListener) {
            if(!newtWindow.isDestroyed()) {
                newtWindow.runOnEDTIfAvail(false, new Runnable() {
                    public void run() {
                        newtWindow.setVisible(true);
                    }
                });
            }
        }*/
    }

    public void componentHidden(java.awt.event.ComponentEvent e) {
        /**
        if(null==newtListener) {
            if(!newtWindow.isDestroyed()) {
                newtWindow.runOnEDTIfAvail(false, new Runnable() {
                    public void run() {
                        newtWindow.setVisible(false);
                    }
                });
            }
        }*/
    }

    public void windowActivated(java.awt.event.WindowEvent e) {
        com.jogamp.newt.event.WindowEvent event = AWTNewtEventFactory.createWindowEvent(e, newtWindow);
        if(null!=newtListener) {
            ((com.jogamp.newt.event.WindowListener)newtListener).windowGainedFocus(event);
        } else {
            enqueueEvent(event);
        }
    }

    public void windowClosed(java.awt.event.WindowEvent e) { }

    public void windowClosing(java.awt.event.WindowEvent e) { }

    public void windowDeactivated(java.awt.event.WindowEvent e) {
        com.jogamp.newt.event.WindowEvent event = AWTNewtEventFactory.createWindowEvent(e, newtWindow);
        if(null!=newtListener) {
            ((com.jogamp.newt.event.WindowListener)newtListener).windowLostFocus(event);
        } else {
            enqueueEvent(event);
        }
    }

    public void windowDeiconified(java.awt.event.WindowEvent e) { }

    public void windowIconified(java.awt.event.WindowEvent e) { }

    public void windowOpened(java.awt.event.WindowEvent e) { }

    class WindowClosingListener implements java.awt.event.WindowListener {
        public void windowClosing(java.awt.event.WindowEvent e) {
            com.jogamp.newt.event.WindowEvent event = AWTNewtEventFactory.createWindowEvent(e, newtWindow);
            if(null!=newtListener) {
                ((com.jogamp.newt.event.WindowListener)newtListener).windowDestroyNotify(event);
            } else {
                enqueueEvent(true, event);
            }
        }

        public void windowActivated(java.awt.event.WindowEvent e) { }
        public void windowClosed(java.awt.event.WindowEvent e) { }
        public void windowDeactivated(java.awt.event.WindowEvent e) { }
        public void windowDeiconified(java.awt.event.WindowEvent e) { }
        public void windowIconified(java.awt.event.WindowEvent e) { }
        public void windowOpened(java.awt.event.WindowEvent e) { }
    }
}

