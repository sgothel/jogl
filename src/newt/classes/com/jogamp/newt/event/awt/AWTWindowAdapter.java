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

package com.jogamp.newt.event.awt;

import java.awt.Dimension;

import jogamp.newt.awt.event.AWTNewtEventFactory;

public class AWTWindowAdapter
    extends AWTAdapter
    implements java.awt.event.ComponentListener, java.awt.event.WindowListener, java.awt.event.FocusListener
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
    public AWTWindowAdapter() {
        super();
    }

    @Override
    public synchronized AWTAdapter addTo(java.awt.Component awtComponent) {
        java.awt.Window win = getWindow(awtComponent);
        awtComponent.addComponentListener(this);
        awtComponent.addFocusListener(this);
        if( null != win && null == windowClosingListener ) {
            windowClosingListener = new WindowClosingListener();
            win.addWindowListener(windowClosingListener);
        }
        if(awtComponent instanceof java.awt.Window) {
            ((java.awt.Window)awtComponent).addWindowListener(this);
        }
        return this;
    }

    public synchronized AWTAdapter removeWindowClosingFrom(java.awt.Component awtComponent) {
        java.awt.Window win = getWindow(awtComponent);
        if( null != win && null != windowClosingListener ) {
            win.removeWindowListener(windowClosingListener);
        }
        return this;
    }

    @Override
    public synchronized AWTAdapter removeFrom(java.awt.Component awtComponent) {
        awtComponent.removeFocusListener(this);
        awtComponent.removeComponentListener(this);
        removeWindowClosingFrom(awtComponent);
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

    @Override
    public synchronized void focusGained(java.awt.event.FocusEvent e) {
        if( !isSetup ) { return; }
        com.jogamp.newt.event.WindowEvent event = AWTNewtEventFactory.createWindowEvent(e, newtWindow);
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("AWT: focusGained: "+e+" -> "+event);
        }
        if(null!=newtListener) {
            ((com.jogamp.newt.event.WindowListener)newtListener).windowGainedFocus(event);
        } else {
            enqueueEvent(false, event);
        }
    }

    @Override
    public synchronized void focusLost(java.awt.event.FocusEvent e) {
        if( !isSetup ) { return; }
        com.jogamp.newt.event.WindowEvent event = AWTNewtEventFactory.createWindowEvent(e, newtWindow);
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("AWT: focusLost: "+e+" -> "+event);
        }
        if(null!=newtListener) {
            ((com.jogamp.newt.event.WindowListener)newtListener).windowLostFocus(event);
        } else {
            enqueueEvent(false, event);
        }
    }

    @Override
    public synchronized void componentResized(java.awt.event.ComponentEvent e) {
        if( !isSetup ) { return; }
        com.jogamp.newt.event.WindowEvent event = AWTNewtEventFactory.createWindowEvent(e, newtWindow);
        if(DEBUG_IMPLEMENTATION) {
            final java.awt.Component c = e.getComponent();
            final java.awt.Dimension sz = c.getSize();
            final java.awt.Insets insets;
            final java.awt.Dimension sz2;
            if(c instanceof java.awt.Container) {
                insets = ((java.awt.Container)c).getInsets();
                sz2 = new Dimension(sz.width - insets.left - insets.right,
                                    sz.height - insets.top - insets.bottom);
            } else {
                insets = null;
                sz2 = sz;
            }
            System.err.println("AWT: componentResized: "+sz+" ( "+insets+", "+sz2+" ), "+e+" -> "+event);
        }
        if(null!=newtListener) {
            ((com.jogamp.newt.event.WindowListener)newtListener).windowResized(event);
        } else {
            enqueueEvent(false, event);
        }
    }

    @Override
    public synchronized void componentMoved(java.awt.event.ComponentEvent e) {
        if( !isSetup ) { return; }
        com.jogamp.newt.event.WindowEvent event = AWTNewtEventFactory.createWindowEvent(e, newtWindow);
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("AWT: componentMoved: "+e+" -> "+event);
        }
        if(null!=newtListener) {
            ((com.jogamp.newt.event.WindowListener)newtListener).windowMoved(event);
        } else {
            enqueueEvent(false, event);
        }
    }

    @Override
    public synchronized void componentShown(java.awt.event.ComponentEvent e) {
        if( !isSetup ) { return; }
        final java.awt.Component comp = e.getComponent();
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("AWT: componentShown: "+comp);
        }
        /**
        if(null==newtListener) {
            if(newtWindow.isValid()) {
                newtWindow.runOnEDTIfAvail(false, new Runnable() {
                    public void run() {
                        newtWindow.setVisible(true);
                    }
                });
            }
        }*/
    }

    @Override
    public synchronized void componentHidden(java.awt.event.ComponentEvent e) {
        if( !isSetup ) { return; }
        final java.awt.Component comp = e.getComponent();
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("AWT: componentHidden: "+comp);
        }
        /**
        if(null==newtListener) {
            if(newtWindow.isValid()) {
                newtWindow.runOnEDTIfAvail(false, new Runnable() {
                    public void run() {
                        newtWindow.setVisible(false);
                    }
                });
            }
        }*/
    }

    @Override
    public synchronized void windowActivated(java.awt.event.WindowEvent e) {
        if( !isSetup ) { return; }
        com.jogamp.newt.event.WindowEvent event = AWTNewtEventFactory.createWindowEvent(e, newtWindow);
        if(null!=newtListener) {
            ((com.jogamp.newt.event.WindowListener)newtListener).windowGainedFocus(event);
        } else {
            enqueueEvent(false, event);
        }
    }

    @Override
    public synchronized void windowClosed(java.awt.event.WindowEvent e) { }

    @Override
    public synchronized void windowClosing(java.awt.event.WindowEvent e) { }

    @Override
    public synchronized void windowDeactivated(java.awt.event.WindowEvent e) {
        if( !isSetup ) { return; }
        com.jogamp.newt.event.WindowEvent event = AWTNewtEventFactory.createWindowEvent(e, newtWindow);
        if(null!=newtListener) {
            ((com.jogamp.newt.event.WindowListener)newtListener).windowLostFocus(event);
        } else {
            enqueueEvent(false, event);
        }
    }

    @Override
    public synchronized void windowDeiconified(java.awt.event.WindowEvent e) { }

    @Override
    public synchronized void windowIconified(java.awt.event.WindowEvent e) { }

    @Override
    public synchronized void windowOpened(java.awt.event.WindowEvent e) { }

    class WindowClosingListener implements java.awt.event.WindowListener {
        @Override
        public void windowClosing(java.awt.event.WindowEvent e) {
            synchronized( AWTWindowAdapter.this ) {
                if( !isSetup ) { return; }
                com.jogamp.newt.event.WindowEvent event = AWTNewtEventFactory.createWindowEvent(e, newtWindow);
                if(null!=newtListener) {
                    ((com.jogamp.newt.event.WindowListener)newtListener).windowDestroyNotify(event);
                } else {
                    enqueueEvent(true, event);
                }
            }
        }
        @Override
        public void windowClosed(java.awt.event.WindowEvent e) {
            synchronized( AWTWindowAdapter.this ) {
                if( !isSetup ) { return; }
                com.jogamp.newt.event.WindowEvent event = AWTNewtEventFactory.createWindowEvent(e, newtWindow);
                if(null!=newtListener) {
                    ((com.jogamp.newt.event.WindowListener)newtListener).windowDestroyed(event);
                } else {
                    enqueueEvent(true, event);
                }
            }
        }

        @Override
        public void windowActivated(java.awt.event.WindowEvent e) { }
        @Override
        public void windowDeactivated(java.awt.event.WindowEvent e) { }
        @Override
        public void windowDeiconified(java.awt.event.WindowEvent e) { }
        @Override
        public void windowIconified(java.awt.event.WindowEvent e) { }
        @Override
        public void windowOpened(java.awt.event.WindowEvent e) { }
    }
}

