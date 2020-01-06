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
import com.jogamp.nativewindow.NativeWindowHolder;
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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.DPIUtil;
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
public class NewtCanvasSWT extends Canvas implements NativeWindowHolder, WindowClosingProtocol {
    private static final boolean DEBUG = Debug.debug("Window");

    private final AbstractGraphicsScreen screen;

    private WindowClosingMode newtChildClosingMode = WindowClosingMode.DISPOSE_ON_CLOSE;
    private final WindowClosingMode closingMode = WindowClosingMode.DISPOSE_ON_CLOSE;
    private volatile Rectangle clientAreaPixels, clientAreaWindow;
    /** pixelScale = pixelUnit / windowUnix */
    private volatile float[] pixelScale = new float[] { 1f, 1f };

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

        clientAreaPixels = getClientArea2InPixels();
        clientAreaWindow = getClientArea2();
        if( 0 < clientAreaWindow.width && 0 < clientAreaWindow.height ) {
            pixelScale[0] = clientAreaPixels.width / clientAreaWindow.width;
            pixelScale[1] = clientAreaPixels.height / clientAreaWindow.height;
        } else {
            pixelScale[0] = 1f;
            pixelScale[1] = 1f;
        }

        final AbstractGraphicsDevice device = SWTAccessor.getDevice(this);
        screen = SWTAccessor.getScreen(device, -1 /* default */);
        nativeWindow = null;

        // Bug 1362 fix or workaround: Seems SWT/GTK3 at least performs lazy initialization
        // Minimal action required: setBackground of the parent canvas before reparenting!
        setBackground(new Color(parent.getDisplay(), 255, 255, 255));

        if(null != child) {
            setNEWTChild(child);
        }
        if(DEBUG) {
            final long nsh = newtChildReady ? newtChild.getSurfaceHandle() : 0;
            System.err.println("NewtCanvasSWT: "+
                    ", ("+Thread.currentThread().getName()+"): newtChildReady "+newtChildReady+
                    ", pixel "+clientAreaPixels.x+"/"+clientAreaPixels.y+" "+clientAreaPixels.width+"x"+clientAreaPixels.height+
                    ", window "+clientAreaWindow.x+"/"+clientAreaWindow.y+" "+clientAreaWindow.width+"x"+clientAreaWindow.height+
                    ", scale "+pixelScale[0]+"/"+pixelScale[1]+
                    " - surfaceHandle 0x"+Long.toHexString(nsh));
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
                                newtChild.setSize(clientAreaWindow.width, clientAreaWindow.height);
                                postSetSize = false;
                            }
                            if( postSetPos ) {
                                newtChild.setPosition(clientAreaWindow.x, clientAreaWindow.y);
                                postSetPos = false;
                            }
                            newtChild.windowRepaint(0, 0, clientAreaPixels.width, clientAreaPixels.height);
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
                    updatePosSizeCheck(false /* updatePos */);
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

    private Rectangle getClientArea2() {
        final Rectangle r = this.getClientArea();
        if( SWTAccessor.isOSX ) {
            // On MacOS SWT implementation is as follows:
            // Scrollable.getClientArea():
            //   - Either using scrollView.contenview.bounds.position and scrollView.contentSize
            //   - Or using 0/0 and view.bounds.size
            //
            // Control's getSize() and getLocation() uses position and size of topView().frame()
            //
            final Rectangle parentClientArea = getParent().getClientArea();
            final org.eclipse.swt.graphics.Point l = getLocation();
            final org.eclipse.swt.graphics.Point s = getSize();
            if( DEBUG ) {
                System.err.println("XXX parent.clientArea: "+parentClientArea);
                System.err.println("XXX clientArea: "+r);
                System.err.println("XXX location:   "+l);
                System.err.println("XXX size:       "+s);
            }
            // Bug 1421
            // Subtracting parentClientArea's position fixes using:
            // - CTabFolder parent: y-position no more pushed down about tab-height (parentClientArea.x/y)
            // - SashForm parent: Offset is preserved, as it is not part of parentClientArea.x/y
            // Notable, this almost mimics the original 'getClientArea()' but
            // preserves SashForm's offset.
            //
            // The resulting offset still shows a 1-2 pixel x/y
            // lower-left corner overlap ..
            // Our GLCanvas doesn't show this behavior.
            r.x = l.x - parentClientArea.x; // FIXME +1?
            r.y = l.y - parentClientArea.y; // FIXME -1?
            r.width = s.x;
            r.height = s.y;
            if( DEBUG ) {
                System.err.println("XXX clientArea2:"+r);
            }
        }
        return r;
    }
    private Rectangle getClientArea2InPixels() {
        if( SWTAccessor.isOSX ) {
            return DPIUtil.autoScaleUp(getClientArea2());
        } else {
            // Essentially the same as: DPIUtil.autoScaleUp(getClientArea())
            return SWTAccessor.getClientAreaInPixels(this);
        }
    }

