/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

package jogamp.newt.driver.awt;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;

import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.util.Point;

import jogamp.newt.WindowImpl;

import com.jogamp.nativewindow.awt.AWTGraphicsConfiguration;
import com.jogamp.nativewindow.awt.AWTGraphicsDevice;
import com.jogamp.nativewindow.awt.AWTGraphicsScreen;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowUpdateEvent;
import com.jogamp.newt.event.awt.AWTKeyAdapter;
import com.jogamp.newt.event.awt.AWTMouseAdapter;
import com.jogamp.newt.event.awt.AWTWindowAdapter;

/** An implementation of the Newt Window class built using the
    AWT. This is provided for convenience of porting to platforms
    supporting Java SE. */

public class WindowDriver extends WindowImpl {

    public WindowDriver() {
        this(null);
    }

    public static Class<?>[] getCustomConstructorArgumentTypes() {
        return new Class<?>[] { Container.class } ;
    }

    public WindowDriver(Container container) {
        super();
        this.container = container;
        if(container instanceof Frame) {
            frame = (Frame) container;
        }
    }

    private boolean owningFrame;
    private Container container = null;
    private Frame frame = null; // same instance as container, just for impl. convenience
    private AWTCanvas canvas;

    protected void requestFocusImpl(boolean reparented) {
        container.requestFocus();
    }

    @Override
    protected void setTitleImpl(final String title) {
        if (frame != null) {
            frame.setTitle(title);
        }
    }

    protected void createNativeImpl() {
        if(0!=getParentWindowHandle()) {
            throw new RuntimeException("Window parenting not supported in AWT, use AWTWindow(Frame) cstr for wrapping instead");
        }

        if(null==container) {
            frame = new Frame();
            container = frame;
            owningFrame=true;
        } else {
            owningFrame=false;
            defineSize(container.getWidth(), container.getHeight());
            definePosition(container.getX(), container.getY());            
        }
        if(null!=frame) {
            frame.setTitle(getTitle());
        }
        container.setLayout(new BorderLayout());
        
        canvas = new AWTCanvas(this, capsRequested, WindowDriver.this.capabilitiesChooser);

        // canvas.addComponentListener(listener);
        container.add(canvas, BorderLayout.CENTER);
        
        // via EDT ..
        new AWTMouseAdapter(this).addTo(canvas); // fwd all AWT Mouse events to here
        new AWTKeyAdapter(this).addTo(canvas); // fwd all AWT Key events to here
        
        // direct w/o EDT
        new AWTWindowAdapter(new LocalWindowListener(), this).addTo(canvas); // fwd all AWT Window events to here

        reconfigureWindowImpl(getX(), getY(), getWidth(), getHeight(), getReconfigureFlags(FLAG_CHANGE_VISIBILITY | FLAG_CHANGE_DECORATION, true));
        // throws exception if failed ..
        
        setWindowHandle(1); // just a marker ..
    }

    protected void closeNativeImpl() {
        setWindowHandle(0); // just a marker ..
        if(null!=container) {
            container.setVisible(false);
            container.remove(canvas);
            container.setEnabled(false);
            canvas.setEnabled(false);
        }
        if(owningFrame && null!=frame) {
            frame.dispose();
            owningFrame=false;
            frame = null;
        }
    }

    @Override
    public boolean hasDeviceChanged() {
        boolean res = canvas.hasDeviceChanged();
        if(res) {
            final AWTGraphicsConfiguration cfg = canvas.getAWTGraphicsConfiguration();
            if (null == cfg) {
                throw new NativeWindowException("Error Device change null GraphicsConfiguration: "+this);
            }
            setGraphicsConfiguration(cfg);
            
            // propagate new info ..
            ((ScreenDriver)getScreen()).setAWTGraphicsScreen((AWTGraphicsScreen)cfg.getScreen());
            ((DisplayDriver)getScreen().getDisplay()).setAWTGraphicsDevice((AWTGraphicsDevice)cfg.getScreen().getDevice());
    
            ((ScreenDriver)getScreen()).updateVirtualScreenOriginAndSize();
        }
        return res;
    }

