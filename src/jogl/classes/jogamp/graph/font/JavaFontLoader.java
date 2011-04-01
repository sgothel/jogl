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
package jogamp.graph.font;

import java.io.File;
import java.io.IOException;

import javax.media.opengl.GLException;

import com.jogamp.common.util.IntObjectHashMap;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontSet;
import com.jogamp.graph.font.FontFactory;

public class JavaFontLoader implements FontSet {
    
    final static FontSet fontLoader = new JavaFontLoader();

    public static FontSet get() {
        return fontLoader;
    }
    
    final static String availableFontFileNames[] =
    {
        /* 00 */ "LucidaBrightRegular.ttf",
        /* 01 */ "LucidaBrightItalic.ttf",
        /* 02 */ "LucidaBrightDemiBold.ttf",
        /* 03 */ "LucidaBrightDemiItalic.ttf",
        /* 04 */ "LucidaSansRegular.ttf",
        /* 05 */ "LucidaSansDemiBold.ttf",
        /* 06 */ "LucidaTypewriterRegular.ttf",
        /* 07 */ "LucidaTypewriterBold.ttf",
    };
        
    final String javaFontPath;
    
    private JavaFontLoader() {
        javaFontPath = System.getProperty("java.home") + "/lib/fonts/";
    }

    // FIXME: Add cache size to limit memory usage 
    static final IntObjectHashMap fontMap = new IntObjectHashMap();
    
    static boolean is(int bits, int bit) {
        return 0 != ( bits & bit ) ;
    }
    
    public Font getDefault() {
        return get(FAMILY_REGULAR, 0) ; // Sans Serif Regular 
    }
    
	public Font get(int family, int style)	{
        Font font = (Font)fontMap.get( ( family << 8 ) | style );
        if (font != null) {
            return font;
        }

        // 1st process Sans Serif (2 fonts)
        if( is(style, STYLE_SERIF) ) {                
            if( is(style, STYLE_BOLD) ) {                
                font = abspath(availableFontFileNames[5], family, style);
            } else {
                font = abspath(availableFontFileNames[4], family, style);
            }
            fontMap.put( ( family << 8 ) | style, font );
            return font;
        }
        
        // Serif Fonts ..
        switch (family) {
            case FAMILY_LIGHT:
            case FAMILY_MEDIUM:
            case FAMILY_CONDENSED:
            case FAMILY_REGULAR:
                if( is(style, STYLE_BOLD) ) {                
                    if( is(style, STYLE_ITALIC) ) {                
                        font = abspath(availableFontFileNames[3], family, style);
                    } else {
                        font = abspath(availableFontFileNames[2], family, style);
                    }
                } else if( is(style, STYLE_ITALIC) ) {                
                    font = abspath(availableFontFileNames[1], family, style);
                } else {
                    font = abspath(availableFontFileNames[0], family, style);
                }
                break;
                
            case FAMILY_MONOSPACED:
                if( is(style, STYLE_BOLD) ) {                
                    font = abspath(availableFontFileNames[7], family, style);
                } else {
                    font = abspath(availableFontFileNames[6], family, style);
                }
                break;                
        }

        return font;
    }
	
    Font abspath(String fname, int family, int style) {
        final String err = "Problem loading font "+fname+", file "+javaFontPath+fname ;
                
        try {
            final Font f = FontFactory.get( new File(javaFontPath+fname) );
            if(null != f) {
                fontMap.put( ( family << 8 ) | style, f );
                return f;
            }
            throw new GLException(err);            
        } catch (IOException ioe) {
            throw new GLException(err, ioe);            
        }
    }    
}
