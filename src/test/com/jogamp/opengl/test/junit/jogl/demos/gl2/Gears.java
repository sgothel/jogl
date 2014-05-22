
package com.jogamp.opengl.test.junit.jogl.demos.gl2;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import com.jogamp.newt.Window;
import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.awt.AWTKeyAdapter;
import com.jogamp.newt.event.awt.AWTMouseAdapter;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.util.TileRendererBase;

/**
 * Gears.java <BR>
 * author: Brian Paul (converted to Java by Ron Cemer and Sven Gothel) <P>
 *
 * This version is equal to Brian Paul's version 1.2 1999/10/21
 */
public class Gears implements GLEventListener, TileRendererBase.TileRendererListener {
  private float view_rotx = 20.0f, view_roty = 30.0f;
  private final float view_rotz = 0.0f;
  private int gear1=0, gear2=0, gear3=0;
  private float angle = 0.0f;
  private boolean doRotate = true;
  private final int swapInterval;
  private final MouseListener gearsMouse = new GearsMouseAdapter();
  private final KeyListener gearsKeys = new GearsKeyAdapter();
  private TileRendererBase tileRendererInUse = null;
  private boolean doRotateBeforePrinting;
  private boolean verbose = true;
  private boolean flipVerticalInGLOrientation = false;

  // private boolean mouseRButtonDown = false;
  private int prevMouseX, prevMouseY;

  public Gears(int swapInterval) {
    this.swapInterval = swapInterval;
  }

  public Gears() {
    this.swapInterval = 1;
  }

  @Override
  public void addTileRendererNotify(TileRendererBase tr) {
      tileRendererInUse = tr;
      doRotateBeforePrinting = doRotate;
      setDoRotation(false);
  }
  @Override
  public void removeTileRendererNotify(TileRendererBase tr) {
      tileRendererInUse = null;
      setDoRotation(doRotateBeforePrinting);
  }
  @Override
  public void startTileRendering(TileRendererBase tr) {
      System.err.println("Gears.startTileRendering: "+tr);
  }
  @Override
  public void endTileRendering(TileRendererBase tr) {
      System.err.println("Gears.endTileRendering: "+tr);
  }

  public void setDoRotation(boolean rotate) { doRotate = rotate; }
  public void setVerbose(boolean v) { verbose = v; }
  public void setFlipVerticalInGLOrientation(boolean v) { flipVerticalInGLOrientation=v; }

  public void setGears(int g1, int g2, int g3) {
      gear1 = g1;
      gear2 = g2;
      gear3 = g3;
  }

  /**
   * @return display list gear1
   */
  public int getGear1() { return gear1; }

  /**
   * @return display list gear2
   */
  public int getGear2() { return gear2; }

  /**
   * @return display list gear3
   */
  public int getGear3() { return gear3; }

  @Override
  public void init(GLAutoDrawable drawable) {
    GL2 gl = drawable.getGL().getGL2();

    init(gl);

    final Object upstreamWidget = drawable.getUpstreamWidget();
    if (upstreamWidget instanceof Window) {
        final Window window = (Window) upstreamWidget;
        window.addMouseListener(gearsMouse);
        window.addKeyListener(gearsKeys);
    } else if (GLProfile.isAWTAvailable() && upstreamWidget instanceof java.awt.Component) {
        final java.awt.Component comp = (java.awt.Component) upstreamWidget;
        new AWTMouseAdapter(gearsMouse).addTo(comp);
        new AWTKeyAdapter(gearsKeys).addTo(comp);
    }
  }

  public void init(GL2 gl) {
    final float lightPos[] = { 5.0f, 5.0f, 10.0f, 0.0f };
    final float red[] = { 0.8f, 0.1f, 0.0f, 0.7f };
    final float green[] = { 0.0f, 0.8f, 0.2f, 0.7f };
    final float blue[] = { 0.2f, 0.2f, 1.0f, 0.7f };

    System.err.println(Thread.currentThread()+" Gears.init: tileRendererInUse "+tileRendererInUse);
    if(verbose) {
        System.err.println("GearsES2 init on "+Thread.currentThread());
        System.err.println("Chosen GLCapabilities: " + gl.getContext().getGLDrawable().getChosenGLCapabilities());
        System.err.println("INIT GL IS: " + gl.getClass().getName());
        System.err.println(JoglVersion.getGLStrings(gl, null, false).toString());
    }

    gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPos, 0);
    if( ! ( flipVerticalInGLOrientation && gl.getContext().getGLDrawable().isGLOriented() ) ) {
        // Only possible if we do not flip the projection matrix
        gl.glEnable(GL2.GL_CULL_FACE);
    }
    gl.glEnable(GL2.GL_LIGHTING);
    gl.glEnable(GL2.GL_LIGHT0);
    gl.glEnable(GL2.GL_DEPTH_TEST);

