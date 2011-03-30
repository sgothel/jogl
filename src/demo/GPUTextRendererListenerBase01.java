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

import java.io.IOException;
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLException;
import com.jogamp.graph.curve.opengl.TextRenderer;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.opengl.GLWindow;

/**
 *
 * GPURendererListenerBase01 Keys:
 * - 1/2: zoom in/out
 * - 6/7: 2nd pass texture size
 * - 0/9: rotate
 * - v: toggle v-sync
 * 
 * Additional Keys:
 * - 3/4: font +/-
 * - s: toogle draw 'font set'
 * - f: toggle draw fps
 * - space: toggle font (ubuntu/java)
 */
public abstract class GPUTextRendererListenerBase01 extends GPURendererListenerBase01 {
    int fontSet = FontFactory.UBUNTU;
    Font font;
    
    boolean drawFontSet = true;
    boolean drawFPS = true;
    boolean updateFont = true;
    int fontSize = 40;
    final int fontSizeModulo = 100;    
    
    static final String text1 = "abcdefghijklmnopqrstuvwxyz\nABCDEFGHIJKLMNOPQRSTUVWXYZ\n0123456789.:,;(*!?/\\\")$%^&-+@~#<>{}[]";
    static final String text2 = "The quick brown fox jumps over the lazy dog";

    public GPUTextRendererListenerBase01(Vertex.Factory<? extends Vertex> factory, int mode, boolean debug, boolean trace) {
        super(TextRenderer.create(factory, mode), debug, trace);        
        this.font = FontFactory.get(fontSet).getDefault();
    }

    public void display(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        
        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f); // Demo02 needs to have this set here as well .. hmm ?
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        final TextRenderer textRenderer = (TextRenderer) getRenderer();
        
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
                textRenderer.resetModelview(null);
                textRenderer.translate(gl, 0, 0, -6000);
                textRenderer.renderString3D(gl, font, fpsS.substring(0, fpsSp+2), getPosition(), fontSize, getTexSize());
            }
            if(drawFontSet) { 
                textRenderer.resetModelview(null);
                textRenderer.translate(gl, 0, height-50, -6000);
                textRenderer.renderString3D(gl, font, text1, getPosition(), fontSize, getTexSize());
            }
            if(_drawFPS || drawFontSet) {
                textRenderer.reshapePerspective(null, 45.0f, width, height, 0.1f, 7000.0f);             
            }
            		
            textRenderer.resetModelview(null);            
            textRenderer.translate(null, getXTran(), getYTran(), getZoom());
            textRenderer.rotate(gl, getAngle(), 0, 1, 0);
            updateMatrix = false;
        }

        textRenderer.renderString3D(gl, font, text2, getPosition(), fontSize, getTexSize());
    }        
        
    public void fontIncr(int v) {
        fontSize = Math.abs((fontSize + v) % fontSizeModulo) ;
        updateFont = true;
        dumpMatrix(true);
    }

    public void nextFontSet() {
        fontSet = ( fontSet == FontFactory.UBUNTU ) ? FontFactory.JAVA : FontFactory.UBUNTU ;
        font = FontFactory.get(fontSet).getDefault();        
    }
    
    public void setFontSet(int set, int family, int stylebits) {
        fontSet = set;
        font = FontFactory.get(fontSet).get(family, stylebits);        
    }
    
    void dumpMatrix(boolean bbox) {
        System.err.println("Matrix: " + getXTran() + "/" + getYTran() + " x"+getZoom() + " @"+getAngle() +" fontSize "+fontSize);
        if(bbox) {
            System.err.println("bbox: "+font.getStringBounds(text2, fontSize));
        }
    }
    
    KeyAction keyAction = null;
    
    @Override
    public void attachInputListenerTo(GLWindow window) {
        if ( null == keyAction ) {
            keyAction = new KeyAction();
            window.addKeyListener(keyAction);
            super.attachInputListenerTo(window);            
        }
                
    }

    @Override
    public void detachFrom(GLWindow window) {
        super.detachFrom(window);
        if ( null == keyAction ) {
            return;
        }
        window.removeKeyListener(keyAction);
    }
    
    public void printScreen(GLAutoDrawable drawable, String dir, String tech, boolean exportAlpha) throws GLException, IOException {
        printScreen(drawable, dir, tech, font.getName(), exportAlpha);
    }
    
    public class KeyAction implements KeyListener {
        public void keyPressed(KeyEvent arg0) {
            if(arg0.getKeyCode() == KeyEvent.VK_3){
                fontIncr(10);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_4){
                fontIncr(-10);
            }
            else if(arg0.getKeyChar() == 's') {
                drawFontSet = !drawFontSet; 
                System.err.println("Draw font set: "+drawFontSet);
            }  
            else if(arg0.getKeyChar() == 'f'){
                drawFPS = !drawFPS; 
                System.err.println("Draw FPS: "+drawFPS);
            }  
            else if(arg0.getKeyChar() == ' ') {      
                nextFontSet();
            }
        }
        public void keyTyped(KeyEvent arg0) {}
        public void keyReleased(KeyEvent arg0) {}
    }
}