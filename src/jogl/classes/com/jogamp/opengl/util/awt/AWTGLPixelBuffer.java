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
package com.jogamp.opengl.util.awt;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.Iterator;

import com.jogamp.nativewindow.util.PixelFormat;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLProfile;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.util.IntObjectHashMap;
import com.jogamp.opengl.util.GLPixelBuffer;

/**
 * AWT {@link GLPixelBuffer} backed by an {@link BufferedImage} of type
 * {@link BufferedImage#TYPE_INT_ARGB} or {@link BufferedImage#TYPE_INT_RGB}.
 * <p>
 * Implementation uses an array backed  {@link IntBuffer}.
 * </p>
 * <p>
 * {@link AWTGLPixelBuffer} can be produced via {@link AWTGLPixelBufferProvider}'s
 * {@link AWTGLPixelBufferProvider#allocate(GL, PixelFormat.Composition, GLPixelAttributes, boolean, int, int, int, int) allocate(..)}.
 * </p>
 * <p>
 * See {@link AWTGLPixelBuffer#requiresNewBuffer(GL, int, int, int)} for {@link #allowRowStride} details.
 * </p>
 * <p>
 * If using <code>allowRowStride == true</code>, user may needs to get the {@link #getAlignedImage(int, int) aligned image}
 * since {@link #requiresNewBuffer(GL, int, int, int)} will allow different width in this case.
 * </p>
 */
public class AWTGLPixelBuffer extends GLPixelBuffer {
    /**
     * Ignoring componentCount, since otherwise no AWT/GL matching types are found.
     * <p>
     * Due to using RGBA and BGRA, pack/unpack usage has makes no difference.
     * </p>
     */
    private static final GLPixelAttributes awtPixelAttributesIntBGRA = new GLPixelAttributes(GL.GL_BGRA, GL.GL_UNSIGNED_BYTE);
    private static final GLPixelAttributes awtPixelAttributesIntRGBA = new GLPixelAttributes(GL.GL_RGBA, GL.GL_UNSIGNED_BYTE);

    /** The underlying {@link BufferedImage}. */
    public final BufferedImage image;

    private final PixelFormat.Composition hostPixelComp;
    private final int awtFormat;

    /**
     * @param hostPixelComp the host {@link PixelFormat.Composition}
     * @param pixelAttributes the desired {@link GLPixelAttributes}
     * @param pack {@code true} for read mode GPU -> CPU, e.g. {@link GL#glReadPixels(int, int, int, int, int, int, Buffer) glReadPixels}.
     *             {@code false} for write mode CPU -> GPU, e.g. {@link GL#glTexImage2D(int, int, int, int, int, int, int, int, Buffer) glTexImage2D}.
     * @param awtFormat the used AWT format, i.e. {@link AWTGLPixelBufferProvider#getAWTFormat(GLProfile, int)}
     * @param width in pixels
     * @param height in pixels
     * @param depth in pixels
     * @param image the AWT image
     * @param buffer the backing array
     * @param allowRowStride If <code>true</code>, allow row-stride, otherwise not. See {@link #requiresNewBuffer(GL, int, int, int)}.
     *                       If <code>true</code>, user shall decide whether to use a {@link #getAlignedImage(int, int) width-aligned image}.
     */
    public AWTGLPixelBuffer(final PixelFormat.Composition hostPixelComp,
                            final GLPixelAttributes pixelAttributes,
                            final boolean pack,
                            final int awtFormat, final int width, final int height, final int depth,
                            final BufferedImage image, final Buffer buffer, final boolean allowRowStride) {
        super(pixelAttributes, pack, width, height, depth, buffer, allowRowStride);
        this.image = image;
        this.hostPixelComp = hostPixelComp;
        this.awtFormat = awtFormat;
    }

    public final PixelFormat.Composition getHostPixelComp() { return hostPixelComp; }
    public final int getAWTFormat() { return awtFormat; }

    @Override
    public void dispose() {
        image.flush();
        super.dispose();
    }

