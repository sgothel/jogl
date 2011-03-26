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
package demo;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLPipelineFactory;

import com.jogamp.graph.curve.text.HwTextRenderer;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.opengl.GLWindow;

public abstract class GPUTextGLListenerBase01 implements GLEventListener {
    Vertex.Factory<? extends Vertex> vfactory;
    HwTextRenderer textRenderer;    
    Font font;
    boolean debug;
    boolean trace;
    
    KeyAction keyAction;
    
    int fontSize = 40;
    final int fontSizeModulo = 100;    
    
    final float[] position = new float[] {0,0,0};
    
    float xTran = -10;
    float yTran =  10;    
    float ang = 0f;
    float zoom = -70f;
    // float zoom = -1000f;
    int texSize = 400; // FBO/tex size ..

    boolean doMatrix = true;
    static final String text1;
    static final String text2;

    static {
        text1 = "abcdef\nghijklmn\nopqrstuv\nwxyz\n0123456789";
        text2 = text1.toUpperCase();        
    }

    public GPUTextGLListenerBase01(Vertex.Factory<? extends Vertex> vfactory, int mode, boolean debug, boolean trace) {
        this.vfactory = vfactory;
        this.textRenderer = new HwTextRenderer(vfactory, mode);
        this.font = textRenderer.createFont(vfactory, "Lucida Sans Regular");
        this.debug = debug;
        this.trace = trace;
    }

    public void setMatrix(float xtrans, float ytrans, float angle, int zoom, int fbosize) {
        this.xTran = xtrans;
        this.yTran = ytrans; 
        this.ang = angle;  
        this.zoom = zoom;
        this.texSize = fbosize;        
    }
    
    public void init(GLAutoDrawable drawable) {
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
        textRenderer.reshape(gl, 45.0f, width, height, 0.1f, 7000.0f);
        
        dumpMatrix(true);
    }
    
    public void display(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        
        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f); // Demo02 needs to have this set here as well .. hmm ?
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        if(doMatrix) {
            textRenderer.resetMatrix(gl);
            textRenderer.translate(gl, xTran, yTran, zoom);
            textRenderer.rotate(gl, ang, 0, 1, 0);
            doMatrix = false;
        }

        textRenderer.renderString3D(gl, font, text2, position, fontSize, texSize);
    }        
        
    public void dispose(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        textRenderer.dispose(gl);
    }    
    
    public void fontIncr(int v) {
        fontSize = Math.abs((fontSize + v) % fontSizeModulo) ;
        dumpMatrix(true);
    }

    public void zoom(int v){
        zoom += v;
        doMatrix = true;
        dumpMatrix(false);
    }
    
    public void move(float x, float y){
        xTran += x;
        yTran += y;
        doMatrix = true;
        dumpMatrix(false);
    }
    public void rotate(float delta){
        ang += delta;
        ang %= 360.0f;
        doMatrix = true;
        dumpMatrix(false);
    }
    
    void dumpMatrix(boolean bbox) {
        System.err.println("Matrix: " + xTran + "/" + yTran + " x"+zoom + " @"+ang +" fontSize "+fontSize);
        if(bbox) {
            System.err.println("bbox: "+font.getStringBounds(text2, fontSize));
        }
    }
    
    public void attachTo(GLWindow window) {
        if ( null == keyAction ) {
            keyAction = new KeyAction();
        }
        window.addGLEventListener(this);
        window.addKeyListener(keyAction);        
    }
    
    public void detachFrom(GLWindow window) {
        if ( null == keyAction ) {
            return;
        }
        window.removeGLEventListener(this);
        window.removeKeyListener(keyAction);
    }
    
    public class KeyAction implements KeyListener {
        public void keyPressed(KeyEvent arg0) {
            if(arg0.getKeyCode() == KeyEvent.VK_1){
                zoom(10);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_2){
                zoom(-10);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_3){
                fontIncr(10);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_4){
                fontIncr(-10);
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
        }
        public void keyTyped(KeyEvent arg0) {}
        public void keyReleased(KeyEvent arg0) {}
    }
}