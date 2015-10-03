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

package com.jogamp.newt;

import java.util.List;

import com.jogamp.newt.Display.PointerIcon;
import com.jogamp.newt.event.GestureHandler;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.MouseListener;

import jogamp.newt.Debug;
import jogamp.newt.WindowImpl;

import com.jogamp.nativewindow.CapabilitiesChooser;
import com.jogamp.nativewindow.CapabilitiesImmutable;
import com.jogamp.nativewindow.NativeWindow;
import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.nativewindow.WindowClosingProtocol;
import com.jogamp.nativewindow.util.Rectangle;
import com.jogamp.nativewindow.util.RectangleImmutable;
import com.jogamp.nativewindow.util.SurfaceSize;

/**
 * Specifying NEWT's Window functionality:
 * <ul>
 *   <li>On- and offscreen windows</li>
 *   <li>Keyboard and multi-pointer input</li>
 *   <li>Native reparenting</li>
 *   <li>Toggable fullscreen and decoration mode</li>
 *   <li>Transparency</li>
 *   <li>... and more</li>
 * </ul>
 * <p>
 * One use case is {@link com.jogamp.newt.opengl.GLWindow}, which delegates
 * window operation to an instance of this interface while providing OpenGL
 * functionality.
 * </p>
 * <p>
 * All values of this interface are represented in window units, if not stated otherwise.
 * </p>
 *
 * <a name="coordinateSystem"><h5>Coordinate System</h5></a>
 * <p>
 *  <ul>
 *      <li>Screen space has it's origin in the top-left corner, and may not be at 0/0.</li>
 *      <li>Window origin is in it's top-left corner, see {@link #getX()} and {@link #getY()}. </li>
 *      <li>Window client-area excludes {@link #getInsets() insets}, i.e. window decoration.</li>
 *      <li>Window origin is relative to it's parent window if exist, or the screen position (top-level).</li>
 *  </ul>
 *  See {@link NativeWindow} and {@link Screen}.
 * </p>
 * <a name="customwindowicons"><h5>Custom Window Icons</h5></a>
 * <p>
 * Custom window icons can be defined via system property <code>newt.window.icons</code>,
 * which shall contain a list of PNG icon locations from low- to high-resolution,
 * separated by one whitespace or one comma character.
 * The location must be resolvable via classpath, i.e. shall reference a location within the jar file.
 * Example (our default):
 * <pre>
 *   -Dnewt.window.icons="newt/data/jogamp-16x16.png,newt/data/jogamp-32x32.png"
 *   -Djnlp.newt.window.icons="newt/data/jogamp-16x16.png,newt/data/jogamp-32x32.png"
 * </pre>
 * The property can also be set programmatically, which must happen before any NEWT classes are <i>touched</i>:
 * <pre>
 *   System.setProperty("newt.window.icons", "newt/data/jogamp-16x16.png, newt/data/jogamp-32x32.png");
 * </pre>
 * To disable even Jogamp's own window icons in favor of system icons,
 * simply set a non-existing location, e.g.:
 * <pre>
 *   -Dnewt.window.icons="null,null"
 * </pre>
 * </p>
 *
 * <a name="lifecycleHeavy"><h5>Use of Lifecycle Heavy functions</h5></a>
 * <p>
 * Some of the methods specified here are lifecycle-heavy. That is, they are able
 * to destroy and/or reattach resources to/from the window. Because of this, the methods
 * are <i>not safe</i> to be called from EDT related threads. For example, it is not
 * safe for a method in an attached {@link KeyListener} to call {@link #setFullscreen(boolean)}
 * on a {@link Window} directly. It is safe, however, for that method to spawn a background
 * worker thread which calls the method directly. The documentation for individual methods
 * indicates whether or not they are lifecycle-heavy.
 * </p>
 */
public interface Window extends NativeWindow, WindowClosingProtocol, ScalableSurface {
    public static final boolean DEBUG_MOUSE_EVENT = Debug.debug("Window.MouseEvent");
    public static final boolean DEBUG_KEY_EVENT = Debug.debug("Window.KeyEvent");
    public static final boolean DEBUG_IMPLEMENTATION = Debug.debug("Window");

    /** A 1s timeout while waiting for a native action response, ie {@link #setVisible(boolean)}. */
    public static final long TIMEOUT_NATIVEWINDOW = 1000;