    /**
     * Returns a width- and height-aligned image representation sharing data w/ {@link #image}.
     * @param width
     * @param height
     * @return
     * @throws IllegalArgumentException if requested size exceeds image size
     */
    public BufferedImage getAlignedImage(final int width, final int height) throws IllegalArgumentException {
        if( width * height > image.getWidth() * image.getHeight() ) {
            throw new IllegalArgumentException("Requested size exceeds image size: "+width+"x"+height+" > "+image.getWidth()+"x"+image.getHeight());
        }
        if( width == image.getWidth() && height == image.getHeight() ) {
            return image;
        } else {
            final ColorModel cm = image.getColorModel();
            final WritableRaster raster0 = image.getRaster();
            final DataBuffer dataBuffer = raster0.getDataBuffer();
            final SinglePixelPackedSampleModel sppsm0 = (SinglePixelPackedSampleModel) raster0.getSampleModel();
            final SinglePixelPackedSampleModel sppsm1 = new SinglePixelPackedSampleModel(dataBuffer.getDataType(),
                        width, height, width /* scanLineStride */, sppsm0.getBitMasks());
            final WritableRaster raster1 = Raster.createWritableRaster(sppsm1, dataBuffer, null);
            return new BufferedImage (cm, raster1, cm.isAlphaPremultiplied(), null);
        }
    }

    public final boolean isDataBufferSource(final BufferedImage imageU) {
        final DataBuffer dataBuffer0 = image.getRaster().getDataBuffer();
        final DataBuffer dataBufferU = imageU.getRaster().getDataBuffer();
        return dataBufferU == dataBuffer0;
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        sb = super.toString(sb);
        sb.append(", allowRowStride ").append(allowRowStride).append(", image [").append(image.getWidth()).append("x").append(image.getHeight()).append(", ").append(image.toString()).append("]");
        return sb;
    }
    @Override
    public String toString() {
        return "AWTGLPixelBuffer["+toString(null).toString()+"]";
    }

    /**
     * Provider for {@link AWTGLPixelBuffer} instances.
     */
    public static class AWTGLPixelBufferProvider implements GLPixelBufferProvider {
        private final boolean allowRowStride;

        /**
         * @param allowRowStride If <code>true</code>, allow row-stride, otherwise not.
         * See {@link #getAllowRowStride()} and {@link AWTGLPixelBuffer#requiresNewBuffer(GL, int, int, int)}.
         * If <code>true</code>, user shall decide whether to use a {@link AWTGLPixelBuffer#getAlignedImage(int, int) width-aligned image}.
         */
        public AWTGLPixelBufferProvider(final boolean allowRowStride) {
            this.allowRowStride = allowRowStride;
        }

        @Override
        public boolean getAllowRowStride() { return allowRowStride; }

        @Override
        public GLPixelAttributes getAttributes(final GL gl, final int componentCount, final boolean pack) {
            return gl.isGLES() ? awtPixelAttributesIntRGBA : awtPixelAttributesIntBGRA;
        }

        public GLPixelAttributes getAttributes(final GLProfile glp, final int componentCount) {
            return glp.isGLES() ? awtPixelAttributesIntRGBA : awtPixelAttributesIntBGRA;
        }

        /**
         * {@inheritDoc}
         * <p>
         * Returns a valid {@link PixelFormat.Composition} instance from {@link #getAWTPixelFormat(GLProfile, int)}.
         * </p>
         */
        @Override
        public PixelFormat.Composition getHostPixelComp(final GLProfile glp, final int componentCount) {
            return getAWTPixelFormat(glp, componentCount).comp;
        }

