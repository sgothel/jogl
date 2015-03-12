/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * OpenGL buffer storage object reflecting it's
 * <ul>
 *   <li>storage size</li>
 *   <li>storage memory if mapped</li>
 *   <li>mutable usage or immutable flags</li>
 * </ul>
 * <p>
 * Buffer storage is created via:
 * <ul>
 *   <li>{@link GL#glBufferData(int, long, java.nio.Buffer, int)} - storage creation via target</li>
 *   <li>{@link GL2#glNamedBufferDataEXT(int, long, java.nio.Buffer, int)} - storage creation, direct</li>
 *   <li>{@link GL4#glNamedBufferData(int, long, java.nio.Buffer, int)} - storage creation, direct</li>
 *   <li>{@link GL4#glBufferStorage(int, long, Buffer, int)} - storage creation via target</li>
 *   <li>{@link GL4#glNamedBufferStorage(int, long, Buffer, int)} - storage creation, direct</li>
 * </ul>
 * Note that storage <i>recreation</i> as mentioned above also invalidate a previous storage instance,
 * i.e. disposed the buffer's current storage if exist and attaches a new storage instance.
 * </p>
 * <p>
 * Buffer storage is disposed via:
 * <ul>
 *   <li>{@link GL#glDeleteBuffers(int, IntBuffer)} - explicit, direct, via {@link #notifyBuffersDeleted(int, IntBuffer)} or {@link #notifyBuffersDeleted(int, int[], int)}</li>
 *   <li>{@link GL#glBufferData(int, long, java.nio.Buffer, int)} - storage recreation via target</li>
 *   <li>{@link GL2#glNamedBufferDataEXT(int, long, java.nio.Buffer, int)} - storage recreation, direct</li>
 *   <li>{@link GL4#glNamedBufferData(int, long, java.nio.Buffer, int)} - storage recreation, direct</li>
 *   <li>{@link GL4#glBufferStorage(int, long, Buffer, int)} - storage recreation via target</li>
 *   <li>{@link GL4#glNamedBufferStorage(int, long, Buffer, int)} - storage recreation, direct</li>
 * </ul>
 * </p>
 * <p>
 * GL buffer storage is mapped via
 * <ul>
 *   <li>{@link GL#mapBuffer(int, int)}</li>
 *   <li>{@link GL#mapBufferRange(int, long, long, int)}</li>
 *   <li>{@link GL2#mapNamedBufferEXT(int, int)}</li>
 *   <li>{@link GL2#mapNamedBufferRangeEXT(int, long, long, int)}</li>
 *   <li>{@link GL4#mapNamedBuffer(int, int)}</li>
 *   <li>{@link GL4#mapNamedBufferRange(int, long, long, int)}</li>
 * </ul>
 * </p>
 * <p>
 * GL buffer storage is unmapped via
 * <ul>
 *   <li>{@link GL#glDeleteBuffers(int, IntBuffer)} - buffer deletion</li>
 *   <li>{@link GL#glUnmapBuffer(int)} - explicit via target</li>
 *   <li>{@link GL2#glUnmapNamedBufferEXT(int)} - explicit direct</li>
 *   <li>{@link GL4#glUnmapNamedBuffer(int)} - explicit direct</li>
 *   <li>{@link GL#glBufferData(int, long, java.nio.Buffer, int)} - storage recreation via target</li>
 *   <li>{@link GL2#glNamedBufferDataEXT(int, long, java.nio.Buffer, int)} - storage recreation, direct</li>
 *   <li>{@link GL4#glNamedBufferData(int, long, java.nio.Buffer, int)} - storage recreation, direct</li>
 *   <li>{@link GL4#glBufferStorage(int, long, Buffer, int)} - storage creation via target</li>
 *   <li>{@link GL4#glNamedBufferStorage(int, long, Buffer, int)} - storage creation, direct</li>
 * </ul>
 * </p>
 */
public abstract class GLBufferStorage {
        private final int name;
        private /* final */ long size;
        private /* final */ int mutableUsage;
        private /* final */ int immutableFlags;
        private ByteBuffer mappedBuffer;

        protected GLBufferStorage(final int name, final long size, final int mutableUsage, final int immutableFlags) {
            this.name = name;
            this.size = size;
            this.mutableUsage = mutableUsage;
            this.immutableFlags = immutableFlags;
            this.mappedBuffer = null;
        }

        protected void reset(final long size, final int mutableUsage, final int immutableFlags) {
            this.size = size;
            this.mutableUsage = mutableUsage;
            this.immutableFlags = immutableFlags;
            this.mappedBuffer = null;
        }
        protected void setMappedBuffer(final ByteBuffer buffer) {
            this.mappedBuffer = buffer;
        }

        /** Return the buffer name */
        public final int getName() { return name; }

        /** Return the buffer's storage size. */
        public final long getSize() { return size; }

        /**
         * Returns <code>true</code> if buffer's storage is mutable, i.e.
         * created via {@link GL#glBufferData(int, long, java.nio.Buffer, int)}.
         * <p>
         * Returns <code>false</code> if buffer's storage is immutable, i.e.
         * created via {@link GL4#glBufferStorage(int, long, Buffer, int)}.
         * </p>
         * @return
         */
        public final boolean isMutableStorage() { return 0 != mutableUsage; }

        /**
         * Returns the mutable storage usage or 0 if storage is not {@link #isMutableStorage() mutable}.
         */
        public final int getMutableUsage() { return mutableUsage; }

        /**
         * Returns the immutable storage flags, invalid if storage is {@link #isMutableStorage() mutable}.
         */
        public final int getImmutableFlags() { return immutableFlags; }

        /**
         * Returns the mapped ByteBuffer, or null if not mapped.
         * Mapping may occur via:
         * <ul>
         *   <li>{@link GL#glMapBuffer(int, int)}</li>
         *   <li>{@link GL#glMapBufferRange(int, long, long, int)}</li>
         *   <li>{@link GL2#glMapNamedBufferEXT(int, int)}</li>
         *   <li>{@link GL2#glMapNamedBufferRangeEXT(int, long, long, int)}
         * </ul>
         */
        public final ByteBuffer getMappedBuffer() { return mappedBuffer; }

        public final String toString() {
            return toString(false);
        }
        public final String toString(final boolean skipMappedBuffer) {
            final String s0;
            if( isMutableStorage() ) {
                s0 = String.format("%s[name %s, size %d, mutable usage 0x%X", msgClazzName, name, size, mutableUsage);
            } else {
                s0 = String.format("%s[name %s, size %d, immutable flags 0x%X", msgClazzName, name, size, immutableFlags);
            }
            if(skipMappedBuffer) {
                return s0+"]";
            } else {
                return s0+", mapped "+mappedBuffer+"]";
            }
        }
        private static final String msgClazzName = "GLBufferStorage";
}
