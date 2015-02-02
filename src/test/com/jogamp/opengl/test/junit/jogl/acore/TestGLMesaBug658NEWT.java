package com.jogamp.opengl.test.junit.jogl.acore;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * The 3.1 compatibility context on Mesa >= 9.0 seems to be broken.
 * <p>
 * This bug lies within Mesa3D (any renderer) and is fixed in
 * commit ??? (not yet).
 * </p>
 * <p>
 * Mesa3D Version 9.0 still exposes this bug,
 * where 9.?.? has it fixed w/ above commit.
 * </p>
 * <https://jogamp.org/bugzilla/show_bug.cgi?id=658>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLMesaBug658NEWT extends UITestCase {

  @Test
  public void test00ShowAvailProfiles() {
      System.err.println(JoglVersion.getDefaultOpenGLInfo(null, null, false).toString());
  }

  @Test
  public void test10GL2PolygonModeFailure() {
      testGLNPolygonModeFailureImpl(GLProfile.GL2);
  }

  @Test
  public void test11GL3bcPolygonModeFailure() {
      testGLNPolygonModeFailureImpl(GLProfile.GL3bc);
  }

  @Test
  public void test12GL3PolygonModeFailure() {
      testGLNPolygonModeFailureImpl(GLProfile.GL3);
  }

  private void testGLNPolygonModeFailureImpl(final String profile) {
    if(!GLProfile.isAvailable(profile)) { System.err.println(profile+" n/a"); return; }

    final GLProfile pro = GLProfile.get(profile);
    final GLCapabilities caps = new GLCapabilities(pro);
    final GLWindow window = GLWindow.create(caps);

    window.setSize(640, 480);
    window.addGLEventListener(new GLEventListener() {
      public void reshape(
        final GLAutoDrawable drawable,
        final int x,
        final int y,
        final int width,
        final int height)
      {
        // Nothing.
      }

      public void init(
        final GLAutoDrawable drawable)
      {
        final GLContext context = drawable.getContext();
        System.err.println("CTX: "+context.getGLVersion());

        final GL2GL3 gl = drawable.getGL().getGL2GL3();
        System.err.println("GL_VENDOR: " + gl.glGetString(GL.GL_VENDOR));
        System.err.println("GL_RENDERER: " + gl.glGetString(GL.GL_RENDERER));
        System.err.println("GL_VERSION: " + gl.glGetString(GL.GL_VERSION));
        System.err.println("GL Renderer Quirks:" + gl.getContext().getRendererQuirks().toString());

        if( gl.isGL2() || gl.isGLES2() ) { // compatibility profile || ES2
            gl.glPolygonMode(GL.GL_FRONT, GL2GL3.GL_FILL);
        } else {
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);
        }

        final int e = gl.glGetError();
        Assert.assertTrue(e == GL.GL_NO_ERROR); // // FIXME On Mesa 9.0.1 w/ GL 3.1 -> GL.GL_INVALID_OPERATION ?
      }

      public void dispose(
        final GLAutoDrawable drawable)
      {
        // Nothing.
      }

      public void display(
        final GLAutoDrawable drawable)
      {
        // Nothing.
      }
    });

    try {
        window.setVisible(true);
    } finally {
        window.destroy();
    }
  }

  @Test
  public void test20GL2BindArrayAttributeFails() {
      testGLNBindArrayAttributeFailsImpl(GLProfile.GL2);
  }

  @Test
  public void test21GL3bcBindArrayAttributeFails() {
      testGLNBindArrayAttributeFailsImpl(GLProfile.GL3bc);
  }

  @Test
  public void test22GL3BindArrayAttributeFails() {
      testGLNBindArrayAttributeFailsImpl(GLProfile.GL3);
  }

  private void testGLNBindArrayAttributeFailsImpl(final String profile) {
    if(!GLProfile.isAvailable(profile)) { System.err.println(profile+ " n/a"); return; }

    final GLProfile pro = GLProfile.get(profile);
    final GLCapabilities caps = new GLCapabilities(pro);
    final GLWindow window = GLWindow.create(caps);

    window.setSize(640, 480);
    window.addGLEventListener(new GLEventListener() {
      public void reshape(
        final GLAutoDrawable drawable,
        final int x,
        final int y,
        final int width,
        final int height)
      {
        // Nothing.
      }

      public void init(
        final GLAutoDrawable drawable)
      {
        final GLContext context = drawable.getContext();
        System.err.println("CTX: "+context.getGLVersion());

        final GL2GL3 gl = drawable.getGL().getGL2GL3();
        System.err.println("GL_VENDOR: " + gl.glGetString(GL.GL_VENDOR));
        System.err.println("GL_RENDERER: " + gl.glGetString(GL.GL_RENDERER));
        System.err.println("GL_VERSION: " + gl.glGetString(GL.GL_VERSION));
        System.err.println("GL Renderer Quirks:" + gl.getContext().getRendererQuirks().toString());

        final int[] name = new int[] { 0 };
        gl.glGenBuffers(1, name, 0);
        Assert.assertTrue(gl.glGetError() == GL.GL_NO_ERROR);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, name[0]);
        Assert.assertTrue(gl.glGetError() == 0);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, 4 * 32, null, GL.GL_STATIC_DRAW);
        Assert.assertTrue(gl.glGetError() == 0);

        Assert.assertTrue(gl.getBoundBuffer(GL.GL_ARRAY_BUFFER) == name[0]);
        gl.glEnableVertexAttribArray(1);
        Assert.assertTrue(gl.glGetError() == GL.GL_NO_ERROR);
        gl.glVertexAttribPointer(1, 4, GL.GL_FLOAT, false, 0, 0L);
        Assert.assertTrue(gl.glGetError() == GL.GL_NO_ERROR); // FIXME On Mesa 9.0.1 w/ GL 3.1 -> GL.GL_INVALID_OPERATION ?
      }

      public void dispose(
        final GLAutoDrawable drawable)
      {
        // Nothing.
      }

      public void display(
        final GLAutoDrawable drawable)
      {
        // Nothing.
      }
    });

    try {
        window.setVisible(true);
    } finally {
        window.destroy();
    }
  }

  public static void main(final String args[]) {
      org.junit.runner.JUnitCore.main(TestGLMesaBug658NEWT.class.getName());
  }

}

