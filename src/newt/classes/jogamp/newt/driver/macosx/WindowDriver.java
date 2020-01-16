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

package jogamp.newt.driver.macosx;

import com.jogamp.common.util.InterruptSource;
import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.GraphicsConfigurationFactory;
import com.jogamp.nativewindow.NativeWindow;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.OffscreenLayerOption;
import com.jogamp.nativewindow.MutableSurface;
import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.nativewindow.VisualIDHolder;
import com.jogamp.nativewindow.util.Point;
import com.jogamp.nativewindow.util.PointImmutable;
import com.jogamp.nativewindow.util.RectangleImmutable;

import jogamp.nativewindow.SurfaceScaleUtils;
import jogamp.nativewindow.macosx.OSXUtil;
import jogamp.newt.PointerIconImpl;
import jogamp.newt.ScreenImpl;
import jogamp.newt.WindowImpl;
import jogamp.newt.driver.DriverClearFocus;
import jogamp.newt.driver.DriverUpdatePosition;

import com.jogamp.newt.Screen;
import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MonitorEvent;
import com.jogamp.opengl.math.FloatUtil;

public class WindowDriver extends WindowImpl implements MutableSurface, DriverClearFocus, DriverUpdatePosition {

    static {
        DisplayDriver.initSingleton();
    }

    public WindowDriver() {
    }

