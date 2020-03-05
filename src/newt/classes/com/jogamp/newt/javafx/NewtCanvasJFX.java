/**
 * Copyright 2019 JogAmp Community. All rights reserved.
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

package com.jogamp.newt.javafx;

import com.jogamp.common.util.PropertyAccess;
import com.jogamp.common.util.locks.RecursiveLock;
import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
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
import com.jogamp.nativewindow.WindowClosingProtocol.WindowClosingMode;
import com.jogamp.nativewindow.util.Insets;
import com.jogamp.nativewindow.util.InsetsImmutable;
import com.jogamp.nativewindow.util.Point;
import com.jogamp.nativewindow.util.Rectangle;
import com.jogamp.opengl.GLCapabilities;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import jogamp.newt.Debug;
import jogamp.newt.javafx.JFXEDTUtil;

import com.jogamp.nativewindow.javafx.JFXAccessor;
import com.jogamp.newt.Display;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.util.EDTUtil;

/**
 * A NEWT based JFX {@link Canvas} specialization allowing a NEWT child {@link Window} to be attached using native parenting.
 * <p>
 * {@link NewtCanvasJFX} allows utilizing custom {@link GLCapabilities} settings independent from the JavaFX's window
 * as well as independent rendering from JavaFX's thread.
 * </p>
 * <p>
 * {@link NewtCanvasJFX} allows native parenting operations before and after
 * it's belonging Group's Scene has been attached to the JavaFX {@link javafx.stage.Window Window}'s actual native window,
 * i.e. becoming fully realized and visible.
 * </p>
 * <p>
 * Note that {@link JFXAccessor#runOnJFXThread(boolean, Runnable)} is still used to for certain
 * mandatory JavaFX lifecycle operation on the JavaFX thread.
 * </p>
 */
public class NewtCanvasJFX extends Canvas implements NativeWindowHolder, WindowClosingProtocol {
    private static final boolean DEBUG = Debug.debug("Window");
    private static final boolean USE_JFX_EDT = PropertyAccess.getBooleanProperty("jogamp.newt.javafx.UseJFXEDT", true, true);
    private volatile javafx.stage.Window parentWindow = null;
    private volatile AbstractGraphicsScreen screen = null;

    private WindowClosingMode newtChildClosingMode = WindowClosingMode.DISPOSE_ON_CLOSE;
    private WindowClosingMode closingMode = WindowClosingMode.DISPOSE_ON_CLOSE;
    private final Rectangle clientArea = new Rectangle();

    private volatile JFXNativeWindow nativeWindow = null;
    private volatile Window newtChild = null;
    private volatile boolean newtChildReady = false; // ready if JFXEDTUtil is set and newtChild parented
    private volatile boolean postSetSize = false; // pending resize
    private volatile boolean postSetPos = false; // pending pos

    private final EventHandler<javafx.stage.WindowEvent> windowClosingListener = new EventHandler<javafx.stage.WindowEvent>() {
        @Override
        public final void handle(final javafx.stage.WindowEvent e) {
            if( DEBUG ) {
                System.err.println("NewtCanvasJFX.Event.DISPOSE, "+e+", closeOp "+closingMode);
            }
            if( WindowClosingMode.DISPOSE_ON_CLOSE == closingMode ) {
                NewtCanvasJFX.this.destroy();
            } else {
                // avoid JavaFX closing operation
                e.consume();
            }
        } };
    private final EventHandler<javafx.stage.WindowEvent> windowShownListener = new EventHandler<javafx.stage.WindowEvent>() {
        @Override
        public final void handle(final javafx.stage.WindowEvent e) {
            if( DEBUG ) {
                System.err.println("NewtCanvasJFX.Event.SHOWN, "+e);
            }
            repaintAction(true);
        } };

