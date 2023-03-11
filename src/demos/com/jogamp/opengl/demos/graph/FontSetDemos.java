package com.jogamp.opengl.demos.graph;

import java.io.IOException;

import com.jogamp.common.util.IOUtil;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.font.FontSet;

public class FontSetDemos {
    public static Font[] getSet01() throws IOException {
        final Font[] fonts = new Font[11];
        int i = 0;
        fonts[i++] = FontFactory.get(FontFactory.UBUNTU).getDefault(); // FontSet.FAMILY_REGULAR, FontSet.STYLE_NONE
        fonts[i++] = FontFactory.get(FontFactory.UBUNTU).get(FontSet.FAMILY_LIGHT, FontSet.STYLE_NONE);
        fonts[i++] = FontFactory.get(FontFactory.UBUNTU).get(FontSet.FAMILY_LIGHT, FontSet.STYLE_ITALIC);
        fonts[i++] = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeMono.ttf",
                FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);
        fonts[i++] = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeMonoBold.ttf",
                FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);
        fonts[i++] = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeSans.ttf",
                FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);
        fonts[i++] = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeSansBold.ttf",
                FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);
        fonts[i++] = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeSerif.ttf",
                FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);
        fonts[i++] = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeSerifBold.ttf",
                FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);
        fonts[i++] = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeSerifBoldItalic.ttf",
                FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);
        fonts[i++] = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeSerifItalic.ttf",
                FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);
        return fonts;
    }
}
