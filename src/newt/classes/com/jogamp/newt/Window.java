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
import com.jogamp.newt.event.MouseListener;
import jogamp.newt.Debug;
import javax.media.nativewindow.CapabilitiesChooser;
import javax.media.nativewindow.CapabilitiesImmutable;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.SurfaceUpdatedListener;
import javax.media.nativewindow.WindowClosingProtocol;
import javax.media.nativewindow.util.Insets;

/**
 * Specifying the public Window functionality for the
 * using a Window and for shadowing one like {@link com.jogamp.newt.opengl.GLWindow}.
 */
public interface Window extends NativeWindow, WindowClosingProtocol {
    public static final boolean DEBUG_MOUSE_EVENT = Debug.debug("Window.MouseEvent");
    public static final boolean DEBUG_KEY_EVENT = Debug.debug("Window.KeyEvent");
    public static final boolean DEBUG_WINDOW_EVENT = Debug.debug("Window.WindowEvent");
    public static final boolean DEBUG_IMPLEMENTATION = Debug.debug("Window");

    /** A 1s timeout while waiting for a native action response, ie {@link #setVisible(boolean)}. */
    public static final long TIMEOUT_NATIVEWINDOW = 1000;

    //
    // Lifecycle
    //

    /**
     * @return True if native window is valid, can be created or recovered.
     * Otherwise false, ie this window is unrecoverable due to a <code>destroy(true)</code> call.
     *
     * @see #destroy(boolean)
     * @see #setVisible(boolean)
     */
    boolean isValid();

    /**
     * @return true if the native window handle is valid and ready to operate, ie
     * if the native window has been created, otherwise false.
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
     * this.visible = visible;
     * if( 0<width*height ) {
     * createNative();
     * }
     * } else if ( this.visible != visible ) {
     * this.visible = visible;
     * setNativeSizeImpl();
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

    //
    // Child Window Management
    // 

    void addChild(NativeWindow win);

    void removeChild(NativeWindow win);

    //
    // Modes / States
    //

    /**
     * Sets the size of the client area of the window, excluding decorations
     * Total size of the window will be
     * {@code width+insets.left+insets.right, height+insets.top+insets.bottom}<br>
     * <p>
     * Zero size semantics are respected, see {@link #setVisible(boolean)}:<br>
     * <pre>
     * if ( 0 != windowHandle && 0>=width*height && visible ) {
     * setVisible(false);
     * } else if ( 0 == windowHandle && 0<width*height && visible ) {
     * setVisible(true);
     * } else {
     * // as expected ..
     * }
     * </pre></p>
     * <p>
     * This call is ignored if in fullscreen mode.<br></p>
     *
     * @param width of the client area of the window
     * @param height of the client area of the window
     */
    void setSize(int width, int height);

    /**
     * Returns the width of the client area of this window
     * @return width of the client area
     */
    int getWidth();

    /**
     * Returns the height of the client area of this window
     * @return height of the client area
     */
    int getHeight();

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

    /**
     * Sets the location of the top left corner of the window, including
     * decorations (so the client area will be placed at
     * {@code x+insets.left,y+insets.top}.<br>
     *
     * This call is ignored if in fullscreen mode.<br>
     *
     * @param x coord of the top left corner
     * @param y coord of the top left corner
     */
    void setPosition(int x, int y);

    int getX();

    int getY();

    /**
     * Returns the insets for this native window (the difference between the
     * size of the toplevel window with the decorations and the client area).
     *
     * @return insets for this platform window
     */
    Insets getInsets();

    void setUndecorated(boolean value);
    
    boolean isUndecorated();
    
    void setTitle(String title);

    String getTitle();

    static interface FocusRunnable {
        /**
         * @return false if NEWT shall proceed requesting the focus,
         * true if NEWT shall not request the focus.
         */
        public boolean run();
    }

    /**
     * May set to a {@link FocusRunnable}, {@link FocusRunnable#run()} before Newt requests the native focus.
     * This allows notifying a covered window toolkit like AWT that the focus is requested,
     * hence focus traversal can be made transparent.
     */
    void setFocusAction(FocusRunnable focusAction);

    void requestFocus();

    boolean hasFocus();

    void windowRepaint(int x, int y, int width, int height);

    void enqueueEvent(boolean wait, com.jogamp.newt.event.NEWTEvent event);

    void runOnEDTIfAvail(boolean wait, final Runnable task);


    //
    // SurfaceUpdateListener
    //

    /**
     * Appends the given {@link com.jogamp.newt.event.SurfaceUpdatedListener} to the end of
     * the list.
     */
    void addSurfaceUpdatedListener(SurfaceUpdatedListener l);

    /**
     *
     * Inserts the given {@link com.jogamp.newt.event.SurfaceUpdatedListener} at the
     * specified position in the list.<br>
     *
     * @param index Position where the listener will be inserted.
     * Should be within (0 <= index && index <= size()).
     * An index value of -1 is interpreted as the end of the list, size().
     * @param l The listener object to be inserted
     * @throws IndexOutOfBoundsException If the index is not within (0 <= index && index <= size()), or -1
     */
    void addSurfaceUpdatedListener(int index, SurfaceUpdatedListener l) throws IndexOutOfBoundsException;

    void removeAllSurfaceUpdatedListener();

    void removeSurfaceUpdatedListener(SurfaceUpdatedListener l);

    SurfaceUpdatedListener getSurfaceUpdatedListener(int index);

    SurfaceUpdatedListener[] getSurfaceUpdatedListeners();


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