        /**
         * Returns one of
         * <ul>
         *   <li>GL__, 4c -> 4c: {@link BufferedImage#TYPE_INT_ARGB} <-> {@link GL#GL_BGRA}</li>
         *   <li>GLES, 4c -> 4c: {@link BufferedImage#TYPE_INT_BGR}  <-> {@link GL#GL_RGBA}</li>
         *   <li>GL__, 3c -> 4c: {@link BufferedImage#TYPE_INT_RGB}  <-> {@link GL#GL_BGRA}</li>
         *   <li>GLES, 3c -> 4c: {@link BufferedImage#TYPE_INT_BGR}  <-> {@link GL#GL_RGBA}</li>
         * </ul>
         * @param glp
         * @param componentCount
         * @return
         */
        public int getAWTFormat(final GLProfile glp, final int componentCount) {
            if( 4 == componentCount ) {
                // FIXME: 4 component solution BufferedImage.TYPE_INT_ARGB: GLES format missing (i.e. GL_BGRA)
                return glp.isGLES() ? BufferedImage.TYPE_INT_BGR : BufferedImage.TYPE_INT_ARGB;
            } else {
                return glp.isGLES() ? BufferedImage.TYPE_INT_BGR : BufferedImage.TYPE_INT_RGB;
            }
        }

        public PixelFormat getAWTPixelFormat(final GLProfile glp, final int componentCount) {
            if( 4 == componentCount ) {
                return glp.isGLES() ? PixelFormat.RGBx8888 : PixelFormat.BGRA8888;
            } else {
                return glp.isGLES() ? PixelFormat.RGBx8888 : PixelFormat.BGRx8888;
            }
        }

        /**
         * {@inheritDoc}
         * <p>
         * Returns an array backed {@link IntBuffer} of size <pre>width*height*{@link Buffers#SIZEOF_INT SIZEOF_INT}</code>.
         * </p>
         */
        @Override
        public AWTGLPixelBuffer allocate(final GL gl, final PixelFormat.Composition hostPixComp, final GLPixelAttributes pixelAttributes, final boolean pack,
                                         final int width, final int height, final int depth, final int minByteSize) {
            if( null == hostPixComp ) {
                throw new IllegalArgumentException("Null hostPixComp");
            }
            final int awtFormat = getAWTFormat(gl.getGLProfile(), hostPixComp.componentCount());
            final BufferedImage image = new BufferedImage(width, height, awtFormat);
            final int[] readBackIntBuffer = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
            final Buffer ibuffer = IntBuffer.wrap( readBackIntBuffer );
            return new AWTGLPixelBuffer(hostPixComp, pixelAttributes, pack,
                                        awtFormat, width, height, depth, image, ibuffer, allowRowStride);
        }
    }

    /**
     * Provider for singleton {@link AWTGLPixelBuffer} instances.
     * <p>
     * Provider instance holds the last {@link AWTGLPixelBuffer} instance
     * {@link #allocate(GL, PixelFormat.Composition, GLPixelAttributes, boolean, int, int, int, int) allocated}.
     * A new {@link #allocate(GL, PixelFormat.Composition, GLPixelAttributes, boolean, int, int, int, int) allocation}
     * will return same instance, if a new buffer is not {@link AWTGLPixelBuffer#requiresNewBuffer(GL, int, int, int) required}.
     * The latter is true if size are compatible, hence <code>allowRowStride</code> should be enabled, if possible.
     * </p>
     */
    public static class SingleAWTGLPixelBufferProvider extends AWTGLPixelBufferProvider implements SingletonGLPixelBufferProvider {
        private final IntObjectHashMap bufferMap = new IntObjectHashMap(8);

        private static int getHashCode(final PixelFormat.Composition hostPixelComp, final GLPixelAttributes pixelAttributes, final boolean pack) {
            // 31 * x == (x << 5) - x
            int hash = hostPixelComp.hashCode();
            hash = ((hash << 5) - hash) + pixelAttributes.hashCode();
            // hash = ((hash << 5) - hash) + (pack ? 100 : 0); // no difference due to RGBA/BGRA only modes.
            return hash;
        }

        /**
         * @param allowRowStride If <code>true</code>, allow row-stride, otherwise not. See {@link AWTGLPixelBuffer#requiresNewBuffer(GL, int, int, int)}.
         */
        public SingleAWTGLPixelBufferProvider(final boolean allowRowStride) {
            super(allowRowStride);
        }

