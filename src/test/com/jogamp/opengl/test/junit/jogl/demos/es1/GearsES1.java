/**
 * Copyright (C) 2011 JogAmp Community. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * BRIAN PAUL BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.jogamp.opengl.test.junit.jogl.demos.es1;

import javax.media.nativewindow.NativeWindow;
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.opengl.test.junit.jogl.demos.GearsObject;

/**
 * GearsES1.java <BR>
 * @author Brian Paul (converted to Java by Ron Cemer and Sven Gothel) <P>
 */
public class GearsES1 implements GLEventListener {
  private final float pos[] = { 5.0f, 5.0f, 10.0f, 0.0f };

  private float view_rotx = 20.0f, view_roty = 30.0f, view_rotz = 0.0f;
  private GearsObject gear1=null, gear2=null, gear3=null;
  private float angle = 0.0f;
  private int swapInterval;
  private boolean initialized = false;

  private int prevMouseX, prevMouseY;

  public GearsES1(int swapInterval) {
    this.swapInterval = swapInterval;
  }

  public GearsES1() {
    this.swapInterval = 1;
  }
  
  public void setGears(GearsObject g1, GearsObject g2, GearsObject g3) {
      gear1 = g1;
      gear2 = g2;
      gear3 = g3;
  }

  /**
   * @return gear1
   */
  public GearsObject getGear1() { return gear1; }

  /**
   * @return gear2
   */
  public GearsObject getGear2() { return gear2; }

  /**
   * @return gear3
   */
  public GearsObject getGear3() { return gear3; }
  
  public void init(GLAutoDrawable drawable) {
    System.err.println(Thread.currentThread()+" GearsES1.init ...");
    initialized = true;
    
    // Use debug pipeline
    // drawable.setGL(new DebugGL(drawable.getGL()));

    GL _gl = drawable.getGL();
    // GL2ES1 gl = FixedFuncUtil.wrapFixedFuncEmul(_gl /*, true*/);
    GL2ES1 gl = _gl.getGL2ES1();
    
    System.err.println("Chosen GLCapabilities: " + drawable.getChosenGLCapabilities());
    System.err.println("INIT GL IS: " + gl.getClass().getName());
    System.err.println("GL_VENDOR: " + gl.glGetString(GL.GL_VENDOR));
    System.err.println("GL_RENDERER: " + gl.glGetString(GL.GL_RENDERER));
    System.err.println("GL_VERSION: " + gl.glGetString(GL.GL_VERSION));

    gl.glLightfv(GL2ES1.GL_LIGHT0, GL2ES1.GL_POSITION, pos, 0);
    gl.glEnable(GL.GL_CULL_FACE);
    gl.glEnable(GL2ES1.GL_LIGHTING);
    gl.glEnable(GL2ES1.GL_LIGHT0);
    gl.glEnable(GL2ES1.GL_DEPTH_TEST);
            
    /* make the gears */
    if(null == gear1) {
        gear1 = new GearsObjectES1(1.0f, 4.0f, 1.0f, 20, 0.7f);
        System.err.println("gear1 created: "+gear1);
    } else {
        System.err.println("gear1 reused: "+gear1);
    }
                
    if(null == gear2) {
        gear2 = new GearsObjectES1(0.5f, 2.0f, 2.0f, 10, 0.7f);
        System.err.println("gear2 created: "+gear2);
    } else {
        System.err.println("gear2 reused: "+gear2);
    }
            
    if(null == gear3) {
        gear3 = new GearsObjectES1(1.3f, 2.0f, 0.5f, 10, 0.7f);
        System.err.println("gear3 created: "+gear3);
    } else {
        System.err.println("gear3 reused: "+gear3);
    }
            
    gl.glEnable(GL2ES1.GL_NORMALIZE);
                
    // MouseListener gearsMouse = new TraceMouseAdapter(new GearsMouseAdapter());
    MouseListener gearsMouse = new GearsMouseAdapter();    
    KeyListener gearsKeys = new GearsKeyAdapter();

    if (drawable instanceof Window) {
        Window window = (Window) drawable;
        window.addMouseListener(gearsMouse);
        window.addKeyListener(gearsKeys);
    } else if (GLProfile.isAWTAvailable() && drawable instanceof java.awt.Component) {
        java.awt.Component comp = (java.awt.Component) drawable;
        new com.jogamp.newt.event.awt.AWTMouseAdapter(gearsMouse).addTo(comp);
        new com.jogamp.newt.event.awt.AWTKeyAdapter(gearsKeys).addTo(comp);
    }
    System.err.println(Thread.currentThread()+" GearsES1.init FIN");
  }
    
  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    System.err.println(Thread.currentThread()+" GearsES1.reshape "+x+"/"+y+" "+width+"x"+height+", swapInterval "+swapInterval);
    GL2ES1 gl = drawable.getGL().getGL2ES1();

