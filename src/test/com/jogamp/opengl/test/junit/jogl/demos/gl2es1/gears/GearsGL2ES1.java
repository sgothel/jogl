
package com.jogamp.opengl.test.junit.jogl.demos.gl2es1.gears;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import com.jogamp.opengl.util.ImmModeSink;
import com.jogamp.opengl.util.glsl.fixedfunc.FixedFuncUtil;

import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.awt.AWTKeyAdapter;
import com.jogamp.newt.event.awt.AWTMouseAdapter;

/**
 * Gears.java <BR>
 * author: Brian Paul (converted to Java by Ron Cemer and Sven Gothel) <P>
 *
 * This version is equal to Brian Paul's version 1.2 1999/10/21
 */

public class GearsGL2ES1 implements GLEventListener {
  private final float pos[] = { 5.0f, 5.0f, 10.0f, 0.0f };
  private final float red[] = { 0.8f, 0.1f, 0.0f, 0.7f };
  private final float green[] = { 0.0f, 0.8f, 0.2f, 0.7f };
  private final float blue[] = { 0.2f, 0.2f, 1.0f, 0.7f };

  private float view_rotx = 20.0f, view_roty = 30.0f, view_rotz = 0.0f;
  private GearBuffers gear1=null, gear2=null, gear3=null;
  private float angle = 0.0f;
  private int swapInterval;

  private boolean mouseRButtonDown = false;
  private int prevMouseX, prevMouseY;

  public GearsGL2ES1(int swapInterval) {
    this.swapInterval = swapInterval;
  }

  public GearsGL2ES1() {
    this.swapInterval = 1;
  }
  
  public void init(GLAutoDrawable drawable) {
    System.err.println("Gears: Init: "+drawable);
    // Use debug pipeline
    // drawable.setGL(new DebugGL(drawable.getGL()));

    GL _gl = drawable.getGL();
    GL2ES1 gl = FixedFuncUtil.wrapFixedFuncEmul(_gl, true);
    
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
        gear1 = gear(gl, 1.0f, 4.0f, 1.0f, 20, 0.7f);
        System.err.println("gear1 created: "+gear1);
    } else {
        System.err.println("gear1 reused: "+gear1);
    }
            
    if(null == gear2) {
        gear2 = gear(gl, 0.5f, 2.0f, 2.0f, 10, 0.7f);
        System.err.println("gear2 created: "+gear2);
    } else {
        System.err.println("gear2 reused: "+gear2);
    }
            
