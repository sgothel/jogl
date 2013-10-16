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

import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.util.Point;

import jogamp.nativewindow.awt.AWTMisc;
import jogamp.newt.WindowImpl;

import com.jogamp.common.os.Platform;
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
        this.awtContainer = container;
        if(container instanceof Frame) {
            awtFrame = (Frame) container;
        }
    }

    private boolean owningFrame;
    private Container awtContainer = null;
    /** same instance as container, just for impl. convenience */
    private Frame awtFrame = null;
    private AWTCanvas awtCanvas;

    protected void requestFocusImpl(boolean reparented) {
        awtContainer.requestFocus();
    }

    @Override
    protected void setTitleImpl(final String title) {
        if (awtFrame != null) {
            awtFrame.setTitle(title);
        }
    }

    protected void createNativeImpl() {
        if(0!=getParentWindowHandle()) {
            throw new RuntimeException("Window parenting not supported in AWT, use AWTWindow(Frame) cstr for wrapping instead");
        }

        if(null==awtContainer) {
            awtFrame = new Frame();
            awtContainer = awtFrame;
            owningFrame=true;
        } else {
            owningFrame=false;
            defineSize(awtContainer.getWidth(), awtContainer.getHeight());
            definePosition(awtContainer.getX(), awtContainer.getY());            
        }
        if(null!=awtFrame) {
            awtFrame.setTitle(getTitle());
        }
        awtContainer.setLayout(new BorderLayout());
        
        if( null == awtCanvas ) {
            awtCanvas = new AWTCanvas(capsRequested, WindowDriver.this.capabilitiesChooser);

            // canvas.addComponentListener(listener);
            awtContainer.add(awtCanvas, BorderLayout.CENTER);
        
            // via EDT ..
            new AWTMouseAdapter(this).addTo(awtCanvas); // fwd all AWT Mouse events to here
            new AWTKeyAdapter(this).addTo(awtCanvas); // fwd all AWT Key events to here
        
            // direct w/o EDT
            new AWTWindowAdapter(new LocalWindowListener(), this).addTo(awtCanvas); // fwd all AWT Window events to here
        }

        reconfigureWindowImpl(getX(), getY(), getWidth(), getHeight(), getReconfigureFlags(FLAG_CHANGE_VISIBILITY | FLAG_CHANGE_DECORATION, true));
        // throws exception if failed ..
        
        final NativeWindow nw = awtCanvas.getNativeWindow();
        if( null != nw ) {
            setGraphicsConfiguration( awtCanvas.getAWTGraphicsConfiguration() );            
            setWindowHandle( nw.getWindowHandle() );
        }
    }

    protected void closeNativeImpl() {
        setWindowHandle(0);
        if(null!=awtContainer) {
            awtContainer.setVisible(false);
            awtContainer.remove(awtCanvas);
            awtContainer.setEnabled(false);
            awtCanvas.setEnabled(false);
        }
        if(owningFrame && null!=awtFrame) {
            awtFrame.dispose();
            owningFrame=false;
        }
        awtCanvas = null;
        awtFrame = null;            
        awtContainer = null;
    }

    @Override
    public boolean hasDeviceChanged() {
        boolean res = awtCanvas.hasDeviceChanged();
        if(res) {
            final AWTGraphicsConfiguration cfg = awtCanvas.getAWTGraphicsConfiguration();
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
        final Insets contInsets = awtContainer.getInsets();
        insets.set(contInsets.left, contInsets.right, contInsets.top, contInsets.bottom);
    }

    private void setCanvasSizeImpl(int width, int height) {
        final Dimension szClient = new Dimension(width, height);
        final java.awt.Window awtWindow = AWTMisc.getWindow(awtCanvas);
        final Container c= null != awtWindow ? awtWindow : awtContainer;
        awtCanvas.setMinimumSize(szClient);
        awtCanvas.setPreferredSize(szClient);
        if(DEBUG_IMPLEMENTATION) {
            final Insets insets = c.getInsets();
            final Dimension szContainer = new Dimension(width + insets.left + insets.right,
                                                        height + insets.top + insets.bottom);
            System.err.println(getThreadName()+": AWTWindow setCanvasSize: szClient "+szClient+", szCont "+szContainer+", insets "+insets);
        }
        awtCanvas.setSize(szClient);
        awtCanvas.invalidate();
        if(null != awtWindow) {
            awtWindow.pack();
        } else {
            awtContainer.validate();
        }
    }
    private void setFrameSizeImpl(int width, int height) {
        final Insets insets = awtContainer.getInsets();
        final Dimension szContainer = new Dimension(width + insets.left + insets.right,
                                                    height + insets.top + insets.bottom);
        if(DEBUG_IMPLEMENTATION) {
            final Dimension szClient = new Dimension(width, height);
            System.err.println(getThreadName()+": AWTWindow setFrameSize: szClient "+szClient+", szCont "+szContainer+", insets "+insets);
        }
        awtContainer.setSize(szContainer);
        awtCanvas.invalidate();
        awtContainer.validate();
    }
    
    protected boolean reconfigureWindowImpl(int x, int y, int width, int height, int flags) {
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("AWTWindow reconfig: "+x+"/"+y+" "+width+"x"+height+", "+
                               getReconfigureFlagsAsString(null, flags));
        }
        if(0 != ( FLAG_CHANGE_DECORATION & flags) && null!=awtFrame) {
            if(!awtContainer.isDisplayable()) {
                awtFrame.setUndecorated(isUndecorated());
            } else {
                if(DEBUG_IMPLEMENTATION) {
                    System.err.println(getThreadName()+": AWTWindow can't undecorate already created frame");
                }
            }
        }
        
        if( 0 != ( FLAG_CHANGE_VISIBILITY & flags) ) {
            if( 0 != ( FLAG_IS_VISIBLE & flags) ) {
                setCanvasSizeImpl(width, height);
                awtContainer.setVisible( true );
            } else {
                awtContainer.setVisible( false );
            }
        } else if( awtCanvas.getWidth() != width || awtCanvas.getHeight() != height ) {
            if( Platform.OSType.MACOS == Platform.getOSType() && awtCanvas.isOffscreenLayerSurfaceEnabled() ) {
                setFrameSizeImpl(width, height);
            } else {
                setCanvasSizeImpl(width, height);
            }
        }
        defineSize(width, height); // we are on AWT-EDT .. change values immediately
        
        if( awtContainer.getX() != x || awtContainer.getY() != y ) {
            awtContainer.setLocation(x, y);
        }
        
        if( 0 != ( FLAG_CHANGE_VISIBILITY & flags) ) {
            if( 0 != ( FLAG_IS_VISIBLE & flags ) ) {
                if( !hasDeviceChanged() ) {
                    // oops ??                   
                    final AWTGraphicsConfiguration cfg = awtCanvas.getAWTGraphicsConfiguration();
                    if(null == cfg) {
                        throw new NativeWindowException("Error: !hasDeviceChanged && null == GraphicsConfiguration: "+this);
                    }
                    setGraphicsConfiguration(cfg);
                }
            }
            visibleChanged(false, 0 != ( FLAG_IS_VISIBLE & flags));
        }
        
        return true;
    }

    protected Point getLocationOnScreenImpl(int x, int y) {
        java.awt.Point ap = awtCanvas.getLocationOnScreen();
        ap.translate(x, y);
        return new Point((int)(ap.getX()+0.5),(int)(ap.getY()+0.5));
    }
   
    @Override
    public NativeSurface getWrappedSurface() {
        return ( null != awtCanvas ) ? awtCanvas.getNativeWindow() : null;
    }

    class LocalWindowListener implements com.jogamp.newt.event.WindowListener { 
        @Override
        public void windowMoved(com.jogamp.newt.event.WindowEvent e) {
            if(null!=awtContainer) {
                WindowDriver.this.positionChanged(false, awtContainer.getX(), awtContainer.getY());
            }
        }
        @Override
        public void windowResized(com.jogamp.newt.event.WindowEvent e) {
            if(null!=awtCanvas) {
                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("Window Resized: "+awtCanvas);
                }
                WindowDriver.this.sizeChanged(false, awtCanvas.getWidth(), awtCanvas.getHeight(), true);
                WindowDriver.this.windowRepaint(false, 0, 0, getWidth(), getHeight());
            }
        }
        @Override
        public void windowDestroyNotify(WindowEvent e) {
            WindowDriver.this.windowDestroyNotify(false);
        }
        @Override
        public void windowDestroyed(WindowEvent e) {
            // Not fwd by AWTWindowAdapter, synthesized by NEWT
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
            if(null!=awtCanvas) {
                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("Window Repaint: "+awtCanvas);
                }
                WindowDriver.this.windowRepaint(false, 0, 0, getWidth(), getHeight());
            }
        }
    }
}
