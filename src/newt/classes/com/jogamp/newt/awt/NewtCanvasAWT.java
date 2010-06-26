/*
 * Copyright (c) 2010 Sven Gothel. All Rights Reserved.
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
 * Neither the name Sven Gothel or the names of
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
 * SVEN GOTHEL HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */

package com.jogamp.newt.awt;

import java.lang.reflect.*;
import java.security.*;

import java.awt.Button;
import java.awt.Frame;
import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.KeyboardFocusManager;

import javax.media.nativewindow.*;

import com.jogamp.newt.event.awt.AWTAdapter;
import com.jogamp.newt.event.awt.AWTParentWindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.impl.Debug;

public class NewtCanvasAWT extends java.awt.Canvas {
    public static final boolean DEBUG_IMPLEMENTATION = Debug.debug("Window");

    NativeWindow parent = null;
    Window newtChild = null;
    AWTAdapter awtAdapter = null;
    boolean hasSwingContainer = false;

    /**
     * Instantiates a NewtCanvas without a NEWT child.<br>
     */
    public NewtCanvasAWT() {
        super();
    }

    /**
     * Instantiates a NewtCanvas with a NEWT child.
     */
    public NewtCanvasAWT(Window child) {
        super();
        setNEWTChild(child);
    }
    
    class UnfocusRunnable implements Runnable {
        boolean focusTraversed = false;
        public void run() {
            KeyboardFocusManager focusManager =
                KeyboardFocusManager.getCurrentKeyboardFocusManager();
            java.awt.Component comp1 =   focusManager.getPermanentFocusOwner();
            java.awt.Component comp2 =   focusManager.getFocusOwner();
            if(DEBUG_IMPLEMENTATION) {
	            System.out.println("AWT Unfocus: traversed "+focusTraversed+" (1)");
	            System.out.println("PRE PermenetFocusOwner: "+comp1);
	            System.out.println("PRE FocusOwner: "+comp2);
            }
            if(null!=comp1) {
                if(!focusTraversed && null==comp2) {
                    comp1.requestFocus();
                    focusTraversed=true;
                    if(DEBUG_IMPLEMENTATION) {
                        System.out.println("AWT Unfocus: traversed "+focusTraversed+" (*)");
                    }
                } else {
                    focusTraversed=false;
                }
                
                if(DEBUG_IMPLEMENTATION) {
                    comp1 = focusManager.getPermanentFocusOwner();
                    comp2 = focusManager.getFocusOwner();
                    System.out.println("MID PermenetFocusOwner: "+comp1);
                    System.out.println("MID FocusOwner: "+comp2);
                }

                focusManager.clearGlobalFocusOwner();

                if(DEBUG_IMPLEMENTATION) {
                    comp1 = focusManager.getPermanentFocusOwner();
                    comp2 = focusManager.getFocusOwner();
                    System.out.println("POST PermenetFocusOwner: "+comp1);
                    System.out.println("POST FocusOwner: "+comp2);
                }
                
                if(focusTraversed && null!=newtChild) {
                    newtChild.requestFocus();
                }
            }
        }
    }
    UnfocusRunnable unfocusRunnable = new UnfocusRunnable();
    
    class FocusListener extends WindowAdapter {
        public synchronized void windowGainedFocus(WindowEvent e) {
            if(DEBUG_IMPLEMENTATION) {
                System.out.println("NewtCanvasAWT focus on: AWT focus "+ NewtCanvasAWT.this.hasFocus()+
                                   ", focusable "+NewtCanvasAWT.this.isFocusable()+", onEDT "+hasSwingContainer);
            }
            if(hasSwingContainer) {
            	java.awt.EventQueue.invokeLater(unfocusRunnable);
            } else {
            	unfocusRunnable.run();
            }
        }
        public synchronized void windowLostFocus(WindowEvent e) {
            if(DEBUG_IMPLEMENTATION) {
                System.out.println("NewtCanvasAWT focus off: AWT focus "+ NewtCanvasAWT.this.hasFocus());
            }
        }
    }
    FocusListener focusListener = new FocusListener();

