
package com.jogamp.opengl.test.junit.jogl.demos.gl2;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

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
import com.jogamp.opengl.GLRendererQuirks;
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
  private Gears sharedGears = null;
  private Object syncObjects = null;
  private float angle = 0.0f;
  private boolean doRotate = true;
  private final int swapInterval;
  private final MouseListener gearsMouse = new GearsMouseAdapter();
  private final KeyListener gearsKeys = new GearsKeyAdapter();
  private TileRendererBase tileRendererInUse = null;
  private boolean doRotateBeforePrinting;
  private boolean verbose = true;
  private boolean flipVerticalInGLOrientation = false;
  private volatile boolean isInit = false;

  // private boolean mouseRButtonDown = false;
  private int prevMouseX, prevMouseY;

  public Gears(final int swapInterval) {
    this.swapInterval = swapInterval;
  }

  public Gears() {
    this.swapInterval = 1;
  }

  @Override
  public void addTileRendererNotify(final TileRendererBase tr) {
      tileRendererInUse = tr;
      doRotateBeforePrinting = doRotate;
      setDoRotation(false);
  }
  @Override
  public void removeTileRendererNotify(final TileRendererBase tr) {
      tileRendererInUse = null;
      setDoRotation(doRotateBeforePrinting);
  }
  @Override
  public void startTileRendering(final TileRendererBase tr) {
      System.err.println("Gears.startTileRendering: "+tr);
  }
  @Override
  public void endTileRendering(final TileRendererBase tr) {
      System.err.println("Gears.endTileRendering: "+tr);
  }

  public void setDoRotation(final boolean rotate) { doRotate = rotate; }
  public void setVerbose(final boolean v) { verbose = v; }
  public void setFlipVerticalInGLOrientation(final boolean v) { flipVerticalInGLOrientation=v; }

  public void setSharedGears(final Gears shared) {
      sharedGears = shared;
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
  public void init(final GLAutoDrawable drawable) {
    final GL2 gl = drawable.getGL().getGL2();

    if( init(gl) ) {
        final Object upstreamWidget = drawable.getUpstreamWidget();
        if (upstreamWidget instanceof Window) {
            final Window window = (Window) upstreamWidget;
            window.addMouseListener(gearsMouse);
            window.addKeyListener(gearsKeys);
        } else if (GLProfile.isAWTAvailable() && upstreamWidget instanceof java.awt.Component) {
            final java.awt.Component comp = (java.awt.Component) upstreamWidget;
            new AWTMouseAdapter(gearsMouse, drawable).addTo(comp);
            new AWTKeyAdapter(gearsKeys, drawable).addTo(comp);
        }
    } else {
        drawable.setGLEventListenerInitState(this, false);
    }
  }

  boolean enableCullFace = false;

  private void enableStates(final GL gl, final boolean enable) {
    final boolean msaa = gl.getContext().getGLDrawable().getChosenGLCapabilities().getSampleBuffers();
    if( enable ) {
        if( enableCullFace ) {
            gl.glEnable(GL.GL_CULL_FACE);
        }
        gl.glEnable(GLLightingFunc.GL_LIGHTING);
        gl.glEnable(GLLightingFunc.GL_LIGHT0);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LESS); // default
        gl.glEnable(GLLightingFunc.GL_NORMALIZE);
        if( msaa ) {
            gl.glEnable(GL.GL_MULTISAMPLE);
        }
    } else {
        if( enableCullFace ) {
            gl.glDisable(GL.GL_CULL_FACE);
        }
        gl.glDisable(GLLightingFunc.GL_LIGHTING);
        gl.glDisable(GLLightingFunc.GL_LIGHT0);
        gl.glDisable(GL.GL_DEPTH_TEST);
        gl.glDisable(GLLightingFunc.GL_NORMALIZE);
        if( msaa ) {
            gl.glDisable(GL.GL_MULTISAMPLE);
        }
    }
  }

  public boolean init(final GL2 gl) {
    if(null != sharedGears && !sharedGears.isInit() ) {
      System.err.println(Thread.currentThread()+" GearsES1.init.0: pending shared Gears .. re-init later XXXXX");
      return false;
    }
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

    gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_POSITION, lightPos, 0);
    if( ! ( flipVerticalInGLOrientation && gl.getContext().getGLDrawable().isGLOriented() ) ) {
        // Only possible if we do not flip the projection matrix
        enableCullFace = true;
    } else {
        enableCullFace = false;
    }
    enableStates(gl, true);

    /* make the gears */
    if( null != sharedGears ) {
        gear1 = sharedGears.getGear1();
        gear2 = sharedGears.getGear2();
        gear3 = sharedGears.getGear3();
        System.err.println("gear1 list reused: "+gear1);
        System.err.println("gear2 list reused: "+gear2);
        System.err.println("gear3 list reused: "+gear3);
        if( gl.getContext().hasRendererQuirk(GLRendererQuirks.NeedSharedObjectSync) ) {
            syncObjects = sharedGears;
            System.err.println("Shared Gears: Synchronized Objects due to quirk "+GLRendererQuirks.toString(GLRendererQuirks.NeedSharedObjectSync));
        } else {
            syncObjects = new Object();
            System.err.println("Shared Gears: Unsynchronized Objects");
        }
    } else {
        gear1 = gl.glGenLists(1);
        gl.glNewList(gear1, GL2.GL_COMPILE);
        gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_AMBIENT_AND_DIFFUSE, red, 0);
        gear(gl, 1.0f, 4.0f, 1.0f, 20, 0.7f);
        gl.glEndList();
        System.err.println("gear1 list created: "+gear1);

        gear2 = gl.glGenLists(1);
        gl.glNewList(gear2, GL2.GL_COMPILE);
        gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_AMBIENT_AND_DIFFUSE, green, 0);
        gear(gl, 0.5f, 2.0f, 2.0f, 10, 0.7f);
        gl.glEndList();
        System.err.println("gear2 list created: "+gear2);

        gear3 = gl.glGenLists(1);
        gl.glNewList(gear3, GL2.GL_COMPILE);
        gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_AMBIENT_AND_DIFFUSE, blue, 0);
        gear(gl, 1.3f, 2.0f, 0.5f, 10, 0.7f);
        gl.glEndList();
        System.err.println("gear3 list created: "+gear3);

        syncObjects = new Object();
    }

    enableStates(gl, false);

    isInit = true;
    return true;
  }

  public final boolean isInit() { return isInit; }

  @Override
  public void reshape(final GLAutoDrawable glad, final int x, final int y, final int width, final int height) {
      if( !isInit ) { return; }
      final GL2 gl = glad.getGL().getGL2();
      gl.setSwapInterval(swapInterval);
      reshape(gl, x, y, width, height, width, height);
  }

  @Override
  public void reshapeTile(final TileRendererBase tr,
          final int tileX, final int tileY, final int tileWidth, final int tileHeight,
          final int imageWidth, final int imageHeight) {
      if( !isInit ) { return; }
      final GL2 gl = tr.getAttachedDrawable().getGL().getGL2();
      gl.setSwapInterval(0);
      reshape(gl, tileX, tileY, tileWidth, tileHeight, imageWidth, imageHeight);
  }

  public void reshape(final GL2 gl, final int tileX, final int tileY, final int tileWidth, final int tileHeight, final int imageWidth, final int imageHeight) {
    System.err.println(Thread.currentThread()+" Gears.reshape "+tileX+"/"+tileY+" "+tileWidth+"x"+tileHeight+" of "+imageWidth+"x"+imageHeight+", swapInterval "+swapInterval+", drawable 0x"+Long.toHexString(gl.getContext().getGLDrawable().getHandle())+", tileRendererInUse "+tileRendererInUse);

    // compute projection parameters 'normal'
    float left, right, bottom, top;
    if( imageHeight > imageWidth ) {
        final float a = (float)imageHeight / (float)imageWidth;
        left = -1.0f;
        right = 1.0f;
        bottom = -a;
        top = a;
    } else {
        final float a = (float)imageWidth / (float)imageHeight;
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

    gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
    gl.glLoadIdentity();
    if( flipVerticalInGLOrientation && gl.getContext().getGLDrawable().isGLOriented() ) {
        gl.glScalef(1f, -1f, 1f);
    }
    gl.glFrustum(l, r, b, t, 5.0f, 60.0f);

    gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
    gl.glLoadIdentity();
    gl.glTranslatef(0.0f, 0.0f, -40.0f);
  }

  @Override
  public void dispose(final GLAutoDrawable drawable) {
    if( !isInit ) { return; }
    isInit = false;
    System.err.println(Thread.currentThread()+" Gears.dispose: tileRendererInUse "+tileRendererInUse);
    try {
        final Object upstreamWidget = drawable.getUpstreamWidget();
        if (upstreamWidget instanceof Window) {
            final Window window = (Window) upstreamWidget;
            window.removeMouseListener(gearsMouse);
            window.removeKeyListener(gearsKeys);
        }
    } catch (final Exception e) { System.err.println("Caught: "); e.printStackTrace(); }
    gear1 = 0;
    gear2 = 0;
    gear3 = 0;
    sharedGears = null;
    syncObjects = null;
  }

  @Override
  public void display(final GLAutoDrawable drawable) {
    if( !isInit ) { return; }

    // Get the GL corresponding to the drawable we are animating
    final GL2 gl = drawable.getGL().getGL2();

    enableStates(gl, true);

    if( null == tileRendererInUse ) {
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    } else {
        gl.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
    }

    // Special handling for the case where the GLJPanel is translucent
    // and wants to be composited with other Java 2D content
    if (GLProfile.isAWTAvailable() &&
        (drawable instanceof com.jogamp.opengl.awt.GLJPanel) &&
        !((com.jogamp.opengl.awt.GLJPanel) drawable).isOpaque() &&
        ((com.jogamp.opengl.awt.GLJPanel) drawable).shouldPreserveColorBufferIfTranslucent()) {
      gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
    } else {
      gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
    }
    displayImpl(gl);

    enableStates(gl, false);
  }

  public void display(final GL2 gl) {
    if( !isInit ) { return; }
    enableStates(gl, true);

    if( null == tileRendererInUse ) {
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    } else {
        gl.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
    }
    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
    displayImpl(gl);

    enableStates(gl, false);
  }

  private void displayImpl(final GL2 gl) {
    if( doRotate ) {
        // Turn the gears' teeth
        angle += 0.5f;
    }
    // Rotate the entire assembly of gears based on how the user
    // dragged the mouse around
    gl.glPushMatrix();
    gl.glRotatef(view_rotx, 1.0f, 0.0f, 0.0f);
    gl.glRotatef(view_roty, 0.0f, 1.0f, 0.0f);
    gl.glRotatef(view_rotz, 0.0f, 0.0f, 1.0f);

    // Place the first gear and call its display list
    synchronized ( syncObjects ) {
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
    }

    // Remember that every push needs a pop; this one is paired with
    // rotating the entire gear assembly
    gl.glPopMatrix();
  }

  public static void gear(final GL2 gl,
                          final float inner_radius,
                          final float outer_radius,
                          final float width,
                          final int teeth,
                          final float tooth_depth)
  {
    int i;
    float r0, r1, r2;
    float angle, da;
    float u, v, len;

    r0 = inner_radius;
    r1 = outer_radius - tooth_depth / 2.0f;
    r2 = outer_radius + tooth_depth / 2.0f;

    da = 2.0f * (float) Math.PI / teeth / 4.0f;

    gl.glShadeModel(GLLightingFunc.GL_FLAT);

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
    gl.glBegin(GL2GL3.GL_QUADS);
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
    gl.glBegin(GL2GL3.GL_QUADS);
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

    gl.glShadeModel(GLLightingFunc.GL_SMOOTH); // default

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
    public void keyPressed(final KeyEvent e) {
        final int kc = e.getKeyCode();
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
      public void mousePressed(final MouseEvent e) {
        prevMouseX = e.getX();
        prevMouseY = e.getY();
        if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
          // mouseRButtonDown = true;
        }
      }

      public void mouseReleased(final MouseEvent e) {
        if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
          // mouseRButtonDown = false;
        }
      }

      public void mouseDragged(final MouseEvent e) {
        final int x = e.getX();
        final int y = e.getY();
        int width=0, height=0;
        final Object source = e.getSource();
        if(source instanceof Window) {
            final Window window = (Window) source;
            width=window.getSurfaceWidth();
            height=window.getSurfaceHeight();
        } else if (source instanceof GLAutoDrawable) {
            final GLAutoDrawable glad = (GLAutoDrawable) source;
            width = glad.getSurfaceWidth();
            height = glad.getSurfaceHeight();
        } else if (GLProfile.isAWTAvailable() && source instanceof java.awt.Component) {
            final java.awt.Component comp = (java.awt.Component) source;
            width=comp.getWidth(); // FIXME HiDPI: May need to convert window units -> pixel units!
            height=comp.getHeight();
        } else {
            throw new RuntimeException("Event source neither Window nor Component: "+source);
        }
        final float thetaY = 360.0f * ( (float)(x-prevMouseX)/(float)width);
        final float thetaX = 360.0f * ( (float)(prevMouseY-y)/(float)height);

        prevMouseX = x;
        prevMouseY = y;

        view_rotx += thetaX;
        view_roty += thetaY;
      }
  }
}