    //
    // States (keep in sync w/ src/newt/native/Window.h)
    //
    /**
     * Visibility of this instance.
     * <p>Native instance gets created at first visibility, following NEWT's lazy creation pattern.</p>
     * <p>Changing this state is <a href="#lifecycleHeavy">lifecycle heavy</a>.</p>
     * <p>Bit number {@value}.</p>
     * <p>Defaults to {@code false}.</p>
     * @see #getStateMask()
     * @since 2.3.2
     */
    public static final int STATE_BIT_VISIBLE = 0; // reconfig-flag
    /**
     * Hinting that no custom position has been set before first {@link #STATE_BIT_VISIBLE visibility} of this instance.
     * <p>If kept {@code false} at creation, this allows the WM to choose the top-level window position,
     * otherwise the custom position is being enforced.</p>
     * <p>Bit number {@value}.</p>
     * <p>Defaults to {@code true}.</p>
     * @see #getStateMask()
     * @since 2.3.2
     */
    public static final int STATE_BIT_AUTOPOSITION = 1;
    /**
     * Set if window is a <i>child window</i>, i.e. has been {@link #reparentWindow(NativeWindow, int, int, int) reparented}.
     * <p>
     * Otherwise bit is cleared, i.e. window is <i>top-level</i>.
     * </p>
     * <p>Changing this state is <a href="#lifecycleHeavy">lifecycle heavy</a>.</p>
     * <p>Bit number {@value}.</p>
     * <p>Defaults to {@code false}.</p>
     * @see #getStateMask()
     * @since 2.3.2
     */
    public static final int STATE_BIT_CHILDWIN = 2;       // reconfig-flag
    /**
     * Set if window has <i>the input focus</i>, otherwise cleared.
     * <p>Bit number {@value}.</p>
     * <p>Defaults to {@code false}.</p>
     * @see #getStateMask()
     * @since 2.3.2
     */
    public static final int STATE_BIT_FOCUSED = 3;
    /**
     * Set if window has <i>window decorations</i>, otherwise cleared.
     * <p>Bit number {@value}.</p>
     * <p>Defaults to {@code false}.</p>
     * @see #getStateMask()
     * @since 2.3.2
     */
    public static final int STATE_BIT_UNDECORATED = 4;    // reconfig-flag
    /**
     * Set if window is <i>always on top</i>, otherwise cleared.
     * <p>Bit number {@value}.</p>
     * <p>Defaults to {@code false}.</p>
     * @see #getStateMask()
     * @since 2.3.2
     */
    public static final int STATE_BIT_ALWAYSONTOP = 5;    // reconfig-flag
    /**
     * Set if window is <i>always on bottom</i>, otherwise cleared.
     * <p>Bit number {@value}.</p>
     * <p>Defaults to {@code false}.</p>
     * @see #getStateMask()
     * @since 2.3.2
     */
    public static final int STATE_BIT_ALWAYSONBOTTOM = 6; // reconfig-flag
    /**
     * Set if window is <i>sticky</i>, i.e. visible <i>on all virtual desktop</i>, otherwise cleared.
     * <p>Bit number {@value}.</p>
     * <p>Defaults to {@code false}.</p>
     * @see #getStateMask()
     * @since 2.3.2
     */
    public static final int STATE_BIT_STICKY = 7;    // reconfig-flag
    /**
     * Set if window is <i>resizable</i>, otherwise cleared.
     * <p>Bit number {@value}.</p>
     * <p>Defaults to {@code true}.</p>
     * @see #getStateMask()
     * @since 2.3.2
     */
    public static final int STATE_BIT_RESIZABLE = 8; // reconfig-flag
    /**
     * Set if window is <i>maximized vertically</i>, otherwise cleared.
     * <p>Bit number {@value}.</p>
     * <p>Defaults to {@code false}.</p>
     * @see #getStateMask()
     * @since 2.3.2
     */
    public static final int STATE_BIT_MAXIMIZED_VERT = 9; // reconfig-flag
    /**
     * Set if window is <i>maximized horizontally</i>, otherwise cleared.
     * <p>Bit number {@value}.</p>
     * <p>Defaults to {@code false}.</p>
     * @see #getStateMask()
     * @since 2.3.2
     */
    public static final int STATE_BIT_MAXIMIZED_HORZ = 10; // reconfig-flag
    /**
     * Set if window is in <i>fullscreen mode</i>, otherwise cleared.
     * <p>
     * Usually fullscreen mode implies {@link #STATE_BIT_UNDECORATED},
     * however, an implementation is allowed to ignore this if unavailable.
     * </p>
     * <p>Bit number {@value}.</p>
     * <p>Defaults to {@code false}.</p>
     * @see #getStateMask()
     * @since 2.3.2
     */
    public static final int STATE_BIT_FULLSCREEN = 11;    // reconfig-flag

    /**
     * Set if the <i>pointer is visible</i> when inside the window, otherwise cleared.
     * <p>Bit number {@value}.</p>
     * <p>Defaults to {@code true}.</p>
     * @see #getStateMask()
     * @since 2.3.2
     */
    public static final int STATE_BIT_POINTERVISIBLE = 12;
    /**
     * Set if the <i>pointer is confined</i> to the window, otherwise cleared.
     * <p>Bit number {@value}.</p>
     * <p>Defaults to {@code false}.</p>
     * @see #getStateMask()
     * @since 2.3.2
     */
    public static final int STATE_BIT_POINTERCONFINED = 13;

