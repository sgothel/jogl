package com.jogamp.opengl.test.bugs;

import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;

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

    public static void main(String[] args) {
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
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });
    }

    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0, 0, 0, 0);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT|GL2.GL_DEPTH_BUFFER_BIT);
        
        
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();        
        //new GLU().gluPerspective(45f, (float)width/(float)height, 0.1f, 1000f);
        gl.glOrtho(0.0, 800, 0.0, 800, -100.0, 100.0);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        
        tr.beginRendering(800,800);
        tr.draw( "die Marktwirtschaft. Da regelt sich � angeblich", 16, 32);
        tr.draw( "Hello World! This text is scrambled", 16, 16);
        tr.endRendering();
        
    }

    public void init(GLAutoDrawable arg0) {
        tr = new TextRenderer(new java.awt.Font("Verdana", java.awt.Font.PLAIN, 12), true, false, null, false);
        tr.setColor(1, 1, 1 ,1);
    }

    public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3, int arg4) {
        width = arg3;
        height = arg4;
        GL2 gl = arg0.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(0.0, 800, 0.0, 200, -100.0, 100.0);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    public void dispose(GLAutoDrawable drawable) {}
}
