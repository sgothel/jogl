/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * Neither the name of Sun Microsystems, Inc. or the names of
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
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 */

package com.jogamp.newt.impl.awt;

import com.jogamp.newt.event.awt.*;
import com.jogamp.newt.util.EDTUtil;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Container;
import java.awt.DisplayMode;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import com.jogamp.newt.Window;
import com.jogamp.newt.impl.WindowImpl;
import java.awt.Insets;
import javax.media.nativewindow.*;
import javax.media.nativewindow.awt.*;

/** An implementation of the Newt Window class built using the
    AWT. This is provided for convenience of porting to platforms
    supporting Java SE. */

public class AWTWindow extends WindowImpl {

    public AWTWindow() {
        this(null);
    }

    public static Class[] getCustomConstructorArgumentTypes() {
        return new Class[] { Container.class } ;
    }

    public AWTWindow(Container container) {
        super();
        title = "AWT NewtWindow";
        this.container = container;
        if(container instanceof Frame) {
            frame = (Frame) container;
        }
    }

    private boolean owningFrame;
    private Container container = null;
    private Frame frame = null; // same instance as container, just for impl. convenience
    private AWTCanvas canvas;
    // non fullscreen dimensions ..
    private int nfs_width, nfs_height, nfs_x, nfs_y;

    protected void requestFocusImpl(boolean reparented) {
        runOnEDT(true, new Runnable() {
                public void run() {
                    container.requestFocus();
                }
            });
    }

    protected void setTitleImpl(final String title) {
        runOnEDT(true, new Runnable() {
                public void run() {
                    if (frame != null) {
                        frame.setTitle(title);
                    }
                }
            });
    }

    protected void createNativeImpl() {

        if(0!=getParentWindowHandle()) {
            throw new RuntimeException("Window parenting not supported in AWT, use AWTWindow(Frame) cstr for wrapping instead");
        }

        final AWTWindow awtWindow = this;

        runOnEDT(true, new Runnable() {
                public void run() {
                    if(null==container) {
                        frame = new Frame();
                        container = frame;
                        owningFrame=true;
                    } else {
                        owningFrame=false;
                        width = container.getWidth();
                        height = container.getHeight();
                        x = container.getX();
                        y = container.getY();
                    }
                    if(null!=frame) {
                        frame.setTitle(getTitle());
                    }
                    container.setLayout(new BorderLayout());
                    canvas = new AWTCanvas(caps);

                    addWindowListener(new LocalWindowListener());

                    new AWTMouseAdapter(awtWindow).addTo(canvas); // fwd all AWT Mouse events to here
                    new AWTKeyAdapter(awtWindow).addTo(canvas); // fwd all AWT Key events to here

                    // canvas.addComponentListener(listener);
                    container.add(canvas, BorderLayout.CENTER);
                    container.setSize(width, height);
                    container.setLocation(x, y);
                    new AWTWindowAdapter(awtWindow).addTo(container); // fwd all AWT Window events to here

                    if(null!=frame) {
                        frame.setUndecorated(undecorated||fullscreen);
                    }
                }
            });
        setWindowHandle(1); // just a marker ..
    }

    protected void closeNativeImpl() {
        setWindowHandle(0); // just a marker ..
        if(null!=container) {
            runOnEDT(true, new Runnable() {
                    public void run() {
                        container.setVisible(false);
                        container.remove(canvas);
                        container.setEnabled(false);
                        canvas.setEnabled(false);
                    }
                });
        }
        if(owningFrame && null!=frame) {
            runOnEDT(true, new Runnable() {
                    public void run() {
                        frame.dispose();
                        owningFrame=false;
                        frame = null;
                    }
                });
        }
    }

    public boolean hasDeviceChanged() {
        boolean res = canvas.hasDeviceChanged();
        if(res) {
            config = canvas.getAWTGraphicsConfiguration();
            if (config == null) {
                throw new NativeWindowException("Error Device change null GraphicsConfiguration: "+this);
            }
            updateDeviceData();
        }
        return res;
    }