    /**
     * Bitmask for {@link #STATE_BIT_VISIBLE}, {@value}.
     * @since 2.3.2
     */
    public static final int STATE_MASK_VISIBLE = 1 << STATE_BIT_VISIBLE;
    /**
     * Bitmask for {@link #STATE_BIT_AUTOPOSITION}, {@value}.
     * @since 2.3.2
     */
    public static final int STATE_MASK_AUTOPOSITION = 1 << STATE_BIT_AUTOPOSITION;
    /**
     * Bitmask for {@link #STATE_BIT_CHILDWIN}, {@value}.
     * @since 2.3.2
     */
    public static final int STATE_MASK_CHILDWIN = 1 << STATE_BIT_CHILDWIN;
    /**
     * Bitmask for {@link #STATE_BIT_FOCUSED}, {@value}.
     * @since 2.3.2
     */
    public static final int STATE_MASK_FOCUSED = 1 << STATE_BIT_FOCUSED;
    /**
     * Bitmask for {@link #STATE_BIT_UNDECORATED}, {@value}.
     * @since 2.3.2
     */
    public static final int STATE_MASK_UNDECORATED = 1 << STATE_BIT_UNDECORATED;
    /**
     * Bitmask for {@link #STATE_BIT_ALWAYSONTOP}, {@value}.
     * @since 2.3.2
     */
    public static final int STATE_MASK_ALWAYSONTOP = 1 << STATE_BIT_ALWAYSONTOP;
    /**
     * Bitmask for {@link #STATE_BIT_ALWAYSONBOTTOM}, {@value}.
     * @since 2.3.2
     */
    public static final int STATE_MASK_ALWAYSONBOTTOM = 1 << STATE_BIT_ALWAYSONBOTTOM;
    /**
     * Bitmask for {@link #STATE_BIT_STICKY}, {@value}.
     * @since 2.3.2
     */
    public static final int STATE_MASK_STICKY = 1 << STATE_BIT_STICKY;
    /**
     * Bitmask for {@link #STATE_BIT_RESIZABLE}, {@value}.
     * @since 2.3.2
     */
    public static final int STATE_MASK_RESIZABLE = 1 << STATE_BIT_RESIZABLE;
    /**
     * Bitmask for {@link #STATE_BIT_MAXIMIZED_VERT}, {@value}.
     * @since 2.3.2
     */
    public static final int STATE_MASK_MAXIMIZED_VERT = 1 << STATE_BIT_MAXIMIZED_VERT;
    /**
     * Bitmask for {@link #STATE_BIT_MAXIMIZED_HORZ}, {@value}.
     * @since 2.3.2
     */
    public static final int STATE_MASK_MAXIMIZED_HORZ = 1 << STATE_BIT_MAXIMIZED_HORZ;
    /**
     * Bitmask for {@link #STATE_BIT_FULLSCREEN}, {@value}.
     * @since 2.3.2
     */
    public static final int STATE_MASK_FULLSCREEN = 1 << STATE_BIT_FULLSCREEN;
    /**
     * Bitmask for {@link #STATE_BIT_POINTERVISIBLE}, {@value}.
     * @since 2.3.2
     */
    public static final int STATE_MASK_POINTERVISIBLE = 1 << STATE_BIT_POINTERVISIBLE;
    /**
     * Bitmask for {@link #STATE_BIT_POINTERCONFINED}, {@value}.
     * @since 2.3.2
     */
    public static final int STATE_MASK_POINTERCONFINED = 1 << STATE_BIT_POINTERCONFINED;

    /**
     * Number of all public state bits.
     * @see #getStateMask()
     * @since 2.3.2
     */
    public int getStatePublicBitCount();

    /**
     * Bitmask covering all public state bits.
     * @see #getStateMask()
     * @since 2.3.2
     */
    public int getStatePublicBitmask();

    /**
     * Returns the current status mask of this instance.
     * @see #getSupportedStateMask()
     * @see #STATE_MASK_VISIBLE
     * @see #STATE_MASK_AUTOPOSITION
     * @see #STATE_MASK_CHILDWIN
     * @see #STATE_MASK_FOCUSED
     * @see #STATE_MASK_UNDECORATED
     * @see #STATE_MASK_ALWAYSONTOP
     * @see #STATE_MASK_ALWAYSONBOTTOM
     * @see #STATE_MASK_STICKY
     * @see #STATE_MASK_RESIZABLE
     * @see #STATE_MASK_MAXIMIZED_VERT
     * @see #STATE_MASK_MAXIMIZED_HORZ
     * @see #STATE_MASK_FULLSCREEN
     * @see #STATE_MASK_POINTERVISIBLE
     * @see #STATE_MASK_POINTERCONFINED
     * @since 2.3.2
     */
    int getStateMask();

    /**
     * Returns a string representation of the {@link #getStateMask() current state mask}.
     * @since 2.3.2
     */
    String getStateMaskString();

    /**
     * Returns the supported {@link #getStateMask() state mask} of the implementation.
     * <p>
     * Implementation provides supported {@link #getStateMask() state mask} values at runtime
     * <i>after</i> native window creation, i.e. first visibility.
     * </p>
     * <p>
     * Please note that a window's size shall also be allowed to change, i.e. {@link #setSize(int, int)}.
     * </p>
     * <p>
     * Default value is {@link #STATE_MASK_VISIBLE} | {@link #STATE_MASK_FOCUSED} | {@link #STATE_MASK_FULLSCREEN},
     * i.e. the <b>minimum requirement</b> for all implementations.
     * </p>
     * <p>
     * Before native window creation {@link #getStatePublicBitmask()} is returned,
     * i.e. it is assumed all features are supported.
     * </p>
     * <p>
     * Semantic of the supported state-mask bits (after native creation, i.e. 1st visibility):
     * <ul>
     * <li>{@link #STATE_MASK_VISIBLE}: {@link #setVisible(boolean) Visibility} can be toggled. <b>Minimum requirement</b>.</li>
     * <li>{@link #STATE_MASK_CHILDWIN}: {@link #reparentWindow(NativeWindow, int, int, int) Native window parenting} is supported.</li>
     * <li>{@link #STATE_MASK_FOCUSED}: Window {@link #requestFocus() focus management} is supported.  <b>Minimum requirement</b>.</li>
     * <li>{@link #STATE_MASK_UNDECORATED}: {@link #setUndecorated(boolean) Window decoration} can be toggled.</li>
     * <li>{@link #STATE_MASK_ALWAYSONTOP}: Window can be set {@link #setAlwaysOnTop(boolean) always-on-top}. </li>
     * <li>{@link #STATE_MASK_ALWAYSONBOTTOM}: Window can be set {@link #setAlwaysOnBottom(boolean) always-on-bottom}. </li>
     * <li>{@link #STATE_MASK_STICKY}: Window can be set {@link #setSticky(boolean) sticky}.</li>
     * <li>{@link #STATE_MASK_RESIZABLE}: Window {@link #setResizable(boolean) resizability} can be toggled.</li>
     * <li>{@link #STATE_MASK_MAXIMIZED_VERT}: Window can be {@link #setMaximized(boolean, boolean) maximized-vertically}. </li>
     * <li>{@link #STATE_MASK_MAXIMIZED_HORZ}: Window can be {@link #setMaximized(boolean, boolean) maximized-horizontally}. </li>
     * <li>{@link #STATE_MASK_FULLSCREEN}: Window {@link #setFullscreen(boolean) fullscreen} can be toggled. </li>
     * <li>{@link #STATE_MASK_POINTERVISIBLE}: Window {@link #setPointerVisible(boolean) pointer visibility} can be toggled. </li>
     * <li>{@link #STATE_MASK_POINTERCONFINED}: Window {@link #confinePointer(boolean) pointer can be confined}. </li>
     * </ul>
     * </p>
     * @see #getStateMask()
     * @since 2.3.2
     */
    int getSupportedStateMask();

