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

import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.MouseListener;
import jogamp.newt.Debug;
import jogamp.newt.WindowImpl;

import javax.media.nativewindow.CapabilitiesChooser;
import javax.media.nativewindow.CapabilitiesImmutable;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.WindowClosingProtocol;
import javax.media.nativewindow.util.RectangleImmutable;

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
 */
public interface Window extends NativeWindow, WindowClosingProtocol {
    public static final boolean DEBUG_MOUSE_EVENT = Debug.debug("Window.MouseEvent");
    public static final boolean DEBUG_KEY_EVENT = Debug.debug("Window.KeyEvent");
    public static final boolean DEBUG_IMPLEMENTATION = Debug.debug("Window");

    /** A 1s timeout while waiting for a native action response, ie {@link #setVisible(boolean)}. */
    public static final long TIMEOUT_NATIVEWINDOW = 1000;

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
     * Returns the {@link MonitorDevice} which {@link MonitorDevice#getViewport() viewport} 
     * {@link MonitorDevice#coverage(RectangleImmutable) covers} this window the most.
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
     * <code>setVisible</code> makes the window and children visible if <code>visible</code> is true,
     * otherwise the window and children becomes invisible.
     * <p>
     * The <code>setVisible(true)</code> is responsible to actual create the native window.
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
     * In case this window is a child window and has a {@link javax.media.nativewindow.NativeWindow} parent,<br>
     * <code>setVisible(true)</code> has no effect as long the parent's is not valid yet,
     * i.e. {@link javax.media.nativewindow.NativeWindow#getWindowHandle()} returns <code>null</code>.<br>
     * <code>setVisible(true)</code> shall be repeated when the parent becomes valid.
     * </p>
     */
    void setVisible(boolean visible);

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
     * Sets the size of the window's client area, excluding decorations.
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
     * @param width of the window's client area
     * @param height of the window's client area
     *
     * @see #getInsets()
     */
    void setSize(int width, int height);

    /**
     * Sets the size of the top-level window including insets (window decorations).
     *
     * <p>
     * Note: Insets (if supported) are available only after the window is set visible and hence has been created.
     * </p>
     *
     * @param width of the top-level window area
     * @param height of the top-level window area
     *
     * @see #setSize(int, int)
     * @see #getInsets()
     */
    void setTopLevelSize(int width, int height);

    /**
     * Sets the location of the window's client area, excluding insets (window decorations).<br>
     *
     * This call is ignored if in fullscreen mode.<br>
     *
     * @param x coord of the client-area's top left corner
     * @param y coord of the client-area's top left corner
     *
     * @see #getInsets()
     */
    void setPosition(int x, int y);

    /**
     * Sets the location of the top-level window inclusive insets (window decorations).<br>
     *
     * <p>
     * Note: Insets (if supported) are available only after the window is set visible and hence has been created.
     * </p>
     *
     * This call is ignored if in fullscreen mode.<br>
     *
     * @param x coord of the top-level left corner
     * @param y coord of the top-level left corner
     *
     * @see #setPosition(int, int)
     * @see #getInsets()
     */
    void setTopLevelPosition(int x, int y);

    void setUndecorated(boolean value);

    boolean isUndecorated();

    void setAlwaysOnTop(boolean value);

    boolean isAlwaysOnTop();

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
     * Moves the pointer to x/y relative to this window's origin.
     *
     * @param x relative pointer x position within this window
     * @param y relative pointer y position within this window
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

    /**
     * Change this window's parent window.<br>
     * <P>
     * In case the old parent is not null and a Window,
     * this window is removed from it's list of children.<br>
     * In case the new parent is not null and a Window,
     * this window is added to it's list of children.<br></P>
     *
     * @param newParent The new parent NativeWindow. If null, this Window becomes a top level window.
     *
     * @return The issued reparent action type (strategy) as defined in Window.ReparentAction
     * @see #reparentWindow(NativeWindow, int, int, boolean)
     */
    ReparentOperation reparentWindow(NativeWindow newParent);

    /**
     * Change this window's parent window.<br>
     * <P>
     * In case the old parent is not null and a Window,
     * this window is removed from it's list of children.<br>
     * In case the new parent is not null and a Window,
     * this window is added to it's list of children.<br></P>
     *
     * @param newParent The new parent NativeWindow. If null, this Window becomes a top level window.
     * @param x new top-level position, use -1 for default position.
     * @param y new top-level position, use -1 for default position.
     * @param forceDestroyCreate if true, uses re-creation strategy for reparenting, default is <code>false</code>.
     * 
     * @return The issued reparent action type (strategy) as defined in Window.ReparentAction
     * @see #reparentWindow(NativeWindow)
     */
    ReparentOperation reparentWindow(NativeWindow newParent, int x, int y, boolean forceDestroyCreate);

    /**
     * Enable or disable fullscreen mode for this window.
     * <p>
     * Fullscreen mode is established on the {@link #getMainMonitor() main monitor}. 
     * </p>
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

    void windowRepaint(int x, int y, int width, int height);

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
     *
     * Appends the given {@link com.jogamp.newt.event.MouseListener} to the end of
     * the list.
     */
    void addMouseListener(MouseListener l);

    /**
     *
     * Inserts the given {@link com.jogamp.newt.event.MouseListener} at the
     * specified position in the list.<br>
     *
     * @param index Position where the listener will be inserted.
     * Should be within (0 <= index && index <= size()).
     * An index value of -1 is interpreted as the end of the list, size().
     * @param l The listener object to be inserted
     * @throws IndexOutOfBoundsException If the index is not within (0 <= index && index <= size()), or -1
     */
    void addMouseListener(int index, MouseListener l);

    void removeMouseListener(MouseListener l);

    MouseListener getMouseListener(int index);

    MouseListener[] getMouseListeners();

}
