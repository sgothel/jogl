/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

package com.jogamp.newt.swt;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.Capabilities;
import javax.media.nativewindow.CapabilitiesImmutable;
import javax.media.nativewindow.GraphicsConfigurationFactory;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.SurfaceUpdatedListener;
import javax.media.nativewindow.WindowClosingProtocol;
import javax.media.nativewindow.util.Insets;
import javax.media.nativewindow.util.InsetsImmutable;
import javax.media.nativewindow.util.Point;
import javax.media.opengl.GLCapabilities;

import jogamp.nativewindow.macosx.OSXUtil;
import jogamp.nativewindow.windows.GDIUtil;
import jogamp.nativewindow.x11.X11Lib;
import jogamp.newt.Debug;
import jogamp.newt.swt.SWTEDTUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.jogamp.nativewindow.swt.SWTAccessor;
import com.jogamp.newt.Display;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.util.EDTUtil;

/**
 * SWT {@link Canvas} containing a NEWT {@link Window} using native parenting.
 * <p>
 * Implementation allows use of custom {@link GLCapabilities}.
 * </p>
 */
public class NewtCanvasSWT extends Canvas implements WindowClosingProtocol {
    private static final boolean DEBUG = Debug.debug("Window");

    private final AbstractGraphicsScreen screen;

    private WindowClosingMode newtChildCloseOp = WindowClosingMode.DISPOSE_ON_CLOSE;
    private volatile Rectangle clientArea;

    private volatile SWTNativeWindow nativeWindow;
    private volatile Window newtChild = null;
    private volatile boolean newtChildReady = false; // ready if SWTEDTUtil is set and newtChild parented
    private volatile boolean postSetSize = false; // pending resize

    /**
     * Creates an instance using {@link #NewtCanvasSWT(Composite, int, Window)}
     * on the SWT thread.
     *
     * <p>
     * Note: The NEWT child {@link Display}'s {@link EDTUtil} is being set to an SWT conform implementation
     *       via {@link Display#setEDTUtil(EDTUtil)}.
     * </p>
     *
     * @param parent the SWT composite
     * @param style additional styles to SWT#NO_BACKGROUND
     * @param child optional preassigned {@link #Window}, maybe null
     * @return a new instance
     */
    public static NewtCanvasSWT create(final Composite parent, final int style, final Window child) {
        final NewtCanvasSWT[] res = new NewtCanvasSWT[] { null };
        parent.getDisplay().syncExec( new Runnable() {
           @Override
           public void run() {
               res[0] = new NewtCanvasSWT( parent, style, child);
           }
        });
        return res[0];
    }

    /**
     * Instantiates a NewtCanvas with a NEWT child.
     *
     * <p>
     * Note: The NEWT child {@link Display}'s {@link EDTUtil} is being set to an SWT conform implementation
     *       via {@link Display#setEDTUtil(EDTUtil)}.
     * </p>
     *
     * @param parent the SWT composite
     * @param style additional styles to SWT#NO_BACKGROUND
     * @param child optional preassigned {@link #Window}, maybe null
     */
    public NewtCanvasSWT(final Composite parent, final int style, Window child) {
        super(parent, style | SWT.NO_BACKGROUND);

        SWTAccessor.setRealized(this, true);

        clientArea = getClientArea();

        final AbstractGraphicsDevice device = SWTAccessor.getDevice(this);
        screen = SWTAccessor.getScreen(device, -1 /* default */);
        nativeWindow = null;

        if(null != child) {
            setNEWTChild(child);
        }

        final Listener listener = new Listener () {
            @Override
            public void handleEvent (Event event) {
                switch (event.type) {
                case SWT.Paint:
                    if( null != nativeWindow || validateNative() ) {
                        if( newtChildReady ) {
                            if( postSetSize ) {
                                newtChild.setSize(clientArea.width, clientArea.height);
                                postSetSize = false;
                            }
                            if( SWTAccessor.isOSX ) {
                                newtChild.setPosition(parent.getLocation().x,parent.getLocation().y);
                            }
                            newtChild.windowRepaint(0, 0, clientArea.width, clientArea.height);
                        }
                    }
                    break;
                case SWT.Resize:
                    updateSizeCheck();
                    break;
                case SWT.Dispose:
                    NewtCanvasSWT.this.dispose();
                    break;
                }
            }
        };
        addListener (SWT.Resize, listener);
        addListener (SWT.Paint, listener);
        addListener (SWT.Dispose, listener);
    }