    /**
     * Returns a string representation of the {@link #getSupportedStateMask() supported state mask}.
     * @since 2.3.2
     */
    String getSupportedStateMaskString();

    //
    // Lifecycle
    //

    /**
     * @return true if the native window handle is valid and ready to operate, ie
     * if the native window has been created via {@link #setVisible(boolean) setVisible(true)}, otherwise false.
     *
     * @see #setVisible(boolean)
     * @see #destroy(boolean)
     */
    boolean isNativeValid();

    /**
     * @return The associated {@link Screen}
     */
    Screen getScreen();

    /**
     * Returns the {@link MonitorDevice} with the highest {@link MonitorDevice#getViewportInWindowUnits() viewport}
     * {@link RectangleImmutable#coverage(RectangleImmutable) coverage} of this window.
     * <p>
     * If no coverage is detected the first {@link MonitorDevice} is returned.
     * </p>
     */
    MonitorDevice getMainMonitor();

    /**
     * Set the CapabilitiesChooser to help determine the native visual type.
     *
     * @param chooser the new CapabilitiesChooser
     * @return the previous CapabilitiesChooser
     */
    CapabilitiesChooser setCapabilitiesChooser(CapabilitiesChooser chooser);

    /**
     * Gets an immutable set of requested capabilities.
     *
     * @return the requested capabilities
     */
    CapabilitiesImmutable getRequestedCapabilities();

    /**
     * Gets an immutable set of chosen capabilities.
     *
     * @return the chosen capabilities
     */
    CapabilitiesImmutable getChosenCapabilities();

    /**
     * {@inheritDoc}
     * <p>
     * Also iterates through this window's children and destroys them.
     * </p>
     * <p>
     * Visibility is set to false.
     * </p>
     * <p>
     * Method sends out {@link WindowEvent#EVENT_WINDOW_DESTROY_NOTIFY pre-} and
     * {@link WindowEvent#EVENT_WINDOW_DESTROYED post-} destruction events
     * to all of it's {@link WindowListener}.
     * </p>
     * <p>
     * This method invokes {@link Screen#removeReference()} after it's own destruction,<br>
     * which will issue {@link Screen#destroy()} if the reference count becomes 0.<br>
     * This destruction sequence shall end up in {@link Display#destroy()}, if all reference counts become 0.
     * </p>
     * <p>
     * The Window can be recreate via {@link #setVisible(boolean) setVisible(true)}.
     * </p>
     * <p>This method is <a href="#lifecycleHeavy">lifecycle heavy</a>.</p>
     * @see #destroy()
     * @see #setVisible(boolean)
     */
    @Override
    void destroy();

    /**
     * Set a custom action handling destruction issued by a {@link WindowImpl#windowDestroyNotify(boolean) toolkit triggered window destroy}
     * replacing the default {@link #destroy()} action.
     * <p>
     * The custom action shall call {@link #destroy()}
     * but may perform further tasks before and after.
     * </p>
     */
    void setWindowDestroyNotifyAction(Runnable r);

    /**
     * Calls {@link #setVisible(boolean, boolean) setVisible(true, visible)},
     * i.e. blocks until the window becomes visible.
     * <p>This method is <a href="#lifecycleHeavy">lifecycle heavy</a>.</p>
     * @see #setVisible(boolean, boolean)
     * @see #STATE_BIT_VISIBLE
     */
    void setVisible(boolean visible);

    /**
     * <code>setVisible(..)</code> makes the window and children visible if <code>visible</code> is true,
     * otherwise the window and children becomes invisible.
     * <p>Native instance gets created at first visibility, following NEWT's lazy creation pattern.</p>
     * <p>
     * If <code>wait</code> is true, method blocks until window is {@link #isVisible() visible} and {@link #isNativeValid() valid},
     * otherwise method returns immediately.
     * </p>
     * <p>
     * Zero size semantics are respected, see {@link #setSize(int,int)}:<br>
     * <pre>
     * if ( 0 == windowHandle && visible ) {
     *   this.visible = visible;
     *   if( 0 &lt; width && 0 &lt; height ) {
     *     createNative();
     *   }
     * } else if ( this.visible != visible ) {
     *   this.visible = visible;
     *   setNativeSizeImpl();
     * }
     * </pre></p>
     * <p>
     * In case this window is {@link #isChildWindow() a child window} and has a {@link com.jogamp.nativewindow.NativeWindow} parent,<br>
     * <code>setVisible(wait, true)</code> has no effect as long the parent's is not valid yet,
     * i.e. {@link com.jogamp.nativewindow.NativeWindow#getWindowHandle()} returns <code>null</code>.<br>
     * <code>setVisible(wait, true)</code> shall be repeated when the parent becomes valid.
     * </p>
     * <p>This method is <a href="#lifecycleHeavy">lifecycle heavy</a>.</p>
     * @see #STATE_BIT_VISIBLE
     */
    void setVisible(boolean wait, boolean visible);

