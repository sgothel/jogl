package jogamp.opengl.awt;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.TextArea;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.util.List;

import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.opengl.JoglVersion;

@SuppressWarnings("serial")
public class VersionApplet extends Applet {
  TextArea tareaVersion;
  TextArea tareaCaps;
  GLCanvas canvas;

  public static void main(final String[] args) {
    final Frame frame = new Frame("JOGL Version Applet");
    frame.setSize(800, 600);
    frame.setLayout(new BorderLayout());

    final VersionApplet va = new VersionApplet();
    frame.addWindowListener(new ClosingWindowAdapter(frame, va));

    va.init();
    frame.add(va, BorderLayout.CENTER);
    frame.validate();

    frame.setVisible(true);
    va.start();
  }

  static class ClosingWindowAdapter extends WindowAdapter {
    Frame f;
    VersionApplet va;
    public ClosingWindowAdapter(final Frame f, final VersionApplet va) {
        this.f = f;
        this.va = va;
    }
    @Override
    public void windowClosing(final WindowEvent ev) {
        f.setVisible(false);
        va.stop();
        va.destroy();
        f.remove(va);
        f.dispose();
        System.exit(0);
    }
  }

  private synchronized void my_init() {
    if(null != canvas) { return; }

    setEnabled(true);

    final GLProfile glp = GLProfile.getDefault();
    final GLCapabilities glcaps = new GLCapabilities(glp);

    setLayout(new BorderLayout());
    String s;

    tareaVersion = new TextArea(120, 60);
    s = VersionUtil.getPlatformInfo().toString();
    System.err.println(s);
    tareaVersion.append(s);

    s = GlueGenVersion.getInstance().toString();
    System.err.println(s);
    tareaVersion.append(s);

    /*
    s = NativeWindowVersion.getInstance().toString();
    System.err.println(s);
    tareaVersion.append(NativeWindowVersion.getInstance().toString());
    */

    s = JoglVersion.getInstance().toString();
    System.err.println(s);
    tareaVersion.append(s);

    tareaCaps = new TextArea(120, 20);
    final GLDrawableFactory factory = GLDrawableFactory.getFactory(glp);
    final List<GLCapabilitiesImmutable> availCaps = factory.getAvailableCapabilities(null);
    for(int i=0; i<availCaps.size(); i++) {
        s = availCaps.get(i).toString();
        System.err.println(s);
        tareaCaps.append(s);
        tareaCaps.append(Platform.getNewline());
    }

    final Container grid = new Container();
    grid.setLayout(new GridLayout(2, 1));
    grid.add(tareaVersion);
    grid.add(tareaCaps);
    add(grid, BorderLayout.CENTER);

    canvas = new GLCanvas(glcaps);
    canvas.addGLEventListener(new GLInfo());
    canvas.setSize(10, 10);
    add(canvas, BorderLayout.SOUTH);
    validate();
  }

  private synchronized void my_release() {
      if(null!=canvas) {
          remove(canvas);
          canvas.destroy();
          canvas = null;
          remove(tareaVersion.getParent()); // remove the grid
          tareaVersion = null;
          tareaCaps = null;
          setEnabled(false);
      }
  }

  @Override
  public void init() {
    System.err.println("VersionApplet: init() - begin");
    my_init();
    System.err.println("VersionApplet: init() - end");
  }

  @Override
  public void start() {
    System.err.println("VersionApplet: start() - begin");
    canvas.setVisible(true);
    System.err.println("VersionApplet: start() - end");
  }

  @Override
  public void stop() {
    System.err.println("VersionApplet: stop() - begin");
    canvas.setVisible(false);
    System.err.println("VersionApplet: stop() - end");
  }

  @Override
  public void destroy() {
    System.err.println("VersionApplet: destroy() - start");
    my_release();
    System.err.println("VersionApplet: destroy() - end");
  }

  class GLInfo implements GLEventListener {
    @Override
    public void init(final GLAutoDrawable drawable) {
        final GL gl = drawable.getGL();
        final String s = JoglVersion.getGLInfo(gl, null).toString();
        System.err.println(s);
        tareaVersion.append(s);
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
    }
  }

}
