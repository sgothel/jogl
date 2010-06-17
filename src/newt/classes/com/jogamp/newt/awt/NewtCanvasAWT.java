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

import java.awt.Canvas;

import javax.media.nativewindow.*;
// import javax.media.nativewindow.awt.*;

import com.jogamp.newt.event.awt.AWTAdapter;
import com.jogamp.newt.event.awt.AWTParentWindowAdapter;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.impl.Debug;

public class NewtCanvasAWT extends java.awt.Canvas {
    public static final boolean DEBUG_IMPLEMENTATION = Debug.debug("Window");

    NativeWindow parent = null;
    Window newtChild = null;
    AWTAdapter awtAdapter = null;

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

    /** sets a new NEWT child, provoking reparenting on the NEWT level. */
    public NewtCanvasAWT setNEWTChild(Window child) {
        if(newtChild!=child) {
            newtChild = child;
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

    static java.awt.Container getContainer(java.awt.Component comp) {
        while( null != comp && !(comp instanceof java.awt.Container) ) {
            comp = comp.getParent();
        }
        if(comp instanceof java.awt.Container) {
            return (java.awt.Container) comp;
        }
        return null;
    }

    public void addNotify() {
        super.addNotify();
        disableBackgroundErase();
        java.awt.Container cont = getContainer(this);
        if(DEBUG_IMPLEMENTATION) {
            // if ( isShowing() == false ) -> Container was not visible yet.
            // if ( isShowing() == true  ) -> Container is already visible.
            System.err.println("NewtCanvasAWT.addNotify: "+newtChild+", "+this+", visible "+isVisible()+", showing "+isShowing()+", displayable "+isDisplayable()+" -> "+cont);
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
          // enqueueWindowEvent(true, WindowEvent.EVENT_WINDOW_REPAINT); // trigger a repaint to listener
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

