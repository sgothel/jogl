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
package jogamp.newt.driver;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;

import jogamp.newt.Debug;

import com.jogamp.common.util.IOUtil;
import com.jogamp.common.util.ReflectionUtil;

public class PNGIcon {
    private static final String err0 = "PNG decoder not implemented.";

    private static final boolean avail;

    static {
        Debug.initSingleton();

        final ClassLoader cl = PNGIcon.class.getClassLoader();
        avail = ReflectionUtil.isClassAvailable("jogamp.newt.driver.opengl.JoglUtilPNGIcon", cl) &&
                ReflectionUtil.isClassAvailable("com.jogamp.opengl.util.texture.spi.PNGImage", cl);
    }

    /** Returns true if PNG decoder is available. */
    public static boolean isAvailable() {
        return avail;
    }

    /**
     * Implemented for X11.
     * @param resources
     * @param data_size
     * @param elem_bytesize
     *
     * @return BGRA8888 bytes with origin at upper-left corner where component B is located on the lowest 8-bit and component A is located on the highest 8-bit.
     *
     * @throws UnsupportedOperationException if not implemented
     * @throws InterruptedException
     * @throws IOException
     * @throws MalformedURLException
     */
    public static ByteBuffer arrayToX11BGRAImages(IOUtil.ClassResources resources, int[] data_size, int[] elem_bytesize) throws UnsupportedOperationException, InterruptedException, IOException, MalformedURLException {
        if( avail ) {
            return jogamp.newt.driver.opengl.JoglUtilPNGIcon.arrayToX11BGRAImages(resources, data_size, elem_bytesize);
        }
        throw new UnsupportedOperationException(err0);
    }

    /**
     * Implemented for Windows.
     * @param resources
     * @param toBGRA if true, arranges stores in BGRA888 order, otherwise RGBA888
     * @param width
     * @param height
     * @param data_size
     * @param elem_bytesize
     * @param resourcesIdx
     * @return pixels with origin at upper-left corner.
     *         If storing RGBA8888, component R is located on the lowest 8-bit.
     *         If storing BGRA8888, component B is located on the lowest 8-bit.
     *         Component A is located on the highest 8-bit.
     *
     * @throws UnsupportedOperationException if not implemented
     * @throws InterruptedException
     * @throws IOException
     * @throws MalformedURLException
     */
    public static ByteBuffer singleToRGBAImage(IOUtil.ClassResources resources, int resourceIdx, boolean toBGRA, int[] width, int[] height, int[] data_size, int[] elem_bytesize) throws UnsupportedOperationException, InterruptedException, IOException, MalformedURLException {
        if( avail ) {
            return jogamp.newt.driver.opengl.JoglUtilPNGIcon.singleToRGBAImage(resources, resourceIdx, toBGRA, width, height, data_size, elem_bytesize);
        }
        throw new UnsupportedOperationException(err0);
    }
}