    /** sets a new NEWT child, provoking reparenting on the NEWT level. */
    public NewtCanvasAWT setNEWTChild(Window child) {
        if(newtChild!=child) {
            if(null!=newtChild) {
                newtChild.removeWindowListener(focusListener);
            }
            newtChild = child;
            if(null!=newtChild) {
                newtChild.addWindowListener(focusListener);
            }
            if(null!=parent) {
                java.awt.Container cont = getContainer(this);
                // reparent right away, addNotify has been called already
                reparentWindow( (null!=newtChild) ? true : false, cont );
            }
        }
        return this;
    }

    /** @return the current NEWT child */
    public Window getNEWTChild() {
        return newtChild;
    }

    /** @return this AWT Canvas NativeWindow represention, may be null in case {@link #removeNotify()} has been called, 
     * or {@link #addNotify()} hasn't been called yet.*/
    public NativeWindow getNativeWindow() { return parent; }

    void setWindowAdapter(boolean attach) {
        if(null!=awtAdapter) {
          awtAdapter.removeFrom(this);
          awtAdapter=null;
        }
        if(attach && null!=newtChild) {
            awtAdapter = new AWTParentWindowAdapter(newtChild).addTo(this);
        }
    }

    static boolean hasSwingContainer(java.awt.Component comp) {
        while( null != comp ) {
        	if( comp instanceof javax.swing.JComponent ) {
        		return true;
        	}
            comp = comp.getParent();
        }
        return false;
    }

    static java.awt.Container getContainer(java.awt.Component comp) {
        while( null != comp ) {
        	if( comp instanceof java.awt.Container ) {
        		return (java.awt.Container) comp;
            }
            comp = comp.getParent();
        }
        return null;
    }

    public void addNotify() {
        super.addNotify();
        disableBackgroundErase();
        hasSwingContainer = hasSwingContainer(this);
        java.awt.Container cont = getContainer(this);
        if(DEBUG_IMPLEMENTATION) {
            // if ( isShowing() == false ) -> Container was not visible yet.
            // if ( isShowing() == true  ) -> Container is already visible.
            System.err.println("NewtCanvasAWT.addNotify: "+newtChild+", "+this+", visible "+isVisible()+", showing "+isShowing()+
            		           ", displayable "+isDisplayable()+", swingContainer "+hasSwingContainer+" -> "+cont);
        }  
        reparentWindow(true, cont);
    }

    public void removeNotify() {
        java.awt.Container cont = getContainer(this);
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("NewtCanvasAWT.removeNotify: "+newtChild+", from "+cont);
        }
        reparentWindow(false, cont);
        super.removeNotify();
    }

    void reparentWindow(boolean add, java.awt.Container cont) {
      if(null==newtChild) {
        return; // nop
      }

      if(add) {
          if(null!=newtChild) {
              parent = NewtFactoryAWT.getNativeWindow(this, newtChild.getRequestedCapabilities());
          }
          if(null!=parent) {
              if(DEBUG_IMPLEMENTATION) {
                System.err.println("NewtCanvasAWT.reparentWindow: "+newtChild);
              }
              setSize(cont.getWidth(), cont.getHeight());
              newtChild.setSize(cont.getWidth(), cont.getHeight());

              Screen screen = null;
              if( !newtChild.isNativeWindowValid() ) {
                  screen = NewtFactoryAWT.createCompatibleScreen(parent);
              }
              newtChild.reparentWindow(parent, screen);
              newtChild.setVisible(true);
              setWindowAdapter(true);
              newtChild.sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED); // trigger a resize/relayout to listener
              newtChild.windowRepaint(0, 0, newtChild.getWidth(), newtChild.getHeight());
          }
      } else {
          setWindowAdapter(false);
          parent = null;
          newtChild.setVisible(false);
          newtChild.reparentWindow(null, null);
      }
    }

    public void paint(Graphics g) {
        if(null!=newtChild) {
            newtChild.windowRepaint(0, 0, getWidth(), getHeight());
        }
    }
    public void update(Graphics g) {
        if(null!=newtChild) {
            newtChild.windowRepaint(0, 0, getWidth(), getHeight());
        }
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
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
              try {
                Class clazz = getToolkit().getClass();
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
    }
    if (disableBackgroundEraseMethod != null) {
      try {
        disableBackgroundEraseMethod.invoke(getToolkit(), new Object[] { this });
      } catch (Exception e) {
        // FIXME: workaround for 6504460 (incorrect backport of 6333613 in 5.0u10)
        // throw new GLException(e);
      }
    }
  }
}

