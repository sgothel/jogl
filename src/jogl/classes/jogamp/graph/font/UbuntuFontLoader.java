/**
 * Copyright 2011-2023 JogAmp Community. All rights reserved.
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

import java.io.IOException;

import com.jogamp.common.net.Uri;
import com.jogamp.common.util.IntObjectHashMap;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontSet;

public class UbuntuFontLoader extends FontLoaderImpl implements FontSet {

    // FIXME: Add cache size to limit memory usage
    private static final IntObjectHashMap fontMap = new IntObjectHashMap();

    private static final Uri.Encoded jarName = Uri.Encoded.cast("jogl-fonts-p0.jar");

    private static final String absFontPath = "jogamp/graph/font/fonts/ubuntu/" ;

    private static final FontSet fontLoader = new UbuntuFontLoader();

    public static final FontSet get() {
        return fontLoader;
    }

    final static String availableFontFileNames[] =
    {
        /* 00 */ "Ubuntu-R.ttf",   // regular
        /* 01 */ "Ubuntu-RI.ttf",  // regular italic
        /* 02 */ "Ubuntu-B.ttf",   // bold
        /* 03 */ "Ubuntu-BI.ttf",  // bold italic
        /* 04 */ "Ubuntu-L.ttf",   // light
        /* 05 */ "Ubuntu-LI.ttf",  // light italic
        /* 06 */ "Ubuntu-M.ttf",   // medium
        /* 07 */ "Ubuntu-MI.ttf",  // medium italic

    };

    private UbuntuFontLoader() {
    }

    @Override
    public Font getDefault() throws IOException {
        return get(FAMILY_REGULAR, 0) ; // Sans Serif Regular
    }

    @Override
    public synchronized Font get(final int family, final int style) throws IOException {
        Font font = (Font)fontMap.get( ( family << 8 ) | style );
        if (font != null) {
            return font;
        }

        switch (family) {
            case FAMILY_MONOSPACED:
            case FAMILY_CONDENSED:
            case FAMILY_REGULAR:
                if( isOneSet(style, STYLE_BOLD) ) {
                    if( isOneSet(style, STYLE_ITALIC) ) {
                        font = readFont(availableFontFileNames[3], jarName, absFontPath);
                    } else {
                        font = readFont(availableFontFileNames[2], jarName, absFontPath);
                    }
                } else if( isOneSet(style, STYLE_ITALIC) ) {
                    font = readFont(availableFontFileNames[1], jarName, absFontPath);
                } else {
                    font = readFont(availableFontFileNames[0], jarName, absFontPath);
                }
                break;

            case FAMILY_LIGHT:
                if( isOneSet(style, STYLE_ITALIC) ) {
                    font = readFont(availableFontFileNames[5], jarName, absFontPath);
                } else {
                    font = readFont(availableFontFileNames[4], jarName, absFontPath);
                }
                break;

            case FAMILY_MEDIUM:
                if( isOneSet(style, STYLE_ITALIC) ) {
                    font = readFont(availableFontFileNames[6], jarName, absFontPath);
                } else {
                    font = readFont(availableFontFileNames[7], jarName, absFontPath);
                }
                break;
        }
        if( null != font ) {
            fontMap.put( ( family << 8 ) | style, font );
        }
        return font;
    }
}
