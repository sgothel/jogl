package com.jogamp.newt.impl.awt.opengl;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.nativewindow.NativeWindowVersion;
import com.jogamp.newt.NewtVersion;
import com.jogamp.opengl.JoglVersion;
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.TextArea;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

public class VersionApplet extends Applet {
  static {
    GLProfile.initSingleton(false);
  }
  TextArea tarea;
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

    setLayout(new BorderLayout());
    String s;

    tarea = new TextArea(120, 80);
    s = VersionUtil.getPlatformInfo().toString();
    System.err.println(s);
    tarea.append(s);

    s = GlueGenVersion.getInstance().toString();
    System.err.println(s);
    tarea.append(s);

    s = NativeWindowVersion.getInstance().toString();
    System.err.println(s);
    tarea.append(NativeWindowVersion.getInstance().toString());

    s = JoglVersion.getInstance().toString();
    System.err.println(s);
    tarea.append(s);

    s = NewtVersion.getInstance().toString();
    System.err.println(s);
    tarea.append(s);

    add(tarea, BorderLayout.CENTER);

    canvas = new GLCanvas();
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
          remove(tarea);
          tarea=null;
      }
  }

  public void init() {
    System.err.println("VersionApplet: init() - begin");
    my_init();
    System.err.println("VersionApplet: init() - end");
  }

  public void start() {
    System.err.println("VersionApplet: start() - begin");
    System.err.println("VersionApplet: start() - end");
  }

  public void stop() {
    System.err.println("VersionApplet: stop() - begin");
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
        String s = JoglVersion.getInstance().getGLInfo(gl, null).toString();
        System.err.println(s);
        tarea.append(s);
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    }

    public void display(GLAutoDrawable drawable) {
    }

    public void dispose(GLAutoDrawable drawable) {
    }
  }

}