    @Override
    public void setBounds(final int x, final int y, final int width, final int height) {
    	super.setBounds(x, y, width, height);
    	if( DEBUG ) {
    	    System.err.println("NewtCanvasSWT.setBounds: "+x+"/"+y+" "+width+"x"+height);
    	}
    	if( SWTAccessor.isOSX ) {
    	    // Propagate the setBounds method coming from parent elements to this element
    	    // and force newtChild to update its position (OSX only)
    	    updatePosSizeCheck(true /* updatePos */);
    	}
    }

    /** assumes nativeWindow == null ! */
    protected final boolean validateNative() {
        updatePosSizeCheck(false /* updatePos */);
        final Rectangle nClientAreaWindow = clientAreaWindow;
        if(0 >= nClientAreaWindow.width || 0 >= nClientAreaWindow.height) {
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
        	    newtChild.setPosition(clientAreaWindow.x, clientAreaWindow.y);
        	}
        }

        return null != nativeWindow;
    }

    protected final void updatePosSizeCheck(final boolean updatePos) {
        final Rectangle oClientAreaWindow = clientAreaWindow;
        final Rectangle nClientAreaPixels = getClientArea2InPixels();
        final Rectangle nClientAreaWindow = getClientArea2();
        final boolean sizeChanged, posChanged;
        {
            sizeChanged = nClientAreaWindow.width != oClientAreaWindow.width || nClientAreaWindow.height != oClientAreaWindow.height;
            posChanged = nClientAreaWindow.x != oClientAreaWindow.x || nClientAreaWindow.y != oClientAreaWindow.y;
            if( sizeChanged || posChanged ) {
                clientAreaPixels = nClientAreaPixels;
                clientAreaWindow = nClientAreaWindow;
                if( 0 < nClientAreaWindow.width && 0 < nClientAreaWindow.height ) {
                    pixelScale[0] = nClientAreaPixels.width / nClientAreaWindow.width;
                    pixelScale[1] = nClientAreaPixels.height / nClientAreaWindow.height;
                } else {
                    pixelScale[0] = 1f;
                    pixelScale[1] = 1f;
                }
            }
        }
        if(DEBUG) {
            final long nsh = newtChildReady ? newtChild.getSurfaceHandle() : 0;
            System.err.println("NewtCanvasSWT.updatePosSizeCheck: sizeChanged "+sizeChanged+", posChanged "+posChanged+", updatePos "+updatePos+
                    ", ("+Thread.currentThread().getName()+"): newtChildReady "+newtChildReady+
                    ", pixel "+nClientAreaPixels.x+"/"+nClientAreaPixels.y+" "+nClientAreaPixels.width+"x"+nClientAreaPixels.height+
                    ", window "+nClientAreaWindow.x+"/"+nClientAreaWindow.y+" "+nClientAreaWindow.width+"x"+nClientAreaWindow.height+
                    ", scale "+pixelScale[0]+"/"+pixelScale[1]+
                    " - surfaceHandle 0x"+Long.toHexString(nsh));
            System.err.println("NewtCanvasSWT.updatePosSizeCheck.self: pixel-units pos "+SWTAccessor.getLocationInPixels(this)+", size "+SWTAccessor.getSizeInPixels(this));
            System.err.println("NewtCanvasSWT.updatePosSizeCheck.self: window-units pos "+this.getLocation()+", size "+this.getSize());
            System.err.println("NewtCanvasSWT.updatePosSizeCheck.self: LOS.0: "+SWTAccessor.getLocationOnScreen(new Point(), this));
        }
        if( sizeChanged ) {
            if( newtChildReady ) {
                newtChild.setSize(nClientAreaWindow.width, nClientAreaWindow.height);
                newtChild.setSurfaceScale(pixelScale);
            } else {
                postSetSize = true;
            }
        }
        if( updatePos && posChanged ) {
            if( newtChildReady ) {
                newtChild.setPosition(nClientAreaWindow.x, nClientAreaWindow.y);
            } else {
                postSetPos = true;
            }
        }
        if( DEBUG ) {
            System.err.println("NewtCanvasSWT.updatePosSizeCheck.X END");
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

    /**
     * {@inheritDoc}
     * @return this SWT Canvas {@link NativeWindow} representation, may be null in case it has not been realized
     */
    @Override
    public NativeWindow getNativeWindow() { return nativeWindow; }

    /**
     * {@inheritDoc}
     * @return this SWT Canvas {@link NativeSurface} representation, may be null in case it has not been realized
     */
    @Override
    public NativeSurface getNativeSurface() { return nativeWindow; }

    @Override
    public WindowClosingMode getDefaultCloseOperation() {
        return closingMode;
    }

    @Override
    public WindowClosingMode setDefaultCloseOperation(final WindowClosingMode op) {
        return closingMode; // TODO: implement!
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
                newtChildClosingMode = newtChild.setDefaultCloseOperation(WindowClosingMode.DO_NOTHING_ON_CLOSE);
            } else {
                newtChild.setFocusAction(null);
                newtChild.setDefaultCloseOperation(newtChildClosingMode);
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
            updatePosSizeCheck(false);

            // set SWT EDT and start it
            {
                final Display newtDisplay = newtChild.getScreen().getDisplay();
                final EDTUtil edtUtil = new SWTEDTUtil(newtDisplay, getDisplay());
                edtUtil.start();
                newtDisplay.setEDTUtil( edtUtil );
            }

            newtChild.setSize(clientAreaWindow.width, clientAreaWindow.height);
            newtChild.reparentWindow(nativeWindow, -1, -1, Window.REPARENT_HINT_BECOMES_VISIBLE);
            newtChild.setVisible(true);
            configureNewtChild(true);
            newtChild.setSurfaceScale(pixelScale); // ensure this to be set after creation, otherwise updatePosSizeCheck is being used
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
            return clientAreaWindow.width;
        }

        @Override
        public int getHeight() {
            return clientAreaWindow.height;
        }

        @Override
        public final int[] convertToWindowUnits(final int[] pixelUnitsAndResult) {
            pixelUnitsAndResult[0] /= pixelScale[0];
            pixelUnitsAndResult[1] /= pixelScale[1];
            return pixelUnitsAndResult;
        }

        @Override
        public final int[] convertToPixelUnits(final int[] windowUnitsAndResult) {
            windowUnitsAndResult[0] *= pixelScale[0];
            windowUnitsAndResult[1] *= pixelScale[1];
            return windowUnitsAndResult;
        }

        @Override
        public int getSurfaceWidth() {
            return clientAreaPixels.width;
        }

        @Override
        public int getSurfaceHeight() {
            return clientAreaPixels.height;
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
                // top-level position -> client window position: OSX needs to add SWT parent position incl. insets
            	// Bug 969 comment 2: let getLOS provide the point where the child window may be placed
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

