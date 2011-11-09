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
 

package com.jogamp.newt.awt;

import java.awt.Canvas;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.KeyboardFocusManager;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.media.nativewindow.Capabilities;
import javax.media.nativewindow.CapabilitiesImmutable;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeSurfaceHolder;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.NativeWindowHolder;
import javax.media.nativewindow.WindowClosingProtocol;
import javax.media.nativewindow.awt.AWTWindowClosingProtocol;
import javax.swing.MenuSelectionManager;

import jogamp.nativewindow.awt.AWTMisc;
import jogamp.newt.Debug;
import jogamp.newt.awt.event.AWTParentWindowAdapter;
import jogamp.newt.awt.event.NewtFactoryAWT;

import com.jogamp.newt.Display;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.awt.AWTAdapter;
import com.jogamp.newt.event.awt.AWTKeyAdapter;
import com.jogamp.newt.event.awt.AWTMouseAdapter;

@SuppressWarnings("serial")
public class NewtCanvasAWT extends java.awt.Canvas implements WindowClosingProtocol, NativeSurfaceHolder, NativeWindowHolder {
    public static final boolean DEBUG = Debug.debug("Window");

    NativeWindow nativeWindow = null;
    Window newtChild = null;
    boolean isNewtChildOnscreen = true;
    int newtChildCloseOp;
    AWTAdapter awtAdapter = null;
    AWTAdapter awtMouseAdapter = null;
    AWTAdapter awtKeyAdapter = null;
    
    private AWTWindowClosingProtocol awtWindowClosingProtocol =
          new AWTWindowClosingProtocol(this, new Runnable() {
                public void run() {
                    NewtCanvasAWT.this.destroy();
                }
            });

    /**
     * Instantiates a NewtCanvas without a NEWT child.<br>
     */
    public NewtCanvasAWT() {
        super();
        createNativeWindow(new Capabilities());
    }

    /**
     * Instantiates a NewtCanvas without a NEWT child.<br>
     */
    public NewtCanvasAWT(GraphicsConfiguration gc) {
        super(gc);
        createNativeWindow(new Capabilities());
    }

    /**
     * Instantiates a NewtCanvas without a NEWT child.<br>
     */
    public NewtCanvasAWT(CapabilitiesImmutable caps) {
        super();
        createNativeWindow(caps);
    }

    /**
     * Instantiates a NewtCanvas without a NEWT child.<br>
     */
    public NewtCanvasAWT(GraphicsConfiguration gc, CapabilitiesImmutable caps) {
        super(gc);
        createNativeWindow(caps);
    }

    /**
     * Instantiates a NewtCanvas with a NEWT child.
     */
    public NewtCanvasAWT(Window child) {
        super();
        createNativeWindow(child.getRequestedCapabilities());
        setNEWTChild(child);
    }

    /**
     * Instantiates a NewtCanvas with a NEWT child.
     */
    public NewtCanvasAWT(GraphicsConfiguration gc, Window child) {
        super(gc);
        createNativeWindow(child.getRequestedCapabilities());
        setNEWTChild(child);
    }
    
    private final void createNativeWindow(CapabilitiesImmutable caps) {
        nativeWindow = NewtFactoryAWT.getNativeWindow(this, caps);
    }
    
    class FocusAction implements Window.FocusRunnable {
        public boolean run() {
            if ( EventQueue.isDispatchThread() ) {
                focusActionImpl.run();
            } else {
                try {
                    EventQueue.invokeAndWait(focusActionImpl);
                } catch (Exception e) {
                    throw new NativeWindowException(e);
                }
                /**
                // wait for AWT focus !
                for(long sleep = Window.TIMEOUT_NATIVEWINDOW; 0<sleep && !isFocusOwner(); sleep-=10 ) {
                    try { Thread.sleep(10); } catch (InterruptedException e) { }
                } */
            }
            return focusActionImpl.result;
        }

        class FocusActionImpl implements Runnable {
            public final boolean result = false; // NEWT shall always proceed requesting the native focus
            public void run() {
                if(DEBUG) {
                    System.err.println("FocusActionImpl.run() "+Display.getThreadName());
                }
                NewtCanvasAWT.this.requestFocusAWTParent();
                if(isNewtChildOnscreen) {                    
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                }
            }
        }
        FocusActionImpl focusActionImpl = new FocusActionImpl();
    }
    FocusAction focusAction = new FocusAction();
    
    WindowListener clearAWTMenusOnNewtFocus = new WindowAdapter() {
          @Override
          public void windowGainedFocus(WindowEvent arg0) {
                  MenuSelectionManager.defaultManager().clearSelectedPath();
          }
    };

