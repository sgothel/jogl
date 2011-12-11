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

import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.opengl.JoglVersion;

@SuppressWarnings("serial")
public class VersionApplet extends Applet {
  TextArea tareaVersion;
  TextArea tareaCaps;
  GLCanvas canvas;

  public static void main(String[] args) {
    Frame frame = new Frame("JOGL Version Applet");
    frame.setSize(800, 600);
    frame.setLayout(new BorderLayout());

    VersionApplet va = new VersionApplet();
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
    public ClosingWindowAdapter(Frame f, VersionApplet va) {
        this.f = f;
        this.va = va;
    }
    public void windowClosing(WindowEvent ev) {
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
    
    GLProfile glp = GLProfile.getDefault();
    GLCapabilities glcaps = new GLCapabilities(glp);

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
    GLDrawableFactory factory = GLDrawableFactory.getFactory(glp);
    List<GLCapabilitiesImmutable> availCaps = factory.getAvailableCapabilities(null);
    for(int i=0; i<availCaps.size(); i++) {
        s = ((GLCapabilitiesImmutable) availCaps.get(i)).toString();
        System.err.println(s);
        tareaCaps.append(s);
        tareaCaps.append(Platform.getNewline());
    }

    Container grid = new Container();
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

  public void init() {
    System.err.println("VersionApplet: init() - begin");
    my_init();
    System.err.println("VersionApplet: init() - end");
  }

  public void start() {
    System.err.println("VersionApplet: start() - begin");
    canvas.setVisible(true);
    System.err.println("VersionApplet: start() - end");
  }

  public void stop() {
    System.err.println("VersionApplet: stop() - begin");
    canvas.setVisible(false);
    System.err.println("VersionApplet: stop() - end");
  }

  public void destroy() {
    System.err.println("VersionApplet: destroy() - start");
    my_release();
    System.err.println("VersionApplet: destroy() - end");
  }

  class GLInfo implements GLEventListener {
    public void init(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();
        String s = JoglVersion.getGLInfo(gl, null).toString();
        System.err.println(s);
        tareaVersion.append(s);
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    }

    public void display(GLAutoDrawable drawable) {
    }

    public void dispose(GLAutoDrawable drawable) {
    }
  }

}