    /* make the gears */
    if(0>=gear1) {
        gear1 = gl.glGenLists(1);
        gl.glNewList(gear1, GL2.GL_COMPILE);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE, red, 0);
        gear(gl, 1.0f, 4.0f, 1.0f, 20, 0.7f);
        gl.glEndList();
        System.err.println("gear1 list created: "+gear1);
    } else {
        System.err.println("gear1 list reused: "+gear1);
    }

    if(0>=gear2) {
        gear2 = gl.glGenLists(1);
        gl.glNewList(gear2, GL2.GL_COMPILE);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE, green, 0);
        gear(gl, 0.5f, 2.0f, 2.0f, 10, 0.7f);
        gl.glEndList();
        System.err.println("gear2 list created: "+gear2);
    } else {
        System.err.println("gear2 list reused: "+gear2);
    }

    if(0>=gear3) {
        gear3 = gl.glGenLists(1);
        gl.glNewList(gear3, GL2.GL_COMPILE);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE, blue, 0);
        gear(gl, 1.3f, 2.0f, 0.5f, 10, 0.7f);
        gl.glEndList();
        System.err.println("gear3 list created: "+gear3);
    } else {
        System.err.println("gear3 list reused: "+gear3);
    }

    gl.glEnable(GL2.GL_NORMALIZE);
  }

  @Override
  public void reshape(GLAutoDrawable glad, int x, int y, int width, int height) {
      final GL2 gl = glad.getGL().getGL2();
      if(-1 != swapInterval) {
          gl.setSwapInterval(swapInterval);
      }
      reshape(gl, x, y, width, height, width, height);
  }

  @Override
  public void reshapeTile(TileRendererBase tr,
          int tileX, int tileY, int tileWidth, int tileHeight,
          int imageWidth, int imageHeight) {
      final GL2 gl = tr.getAttachedDrawable().getGL().getGL2();
      gl.setSwapInterval(0);
      reshape(gl, tileX, tileY, tileWidth, tileHeight, imageWidth, imageHeight);
  }

  public void reshape(GL2 gl, int tileX, int tileY, int tileWidth, int tileHeight, int imageWidth, int imageHeight) {
    final boolean msaa = gl.getContext().getGLDrawable().getChosenGLCapabilities().getSampleBuffers();
    System.err.println(Thread.currentThread()+" Gears.reshape "+tileX+"/"+tileY+" "+tileWidth+"x"+tileHeight+" of "+imageWidth+"x"+imageHeight+", swapInterval "+swapInterval+", drawable 0x"+Long.toHexString(gl.getContext().getGLDrawable().getHandle())+", msaa "+msaa+", tileRendererInUse "+tileRendererInUse);

    if( msaa ) {
        gl.glEnable(GL.GL_MULTISAMPLE);
    }

    // compute projection parameters 'normal'
    float left, right, bottom, top;
    if( imageHeight > imageWidth ) {
        float a = (float)imageHeight / (float)imageWidth;
        left = -1.0f;
        right = 1.0f;
        bottom = -a;
        top = a;
    } else {
        float a = (float)imageWidth / (float)imageHeight;
        left = -a;
        right = a;
        bottom = -1.0f;
        top = 1.0f;
    }
    final float w = right - left;
    final float h = top - bottom;

    // compute projection parameters 'tiled'
    final float l = left + tileX * w / imageWidth;
    final float r = l + tileWidth * w / imageWidth;

    final float b = bottom + tileY * h / imageHeight;
    final float t = b + tileHeight * h / imageHeight;

    final float _w = r - l;
    final float _h = t - b;
    if(verbose) {
        System.err.println(">> Gears angle "+angle+", [l "+left+", r "+right+", b "+bottom+", t "+top+"] "+w+"x"+h+" -> [l "+l+", r "+r+", b "+b+", t "+t+"] "+_w+"x"+_h+", v-flip "+flipVerticalInGLOrientation);
    }

    gl.glMatrixMode(GL2.GL_PROJECTION);
    gl.glLoadIdentity();
    if( flipVerticalInGLOrientation && gl.getContext().getGLDrawable().isGLOriented() ) {
        gl.glScalef(1f, -1f, 1f);
    }
    gl.glFrustum(l, r, b, t, 5.0f, 60.0f);

    gl.glMatrixMode(GL2.GL_MODELVIEW);
    gl.glLoadIdentity();
    gl.glTranslatef(0.0f, 0.0f, -40.0f);

    if( msaa ) {
        gl.glDisable(GL.GL_MULTISAMPLE);
    }
  }

  @Override
  public void dispose(GLAutoDrawable drawable) {
    System.err.println(Thread.currentThread()+" Gears.dispose: tileRendererInUse "+tileRendererInUse);
    try {
        final Object upstreamWidget = drawable.getUpstreamWidget();
        if (upstreamWidget instanceof Window) {
            final Window window = (Window) upstreamWidget;
            window.removeMouseListener(gearsMouse);
            window.removeKeyListener(gearsKeys);
        }
    } catch (Exception e) { System.err.println("Catched: "); e.printStackTrace(); }
    setGears(0, 0, 0);
  }

  @Override
  public void display(GLAutoDrawable drawable) {
    // Get the GL corresponding to the drawable we are animating
    GL2 gl = drawable.getGL().getGL2();
    final boolean msaa = gl.getContext().getGLDrawable().getChosenGLCapabilities().getSampleBuffers();

    if( msaa ) {
        gl.glEnable(GL.GL_MULTISAMPLE);
    }

    if( null == tileRendererInUse ) {
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    } else {
        gl.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
    }

    // Special handling for the case where the GLJPanel is translucent
    // and wants to be composited with other Java 2D content
    if (GLProfile.isAWTAvailable() &&
        (drawable instanceof javax.media.opengl.awt.GLJPanel) &&
        !((javax.media.opengl.awt.GLJPanel) drawable).isOpaque() &&
        ((javax.media.opengl.awt.GLJPanel) drawable).shouldPreserveColorBufferIfTranslucent()) {
      gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);
    } else {
      gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
    }
    displayImpl(gl);
    if( msaa ) {
        gl.glDisable(GL.GL_MULTISAMPLE);
    }
  }
  public void display(GL2 gl) {
    final boolean msaa = gl.getContext().getGLDrawable().getChosenGLCapabilities().getSampleBuffers();
    if( msaa ) {
        gl.glEnable(GL.GL_MULTISAMPLE);
    }
    if( null == tileRendererInUse ) {
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    } else {
        gl.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
    }
    gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
    displayImpl(gl);
    if( msaa ) {
        gl.glDisable(GL.GL_MULTISAMPLE);
    }
  }
  private void displayImpl(GL2 gl) {
    if( doRotate ) {
        // Turn the gears' teeth
        angle += 2.0f;
    }
    // Rotate the entire assembly of gears based on how the user
    // dragged the mouse around
    gl.glPushMatrix();
    gl.glRotatef(view_rotx, 1.0f, 0.0f, 0.0f);
    gl.glRotatef(view_roty, 0.0f, 1.0f, 0.0f);
    gl.glRotatef(view_rotz, 0.0f, 0.0f, 1.0f);

    // Place the first gear and call its display list
    gl.glPushMatrix();
    gl.glTranslatef(-3.0f, -2.0f, 0.0f);
    gl.glRotatef(angle, 0.0f, 0.0f, 1.0f);
    gl.glCallList(gear1);
    gl.glPopMatrix();

    // Place the second gear and call its display list
    gl.glPushMatrix();
    gl.glTranslatef(3.1f, -2.0f, 0.0f);
    gl.glRotatef(-2.0f * angle - 9.0f, 0.0f, 0.0f, 1.0f);
    gl.glCallList(gear2);
    gl.glPopMatrix();

    // Place the third gear and call its display list
    gl.glPushMatrix();
    gl.glTranslatef(-3.1f, 4.2f, 0.0f);
    gl.glRotatef(-2.0f * angle - 25.0f, 0.0f, 0.0f, 1.0f);
    gl.glCallList(gear3);
    gl.glPopMatrix();

    // Remember that every push needs a pop; this one is paired with
    // rotating the entire gear assembly
    gl.glPopMatrix();
  }

  public static void gear(GL2 gl,
                          float inner_radius,
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

    gl.glShadeModel(GL2.GL_FLAT);

    gl.glNormal3f(0.0f, 0.0f, 1.0f);

    /* draw front face */
    gl.glBegin(GL2.GL_QUAD_STRIP);
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
    gl.glBegin(GL2.GL_QUADS);
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
    gl.glBegin(GL2.GL_QUAD_STRIP);
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
    gl.glBegin(GL2.GL_QUADS);
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
    gl.glBegin(GL2.GL_QUAD_STRIP);
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

    gl.glShadeModel(GL2.GL_SMOOTH);

    /* draw inside radius cylinder */
    gl.glBegin(GL2.GL_QUAD_STRIP);
    for (i = 0; i <= teeth; i++)
      {
        angle = i * 2.0f * (float) Math.PI / teeth;
        gl.glNormal3f(-(float)Math.cos(angle), -(float)Math.sin(angle), 0.0f);
        gl.glVertex3f(r0 * (float)Math.cos(angle), r0 * (float)Math.sin(angle), -width * 0.5f);
        gl.glVertex3f(r0 * (float)Math.cos(angle), r0 * (float)Math.sin(angle), width * 0.5f);
      }
    gl.glEnd();
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
        if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
          // mouseRButtonDown = true;
        }
      }

      public void mouseReleased(MouseEvent e) {
        if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
          // mouseRButtonDown = false;
        }
      }

      public void mouseDragged(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        int width=0, height=0;
        Object source = e.getSource();
        if(source instanceof Window) {
            Window window = (Window) source;
            width=window.getSurfaceWidth();
            height=window.getSurfaceHeight();
        } else if (source instanceof GLAutoDrawable) {
            GLAutoDrawable glad = (GLAutoDrawable) source;
            width = glad.getSurfaceWidth();
            height = glad.getSurfaceHeight();
        } else if (GLProfile.isAWTAvailable() && source instanceof java.awt.Component) {
            java.awt.Component comp = (java.awt.Component) source;
            width=comp.getWidth(); // FIXME HiDPI: May need to convert window units -> pixel units!
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