    if(null == gear3) {
        gear3 = gear(gl, 1.3f, 2.0f, 0.5f, 10, 0.7f);
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
        new AWTMouseAdapter(gearsMouse).addTo(comp);
        new AWTKeyAdapter(gearsKeys).addTo(comp);
    }
  }
    
  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    System.err.println("Gears: Reshape "+x+"/"+y+" "+width+"x"+height);
    GL2ES1 gl = drawable.getGL().getGL2ES1();

    gl.setSwapInterval(swapInterval);

    float h = (float)height / (float)width;
            
    gl.glMatrixMode(GL2ES1.GL_PROJECTION);

    gl.glLoadIdentity();
    gl.glFrustum(-1.0f, 1.0f, -h, h, 5.0f, 60.0f);
    gl.glMatrixMode(GL2ES1.GL_MODELVIEW);
    gl.glLoadIdentity();
    gl.glTranslatef(0.0f, 0.0f, -40.0f);
  }

  public void dispose(GLAutoDrawable drawable) {
    System.err.println("Gears: Dispose");
  }

  public void display(GLAutoDrawable drawable) {
    // Turn the gears' teeth
    angle += 2.0f;

    // Get the GL corresponding to the drawable we are animating
    GL2ES1 gl = drawable.getGL().getGL2ES1();

    gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

    // Special handling for the case where the GLJPanel is translucent
    // and wants to be composited with other Java 2D content
    if (GLProfile.isAWTAvailable() && 
        (drawable instanceof javax.media.opengl.awt.GLJPanel) &&
        !((javax.media.opengl.awt.GLJPanel) drawable).isOpaque() &&
        ((javax.media.opengl.awt.GLJPanel) drawable).shouldPreserveColorBufferIfTranslucent()) {
      gl.glClear(GL2ES1.GL_DEPTH_BUFFER_BIT);
    } else {
      gl.glClear(GL2ES1.GL_COLOR_BUFFER_BIT | GL2ES1.GL_DEPTH_BUFFER_BIT);
    }

    gl.glNormal3f(0.0f, 0.0f, 1.0f);
    
    // Rotate the entire assembly of gears based on how the user
    // dragged the mouse around
    gl.glPushMatrix();
    gl.glRotatef(view_rotx, 1.0f, 0.0f, 0.0f);
    gl.glRotatef(view_roty, 0.0f, 1.0f, 0.0f);
    gl.glRotatef(view_rotz, 0.0f, 0.0f, 1.0f);
            
    final boolean disableBufferAfterDraw = true;
    
    // Place the first gear and call its display list
    gl.glPushMatrix();
    gl.glTranslatef(-3.0f, -2.0f, 0.0f);
    gl.glRotatef(angle, 0.0f, 0.0f, 1.0f);
    gl.glMaterialfv(GL2ES1.GL_FRONT, GL2ES1.GL_AMBIENT_AND_DIFFUSE, red, 0);
    gear1.draw(gl, disableBufferAfterDraw);
    gl.glPopMatrix();
            
    // Place the second gear and call its display list
    gl.glPushMatrix();
    gl.glTranslatef(3.1f, -2.0f, 0.0f);
    gl.glRotatef(-2.0f * angle - 9.0f, 0.0f, 0.0f, 1.0f);
    gl.glMaterialfv(GL2ES1.GL_FRONT, GL2ES1.GL_AMBIENT_AND_DIFFUSE, green, 0);
    gear2.draw(gl, disableBufferAfterDraw);
    gl.glPopMatrix();
            
    // Place the third gear and call its display list
    gl.glPushMatrix();
    gl.glTranslatef(-3.1f, 4.2f, 0.0f);
    gl.glRotatef(-2.0f * angle - 25.0f, 0.0f, 0.0f, 1.0f);
    gl.glMaterialfv(GL2ES1.GL_FRONT, GL2ES1.GL_AMBIENT_AND_DIFFUSE, blue, 0);
    gear3.draw(gl, disableBufferAfterDraw);
    gl.glPopMatrix();
            
    // Remember that every push needs a pop; this one is paired with
    // rotating the entire gear assembly
    gl.glPopMatrix();
  }

  static class GearBuffers {
      public final ImmModeSink frontFace;
      public final ImmModeSink frontSide;
      public final ImmModeSink backFace;
      public final ImmModeSink backSide;
      public final ImmModeSink outwardFace;
      public final ImmModeSink insideRadiusCyl;
      
      public GearBuffers(
          ImmModeSink frontFace,
          ImmModeSink frontSide,
          ImmModeSink backFace,
          ImmModeSink backSide,
          ImmModeSink outwardFace,
          ImmModeSink insideRadiusCyl) {
          this.frontFace = frontFace;
          this.frontSide = frontSide;
          this.backFace = backFace;          
          this.backSide = backSide;
          this.outwardFace = outwardFace;
          this.insideRadiusCyl = insideRadiusCyl;          
      }
      
      public void draw(GL2ES1 gl, boolean disableBufferAfterDraw) {
          gl.glShadeModel(GL2ES1.GL_FLAT);          
          frontFace.draw(gl, disableBufferAfterDraw);
          frontSide.draw(gl, disableBufferAfterDraw);
          backFace.draw(gl, disableBufferAfterDraw);
          backSide.draw(gl, disableBufferAfterDraw);
          outwardFace.draw(gl, disableBufferAfterDraw);
          gl.glShadeModel(GL2ES1.GL_SMOOTH);          
          insideRadiusCyl.draw(gl, disableBufferAfterDraw);
      }
  }
  
  public static GearBuffers gear(GL2ES1 gl,
                                 float inner_radius,
                                 float outer_radius,
                                 float width,
                                 int teeth,
                                 float tooth_depth)
  {
    final float dz = width * 0.5f; 
    int i;
    float r0, r1, r2;
    float angle, da;
    float u, v, len;

    r0 = inner_radius;
    r1 = outer_radius - tooth_depth / 2.0f;
    r2 = outer_radius + tooth_depth / 2.0f;
            
    da = 2.0f * (float) Math.PI / teeth / 4.0f;
            
    /* draw front face */
    ImmModeSink vboFrontFace = ImmModeSink.createFixed(gl, GL.GL_STATIC_DRAW, 4*teeth+2,
                          /* vertex */ 3, GL.GL_FLOAT,  /* color */ 0, GL.GL_FLOAT,  
                          /* normal */ 0, GL.GL_BYTE,   /* texture */ 0, GL.GL_FLOAT);     
    vboFrontFace.glBegin(GL.GL_TRIANGLE_STRIP);
    for (i = 0; i < teeth; i++) {
        angle = i * 2.0f * (float) Math.PI / teeth;
        vboFrontFace.glVertex3f(r0 * (float)Math.cos(angle), r0 * (float)Math.sin(angle), dz);
        vboFrontFace.glVertex3f(r1 * (float)Math.cos(angle), r1 * (float)Math.sin(angle), dz);
        vboFrontFace.glVertex3f(r0 * (float)Math.cos(angle), r0 * (float)Math.sin(angle), dz);
        vboFrontFace.glVertex3f(r1 * (float)Math.cos(angle + 3.0f * da), r1 * (float)Math.sin(angle + 3.0f * da), dz);
    }
    vboFrontFace.glVertex3f(r0 * (float)Math.cos(0f), r0 * (float)Math.sin(0f), dz);
    vboFrontFace.glVertex3f(r1 * (float)Math.cos(0f), r1 * (float)Math.sin(0f), dz);
    vboFrontFace.glEnd(gl, false /* immediate */);

    /* draw front sides of teeth */
    ImmModeSink vboFrontSide = ImmModeSink.createFixed(gl, GL.GL_STATIC_DRAW, 6*teeth,
                          /* vertex */ 3, GL.GL_FLOAT,  /* color */ 0, GL.GL_FLOAT,  
                          /* normal */ 0, GL.GL_FLOAT,   /* texture */ 0, GL.GL_FLOAT); 
    vboFrontSide.glBegin(GL.GL_TRIANGLES);
    for (i = 0; i < teeth; i++) {
        // QUAD [s0..s3] -> 2x TRIs
        angle = i * 2.0f * (float) Math.PI / teeth;
        // s0
        vboFrontSide.glVertex3f(r1 * (float)Math.cos(angle), r1 * (float)Math.sin(angle), dz);
        // s1
        vboFrontSide.glVertex3f(r2 * (float)Math.cos(angle + da), r2 * (float)Math.sin(angle + da), dz);
        // s2
        vboFrontSide.glVertex3f(r2 * (float)Math.cos(angle + 2.0f * da), r2 * (float)Math.sin(angle + 2.0f * da), dz);

        // s0
        vboFrontSide.glVertex3f(r1 * (float)Math.cos(angle), r1 * (float)Math.sin(angle), dz);
        // s2
        vboFrontSide.glVertex3f(r2 * (float)Math.cos(angle + 2.0f * da), r2 * (float)Math.sin(angle + 2.0f * da), dz);        
        // s3
        vboFrontSide.glVertex3f(r1 * (float)Math.cos(angle + 3.0f * da), r1 * (float)Math.sin(angle + 3.0f * da), dz);
    }
    vboFrontSide.glEnd(gl, false /* immediate */);
    
    /* draw back face */
    ImmModeSink vboBackFace = ImmModeSink.createFixed(gl, GL.GL_STATIC_DRAW, 4*teeth+2,
                          /* vertex */ 3, GL.GL_FLOAT,  /* color */ 0, GL.GL_FLOAT,  
                          /* normal */ 0, GL.GL_FLOAT,   /* texture */ 0, GL.GL_FLOAT);     
    vboBackFace.glBegin(GL.GL_TRIANGLE_STRIP);
    for (i = 0; i < teeth; i++) {
        angle = i * 2.0f * (float) Math.PI / teeth;
        vboBackFace.glVertex3f(r1 * (float)Math.cos(angle), r1 * (float)Math.sin(angle), -dz);
        vboBackFace.glVertex3f(r0 * (float)Math.cos(angle), r0 * (float)Math.sin(angle), -dz);
        vboBackFace.glVertex3f(r1 * (float)Math.cos(angle + 3 * da), r1 * (float)Math.sin(angle + 3 * da), -dz);
        vboBackFace.glVertex3f(r0 * (float)Math.cos(angle), r0 * (float)Math.sin(angle), -dz);
    }
    vboBackFace.glVertex3f(r1 * (float)Math.cos(0f), r1 * (float)Math.sin(0f), -dz);
    vboBackFace.glVertex3f(r0 * (float)Math.cos(0f), r0 * (float)Math.sin(0f), -dz);
    vboBackFace.glEnd(gl, false /* immediate */);
    
    /* draw back sides of teeth */
    ImmModeSink vboBackSide = ImmModeSink.createFixed(gl, GL.GL_STATIC_DRAW, 6*teeth,
                          /* vertex */ 3, GL.GL_FLOAT,  /* color */ 0, GL.GL_FLOAT,  
                          /* normal */ 0, GL.GL_FLOAT,   /* texture */ 0, GL.GL_FLOAT);     
    vboBackSide.glBegin(GL.GL_TRIANGLES);
    for (i = 0; i < teeth; i++) {
        // QUAD [s0..s3] -> 2x TRIs
        angle = i * 2.0f * (float) Math.PI / teeth;
        // s0
        vboBackSide.glVertex3f(r1 * (float)Math.cos(angle + 3 * da), r1 * (float)Math.sin(angle + 3 * da), -dz);
        // s1
        vboBackSide.glVertex3f(r2 * (float)Math.cos(angle + 2 * da), r2 * (float)Math.sin(angle + 2 * da), -dz);
        // s2
        vboBackSide.glVertex3f(r2 * (float)Math.cos(angle + da), r2 * (float)Math.sin(angle + da), -dz);
        
        // s0
        vboBackSide.glVertex3f(r1 * (float)Math.cos(angle + 3 * da), r1 * (float)Math.sin(angle + 3 * da), -dz);
        // s2
        vboBackSide.glVertex3f(r2 * (float)Math.cos(angle + da), r2 * (float)Math.sin(angle + da), -dz);        
        // s3
        vboBackSide.glVertex3f(r1 * (float)Math.cos(angle), r1 * (float)Math.sin(angle), -dz);
    }
    vboBackSide.glEnd(gl, false /* immediate */);
    
    /* draw outward faces of teeth */
    ImmModeSink vboOutwardFace = ImmModeSink.createFixed(gl, GL.GL_STATIC_DRAW, 4*4*teeth,
                          /* vertex */ 3, GL.GL_FLOAT,  /* color */ 0, GL.GL_FLOAT,  
                          /* normal */ 3, GL.GL_FLOAT,  /* texture */ 0, GL.GL_FLOAT); 
    vboOutwardFace.glBegin(GL.GL_TRIANGLE_STRIP);
    for (i = 0; i < teeth; i++) {
        angle = i * 2.0f * (float) Math.PI / teeth;
        /*if(i>0) {
            vboOutwardFace.glVertex3f(r1 * (float)Math.cos(angle), r1 * (float)Math.sin(angle), dz);
            vboOutwardFace.glVertex3f(r1 * (float)Math.cos(angle), r1 * (float)Math.sin(angle), -dz);
        }*/
        u = r2 * (float)Math.cos(angle + da) - r1 * (float)Math.cos(angle);
        v = r2 * (float)Math.sin(angle + da) - r1 * (float)Math.sin(angle);
        len = (float)Math.sqrt(u * u + v * v);
        u /= len;
        v /= len;
        
        vboOutwardFace.glNormal3f(v, -u, 0.0f);
        vboOutwardFace.glVertex3f(r1 * (float)Math.cos(angle), r1 * (float)Math.sin(angle), dz);
        vboOutwardFace.glVertex3f(r1 * (float)Math.cos(angle), r1 * (float)Math.sin(angle), -dz);
        vboOutwardFace.glVertex3f(r2 * (float)Math.cos(angle + 1 * da), r2 * (float)Math.sin(angle + 1 * da), dz);
        vboOutwardFace.glVertex3f(r2 * (float)Math.cos(angle + 1 * da), r2 * (float)Math.sin(angle + 1 * da), -dz);
        
        vboOutwardFace.glNormal3f((float)Math.cos(angle), (float)Math.sin(angle), 0.0f);
        vboOutwardFace.glVertex3f(r2 * (float)Math.cos(angle + 1 * da), r2 * (float)Math.sin(angle + 1 * da), dz);
        vboOutwardFace.glVertex3f(r2 * (float)Math.cos(angle + 1 * da), r2 * (float)Math.sin(angle + 1 * da), -dz);
        vboOutwardFace.glVertex3f(r2 * (float)Math.cos(angle + 2 * da), r2 * (float)Math.sin(angle + 2 * da), dz);
        vboOutwardFace.glVertex3f(r2 * (float)Math.cos(angle + 2 * da), r2 * (float)Math.sin(angle + 2 * da), -dz);
        
        u = r1 * (float)Math.cos(angle + 3 * da) - r2 * (float)Math.cos(angle + 2 * da);
        v = r1 * (float)Math.sin(angle + 3 * da) - r2 * (float)Math.sin(angle + 2 * da);
        vboOutwardFace.glNormal3f(v, -u, 0.0f);
        vboOutwardFace.glVertex3f(r2 * (float)Math.cos(angle + 2 * da), r2 * (float)Math.sin(angle + 2 * da), dz);
        vboOutwardFace.glVertex3f(r2 * (float)Math.cos(angle + 2 * da), r2 * (float)Math.sin(angle + 2 * da), -dz);        
        vboOutwardFace.glVertex3f(r1 * (float)Math.cos(angle + 3 * da), r1 * (float)Math.sin(angle + 3 * da), dz);
        vboOutwardFace.glVertex3f(r1 * (float)Math.cos(angle + 3 * da), r1 * (float)Math.sin(angle + 3 * da), -dz);
        
        vboOutwardFace.glNormal3f((float)Math.cos(angle), (float)Math.sin(angle), 0.0f);
        vboOutwardFace.glVertex3f(r1 * (float)Math.cos(angle + 3 * da), r1 * (float)Math.sin(angle + 3 * da), dz);
        vboOutwardFace.glVertex3f(r1 * (float)Math.cos(angle + 3 * da), r1 * (float)Math.sin(angle + 3 * da), -dz);
        vboOutwardFace.glVertex3f(r1 * (float)Math.cos(angle), r1 * (float)Math.sin(angle), dz);
        vboOutwardFace.glVertex3f(r1 * (float)Math.cos(angle), r1 * (float)Math.sin(angle), -dz);
    }
    //vboOutwardFace.glVertex3f(r1 * (float)Math.cos(0f), r1 * (float)Math.sin(0f), dz);
    //vboOutwardFace.glVertex3f(r1 * (float)Math.cos(0f), r1 * (float)Math.sin(0f), -dz);
    vboOutwardFace.glEnd(gl, false /* immediate */);
    
    /* draw inside radius cylinder */
    ImmModeSink vboInsideRadiusCyl = ImmModeSink.createFixed(gl, GL.GL_STATIC_DRAW, 2*teeth+2,
                          /* vertex */ 3, GL.GL_FLOAT,  /* color */ 0, GL.GL_FLOAT,  
                          /* normal */ 3, GL.GL_FLOAT,  /* texture */ 0, GL.GL_FLOAT); 
    vboInsideRadiusCyl.glBegin(GL.GL_TRIANGLE_STRIP);
    for (i = 0; i < teeth; i++) {
        angle = i * 2.0f * (float) Math.PI / teeth;
        vboInsideRadiusCyl.glNormal3f(-(float)Math.cos(angle), -(float)Math.sin(angle), 0.0f);
        vboInsideRadiusCyl.glVertex3f(r0 * (float)Math.cos(angle), r0 * (float)Math.sin(angle), -dz);
        vboInsideRadiusCyl.glVertex3f(r0 * (float)Math.cos(angle), r0 * (float)Math.sin(angle), dz);
    }
    vboInsideRadiusCyl.glNormal3f(-(float)Math.cos(0f), -(float)Math.sin(0f), 0.0f);
    vboInsideRadiusCyl.glVertex3f(r0 * (float)Math.cos(0f), r0 * (float)Math.sin(0f), -dz);
    vboInsideRadiusCyl.glVertex3f(r0 * (float)Math.cos(0f), r0 * (float)Math.sin(0f), dz);
    vboInsideRadiusCyl.glEnd(gl, false /* immediate */);
    return new GearBuffers(vboFrontFace, vboFrontSide, vboBackFace, vboBackSide, vboOutwardFace, vboInsideRadiusCyl);    
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
        if ((e.getModifiers() & e.BUTTON3_MASK) != 0) {
          mouseRButtonDown = true;
        }
      }
        
      public void mouseReleased(MouseEvent e) {
        if ((e.getModifiers() & e.BUTTON3_MASK) != 0) {
          mouseRButtonDown = false;
        }
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
