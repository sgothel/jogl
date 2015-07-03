package com.jogamp.opengl.test.bugs;

import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLU;

import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.awt.TextRenderer;

/**
 * Demonstrates corruption with older versions of TextRenderer. Two
 * problems: errors when punting from glyph-based renderer to
 * string-by-string renderer, and failure of glyph-based renderer when
 * backing store was NPOT using GL_ARB_texture_rectangle.
 *
 * @author emzic
 */

public class Issue326Test1 extends Frame implements GLEventListener {

    int width, height;

    public static void main(final String[] args) {
        new Issue326Test1();
    }

    GLCanvas canvas;
    TextRenderer tr ;

    public Issue326Test1() {
        super("TextTest");
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


        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        gl.glLoadIdentity();
        //new GLU().gluPerspective(45f, (float)width/(float)height, 0.1f, 1000f);
        gl.glOrtho(0.0, 800, 0.0, 800, -100.0, 100.0);
        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        gl.glLoadIdentity();

        tr.beginRendering(800,800);
        tr.draw( "die Marktwirtschaft. Da regelt sich � angeblich", 16, 32);
        tr.draw( "Hello World! This text is scrambled", 16, 16);
        tr.endRendering();

    }

    public void init(final GLAutoDrawable arg0) {
        tr = new TextRenderer(new java.awt.Font("Verdana", java.awt.Font.PLAIN, 12), true, false, null, false);
        tr.setColor(1, 1, 1 ,1);
    }

    public void reshape(final GLAutoDrawable arg0, final int arg1, final int arg2, final int arg3, final int arg4) {
        width = arg3;
        height = arg4;
        final GL2 gl = arg0.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(0.0, 800, 0.0, 200, -100.0, 100.0);
        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    public void dispose(final GLAutoDrawable drawable) {}
}