    /**
     * Instantiates a NewtCanvas with a NEWT child.
     *
     * <p>
     * Note: The NEWT child {@link Display}'s {@link EDTUtil} is being set to an JFX conform implementation
     *       via {@link Display#setEDTUtil(EDTUtil)}.
     * </p>
     * @param child optional preassigned {@link #Window}, maybe null
     */
    public NewtCanvasJFX(final Window child) {
        super();

        updateParentWindowAndScreen();

        final ChangeListener<Number> sizeListener = new ChangeListener<Number>() {
            @Override public void changed(final ObservableValue<? extends Number> observable, final Number oldValue, final Number newValue) {
                if( DEBUG ) {
                    System.err.println("NewtCanvasJFX.Event.Size, "+oldValue.doubleValue()+" -> "+newValue.doubleValue()+", has "+getWidth()+"x"+getHeight());
                }
                updateSizeCheck((int)getWidth(), (int)getHeight());
                repaintAction(isVisible());
            } };
        this.widthProperty().addListener(sizeListener);
        this.heightProperty().addListener(sizeListener);
        this.visibleProperty().addListener(new ChangeListener<Boolean>() {
            @Override public void changed(final ObservableValue<? extends Boolean> observable, final Boolean oldValue, final Boolean newValue) {
                if( DEBUG ) {
                    System.err.println("NewtCanvasJFX.Event.Visible, "+oldValue.booleanValue()+" -> "+newValue.booleanValue()+", has "+isVisible());
                }
                repaintAction(newValue.booleanValue());
            }
        });
        this.sceneProperty().addListener(new ChangeListener<Scene>() {
            @Override public void changed(final ObservableValue<? extends Scene> observable, final Scene oldValue, final Scene newValue) {
                if( DEBUG ) {
                    System.err.println("NewtCanvasJFX.Event.Scene, "+oldValue+" -> "+newValue+", has "+getScene());
                    if(null != newValue) {
                        final javafx.stage.Window w = newValue.getWindow();
                        System.err.println("NewtCanvasJFX.Event.Scene window "+w+" (showing "+(null!=w?w.isShowing():0)+")");
                    }
                }
                if( updateParentWindowAndScreen() ) {
                    repaintAction(isVisible());
                }
            }
        });

        if(null != child) {
            setNEWTChild(child);
        }
    }

    private final void repaintAction(final boolean visible) {
        if( visible && validateNative(true /* completeReparent */) ) {
            if( newtChildReady ) {
                if( postSetSize ) {
                    newtChild.setSize(clientArea.getWidth(), clientArea.getHeight());
                    postSetSize = false;
                }
                if( postSetPos ) {
                    newtChild.setPosition(clientArea.getX(), clientArea.getY());
                    postSetPos = false;
                }
                newtChild.windowRepaint(0, 0, clientArea.getWidth(), clientArea.getHeight());
            }
        }
    }

    private final void updatePosSizeCheck() {
        final Bounds b = localToScene(getBoundsInLocal());
        updatePosCheck((int)b.getMinX(), (int)b.getMinY());
        updateSizeCheck((int)getWidth(), (int)getHeight());
    }
    private final void updatePosCheck(final int newX, final int newY) {
        final boolean posChanged;
        {
            final Rectangle oClientArea = clientArea;
            posChanged = newX != oClientArea.getX() || newY != oClientArea.getY();
            if( posChanged ) {
                clientArea.setX(newX);
                clientArea.setY(newY);
            }
        }
        if(DEBUG) {
            final long nsh = newtChildReady ? newtChild.getSurfaceHandle() : 0;
            System.err.println("NewtCanvasJFX.updatePosCheck: posChanged "+posChanged+", ("+Thread.currentThread().getName()+"): newtChildReady "+newtChildReady+", "+clientArea.getX()+"/"+clientArea.getY()+" "+clientArea.getWidth()+"x"+clientArea.getHeight()+" - surfaceHandle 0x"+Long.toHexString(nsh));
        }
        if( posChanged ) {
            if( newtChildReady ) {
                newtChild.setPosition(clientArea.getX(), clientArea.getY());
            } else {
                postSetPos = true;
            }
        }
    }
    private final void updateSizeCheck(final int newWidth, final int newHeight) {
        final boolean sizeChanged;
        {
            final Rectangle oClientArea = clientArea;
            sizeChanged = newWidth != oClientArea.getWidth() || newHeight != oClientArea.getHeight();
            if( sizeChanged ) {
                clientArea.setWidth(newWidth);
                clientArea.setHeight(newHeight);
            }
        }
        if(DEBUG) {
            final long nsh = newtChildReady ? newtChild.getSurfaceHandle() : 0;
            System.err.println("NewtCanvasJFX.updateSizeCheck: sizeChanged "+sizeChanged+", ("+Thread.currentThread().getName()+"): newtChildReady "+newtChildReady+", "+clientArea.getX()+"/"+clientArea.getY()+" "+clientArea.getWidth()+"x"+clientArea.getHeight()+" - surfaceHandle 0x"+Long.toHexString(nsh));
        }
        if( sizeChanged ) {
            if( newtChildReady ) {
                newtChild.setSize(clientArea.getWidth(), clientArea.getHeight());
            } else {
                postSetSize = true;
            }
        }
    }

