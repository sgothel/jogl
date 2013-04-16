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

package jogamp.opengl;

import java.util.StringTokenizer;

import javax.media.opengl.GLContext;

import com.jogamp.common.util.VersionNumberString;

/**
 * A class for storing and comparing OpenGL version numbers.
 * This only works for desktop OpenGL at the moment.
 */
class GLVersionNumber extends VersionNumberString {

    private final boolean valid;

    private GLVersionNumber(int[] val, String versionString, boolean valid) {
        super(val[0], val[1], val[2], versionString);
        this.valid = valid;
    }
    
    public static GLVersionNumber create(String versionString) {
        int[] val = new int[] { 0, 0, 0 };
        try {
            if (versionString.startsWith("GL_VERSION_")) {
                StringTokenizer tok = new StringTokenizer(versionString, "_");
                tok.nextToken(); // GL_
                tok.nextToken(); // VERSION_
                if (!tok.hasMoreTokens()) {
                    val[0] = 0;
                } else {
                    val[0] = Integer.valueOf(tok.nextToken()).intValue();
                    if (!tok.hasMoreTokens()) {
                        val[1] = 0;
                    } else {
                        val[1] = Integer.valueOf(tok.nextToken()).intValue();
                        if (!tok.hasMoreTokens()) {
                            val[2] = 0;
                        } else {
                            val[2] = Integer.valueOf(tok.nextToken()).intValue();
                        }
                    }
                }
            } else {
                int radix = 10;
                if (versionString.length() > 2) {
                    if (Character.isDigit(versionString.charAt(0)) && versionString.charAt(1) == '.' && Character.isDigit(versionString.charAt(2))) {
                        val[0] = Character.digit(versionString.charAt(0), radix);
                        val[1] = Character.digit(versionString.charAt(2), radix);
                        // See if there's version-specific information which might
                        // imply a more recent OpenGL version
                        StringTokenizer tok = new StringTokenizer(versionString, " ");
                        if (tok.hasMoreTokens()) {
                            tok.nextToken();
                            if (tok.hasMoreTokens()) {
                                String token = tok.nextToken();
                                int i = 0;
                                while (i < token.length() && !Character.isDigit(token.charAt(i))) {
                                    i++;
                                }
                                if (i < token.length() - 2 && Character.isDigit(token.charAt(i)) && token.charAt(i + 1) == '.' && Character.isDigit(token.charAt(i + 2))) {
                                    int altMajor = Character.digit(token.charAt(i), radix);
                                    int altMinor = Character.digit(token.charAt(i + 2), radix);
                                    // Avoid possibly confusing situations by putting some
                                    // constraints on the upgrades we do to the major and
                                    // minor versions
                                    if ( (altMajor == val[0] && altMinor > val[1]) || altMajor == val[0] + 1 ) {
                                        if( GLContext.isValidGLVersion(altMajor, altMinor) ) {
                                            val[0] = altMajor;
                                            val[1] = altMinor;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return new GLVersionNumber(val, versionString, true);
        } catch (Exception e) {
            e.printStackTrace();
            // FIXME: refactor desktop OpenGL dependencies and make this
            // class work properly for OpenGL ES
            System.err.println("Info: ExtensionAvailabilityCache: FunctionAvailabilityCache.Version.<init>: " + e);
            val[0] = 1;
            val[1] = 0;
            /*
            throw (IllegalArgumentException)
            new IllegalArgumentException(
            "Illegally formatted version identifier: \"" + versionString + "\"")
            .initCause(e);
             */            
        }
        return new GLVersionNumber(val, versionString, false);
    }

    public final boolean isValid() {
        return valid;
    }
    
    /** 
     * Returns the optional vendor version at the end of the 
     * <code>GL_VERSION</code> string if exists, otherwise <code>null</code>.
     * <pre>
     *   2.1 Mesa 7.0.3-rc2 -> 7.0.3 (7.0.3-rc2)
     *   4.2.12171 Compatibility Profile Context 9.01.8 -> 9.1.8 (9.01.8)
     *   4.3.0 NVIDIA 310.32 -> 310.32 (310.32)
     * </pre> 
     */
    public static final VersionNumberString createVendorVersion(String versionString) {
        if (versionString == null || versionString.length() <= 0) {
            return null;
        }    
        final String[] strings = versionString.trim().split("\\s+");
        if ( strings.length <= 0 ) {
            return null;
        }
        // Test all segments backwards from [len-1..1], skipping the 1st entry (GL version)
        // If a segment represents a valid VersionNumber - use it.
        for(int i=strings.length-1; i>=1; i--) {
            final String s = strings[i];
            final VersionNumberString version = new VersionNumberString(s, ".");
            if( !version.isZero() ) {
                return version;
            }
        }
        return null;
    }
}
