package com.jogamp.opengl.test.bugs;

import javax.swing.*;
import java.awt.*;
import javax.media.opengl.*;
import javax.media.opengl.awt.*;

public class Bug427GLJPanelTest1 extends JFrame implements GLEventListener {

    public Bug427GLJPanelTest1() {
        super("Bug427GLJPanelTest1");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        setSize(600, 600);
        setLocation(40, 40);
        setVisible(true);

        GLProfile glp = GLProfile.getDefault();
        GLCapabilities caps = new GLCapabilities(glp);
        caps.setDoubleBuffered(true);
        caps.setHardwareAccelerated(true);

        GLJPanel panel = new GLJPanel(caps);
        panel.addGLEventListener(this);

        add(panel, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        Bug427GLJPanelTest1 demo = new Bug427GLJPanelTest1();
        demo.setVisible(true);
    }

    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

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

    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glClearColor(0, 0, 0, 0);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(0, 1, 0, 1, -1, 1);
    }

    public void reshape(GLAutoDrawable glDrawable, int x, int y, int w, int h) {
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }
    
    public void dispose(GLAutoDrawable drawable) {
    }
}
