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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLPipelineFactory;
import javax.media.opengl.GLRunnable;

import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.TextRenderer;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.graph.demos.Screenshot;

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
public abstract class UIListenerBase01 implements GLEventListener {
    private Screenshot screenshot;
    private RegionRenderer rRenderer;
    private TextRenderer tRenderer;
    private boolean debug;
    private boolean trace;
    
    protected RIButton button;
    
    private KeyAction keyAction;
    private MouseAction mouseAction;
    
    private volatile GLAutoDrawable autoDrawable = null;
    
    private final float[] position = new float[] {0,0,0};
    
    private float xTran = -10;
    private float yTran =  10;    
    private float ang = 0f;
    private float zoom = -70f;

    boolean ignoreInput = false;

    public UIListenerBase01(RegionRenderer rRenderer, TextRenderer tRenderer, boolean debug, boolean trace) {
        this.rRenderer = rRenderer;
        this.tRenderer = tRenderer;
        this.debug = debug;
        this.trace = trace;
        this.screenshot = new Screenshot();
    }
    
    public final RegionRenderer getRegionRenderer() { return rRenderer; }
    public final TextRenderer getTextRenderer() { return tRenderer; }
    public final float getZoom() { return zoom; }
    public final float getXTran() { return xTran; }
    public final float getYTran() { return yTran; }
    public final float getAngle() { return ang; }
    public final float[] getPosition() { return position; }

    public void setMatrix(float xtrans, float ytrans, float angle, int zoom) {
        this.xTran = xtrans;
        this.yTran = ytrans; 
        this.ang = angle;  
        this.zoom = zoom;
    }
    
    public void init(GLAutoDrawable drawable) {
        autoDrawable = drawable;
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        if(debug) {
            gl = gl.getContext().setGL( GLPipelineFactory.create("javax.media.opengl.Debug", null, gl, null) ).getGL2ES2();
        }
        if(trace) {
            gl = gl.getContext().setGL( GLPipelineFactory.create("javax.media.opengl.Trace", null, gl, new Object[] { System.err } ) ).getGL2ES2();
        }
        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
    
    public void reshape(GLAutoDrawable drawable, int xstart, int ystart, int width, int height) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        
        gl.glViewport(xstart, ystart, width, height);        
        rRenderer.reshapePerspective(gl, 45.0f, width, height, 0.1f, 7000.0f);
        tRenderer.reshapePerspective(gl, 45.0f, width, height, 0.1f, 7000.0f);
        dumpMatrix();
    }
    
    public void dispose(GLAutoDrawable drawable) {
        autoDrawable = null;
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        screenshot.dispose(gl);
        rRenderer.destroy(gl);
        tRenderer.destroy(gl);
    }    
    
    public void zoom(int v){
        zoom += v;
        dumpMatrix();
    }
    
    public void move(float x, float y){
        xTran += x;
        yTran += y;
        dumpMatrix();
    }
    public void rotate(float delta){
        ang += delta;
        ang %= 360.0f;
        dumpMatrix();
    }
    
    void dumpMatrix() {
        System.err.println("Matrix: " + xTran + "/" + yTran + " x"+zoom + " @"+ang);
    }
    
    /** Attach the input listener to the window */ 
    public void attachInputListenerTo(GLWindow window) {
        if ( null == keyAction ) {
            keyAction = new KeyAction();
            window.addKeyListener(keyAction);        
        }
        if ( null == mouseAction ) {
            mouseAction = new MouseAction();
            window.addMouseListener(mouseAction);        
        }
    }
    
    public void detachFrom(GLWindow window) {
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
    
    public void printScreen(GLAutoDrawable drawable, String dir, String tech, String objName, boolean exportAlpha) throws GLException, IOException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.printf("-%03dx%03d-Z%04d-T%04d-%s", drawable.getWidth(), drawable.getHeight(), (int)Math.abs(zoom), 0, objName);
        
        String filename = dir + tech + sw +".tga";
        screenshot.surface2File(drawable, filename /*, exportAlpha */);
    }
    
    int screenshot_num = 0;

    public void setIgnoreInput(boolean v) {
        ignoreInput = v;
    }
    public boolean getIgnoreInput() {
        return ignoreInput;
    }
    
    public class MouseAction implements MouseListener{

        public void mouseClicked(MouseEvent e) {
            
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            button.setLabelColor(0.8f,0.8f,0.8f);
            button.setButtonColor(0.1f, 0.1f, 0.1f);
        }

        public void mouseReleased(MouseEvent e) {
            button.setLabelColor(1.0f,1.0f,1.0f);
            button.setButtonColor(0.6f,0.6f,0.6f);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void mouseWheelMoved(MouseEvent e) {
            // TODO Auto-generated method stub
            
        }
        
    }
    
    public class KeyAction implements KeyListener {
        public void keyPressed(KeyEvent arg0) {
            if(ignoreInput) {
                return;
            }
            
            if(arg0.getKeyCode() == KeyEvent.VK_1){
                zoom(10);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_2){
                zoom(-10);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_UP){
                move(0, -1);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_DOWN){
                move(0, 1);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_LEFT){
                move(1, 0);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_RIGHT){
                move(-1, 0);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_4){
                button.setSpacing(button.getSpacing()-0.1f);
                System.err.println("Button Spacing: " + button.getSpacing());
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_5){
                button.setSpacing(button.getSpacing()+0.1f);
                System.err.println("Button Spacing: " + button.getSpacing());
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_6){
                button.setCorner(button.getCorner()-0.1f);
                System.err.println("Button Corner: " + button.getCorner());
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_7){
                button.setCorner(button.getCorner()+0.1f);
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
                        public void run(GLAutoDrawable drawable) {
                            GL gl = drawable.getGL();
                            int i = gl.getSwapInterval();      
                            i = i==0 ? 1 : 0;
                            gl.setSwapInterval(i);
                            final GLAnimatorControl a = drawable.getAnimator();
                            if( null != a ) {
                                a.resetFPSCounter();
                            }
                            System.err.println("Swap Interval: "+i);
                        }
                    });
                }                
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_S){
                rotate(-1);
                    if(null != autoDrawable) {
                        autoDrawable.invoke(false, new GLRunnable() {
                            public void run(GLAutoDrawable drawable) {
                                try {
                                    final String type = ( 1 == rRenderer.getRenderType() ) ? "r2t0-msaa1" : "r2t1-msaa0" ; 
                                    printScreen(drawable, "./", "demo-"+type, "snap"+screenshot_num, false);
                                    screenshot_num++;
                                } catch (GLException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }                                
                            }
                        });
                    }                
            }  
        }
        public void keyTyped(KeyEvent arg0) {}
        public void keyReleased(KeyEvent arg0) {}
    }
}
