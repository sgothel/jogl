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

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.Capabilities;
import com.jogamp.nativewindow.CapabilitiesImmutable;
import com.jogamp.nativewindow.GraphicsConfigurationFactory;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.NativeWindow;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.SurfaceUpdatedListener;
import com.jogamp.nativewindow.WindowClosingProtocol;
import com.jogamp.nativewindow.util.Insets;
import com.jogamp.nativewindow.util.InsetsImmutable;
import com.jogamp.nativewindow.util.Point;
import com.jogamp.opengl.GLCapabilities;

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
    private volatile boolean postSetPos = false; // pending pos

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
    public NewtCanvasSWT(final Composite parent, final int style, final Window child) {
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
            public void handleEvent (final Event event) {
                switch (event.type) {
                case SWT.Paint:
                    if( DEBUG ) {
                        System.err.println("NewtCanvasSWT.Event.PAINT, "+event);
                    }
                    if( null != nativeWindow || validateNative() ) {
                        if( newtChildReady ) {
                            if( postSetSize ) {
                                newtChild.setSize(clientArea.width, clientArea.height);
                                postSetSize = false;
                            }
                            if( postSetPos ) {
                                newtChild.setPosition(clientArea.x, clientArea.y);
                                postSetPos = false;
                            }
                            newtChild.windowRepaint(0, 0, clientArea.width, clientArea.height);
                        }
                    }
                    break;
                case SWT.Move:
                    if( DEBUG ) {
                        System.err.println("NewtCanvasSWT.Event.MOVE, "+event);
                    }
                    // updatePosSizeCheck();
                    break;
                case SWT.Resize:
                    if( DEBUG ) {
                        System.err.println("NewtCanvasSWT.Event.RESIZE, "+event);
                    }
                    updateSizeCheck();
                    break;
                case SWT.Dispose:
                    if( DEBUG ) {
                        System.err.println("NewtCanvasSWT.Event.DISPOSE, "+event);
                    }
                    NewtCanvasSWT.this.dispose();
                    break;
                default:
                    if( DEBUG ) {
                        System.err.println("NewtCanvasSWT.Event.misc: "+event.type+", "+event);
                    }
                }
            }
        };
        // addListener (SWT.Move, listener);
        addListener (SWT.Resize, listener);
        addListener (SWT.Paint, listener);
        addListener (SWT.Dispose, listener);
    }

    @Override
    public void setBounds(final int x, final int y, final int width, final int height) {
    	super.setBounds(x, y, width, height);
    	if( DEBUG ) {
    	    System.err.println("NewtCanvasSWT.setBounds: "+x+"/"+y+" "+width+"x"+height);
    	}
    	if( SWTAccessor.isOSX ) {
            // Force newtChild to update its size and position (OSX only)
    	    updatePosSizeCheck(x, y, width, height, true /* updatePos */);
    	}
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
        	if( SWTAccessor.isOSX && newtChildReady ) {
        	    // initial positioning for OSX, called when the window is created
        	    newtChild.setPosition(getLocation().x, getLocation().y);
        	}
        }

        return null != nativeWindow;
    }

    protected final void updateSizeCheck() {
        final Rectangle nClientArea = getClientArea();
        if( null != nClientArea ) {
            updatePosSizeCheck(nClientArea.x, nClientArea.y, nClientArea.width, nClientArea.height, false /* updatePos */);
        }
    }
    protected final void updatePosSizeCheck() {
        final Rectangle nClientArea = getClientArea();
        if( null != nClientArea ) {
            updatePosSizeCheck(nClientArea.x, nClientArea.y, nClientArea.width, nClientArea.height, true /* updatePos */);
        }
    }
    protected final void updatePosSizeCheck(final int newX, final int newY, final int newWidth, final int newHeight, final boolean updatePos) {
        final boolean sizeChanged, posChanged;
        final Rectangle nClientArea;
        {
            final Rectangle oClientArea = clientArea;
            sizeChanged = newWidth != oClientArea.width || newHeight != oClientArea.height;
            posChanged = newX != oClientArea.x || newY != oClientArea.y;
            if( sizeChanged || posChanged ) {
                nClientArea = new Rectangle(updatePos ? newX : oClientArea.x, updatePos ? newY : oClientArea.y, newWidth, newHeight);
                clientArea = nClientArea;
            } else {
                nClientArea = clientArea;
            }
        }
        if(DEBUG) {
            final long nsh = newtChildReady ? newtChild.getSurfaceHandle() : 0;
            System.err.println("NewtCanvasSWT.updatePosSizeCheck: sizeChanged "+sizeChanged+", posChanged "+posChanged+", updatePos "+updatePos+", ("+Thread.currentThread().getName()+"): newtChildReady "+newtChildReady+", "+nClientArea.x+"/"+nClientArea.y+" "+nClientArea.width+"x"+nClientArea.height+" - surfaceHandle 0x"+Long.toHexString(nsh));
        }
        if( sizeChanged ) {
            if( newtChildReady ) {
                newtChild.setSize(nClientArea.width, nClientArea.height);
            } else {
                postSetSize = true;
            }
        }
        if( updatePos && posChanged ) {
            if( newtChildReady ) {
                newtChild.setPosition(nClientArea.x, nClientArea.y);
            } else {
                postSetPos = true;
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
            newtChild.reparentWindow(null, -1, -1, 0 /* hint */);
            newtChild.destroy();
            newtChild = null;
        }
        screen.getDevice().close();
        nativeWindow = null;
        super.dispose();
    }

    private Point getParentLocationOnScreen() {
        final org.eclipse.swt.graphics.Point[] parentLoc = new org.eclipse.swt.graphics.Point[] { null };
        SWTAccessor.invoke(true, new Runnable() {
            public void run() {
                parentLoc[0] = getParent().toDisplay(0,0);
            } } );
        return new Point(parentLoc[0].x, parentLoc[0].y);
    }

    /** @return this SWT Canvas NativeWindow representation, may be null in case it has not been realized. */
    public NativeWindow getNativeWindow() { return nativeWindow; }

    @Override
    public WindowClosingMode getDefaultCloseOperation() {
        return newtChildCloseOp; // TODO: implement ?!
    }

    @Override
    public WindowClosingMode setDefaultCloseOperation(final WindowClosingMode op) {
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
    public boolean setParent(final Composite parent) {
        return super.setParent(parent);
    }

    /* package */ void configureNewtChild(final boolean attach) {
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

    void reparentWindow(final boolean add) {
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
            newtChild.reparentWindow(nativeWindow, -1, -1, Window.REPARENT_HINT_BECOMES_VISIBLE);
            newtChild.setVisible(true);
            configureNewtChild(true);
            newtChild.sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED); // trigger a resize/relayout to listener

            // force this SWT Canvas to be focus-able,
            // since it is completely covered by the newtChild (z-order).
            setEnabled(true);
        } else {
            configureNewtChild(false);
            newtChild.setVisible(false);
            newtChild.reparentWindow(null, -1, -1, 0 /* hints */);
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

        public SWTNativeWindow(final AbstractGraphicsConfiguration config, final long nativeWindowHandle) {
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
        public void addSurfaceUpdatedListener(final SurfaceUpdatedListener l) { }

        @Override
        public void addSurfaceUpdatedListener(final int index, final SurfaceUpdatedListener l) throws IndexOutOfBoundsException {
        }

        @Override
        public void removeSurfaceUpdatedListener(final SurfaceUpdatedListener l) { }

        @Override
        public long getSurfaceHandle() {
            return 0;
        }

        @Override
        public int getWidth() {
            return getSurfaceWidth(); // FIXME: Use 'scale' or an actual window-width
        }

        @Override
        public int getHeight() {
            return getSurfaceHeight(); // FIXME: Use 'scale' or an actual window-width
        }

        @Override
        public final int[] convertToWindowUnits(final int[] pixelUnitsAndResult) {
            return pixelUnitsAndResult; // FIXME HiDPI: use 'pixelScale'
        }

        @Override
        public final int[] convertToPixelUnits(final int[] windowUnitsAndResult) {
            return windowUnitsAndResult; // FIXME HiDPI: use 'pixelScale'
        }

        @Override
        public int getSurfaceWidth() {
            return clientArea.width;
        }

        @Override
        public int getSurfaceHeight() {
            return clientArea.height;
        }

        @Override
        public final NativeSurface getNativeSurface() { return this; }

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
        public void surfaceUpdated(final Object updater, final NativeSurface ns, final long when) { }

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
        public Point getLocationOnScreen(final Point point) {
            final Point los; // client window location on screen
            if( SWTAccessor.isOSX ) {
            	// let getLOS provide the point where the child window may be placed
            	// from, as taken from SWT Control.toDisplay();
            	los = getParentLocationOnScreen();
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

    static String newtWinHandleToHexString(final Window w) {
        return null != w ? toHexString(w.getWindowHandle()) : "nil";
    }
    static String toHexString(final long l) {
        return "0x"+Long.toHexString(l);
    }
}