    /**
     * @see #STATE_BIT_VISIBLE
     * @see #setVisible(boolean, boolean)
     */
    boolean isVisible();

    /**
     * If the implementation uses delegation, return the delegated {@link Window} instance,
     * otherwise return <code>this</code> instance. */
    Window getDelegatedWindow();

    //
    // Child Window Management
    //

    boolean addChild(NativeWindow win);

    boolean removeChild(NativeWindow win);

    //
    // Modes / States
    //

    /**
     * Returns a newly created {@link Rectangle} containing window origin, {@link #getX()} & {@link #getY()},
     * and size, {@link #getWidth()} & {@link #getHeight()}, in window units.
     */
    Rectangle getBounds();

    /**
     * Returns the <i>pixels per millimeter</i> of this window's {@link NativeSurface}
     * according to the {@link #getMainMonitor() main monitor}'s <i>current</i> {@link MonitorMode mode}'s
     * {@link SurfaceSize#getResolution() surface resolution}.
     * <p>
     * Method takes the {@link #getCurrentSurfaceScale(float[]) current surface-scale} and {@link #getMaximumSurfaceScale(float[]) native surface-scale}
     * into account, i.e.:
     * <pre>
     *    surfacePpMM = monitorPpMM * currentSurfaceScale / nativeSurfaceScale,
     *    with PpMM == pixel per millimeter
     * </pre>
     * </p>
     * <p>
     * To convert the result to <i>dpi</i>, i.e. dots-per-inch, multiply both components with <code>25.4f</code>.
     * </p>
     * @param ppmmStore float[2] storage for the ppmm result
     * @return the passed storage containing the ppmm for chaining
     */
    float[] getPixelsPerMM(final float[] ppmmStore);

    /**
     * Sets the size of the window's client area in window units, excluding decorations.
     *
     * <p>
     * Zero size semantics are respected, see {@link #setVisible(boolean)}:<br>
     * <pre>
     * if ( visible && 0 != windowHandle && ( 0 &ge; width || 0 &ge; height ) ) {
     *   setVisible(false);
     * } else if ( visible && 0 == windowHandle && 0 &lt; width && 0 &lt; height ) {
     *   setVisible(true);
     * } else {
     *   // as expected ..
     * }
     * </pre></p>
     * <p>
     * This call is ignored if in fullscreen mode.<br></p>
     *
     * @param width of the window's client area in window units
     * @param height of the window's client area in window units
     *
     * @see #setSurfaceSize(int, int)
     * @see #setTopLevelSize(int, int)
     * @see #getInsets()
     */
    void setSize(int width, int height);

    /**
     * Sets the size of the window's surface in pixel units which claims the window's client area excluding decorations.
     *
     * <p>
     * In multiple monitor mode, setting the window's surface size in pixel units
     * might not be possible due to unknown <i>scale</i> values of the target display.
     * Hence re-setting the pixel unit size after window creation is recommended.
     * </p>
     * <p>
     * Zero size semantics are respected, see {@link #setVisible(boolean)}:<br>
     * <pre>
     * if ( visible && 0 != windowHandle && ( 0 &ge; width || 0 &ge; height ) ) {
     *   setVisible(false);
     * } else if ( visible && 0 == windowHandle && 0 &lt; width && 0 &lt; height ) {
     *   setVisible(true);
     * } else {
     *   // as expected ..
     * }
     * </pre></p>
     * <p>
     * This call is ignored if in fullscreen mode.<br></p>
     *
     * @param pixelWidth of the window's client area in pixel units
     * @param pixelHeight of the window's client area in pixel units
     *
     * @see #setSize(int, int)
     * @see #getInsets()
     */
    void setSurfaceSize(int pixelWidth, int pixelHeight);

    /**
     * Sets the size of the top-level window including insets (window decorations) in window units.
     *
     * <p>
     * Note: Insets (if supported) are available only after the window is set visible and hence has been created.
     * </p>
     *
     * @param width of the top-level window area in window units
     * @param height of the top-level window area in window units
     *
     * @see #setSize(int, int)
     * @see #getInsets()
     */
    void setTopLevelSize(int width, int height);

    /**
     * Sets the location of the window's client area excluding insets (window decorations) in window units.<br>
     *
     * This call is ignored if in fullscreen mode.<br>
     *
     * @param x coord of the client-area's top left corner in window units
     * @param y coord of the client-area's top left corner in window units
     *
     * @see #getInsets()
     */
    void setPosition(int x, int y);

    /**
     * Sets the location of the top-level window inclusive insets (window decorations) in window units.<br>
     *
     * <p>
     * Note: Insets (if supported) are available only after the window is set visible and hence has been created.
     * </p>
     *
     * This call is ignored if in fullscreen mode.<br>
     *
     * @param x coord of the top-level left corner in window units
     * @param y coord of the top-level left corner in window units
     *
     * @see #setPosition(int, int)
     * @see #getInsets()
     */
    void setTopLevelPosition(int x, int y);

