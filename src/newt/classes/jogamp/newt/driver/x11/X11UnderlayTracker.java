/**
 * Copyright 2015 JogAmp Community. All rights reserved.
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

package jogamp.newt.driver.x11;

import jogamp.nativewindow.x11.X11Util;
import jogamp.newt.WindowImpl;
import jogamp.newt.driver.MouseTracker;
import jogamp.newt.driver.KeyTracker;

import com.jogamp.common.util.ArrayHashMap;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.nativewindow.Capabilities;
import com.jogamp.nativewindow.GraphicsConfigurationFactory;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.util.Point;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;

/**
 * UnderlayTracker can be used as input for WM that only provide undecorated
 * overlays.
 * 
 * The UnderlayTracker enable move and resize manipulation of the overlays.
 * 
 * A NEWT window may use the UnderlayTracker by calling
 * <code>addWindowListener(X11UnderlayTracker.getSingleton())</code>
 */
public class X11UnderlayTracker implements WindowListener, KeyListener, MouseListener,
                                           MouseTracker, KeyTracker {

    private static final X11UnderlayTracker tracker;
    private volatile WindowImpl focusedWindow = null; // Key events is sent to the focusedWindow
    private volatile MouseEvent lastMouse;
    private volatile static ArrayHashMap<WindowImpl, WindowImpl> underlayWindowMap = new ArrayHashMap<WindowImpl, WindowImpl>(false, ArrayHashMap.DEFAULT_INITIAL_CAPACITY, ArrayHashMap.DEFAULT_LOAD_FACTOR);
    private volatile static ArrayHashMap<WindowImpl, WindowImpl> overlayWindowMap = new ArrayHashMap<WindowImpl, WindowImpl>(false, ArrayHashMap.DEFAULT_INITIAL_CAPACITY, ArrayHashMap.DEFAULT_LOAD_FACTOR);
    private final Display display;
    private final Screen screen;

    static {
        /*
         * X11UnderlayTracker is intended to be used on systems where X11 is not
         * the default WM. We must explicitly initialize all X11 dependencies to
         * make sure they are available.
         */
        X11Util.initSingleton();
        GraphicsConfigurationFactory.initSingleton();
        try {
            ReflectionUtil.callStaticMethod(
                    "jogamp.nativewindow.x11.X11GraphicsConfigurationFactory",
                    "registerFactory", null, null,
                    GraphicsConfigurationFactory.class.getClassLoader());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        tracker = new X11UnderlayTracker();
    }

    public static X11UnderlayTracker getSingleton() {
        return tracker;
    }

    private X11UnderlayTracker() {
        /* 1178 cc10: Fix regression caused by the fix for cc7.
         * We no longer throw an ExceptionInInitializerError
         * when X11 is not available.
         *
         * Fix 1178 cc10: We need to use an X11 resource in the constructor
         * in order to throw an ExceptionInInitializerError if X11 is not available.
         * We can resolve this by query for the
         * X11 display and screen inside the constructor.
         */
        display = NewtFactory.createDisplay(NativeWindowFactory.TYPE_X11, null, false);
        screen = NewtFactory.createScreen(display, 0);
    }

    @Override
    public void windowResized(final WindowEvent e) {
        final Object s = e.getSource();
        if (underlayWindowMap.containsKey(s)) {
            WindowImpl underlayWindow = (WindowImpl)s;
            WindowImpl overlayWindow = underlayWindowMap.get(s);
            if(overlayWindow.getSurfaceWidth() != underlayWindow.getSurfaceWidth() ||
               overlayWindow.getSurfaceHeight() != underlayWindow.getSurfaceHeight()) {
               overlayWindow.setSize(underlayWindow.getSurfaceWidth(),
                                     underlayWindow.getSurfaceHeight());
            }
        } else if (overlayWindowMap.containsKey(s)){
            WindowImpl overlayWindow = (WindowImpl)s;
            WindowImpl underlayWindow = overlayWindowMap.get(s);
            if(overlayWindow.getSurfaceWidth() != underlayWindow.getSurfaceWidth() ||
               overlayWindow.getSurfaceHeight() != underlayWindow.getSurfaceHeight()) {
                underlayWindow.setSize(overlayWindow.getSurfaceWidth(),
                                       overlayWindow.getSurfaceHeight());
            }
        }
    }

    @Override
    public void windowMoved(final WindowEvent e) {
        final Object s = e.getSource();
        if (underlayWindowMap.containsKey(s)) {
            WindowImpl underlayWindow = (WindowImpl)s;
            WindowImpl overlayWindow = underlayWindowMap.get(s);
            Point overlayOnScreen = new Point();
            Point underlayOnScreen = new Point();
            overlayWindow.getLocationOnScreen(overlayOnScreen);
            underlayWindow.getLocationOnScreen(underlayOnScreen);
            if(overlayOnScreen.getX()!=underlayOnScreen.getX() ||
               overlayOnScreen.getY()!=underlayOnScreen.getY()) {
                overlayWindow.setPosition(underlayOnScreen.getX(), underlayOnScreen.getY());
            }
        } else if (overlayWindowMap.containsKey(s)) {
            WindowImpl overlayWindow = (WindowImpl)s;
            WindowImpl underlayWindow = overlayWindowMap.get(s);
            // FIXME: Pressing Maximize on the underlay X11
            // with these lines enabled locks-up the NEWT EDT
            /*
            Point overlayOnScreen = new Point();
            Point underlayOnScreen = new Point();
            overlayWindow.getLocationOnScreen(overlayOnScreen);
            underlayWindow.getLocationOnScreen(underlayOnScreen);
            if(overlayOnScreen.getX()!=underlayOnScreen.getX() ||
               overlayOnScreen.getY()!=underlayOnScreen.getY()) {
                underlayWindow.setPosition(overlayOnScreen.getX(), overlayOnScreen.getY());
            }
            */
           /* it locks up like this
    Caused by: java.lang.RuntimeException: Waited 5000ms for: <5ccc078, 45700941>[count 1, qsz 0, owner <main-Display-.x11_:0-1-EDT-1>] - <main-Display-.x11_:0-2-EDT-1>
    at jogamp.common.util.locks.RecursiveLockImpl01Unfairish.lock(RecursiveLockImpl01Unfairish.java:198)
    at jogamp.nativewindow.ResourceToolkitLock.lock(ResourceToolkitLock.java:56)
    at com.jogamp.nativewindow.DefaultGraphicsDevice.lock(DefaultGraphicsDevice.java:126)
    at jogamp.newt.DisplayImpl.runWithLockedDevice(DisplayImpl.java:780)
    at jogamp.newt.DisplayImpl.runWithLockedDisplayDevice(DisplayImpl.java:793)
    at jogamp.newt.driver.x11.WindowDriver.runWithLockedDisplayDevice(WindowDriver.java:425)
    at jogamp.newt.driver.x11.WindowDriver.getLocationOnScreenImpl(WindowDriver.java:334)
    at jogamp.newt.WindowImpl.getLocationOnScreen(WindowImpl.java:1113)
    at jogamp.newt.driver.x11.X11UnderlayTracker.windowMoved(X11UnderlayTracker.java:153)
    at jogamp.newt.WindowImpl.consumeWindowEvent(WindowImpl.java:4243)
    at jogamp.newt.WindowImpl.sendWindowEvent(WindowImpl.java:4174)
    at jogamp.newt.WindowImpl.positionChanged(WindowImpl.java:4403)
    at jogamp.newt.WindowImpl.sizePosMaxInsetsChanged(WindowImpl.java:4567)
    at jogamp.newt.driver.x11.DisplayDriver.DispatchMessages0(Native Method)
    at jogamp.newt.driver.x11.DisplayDriver.dispatchMessagesNative(DisplayDriver.java:112)
    at jogamp.newt.WindowImpl.waitForPosition(WindowImpl.java:4438)
    at jogamp.newt.WindowImpl.access$2200(WindowImpl.java:96)
    at jogamp.newt.WindowImpl$SetPositionAction.run(WindowImpl.java:2765)
    at com.jogamp.common.util.RunnableTask.run(RunnableTask.java:150)
    at jogamp.newt.DefaultEDTUtil$NEDT.run(DefaultEDTUtil.java:372)
            */
        }
    }

    @Override
    public void windowDestroyNotify(final WindowEvent e) {
        final Object s = e.getSource();
        if (underlayWindowMap.containsKey(s)) {
            WindowImpl underlayWindow = (WindowImpl)s;
            WindowImpl overlayWindow = underlayWindowMap.get(s);
            overlayWindowMap.remove(overlayWindow);
            underlayWindowMap.remove(underlayWindow);
            if (focusedWindow == overlayWindow) {
                focusedWindow = null;
            }
            overlayWindow.destroy();
        } else if (overlayWindowMap.containsKey(s)) {
            WindowImpl overlayWindow = (WindowImpl)s;
            WindowImpl underlayWindow = overlayWindowMap.get(s);
            overlayWindowMap.remove(overlayWindow);
            underlayWindowMap.remove(underlayWindow);
            if (focusedWindow == overlayWindow) {
                focusedWindow = null;
            }
            underlayWindow.destroy();
        }
    }

    @Override
    public void windowDestroyed(final WindowEvent e) {
    }

    @Override
    public void windowGainedFocus(final WindowEvent e) {
        final Object s = e.getSource();
        if (s instanceof WindowImpl) {
            if (underlayWindowMap.containsKey(s)) {
                WindowImpl overlayWindow = underlayWindowMap.get(s);
                focusedWindow = overlayWindow;
            } else if (overlayWindowMap.containsKey(s)) {
                focusedWindow = (WindowImpl) s;
            } else {
                /*
                 * Initialize the X11 under-lay tracker window.
                 * when a new overlay gain focus.
                 */
                WindowImpl overlayWindow = (WindowImpl) s;
                Capabilities caps = new Capabilities();

                /* 1178 cc6: if you render the overlay window transparent -> caps.setBackgroundOpaque(false);
                 *      then you will see that the under-lay tracker window newer repaints -> looks a bit like a mess.
                 * Attempted fix 1178 cc6: x11 under-lay tracker window can be set transparent as well.
                 * FIXME: The under-lay tracker window is still filled with opaque garbage.
                 */
                caps.setBackgroundOpaque(false);

                WindowImpl underlayWindow = WindowImpl.create(null, 0, screen, caps);

                /*
                 * Register overlay and under-lay window in the map's before generating events.
                 */
                underlayWindowMap.put(underlayWindow, overlayWindow);
                overlayWindowMap.put(overlayWindow, underlayWindow);

                /* 1178 cc4: another window overlaps NEWT under-lay window -> overlay window is still on top.
                 * Fix 1178 cc4: we can request the NEWT under-lay window to use always on top.
                 */
                underlayWindow.setAlwaysOnTop(true);

                underlayWindow.setTitle(overlayWindow.getTitle());

                if(overlayWindow.isUndecorated()){
                    underlayWindow.setUndecorated(true);
                }

                underlayWindow.addKeyListener(this);
                underlayWindow.addMouseListener(this);
                underlayWindow.addWindowListener(this);

                underlayWindow.setSize(overlayWindow.getSurfaceWidth(),
                                       overlayWindow.getSurfaceHeight());
                underlayWindow.setPosition(overlayWindow.getX(), overlayWindow.getY());

                underlayWindow.setVisible(false, true);

                focusedWindow = (WindowImpl) s;
            }
        }
    }

    @Override
    public void windowLostFocus(final WindowEvent e) {
        final Object s = e.getSource();
        if (underlayWindowMap.containsKey(s)) {
            WindowImpl overlayWindow = underlayWindowMap.get(s);
            if (focusedWindow == overlayWindow) {
                focusedWindow = null;
            }
        } else {
            if (focusedWindow == s) {
                focusedWindow = null;
            }
        }
    }

    @Override
    public void windowRepaint(final WindowUpdateEvent e) {
    }

    public static void main(String[] args) throws InterruptedException {
        Capabilities caps = new Capabilities();
        caps.setBackgroundOpaque(false);

        Window w = NewtFactory.createWindow(caps);
        w.setUndecorated(true);
        w.addWindowListener(X11UnderlayTracker.getSingleton());
        w.setTitle("1");
        w.setVisible(true);

        w = NewtFactory.createWindow(caps);
        w.setUndecorated(false);
        w.addWindowListener(X11UnderlayTracker.getSingleton());
        w.setTitle("2");
        w.setVisible(true);

        Thread.sleep(25000);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        lastMouse = e;
        final Object s = e.getSource();
        if (underlayWindowMap.containsKey(s)) {
            WindowImpl overlayWindow = underlayWindowMap.get(s);
            overlayWindow.sendMouseEvent(MouseEvent.EVENT_MOUSE_CLICKED, 0,
                                         e.getX(), e.getY(), (short) 0, 0);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        lastMouse = e;
        final Object s = e.getSource();
        if (underlayWindowMap.containsKey(s)) {
            WindowImpl overlayWindow = underlayWindowMap.get(s);
            overlayWindow.sendMouseEvent(MouseEvent.EVENT_MOUSE_ENTERED, 0,
                                         e.getX(), e.getY(), (short) 0, 0);
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        lastMouse = e;
        final Object s = e.getSource();
        if (underlayWindowMap.containsKey(s)) {
            WindowImpl overlayWindow = underlayWindowMap.get(s);
            overlayWindow.sendMouseEvent(MouseEvent.EVENT_MOUSE_EXITED, 0,
                                         e.getX(), e.getY(), (short) 0, 0);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        lastMouse = e;
        final Object s = e.getSource();
        if (underlayWindowMap.containsKey(s)) {
            WindowImpl overlayWindow = underlayWindowMap.get(s);
            overlayWindow.sendMouseEvent(MouseEvent.EVENT_MOUSE_PRESSED, 0,
                                         e.getX(), e.getY(), (short) 0, 0);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        lastMouse = e;
        final Object s = e.getSource();
        if (underlayWindowMap.containsKey(s)) {
            WindowImpl overlayWindow = underlayWindowMap.get(s);
            overlayWindow.sendMouseEvent(MouseEvent.EVENT_MOUSE_RELEASED, 0,
                                         e.getX(), e.getY(), (short) 0, 0);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        lastMouse = e;
        final Object s = e.getSource();
        if (underlayWindowMap.containsKey(s)) {
            WindowImpl overlayWindow = underlayWindowMap.get(s);
            overlayWindow.sendMouseEvent(MouseEvent.EVENT_MOUSE_MOVED, 0,
                                         e.getX(), e.getY(), (short) 0, 0);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        lastMouse = e;
        final Object s = e.getSource();
        if (underlayWindowMap.containsKey(s)) {
            WindowImpl overlayWindow = underlayWindowMap.get(s);
            overlayWindow.sendMouseEvent(MouseEvent.EVENT_MOUSE_DRAGGED, 0,
                                         e.getX(), e.getY(), (short) 0, 0);
        }
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        lastMouse = e;
        final Object s = e.getSource();
        if (underlayWindowMap.containsKey(s)) {
            WindowImpl overlayWindow = underlayWindowMap.get(s);
            overlayWindow.sendMouseEvent(MouseEvent.EVENT_MOUSE_WHEEL_MOVED, 0,
                                         e.getX(), e.getY(), (short) 0, 0);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (focusedWindow != null) {
            focusedWindow.sendKeyEvent(e.getEventType(), e.getModifiers(),
                                       e.getKeyCode(), e.getKeySymbol(), e.getKeyChar());
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (focusedWindow != null) {
            focusedWindow.sendKeyEvent(e.getEventType(), e.getModifiers(),
                                       e.getKeyCode(), e.getKeySymbol(), e.getKeyChar());
        }
    }

    @Override
    public int getLastY() {
        if (lastMouse != null)
            return lastMouse.getY();
        return 0;
    }

    @Override
    public int getLastX() {
        if (lastMouse != null)
            return lastMouse.getX();
        return 0;
    }
}
