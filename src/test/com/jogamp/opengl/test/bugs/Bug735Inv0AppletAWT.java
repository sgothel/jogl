package com.jogamp.opengl.test.bugs;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.FloatBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLRunnable;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.LandscapeES2;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;

/**
 * Original test case.
 * <br/>
 * OSX Results:
 * <pre>
 *   - Visible content
 *   - Fluent animation
 * </pre>
 */
@SuppressWarnings("serial")
public class Bug735Inv0AppletAWT extends Applet implements Runnable {
  static public final int AWT  = 0;
  static public final int NEWT = 1;

  static public final int APPLET_WIDTH  = 500;
  static public final int APPLET_HEIGHT = 290;
  static public final int TARGET_FPS    = 120;
  static public final int TOOLKIT       = NEWT;
  static public final boolean MANUAL_FRAME_HANDLING = true;

  //////////////////////////////////////////////////////////////////////////////

  static private Frame frame;
  static private Bug735Inv0AppletAWT applet;
  private GLCanvas awtCanvas;
  private GLWindow newtWindow;
  private NewtCanvasAWT newtCanvas;
  private DrawRunnable drawRunnable;
  private GLContext context;
  private GLU glu;

  private int width;
  private int height;
  private Thread thread;

  private boolean doneInit = false;
  private boolean doneSetup = false;

  private final long frameRatePeriod = 1000000000L / TARGET_FPS;
  private long millisOffset;
  private int frameCount;
  private float frameRate;

  private ShaderCode vertShader;
  private ShaderCode fragShader;
  private ShaderProgram shaderProg;
  private ShaderState shaderState;
  private GLUniformData resolution;
  private GLUniformData time;
  private GLArrayDataServer vertices;

  private int fcount = 0, lastm = 0;
  private final int fint = 1;

  public void init() {
    setSize(APPLET_WIDTH, APPLET_HEIGHT);
    setPreferredSize(new Dimension(APPLET_WIDTH, APPLET_HEIGHT));
    width = APPLET_WIDTH;
    height = APPLET_HEIGHT;
  }

  public void start() {
    thread = new InterruptSource.Thread(null, this, "Animation Thread");
    thread.start();
  }

  public void run() {
    int noDelays = 0;
    // Number of frames with a delay of 0 ms before the
    // animation thread yields to other running threads.
    final int NO_DELAYS_PER_YIELD = 15;
    final int TIMEOUT_SECONDS = 2;

    long beforeTime = System.nanoTime();
    long overSleepTime = 0L;

    millisOffset = System.currentTimeMillis();
    frameCount = 1;
    while (Thread.currentThread() == thread) {
      final CountDownLatch latch = new CountDownLatch(1);
      requestDraw(latch);
      try {
        latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
      } catch (final InterruptedException e) {
        e.printStackTrace();
      }

      if (frameCount == 1) {
        EventQueue.invokeLater(new Runnable() {
          public void run() {
            requestFocusInWindow();
          }
        });
      }

      final long afterTime = System.nanoTime();
      final long timeDiff = afterTime - beforeTime;
      final long sleepTime = (frameRatePeriod - timeDiff) - overSleepTime;
      if (sleepTime > 0) {  // some time left in this cycle
        try {
          Thread.sleep(sleepTime / 1000000L, (int) (sleepTime % 1000000L));
          noDelays = 0;  // Got some sleep, not delaying anymore
        } catch (final InterruptedException ex) { }
        overSleepTime = (System.nanoTime() - afterTime) - sleepTime;
      } else {    // sleepTime <= 0; the frame took longer than the period
        overSleepTime = 0L;
        noDelays++;
        if (noDelays > NO_DELAYS_PER_YIELD) {
          Thread.yield();   // give another thread a chance to run
          noDelays = 0;
        }
      }
      beforeTime = System.nanoTime();
    }
  }

  public void requestDraw(final CountDownLatch latch) {
    if (!doneInit) {
      initDraw();
    }

    if (TOOLKIT == AWT) {
      awtCanvas.invoke(true, drawRunnable);
    } else if (TOOLKIT == NEWT) {
      newtWindow.invoke(true, drawRunnable);
    }

    if (latch != null) {
      latch.countDown();
    }
  }