    /**
     * @see {@link #STATE_BIT_UNDECORATED}
     * @see {@link #STATE_MASK_UNDECORATED}
     */
    void setUndecorated(boolean value);
    /**
     * @see {@link #STATE_BIT_UNDECORATED}
     * @see {@link #STATE_MASK_UNDECORATED}
     */
    boolean isUndecorated();

    /**
     * <p>Operation is ignored if this instance {@link #isChildWindow() is a child window}.</p>
     * @see {@link #STATE_BIT_ALWAYSONTOP}
     * @see {@link #STATE_MASK_ALWAYSONTOP}
     */
    void setAlwaysOnTop(boolean value);
    /**
     * @see {@link #STATE_BIT_ALWAYSONTOP}
     * @see {@link #STATE_MASK_ALWAYSONTOP}
     */
    boolean isAlwaysOnTop();

    /**
     * <p>Operation is ignored if this instance {@link #isChildWindow() is a child window}.</p>
     * @see {@link #STATE_BIT_ALWAYSONBOTTOM}
     * @see {@link #STATE_MASK_ALWAYSONBOTTOM}
     * @since 2.3.2
     */
    void setAlwaysOnBottom(boolean value);
    /**
     * @see {@link #STATE_BIT_ALWAYSONBOTTOM}
     * @see {@link #STATE_MASK_ALWAYSONBOTTOM}
     * @since 2.3.2
     */
    boolean isAlwaysOnBottom();

    /**
     * <p>Operation is ignored if this instance {@link #isChildWindow() is a child window}.</p>
     * @see {@link #STATE_BIT_RESIZABLE}
     * @see {@link #STATE_MASK_RESIZABLE}
     * @since 2.3.2
     */
    void setResizable(final boolean value);
    /**
     * @see {@link #STATE_BIT_RESIZABLE}
     * @see {@link #STATE_MASK_RESIZABLE}
     * @since 2.3.2
     */
    boolean isResizable();

    /**
     * <p>Operation is ignored if this instance {@link #isChildWindow() is a child window}.</p>
     * @see {@link #STATE_BIT_STICKY}
     * @see {@link #STATE_MASK_STICKY}
     * @since 2.3.2
     */
    void setSticky(final boolean value);
    /**
     * @see {@link #STATE_BIT_STICKY}
     * @see {@link #STATE_MASK_STICKY}
     * @since 2.3.2
     */
    boolean isSticky();

    /**
     * <p>Operation is ignored if this instance {@link #isChildWindow() is a child window}.</p>
     * @see {@link #STATE_BIT_MAXIMIZED_HORZ}
     * @see {@link #STATE_BIT_MAXIMIZED_VERT}
     * @see {@link #STATE_MASK_MAXIMIZED_HORZ}
     * @see {@link #STATE_MASK_MAXIMIZED_VERT}
     * @since 2.3.2
     */
    void setMaximized(final boolean horz, final boolean vert);
    /**
     * @see {@link #STATE_BIT_MAXIMIZED_VERT}
     * @see {@link #STATE_MASK_MAXIMIZED_VERT}
     * @since 2.3.2
     */
    boolean isMaximizedVert();
    /**
     * @see {@link #STATE_BIT_MAXIMIZED_HORZ}
     * @see {@link #STATE_MASK_MAXIMIZED_HORZ}
     * @since 2.3.2
     */
    boolean isMaximizedHorz();

    void setTitle(String title);

    String getTitle();

    /** @see #setPointerVisible(boolean) */
    boolean isPointerVisible();

    /**
     * Makes the pointer visible or invisible.
     *
     * @param pointerVisible defaults to <code>true</code> for platforms w/ visible pointer,
     *                       otherwise defaults to <code>true</code>, eg. Android.
     * @see #confinePointer(boolean)
     */
    void setPointerVisible(boolean pointerVisible);

    /**
     * Returns the current {@link PointerIcon}, which maybe <code>null</code> for the default.
     * @see #setPointerIcon(PointerIcon)
     */
    PointerIcon getPointerIcon();

    /**
     * @param pi Valid {@link PointerIcon} reference or <code>null</code> to reset the pointer icon to default.
     *
     * @see PointerIcon
     * @see Display#createPointerIcon(com.jogamp.common.util.IOUtil.ClassResources, int, int)
     */
    void setPointerIcon(final PointerIcon pi);

    /** @see #confinePointer(boolean) */
    boolean isPointerConfined();

    /**
     * Confine the pointer to this window, ie. pointer jail.
     * <p>
     * Before jailing the mouse pointer,
     * the window request the focus and the pointer is centered in the window.
     * </p>
     * <p>
     * In combination w/ {@link #warpPointer(int, int)}
     * and maybe {@link #setPointerVisible(boolean)} a simple mouse
     * navigation can be realized.</p>
     *
     * @param confine defaults to <code>false</code>.
     */
    void confinePointer(boolean confine);

    /**
     * Moves the pointer to x/y relative to this window's origin in pixel units.
     *
     * @param x relative pointer x position within this window in pixel units
     * @param y relative pointer y position within this window in pixel units
     *
     * @see #confinePointer(boolean)
     */
    void warpPointer(int x, int y);

    /** Reparenting operation types */
    public enum ReparentOperation {
        /** No native reparenting valid */
        ACTION_INVALID,

        /** No native reparenting action required, no change*/
        ACTION_NOP,

