package com.jogamp.opengl.test.junit.graph;

import java.io.IOException;

import com.jogamp.common.util.IOUtil;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.font.FontSet;

public class FontSet01 {
    public static Font[] getSet00() throws IOException {
        final Font[] fonts = new Font[7];
        int i = 0;
        fonts[i++] = FontFactory.get(FontFactory.UBUNTU).getDefault(); // FontSet.FAMILY_REGULAR, FontSet.STYLE_NONE
        fonts[i++] = FontFactory.get(FontFactory.UBUNTU).get(FontSet.FAMILY_LIGHT, FontSet.STYLE_NONE);
        fonts[i++] = FontFactory.get(FontFactory.UBUNTU).get(FontSet.FAMILY_LIGHT, FontSet.STYLE_ITALIC);
        fonts[i++] = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeMono.ttf",
                FontSet01.class.getClassLoader(), FontSet01.class).getInputStream(), true);
        fonts[i++] = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeSans.ttf",
                FontSet01.class.getClassLoader(), FontSet01.class).getInputStream(), true);
        fonts[i++] = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeSerif.ttf",
                FontSet01.class.getClassLoader(), FontSet01.class).getInputStream(), true);
        fonts[i++] = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeSerifItalic.ttf",
                FontSet01.class.getClassLoader(), FontSet01.class).getInputStream(), true);
        return fonts;
    }
    public static Font[] getSet01() throws IOException {
        final Font[] fonts = new Font[11];
        int i = 0;
        fonts[i++] = FontFactory.get(FontFactory.UBUNTU).getDefault(); // FontSet.FAMILY_REGULAR, FontSet.STYLE_NONE
        fonts[i++] = FontFactory.get(FontFactory.UBUNTU).get(FontSet.FAMILY_LIGHT, FontSet.STYLE_NONE);
        fonts[i++] = FontFactory.get(FontFactory.UBUNTU).get(FontSet.FAMILY_LIGHT, FontSet.STYLE_ITALIC);
        fonts[i++] = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeMono.ttf",
                FontSet01.class.getClassLoader(), FontSet01.class).getInputStream(), true);
        fonts[i++] = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeMonoBold.ttf",
                FontSet01.class.getClassLoader(), FontSet01.class).getInputStream(), true);
        fonts[i++] = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeSans.ttf",
                FontSet01.class.getClassLoader(), FontSet01.class).getInputStream(), true);
        fonts[i++] = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeSansBold.ttf",
                FontSet01.class.getClassLoader(), FontSet01.class).getInputStream(), true);
        fonts[i++] = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeSerif.ttf",
                FontSet01.class.getClassLoader(), FontSet01.class).getInputStream(), true);
        fonts[i++] = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeSerifBold.ttf",
                FontSet01.class.getClassLoader(), FontSet01.class).getInputStream(), true);
        fonts[i++] = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeSerifBoldItalic.ttf",
                FontSet01.class.getClassLoader(), FontSet01.class).getInputStream(), true);
        fonts[i++] = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeSerifItalic.ttf",
                FontSet01.class.getClassLoader(), FontSet01.class).getInputStream(), true);
        return fonts;
    }
}