  private class DrawRunnable implements GLRunnable {
    private boolean notCurrent;

    @Override
    public boolean run(final GLAutoDrawable drawable) {
      if (MANUAL_FRAME_HANDLING) {
        makeContextCurrent();
      }

      if (doneSetup) {
        draw(drawable.getGL().getGL2ES2());
      } else {
        setup(drawable.getGL().getGL2ES2());
      }
      checkGLErrors(drawable.getGL());

      if (MANUAL_FRAME_HANDLING) {
        swapBuffers();
        releaseCurrentContext();
      }

      return true;
    }

    private void makeContextCurrent() {
      final int MAX_CONTEXT_GRAB_ATTEMPTS = 10;

      if (context.isCurrent()) {
        notCurrent = false;
      } else {
        notCurrent = true;
        int value = GLContext.CONTEXT_NOT_CURRENT;
        int attempt = 0;
        do {
          try {
            value = context.makeCurrent();
            System.out.println("Made context current");
          } catch (final GLException gle) {
            gle.printStackTrace();
          } finally {
            attempt++;
            if (attempt == MAX_CONTEXT_GRAB_ATTEMPTS) {
              throw new RuntimeException("Failed to claim OpenGL context.");
            }
          }
          try {
            Thread.sleep(5);
          } catch (final InterruptedException e) {
            e.printStackTrace();
          }

        } while (value == GLContext.CONTEXT_NOT_CURRENT);
      }
    }

    private void swapBuffers() {
      final GL gl = GLContext.getCurrentGL();
      gl.glFlush();
      GLContext.getCurrent().getGLDrawable().swapBuffers();
    }

    private void releaseCurrentContext() {
      if (notCurrent) {
        try {
          context.release();
          System.out.println("Released context");
        } catch (final GLException gle) {
          gle.printStackTrace();
        }
      }
    }
  }

  private void initGL() {
    final GLProfile profile = GLProfile.getDefault();
    final GLCapabilities caps = new GLCapabilities(profile);
    caps.setBackgroundOpaque(true);
    caps.setOnscreen(true);
    caps.setSampleBuffers(false);

    if (TOOLKIT == AWT) {
      awtCanvas = new GLCanvas(caps);
      awtCanvas.setBounds(0, 0, applet.width, applet.height);
      awtCanvas.setBackground(new Color(0xFFCCCCCC, true));
      awtCanvas.setFocusable(true);

      applet.setLayout(new BorderLayout());
      applet.add(awtCanvas, BorderLayout.CENTER);

      if (MANUAL_FRAME_HANDLING) {
        awtCanvas.setIgnoreRepaint(true);
        awtCanvas.setAutoSwapBufferMode(false);
      }
    } else if (TOOLKIT == NEWT) {
      newtWindow = GLWindow.create(caps);
      newtCanvas = new NewtCanvasAWT(newtWindow);
      newtCanvas.setBounds(0, 0, applet.width, applet.height);
      newtCanvas.setBackground(new Color(0xFFCCCCCC, true));
      newtCanvas.setFocusable(true);

      applet.setLayout(new BorderLayout());
      applet.add(newtCanvas, BorderLayout.CENTER);

      if (MANUAL_FRAME_HANDLING) {
        newtCanvas.setIgnoreRepaint(true);
        newtWindow.setAutoSwapBufferMode(false);
      }
    }
  }

  private void initDraw() {
    if (TOOLKIT == AWT) {
      awtCanvas.setVisible(true);
      // Force the realization
      awtCanvas.display();
      if (awtCanvas.getDelegatedDrawable().isRealized()) {
        // Request the focus here as it cannot work when the window is not visible
        awtCanvas.requestFocus();
        context = awtCanvas.getContext();
      }
    } else if (TOOLKIT == NEWT) {
      newtCanvas.setVisible(true);
      // Force the realization
      newtWindow.display();
      if (newtWindow.isRealized()) {
        // Request the focus here as it cannot work when the window is not visible
        newtCanvas.requestFocus();
        context = newtWindow.getContext();
      }
    }

    drawRunnable = new DrawRunnable();

    doneInit = true;
  }

