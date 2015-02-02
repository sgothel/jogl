package com.jogamp.opengl.test.bugs;

import java.awt.Font;
import java.awt.Frame;
import java.awt.event.*;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.opengl.util.awt.*;

/**
 * Another test case demonstrating corruption with older version of
 * TextRenderer when glyphs were too big for backing store. Font and
 * text courtesy of Patrick Murris. Adapted from Issue326Test1.
 */

public class Issue326Test2 extends Frame implements GLEventListener {

    int width, height;

    public static void main(final String[] args) {
        new Issue326Test2();
    }

    GLCanvas canvas;
    TextRenderer tr;

    public Issue326Test2() {
        super("");
        this.setSize(800, 800);
        canvas = new GLCanvas();
        canvas.addGLEventListener(this);
        add(canvas);

        setVisible(true);
        addWindowListener(new WindowAdapter() {
                public void windowClosing(final WindowEvent e) {
                    System.exit(0);
                }
            });
    }

    public void display(final GLAutoDrawable drawable) {
        final GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0, 0, 0, 0);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT|GL.GL_DEPTH_BUFFER_BIT);

        tr.beginRendering(drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
        tr.draw("LA CLAPI\u00c8RE \nAlt: 1100-1700m \nGlissement de terrain majeur", 16, 80);
        tr.draw("dans la haute Tin\u00e9e, sur un flanc du Parc du Mercantour.", 16, 16);
        tr.endRendering();

    }

    public void init(final GLAutoDrawable arg0) {
        tr = new TextRenderer(Font.decode("Arial-BOLD-64"));
        tr.setColor(1, 1, 1 ,1);
    }

    public void reshape(final GLAutoDrawable arg0, final int x, final int y, final int w, final int h) {
        final GL2 gl = arg0.getGL().getGL2();
        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(0.0, w, 0.0, h, -1, 1);
        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    public void dispose(final GLAutoDrawable drawable) {}
}

