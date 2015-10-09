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

import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.NativeWindow;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.util.Point;

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

    public WindowDriver(final Container container) {
        super();
        this.withinLocalDispose = false;
        this.addWindowListener(0, new NEWTWindowListener());
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
    private volatile boolean withinLocalDispose;

    @Override
    protected void requestFocusImpl(final boolean reparented) {
        awtContainer.requestFocus();
    }

    @Override
    protected void setTitleImpl(final String title) {
        if (awtFrame != null) {
            awtFrame.setTitle(title);
        }
    }

    private final AWTCanvas.UpstreamScalable upstreamScalable = new AWTCanvas.UpstreamScalable() {
        @Override
        public float[] getReqPixelScale() {
            return WindowDriver.this.reqPixelScale;
        }

        @Override
        public void setHasPixelScale(final float[] pixelScale) {
            System.arraycopy(pixelScale, 0, WindowDriver.this.hasPixelScale, 0, 2);
        }
    };

    @Override
    protected void createNativeImpl() {
        if( withinLocalDispose ) {
            setupHandleAndGC();
            definePosition(getX(), getY()); // clear AUTOPOS
            visibleChanged(false, true);
            withinLocalDispose = false;
        } else {
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
                awtCanvas = new AWTCanvas(this, capsRequested, WindowDriver.this.capabilitiesChooser, upstreamScalable);

                // canvas.addComponentListener(listener);
                awtContainer.add(awtCanvas, BorderLayout.CENTER);

                // via EDT ..
                new AWTMouseAdapter(this).addTo(awtCanvas); // fwd all AWT Mouse events to here
                new AWTKeyAdapter(this).addTo(awtCanvas); // fwd all AWT Key events to here

                // direct w/o EDT
                new AWTWindowAdapter(new AWTWindowListener(), this).addTo(awtCanvas); // fwd all AWT Window events to here
            } else {
                awtContainer.add(awtCanvas, BorderLayout.CENTER);
            }
            reconfigureWindowImpl(getX(), getY(), getWidth(), getHeight(), getReconfigureMask(CHANGE_MASK_VISIBILITY | CHANGE_MASK_DECORATION, true));
            // throws exception if failed ..
            // AWTCanvas -> localCreate -> setupHandleAndGC();
        }
    }

    private void setupHandleAndGC() {
        // reconfigureWindowImpl(getX(), getY(), getWidth(), getHeight(), getReconfigureMask(CHANGE_MASK_VISIBILITY | CHANGE_MASK_DECORATION, true));
        if( null != awtCanvas ) {
            final NativeWindow nw = awtCanvas.getNativeWindow();
            if( null != nw ) {
                setGraphicsConfiguration( awtCanvas.getAWTGraphicsConfiguration() );
                setWindowHandle( nw.getWindowHandle() );
            }
        }
    }

    void localCreate() {
        if( withinLocalDispose ) {
            setVisible(true);
        } else {
            setupHandleAndGC();
        }
    }

    void localDestroy() {
        this.withinLocalDispose = true;
        super.destroy();
    }

    @Override
    protected void closeNativeImpl() {
        setWindowHandle(0);
        if( this.withinLocalDispose ) {
            if(null!=awtCanvas) {
                awtCanvas.dispose();
            }
        } else {
            if(null!=awtContainer) {
                awtContainer.setVisible(false);
                awtContainer.remove(awtCanvas);
                awtContainer.setEnabled(false);
                awtCanvas.setEnabled(false);
                awtCanvas.dispose();
            }
            if(owningFrame && null!=awtFrame) {
                awtFrame.dispose();
                owningFrame=false;
            }
            awtCanvas = null;
            awtFrame = null;
            awtContainer = null;
        }
    }

    @Override
    public boolean hasDeviceChanged() {
        final boolean res = awtCanvas.hasDeviceChanged();
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

    private void setCanvasSizeImpl(final int width, final int height) {
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
    private void setFrameSizeImpl(final int width, final int height) {
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

    @Override
    protected final int getSupportedReconfigMaskImpl() {
        return minimumReconfigStateMask;
    }

    @Override
    protected boolean reconfigureWindowImpl(final int x, final int y, final int width, final int height, final int flags) {
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("AWTWindow reconfig: "+x+"/"+y+" "+width+"x"+height+", "+
                               getReconfigStateMaskString(flags));
        }
        if(0 != ( CHANGE_MASK_DECORATION & flags) && null!=awtFrame) {
            if(!awtContainer.isDisplayable()) {
                awtFrame.setUndecorated(isUndecorated());
            } else {
                if(DEBUG_IMPLEMENTATION) {
                    System.err.println(getThreadName()+": AWTWindow can't undecorate already created frame");
                }
            }
        }

        if( 0 != ( CHANGE_MASK_VISIBILITY & flags) ) {
            if( 0 != ( STATE_MASK_VISIBLE & flags) ) {
                setCanvasSizeImpl(width, height);
                awtContainer.setVisible( true );
                final Insets contInsets = awtContainer.getInsets();
                insetsChanged(false, contInsets.left, contInsets.right, contInsets.top, contInsets.bottom);
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
        definePosition(x, y);

        if( 0 != ( CHANGE_MASK_VISIBILITY & flags) ) {
            if( 0 != ( STATE_MASK_VISIBLE & flags ) ) {
                if( !hasDeviceChanged() ) {
                    // oops ??
                    final AWTGraphicsConfiguration cfg = awtCanvas.getAWTGraphicsConfiguration();
                    if(null == cfg) {
                        throw new NativeWindowException("Error: !hasDeviceChanged && null == GraphicsConfiguration: "+this);
                    }
                    setGraphicsConfiguration(cfg);
                }
            }
            visibleChanged(false, 0 != ( STATE_MASK_VISIBLE & flags));
        }
        if( isVisible() ) {
            windowRepaint(false, 0, 0, getSurfaceWidth(), getSurfaceHeight());
        }

        return true;
    }

    @Override
    protected Point getLocationOnScreenImpl(final int x, final int y) {
        final java.awt.Point ap = awtCanvas.getLocationOnScreen();
        ap.translate(x, y);
        return new Point((int)(ap.getX()+0.5),(int)(ap.getY()+0.5));
    }

    @Override
    public NativeSurface getWrappedSurface() {
        return ( null != awtCanvas ) ? awtCanvas.getNativeWindow() : null;
    }

    class AWTWindowListener implements com.jogamp.newt.event.WindowListener {
        @Override
        public void windowMoved(final com.jogamp.newt.event.WindowEvent e) {
            if(null!=awtContainer) {
                WindowDriver.this.positionChanged(false, awtContainer.getX(), awtContainer.getY());
            }
        }
        @Override
        public void windowResized(final com.jogamp.newt.event.WindowEvent e) {
            if(null!=awtCanvas) {
                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("Window Resized: "+awtCanvas);
                }
                WindowDriver.this.sizeChanged(false, awtCanvas.getWidth(), awtCanvas.getHeight(), true);
                WindowDriver.this.windowRepaint(false, 0, 0, getSurfaceWidth(), getSurfaceHeight());
            }
        }
        @Override
        public void windowDestroyNotify(final WindowEvent e) {
            WindowDriver.this.windowDestroyNotify(false);
        }
        @Override
        public void windowDestroyed(final WindowEvent e) {
            // Not fwd by AWTWindowAdapter, synthesized by NEWT
        }
        @Override
        public void windowGainedFocus(final WindowEvent e) {
            WindowDriver.this.focusChanged(false, true);
        }
        @Override
        public void windowLostFocus(final WindowEvent e) {
            WindowDriver.this.focusChanged(false, false);
        }
        @Override
        public void windowRepaint(final WindowUpdateEvent e) {
            if(null!=awtCanvas) {
                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("Window Repaint: "+awtCanvas);
                }
                WindowDriver.this.windowRepaint(false, 0, 0, getSurfaceWidth(), getSurfaceHeight());
            }
        }
    }
    class NEWTWindowListener implements com.jogamp.newt.event.WindowListener {
        @Override
        public void windowMoved(final com.jogamp.newt.event.WindowEvent e) { }
        @Override
        public void windowResized(final com.jogamp.newt.event.WindowEvent e) { }
        @Override
        public void windowDestroyNotify(final WindowEvent e) {
            if( withinLocalDispose ) {
                e.setConsumed(true);
            }
        }
        @Override
        public void windowDestroyed(final WindowEvent e) {
            if( withinLocalDispose ) {
                e.setConsumed(true);
            }
        }
        @Override
        public void windowGainedFocus(final WindowEvent e) { }
        @Override
        public void windowLostFocus(final WindowEvent e) { }
        @Override
        public void windowRepaint(final WindowUpdateEvent e) { }
    }
}
