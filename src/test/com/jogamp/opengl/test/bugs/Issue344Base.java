package com.jogamp.opengl.test.bugs;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.*;
import java.awt.geom.*;

import com.jogamp.common.util.InterruptSource;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.*;

import com.jogamp.opengl.util.awt.TextRenderer;

/** Test Code adapted from TextCube.java (in JOGL demos)
 *
 * @author spiraljetty
 * @author kbr
 */

public abstract class Issue344Base implements GLEventListener
{
    GLU glu = new GLU();
    TextRenderer renderer;

    float textScaleFactor;
    Font font;
    boolean useMipMaps;

    protected Issue344Base() {
        font = new Font("default", Font.PLAIN, 200);
        useMipMaps = true; //false
    }

    protected abstract String getText();

    protected void run(final String[] args) {
        final Frame frame = new Frame(getClass().getName());
        frame.setLayout(new BorderLayout());

        final GLCanvas canvas = new GLCanvas();
        canvas.addGLEventListener(this);
        frame.add(canvas, BorderLayout.CENTER);

        frame.setSize(512, 512);
        frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(final WindowEvent e) {
                    new InterruptSource.Thread(null, new Runnable() {
                            public void run() {
                                System.exit(0);
                            }
                        }).start();
                }
            });
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setVisible(true);
                } } );
        } catch(final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void init(final GLAutoDrawable drawable)
    {
        final GL2 gl = drawable.getGL().getGL2();

        gl.glEnable(GL.GL_DEPTH_TEST);

        renderer = new TextRenderer(font, useMipMaps);

        final Rectangle2D bounds = renderer.getBounds(getText());
        final float w = (float) bounds.getWidth();
        // final float h = (float) bounds.getHeight();
        textScaleFactor = 2.0f / (w * 1.1f);
        gl.setSwapInterval(0);
    }

    public void display(final GLAutoDrawable drawable)
    {
        final GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        gl.glLoadIdentity();
        glu.gluLookAt(0, 0, 10,
                      0, 0, 0,
                      0, 1, 0);

        renderer.begin3DRendering();
        final Rectangle2D bounds = renderer.getBounds(getText());
        final float w = (float) bounds.getWidth();
        final float h = (float) bounds.getHeight();
        renderer.draw3D(getText(),
                        w / -2.0f * textScaleFactor,
                        h / -2.0f * textScaleFactor,
                        3f,
                        textScaleFactor);

        renderer.end3DRendering();
    }

    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height)
    {
        final GL2 gl = drawable.getGL().getGL2();
        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(15, (float) width / (float) height, 5, 15);
    }

    public void dispose(final GLAutoDrawable drawable) {}
}
