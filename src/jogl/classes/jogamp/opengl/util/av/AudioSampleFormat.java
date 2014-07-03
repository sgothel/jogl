/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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
package jogamp.opengl.util.av;

/** FFMPEG/libAV compatible audio sample formats */
public enum AudioSampleFormat {
    // NONE = -1,
    U8,          ///< unsigned 8 bits
    S16,         ///< signed 16 bits
    S32,         ///< signed 32 bits
    FLT,         ///< float
    DBL,         ///< double

    U8P,         ///< unsigned 8 bits, planar
    S16P,        ///< signed 16 bits, planar
    S32P,        ///< signed 32 bits, planar
    FLTP,        ///< float, planar
    DBLP,        ///< double, planar

    COUNT;       ///< Number of sample formats.

    /**
     * Returns the matching SampleFormat value corresponding to the given SampleFormat's integer ordinal.
     * <pre>
     *   given:
     *     ordinal = enumValue.ordinal()
     *   reverse:
     *     enumValue = EnumClass.values()[ordinal]
     * </pre>
     * @throws IllegalArgumentException if the given ordinal is out of range, i.e. not within [ 0 .. SampleFormat.values().length-1 ]
     */
    public static AudioSampleFormat valueOf(final int ordinal) throws IllegalArgumentException {
        final AudioSampleFormat[] all = AudioSampleFormat.values();
        if( 0 <= ordinal && ordinal < all.length ) {
            return all[ordinal];
        }
        throw new IllegalArgumentException("Ordinal "+ordinal+" out of range of SampleFormat.values()[0.."+(all.length-1)+"]");
    }
}