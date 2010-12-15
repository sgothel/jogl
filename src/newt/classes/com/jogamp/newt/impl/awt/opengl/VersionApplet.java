package com.jogamp.newt.impl.awt.opengl;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.nativewindow.NativeWindowVersion;
import com.jogamp.newt.NewtVersion;
import com.jogamp.opengl.JoglVersion;
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.TextArea;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

public class VersionApplet extends Applet {
    TextArea tarea;

  public void init() {
    System.err.println("VersionApplet: init() - begin");
    GLProfile.initSingleton(false);
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

    GLCanvas canvas = new GLCanvas();
    canvas.addGLEventListener(new GLInfo());
    add(canvas, BorderLayout.SOUTH);
    System.err.println("VersionApplet: init() - end");
  }

  public void start() {
    System.err.println("VersionApplet: start() - begin");
    System.err.println("VersionApplet: start() - end");
  }

  public void stop() {
    // FIXME: do I need to do anything else here?
    System.err.println("VersionApplet: stop() - begin");
    System.err.println("VersionApplet: stop() - end");
  }

  public void destroy() {
    System.err.println("VersionApplet: destroy() - X");
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