    /** sets a new NEWT child, provoking reparenting. */
    private NewtCanvasAWT setNEWTChild(Window child) {
        if(newtChild!=child) {
            newtChild = child;
            if(isDisplayable()) {
                // reparent right away, addNotify has been called already
                final java.awt.Container cont = AWTMisc.getContainer(this);
                reparentWindow( (null!=child) ? true : false, cont );
            }
        }
        return this;
    }

    /** @return the current NEWT child */
    public Window getNEWTChild() {
        return newtChild;
    }

    /** @return this AWT Canvas NativeWindow representation, may be null in case {@link #removeNotify()} has been called,
     * or {@link #addNotify()} hasn't been called yet.*/
    public NativeWindow getNativeWindow() { return nativeWindow; }
    
    /** See {@link #getNativeWindow()} */    
    public NativeSurface getNativeSurface() { return nativeWindow; }

    public int getDefaultCloseOperation() {
        return awtWindowClosingProtocol.getDefaultCloseOperation();
    }

    public int setDefaultCloseOperation(int op) {
        return awtWindowClosingProtocol.setDefaultCloseOperation(op);
    }

    private final void configureNewtChildInputEventHandler() {
        if(null==awtMouseAdapter && null != newtChild.getGraphicsConfiguration()) {
            isNewtChildOnscreen = newtChild.getGraphicsConfiguration().getChosenCapabilities().isOnscreen();
            if(!isNewtChildOnscreen) {
                // offscreen childs require AWT event fwd for key/mouse
                awtMouseAdapter = new AWTMouseAdapter(newtChild).addTo(this);
                awtKeyAdapter = new AWTKeyAdapter(newtChild).addTo(this);
            }
        }
    }
    
    /* package */ void configureNewtChild(boolean attach) {
        if(null!=awtAdapter) {
          awtAdapter.removeFrom(this);
          awtAdapter=null;
        }
        if(null!=awtMouseAdapter) {
            awtMouseAdapter.removeFrom(this);
            awtMouseAdapter = null;
        }
        if(null!=awtKeyAdapter) {
            awtKeyAdapter.removeFrom(this);
            awtKeyAdapter = null;
        }
        
        if( null != newtChild ) {
            if(attach) {
                awtAdapter = new AWTParentWindowAdapter(newtChild).addTo(this);
                newtChild.addWindowListener(clearAWTMenusOnNewtFocus);
                newtChild.setFocusAction(focusAction); // enable AWT focus traversal
                newtChildCloseOp = newtChild.setDefaultCloseOperation(WindowClosingProtocol.DO_NOTHING_ON_CLOSE);
                awtWindowClosingProtocol.addClosingListenerOneShot();
            } else {
                newtChild.removeWindowListener(clearAWTMenusOnNewtFocus);
                newtChild.setFocusAction(null);
                newtChild.setDefaultCloseOperation(newtChildCloseOp);
                awtWindowClosingProtocol.removeClosingListener();
            }
        }
    }

    @Override
    public void addNotify() {

        // before native peer is valid: X11
        disableBackgroundErase();

        // creates the native peer
        super.addNotify();

        // after native peer is valid: Windows
        disableBackgroundErase();

        java.awt.Container cont = AWTMisc.getContainer(this);
        if(DEBUG) {
            // if ( isShowing() == false ) -> Container was not visible yet.
            // if ( isShowing() == true  ) -> Container is already visible.
            System.err.println("NewtCanvasAWT.addNotify: "+newtChild+", "+this+", visible "+isVisible()+", showing "+isShowing()+
                               ", displayable "+isDisplayable()+" -> "+cont);
        }  
        reparentWindow(true, cont);
    }

    @Override
    public void removeNotify() {
        java.awt.Container cont = AWTMisc.getContainer(this);
        if(DEBUG) {
            System.err.println("NewtCanvasAWT.removeNotify: "+newtChild+", from "+cont);
        }
        reparentWindow(false, cont);
        super.removeNotify();
    }