    private boolean updatePixelScale(final boolean sendEvent, final boolean defer, final boolean deferOffThread,
                                     final float newPixelScaleRaw, final float maxPixelScaleRaw) {
        final float[] newPixelScale = new float[2];
        {
            final float _newPixelScale = FloatUtil.isZero(newPixelScaleRaw, FloatUtil.EPSILON) ? ScalableSurface.IDENTITY_PIXELSCALE : newPixelScaleRaw;
            newPixelScale[0]= _newPixelScale;
            newPixelScale[1]= _newPixelScale;
            final float _maxPixelScale = FloatUtil.isZero(maxPixelScaleRaw, FloatUtil.EPSILON) ? ScalableSurface.IDENTITY_PIXELSCALE : maxPixelScaleRaw;
            maxPixelScale[0]= _maxPixelScale;
            maxPixelScale[1]= _maxPixelScale;
        }
        // We keep minPixelScale at [1f, 1f]!

        if( SurfaceScaleUtils.setNewPixelScale(hasPixelScale, hasPixelScale, newPixelScale, minPixelScale, maxPixelScale, DEBUG_IMPLEMENTATION ? getClass().getName() : null) ) {
            if( sendEvent ) {
                if( defer && deferOffThread ) {
                    superSizeChangedOffThread(defer, getWidth(), getHeight(), true);
                } else {
                    super.sizeChanged(defer, getWidth(), getHeight(), true);
                }
            } else {
                defineSize(getWidth(), getHeight());
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Essentially updates {@code maxPixelScale} ..
     */
    private boolean updateMaxScreenPixelScaleByDisplayID(final boolean sendEvent) {
        final float maxPixelScaleRaw = OSXUtil.GetScreenPixelScaleByDisplayID(getDisplayID());
        if( DEBUG_IMPLEMENTATION ) {
            System.err.println("WindowDriver.updatePixelScale.1: req "+reqPixelScale[0]+", has "+hasPixelScale[0]+", max "+maxPixelScaleRaw);
        }
        return updatePixelScale(sendEvent, true /* defer */, false /*offthread */, hasPixelScale[0], maxPixelScaleRaw);
    }

    /**
     * Essentially updates {@code hasPixelScale} and {@code maxPixelScale} ..
     */
    private boolean updateMaxScreenPixelScaleByWindowHandle(final boolean sendEvent) {
        final long handle = getWindowHandle();
        if( 0 != handle ) {
            final float maxPixelScaleRaw = OSXUtil.GetScreenPixelScale(handle);
            final float newPixelScaleRaw = OSXUtil.GetWindowPixelScale(handle);
            if( DEBUG_IMPLEMENTATION ) {
                System.err.println("WindowDriver.updatePixelScale.2: req "+reqPixelScale[0]+", has "+hasPixelScale[0]+", new "+newPixelScaleRaw+", max "+maxPixelScaleRaw);
            }
            return updatePixelScale(sendEvent, true /* defer */, false /*offthread */, newPixelScaleRaw, maxPixelScaleRaw);
        } else {
            return false;
        }
    }

    /** Called from native code */
    protected void updatePixelScale(final boolean defer, final float oldPixelScaleRaw, final float newPixelScaleRaw, final float maxPixelScaleRaw, final boolean changed) {
        if( DEBUG_IMPLEMENTATION ) {
            System.err.println("WindowDriver.updatePixelScale.3: req "+reqPixelScale[0]+", has "+hasPixelScale[0]+", old "+oldPixelScaleRaw+", new "+newPixelScaleRaw+", max "+maxPixelScaleRaw+", changed "+changed);
        }
        updatePixelScale(true /* sendEvent*/, defer, true /*offthread */, newPixelScaleRaw, maxPixelScaleRaw);
    }

    @Override
    protected final void instantiationFinishedImpl() {
        updateMaxScreenPixelScaleByDisplayID(false /* sendEvent*/);
    }

    @Override
    protected void setScreen(final ScreenImpl newScreen) { // never null !
        super.setScreen(newScreen);
        updateMaxScreenPixelScaleByDisplayID(false /* sendEvent*/);  // caller (reparent, ..) will send reshape event
    }

    @Override
    protected void monitorModeChanged(final MonitorEvent me, final boolean success) {
        updateMaxScreenPixelScaleByWindowHandle(false /* sendEvent*/); // send reshape event itself
    }

    @Override
    public final boolean setSurfaceScale(final float[] pixelScale) {
        super.setSurfaceScale(pixelScale);

        boolean changed = false;
        if( isNativeValid() ) {
            if( isOffscreenInstance ) {
                final NativeWindow pWin = getParent();
                if( pWin instanceof ScalableSurface ) {
                    final ScalableSurface sSurf = (ScalableSurface)pWin;
                    sSurf.setSurfaceScale(reqPixelScale);
                    sSurf.getMaximumSurfaceScale(maxPixelScale);
                    sSurf.getMinimumSurfaceScale(minPixelScale);
                    final float[] pPixelScale = sSurf.getCurrentSurfaceScale(new float[2]);
                    changed = updatePixelScale(true /* sendEvent */, true /* defer */, true /*offthread */, pPixelScale[0], maxPixelScale[0]); // HiDPI: uniformPixelScale
                } else {
                    // just notify updated pixelScale if offscreen
                    changed = updatePixelScale(true /* sendEvent */, true /* defer */, true /*offthread */, reqPixelScale[0], maxPixelScale[0]); // HiDPI: uniformPixelScale
                }
            } else {
                // setPixelScale0(..) _should_ issue an update updatePixelScale(..) via NSView::viewDidChangeBackingProperties
                // We pre-query whether pixel-scale will change, without affecting current state 'hasPixelScale',
                // but to issue setPixelScale0(..) only when required.
                final float[] _hasPixelScale = new float[2];
                System.arraycopy(hasPixelScale, 0, _hasPixelScale, 0, 2);
                if( SurfaceScaleUtils.setNewPixelScale(_hasPixelScale, _hasPixelScale, reqPixelScale, minPixelScale, maxPixelScale, DEBUG_IMPLEMENTATION ? getClass().getName() : null) ) {
                    OSXUtil.RunOnMainThread(true, false, new Runnable() {
                        @Override
                        public void run() {
                            final long windowHandle = getWindowHandle();
                            setPixelScale0(windowHandle, surfaceHandle, _hasPixelScale[0]); // HiDPI: uniformPixelScale
                            // For some reason NSView::viewDidChangeBackingProperties won't get called on child windows after creation
                            // hence explicitly call it here.
                            final float newPixelScaleRaw = OSXUtil.GetWindowPixelScale(windowHandle);
                            updatePixelScale(true /* sendEvent */, true /* defer */, false /*offthread */, newPixelScaleRaw, maxPixelScale[0]);
                        }
                    } );
                    changed = true;
                }
            }
        }
        if( DEBUG_IMPLEMENTATION ) {
            System.err.println("WindowDriver.setPixelScale: min["+minPixelScale[0]+", "+minPixelScale[1]+"], max["+
                                maxPixelScale[0]+", "+maxPixelScale[1]+"], req["+
                                reqPixelScale[0]+", "+reqPixelScale[1]+"] -> result["+
                                hasPixelScale[0]+", "+hasPixelScale[1]+"] - changed "+changed+", realized "+isNativeValid());
        }
        return changed;
    }

    @Override
    protected void createNativeImpl() {
        final AbstractGraphicsConfiguration cfg = GraphicsConfigurationFactory.getFactory(getScreen().getDisplay().getGraphicsDevice(), capsRequested).chooseGraphicsConfiguration(
                capsRequested, capsRequested, capabilitiesChooser, getScreen().getGraphicsScreen(), VisualIDHolder.VID_UNDEFINED);
        if (null == cfg) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
        setGraphicsConfiguration(cfg);
        reconfigureWindowImpl(getX(), getY(), getWidth(), getHeight(), getReconfigureMask(CHANGE_MASK_VISIBILITY, true));
        if ( !isNativeValid() ) {
            throw new NativeWindowException("Error creating window");
        }
    }

    @Override
    protected void closeNativeImpl() {
        try {
            if(DEBUG_IMPLEMENTATION) { System.err.println("MacWindow.CloseAction "+Thread.currentThread().getName()); }
            final long handle = getWindowHandle();
            visibleChanged(false);
            setWindowHandle(0);
            surfaceHandle = 0;
            sscSurfaceHandle = 0;
            isOffscreenInstance = false;
            resizeAnimatorPaused = false;
            if (0 != handle) {
                OSXUtil.RunOnMainThread(false, true /* kickNSApp */, new Runnable() {
                   @Override
                   public void run() {
                       close0( handle );
                   } });
            }
        } catch (final Throwable t) {
            if(DEBUG_IMPLEMENTATION) {
                final Exception e = new Exception("Warning: closeNative failed - "+Thread.currentThread().getName(), t);
                e.printStackTrace();
            }
        }
    }

    @Override
    protected int lockSurfaceImpl() {
        /**
         * if( isOffscreenInstance ) {
         *    return LOCK_SUCCESS;
         * }
         */
        final long w = getWindowHandle();
        final long v = surfaceHandle;
        if( 0 != v && 0 != w ) {
            return lockSurface0(w, v) ? LOCK_SUCCESS : LOCK_SURFACE_NOT_READY;
        }
        return LOCK_SURFACE_NOT_READY;
    }

    @Override
    protected void unlockSurfaceImpl() {
        /**
         * if( isOffscreenInstance ) {
         *    return;
         * }
         */
        final long w = getWindowHandle();
        final long v = surfaceHandle;
        if(0 != w && 0 != v) {
            if( !unlockSurface0(w, v) ) {
                throw new NativeWindowException("Failed to unlock surface, probably not locked!");
            }
        }
    }

    @Override
    public final long getSurfaceHandle() {
        return 0 != sscSurfaceHandle ? sscSurfaceHandle : surfaceHandle;
    }

    @Override
    public void setSurfaceHandle(final long surfaceHandle) {
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("MacWindow.setSurfaceHandle(): 0x"+Long.toHexString(surfaceHandle));
        }
        sscSurfaceHandle = surfaceHandle;
        if ( isNativeValid() ) {
            if (0 != sscSurfaceHandle) {
                OSXUtil.RunOnMainThread(false, false, new Runnable() {
                        @Override
                        public void run() {
                            orderOut0( 0 != getParentWindowHandle() ? getParentWindowHandle() : getWindowHandle() );
                        } } );
            } /** this is done by recreation!
              else if (isVisible()){
                OSXUtil.RunOnMainThread(false, new Runnable() {
                    public void run() {
                        orderFront0( 0!=getParentWindowHandle() ? getParentWindowHandle() : getWindowHandle() );
                    } } );
            } */
        }
    }

    @Override
    protected void setTitleImpl(final String title) {
        OSXUtil.RunOnMainThread(false, false, new Runnable() {
                @Override
                public void run() {
                    setTitle0(getWindowHandle(), title);
                } } );
    }

    @Override
    protected void requestFocusImpl(final boolean force) {
        final boolean _isFullscreen = isFullscreen();
        final boolean _isOffscreenInstance = isOffscreenInstance;
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("MacWindow: requestFocusImpl(), isOffscreenInstance "+_isOffscreenInstance+", isFullscreen "+_isFullscreen);
        }
        if(!_isOffscreenInstance) {
            OSXUtil.RunOnMainThread(false, false, new Runnable() {
                    @Override
                    public void run() {
                        requestFocus0(getWindowHandle(), force);
                        if(_isFullscreen) {
                            // 'NewtMacWindow::windowDidBecomeKey()' is not always called in fullscreen-mode!
                            focusChanged(false, true);
                        }
                    } } );
        } else {
            focusChanged(false, true);
        }
    }

    @Override
    public final void clearFocus() {
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("MacWindow: clearFocus(), isOffscreenInstance "+isOffscreenInstance);
        }
        if(!isOffscreenInstance) {
            OSXUtil.RunOnMainThread(false, false, new Runnable() {
                    @Override
                    public void run() {
                        resignFocus0(getWindowHandle());
                    } } );
        } else {
            focusChanged(false, false);
        }
    }