        /** Native reparenting incl. Window tree */
        ACTION_NATIVE_REPARENTING,

        /** Native window creation after tree change - instead of reparenting. */
        ACTION_NATIVE_CREATION,

        /** Change Window tree only, native creation is pending */
        ACTION_NATIVE_CREATION_PENDING;
    }

    /** Reparenting hint (bitfield value): Force destroy and hence {@link ReparentOperation#ACTION_NATIVE_CREATION re-creating} the window. */
    public static final int REPARENT_HINT_FORCE_RECREATION = 1 << 0;
    /** Reparenting hint (bitfield value): Claim window becomes visible after reparenting, which is important for e.g. preserving the GL-states in case window is invisible while reparenting. */
    public static final int REPARENT_HINT_BECOMES_VISIBLE = 1 << 1;

    /**
     * Change this window's parent window.<br>
     * <P>
     * In case the old parent is not null and a Window,
     * this window is removed from it's list of children.<br>
     * In case the new parent is not null and a Window,
     * this window is added to it's list of children.<br></P>
     * <p>This method is <a href="#lifecycleHeavy">lifecycle heavy</a>.</p>
     *
     * @param newParent The new parent NativeWindow. If null, this Window becomes a top level window.
     * @param x new top-level position in window units, use -1 for default position.
     * @param y new top-level position in window units, use -1 for default position.
     * @param hints May contain hints (bitfield values) like {@link #REPARENT_HINT_FORCE_RECREATION} or {@link #REPARENT_HINT_BECOMES_VISIBLE}.
     *
     * @return The issued reparent action type (strategy) as defined in Window.ReparentAction
     */
    ReparentOperation reparentWindow(NativeWindow newParent, int x, int y, int hints);

    /**
     * Returns {@code true} if this window is a child window,
     * i.e. has been {@link #reparentWindow(NativeWindow, int, int, int) reparented}.
     * <p>
     * Otherwise return {@code false}, i.e. this window is a top-level window.
     * </p>
     */
    boolean isChildWindow();

    /**
     * Enable or disable fullscreen mode for this window.
     * <p>
     * Fullscreen mode is established on the {@link #getMainMonitor() main monitor}.
     * </p>
     * <p>This method is <a href="#lifecycleHeavy">lifecycle heavy</a>.</p>
     * @param fullscreen enable or disable fullscreen mode
     * @return success
     * @see #setFullscreen(List)
     * @see #isFullscreen()
     */
    boolean setFullscreen(boolean fullscreen);

    /**
     * Enable fullscreen mode for this window spanning across the given {@link MonitorDevice}s
     * or across all {@link MonitorDevice}s.
     * <p>
     * Disable fullscreen via {@link #setFullscreen(boolean)}.
     * </p>
     * <p>This method is <a href="#lifecycleHeavy">lifecycle heavy</a>.</p>
     * @param monitors if <code>null</code> fullscreen will be spanned across all {@link MonitorDevice}s,
     *                 otherwise across the given list of {@link MonitorDevice}.
     * @return success
     * @see #setFullscreen(boolean)
     * @see #isFullscreen()
     */
    boolean setFullscreen(List<MonitorDevice> monitors);

    boolean isFullscreen();

    static interface FocusRunnable {
        /**
         * @return false if NEWT shall proceed requesting the focus,
         * true if NEWT shall not request the focus.
         */
        public boolean run();
    }

    /**
     * Sets a {@link FocusRunnable},
     * which {@link FocusRunnable#run()} method is executed before the native focus is requested.
     * <p>
     * This allows notifying a covered window toolkit like AWT that the focus is requested,
     * hence focus traversal can be made transparent.
     * </p>
     */
    void setFocusAction(FocusRunnable focusAction);

    /**
     * Sets a {@link KeyListener} allowing focus traversal with a covered window toolkit like AWT.
     * <p>
     * The {@link KeyListener} methods are invoked prior to all other {@link KeyListener}'s
     * allowing to suppress the {@link KeyEvent} via the {@link InputEvent#consumedTag}
     * and to perform focus traversal with a 3rd party toolkit.
     * </p>
     * <p>
     * The {@link KeyListener} methods are not invoked for {@link KeyEvent#isAutoRepeat() auto-repeat} events.
     * </p>
     * @param l
     */
    void setKeyboardFocusHandler(KeyListener l);

    /**
     * Request focus for this native window
     * <p>
     * The request is handled on this Window EDT and blocked until finished.
     * </p>
     *
     * @see #requestFocus(boolean)
     */
    void requestFocus();

    /**
     * Request focus for this native window
     * <p>
     * The request is handled on this Window EDT.
     * </p>
     *
     * @param wait true if waiting until the request is executed, otherwise false
     * @see #requestFocus()
     */
    void requestFocus(boolean wait);

    /**
     * Trigger window repaint while passing the dirty region in pixel units.
     * @param x dirty-region y-pos in pixel units
     * @param y dirty-region x-pos in pixel units
     * @param width dirty-region width in pixel units
     * @param height dirty-region height in pixel units
     */
    void windowRepaint(int x, int y, int width, int height);

    /**
     * Enqueues a {@link com.jogamp.newt.event.NEWTEvent NEWT event}.
     * @param wait Passing <code>true</code> will block until the event has been processed, otherwise method returns immediately.
     * @param event The {@link com.jogamp.newt.event.NEWTEvent event} to enqueue.
     */
    void enqueueEvent(boolean wait, com.jogamp.newt.event.NEWTEvent event);

