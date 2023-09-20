/**
 * Copyright 2023 JogAmp Community. All rights reserved.
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
package com.jogamp.math.util;

import java.nio.Buffer;
import java.nio.FloatBuffer;

import com.jogamp.math.Matrix4f;

/** {@link SyncBuffer} {@link SyncMatrix4f} implementation for a single underlying {@link Matrix4f} using one {@code float[16]} backing array. */
public final class SyncMatrix4f16 implements SyncMatrix4f {
    private final Matrix4f mat;
    private final float[] f16;
    private final FloatBuffer fbuf;
    private final SyncAction action = new SyncAction() {
        @Override
        public void sync() { mat.get(f16); }
    };

    public SyncMatrix4f16() {
        this.mat = new Matrix4f();
        this.f16 = new float[16];
        this.fbuf = FloatBuffer.wrap(f16);
    }

    public SyncMatrix4f16(final Matrix4f m) {
        this.mat = m;
        this.f16 = new float[16];
        this.fbuf = FloatBuffer.wrap(f16);
    }

    @Override
    public SyncAction getAction() { return action; }

    @Override
    public Buffer getBuffer() { return fbuf; }

    @Override
    public SyncBuffer sync() { getAction().sync(); return this; }

    @Override
    public Buffer getSyncBuffer() { getAction().sync(); return fbuf; }

    @Override
    public Matrix4f getMatrix() { return mat; }

    @Override
    public FloatBuffer getSyncFloats() { getAction().sync(); return fbuf; }
}