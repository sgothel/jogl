package com.jogamp.opengl.test.bugs;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.*;
import java.awt.geom.*;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.*;
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

    protected void run(String[] args) {
        Frame frame = new Frame(getClass().getName());
        frame.setLayout(new BorderLayout());

        GLCanvas canvas = new GLCanvas();
        canvas.addGLEventListener(this);
        frame.add(canvas, BorderLayout.CENTER);

        frame.setSize(512, 512);
        frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    new Thread(new Runnable() {
                            public void run() {
                                System.exit(0);
                            }
                        }).start();
                }
            });
        frame.setVisible(true);
    }

    public void init(GLAutoDrawable drawable)
    {
        GL2 gl = drawable.getGL().getGL2();

        gl.glEnable(GL2.GL_DEPTH_TEST);

        renderer = new TextRenderer(font, useMipMaps);

        Rectangle2D bounds = renderer.getBounds(getText());
        float w = (float) bounds.getWidth();
        float h = (float) bounds.getHeight();
        textScaleFactor = 2.0f / (w * 1.1f);
        gl.setSwapInterval(0);
    }

    public void display(GLAutoDrawable drawable)
    {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        glu.gluLookAt(0, 0, 10,
                      0, 0, 0,
                      0, 1, 0);

        renderer.begin3DRendering();
        Rectangle2D bounds = renderer.getBounds(getText());
        float w = (float) bounds.getWidth();
        float h = (float) bounds.getHeight();
        renderer.draw3D(getText(),
                        w / -2.0f * textScaleFactor,
                        h / -2.0f * textScaleFactor,
                        3f,
                        textScaleFactor);
        
        renderer.end3DRendering();
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
    {
        GL2 gl = drawable.getGL().getGL2();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(15, (float) width / (float) height, 5, 15);
    }

    public void dispose(GLAutoDrawable drawable) {}
}
