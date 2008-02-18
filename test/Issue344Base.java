import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.*;
import java.awt.geom.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.util.*;
import com.sun.opengl.util.j2d.*;

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
        frame.show();
    }

    public void init(GLAutoDrawable drawable)
    {
        GL gl = drawable.getGL();

        gl.glEnable(GL.GL_DEPTH_TEST);

        renderer = new TextRenderer(font, useMipMaps);

        Rectangle2D bounds = renderer.getBounds(getText());
        float w = (float) bounds.getWidth();
        float h = (float) bounds.getHeight();
        textScaleFactor = 2.0f / (w * 1.1f);
        gl.setSwapInterval(0);
    }

    public void display(GLAutoDrawable drawable)
    {
        GL gl = drawable.getGL();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        gl.glMatrixMode(GL.GL_MODELVIEW);
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
        GL gl = drawable.getGL();
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(15, (float) width / (float) height, 5, 15);
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged,
                               boolean deviceChanged)
    {
    }
}
