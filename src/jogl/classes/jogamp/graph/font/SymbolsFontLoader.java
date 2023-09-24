/**
 * Copyright 2023 JogAmp Community. All rights reserved.
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
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontSet;

public class SymbolsFontLoader extends FontLoaderImpl implements FontSet {
    private static final Uri.Encoded jarName = Uri.Encoded.cast("jogl-fonts-p0.jar");

    private static final String absFontPath = "jogamp/graph/font/fonts/symbols/" ;

    private static final FontSet fontLoader = new SymbolsFontLoader();

    private static Font cachedFont = null;

    public static final FontSet get() {
        return fontLoader;
    }

    private SymbolsFontLoader() {
    }

    @Override
    public Font getDefault() throws IOException {
        return get(FAMILY_REGULAR, 0) ; // Sans Serif Regular
    }

    @Override
    public synchronized Font get(final int family, final int style) throws IOException {
        Font font = cachedFont;
        if (font != null) {
            return font;
        }
        font = readFont("MaterialIconsRound-Regular.ttf", jarName, absFontPath);
        if( null != font ) {
            cachedFont = font;
        }
        return font;
    }
}