    gl.setSwapInterval(swapInterval);

    float h = (float)height / (float)width;
            
    gl.glMatrixMode(GL2ES1.GL_PROJECTION);

    gl.glLoadIdentity();
    gl.glFrustum(-1.0f, 1.0f, -h, h, 5.0f, 60.0f);
    gl.glMatrixMode(GL2ES1.GL_MODELVIEW);
    gl.glLoadIdentity();
    gl.glTranslatef(0.0f, 0.0f, -40.0f);
    System.err.println(Thread.currentThread()+" GearsES1.reshape FIN");
  }

  public void dispose(GLAutoDrawable drawable) {
    System.err.println(Thread.currentThread()+" GearsES1.dispose ... ");
    initialized = false;
    GL gl = drawable.getGL();
    gear1.destroy(gl);
    gear1 = null;
    gear2.destroy(gl);
    gear2 = null;
    gear3.destroy(gl);
    gear3 = null;
    System.err.println(Thread.currentThread()+" GearsES1.dispose FIN");
  }

  public void display(GLAutoDrawable drawable) {    
    // Turn the gears' teeth
    angle += 2.0f;

    // Get the GL corresponding to the drawable we are animating
    GL2ES1 gl = drawable.getGL().getGL2ES1();

    final boolean hasFocus;
    if(drawable.getNativeSurface() instanceof NativeWindow) {
      hasFocus = ((NativeWindow)drawable.getNativeSurface()).hasFocus();
    } else {
      hasFocus = true;
    }
    if(hasFocus) {
      gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    } else {
      gl.glClearColor(0.2f, 0.2f, 0.2f, 0.0f);
    }
    
    // Special handling for the case where the GLJPanel is translucent
    // and wants to be composited with other Java 2D content
    if (GLProfile.isAWTAvailable() && 
        (drawable instanceof javax.media.opengl.awt.GLJPanel) &&
        !((javax.media.opengl.awt.GLJPanel) drawable).isOpaque() &&
        ((javax.media.opengl.awt.GLJPanel) drawable).shouldPreserveColorBufferIfTranslucent()) {
      gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
    } else {
      gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
    }

    gl.glNormal3f(0.0f, 0.0f, 1.0f);
    
    // Rotate the entire assembly of gears based on how the user
    // dragged the mouse around
    gl.glPushMatrix();
    gl.glRotatef(view_rotx, 1.0f, 0.0f, 0.0f);
    gl.glRotatef(view_roty, 0.0f, 1.0f, 0.0f);
    gl.glRotatef(view_rotz, 0.0f, 0.0f, 1.0f);
            
    gear1.draw(gl, -3.0f, -2.0f, angle, GearsObject.red);            
    gear2.draw(gl, 3.1f, -2.0f, -2.0f * angle - 9.0f, GearsObject.green);    
    gear3.draw(gl, -3.1f, 4.2f, -2.0f * angle - 25.0f, GearsObject.blue);
    
    // Remember that every push needs a pop; this one is paired with
    // rotating the entire gear assembly
    gl.glPopMatrix();
  }

  
  class GearsKeyAdapter extends KeyAdapter {      
    public void keyPressed(KeyEvent e) {
        int kc = e.getKeyCode();
        if(KeyEvent.VK_LEFT == kc) {
            view_roty -= 1;
        } else if(KeyEvent.VK_RIGHT == kc) {
            view_roty += 1;
        } else if(KeyEvent.VK_UP == kc) {
            view_rotx -= 1;
        } else if(KeyEvent.VK_DOWN == kc) {
            view_rotx += 1;
        }
    }
  }
  
  class GearsMouseAdapter extends MouseAdapter {
      public void mousePressed(MouseEvent e) {
        prevMouseX = e.getX();
        prevMouseY = e.getY();
      }
        
      public void mouseReleased(MouseEvent e) {
      }
        
      public void mouseDragged(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        int width=0, height=0;
        Object source = e.getSource();
        if(source instanceof Window) {
            Window window = (Window) source;
            width=window.getWidth();
            height=window.getHeight();
        } else if (GLProfile.isAWTAvailable() && source instanceof java.awt.Component) {
            java.awt.Component comp = (java.awt.Component) source;
            width=comp.getWidth();
            height=comp.getHeight();
        } else {
            throw new RuntimeException("Event source neither Window nor Component: "+source);
        }
        float thetaY = 360.0f * ( (float)(x-prevMouseX)/(float)width);
        float thetaX = 360.0f * ( (float)(prevMouseY-y)/(float)height);
        
        prevMouseX = x;
        prevMouseY = y;

        view_rotx += thetaX;
        view_roty += thetaY;
      }
  }
}