    private final ChangeListener<javafx.stage.Window> sceneWindowChangeListener = new ChangeListener<javafx.stage.Window>() {
            @Override public void changed(final ObservableValue<? extends javafx.stage.Window> observable, final javafx.stage.Window oldValue, final javafx.stage.Window newValue) {
                if( DEBUG ) {
                    System.err.println("NewtCanvasJFX.Event.Window, "+oldValue+" -> "+newValue);
                }
                if( updateParentWindowAndScreen() ) {
                    repaintAction(isVisible());
                }
            } };

    private boolean updateParentWindowAndScreen() {
        final Scene s = this.getScene();
        if( null != s ) {
            final javafx.stage.Window w = s.getWindow();
            if( DEBUG ) {
                System.err.println("NewtCanvasJFX.updateParentWindowAndScreen: Scene "+s+", Window "+w+" (showing "+(null!=w?w.isShowing():0)+")");
            }
            if( w != parentWindow ) {
                destroyImpl(false);
            }
            parentWindow = w;
            if( null != w ) {
                screen = JFXAccessor.getScreen(JFXAccessor.getDevice(parentWindow), -1 /* default */);
                parentWindow.addEventHandler(javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST, windowClosingListener);
                parentWindow.addEventHandler(javafx.stage.WindowEvent.WINDOW_SHOWN, windowShownListener);
                return true;
            } else {
                s.windowProperty().addListener(sceneWindowChangeListener);
            }
        } else {
            if( DEBUG ) {
                System.err.println("NewtCanvasJFX.updateParentWindowAndScreen: Null Scene");
            }
            if( null != parentWindow ) {
                destroyImpl(false);
            }
        }
        return false;
    }

    /**
     * Destroys this resource:
     * <ul>
     *   <li> Make the NEWT Child invisible </li>
     *   <li> Disconnects the NEWT Child from this Canvas NativeWindow, reparent to NULL </li>
     *   <li> Issues {@link Window#destroy()} on the NEWT Child</li>
     *   <li> Remove reference to the NEWT Child</li>
     * </ul>
     * JavaFX will issue this call when sending out the {@link javafx.stage.WindowEvent#WINDOW_CLOSE_REQUEST} automatically,
     * if the user has not overridden the default {@link WindowClosingMode#DISPOSE_ON_CLOSE} to {@link WindowClosingMode#DO_NOTHING_ON_CLOSE}
     * via {@link #setDefaultCloseOperation(com.jogamp.nativewindow.WindowClosingProtocol.WindowClosingMode)}.
     * @see Window#destroy()
     * @see #setDefaultCloseOperation(com.jogamp.nativewindow.WindowClosingProtocol.WindowClosingMode)
     */
    public void destroy() {
        destroyImpl(true);
    }
    private void destroyImpl(final boolean disposeNewtChild) {
        if(DEBUG) {
            System.err.println("NewtCanvasJFX.dispose: (has parent "+(null!=parentWindow)+", hasNative "+(null!=nativeWindow)+",\n\t"+newtChild);
        }
        if( null != newtChild ) {
            if(DEBUG) {
                System.err.println("NewtCanvasJFX.dispose.1: EDTUtil cur "+newtChild.getScreen().getDisplay().getEDTUtil());
            }
            if( null != nativeWindow ) {
                configureNewtChild(false);
                newtChild.setVisible(false);
                newtChild.reparentWindow(null, -1, -1, 0 /* hint */);
            }
            if( disposeNewtChild ) {
                newtChild.destroy();
                newtChild = null;
            }
        }
        if( null != parentWindow ) {
            parentWindow.getScene().windowProperty().removeListener(sceneWindowChangeListener);
            parentWindow.removeEventHandler(javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST, windowClosingListener);
            parentWindow.removeEventHandler(javafx.stage.WindowEvent.WINDOW_SHOWN, windowShownListener);
            parentWindow = null;
        }
        if( null != screen ) {
            screen.getDevice().close();
            screen = null;
        }
        nativeWindow = null;
    }

