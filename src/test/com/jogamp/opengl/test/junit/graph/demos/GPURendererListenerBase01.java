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
package com.jogamp.opengl.test.junit.graph.demos;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.media.opengl.FPSCounter;
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLPipelineFactory;
import javax.media.opengl.GLRunnable;


import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.Renderer;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.GLReadBufferUtil;

/**
 *
 * Action Keys:
 * - 1/2: zoom in/out
 * - 6/7: 2nd pass texture size
 * - 0/9: rotate 
 * - Q/W: change weight
 * - v: toggle v-sync
 * - s: screenshot
 */
public abstract class GPURendererListenerBase01 implements GLEventListener {
    private GLReadBufferUtil screenshot;
    private Renderer renderer;
    private int renderModes;
    private boolean debug;
    private boolean trace;
    
    protected GLRegion region;

    private KeyAction keyAction;
    
    private volatile GLAutoDrawable autoDrawable = null;
    
    private final float[] position = new float[] {0,0,0};
    
    private float xTran = -10;
    private float yTran =  10;    
    private float ang = 0f;
    private float zoom = -70f;
    private int texSize = 400; 

    protected volatile float weight = 1.0f;
    boolean ignoreInput = false;

    public GPURendererListenerBase01(Renderer renderer, int renderModes, boolean debug, boolean trace) {
        this.renderer = renderer;
        this.renderModes = renderModes;
        this.debug = debug;
        this.trace = trace;
        this.screenshot = new GLReadBufferUtil(false, false);
    }
    
    public final Renderer getRenderer() { return renderer; }
    public final int getRenderModes() { return renderModes; }
    public final float getZoom() { return zoom; }
    public final float getXTran() { return xTran; }
    public final float getYTran() { return yTran; }
    public final float getAngle() { return ang; }
    public final int getTexSize() { return texSize; }
    public final float[] getPosition() { return position; }

    public void setMatrix(float xtrans, float ytrans, float angle, int zoom, int fbosize) {
        this.xTran = xtrans;
        this.yTran = ytrans; 
        this.ang = angle;  
        this.zoom = zoom;
        this.texSize = fbosize;     
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
        System.err.println("*** "+gl.getContext().getGLVersion());
        System.err.println("*** GLDebugMessage "+gl.getContext().isGLDebugMessageEnabled());
        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        getRenderer().init(gl);
    }
    
    public void reshape(GLAutoDrawable drawable, int xstart, int ystart, int width, int height) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        
        gl.glViewport(xstart, ystart, width, height);        
        renderer.reshapePerspective(gl, 45.0f, width, height, 0.1f, 7000.0f);
        
        dumpMatrix();
        System.err.println("Reshape: "+renderer.getRenderState());        
    }
    
    public void dispose(GLAutoDrawable drawable) {
        autoDrawable = null;
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        if(null != region) {
            region.destroy(gl, renderer.getRenderState());
        }
        screenshot.dispose(gl);
        renderer.destroy(gl);
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
    public void editGlobalWeight(float delta) {
        if( !Renderer.isWeightValid(weight+delta) ) {
            return;
        }
        weight += delta;
        System.err.println("Global Weight: "+ weight);
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
    }
    
    public void detachInputListenerFrom(GLWindow window) {
        if ( null == keyAction ) {
            return;
        }
        window.removeKeyListener(keyAction);
    }
    
    public void printScreen(GLAutoDrawable drawable, String dir, String tech, String objName, boolean exportAlpha) throws GLException, IOException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.printf("-%03dx%03d-Z%04d-T%04d-%s", drawable.getWidth(), drawable.getHeight(), (int)Math.abs(zoom), texSize, objName);
        
        final String filename = dir + tech + sw +".tga";
        screenshot.readPixels(drawable.getGL(), drawable, false);
        screenshot.write(new File(filename));
    }
    
    int screenshot_num = 0;

    public void setIgnoreInput(boolean v) {
        ignoreInput = v;
    }
    public boolean getIgnoreInput() {
        return ignoreInput;
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
            else if(arg0.getKeyCode() == KeyEvent.VK_6){
                texSize -= 10;
                System.err.println("Tex Size: " + texSize);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_7){
                texSize += 10;
                System.err.println("Tex Size: " + texSize);
            }            
            else if(arg0.getKeyCode() == KeyEvent.VK_0){
                rotate(1);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_9){
                rotate(-1);
            }  
            else if(arg0.getKeyCode() == KeyEvent.VK_Q){
                editGlobalWeight(-0.1f);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_W){
                editGlobalWeight(0.1f);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_V) {
                if(null != autoDrawable) {
                    autoDrawable.invoke(false, new GLRunnable() {
                        public boolean run(GLAutoDrawable drawable) {
                            GL gl = drawable.getGL();
                            int i = gl.getSwapInterval();      
                            i = i==0 ? 1 : 0;
                            gl.setSwapInterval(i);
                            final GLAnimatorControl a = drawable.getAnimator();
                            if( null != a ) {
                                a.resetFPSCounter();
                            }
                            if(drawable instanceof FPSCounter) {
                                ((FPSCounter)drawable).resetFPSCounter();
                            }
                            System.err.println("Swap Interval: "+i);
                            return true;
                        }
                    });
                }                
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_S){
                rotate(-1);
                    if(null != autoDrawable) {
                        autoDrawable.invoke(false, new GLRunnable() {
                            public boolean run(GLAutoDrawable drawable) {
                                try {
                                    final String type = Region.isVBAA(renderModes) ? "vbaa0-msaa1" : "vbaa1-msaa0" + ( Region.isNonUniformWeight(renderModes) ? "-vc" : "-uc" ) ; 
                                    printScreen(drawable, "./", "demo-"+type, "snap"+screenshot_num, false);
                                    screenshot_num++;
                                } catch (GLException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }     
                                return true;
                            }
                        });
                    }                
            }  
        }
        public void keyTyped(KeyEvent arg0) {}
        public void keyReleased(KeyEvent arg0) {}
    }
}
