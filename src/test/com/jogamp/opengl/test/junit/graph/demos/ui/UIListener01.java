/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package com.jogamp.opengl.test.junit.graph.demos.ui;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.jogamp.opengl.FPSCounter;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLPipelineFactory;
import com.jogamp.opengl.GLRunnable;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.test.junit.graph.demos.MSAATool;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.PMVMatrix;

/**
 *
 * Action Keys:
 * - 1/2: zoom in/out
 * - 4/5: increase/decrease shape/text spacing
 * - 6/7: increase/decrease corner size
 * - 0/9: rotate
 * - v: toggle v-sync
 * - s: screenshot
 */
public class UIListener01 implements GLEventListener {
    private final GLReadBufferUtil screenshot;
    private final int renderModes;
    private final RegionRenderer rRenderer;
    private final boolean debug;
    private final boolean trace;

    protected LabelButton button;

    private KeyAction keyAction;
    private MouseAction mouseAction;

    private volatile GLAutoDrawable autoDrawable = null;

    private final float[] position = new float[] {0,0,0};

    private float xTran = -10;
    private float yTran =  10;
    private float ang = 0f;
    private float zoom = -70f;

    boolean ignoreInput = false;

    public UIListener01(final int renderModes, final RenderState rs, final boolean debug, final boolean trace) {
        this.renderModes = renderModes;
        this.rRenderer = RegionRenderer.create(rs, RegionRenderer.defaultBlendEnable, RegionRenderer.defaultBlendDisable);
        this.debug = debug;
        this.trace = trace;
        this.screenshot = new GLReadBufferUtil(false, false);

        setMatrix(-4, -2, 0f, -10);
        try {
            final Font font = FontFactory.get(FontFactory.UBUNTU).getDefault();
            button = new LabelButton(SVertex.factory(), 0, font, "Click me!", 4f, 2f);
            button.translate(2,1,0);
            /** Button defaults !
                button.setLabelColor(1.0f,1.0f,1.0f);
                button.setButtonColor(0.6f,0.6f,0.6f);
                button.setCorner(1.0f);
                button.setSpacing(2.0f);
             */
            System.err.println(button);
        } catch (final IOException ex) {
            System.err.println("Caught: "+ex.getMessage());
            ex.printStackTrace();
        }
    }

    public final RegionRenderer getRegionRenderer() { return rRenderer; }
    public final float getZoom() { return zoom; }
    public final float getXTran() { return xTran; }
    public final float getYTran() { return yTran; }
    public final float getAngle() { return ang; }
    public final float[] getPosition() { return position; }

    public void setMatrix(final float xtrans, final float ytrans, final float angle, final int zoom) {
        this.xTran = xtrans;
        this.yTran = ytrans;
        this.ang = angle;
        this.zoom = zoom;
    }

    @Override
    public void init(final GLAutoDrawable drawable) {
        autoDrawable = drawable;
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        if(debug) {
            gl = gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Debug", null, gl, null) ).getGL2ES2();
        }
        if(trace) {
            gl = gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Trace", null, gl, new Object[] { System.err } ) ).getGL2ES2();
        }
        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        getRegionRenderer().init(gl, renderModes);

        gl.setSwapInterval(1);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
        MSAATool.dump(drawable);
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int xstart, final int ystart, final int width, final int height) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glViewport(xstart, ystart, width, height);
        rRenderer.reshapePerspective(45.0f, width, height, 0.1f, 7000.0f);
        dumpMatrix();
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        final int[] sampleCount = { 4 };
        final float[] translate = button.getTranslate();

        final RegionRenderer regionRenderer = getRegionRenderer();
        final PMVMatrix pmv = regionRenderer.getMatrix();
        pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmv.glLoadIdentity();
        pmv.glTranslatef(getXTran(), getYTran(), getZoom());
        pmv.glRotatef(getAngle(), 0, 1, 0);
        pmv.glTranslatef(translate[0], translate[1], 0);
        button.drawShape(gl, regionRenderer, sampleCount);
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        button.destroy(gl, getRegionRenderer());

