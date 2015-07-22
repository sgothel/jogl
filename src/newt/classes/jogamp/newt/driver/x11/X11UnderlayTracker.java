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

import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.nativewindow.Capabilities;
import com.jogamp.nativewindow.GraphicsConfigurationFactory;
import com.jogamp.nativewindow.NativeWindowFactory;
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
 * UnderlayTracker can be used as input for
 * WM that only provide undecorated overlays.
 * 
 * The UnderlayTracker enable move and resize
 * manipulation of the overlays.
 * 
 * A NEWT window may use the UnderlayTracker by calling
 * <code>addWindowListener(X11UnderlayTracker.getSingleton())</code>
 */
public class X11UnderlayTracker implements WindowListener, KeyListener, MouseListener, MouseTracker, KeyTracker {

    private static final X11UnderlayTracker tracker;
    private static Window window;
    private volatile WindowImpl focusedWindow = null;
	private volatile MouseEvent lastMouse;

    static {
    	tracker = new X11UnderlayTracker();
    }

    public static X11UnderlayTracker getSingleton() {
        return tracker;
    }
    
    private X11UnderlayTracker() {
    	
    	/* 
    	 * X11UnderlayTracker is intended to be used on systems where
    	 * X11 is not the default WM.
    	 * We must explicitly initialize all X11 dependencies
    	 * to make sure they are available.
    	 */
    	X11Util.initSingleton();
    	GraphicsConfigurationFactory.initSingleton();
    	try {
             ReflectionUtil.callStaticMethod("jogamp.nativewindow.x11.X11GraphicsConfigurationFactory",
                                             "registerFactory", null, null, GraphicsConfigurationFactory.class.getClassLoader());
        } catch (final Exception e) {
                throw new RuntimeException(e);
        }
    	
    	/*
    	 * Initialize the X11 window.
    	 */
    	Capabilities caps = new Capabilities();
        final Display display = NewtFactory.createDisplay(NativeWindowFactory.TYPE_X11, null, false);
        final Screen screen  = NewtFactory.createScreen(display, 0);
             	
    	window = WindowImpl.create(null, 0, screen, caps);
        //window.setSize(200, 140);
        //window.setPosition(300, 400);
        window.setVisible(false, true);
        
    	window.addKeyListener(this);
        window.addMouseListener(this);
        window.addWindowListener(this);
    }



    @Override
    public void windowResized(final WindowEvent e) {
    	final Object s = e.getSource();
    	
    	if(s instanceof WindowImpl) {
    		if(window == s) {
    			if(focusedWindow!=null){
    				focusedWindow.setSize(window.getSurfaceWidth(),window.getSurfaceHeight());
    				// workaround bvm.vc.iv possition gets moved during setSize
    				focusedWindow.setPosition(window.getX(),window.getY());
    			}
    		} else {
    			// FIXME null check here used as a workaround to prevent event avalance
    			// fixing it will allow the user to resize the NEWT window using code
    			// after it has gained focus.
    			if(focusedWindow==null){
    				WindowImpl sourceWindow = (WindowImpl) s;
    				window.setSize(sourceWindow.getSurfaceWidth(),sourceWindow.getSurfaceHeight());
    			}
    		}
        }
    }

    @Override
    public void windowMoved(final WindowEvent e) {
    	final Object s = e.getSource();
    	if(s instanceof WindowImpl) {
    		if(window == s) {
    			if(focusedWindow!=null){
    				//focusedWindow.setSize(window.getSurfaceWidth(),window.getSurfaceHeight());
    				focusedWindow.setPosition(window.getX(), window.getY());
    			}
    		} else {
    			// FIXME null check here used as a workaround to prevent event avalance
    			// fixing it will allow the user to move the NEWT window using code
    			// after it has gained focus.
    			if(focusedWindow==null){
    				WindowImpl sourceWindow = (WindowImpl) s;
    				window.setPosition(sourceWindow.getX(), sourceWindow.getY());
    			}
    		}
        }
    }

    @Override
    public void windowDestroyNotify(final WindowEvent e) {
        final Object s = e.getSource();
        
        if(window == s) {
    		if(focusedWindow!=null){
    			focusedWindow.destroy();
    			focusedWindow = null;
    		}
    	} else {
    		if(focusedWindow == s) {
    			focusedWindow = null;
    			window.setVisible(false, false);
    			window.destroy();
    		}
    	}
    }

    @Override
    public void windowDestroyed(final WindowEvent e) { }