    void runOnEDTIfAvail(boolean wait, final Runnable task);


    //
    // WindowListener
    //

    /**
     * Send a {@link WindowEvent} to all {@link WindowListener}.
     * @param eventType a {@link WindowEvent} type, e.g. {@link WindowEvent#EVENT_WINDOW_REPAINT}.
     */
    public void sendWindowEvent(int eventType);

    /**
     * Appends the given {@link com.jogamp.newt.event.WindowListener} to the end of
     * the list.
     */
    void addWindowListener(WindowListener l);

    /**
     *
     * Inserts the given {@link com.jogamp.newt.event.WindowListener} at the
     * specified position in the list.<br>
     *
     * @param index Position where the listener will be inserted.
     * Should be within (0 <= index && index <= size()).
     * An index value of -1 is interpreted as the end of the list, size().
     * @param l The listener object to be inserted
     * @throws IndexOutOfBoundsException If the index is not within (0 <= index && index <= size()), or -1
     */
    void addWindowListener(int index, WindowListener l) throws IndexOutOfBoundsException;

    void removeWindowListener(WindowListener l);

    WindowListener getWindowListener(int index);

    WindowListener[] getWindowListeners();

    //
    // KeyListener
    //

    /**
     * In case the platform supports or even requires a virtual on-screen keyboard,
     * this method shows or hide it depending on whether <code>visible</code> is <code>true</code>
     * or <code>false</code>.
     * <p>
     * One known platform where NEWT supports this feature is <code>Android</code>.
     * </p>
     */
    void setKeyboardVisible(boolean visible);

    /**
     * Return <code>true</code> if the virtual on-screen keyboard is visible, otherwise <code>false</code>.
     * <p>
     * Currently on <code>Android</code>, the only supported platform right now,
     * there is no way to reliably be notified of the current keyboard state.<br>
     * It would be best, if your code does not rely on this information.
     * </p>
     * @see #setKeyboardVisible(boolean)
     */
    boolean isKeyboardVisible();

    /**
     *
     * Appends the given {@link com.jogamp.newt.event.KeyListener} to the end of
     * the list.
     */
    void addKeyListener(KeyListener l);

    /**
     *
     * Inserts the given {@link com.jogamp.newt.event.KeyListener} at the
     * specified position in the list.<br>
     *
     * @param index Position where the listener will be inserted.
     * Should be within (0 <= index && index <= size()).
     * An index value of -1 is interpreted as the end of the list, size().
     * @param l The listener object to be inserted
     * @throws IndexOutOfBoundsException If the index is not within (0 <= index && index <= size()), or -1
     */
    void addKeyListener(int index, KeyListener l);

    void removeKeyListener(KeyListener l);

    KeyListener getKeyListener(int index);

    KeyListener[] getKeyListeners();


    //
    // MouseListener
    //

    /**
     * Appends the given {@link MouseListener} to the end of the list.
     */
    void addMouseListener(MouseListener l);

    /**
     * Inserts the given {@link MouseListener} at the
     * specified position in the list.<br>
     *
     * @param index Position where the listener will be inserted.
     * Should be within (0 <= index && index <= size()).
     * An index value of -1 is interpreted as the end of the list, size().
     * @param l The listener object to be inserted
     * @throws IndexOutOfBoundsException If the index is not within (0 <= index && index <= size()), or -1
     */
    void addMouseListener(int index, MouseListener l);

    /**
     * Removes the given {@link MouseListener} from the list.
     */
    void removeMouseListener(MouseListener l);

    /**
     * Returns the {@link MouseListener} from the list at the given index.
     */
    MouseListener getMouseListener(int index);

    /**
     * Returns all {@link MouseListener}
     */
    MouseListener[] getMouseListeners();

    /** Enable or disable default {@link GestureHandler}. Default is enabled. */
    void setDefaultGesturesEnabled(boolean enable);
    /** Return true if default {@link GestureHandler} are enabled. */
    boolean areDefaultGesturesEnabled();
    /**
     * Appends the given {@link GestureHandler} to the end of the list.
     */
    void addGestureHandler(GestureHandler gh);
    /**
     * Inserts the given {@link GestureHandler} at the
     * specified position in the list.<br>
     *
     * @param index Position where the listener will be inserted.
     * Should be within (0 <= index && index <= size()).
     * An index value of -1 is interpreted as the end of the list, size().
     * @param l The listener object to be inserted
     * @throws IndexOutOfBoundsException If the index is not within (0 <= index && index <= size()), or -1
     */
    void addGestureHandler(int index, GestureHandler gh);
    /**
     * Removes the given {@link GestureHandler} from the list.
     */
    void removeGestureHandler(GestureHandler gh);
    /**
     * Appends the given {@link GestureHandler.GestureListener} to the end of the list.
     */
    void addGestureListener(GestureHandler.GestureListener gl);
    /**
     * Inserts the given {@link GestureHandler.GestureListener} at the
     * specified position in the list.<br>
     *
     * @param index Position where the listener will be inserted.
     * Should be within (0 <= index && index <= size()).
     * An index value of -1 is interpreted as the end of the list, size().
     * @param l The listener object to be inserted
     * @throws IndexOutOfBoundsException If the index is not within (0 <= index && index <= size()), or -1
     */
    void addGestureListener(int index, GestureHandler.GestureListener gl);
    /**
     * Removes the given {@link GestureHandler.GestureListener} from the list.
     */
    void removeGestureListener(GestureHandler.GestureListener gl);
}