    void reparentWindow(boolean add, java.awt.Container cont) {
      if(null==newtChild) {
        return; // nop
      }

      newtChild.setFocusAction(null); // no AWT focus traversal ..
      if(add) {
          NewtFactoryAWT.updateGraphicsConfiguration(nativeWindow, this);
          if(DEBUG) {
            System.err.println("NewtCanvasAWT.reparentWindow: "+newtChild);
          }
          final int w;
          final int h;
          if(isPreferredSizeSet()) {
             java.awt.Dimension d = getPreferredSize();
             w = d.width;
             h = d.height;
          } else {
             final java.awt.Dimension min;
             if(this.isMinimumSizeSet()) {
                 min = getMinimumSize();
             } else {
                 min = new java.awt.Dimension(0, 0);
             }
             java.awt.Insets ins = cont.getInsets();
             w = Math.max(min.width, cont.getWidth() - ins.left - ins.right);
             h = Math.max(min.height, cont.getHeight() - ins.top - ins.bottom);
          }
          setSize(w, h);
          newtChild.setSize(w, h);
          newtChild.reparentWindow(nativeWindow);
          newtChild.setVisible(true);
          configureNewtChild(true);
          newtChild.sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED); // trigger a resize/relayout to listener
          newtChild.windowRepaint(0, 0, w, h);
      } else {
          configureNewtChild(false);
          nativeWindow = null;
          newtChild.setVisible(false);
          newtChild.reparentWindow(null);
      }
    }

    /**
     * Destroys this resource:
     * <ul>
     *   <li> Make the NEWT Child invisible </li>
     *   <li> Disconnects the NEWT Child from this Canvas NativeWindow, reparent to NULL </li>
     *   <li> Issues <code>destroy()</code> on the NEWT Child</li>
     *   <li> Remove reference to the NEWT Child</li>
     *   <li> Remove this Canvas from it's parent.</li>
     * </ul>
     * @see Window#destroy()
     */
    public final void destroy() {
        if(null!=newtChild) {
            java.awt.Container cont = AWTMisc.getContainer(this);
            if(DEBUG) {
                System.err.println("NewtCanvasAWT.destroy(): "+newtChild+", from "+cont);
            }
            configureNewtChild(false);
            if(null!=nativeWindow) {
                nativeWindow.destroy();
                nativeWindow=null;
            }
            newtChild.setVisible(false);
            newtChild.reparentWindow(null);
            newtChild.destroy();
            newtChild=null;
            if(null!=cont) {
                cont.remove(this);
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        awtWindowClosingProtocol.addClosingListenerOneShot();
        if(null!=newtChild) {
            configureNewtChildInputEventHandler();
            newtChild.windowRepaint(0, 0, getWidth(), getHeight());
        }
    }
    @Override
    public void update(Graphics g) {
        awtWindowClosingProtocol.addClosingListenerOneShot();
        if(null!=newtChild) {
            configureNewtChildInputEventHandler();
            newtChild.windowRepaint(0, 0, getWidth(), getHeight());
        }
    }

    private final void requestFocusAWTParent() {
        super.requestFocusInWindow();
    }

    private final void requestFocusNEWTChild() {
        if(null!=newtChild) {
            newtChild.setFocusAction(null);
            if(isNewtChildOnscreen) {                    
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
            }
            newtChild.requestFocus();
            newtChild.setFocusAction(focusAction);
        }
    }

    @Override
    public void requestFocus() {
        super.requestFocus();
        requestFocusNEWTChild();
    }

    @Override
    public boolean requestFocus(boolean temporary) {
        final boolean res = super.requestFocus(temporary);
        if(res) {
            requestFocusNEWTChild();
        }
        return res;
    }

    @Override
    public boolean requestFocusInWindow() {
        final boolean res = super.requestFocusInWindow();
        if(res) {
            requestFocusNEWTChild();
        }
        return res;
    }

    @Override
    public boolean requestFocusInWindow(boolean temporary) {
        final boolean res = super.requestFocusInWindow(temporary);
        if(res) {
            requestFocusNEWTChild();
        }
        return res;
    }

  // Disables the AWT's erasing of this Canvas's background on Windows
  // in Java SE 6. This internal API is not available in previous
  // releases, but the system property
  // -Dsun.awt.noerasebackground=true can be specified to get similar
  // results globally in previous releases.
  private static boolean disableBackgroundEraseInitialized;
  private static Method  disableBackgroundEraseMethod;
  private void disableBackgroundErase() {
    if (!disableBackgroundEraseInitialized) {
      try {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
              try {
                Class<?> clazz = getToolkit().getClass();
                while (clazz != null && disableBackgroundEraseMethod == null) {
                  try {
                    disableBackgroundEraseMethod =
                      clazz.getDeclaredMethod("disableBackgroundErase",
                                              new Class[] { Canvas.class });
                    disableBackgroundEraseMethod.setAccessible(true);
                  } catch (Exception e) {
                    clazz = clazz.getSuperclass();
                  }
                }
              } catch (Exception e) {
              }
              return null;
            }
          });
      } catch (Exception e) {
      }
      disableBackgroundEraseInitialized = true;
      if(DEBUG) {
        System.err.println("NewtCanvasAWT: TK disableBackgroundErase method found: "+
                (null!=disableBackgroundEraseMethod));
      }
    }
    if (disableBackgroundEraseMethod != null) {
      Throwable t=null;
      try {
        disableBackgroundEraseMethod.invoke(getToolkit(), new Object[] { this });
      } catch (Exception e) {
        t = e;
      }
      if(DEBUG) {
        System.err.println("NewtCanvasAWT: TK disableBackgroundErase error: "+t);
      }
    }
  }
}