    /**
     * Returns {@code true} if not {@link #isFullscreen()} and if given {@code parent} is valid and created.
     */
    private boolean useParentLocation(final NativeWindow parent) {
        return !isFullscreen() && null != parent && 0 != parent.getWindowHandle();
    }

    @Override
    public void updatePosition(final int x, final int y) {
        final long handle = getWindowHandle();
        if( 0 != handle && !isOffscreenInstance ) {
            final NativeWindow parent = getParent();
            final boolean useParentLoc = useParentLocation(parent);
            final Point p0S;
            if( useParentLoc ) {
                p0S = getLocationOnScreenByParent(x, y, parent);
            } else {
                p0S = (Point) getLocationOnScreen0(handle, x, y);
            }
            if(DEBUG_IMPLEMENTATION) {
                final int pX=parent.getX(), pY=parent.getY();
                System.err.println("MacWindow: updatePosition() parent["+useParentLoc+" "+pX+"/"+pY+"] "+x+"/"+y+" ->  "+x+"/"+y+" rel-client-pos, "+p0S+" screen-client-pos");
            }
            OSXUtil.RunOnMainThread(false, false, new Runnable() {
                    @Override
                    public void run() {
                        setWindowClientTopLeftPoint0(getWindowHandle(), p0S.getX(), p0S.getY(), isVisible());
                    } } );
            // no native event (fullscreen, some reparenting)
            positionChanged(true, x, y);
        }
    }