    protected void setVisibleImpl(final boolean visible) {
        runOnEDT(true, new Runnable() {
                public void run() {
                    container.setVisible(visible);
                }
            });

        config = canvas.getAWTGraphicsConfiguration();

        if (config == null) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }

        updateDeviceData();
    }

    private void updateDeviceData() {
        // propagate new info ..
        ((AWTScreen)getScreen()).setAWTGraphicsScreen((AWTGraphicsScreen)config.getScreen());
        ((AWTDisplay)getScreen().getDisplay()).setAWTGraphicsDevice((AWTGraphicsDevice)config.getScreen().getDevice());

        DisplayMode mode = ((AWTGraphicsDevice)config.getScreen().getDevice()).getGraphicsDevice().getDisplayMode();
        int w = mode.getWidth();
        int h = mode.getHeight();
        ((AWTScreen)getScreen()).setScreenSize(w, h);
    }

    protected void setSizeImpl(final int width, final int height) {
        if(null!=container) {
            /** An AWT event on setSize() would bring us in a deadlock situation, hence invokeLater() */
            runOnEDT(false, new Runnable() {
                    public void run() {
                        Insets insets = container.getInsets();
                        container.setSize(width + insets.left + insets.right,
                                          height + insets.top + insets.bottom);
                    }
                });
        }
    }

    public com.jogamp.newt.util.Insets getInsets() {
        final int insets[] = new int[] { 0, 0, 0, 0 };
        runOnEDT(true, new Runnable() {
                public void run() {
                    Insets contInsets = container.getInsets();
                    insets[0] = contInsets.top;
                    insets[1] = contInsets.left;
                    insets[2] = contInsets.bottom;
                    insets[3] = contInsets.right;
                }
            });
        return new com.jogamp.newt.util.
            Insets(insets[0],insets[1],insets[2],insets[3]);
    }

    protected void setPositionImpl(final int x, final int y) {
        if(null!=container) {
            runOnEDT(true, new Runnable() {
                    public void run() {
                        container.setLocation(x, y);
                    }
                });
        }
    }

    protected void reconfigureWindowImpl(final int x, final int y, final int width, final int height) {
        /** An AWT event on setSize() would bring us in a deadlock situation, hence invokeLater() */
        runOnEDT(false, new Runnable() {
                public void run() {
                    if(null!=frame) {
                        if(!container.isDisplayable()) {
                            frame.setUndecorated(isUndecorated(fullscreen));
                        } else {
                            if(DEBUG_IMPLEMENTATION || DEBUG_WINDOW_EVENT) {
                                System.err.println("AWTWindow can't undecorate already created frame");
                            }
                        }
                    }
                    container.setLocation(x, y);
                    container.setSize(width, height);
                }
            });
    }

    public Object getWrappedWindow() {
        return canvas;
    }

    private void runOnEDT(boolean wait, Runnable r) {
        EDTUtil edtUtil = getScreen().getDisplay().getEDTUtil();
        if ( ( null != edtUtil && edtUtil.isCurrentThreadEDT() ) || EventQueue.isDispatchThread() ) {
            r.run();
        } else {
            try {
                if(wait) {
                    EventQueue.invokeAndWait(r);
                } else {
                    EventQueue.invokeLater(r);
                }
            } catch (Exception e) {
                throw new NativeWindowException(e);
            }
        }
    }

    class LocalWindowListener extends com.jogamp.newt.event.WindowAdapter { 
        public void windowMoved(com.jogamp.newt.event.WindowEvent e) {
            if(null!=container) {
                x = container.getX();
                y = container.getY();
            }
        }
        public void windowResized(com.jogamp.newt.event.WindowEvent e) {
            if(null!=canvas) {
                width = canvas.getWidth();
                height = canvas.getHeight();
            }
        }
    }
}