    /** assumes nativeWindow == null ! */
    protected final boolean validateNative() {
        updateSizeCheck();
        final Rectangle nClientArea = clientArea;
        if(0 >= nClientArea.width || 0 >= nClientArea.height) {
            return false;
        }
        screen.getDevice().open();

        /* Native handle for the control, used to associate with GLContext */
        final long nativeWindowHandle = SWTAccessor.getWindowHandle(this);
        final int visualID = SWTAccessor.getNativeVisualID(screen.getDevice(), nativeWindowHandle);
        final boolean visualIDValid = NativeWindowFactory.isNativeVisualIDValidForProcessing(visualID);
        if(DEBUG) {
            System.err.println("NewtCanvasSWT.validateNative() windowHandle 0x"+Long.toHexString(nativeWindowHandle)+", visualID 0x"+Integer.toHexString(visualID)+", valid "+visualIDValid);
        }
        if( visualIDValid ) {
            /* Get the nativewindow-Graphics Device associated with this control (which is determined by the parent Composite).
             * Note: SWT is owner of the native handle, hence no closing operation will be a NOP. */
            final CapabilitiesImmutable caps = new Capabilities();
            final GraphicsConfigurationFactory factory = GraphicsConfigurationFactory.getFactory(screen.getDevice(), caps);
            final AbstractGraphicsConfiguration config = factory.chooseGraphicsConfiguration( caps, caps, null, screen, visualID );
            if(DEBUG) {
                System.err.println("NewtCanvasSWT.validateNative() factory: "+factory+", windowHandle 0x"+Long.toHexString(nativeWindowHandle)+", visualID 0x"+Integer.toHexString(visualID)+", chosen config: "+config);
                // Thread.dumpStack();
            }
            if (null == config) {
                throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
            }

            nativeWindow = new SWTNativeWindow(config, nativeWindowHandle);
            reparentWindow( true );
        }

        return null != nativeWindow;
    }

    protected final void updateSizeCheck() {
        final Rectangle oClientArea = clientArea;
        final Rectangle nClientArea = getClientArea();
        if ( nClientArea != null &&
             ( nClientArea.width != oClientArea.width || nClientArea.height != oClientArea.height )
           ) {
            clientArea = nClientArea; // write back new value
            if(DEBUG) {
                final long nsh = newtChildReady ? newtChild.getSurfaceHandle() : 0;
                System.err.println("NewtCanvasSWT.sizeChanged: ("+Thread.currentThread().getName()+"): newtChildReady "+newtChildReady+", "+nClientArea.x+"/"+nClientArea.y+" "+nClientArea.width+"x"+nClientArea.height+" - surfaceHandle 0x"+Long.toHexString(nsh));
            }
            if( newtChildReady ) {
                newtChild.setSize(clientArea.width, clientArea.height);
            } else {
                postSetSize = true;
            }
        }
    }

    @Override
    public void update() {
        // don't paint background etc .. nop avoids flickering
    }

    /**
     * Destroys this resource:
     * <ul>
     *   <li> Make the NEWT Child invisible </li>
     *   <li> Disconnects the NEWT Child from this Canvas NativeWindow, reparent to NULL </li>
     *   <li> Issues <code>destroy()</code> on the NEWT Child</li>
     *   <li> Remove reference to the NEWT Child</li>
     * </ul>
     * @see Window#destroy()
     */
    @Override
    public void dispose() {
        if( null != newtChild ) {
            if(DEBUG) {
                System.err.println("NewtCanvasSWT.dispose.0: EDTUtil cur "+newtChild.getScreen().getDisplay().getEDTUtil()+
                                   ",\n\t"+newtChild);
            }
            configureNewtChild(false);
            newtChild.setVisible(false);
            newtChild.reparentWindow(null);
            newtChild.destroy();
            newtChild = null;
        }
        screen.getDevice().close();
        nativeWindow = null;
        super.dispose();
    }

