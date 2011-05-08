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

import jogamp.graph.curve.text.GlyphString;

import com.jogamp.graph.font.Font;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;

public class Label extends UIShape implements UITextShape {
    protected Font font;
    protected int size;
    protected String text;
    protected GlyphString glyphString; 
    
    public Label(Factory<? extends Vertex> factory, Font font, int size, String text) {
        super(factory);
        this.font = font;
        this.size = size;
        this.text = text;
    }
    
    public GlyphString getGlyphString() {
        return glyphString;
    }
    
    public String getText(){
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
        dirty |= DIRTY_SHAPE;
    }
    
    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
        dirty |= DIRTY_SHAPE;        
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
        dirty |= DIRTY_SHAPE;        
    }

    public String toString(){
        return "Label [" + font.toString() + ", size " + size + ", " + getText() + "]";
    }

    @Override
    protected void clearImpl() {
        if(null != glyphString) {
            glyphString.destroy(null, null);
        }        
    }
    
    @Override
    protected void createShape() {
        clearImpl();
        glyphString = GlyphString.createString(shape, getVertexFactory(), font, size, text);        
    }
}