    @Override
    protected final int getSupportedReconfigMaskImpl() {
        return mutableSizePosReconfigStateMask |
               STATE_MASK_CHILDWIN |
               STATE_MASK_UNDECORATED |
               STATE_MASK_ALWAYSONTOP |
               STATE_MASK_ALWAYSONBOTTOM |
               STATE_MASK_STICKY |
               STATE_MASK_MAXIMIZED_VERT |
               STATE_MASK_MAXIMIZED_HORZ |
               STATE_MASK_POINTERVISIBLE |
               STATE_MASK_POINTERCONFINED;
    }

    @Override
    protected boolean reconfigureWindowImpl(int _x, int _y, int _width, int _height, final int flags) {
        final NativeWindow parent = getParent();
        final boolean useParentLoc = useParentLocation(parent);
        final boolean _isOffscreenInstance = isOffscreenInstance(this, parent);
        isOffscreenInstance = 0 != sscSurfaceHandle || _isOffscreenInstance;
        final PointImmutable pClientLevelOnSreen;
        if( isOffscreenInstance ) {
            _x = 0; _y = 0;
            pClientLevelOnSreen = new Point(0, 0);
        } else  {
            if( useParentLoc ) {
                pClientLevelOnSreen = getLocationOnScreenByParent(_x, _y, parent);
            } else {
                if( 0 != ( ( CHANGE_MASK_MAXIMIZED_HORZ | CHANGE_MASK_MAXIMIZED_VERT ) & flags ) ) {
                    final int[] posSize = { _x, _y, _width, _height };
                    reconfigMaximizedManual(flags, posSize, getInsets());
                    _x = posSize[0];
                    _y = posSize[1];
                    _width = posSize[2];
                    _height = posSize[3];
                }
                pClientLevelOnSreen = new Point(_x, _y);
            }
        }
        final int x=_x, y=_y;
        final int width=_width, height=_height;

        final boolean hasFocus = hasFocus();

        if(DEBUG_IMPLEMENTATION) {
            final AbstractGraphicsConfiguration cWinCfg = this.getGraphicsConfiguration();
            final AbstractGraphicsConfiguration pWinCfg = null != parent ? parent.getGraphicsConfiguration() : null;
            System.err.println("MacWindow reconfig.0: "+x+"/"+y+" -> clientPosOnScreen "+pClientLevelOnSreen+" - "+width+"x"+height+
                               ", "+getReconfigStateMaskString(flags)+
                               ",\n\t useParent "+useParentLoc+", parent type "+(null != parent ? parent.getClass().getName() : null)+
                               ",\n\t   this-chosenCaps "+(null != cWinCfg ? cWinCfg.getChosenCapabilities() : null)+
                               ",\n\t parent-chosenCaps "+(null != pWinCfg ? pWinCfg.getChosenCapabilities() : null)+
                               ", isOffscreenInstance(sscSurfaceHandle "+toHexString(sscSurfaceHandle)+
                               ", ioi: "+_isOffscreenInstance+
                               ") -> "+isOffscreenInstance);
            // Thread.dumpStack();
        }

        final long oldWindowHandle = getWindowHandle();
        if( 0 != ( CHANGE_MASK_VISIBILITY & flags) &&
            0 == ( STATE_MASK_VISIBLE & flags) )
        {
            if ( 0 != oldWindowHandle && !isOffscreenInstance ) {
                OSXUtil.RunOnMainThread(false, false, new Runnable() {
                        @Override
                        public void run() {
                            orderOut0(oldWindowHandle);
                            visibleChanged(false);
                        } } );
            } else {
                visibleChanged(false);
            }
        }
        if( ( 0 == oldWindowHandle && 0 != ( STATE_MASK_VISIBLE & flags) ) ||
            0 != ( CHANGE_MASK_PARENTING & flags)  ||
            0 != ( CHANGE_MASK_DECORATION & flags) ||
            0 != ( CHANGE_MASK_ALWAYSONTOP & flags) ||
            0 != ( CHANGE_MASK_ALWAYSONBOTTOM & flags) ||
            0 != ( CHANGE_MASK_RESIZABLE & flags)  ||
            0 != ( CHANGE_MASK_FULLSCREEN & flags) ) {
            if(isOffscreenInstance) {
                createWindow(true, 0 != oldWindowHandle, pClientLevelOnSreen, 64, 64, flags);
            } else {
                createWindow(false, 0 != oldWindowHandle, pClientLevelOnSreen, width, height, flags);
            }
            // no native event (fullscreen, some reparenting)
            updateMaxScreenPixelScaleByWindowHandle(false /* sendEvent */);
            if( isOffscreenInstance ) {
                super.sizeChanged(false, width, height, true);
                positionChanged(false,  x, y);
            } else {
                OSXUtil.RunOnMainThread(true, false, new Runnable() {
                    @Override
                    public void run() {
                        updateSizePosInsets0(getWindowHandle(), false);  // calls: sizeScreenPosInsetsChanged(..)
                    } } );
            }
            visibleChanged(0 != ( STATE_MASK_VISIBLE & flags));
            if( hasFocus ) {
                requestFocusImpl(true);
            }
        } else if( 0 != oldWindowHandle ) {
            if( width>0 && height>0 ) {
                if( !isOffscreenInstance ) {
                    OSXUtil.RunOnMainThread(true, false, new Runnable() {
                        @Override
                        public void run() {
                            if( useParentLoc && 0 == ( STATE_MASK_VISIBLE & flags) ) {
                                // Fake invisible child window: We can't use true orderOut
                                // Ensure it stays out of sight by moving it to 2x width/height of viewport.
                                final RectangleImmutable r = getScreen().getViewportInWindowUnits();
                                setWindowClientTopLeftPointAndSize0(oldWindowHandle,
                                        r.getX()+2*r.getWidth(), r.getY()+2*r.getHeight(),
                                        width, height, false /* no display */);
                            } else {
                                // Normal visibility
                                setWindowClientTopLeftPointAndSize0(oldWindowHandle,
                                        pClientLevelOnSreen.getX(), pClientLevelOnSreen.getY(),
                                        width, height, 0 != ( STATE_MASK_VISIBLE & flags));
                                updateSizePosInsets0(oldWindowHandle, false); // calls: sizeScreenPosInsetsChanged(..)
                            }
                        } } );
                } else { // else offscreen size is realized via recreation
                    // no native event (fullscreen, some reparenting)
                    super.sizeChanged(false, width, height, false);
                    positionChanged(false,  x, y);
                }
            }
            if( 0 != ( CHANGE_MASK_VISIBILITY & flags) &&
                0 != ( STATE_MASK_VISIBLE & flags) )
            {
                if( !isOffscreenInstance ) {
                    OSXUtil.RunOnMainThread(false, false, new Runnable() {
                            @Override
                            public void run() {
                                orderFront0(getWindowHandle());
                                visibleChanged(true);
                            } } );
                } else {
                    visibleChanged(true);
                }
            }
        } else {
            throw new InternalError("Null windowHandle but no re-creation triggered, check visibility: "+getStateMaskString());
        }
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("MacWindow reconfig.X: "+getLocationOnScreenImpl(0, 0)+" "+getWidth()+"x"+getHeight()+", insets "+getInsets()+", "+getStateMaskString());
        }
        return true;
    }

    @Override
    protected Point getLocationOnScreenImpl(final int x, final int y) {
        final NativeWindow parent = getParent();
        if( useParentLocation(parent) ) {
            return getLocationOnScreenByParent(x, y, parent);
        } else {
            final long windowHandle = getWindowHandle();
            if( !isOffscreenInstance && 0 != windowHandle ) {
                return (Point) getLocationOnScreen0(windowHandle, x, y);
            } else {
                return new Point(x, y);
            }
        }
    }

    private Point getLocationOnScreenByParent(final int x, final int y, final NativeWindow parent) {
        // return new Point(x, y).translate( parent.getLocationOnScreen(null) ); // Bug 1393: deadlock AppKit + EDT

        // If parent is JAWT on OSX (offscreen), we need to query its non-blocking position
        if( parent instanceof OffscreenLayerOption && ((OffscreenLayerOption)parent).isOffscreenLayerSurfaceEnabled() ) {
            return new Point(x, y).translate( parent.getLocationOnScreen(null) ); // uses non-blocking specialization
        }
        return new Point(x, y).translate( OSXUtil.GetLocationOnScreen(parent.getWindowHandle(), 0, 0) ); // non-blocking
    }

    /** Callback for native screen position change event of the client area. */
    protected void screenPositionChanged(final boolean defer, final int newX, final int newY) {
        // passed coordinates are in screen position of the client area
        if( isNativeValid() ) {
            final NativeWindow parent = getParent();
            final boolean useParentLoc = useParentLocation(parent);
            if( !useParentLoc || isOffscreenInstance ) {
                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("MacWindow.positionChanged.0 (Screen Pos - TOP): ("+getThreadName()+"): (defer: "+defer+") "+getX()+"/"+getY()+" -> "+newX+"/"+newY);
                }
                positionChanged(defer, newX, newY);
            } else if( useParentLoc && !isVisible() ) {
                // Fake invisible child window: drop fake position update for fake invisibility
                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("MacWindow.positionChanged.1 (Screen Pos - invisible CHILD): ("+getThreadName()+"): (defer: "+defer+") "+getX()+"/"+getY()+", ignoring absPos "+newX+"/"+newY);
                }
            } else {
                // visible childWindow or offscreen instance
                final Runnable action = new Runnable() {
                    public void run() {
                        // screen position -> rel child window position
                        final Point absPos = new Point(newX, newY);
                        // final Point parentOnScreen = parent.getLocationOnScreen(null); // Bug 1393: deadlock AppKit + EDT
                        final Point parentOnScreen;
                        if( parent instanceof OffscreenLayerOption && ((OffscreenLayerOption)parent).isOffscreenLayerSurfaceEnabled() ) {
                            // If parent is JAWT on OSX (offscreen), we need to query its non-blocking position
                            parentOnScreen = parent.getLocationOnScreen(null); // uses non-blocking specialization
                        } else {
                            parentOnScreen = OSXUtil.GetLocationOnScreen(parent.getWindowHandle(), 0, 0); // non-blocking
                        }
                        absPos.translate( parentOnScreen.scale(-1, -1) );
                        if(DEBUG_IMPLEMENTATION) {
                            System.err.println("MacWindow.positionChanged.1 (Screen Pos - CHILD): ("+getThreadName()+"): (defer: "+defer+") "+getX()+"/"+getY()+" -> absPos "+newX+"/"+newY+", parentOnScreen "+parentOnScreen+" -> "+absPos);
                        }
                        positionChanged(false, absPos.getX(), absPos.getY());
                    } };
                if( defer ) {
                    new InterruptSource.Thread(null, action).start();
                } else {
                    action.run();
                }

            }
        } else if(DEBUG_IMPLEMENTATION) {
            System.err.println("MacWindow.positionChanged.2 (Screen Pos - IGN): ("+getThreadName()+"): (defer: "+defer+") "+getX()+"/"+getY()+" -> "+newX+"/"+newY);
        }
    }

    @Override
    protected void sizeChanged(final boolean defer, final int newWidth, final int newHeight, final boolean force) {
        if(force || getWidth() != newWidth || getHeight() != newHeight) {
            if( isNativeValid() && isVisible() && !isOffscreenInstance ) {
                final NativeWindow parent = getParent();
                final boolean useParentLoc = useParentLocation(parent);
                if( useParentLoc ) {
                    // Visible child-windows: Reset position
                    // Fake invisible child window: These are ignored
                    final int x=getX(), y=getY();
                    final Point p0S = getLocationOnScreenByParent(x, y, parent);
                    if(DEBUG_IMPLEMENTATION) {
                        System.err.println("MacWindow: sizeChanged() parent["+useParentLoc+" "+x+"/"+y+"] "+getX()+"/"+getY()+" "+newWidth+"x"+newHeight+" ->  "+p0S+" screen-client-pos");
                    }
                    OSXUtil.RunOnMainThread(false, false, new Runnable() {
                        @Override
                        public void run() {
                            setWindowClientTopLeftPoint0(getWindowHandle(), p0S.getX(), p0S.getY(), isVisible());
                        } } );
                }
            }
            superSizeChangedOffThread(defer, newWidth, newHeight, force);
        }
    }
    private void superSizeChangedOffThread(final boolean defer, final int newWidth, final int newHeight, final boolean force) {
        if( defer ) {
            new InterruptSource.Thread() {
                public void run() {
                    WindowDriver.super.sizeChanged(false /* defer */, newWidth, newHeight, force);
                } }.start();
        } else {
            WindowDriver.super.sizeChanged(false /* defer */, newWidth, newHeight, force);
        }
    }

    //
    // Accumulated actions
    //

    /** Triggered by implementation's WM events to update the client-area position, size and insets. */
    protected void sizeScreenPosInsetsChanged(final boolean defer,
                                              final int newX, final int newY,
                                              final int newWidth, final int newHeight,
                                              final int left, final int right, final int top, final int bottom,
                                              final boolean force,
                                              final boolean withinLiveResize) {
        final LifecycleHook lh = getLifecycleHook();
        if( withinLiveResize && !resizeAnimatorPaused && null!=lh ) {
            resizeAnimatorPaused = lh.pauseRenderingAction();
        }
        sizeChanged(defer, newWidth, newHeight, force);
        screenPositionChanged(defer, newX, newY);
        insetsChanged(left, right, top, bottom);
        if( !withinLiveResize && resizeAnimatorPaused ) {
            resizeAnimatorPaused = false;
            if( null!=lh ) {
                lh.resumeRenderingAction();
            }
        }
    }

    @Override
    protected void setPointerIconImpl(final PointerIconImpl pi) {
        if( !isOffscreenInstance ) {
            final long piHandle = null != pi ? pi.validatedHandle() : 0;
            OSXUtil.RunOnMainThread(false, false, new Runnable() {
                    @Override
                    public void run() {
                        setPointerIcon0(getWindowHandle(), piHandle);
                    } } );
        }
    }

    @Override
    protected boolean setPointerVisibleImpl(final boolean pointerVisible) {
        if( !isOffscreenInstance ) {
            OSXUtil.RunOnMainThread(false, false, new Runnable() {
                    @Override
                    public void run() {
                        setPointerVisible0(getWindowHandle(), hasFocus(), pointerVisible);
                    } } );
            return true;
        }
        return false;
    }

    @Override
    protected boolean confinePointerImpl(final boolean confine) {
        if( !isOffscreenInstance ) {
            confinePointer0(getWindowHandle(), confine);
            return true;
        } // else may need offscreen solution ? FIXME
        return false;
    }

    @Override
    protected void warpPointerImpl(final int x, final int y) {
        if( !isOffscreenInstance ) {
            warpPointer0(getWindowHandle(),
                         SurfaceScaleUtils.scaleInv(x, getPixelScaleX()),
                         SurfaceScaleUtils.scaleInv(y, getPixelScaleY()));
        } // else may need offscreen solution ? FIXME
    }

    @Override
    protected final void doMouseEvent(final boolean enqueue, final boolean wait, final short eventType, final int modifiers,
                                      final int x, final int y, final short button, final float[] rotationXYZ, final float rotationScale) {
        super.doMouseEvent(enqueue, wait, eventType, modifiers,
                           SurfaceScaleUtils.scale(x, getPixelScaleX()),
                           SurfaceScaleUtils.scale(y, getPixelScaleY()), button, rotationXYZ, rotationScale);
    }

    @Override
    public final void sendKeyEvent(final short eventType, final int modifiers, final short keyCode, final short keySym, final char keyChar) {
        throw new InternalError("XXX: Adapt Java Code to Native Code Changes");
    }

    @Override
    public final void enqueueKeyEvent(final boolean wait, final short eventType, final int modifiers, final short _keyCode, final short _keySym, final char keyChar) {
        throw new InternalError("XXX: Adapt Java Code to Native Code Changes");
    }

    protected final void enqueueKeyEvent(final boolean wait, final short eventType, int modifiers, final short _keyCode, final char keyChar, final char keySymChar) {
        // Note that we send the key char for the key code on this
        // platform -- we do not get any useful key codes out of the system
        final short keyCode = MacKeyUtil.validateKeyCode(_keyCode, keyChar);
        final short keySym;
        {
            final short _keySym = KeyEvent.NULL_CHAR != keySymChar ? KeyEvent.utf16ToVKey(keySymChar) : KeyEvent.VK_UNDEFINED;
            keySym = KeyEvent.VK_UNDEFINED != _keySym ? _keySym : keyCode;
        }
        /**
        {
            final boolean isModifierKeyCode = KeyEvent.isModifierKey(keyCode);
            System.err.println("*** handleKeyEvent: event "+KeyEvent.getEventTypeString(eventType)+
                               ", keyCode 0x"+Integer.toHexString(_keyCode)+" -> 0x"+Integer.toHexString(keyCode)+
                               ", keySymChar '"+keySymChar+"', 0x"+Integer.toHexString(keySymChar)+" -> 0x"+Integer.toHexString(keySym)+
                               ", mods "+toHexString(modifiers)+
                               ", was: pressed "+isKeyPressed(keyCode)+", isModifierKeyCode "+isModifierKeyCode+
                               ", nativeValid "+isNativeValid()+", isOffscreen "+isOffscreenInstance);
        } */

        // OSX delivery order is PRESSED (t0), RELEASED (t1) and TYPED (t2) -> NEWT order: PRESSED (t0) and RELEASED (t1)
        // Auto-Repeat: OSX delivers only PRESSED, inject auto-repeat RELEASE key _before_ PRESSED
        switch(eventType) {
            case KeyEvent.EVENT_KEY_RELEASED:
                if( isKeyCodeTracked(keyCode) ) {
                    setKeyPressed(keyCode, false);
                }
                break;
            case KeyEvent.EVENT_KEY_PRESSED:
                if( isKeyCodeTracked(keyCode) ) {
                    if( setKeyPressed(keyCode, true) ) {
                        // key was already pressed
                        modifiers |= InputEvent.AUTOREPEAT_MASK;
                        super.enqueueKeyEvent(wait, KeyEvent.EVENT_KEY_RELEASED, modifiers, keyCode, keySym, keyChar); // RELEASED
                    }
                }
                break;
        }
        super.enqueueKeyEvent(wait, eventType, modifiers, keyCode, keySym, keyChar);
    }

    protected int getDisplayID() {
        if( !isOffscreenInstance ) {
            return getDisplayID0(getWindowHandle());
        }
        return 0;
    }

    //----------------------------------------------------------------------
    // Internals only
    //

    private void createWindow(final boolean offscreenInstance, final boolean recreate,
                              final PointImmutable pS, final int width, final int height,
                              final int flags)
    {
        final long parentWinHandle = getParentWindowHandle();
        final long oldWinHandle = getWindowHandle();

        if(DEBUG_IMPLEMENTATION) {
            System.err.println("MacWindow.createWindow on thread "+Thread.currentThread().getName()+
                               ": offscreen "+offscreenInstance+", recreate "+recreate+
                               ", pS "+pS+", "+width+"x"+height+", reqPixelScale "+reqPixelScale[0]+", state "+getReconfigStateMaskString(flags)+
                               ", preWinHandle "+toHexString(oldWinHandle)+", parentWin "+toHexString(parentWinHandle)+
                               ", surfaceHandle "+toHexString(surfaceHandle));
            // Thread.dumpStack();
        }

        try {
            if( 0 != oldWinHandle ) {
                setWindowHandle(0);
                if( 0 == surfaceHandle ) {
                    throw new NativeWindowException("Internal Error - create w/ window, but no Newt NSView");
                }
            } else {
                if( 0 != surfaceHandle ) {
                    throw new NativeWindowException("Internal Error - create w/o window, but has Newt NSView");
                }
            }

            final int windowStyle;
            {
                int ws = 0;
                if( 0 != ( STATE_MASK_UNDECORATED & flags) || offscreenInstance ) {
                    ws = NSBorderlessWindowMask;
                } else {
                    ws = NSTitledWindowMask|NSClosableWindowMask|NSMiniaturizableWindowMask;
                    if( 0 != ( STATE_MASK_RESIZABLE & flags) ) {
                        ws |= NSResizableWindowMask;
                    }
                }
                windowStyle = ws;
            }
            // Blocking initialization on main-thread!
            final long[] newWin = { 0 };
            OSXUtil.RunOnMainThread(true, false /* kickNSApp */, new Runnable() {
                    @Override
                    public void run() {
                        /**
                         * Does everything at once, same as original code path:
                         * 1) if oldWinHandle: changeContentView (detaching view) + close0(oldWindHandle)
                         * 2) create new window
                         * 3) create new view if previous didn't exist (oldWinHandle)
                         * 4) changeContentView (attaching view) etc ..
                         */
                        final boolean isOpaque = getGraphicsConfiguration().getChosenCapabilities().isBackgroundOpaque() && !offscreenInstance;
                        newWin[0] = createWindow1( oldWinHandle, parentWinHandle, pS.getX(), pS.getY(), width, height, reqPixelScale[0] /* HiDPI uniformPixelScale */,
                                                   0 != ( STATE_MASK_FULLSCREEN & flags),
                                                   windowStyle, NSBackingStoreBuffered,
                                                   isOpaque,
                                                   !offscreenInstance && 0 != ( STATE_MASK_ALWAYSONTOP & flags),
                                                   !offscreenInstance && 0 != ( STATE_MASK_ALWAYSONBOTTOM & flags),
                                                   !offscreenInstance && 0 != ( STATE_MASK_VISIBLE & flags),
                                                   surfaceHandle);
                        surfaceHandle = OSXUtil.GetNSView(newWin[0]);

                        if( offscreenInstance ) {
                            orderOut0(0!=parentWinHandle ? parentWinHandle : newWin[0]);
                        } else {
                            setTitle0(newWin[0], getTitle());
                        }
                    } });

            if ( newWin[0] == 0 || 0 == surfaceHandle ) {
                throw new NativeWindowException("Could not create native window "+Thread.currentThread().getName()+" "+this);
            }
            setWindowHandle( newWin[0] );
        } catch (final Exception ie) {
            ie.printStackTrace();
        }
    }

    protected static native boolean initIDs0();
    /** Must be called on Main-Thread */
    private native long createWindow1(long oldWindow, long parentWindow, int x, int y, int w, int h, float reqPixelScale,
                                      boolean fullscreen, int windowStyle, int backingStoreType,
                                      boolean opaque, boolean atop, boolean abottom, boolean visible, long view);

    private native int getDisplayID0(long window);
    private native void setPixelScale0(long window, long view, float reqPixelScale);
    private native boolean lockSurface0(long window, long view);
    private native boolean unlockSurface0(long window, long view);
    /** Must be called on Main-Thread */
    private native void requestFocus0(long window, boolean force);
    /** Must be called on Main-Thread */
    private native void resignFocus0(long window);
    /** Must be called on Main-Thread. In case this is a child window and parent is still visible, orderBack(..) is issued instead of orderOut(). */
    private native void orderOut0(long window);
    /** Must be called on Main-Thread */
    private native void orderFront0(long window);
    /** Must be called on Main-Thread */
    private native void close0(long window);
    /** Must be called on Main-Thread */
    private native void setTitle0(long window, String title);
    private native long contentView0(long window);
    /** Must be called on Main-Thread */
    private native void changeContentView0(long parentWindowOrView, long window, long view);
    /** Must be called on Main-Thread */
    private native void setWindowClientTopLeftPointAndSize0(long window, int x, int y, int w, int h, boolean display);
    /** Must be called on Main-Thread */
    private native void setWindowClientTopLeftPoint0(long window, int x, int y, boolean display);
    /** Triggers {@link #sizeScreenPosInsetsChanged(boolean, int, int, int, int, int, int, int, int, boolean)} */
    private native void updateSizePosInsets0(long window, boolean defer);
    private static native Object getLocationOnScreen0(long windowHandle, int src_x, int src_y);
    private static native void setPointerIcon0(long windowHandle, long handle);
    private static native void setPointerVisible0(long windowHandle, boolean hasFocus, boolean visible);
    private static native void confinePointer0(long windowHandle, boolean confine);
    private static native void warpPointer0(long windowHandle, int x, int y);

    // Window styles
    private static final int NSBorderlessWindowMask     = 0;
    private static final int NSTitledWindowMask         = 1 << 0;
    private static final int NSClosableWindowMask       = 1 << 1;
    private static final int NSMiniaturizableWindowMask = 1 << 2;
    private static final int NSResizableWindowMask      = 1 << 3;

    // Window backing store types
    private static final int NSBackingStoreRetained     = 0;
    private static final int NSBackingStoreNonretained  = 1;
    private static final int NSBackingStoreBuffered     = 2;

    private volatile long surfaceHandle = 0;
    private long sscSurfaceHandle = 0;
    private boolean isOffscreenInstance = false;
    private boolean resizeAnimatorPaused = false;
}