  private void setup(final GL2ES2 gl) {
    if (60 < TARGET_FPS) {
      // Disables vsync
      gl.setSwapInterval(0);
    }
    glu = new GLU();

    vertShader = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, LandscapeES2.class, "shader", "shader/bin", "landscape", true);
    fragShader = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, LandscapeES2.class, "shader", "shader/bin", "landscape", true);
    vertShader.defaultShaderCustomization(gl, true, true);
    fragShader.defaultShaderCustomization(gl, true, true);
    shaderProg = new ShaderProgram();
    shaderProg.add(gl, vertShader, System.err);
    shaderProg.add(gl, fragShader, System.err);

    shaderState = new ShaderState();
    shaderState.attachShaderProgram(gl, shaderProg, true);

    resolution = new GLUniformData("iResolution", 3, FloatBuffer.wrap(new float[] {width, height, 0}));
    shaderState.ownUniform(resolution);
    shaderState.uniform(gl, resolution);

    time = new GLUniformData("iGlobalTime", 0.0f);
    shaderState.ownUniform(time);

    vertices = GLArrayDataServer.createGLSL("inVertex", 2, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW);
    vertices.putf(-1.0f); vertices.putf(-1.0f);
    vertices.putf(+1.0f); vertices.putf(-1.0f);
    vertices.putf(-1.0f); vertices.putf(+1.0f);
    vertices.putf(+1.0f); vertices.putf(+1.0f);
    vertices.seal(gl, true);
    shaderState.ownAttribute(vertices, true);
    shaderState.useProgram(gl, false);

    doneSetup = true;
  }

  private void draw(final GL2ES2 gl) {
    // gl.glClearColor(0.5f, 0.1f, 0.1f, 1);
    // gl.glClear(GL2ES2.GL_COLOR_BUFFER_BIT);

    shaderState.useProgram(gl, true);

    time.setData((System.currentTimeMillis() - millisOffset) / 1000.0f);
    shaderState.uniform(gl, time);
    vertices.enableBuffer(gl, true);
    gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
    vertices.enableBuffer(gl, false);

    shaderState.useProgram(gl, false);

    // Compute current framerate and printout.
    frameCount++;
    fcount += 1;
    final int m = (int) (System.currentTimeMillis() - millisOffset);
    if (m - lastm > 1000 * fint) {
      frameRate = (float)(fcount) / fint;
      fcount = 0;
      lastm = m;
    }
    if (frameCount % TARGET_FPS == 0) {
      System.out.println("FrameCount: " + frameCount + " - " +
                         "FrameRate: " + frameRate);
    }
  }

  private void checkGLErrors(final GL gl) {
    final int err = gl.glGetError();
    if (err != 0) {
      final String errString = glu.gluErrorString(err);
      System.out.println(errString);
    }
  }

  static public void main(final String[] args) {
    final GraphicsEnvironment environment =
        GraphicsEnvironment.getLocalGraphicsEnvironment();
    final GraphicsDevice displayDevice = environment.getDefaultScreenDevice();

    frame = new Frame(displayDevice.getDefaultConfiguration());
    frame.setBackground(new Color(0xCC, 0xCC, 0xCC));
    frame.setTitle("TestBug735Inv0AppletAWT");

    try {
      final Class<?> c = Thread.currentThread().getContextClassLoader().
          loadClass(Bug735Inv0AppletAWT.class.getName());
      applet = (Bug735Inv0AppletAWT) c.newInstance();
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }

    frame.setLayout(null);
    frame.add(applet);
    frame.pack();
    frame.setResizable(false);

    applet.init();

    final Insets insets = frame.getInsets();
    final int windowW = applet.width + insets.left + insets.right;
    final int windowH = applet.height + insets.top + insets.bottom;
    frame.setSize(windowW, windowH);

    final Rectangle screenRect = displayDevice.getDefaultConfiguration().getBounds();
    frame.setLocation(screenRect.x + (screenRect.width - applet.width) / 2,
        screenRect.y + (screenRect.height - applet.height) / 2);

    final int usableWindowH = windowH - insets.top - insets.bottom;
    applet.setBounds((windowW - applet.width)/2,
                     insets.top + (usableWindowH - applet.height)/2,
                     applet.width, applet.height);

    // This allows to close the frame.
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(final WindowEvent e) {
        System.exit(0);
      }
    });

    applet.initGL();
    frame.setVisible(true);
    applet.start();
  }
}