    protected void updateInsetsImpl(javax.media.nativewindow.util.Insets insets) {
        Insets contInsets = container.getInsets();
        insets.setLeftWidth(contInsets.left);
        insets.setRightWidth(contInsets.right);
        insets.setTopHeight(contInsets.top);
        insets.setBottomHeight(contInsets.bottom);
    }

    protected boolean reconfigureWindowImpl(int x, int y, int width, int height, int flags) {
        if(0 != ( FLAG_CHANGE_DECORATION & flags) && null!=frame) {
            if(!container.isDisplayable()) {
                frame.setUndecorated(isUndecorated());
            } else {
                if(DEBUG_IMPLEMENTATION) {
                    System.err.println(getThreadName()+": AWTWindow can't undecorate already created frame");
                }
            }
        }
        
        final Dimension szClient = new Dimension(width, height);
        canvas.setMinimumSize(szClient);
        canvas.setPreferredSize(szClient);
        canvas.setSize(szClient);
        if(DEBUG_IMPLEMENTATION) {
            final Insets insets = container.getInsets();
            final Dimension szContainer = new Dimension(width + insets.left + insets.right,
                                                        height + insets.top + insets.bottom);
            System.err.println(getThreadName()+": AWTWindow new size: szClient "+szClient+", szCont "+szContainer+", insets "+insets);
        }
        
        if( 0 != ( FLAG_CHANGE_VISIBILITY & flags) ) {
            if(null != frame) {
                frame.pack();
            }
            container.validate();            
            container.setVisible(0 != ( FLAG_IS_VISIBLE & flags));
        }
        
        container.setLocation(x, y);
        
        if( 0 != ( FLAG_CHANGE_VISIBILITY & flags) ) {
            if( 0 != ( FLAG_IS_VISIBLE & flags ) ) {
                if( !hasDeviceChanged() ) {
                    // oops ??                   
                    final AWTGraphicsConfiguration cfg = canvas.getAWTGraphicsConfiguration();
                    if(null == cfg) {
                        throw new NativeWindowException("Error: !hasDeviceChanged && null == GraphicsConfiguration: "+this);
                    }
                    setGraphicsConfiguration(cfg);
                }
            }
            visibleChanged(false, 0 != ( FLAG_IS_VISIBLE & flags));
        } else {
            container.invalidate();
            if(null != frame) {
                frame.pack();
            }
            container.validate();            
        }
        
        return true;
    }

    protected Point getLocationOnScreenImpl(int x, int y) {
        java.awt.Point ap = canvas.getLocationOnScreen();
        ap.translate(x, y);
        return new Point((int)(ap.getX()+0.5),(int)(ap.getY()+0.5));
    }
   
    @Override
    public Object getWrappedWindow() {
        return canvas;
    }

    class LocalWindowListener implements com.jogamp.newt.event.WindowListener { 
        @Override
        public void windowMoved(com.jogamp.newt.event.WindowEvent e) {
            if(null!=container) {
                WindowDriver.this.positionChanged(false, container.getX(), container.getY());
            }
        }
        @Override
        public void windowResized(com.jogamp.newt.event.WindowEvent e) {
            if(null!=canvas) {
                WindowDriver.this.sizeChanged(false, canvas.getWidth(), canvas.getHeight(), false);
            }
        }
        @Override
        public void windowDestroyNotify(WindowEvent e) {
            WindowDriver.this.windowDestroyNotify(false);
        }
        @Override
        public void windowDestroyed(WindowEvent e) {
            if(isNativeValid()) {
                WindowDriver.this.windowDestroyNotify(true);
            }
            
        }
        @Override
        public void windowGainedFocus(WindowEvent e) {
            WindowDriver.this.focusChanged(false, true);            
        }
        @Override
        public void windowLostFocus(WindowEvent e) {
            WindowDriver.this.focusChanged(false, false);            
        }
        @Override
        public void windowRepaint(WindowUpdateEvent e) {
            WindowDriver.this.windowRepaint(false, 0, 0, getWidth(), getHeight());            
        }
    }
}
