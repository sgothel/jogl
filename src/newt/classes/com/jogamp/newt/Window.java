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

import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.MouseListener;
import jogamp.newt.Debug;
import javax.media.nativewindow.CapabilitiesChooser;
import javax.media.nativewindow.CapabilitiesImmutable;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.WindowClosingProtocol;

/**
 * Specifying the public Window functionality for the
 * using a Window and for shadowing one like {@link com.jogamp.newt.opengl.GLWindow}.
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
     * @return The associated Screen
     */
    Screen getScreen();

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
     * Destroy the Window and it's children, incl. native destruction.<br>
     * The Window can be recreate via {@link #setVisible(boolean) setVisible(true)}.
     * <p>Visibility is set to false.</p>
     * <p>
     * This method invokes {@link Screen#removeReference()} after it's own destruction,<br>
     * which will issue {@link Screen#destroy()} if the reference count becomes 0.<br>
     * This destruction sequence shall end up in {@link Display#destroy()}, if all reference counts become 0.
     * </p>
     * @see #destroy()
     * @see #setVisible(boolean)
     */
    void destroy();

    /**
     * <p>
     * <code>setVisible</code> makes the window and children visible if <code>visible</code> is true,
     * otherwise the window and children becomes invisible.<br></p>
     * <p>
     * The <code>setVisible(true)</code> is responsible to actual create the native window.<br></p>
     * <p>
     * Zero size semantics are respected, see {@link #setSize(int,int)}:<br>
     * <pre>
     * if ( 0 == windowHandle && visible ) {
     *   this.visible = visible;
     *   if( 0 &lt; width*height ) {
     *     createNative();
     *   }
     * } else if ( this.visible != visible ) {
     *   this.visible = visible;
     *   setNativeSizeImpl();
     * }
     * </pre></p>
     * <p>
     * In case this window is a child window and a parent {@link javax.media.nativewindow.NativeWindow} is being used,<br>
     * the parent's {@link javax.media.nativewindow.NativeWindow} handle is retrieved via {@link javax.media.nativewindow.NativeWindow#getWindowHandle()}.<br>
     * If this action fails, ie if the parent {@link javax.media.nativewindow.NativeWindow} is not valid yet,<br>
     * no native window is created yet and <code>setVisible(true)</code> shall be repeated when it is.<br></p>
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
     * if ( 0 != windowHandle && 0 &ge; width*height && visible ) {
     *   setVisible(false);
     * } else if ( 0 == windowHandle && 0 &lt; width*height && visible ) {
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

    boolean isPointerVisible();
    
    /**
     * Makes the pointer visible or invisible.
     * 
     * @param pointerVisible defaults to <code>true</code> for platforms w/ visible pointer,
     *                       otherwise defaults to <code>true</code>, eg. Android.
     * @see #confinePointer(boolean)
     */
    void setPointerVisible(boolean pointerVisible);

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
    
    /** Defining ids for the reparenting strategy */
    public interface ReparentAction {
        /** No native reparenting valid */
        static final int ACTION_INVALID = -1;

        /** No native reparenting action required, no change*/
        static final int ACTION_UNCHANGED = 0;

        /** Native reparenting incl. Window tree */
        static final int ACTION_NATIVE_REPARENTING = 1;

        /** Native window creation after tree change - instead of reparenting. */
        static final int ACTION_NATIVE_CREATION = 2;

        /** Change Window tree only, native creation is pending */
        static final int ACTION_NATIVE_CREATION_PENDING = 3;
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
     */
    int reparentWindow(NativeWindow newParent);

    int reparentWindow(NativeWindow newParent, boolean forceDestroyCreate);

    boolean setFullscreen(boolean fullscreen);
    
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
     * allowing to suppress the {@link KeyEvent} via the {@link InputEvent#consumedTag}.
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

    public void sendWindowEvent(int eventType);

    /**
     *
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
