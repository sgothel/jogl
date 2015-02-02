package com.jogamp.opengl.test.bugs;

import javax.swing.*;

import java.awt.*;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.*;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

public class Bug427GLJPanelTest1 extends JFrame implements GLEventListener {

    public Bug427GLJPanelTest1() {
        super("Bug427GLJPanelTest1");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    setSize(600, 600);
                    setLocation(40, 40);
                    setVisible(true);
                } } );
        } catch(final Exception ex) {
            throw new RuntimeException(ex);
        }

        final GLProfile glp = GLProfile.get(GLProfile.GL2);
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setDoubleBuffered(true);
        caps.setHardwareAccelerated(true);

        final GLJPanel panel = new GLJPanel(caps);
        panel.addGLEventListener(this);

        add(panel, BorderLayout.CENTER);
    }

    public static void main(final String[] args) {
        final Bug427GLJPanelTest1 demo = new Bug427GLJPanelTest1();
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    demo.setVisible(true);
                } } );
        } catch(final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void display(final GLAutoDrawable drawable) {
        final GL2 gl = drawable.getGL().getGL2();

        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        gl.glBegin(GL.GL_TRIANGLES);

        gl.glColor3f(1, 0, 0);
        gl.glVertex3f(0.25f, 0.25f, 0);

        gl.glColor3f(0, 1, 0);
        gl.glVertex3f(0.5f, 0.25f, 0);

        gl.glColor3f(0, 0, 1);
        gl.glVertex3f(0.25f, 0.5f, 0);

        gl.glEnd();
        gl.glFlush();
    }

    public void init(final GLAutoDrawable drawable) {
        final GL2 gl = drawable.getGL().getGL2();

        gl.glClearColor(0, 0, 0, 0);
        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(0, 1, 0, 1, -1, 1);
    }

    public void reshape(final GLAutoDrawable glDrawable, final int x, final int y, final int w, final int h) {
    }

    public void displayChanged(final GLAutoDrawable drawable, final boolean modeChanged, final boolean deviceChanged) {
    }

    public void dispose(final GLAutoDrawable drawable) {
    }
}