        /**
         * {@inheritDoc}
         * <p>
         * Returns an array backed {@link IntBuffer} of size <pre>width*height*{@link Buffers#SIZEOF_INT SIZEOF_INT}</code>.
         * </p>
         */
        @Override
        public AWTGLPixelBuffer allocate(final GL gl, PixelFormat.Composition hostPixComp, final GLPixelAttributes pixelAttributes,
                                         final boolean pack, final int width, final int height, final int depth, final int minByteSize) {
            if( null == hostPixComp ) {
                hostPixComp = pixelAttributes.pfmt.comp;
            }
            final int bufferKey = getHashCode(hostPixComp, pixelAttributes, pack);
            AWTGLPixelBuffer r = (AWTGLPixelBuffer) bufferMap.get(bufferKey);
            if( null == r || r.requiresNewBuffer(gl, width, height, minByteSize) ) {
                if( null != r ) {
                    r.dispose();
                }
                r = allocateImpl(hostPixComp, pixelAttributes, pack,
                                 getAWTFormat(gl.getGLProfile(), hostPixComp.componentCount()), width, height, depth, minByteSize);
                bufferMap.put(bufferKey, r);
            }
            return r;
        }

        private AWTGLPixelBuffer allocateImpl(final PixelFormat.Composition hostPixComp,
                                              final GLPixelAttributes pixelAttributes,
                                              final boolean pack,
                                              final int awtFormat, final int width, final int height, final int depth,
                                              final int minByteSize) {
            final BufferedImage image = new BufferedImage(width, height, awtFormat);
            final int[] readBackIntBuffer = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
            final Buffer ibuffer = IntBuffer.wrap( readBackIntBuffer );
            return new AWTGLPixelBuffer(hostPixComp, pixelAttributes, pack,
                                        awtFormat, width, height, depth, image, ibuffer, getAllowRowStride());
        }

        /**
         * Return the last {@link #allocate(GL, PixelFormat.Composition, GLPixelAttributes, boolean, int, int, int, int) allocated}
         * {@link AWTGLPixelBuffer}, if compatible w/ the given {@link PixelFormat.Composition} and {@link GLPixelAttributes}.
         **/
        @Override
        public AWTGLPixelBuffer getSingleBuffer(final PixelFormat.Composition hostPixelComp, final GLPixelAttributes pixelAttributes, final boolean pack) {
            return (AWTGLPixelBuffer) bufferMap.get(getHashCode(hostPixelComp, pixelAttributes, pack));
        }

        /**
         * Initializes the single {@link AWTGLPixelBuffer} w/ a given size, if not yet {@link #allocate(GL, PixelFormat.Composition, GLPixelAttributes, boolean, int, int, int, int) allocated}.
         * @return the newly initialized single {@link AWTGLPixelBuffer}, or null if already allocated.
         */
        @Override
        public AWTGLPixelBuffer initSingleton(final GLProfile glp, final int componentCount,
                                              final boolean pack, final int width, final int height, final int depth) {
            final GLPixelAttributes pixelAttributes = getAttributes(glp, componentCount);
            final PixelFormat awtPixelFormat = getAWTPixelFormat(glp, componentCount);
            final int awtFormat = getAWTFormat(glp, componentCount);
            final int bufferKey = getHashCode(awtPixelFormat.comp, pixelAttributes, pack);
            AWTGLPixelBuffer r = (AWTGLPixelBuffer) bufferMap.get(bufferKey);
            if( null != r ) {
                return null;
            }
            r = allocateImpl(awtPixelFormat.comp, pixelAttributes, pack, awtFormat, width, height, depth, 0);
            bufferMap.put(bufferKey, r);
            return r;
        }

        @Override
        public void dispose() {
            for(final Iterator<IntObjectHashMap.Entry> i=bufferMap.iterator(); i.hasNext(); ) {
                final AWTGLPixelBuffer b = (AWTGLPixelBuffer)i.next().value;
                b.dispose();
            }
            bufferMap.clear();
        }
    }
}