    @Override
    public void windowGainedFocus(final WindowEvent e) {
        final Object s = e.getSource();
        if(s instanceof WindowImpl) {
        	if(window == s) {
        		// do nothing
        	} else {
        		if(focusedWindow==null) {
        			// hack that initially make the tracking window the same size as the overlay
        			WindowImpl sourceWindow = (WindowImpl) s;
            		window.setSize(sourceWindow.getSurfaceWidth(),sourceWindow.getSurfaceHeight());
            		window.setPosition(sourceWindow.getX(), sourceWindow.getY());
        		}
        		focusedWindow = (WindowImpl) s;
        	}
        }
    }

    @Override
    public void windowLostFocus(final WindowEvent e) {
        final Object s = e.getSource();
    	if(window == s) {
    		// do nothing
    	} else {
    		if(focusedWindow == s) {
    			focusedWindow = null;
    		}
    	}
    }

    @Override
    public void windowRepaint(final WindowUpdateEvent e) { }
    
    public static void main(String[] args) throws InterruptedException{
    	X11UnderlayTracker.getSingleton();
        
    	Thread.sleep(25000);
    }

	@Override
	public void mouseClicked(MouseEvent e) {
		lastMouse = e;
		if(focusedWindow != null){
			//e.setConsumed(false);
			//focusedWindow.consumeEvent(e);
			focusedWindow.sendMouseEvent(MouseEvent.EVENT_MOUSE_CLICKED, 0, e.getX(), e.getY(), (short)0, 0 );
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		lastMouse = e;
		if(focusedWindow != null){
			//e.setConsumed(false);
			//focusedWindow.consumeEvent(e);
			focusedWindow.sendMouseEvent(MouseEvent.EVENT_MOUSE_ENTERED, 0, e.getX(), e.getY(), (short)0, 0 );
		}
	}

	@Override
	public void mouseExited(MouseEvent e) {
		lastMouse = e;
		if(focusedWindow != null){
			//e.setConsumed(false);
			//focusedWindow.consumeEvent(e);
			focusedWindow.sendMouseEvent(MouseEvent.EVENT_MOUSE_EXITED, 0, e.getX(), e.getY(), (short)0, 0 );
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		lastMouse = e;
		if(focusedWindow != null){
            //e.setConsumed(false);
			//focusedWindow.consumeEvent(e);
			focusedWindow.sendMouseEvent(MouseEvent.EVENT_MOUSE_PRESSED, 0, e.getX(), e.getY(), (short)0, 0 );
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		lastMouse = e;
		if(focusedWindow != null){
			//e.setConsumed(false);
			//focusedWindow.consumeEvent(e);
			focusedWindow.sendMouseEvent(MouseEvent.EVENT_MOUSE_RELEASED, 0, e.getX(), e.getY(), (short)0, 0 );
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		lastMouse = e;
		if(focusedWindow != null){
			//e.setConsumed(false);
			//focusedWindow.consumeEvent(e);
			focusedWindow.sendMouseEvent(MouseEvent.EVENT_MOUSE_MOVED, 0, e.getX(), e.getY(), (short)0, 0 );
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		lastMouse = e;
		if(focusedWindow != null){
			//e.setConsumed(false);
			//focusedWindow.consumeEvent(e);
			focusedWindow.sendMouseEvent(MouseEvent.EVENT_MOUSE_DRAGGED, 0, e.getX(), e.getY(), (short)0, 0 );
		}
	}

	@Override
	public void mouseWheelMoved(MouseEvent e) {
		lastMouse = e;
		if(focusedWindow != null){
			//e.setConsumed(false);
			//focusedWindow.consumeEvent(e);
			focusedWindow.sendMouseEvent(MouseEvent.EVENT_MOUSE_WHEEL_MOVED, 0, e.getX(), e.getY(), (short)0, 0 );
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if(focusedWindow != null){
			//e.setConsumed(false);
			//focusedWindow.consumeEvent(e);
			focusedWindow.sendKeyEvent(e.getEventType(), e.getModifiers(), e.getKeyCode(), e.getKeyCode(), (char)e.getKeySymbol());
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if(focusedWindow != null){
			//e.setConsumed(false);
			//focusedWindow.consumeEvent(e);
			focusedWindow.sendKeyEvent(e.getEventType(), e.getModifiers(), e.getKeyCode(), e.getKeyCode(), (char)e.getKeySymbol());
		}
	}

	@Override
	public int getLastY() {
        if(lastMouse!=null)
            return lastMouse.getY();
		return 0;
	}

	@Override
	public int getLastX() {
        if(lastMouse!=null)
            return lastMouse.getX();	
		return 0;
	}
}
