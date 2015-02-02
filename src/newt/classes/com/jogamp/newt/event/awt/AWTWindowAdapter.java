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

import com.jogamp.nativewindow.NativeSurfaceHolder;

import jogamp.newt.awt.event.AWTNewtEventFactory;

public class AWTWindowAdapter
    extends AWTAdapter
    implements java.awt.event.ComponentListener, java.awt.event.WindowListener, java.awt.event.FocusListener
{
    WindowClosingListener windowClosingListener;

    public AWTWindowAdapter(final com.jogamp.newt.event.WindowListener newtListener, final NativeSurfaceHolder nsProxy) {
        super(newtListener, nsProxy);
    }

    public AWTWindowAdapter(final com.jogamp.newt.event.WindowListener newtListener, final com.jogamp.newt.Window newtProxy) {
        super(newtListener, newtProxy);
    }

    public AWTWindowAdapter(final com.jogamp.newt.Window downstream) {
        super(downstream);
    }

    public AWTWindowAdapter() {
        super();
    }

    @Override
    public synchronized AWTAdapter addTo(final java.awt.Component awtComponent) {
        final java.awt.Window win = getWindow(awtComponent);
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

    public synchronized AWTAdapter removeWindowClosingFrom(final java.awt.Component awtComponent) {
        final java.awt.Window win = getWindow(awtComponent);
        if( null != win && null != windowClosingListener ) {
            win.removeWindowListener(windowClosingListener);
        }
        return this;
    }

    @Override
    public synchronized AWTAdapter removeFrom(final java.awt.Component awtComponent) {
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
        return (java.awt.Window) comp; // either null or a 'java.awt.Window'
    }

    @Override
    public synchronized void focusGained(final java.awt.event.FocusEvent e) {
        if( !isSetup ) { return; }
        final com.jogamp.newt.event.WindowEvent event = AWTNewtEventFactory.createWindowEvent(e, nsHolder);
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("AWT: focusGained: "+e+" -> "+event);
        }
        if( EventProcRes.DISPATCH == processEvent(false, event) ) {
            ((com.jogamp.newt.event.WindowListener)newtListener).windowGainedFocus(event);
        }
    }

    @Override
    public synchronized void focusLost(final java.awt.event.FocusEvent e) {
        if( !isSetup ) { return; }
        final com.jogamp.newt.event.WindowEvent event = AWTNewtEventFactory.createWindowEvent(e, nsHolder);
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("AWT: focusLost: "+e+" -> "+event);
        }
        if( EventProcRes.DISPATCH == processEvent(false, event) ) {
            ((com.jogamp.newt.event.WindowListener)newtListener).windowLostFocus(event);
        }
    }

    @Override
    public synchronized void componentResized(final java.awt.event.ComponentEvent e) {
        if( !isSetup ) { return; }
        final com.jogamp.newt.event.WindowEvent event = AWTNewtEventFactory.createWindowEvent(e, nsHolder);
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
        if( EventProcRes.DISPATCH == processEvent(false, event) ) {
            ((com.jogamp.newt.event.WindowListener)newtListener).windowResized(event);
        }
    }

    @Override
    public synchronized void componentMoved(final java.awt.event.ComponentEvent e) {
        if( !isSetup ) { return; }
        final com.jogamp.newt.event.WindowEvent event = AWTNewtEventFactory.createWindowEvent(e, nsHolder);
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("AWT: componentMoved: "+e+" -> "+event);
        }
        if( EventProcRes.DISPATCH == processEvent(false, event) ) {
            ((com.jogamp.newt.event.WindowListener)newtListener).windowMoved(event);
        }
    }

    @Override
    public synchronized void componentShown(final java.awt.event.ComponentEvent e) {
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
    public synchronized void componentHidden(final java.awt.event.ComponentEvent e) {
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
    public synchronized void windowActivated(final java.awt.event.WindowEvent e) {
        if( !isSetup ) { return; }
        final com.jogamp.newt.event.WindowEvent event = AWTNewtEventFactory.createWindowEvent(e, nsHolder);
        if( EventProcRes.DISPATCH == processEvent(false, event) ) {
            ((com.jogamp.newt.event.WindowListener)newtListener).windowGainedFocus(event);
        }
    }

    @Override
    public synchronized void windowClosed(final java.awt.event.WindowEvent e) { }

    @Override
    public synchronized void windowClosing(final java.awt.event.WindowEvent e) { }

    @Override
    public synchronized void windowDeactivated(final java.awt.event.WindowEvent e) {
        if( !isSetup ) { return; }
        final com.jogamp.newt.event.WindowEvent event = AWTNewtEventFactory.createWindowEvent(e, nsHolder);
        if( EventProcRes.DISPATCH == processEvent(false, event) ) {
            ((com.jogamp.newt.event.WindowListener)newtListener).windowLostFocus(event);
        }
    }

    @Override
    public synchronized void windowDeiconified(final java.awt.event.WindowEvent e) { }

    @Override
    public synchronized void windowIconified(final java.awt.event.WindowEvent e) { }

    @Override
    public synchronized void windowOpened(final java.awt.event.WindowEvent e) { }

    class WindowClosingListener implements java.awt.event.WindowListener {
        @Override
        public void windowClosing(final java.awt.event.WindowEvent e) {
            synchronized( AWTWindowAdapter.this ) {
                if( !isSetup ) { return; }
                final com.jogamp.newt.event.WindowEvent event = AWTNewtEventFactory.createWindowEvent(e, nsHolder);
                if( EventProcRes.DISPATCH == processEvent(true, event) ) {
                    ((com.jogamp.newt.event.WindowListener)newtListener).windowDestroyNotify(event);
                }
            }
        }
        @Override
        public void windowClosed(final java.awt.event.WindowEvent e) {
            synchronized( AWTWindowAdapter.this ) {
                if( !isSetup ) { return; }
                final com.jogamp.newt.event.WindowEvent event = AWTNewtEventFactory.createWindowEvent(e, nsHolder);
                if( EventProcRes.DISPATCH == processEvent(true, event) ) {
                    ((com.jogamp.newt.event.WindowListener)newtListener).windowDestroyed(event);
                }
            }
        }

        @Override
        public void windowActivated(final java.awt.event.WindowEvent e) { }
        @Override
        public void windowDeactivated(final java.awt.event.WindowEvent e) { }
        @Override
        public void windowDeiconified(final java.awt.event.WindowEvent e) { }
        @Override
        public void windowIconified(final java.awt.event.WindowEvent e) { }
        @Override
        public void windowOpened(final java.awt.event.WindowEvent e) { }
    }
}

