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

import java.io.File;
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

import com.jogamp.graph.curve.HwTextRenderer;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.awt.Screenshot;

/**
 *
 * Action Keys:
 * - 1/2: zoom in/out
 * - 3/4: font +/-
 * - 6/7: 2nd pass texture size
 * - 0/9: rotate 
 * - s: toogle draw 'font set'
 * - f: toggle draw fps
 * - v: toggle v-sync
 * - space: toggle font (ubuntu/java)
 */
public abstract class GPUTextGLListenerBase01 implements GLEventListener {
    Vertex.Factory<? extends Vertex> vfactory;
    protected HwTextRenderer textRenderer;
    int fontSet = FontFactory.UBUNTU;
    Font font;
    boolean debug;
    boolean trace;
    
    KeyAction keyAction;
    
    volatile GLAutoDrawable autoDrawable = null;
    boolean drawFontSet = true;
    boolean drawFPS = true;
    boolean updateFont = true;
    int fontSize = 40;
    final int fontSizeModulo = 100;    
    
    final float[] position = new float[] {0,0,0};
    
    float xTran = -10;
    float yTran =  10;    
    float ang = 0f;
    float zoom = -70f;
    int texSize = 400; 

    boolean updateMatrix = true;
    static final String text1;
    static final String text2;

    static {
        text1 = "abcdefghijklmnopqrstuvwxyz\nABCDEFGHIJKLMNOPQRSTUVWXYZ\n0123456789.:,;(*!?/\\\")$%^&-+@~#<>{}[]";
        text2 = "The quick brown fox jumps over the lazy dog";      
    }

    public GPUTextGLListenerBase01(Vertex.Factory<? extends Vertex> vfactory, int mode, boolean debug, boolean trace) {
        // this.font = FontFactory.get(FontFactory.JAVA).getDefault();
        this.font = FontFactory.get(fontSet).getDefault();
        this.vfactory = vfactory;
        this.textRenderer = new HwTextRenderer(vfactory, mode);
        this.debug = debug;
        this.trace = trace;
    }

    public void setMatrix(float xtrans, float ytrans, float angle, int zoom, int fbosize) {
        this.xTran = xtrans;
        this.yTran = ytrans; 
        this.ang = angle;  
        this.zoom = zoom;
        this.texSize = fbosize;     
        updateMatrix = true;
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
        textRenderer.reshapePerspective(gl, 45.0f, width, height, 0.1f, 7000.0f);
        
        dumpMatrix(true);
    }
    protected boolean printScreen = true;
    public void display(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        
        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f); // Demo02 needs to have this set here as well .. hmm ?
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        if(drawFPS || drawFontSet || updateMatrix) {
            final int width = drawable.getWidth();
            final int height = drawable.getHeight();
            final GLAnimatorControl animator = drawable.getAnimator();
            final boolean _drawFPS = drawFPS && null != animator && animator.getTotalFrames()>10;
            
            if(_drawFPS || drawFontSet) {
                textRenderer.reshapeOrtho(null, width, height, 0.1f, 7000.0f);                
            }
            if(_drawFPS) {
                final float fps = ( animator.getTotalFrames() * 1000.0f ) / (float) animator.getDuration() ;
                final String fpsS = String.valueOf(fps);
                final int fpsSp = fpsS.indexOf('.');
                textRenderer.resetMatrix(null);
                textRenderer.translate(gl, 0, 0, -6000);
                textRenderer.renderString3D(gl, font, fpsS.substring(0, fpsSp+2), position, fontSize, texSize);
            }
            if(drawFontSet) { 
                textRenderer.resetMatrix(null);
                textRenderer.translate(gl, 0, height-50, -6000);
                textRenderer.renderString3D(gl, font, text1, position, fontSize, texSize);
            }
            if(_drawFPS || drawFontSet) {
                textRenderer.reshapePerspective(null, 45.0f, width, height, 0.1f, 7000.0f);             
            }
            		
            textRenderer.resetMatrix(null);            
            textRenderer.translate(null, xTran, yTran, zoom);
            textRenderer.rotate(gl, ang, 0, 1, 0);
            updateMatrix = false;
        }

        textRenderer.renderString3D(gl, font, text2, position, fontSize, texSize);
    }        
        
    public void dispose(GLAutoDrawable drawable) {
        autoDrawable = null;
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        textRenderer.dispose(gl);
    }    
    
    public void fontIncr(int v) {
        fontSize = Math.abs((fontSize + v) % fontSizeModulo) ;
        updateFont = true;
        dumpMatrix(true);
    }

    public void zoom(int v){
        zoom += v;
        updateMatrix = true;
        dumpMatrix(false);
    }
    
    public void nextFontSet() {
        fontSet = ( fontSet == FontFactory.UBUNTU ) ? FontFactory.JAVA : FontFactory.UBUNTU ;
        font = FontFactory.get(fontSet).getDefault();        
    }
    
    public void setFontSet(int set, int family, int stylebits) {
        fontSet = set;
        font = FontFactory.get(fontSet).get(family, stylebits);        
    }
    
    public void move(float x, float y){
        xTran += x;
        yTran += y;
        updateMatrix = true;
        dumpMatrix(false);
    }
    public void rotate(float delta){
        ang += delta;
        ang %= 360.0f;
        updateMatrix = true;
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
    
    public void printScreen(String dir, String tech, int width, int height, boolean exportAlpha) throws GLException, IOException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.printf("-%03dx%03d-Z%04d-T%04d-%s", width, height, (int)Math.abs(zoom), texSize, font.getName());
        
    	String filename = dir + tech + sw +".tga";
    	Screenshot.writeToTargaFile(new File(filename), width, height, exportAlpha);
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
            else if(arg0.getKeyChar() == 's') {
                drawFontSet = !drawFontSet; 
                System.err.println("Draw font set: "+drawFontSet);
            }  
            else if(arg0.getKeyChar() == 'f'){
                drawFPS = !drawFPS; 
                System.err.println("Draw FPS: "+drawFPS);
            }  
            else if(arg0.getKeyChar() == 'v') {
                if(null != autoDrawable) {
                    autoDrawable.invoke(false, new GLRunnable() {
                        public void run(GLAutoDrawable drawable) {
                            GL gl = drawable.getGL();
                            int i = gl.getSwapInterval();      
                            i = i==0 ? 1 : 0;
                            gl.setSwapInterval(i);
                            final GLAnimatorControl a = drawable.getAnimator();
                            if( null != a ) {
                                a.resetCounter();
                            }
                            System.err.println("Swap Interval: "+i);
                        }
                    });
                }                
            }
            else if(arg0.getKeyChar() == ' ') {      
                nextFontSet();
            }
        }
        public void keyTyped(KeyEvent arg0) {}
        public void keyReleased(KeyEvent arg0) {}
    }
}