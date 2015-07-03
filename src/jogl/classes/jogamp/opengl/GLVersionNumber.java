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

import com.jogamp.common.util.VersionNumber;
import com.jogamp.common.util.VersionNumberString;

/**
 * A class for storing and comparing OpenGL version numbers.
 * This only works for desktop OpenGL at the moment.
 */
public class GLVersionNumber extends VersionNumberString {

    private final boolean valid;

    private GLVersionNumber(final int[] val, final int strEnd, final short state, final String versionString, final boolean valid) {
        super(val[0], val[1], val[2], strEnd, state, versionString);
        this.valid = valid;
    }

    private static java.util.regex.Pattern getUnderscorePattern() {
        if( null == _Pattern ) { // volatile dbl-checked-locking OK
            synchronized( VersionNumber.class ) {
                if( null == _Pattern ) {
                    _Pattern = getVersionNumberPattern("_");
                }
            }
        }
        return _Pattern;
    }
    private static volatile java.util.regex.Pattern _Pattern = null;

    public static final GLVersionNumber create(final String versionString) {
        final int[] val = new int[] { 0, 0, 0 };
        int strEnd = 0;
        short state = 0;
        boolean valid = false;
        if (versionString != null && versionString.length() > 0) {
            try {
                final java.util.regex.Pattern versionPattern;
                if (versionString.startsWith("GL_VERSION_")) {
                    versionPattern = getUnderscorePattern();
                } else {
                    versionPattern = VersionNumber.getDefaultVersionNumberPattern();
                }
                final VersionNumberString version = new VersionNumberString(versionString, versionPattern);
                strEnd = version.endOfStringMatch();
                val[0] = version.getMajor();
                val[1] = version.getMinor();
                state = (short) ( ( version.hasMajor() ? VersionNumber.HAS_MAJOR : (short)0 ) |
                                  ( version.hasMinor() ? VersionNumber.HAS_MINOR : (short)0 ) );
                valid = version.hasMajor() && version.hasMinor(); // Requires at least a defined major and minor version component!
            } catch (final Exception e) {
                e.printStackTrace();
                System.err.println("Info: ExtensionAvailabilityCache: FunctionAvailabilityCache.Version.<init>: " + e);
                val[0] = 1;
                val[1] = 0;
            }
        }
        return new GLVersionNumber(val, strEnd, state, versionString, valid);
    }

    public final boolean isValid() {
        return valid;
    }

    /**
     * Returns the optional vendor version at the end of the
     * <code>GL_VERSION</code> string if exists, otherwise the {@link VersionNumberString#zeroVersion zero version} instance.
     * <pre>
     *   2.1 Mesa 7.0.3-rc2 -> 7.0.3 (7.0.3-rc2)
     *   2.1 Mesa 7.12-devel (git-d6c318e) -> 7.12.0 (7.12-devel)
     *   4.2.12171 Compatibility Profile Context 9.01.8 -> 9.1.8 (9.01.8)
     *   4.2.12198 Compatibility Profile Context 12.102.3.0 -> 12.102.3 (12.102.3.0)
     *   4.3.0 NVIDIA 310.32 -> 310.32 (310.32)
     * </pre>
     */
    public static final VersionNumberString createVendorVersion(final String versionString) {
        if (versionString == null || versionString.length() <= 0) {
            return null;
        }

        // Skip the 1st GL version
        String str;
        {
            final GLVersionNumber glv = create(versionString);
            str = versionString.substring(glv.endOfStringMatch()).trim();
        }

        while ( str.length() > 0 ) {
            final VersionNumberString version = new VersionNumberString(str, getDefaultVersionNumberPattern());
            final int eosm = version.endOfStringMatch();
            if( 0 < eosm ) {
                if( version.hasMajor() && version.hasMinor() ) { // Requires at least a defined major and minor version component!
                    return version;
                }
                str = str.substring( eosm ).trim();
            } else {
                break; // no match
            }
        }
        return VersionNumberString.zeroVersion;
    }
}