    private final boolean validateNative(final boolean completeReparent) {
        if( null != nativeWindow ) {
            return true; // already valid
        }
        if( null == parentWindow ) {
            return false;
        }
        updatePosSizeCheck();
        if(0 >= clientArea.getWidth() || 0 >= clientArea.getHeight()) {
            return false;
        }
        final long nativeWindowHandle = JFXAccessor.getWindowHandle(parentWindow);
        if( 0 == nativeWindowHandle ) {
            return false;
        }
        screen.getDevice().open();

        /* Native handle for the control, used to associate with GLContext */
        final int visualID = JFXAccessor.getNativeVisualID(screen.getDevice(), nativeWindowHandle);
        final boolean visualIDValid = NativeWindowFactory.isNativeVisualIDValidForProcessing(visualID);
        if(DEBUG) {
            System.err.println("NewtCanvasJFX.validateNative() windowHandle 0x"+Long.toHexString(nativeWindowHandle)+", visualID 0x"+Integer.toHexString(visualID)+", valid "+visualIDValid);
        }
        if( visualIDValid ) {
            /* Get the nativewindow-Graphics Device associated with this control (which is determined by the parent Composite).
             * Note: JFX is owner of the native handle, hence no closing operation will be a NOP. */
            final CapabilitiesImmutable caps = new Capabilities();
            final GraphicsConfigurationFactory factory = GraphicsConfigurationFactory.getFactory(screen.getDevice(), caps);
            final AbstractGraphicsConfiguration config = factory.chooseGraphicsConfiguration( caps, caps, null, screen, visualID );
            if(DEBUG) {
                System.err.println("NewtCanvasJFX.validateNative() factory: "+factory+", windowHandle 0x"+Long.toHexString(nativeWindowHandle)+", visualID 0x"+Integer.toHexString(visualID)+", chosen config: "+config);
                // Thread.dumpStack();
            }
            if (null == config) {
                throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
            }

            nativeWindow = new JFXNativeWindow(config, nativeWindowHandle);
            if( completeReparent ) {
                reparentWindow( true );
            }
        }

        return null != nativeWindow;
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
     * Note: The NEWT child {@link Display}'s {@link EDTUtil} is being set to an JFX conform implementation
     *       via {@link Display#setEDTUtil(EDTUtil)}.
     * </p>
     * @return the previous attached newt child.
     */
    public Window setNEWTChild(final Window newChild) {
        final Window prevChild = newtChild;
        if(DEBUG) {
            System.err.println("NewtCanvasJFX.setNEWTChild.0: win "+newtWinHandleToHexString(prevChild)+" -> "+newtWinHandleToHexString(newChild));
        }
        // remove old one
        if(null != newtChild) {
            reparentWindow( false );
            newtChild = null;
        }
        // add new one, reparent only if ready
        newtChild = newChild;
        if( null != newtChild && validateNative(false /* completeReparent */) ) {
            reparentWindow( true );
        }
        return prevChild;
    }

    private void reparentWindow(final boolean add) {
        if( null == newtChild ) {
            return; // nop
        }
        if(DEBUG) {
            System.err.println("NewtCanvasJFX.reparentWindow.0: add="+add+", win "+newtWinHandleToHexString(newtChild)+", EDTUtil: cur "+newtChild.getScreen().getDisplay().getEDTUtil());
        }

        newtChild.setFocusAction(null); // no AWT focus traversal ..
        if(add) {
            assert null != nativeWindow && null != parentWindow;
            updatePosSizeCheck();
            final int x = clientArea.getX();
            final int y = clientArea.getY();
            final int w = clientArea.getWidth();
            final int h = clientArea.getHeight();

            if(USE_JFX_EDT) {
                // setup JFX EDT and start it
                final Display newtDisplay = newtChild.getScreen().getDisplay();
                final EDTUtil oldEDTUtil = newtDisplay.getEDTUtil();
                if( ! ( oldEDTUtil instanceof JFXEDTUtil ) ) {
                    final EDTUtil newEDTUtil = new JFXEDTUtil(newtDisplay);
                    if(DEBUG) {
                        System.err.println("NewtCanvasJFX.reparentWindow.1: replacing EDTUtil "+oldEDTUtil+" -> "+newEDTUtil);
                    }
                    newEDTUtil.start();
                    newtDisplay.setEDTUtil( newEDTUtil );
                }
            }

            newtChild.setSize(w, h);
            newtChild.reparentWindow(nativeWindow, x, y, Window.REPARENT_HINT_BECOMES_VISIBLE);
            newtChild.setPosition(x, y);
            newtChild.setVisible(true);
            configureNewtChild(true);
            newtChild.sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED); // trigger a resize/relayout to listener

            // force this JFX Canvas to be focus-able,
            // since it is completely covered by the newtChild (z-order).
            // FIXME ??? super.requestFocus();
        } else {
            configureNewtChild(false);
            newtChild.setVisible(false);
            newtChild.reparentWindow(null, -1, -1, 0 /* hints */);
        }
        if(DEBUG) {
            System.err.println("NewtCanvasJFX.reparentWindow.X: add="+add+", win "+newtWinHandleToHexString(newtChild)+", EDTUtil: cur "+newtChild.getScreen().getDisplay().getEDTUtil());
        }
    }

    private void configureNewtChild(final boolean attach) {
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

    /** @return the current NEWT child */
    public Window getNEWTChild() {
        return newtChild;
    }

    /**
     * {@inheritDoc}
     * @return this JFX Canvas {@link NativeWindow} representation, may be null in case it has not been realized
     */
    @Override
    public NativeWindow getNativeWindow() { return nativeWindow; }

    /**
     * {@inheritDoc}
     * @return this JFX Canvas {@link NativeSurface} representation, may be null in case it has not been realized
     */
    @Override
    public NativeSurface getNativeSurface() { return nativeWindow; }

    @Override
    public WindowClosingMode getDefaultCloseOperation() {
        return closingMode;
    }

    @Override
    public WindowClosingMode setDefaultCloseOperation(final WindowClosingMode op) {
        final WindowClosingMode old = closingMode;
        closingMode = op;
        return old;
    }


    boolean isParent() {
        return null!=newtChild ;
    }

    boolean isFullscreen() {
        return null != newtChild && newtChild.isFullscreen();
    }

    private final void requestFocusNEWTChild() {
        if( newtChildReady ) {
            newtChild.setFocusAction(null);
            newtChild.requestFocus();
        }
    }

    @Override
    public void requestFocus() {
        NewtCanvasJFX.super.requestFocus();
        requestFocusNEWTChild();
    }

    private class JFXNativeWindow implements NativeWindow {
        private final AbstractGraphicsConfiguration config;
        private final long nativeWindowHandle;
        private final InsetsImmutable insets; // only required to allow proper client position calculation on OSX

        public JFXNativeWindow(final AbstractGraphicsConfiguration config, final long nativeWindowHandle) {
            this.config = config;
            this.nativeWindowHandle = nativeWindowHandle;
            this.insets = new Insets(0, 0, 0, 0);
        }

        @Override
        public RecursiveLock getLock() { return null; }

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
            return clientArea.getWidth();
        }

        @Override
        public int getSurfaceHeight() {
            return clientArea.getHeight();
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
            return NewtCanvasJFX.this.clientArea.getX();
        }

        @Override
        public int getY() {
            return NewtCanvasJFX.this.clientArea.getY();
        }

        @Override
        public Point getLocationOnScreen(final Point point) {
            final Point los = NativeWindowFactory.getLocationOnScreen(this); // client window location on screen
            if(null!=point) {
              return point.translate(los);
            } else {
              return los;
            }
        }

        @Override
        public boolean hasFocus() {
            return isFocused();
        }
    };

    static String newtWinHandleToHexString(final Window w) {
        return null != w ? toHexString(w.getWindowHandle()) : "nil";
    }
    static String toHexString(final long l) {
        return "0x"+Long.toHexString(l);
    }
}

