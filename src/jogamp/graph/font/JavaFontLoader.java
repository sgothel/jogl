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

public class JavaFontLoader {

	static String javaFontPath;
	static
	{
		javaFontPath = System.getProperty("java.home") + "/lib/fonts/";
	}

	public static final int MAX_BITMAP_FONT_SIZE    = 120;

	public static final int MONOSPACED              = 1;
	public static final int SERIF                   = 2;
	public static final int SANSERIF                = 3;
	public static final int CURSIVE                 = 4;
	public static final int FANTASY                 = 5;
	
	final static String availableJavaFontNames[] =
	{
		"Lucida Bright Regular",
		"Lucida Bright Italic",
		"Lucida Bright Demibold",
		"Lucida Bright Demibold Italic",
		"Lucida Sans Regular",
		"Lucida Sans Demibold",
		"Lucida Sans Typewriter Regular",
		"Lucida Sans Typewriter Bold",
	};
	static public String[] getAvailableNames()
	{
		return availableJavaFontNames;
	}

	final static String availableJavaFontFileNames[] =
	{
		"LucidaBrightRegular.ttf",
		"LucidaBrightItalic.ttf",
		"LucidaBrightDemiBold.ttf",
		"LucidaBrightDemiItalic.ttf",
		"LucidaSansRegular.ttf",
		"LucidaSansDemiBold.ttf",
		"LucidaTypewriterRegular.ttf",
		"LucidaTypewriterBold.ttf",
	};

	static public String get(int type)
	{
		String font = null;
		
		switch (type)
		{
		case MONOSPACED:
			font = getByName("Lucida Sans Typewriter Regular");
			break;
		case SERIF:
			font = getByName("Lucida Bright Regular");
			break;
		case SANSERIF:
			font = getByName("Lucida Sans Regular");
			break;
		case CURSIVE:
			font = getByName("Lucida Bright Regular");
			break;
		case FANTASY:
			font = getByName("Lucida Sans Regular");
			break;
		}

		return font;
	}
	
	static public String getByName(String name)
	{
		for (int i=0; i<availableJavaFontNames.length; i++)
		{
			if (name.equals(availableJavaFontNames[i]) == true)
			{
				return javaFontPath+availableJavaFontFileNames[i];
			}
		}
		return null;
	}	
}
