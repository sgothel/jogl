import java.awt.*;
import java.awt.event.*;

import net.java.games.jogl.*;

/**
 * @(#) Gears.java
 * @(#) author: Brian Paul (converted to Java by Ron Cemer and Sven Goethel)
 *
 * This version is equal to Brian Paul's version 1.2 1999/10/21
 */

public class Gears {
  public static void main(String[] args) {
    Frame frame = new Frame("Gear Demo");
    GLCanvas canvas = GLDrawableFactory.getFactory().createGLCanvas(new GLCapabilities());

    // Use debug pipeline
    //    canvas.setGL(new DebugGL(canvas.getGL()));
    System.err.println("CANVAS GL IS: " + canvas.getGL().getClass().getName());
    System.err.println("CANVAS GLU IS: " + canvas.getGLU().getClass().getName());

    canvas.addGLEventListener(new GearRenderer());
    frame.add(canvas);
    frame.setSize(300, 300);
    final Animator animator = new Animator(canvas);
    frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          animator.stop();
          System.exit(0);
        }
      });
    frame.show();
    animator.start();

  }

  static class GearRenderer implements GLEventListener, MouseListener, MouseMotionListener {
    private float view_rotx = 20.0f, view_roty = 30.0f, view_rotz = 0.0f;
    private int gear1, gear2, gear3;
    private float angle = 0.0f;

    private int prevMouseX, prevMouseY;
    private boolean mouseRButtonDown = false;

    private GL gl;
    private GLDrawable gldrawable;

    public void init(GLDrawable drawable) {
      gl = drawable.getGL();
      this.gldrawable = drawable;
      System.err.println("INIT GL IS: " + gl.getClass().getName());

      float pos[] = { 5.0f, 5.0f, 10.0f, 0.0f };
      float red[] = { 0.8f, 0.1f, 0.0f, 1.0f };
      float green[] = { 0.0f, 0.8f, 0.2f, 1.0f };
      float blue[] = { 0.2f, 0.2f, 1.0f, 1.0f };

      gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, pos);
      gl.glEnable(GL.GL_CULL_FACE);
      gl.glEnable(GL.GL_LIGHTING);
      gl.glEnable(GL.GL_LIGHT0);
      gl.glEnable(GL.GL_DEPTH_TEST);
            
      /* make the gears */
      gear1 = gl.glGenLists(1);
      gl.glNewList(gear1, GL.GL_COMPILE);
      gl.glMaterialfv(GL.GL_FRONT, GL.GL_AMBIENT_AND_DIFFUSE, red);
      gear(1.0f, 4.0f, 1.0f, 20, 0.7f);
      gl.glEndList();
            
      gear2 = gl.glGenLists(1);
      gl.glNewList(gear2, GL.GL_COMPILE);
      gl.glMaterialfv(GL.GL_FRONT, GL.GL_AMBIENT_AND_DIFFUSE, green);
      gear(0.5f, 2.0f, 2.0f, 10, 0.7f);
      gl.glEndList();
            
      gear3 = gl.glGenLists(1);
      gl.glNewList(gear3, GL.GL_COMPILE);
      gl.glMaterialfv(GL.GL_FRONT, GL.GL_AMBIENT_AND_DIFFUSE, blue);
      gear(1.3f, 2.0f, 0.5f, 10, 0.7f);
      gl.glEndList();
            
      gl.glEnable(GL.GL_NORMALIZE);
                
      drawable.addMouseListener(this);
      drawable.addMouseMotionListener(this);
    }
    
    public void reshape(GLDrawable drawable, int x, int y, int width, int height) {
      float h = (float)height / (float)width;
            
      gl.glMatrixMode(GL.GL_PROJECTION);

      System.err.println("GL_VENDOR: " + gl.glGetString(GL.GL_VENDOR));
      System.err.println("GL_RENDERER: " + gl.glGetString(GL.GL_RENDERER));
      System.err.println("GL_VERSION: " + gl.glGetString(GL.GL_VERSION));
      System.err.println();
      System.err.println("glLoadTransposeMatrixfARB() supported: " +
                         gl.isFunctionAvailable("glLoadTransposeMatrixfARB"));
      if (!gl.isFunctionAvailable("glLoadTransposeMatrixfARB")) {
        // --- not using extensions
        gl.glLoadIdentity();
      } else {
        // --- using extensions
        final float[] identityTranspose = new float[] {
          1, 0, 0, 0,
          0, 1, 0, 0,
          0, 0, 1, 0,
          0, 0, 0, 1
        };
        gl.glLoadTransposeMatrixfARB(identityTranspose);
      }
      gl.glFrustum(-1.0f, 1.0f, -h, h, 5.0f, 60.0f);
      gl.glMatrixMode(GL.GL_MODELVIEW);
      gl.glLoadIdentity();
      gl.glTranslatef(0.0f, 0.0f, -40.0f);
    }

    public void display(GLDrawable drawable) {
      angle += 2.0f;

      gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
            
      gl.glPushMatrix();
      gl.glRotatef(view_rotx, 1.0f, 0.0f, 0.0f);
      gl.glRotatef(view_roty, 0.0f, 1.0f, 0.0f);
      gl.glRotatef(view_rotz, 0.0f, 0.0f, 1.0f);
            
      gl.glPushMatrix();
      gl.glTranslatef(-3.0f, -2.0f, 0.0f);
      gl.glRotatef(angle, 0.0f, 0.0f, 1.0f);
      gl.glCallList(gear1);
      gl.glPopMatrix();
            
      gl.glPushMatrix();
      gl.glTranslatef(3.1f, -2.0f, 0.0f);
      gl.glRotatef(-2.0f * angle - 9.0f, 0.0f, 0.0f, 1.0f);
      gl.glCallList(gear2);
      gl.glPopMatrix();
            
      gl.glPushMatrix();
      gl.glTranslatef(-3.1f, 4.2f, 0.0f);
      gl.glRotatef(-2.0f * angle - 25.0f, 0.0f, 0.0f, 1.0f);
      gl.glCallList(gear3);
      gl.glPopMatrix();
            
      gl.glPopMatrix();
      //      gl.glEnd(); // DELIBERATE ERROR!
    }

    public void displayChanged(GLDrawable drawable, boolean modeChanged, boolean deviceChanged) {}

    private void gear(float inner_radius,
                      float outer_radius,
                      float width,
                      int teeth,
                      float tooth_depth)
    {
      int i;
      float r0, r1, r2;
      float angle, da;
      float u, v, len;

      r0 = inner_radius;
      r1 = outer_radius - tooth_depth / 2.0f;
      r2 = outer_radius + tooth_depth / 2.0f;
            
      da = 2.0f * (float) Math.PI / teeth / 4.0f;
            
      gl.glShadeModel(GL.GL_FLAT);

      gl.glNormal3f(0.0f, 0.0f, 1.0f);

      /* draw front face */
      gl.glBegin(GL.GL_QUAD_STRIP);
      for (i = 0; i <= teeth; i++)
        {
          angle = i * 2.0f * (float) Math.PI / teeth;
          gl.glVertex3f(r0 * (float)Math.cos(angle), r0 * (float)Math.sin(angle), width * 0.5f);
          gl.glVertex3f(r1 * (float)Math.cos(angle), r1 * (float)Math.sin(angle), width * 0.5f);
          if(i < teeth)
            {
              gl.glVertex3f(r0 * (float)Math.cos(angle), r0 * (float)Math.sin(angle), width * 0.5f);
              gl.glVertex3f(r1 * (float)Math.cos(angle + 3.0f * da), r1 * (float)Math.sin(angle + 3.0f * da), width * 0.5f);
            }
        }
      gl.glEnd();

      /* draw front sides of teeth */
      gl.glBegin(GL.GL_QUADS);
      for (i = 0; i < teeth; i++)
        {
          angle = i * 2.0f * (float) Math.PI / teeth;
          gl.glVertex3f(r1 * (float)Math.cos(angle), r1 * (float)Math.sin(angle), width * 0.5f);
          gl.glVertex3f(r2 * (float)Math.cos(angle + da), r2 * (float)Math.sin(angle + da), width * 0.5f);
          gl.glVertex3f(r2 * (float)Math.cos(angle + 2.0f * da), r2 * (float)Math.sin(angle + 2.0f * da), width * 0.5f);
          gl.glVertex3f(r1 * (float)Math.cos(angle + 3.0f * da), r1 * (float)Math.sin(angle + 3.0f * da), width * 0.5f);
        }
      gl.glEnd();
    
      /* draw back face */
      gl.glBegin(GL.GL_QUAD_STRIP);
      for (i = 0; i <= teeth; i++)
        {
          angle = i * 2.0f * (float) Math.PI / teeth;
          gl.glVertex3f(r1 * (float)Math.cos(angle), r1 * (float)Math.sin(angle), -width * 0.5f);
          gl.glVertex3f(r0 * (float)Math.cos(angle), r0 * (float)Math.sin(angle), -width * 0.5f);
          gl.glVertex3f(r1 * (float)Math.cos(angle + 3 * da), r1 * (float)Math.sin(angle + 3 * da), -width * 0.5f);
          gl.glVertex3f(r0 * (float)Math.cos(angle), r0 * (float)Math.sin(angle), -width * 0.5f);
        }
      gl.glEnd();
    
      /* draw back sides of teeth */
      gl.glBegin(GL.GL_QUADS);
      for (i = 0; i < teeth; i++)
        {
          angle = i * 2.0f * (float) Math.PI / teeth;
          gl.glVertex3f(r1 * (float)Math.cos(angle + 3 * da), r1 * (float)Math.sin(angle + 3 * da), -width * 0.5f);
          gl.glVertex3f(r2 * (float)Math.cos(angle + 2 * da), r2 * (float)Math.sin(angle + 2 * da), -width * 0.5f);
          gl.glVertex3f(r2 * (float)Math.cos(angle + da), r2 * (float)Math.sin(angle + da), -width * 0.5f);
          gl.glVertex3f(r1 * (float)Math.cos(angle), r1 * (float)Math.sin(angle), -width * 0.5f);
        }
      gl.glEnd();
    
      /* draw outward faces of teeth */
      gl.glBegin(GL.GL_QUAD_STRIP);
      for (i = 0; i < teeth; i++)
        {
          angle = i * 2.0f * (float) Math.PI / teeth;
          gl.glVertex3f(r1 * (float)Math.cos(angle), r1 * (float)Math.sin(angle), width * 0.5f);
          gl.glVertex3f(r1 * (float)Math.cos(angle), r1 * (float)Math.sin(angle), -width * 0.5f);
          u = r2 * (float)Math.cos(angle + da) - r1 * (float)Math.cos(angle);
          v = r2 * (float)Math.sin(angle + da) - r1 * (float)Math.sin(angle);
          len = (float)Math.sqrt(u * u + v * v);
          u /= len;
          v /= len;
          gl.glNormal3f(v, -u, 0.0f);
          gl.glVertex3f(r2 * (float)Math.cos(angle + da), r2 * (float)Math.sin(angle + da), width * 0.5f);
          gl.glVertex3f(r2 * (float)Math.cos(angle + da), r2 * (float)Math.sin(angle + da), -width * 0.5f);
          gl.glNormal3f((float)Math.cos(angle), (float)Math.sin(angle), 0.0f);
          gl.glVertex3f(r2 * (float)Math.cos(angle + 2 * da), r2 * (float)Math.sin(angle + 2 * da), width * 0.5f);
          gl.glVertex3f(r2 * (float)Math.cos(angle + 2 * da), r2 * (float)Math.sin(angle + 2 * da), -width * 0.5f);
          u = r1 * (float)Math.cos(angle + 3 * da) - r2 * (float)Math.cos(angle + 2 * da);
          v = r1 * (float)Math.sin(angle + 3 * da) - r2 * (float)Math.sin(angle + 2 * da);
          gl.glNormal3f(v, -u, 0.0f);
          gl.glVertex3f(r1 * (float)Math.cos(angle + 3 * da), r1 * (float)Math.sin(angle + 3 * da), width * 0.5f);
          gl.glVertex3f(r1 * (float)Math.cos(angle + 3 * da), r1 * (float)Math.sin(angle + 3 * da), -width * 0.5f);
          gl.glNormal3f((float)Math.cos(angle), (float)Math.sin(angle), 0.0f);
        }
      gl.glVertex3f(r1 * (float)Math.cos(0), r1 * (float)Math.sin(0), width * 0.5f);
      gl.glVertex3f(r1 * (float)Math.cos(0), r1 * (float)Math.sin(0), -width * 0.5f);
      gl.glEnd();
    
      gl.glShadeModel(GL.GL_SMOOTH);
    
      /* draw inside radius cylinder */
      gl.glBegin(GL.GL_QUAD_STRIP);
      for (i = 0; i <= teeth; i++)
        {
          angle = i * 2.0f * (float) Math.PI / teeth;
          gl.glNormal3f(-(float)Math.cos(angle), -(float)Math.sin(angle), 0.0f);
          gl.glVertex3f(r0 * (float)Math.cos(angle), r0 * (float)Math.sin(angle), -width * 0.5f);
          gl.glVertex3f(r0 * (float)Math.cos(angle), r0 * (float)Math.sin(angle), width * 0.5f);
        }
      gl.glEnd();
    }

    // Methods required for the implementation of MouseListener
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}

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
    
    public void mouseClicked(MouseEvent e) {}
    
    // Methods required for the implementation of MouseMotionListener
    public void mouseDragged(MouseEvent e) {
      int x = e.getX();
      int y = e.getY();
      Dimension size = e.getComponent().getSize();

      float thetaY = 360.0f * ( (float)(x-prevMouseX)/(float)size.width);
      float thetaX = 360.0f * ( (float)(prevMouseY-y)/(float)size.height);
    
      prevMouseX = x;
      prevMouseY = y;

      view_rotx += thetaX;
      view_roty += thetaY;
    }
    
    public void mouseMoved(MouseEvent e) {}
  }
}