    private Rectangle getSWTCanvasPosition() {
        return super.getBounds();
    }
    /** @return this SWT Canvas NativeWindow representation, may be null in case it has not been realized. */
    public NativeWindow getNativeWindow() { return nativeWindow; }

    @Override
    public WindowClosingMode getDefaultCloseOperation() {
        return newtChildCloseOp; // TODO: implement ?!
    }

    @Override
    public WindowClosingMode setDefaultCloseOperation(WindowClosingMode op) {
        return newtChildCloseOp = op; // TODO: implement ?!
    }


    boolean isParent() {
        return null!=newtChild ;
    }

    boolean isFullscreen() {
        return null != newtChild && newtChild.isFullscreen();
    }

    /**
     * Sets a new NEWT child, provoking reparenting.
     * <p>
     * A previously detached <code>newChild</code> will be released to top-level status
     * and made invisible.
     * </p>
     * <p>
     * Note: When switching NEWT child's, detaching the previous first via <code>setNEWTChild(null)</code>
     * produced much cleaner visual results.
     * </p>
     * <p>
     * Note: The NEWT child {@link Display}'s {@link EDTUtil} is being set to an SWT conform implementation
     *       via {@link Display#setEDTUtil(EDTUtil)}.
     * </p>
     * @return the previous attached newt child.
     */
    public Window setNEWTChild(final Window newChild) {
        final Window prevChild = newtChild;
        if(DEBUG) {
            System.err.println("NewtCanvasSWT.setNEWTChild.0: win "+newtWinHandleToHexString(prevChild)+" -> "+newtWinHandleToHexString(newChild));
        }
        // remove old one
        if(null != newtChild) {
            reparentWindow( false );
            newtChild = null;
        }
        // add new one, reparent only if ready
        newtChild = newChild;
        if(null != nativeWindow && null != newChild) {
            reparentWindow( true );
        }
        return prevChild;
    }

    /** @return the current NEWT child */
    public Window getNEWTChild() {
        return newtChild;
    }

    @Override
    public boolean setParent(Composite parent) {
        return super.setParent(parent);
    }

    /* package */ void configureNewtChild(boolean attach) {
        newtChildReady = attach;
        if( null != newtChild ) {
            newtChild.setKeyboardFocusHandler(null);
            if(attach) {
                newtChildCloseOp = newtChild.setDefaultCloseOperation(WindowClosingMode.DO_NOTHING_ON_CLOSE);
            } else {
                newtChild.setFocusAction(null);
                newtChild.setDefaultCloseOperation(newtChildCloseOp);
            }
        }
    }