        autoDrawable = null;
        screenshot.dispose(gl);
        rRenderer.destroy(gl);
    }

    public void zoom(final int v){
        zoom += v;
        dumpMatrix();
    }

    public void move(final float x, final float y){
        xTran += x;
        yTran += y;
        dumpMatrix();
    }
    public void rotate(final float delta){
        ang += delta;
        ang %= 360.0f;
        dumpMatrix();
    }

    void dumpMatrix() {
        System.err.println("Matrix: " + xTran + "/" + yTran + " x"+zoom + " @"+ang);
    }

    /** Attach the input listener to the window */
    public void attachInputListenerTo(final GLWindow window) {
        if ( null == keyAction ) {
            keyAction = new KeyAction();
            window.addKeyListener(keyAction);
        }
        if ( null == mouseAction ) {
            mouseAction = new MouseAction();
            window.addMouseListener(mouseAction);
        }
    }

    public void detachFrom(final GLWindow window) {
        if ( null == keyAction ) {
            return;
        }
        if ( null == mouseAction ) {
            return;
        }
        window.removeGLEventListener(this);
        window.removeKeyListener(keyAction);
        window.removeMouseListener(mouseAction);
    }

    public void printScreen(final GLAutoDrawable drawable, final String dir, final String tech, final String objName, final boolean exportAlpha) throws GLException, IOException {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        pw.printf("-%03dx%03d-Z%04d-T%04d-%s", drawable.getSurfaceWidth(), drawable.getSurfaceHeight(), (int)Math.abs(zoom), 0, objName);

        final String filename = dir + tech + sw +".png";
        if(screenshot.readPixels(drawable.getGL(), false)) {
            screenshot.write(new File(filename));
        }
    }

    int screenshot_num = 0;

    public void setIgnoreInput(final boolean v) {
        ignoreInput = v;
    }
    public boolean getIgnoreInput() {
        return ignoreInput;
    }

    public class MouseAction implements MouseListener{

        @Override
        public void mouseClicked(final MouseEvent e) {

        }

        @Override
        public void mouseEntered(final MouseEvent e) {
        }

        @Override
        public void mouseExited(final MouseEvent e) {
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            button.setLabelColor(0.8f,0.8f,0.8f);
            button.setColor(0.1f, 0.1f, 0.1f, 1.0f);
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            button.setLabelColor(1.0f,1.0f,1.0f);
            button.setColor(0.6f,0.6f,0.6f, 1.0f);
        }

        @Override
        public void mouseMoved(final MouseEvent e) {
            // TODO Auto-generated method stub

        }

        @Override
        public void mouseDragged(final MouseEvent e) {
            // TODO Auto-generated method stub

        }

        @Override
        public void mouseWheelMoved(final MouseEvent e) {
            // TODO Auto-generated method stub

        }

    }

    public class KeyAction implements KeyListener {
        @Override
        public void keyPressed(final KeyEvent arg0) {
            if(ignoreInput) {
                return;
            }

            if(arg0.getKeyCode() == KeyEvent.VK_1){
                zoom(2);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_2){
                zoom(-2);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_UP){
                move(0, 1);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_DOWN){
                move(0, -1);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_LEFT){
                move(-1, 0);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_RIGHT){
                move(1, 0);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_4){
                button.setSpacing(button.getSpacingX()-0.01f, button.getSpacingY()-0.005f);
                System.err.println("Button Spacing: " + button.getSpacingX());
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_5){
                button.setSpacing(button.getSpacingX()+0.01f, button.getSpacingY()+0.005f);
                System.err.println("Button Spacing: " + button.getSpacingX());
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_6){
                button.setCorner(button.getCorner()-0.01f);
                System.err.println("Button Corner: " + button.getCorner());
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_7){
                button.setCorner(button.getCorner()+0.01f);
                System.err.println("Button Corner: " + button.getCorner());
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_0){
                rotate(1);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_9){
                rotate(-1);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_V) {
                if(null != autoDrawable) {
                    autoDrawable.invoke(false, new GLRunnable() {
                        @Override
                        public boolean run(final GLAutoDrawable drawable) {
                            final GL gl = drawable.getGL();
                            final int _i = gl.getSwapInterval();
                            final int i;
                            switch(_i) {
                                case  0: i =  1; break;
                                case  1: i = -1; break;
                                case -1: i =  0; break;
                                default: i =  1; break;
                            }
                            gl.setSwapInterval(i);

                            final GLAnimatorControl a = drawable.getAnimator();
                            if( null != a ) {
                                a.resetFPSCounter();
                            }
                            if(drawable instanceof FPSCounter) {
                                ((FPSCounter)drawable).resetFPSCounter();
                            }
                            System.err.println("Swap Interval: "+_i+" -> "+i+" -> "+gl.getSwapInterval());
                            return true;
                        }
                    });
                }
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_S){
                rotate(-1);
                    if(null != autoDrawable) {
                        autoDrawable.invoke(false, new GLRunnable() {
                            @Override
                            public boolean run(final GLAutoDrawable drawable) {
                                try {
                                    final String type = Region.getRenderModeString(renderModes);
                                    printScreen(drawable, "./", "demo-"+type, "snap"+screenshot_num, false);
                                    screenshot_num++;
                                } catch (final GLException e) {
                                    e.printStackTrace();
                                } catch (final IOException e) {
                                    e.printStackTrace();
                                }
                                return true;
                            }
                        });
                    }
            }
        }
        @Override
        public void keyReleased(final KeyEvent arg0) {}
    }
}
