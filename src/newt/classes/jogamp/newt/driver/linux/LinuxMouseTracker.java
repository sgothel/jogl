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

package jogamp.newt.driver.linux;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import jogamp.newt.WindowImpl;
import jogamp.newt.driver.MouseTracker;

import com.jogamp.common.util.InterruptSource;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;

/**
 * Experimental native mouse tracker thread for GNU/Linux
 * just reading <code>/dev/input/mice</code>
 * within it's own polling thread.
 */
public class LinuxMouseTracker implements WindowListener, MouseTracker {

    private static final LinuxMouseTracker lmt;

    static {
        lmt = new LinuxMouseTracker();
        final Thread t = new InterruptSource.Thread(null, lmt.mouseDevicePoller, "NEWT-LinuxMouseTracker");
        t.setDaemon(true);
        t.start();
    }

    public static LinuxMouseTracker getSingleton() {
        return lmt;
    }

    private volatile boolean stop = false;
    private int x = 0;
    private int y = 0;
    private short buttonDown = 0;
    private int old_x = 0;
    private int old_y = 0;
    private volatile int lastFocusedX = 0;
    private volatile int lastFocusedY = 0;
    private short old_buttonDown = 0;
    private WindowImpl focusedWindow = null;
    private final MouseDevicePoller mouseDevicePoller = new MouseDevicePoller();

    public final int getLastX() { return lastFocusedX; }
    public final int getLastY() { return lastFocusedY; }

    @Override
    public void windowResized(final WindowEvent e) { }

    @Override
    public void windowMoved(final WindowEvent e) { }

    @Override
    public void windowDestroyNotify(final WindowEvent e) {
        final Object s = e.getSource();
        if(focusedWindow == s) {
            focusedWindow = null;
        }
    }

    @Override
    public void windowDestroyed(final WindowEvent e) { }

    @Override
    public void windowGainedFocus(final WindowEvent e) {
        final Object s = e.getSource();
        if(s instanceof WindowImpl) {
            focusedWindow = (WindowImpl) s;
        }
    }

    @Override
    public void windowLostFocus(final WindowEvent e) {
        final Object s = e.getSource();
        if(focusedWindow == s) {
            focusedWindow = null;
        }
    }

    @Override
    public void windowRepaint(final WindowUpdateEvent e) { }

    class MouseDevicePoller implements Runnable {
        @Override
        public void run() {
            final byte[] b = new byte[3];
            final File f = new File("/dev/input/mice");
            f.setReadOnly();
            InputStream fis;
            try {
                fis = new FileInputStream(f);
            } catch (final FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
            }
            int xd=0,yd=0; //x/y movement delta
            boolean xo=false,yo=false; // x/y overflow (out of range -255 to +255)
            boolean lb=false,mb=false,rb=false,hs=false,vs=false; //left/middle/right mousebutton
            while(!stop) {
                int remaining=3;
                while(remaining>0) {
                    int read = 0;
                    try {
                        read = fis.read(b, 0, remaining);
                    } catch (final IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    if(read<0) {
                        stop = true; // EOF of mouse !?
                    } else {
                        remaining -= read;
                    }
                }
                lb=(b[0]&1)>0;
                rb=(b[0]&2)>0;
                mb=(b[0]&4)>0;
                hs=(b[0]&16)>0;
                vs=(b[0]&32)>0;
                xo=(b[0]&64)>0;
                yo=(b[0]&128)>0;
                xd=b[1];
                yd=b[2];

                x+=xd;
                y-=yd;

                if(x<0) {
                    x=0;
                }
                if(y<0) {
                    y=0;
                }

                buttonDown = 0;
                if(lb) {
                    buttonDown = MouseEvent.BUTTON1;
                }
                if(mb) {
                    buttonDown = MouseEvent.BUTTON2;
                }
                if(rb) {
                    buttonDown = MouseEvent.BUTTON3;
                }

                if(null != focusedWindow) {
                    // Clip to Screen Size
                    {
                        final Screen focusedScreen = focusedWindow.getScreen();
                        final int sw = focusedScreen.getWidth();
                        final int sh = focusedScreen.getHeight();
                        if( x >= sw ) {
                            x = sw - 1;
                        }
                        if( y >= sh ) {
                            y = sh - 1;
                        }
                    }
                    final int[] winScreenPos = focusedWindow.convertToPixelUnits(new int[] { focusedWindow.getX(), focusedWindow.getY() });
                    final int wx = x - winScreenPos[0], wy = y - winScreenPos[1];
                    if(old_x != x || old_y != y) {
                        // mouse moved
                        lastFocusedX = wx;
                        lastFocusedY = wy;
                        focusedWindow.sendMouseEvent(MouseEvent.EVENT_MOUSE_MOVED, 0, wx, wy, (short)0, 0 );
                    }

                    if(old_buttonDown != buttonDown) {
                        // press/release
                        if( 0 != buttonDown ) {
                            focusedWindow.sendMouseEvent(MouseEvent.EVENT_MOUSE_PRESSED, 0, wx, wy, buttonDown, 0 );
                        } else {
                            focusedWindow.sendMouseEvent(MouseEvent.EVENT_MOUSE_RELEASED, 0, wx, wy, old_buttonDown, 0 );
                        }
                    }
                } else {
                    if(Window.DEBUG_MOUSE_EVENT) {
                        System.out.println(x+"/"+y+", hs="+hs+",vs="+vs+",lb="+lb+",rb="+rb+",mb="+mb+",xo="+xo+",yo="+yo+"xd="+xd+",yd="+yd);
                    }
                }

                old_x = x;
                old_y = y;
                old_buttonDown = buttonDown;

            }
            if(null != fis) {
                try {
                    fis.close();
                } catch (final IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
}
