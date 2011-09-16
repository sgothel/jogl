/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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
package com.jogamp.graph.font;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.AccessController;

import com.jogamp.common.util.ReflectionUtil;

import jogamp.graph.font.FontConstructor;
import jogamp.graph.font.JavaFontLoader;
import jogamp.graph.font.UbuntuFontLoader;
import jogamp.opengl.Debug;

public class FontFactory {
    /** Ubuntu is the default font family */
    public static final int UBUNTU = 0;
    
    /** Java fonts are optional */
    public static final int JAVA = 1;
    
    private static final FontConstructor fontConstr;

    static {
        /**
         * For example:
         *   "jogamp.graph.font.typecast.TypecastFontFactory" (default)
         *   "jogamp.graph.font.ttf.TTFFontImpl"
         */
        String fontImplName = Debug.getProperty("FontImpl", true, AccessController.getContext());
        if(null == fontImplName) {
            fontImplName = "jogamp.graph.font.typecast.TypecastFontConstructor";
        }
        fontConstr = (FontConstructor) ReflectionUtil.createInstance(fontImplName, FontFactory.class.getClassLoader());
    }
    
    public static final FontSet getDefault() {
        return get(UBUNTU);
    }
    
    public static final FontSet get(int font) {
        switch (font) {
            case JAVA:
                return JavaFontLoader.get();
            default:
                return UbuntuFontLoader.get();
        }
    }
    
    public static final Font get(File file) throws IOException {
        return fontConstr.create(file);
    }

    public static final Font get(final URL url) throws IOException {
        return fontConstr.create(url);
    }    
    
    public static boolean isPrintableChar( char c ) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of( c );
        return (!Character.isISOControl(c)) &&
                c != 0 &&
                block != null &&
                block != Character.UnicodeBlock.SPECIALS;
    }    
}
