package com.jogamp.opengl.test.bugs;

import java.awt.Font;
import java.awt.Frame;
import java.awt.event.*;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;

import com.jogamp.opengl.util.awt.*;

/**
 * Another test case demonstrating corruption with older version of
 * TextRenderer when glyphs were too big for backing store. Font and
 * text courtesy of Patrick Murris. Adapted from Issue326Test1.
 */

public class Issue326Test2 extends Frame implements GLEventListener {

    int width, height;

    public static void main(String[] args) {
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
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });
    }

    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0, 0, 0, 0);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT|GL2.GL_DEPTH_BUFFER_BIT);
        
        tr.beginRendering(drawable.getWidth(), drawable.getHeight());
        tr.draw("LA CLAPI\u00c8RE \nAlt: 1100-1700m \nGlissement de terrain majeur", 16, 80);
        tr.draw("dans la haute Tin\u00e9e, sur un flanc du Parc du Mercantour.", 16, 16);
        tr.endRendering();
        
    }

    public void init(GLAutoDrawable arg0) {
        tr = new TextRenderer(Font.decode("Arial-BOLD-64"));
        tr.setColor(1, 1, 1 ,1);
    }

    public void reshape(GLAutoDrawable arg0, int x, int y, int w, int h) {
        GL2 gl = arg0.getGL().getGL2();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(0.0, w, 0.0, h, -1, 1);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    public void dispose(GLAutoDrawable drawable) {}
}

