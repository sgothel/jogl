package com.jogamp.opengl.test.junit.graph;

import java.io.IOException;

import com.jogamp.common.util.IOUtil;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.font.FontSet;

public class FontSet01 {
    public static Font[] getSet01() throws IOException {
        final Font[] fonts = new Font[11];
        int i = 0;
        fonts[i++] = FontFactory.get(FontFactory.UBUNTU).getDefault(); // FontSet.FAMILY_REGULAR, FontSet.STYLE_NONE
        fonts[i++] = FontFactory.get(FontFactory.UBUNTU).get(FontSet.FAMILY_REGULAR, FontSet.STYLE_ITALIC);
        fonts[i++] = FontFactory.get(FontFactory.UBUNTU).get(FontSet.FAMILY_REGULAR, FontSet.STYLE_BOLD);
        fonts[i++] = FontFactory.get(IOUtil.getResource(TestTextRendererNEWTBugXXXX.class,
                "fonts/freefont/FreeMono.ttf").getInputStream(), true);
        fonts[i++] = FontFactory.get(IOUtil.getResource(TestTextRendererNEWTBugXXXX.class,
                "fonts/freefont/FreeMonoBold.ttf").getInputStream(), true);
        fonts[i++] = FontFactory.get(IOUtil.getResource(TestTextRendererNEWTBugXXXX.class,
                "fonts/freefont/FreeSans.ttf").getInputStream(), true);
        fonts[i++] = FontFactory.get(IOUtil.getResource(TestTextRendererNEWTBugXXXX.class,
                "fonts/freefont/FreeSansBold.ttf").getInputStream(), true);
        fonts[i++] = FontFactory.get(IOUtil.getResource(TestTextRendererNEWTBugXXXX.class,
                "fonts/freefont/FreeSerif.ttf").getInputStream(), true);
        fonts[i++] = FontFactory.get(IOUtil.getResource(TestTextRendererNEWTBugXXXX.class,
                "fonts/freefont/FreeSerifBold.ttf").getInputStream(), true);
        fonts[i++] = FontFactory.get(IOUtil.getResource(TestTextRendererNEWTBugXXXX.class,
                "fonts/freefont/FreeSerifBoldItalic.ttf").getInputStream(), true);
        fonts[i++] = FontFactory.get(IOUtil.getResource(TestTextRendererNEWTBugXXXX.class,
                "fonts/freefont/FreeSerifItalic.ttf").getInputStream(), true);
        return fonts;
    }
}