    void reparentWindow(boolean add) {
        if( null == newtChild ) {
            return; // nop
        }
        if(DEBUG) {
            System.err.println("NewtCanvasSWT.reparentWindow.0: add="+add+", win "+newtWinHandleToHexString(newtChild)+", EDTUtil: cur "+newtChild.getScreen().getDisplay().getEDTUtil());
        }

        newtChild.setFocusAction(null); // no AWT focus traversal ..
        if(add) {
            updateSizeCheck();
            final int w = clientArea.width;
            final int h = clientArea.height;

            // set SWT EDT and start it
            {
                final Display newtDisplay = newtChild.getScreen().getDisplay();
                final EDTUtil edtUtil = new SWTEDTUtil(newtDisplay, getDisplay());
                edtUtil.start();
                newtDisplay.setEDTUtil( edtUtil );
            }

            newtChild.setSize(w, h);
            newtChild.reparentWindow(nativeWindow);
            newtChild.setVisible(true);
            configureNewtChild(true);
            newtChild.sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED); // trigger a resize/relayout to listener

            // force this SWT Canvas to be focus-able,
            // since it is completely covered by the newtChild (z-order).
            setEnabled(true);
        } else {
            configureNewtChild(false);
            newtChild.setVisible(false);
            newtChild.reparentWindow(null);
        }
        if(DEBUG) {
            System.err.println("NewtCanvasSWT.reparentWindow.X: add="+add+", win "+newtWinHandleToHexString(newtChild)+", EDTUtil: cur "+newtChild.getScreen().getDisplay().getEDTUtil());
        }
    }

    private final void requestFocusNEWTChild() {
        if( newtChildReady ) {
            newtChild.setFocusAction(null);
            newtChild.requestFocus();
        }
    }

    @Override
    public boolean forceFocus() {
        final boolean res = NewtCanvasSWT.super.forceFocus();
        requestFocusNEWTChild();
        return res;
    }

    private class SWTNativeWindow implements NativeWindow {
        private final AbstractGraphicsConfiguration config;
        private final long nativeWindowHandle;
        private final InsetsImmutable insets; // only required to allow proper client position calculation on OSX

        public SWTNativeWindow(AbstractGraphicsConfiguration config, long nativeWindowHandle) {
            this.config = config;
            this.nativeWindowHandle = nativeWindowHandle;
            if( SWTAccessor.isOSX ) {
                this.insets = OSXUtil.GetInsets(nativeWindowHandle);
            } else {
                this.insets = new Insets(0, 0, 0, 0);
            }
        }

        @Override
        public int lockSurface() throws NativeWindowException, RuntimeException {
            return NativeSurface.LOCK_SUCCESS;
        }

        @Override
        public void unlockSurface() { }

        @Override
        public boolean isSurfaceLockedByOtherThread() {
            return false;
        }

        @Override
        public Thread getSurfaceLockOwner() {
            return null;
        }

        @Override
        public boolean surfaceSwap() {
            return false;
        }

        @Override
        public void addSurfaceUpdatedListener(SurfaceUpdatedListener l) { }

        @Override
        public void addSurfaceUpdatedListener(int index, SurfaceUpdatedListener l) throws IndexOutOfBoundsException {
        }

        @Override
        public void removeSurfaceUpdatedListener(SurfaceUpdatedListener l) { }

        @Override
        public long getSurfaceHandle() {
            return 0;
        }

        @Override
        public int getWidth() {
            return clientArea.width;
        }

        @Override
        public int getHeight() {
            return clientArea.height;
        }

        @Override
        public AbstractGraphicsConfiguration getGraphicsConfiguration() {
            return config;
        }

        @Override
        public long getDisplayHandle() {
            return config.getScreen().getDevice().getHandle();
        }

        @Override
        public int getScreenIndex() {
            return config.getScreen().getIndex();
        }

        @Override
        public void surfaceUpdated(Object updater, NativeSurface ns, long when) { }

        @Override
        public void destroy() { }

        @Override
        public NativeWindow getParent() {
            return null;
        }

        @Override
        public long getWindowHandle() {
            return nativeWindowHandle;
        }

        @Override
        public InsetsImmutable getInsets() {
            return insets;
        }

        @Override
        public int getX() {
            return 0;
        }

        @Override
        public int getY() {
            return 0;
        }

        @Override
        public Point getLocationOnScreen(Point point) {
            final Point los; // client window location on screen
            if( SWTAccessor.isOSX ) {
                los = OSXUtil.GetLocationOnScreen(nativeWindowHandle, false, 0, 0);
                // top-level position -> client window position: OSX needs to add SWT parent position incl. insets
                final Rectangle swtCanvasPosition = getSWTCanvasPosition();
                los.translate(swtCanvasPosition.x + insets.getLeftWidth(), swtCanvasPosition.y + insets.getTopHeight());
            } else if (SWTAccessor.isX11) {
                final AbstractGraphicsScreen s = config.getScreen();
                los = X11Lib.GetRelativeLocation(s.getDevice().getHandle(), s.getIndex(), nativeWindowHandle, 0 /*root win*/, 0, 0);
            } else if (SWTAccessor.isWindows) {
                los = GDIUtil.GetRelativeLocation( nativeWindowHandle, 0 /*root win*/, 0, 0);
            } else {
                // fall-back to 0/0
                los = new Point(0, 0);
            }
            if(null!=point) {
              return point.translate(los);
            } else {
              return los;
            }
        }

        @Override
        public boolean hasFocus() {
            return isFocusControl();
        }
    };

    static String newtWinHandleToHexString(Window w) {
        return null != w ? toHexString(w.getWindowHandle()) : "nil";
    }
    static String toHexString(long l) {
        return "0x"+Long.toHexString(l);
    }
}

