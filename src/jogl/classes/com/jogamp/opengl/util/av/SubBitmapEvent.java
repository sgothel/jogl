/**
 * Copyright 2024 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.util.av;

import com.jogamp.math.Vec2i;
import com.jogamp.opengl.util.texture.Texture;

/**
 * Bitmap {@link Texture} event of {@link SubtitleEvent}
 * <p>
 * Consider {@link SubtitleEvent#pts_end} and {@link SubEmptyEvent}.
 * </p>
 */
public class SubBitmapEvent extends SubtitleEvent {
    /** To be implemented by the {@link Texture} owner to release the texture. */
    public static interface TextureOwner {
        /** The given {@link Texture} is to be released by the owner. */
        void release(Texture tex);
    }
    /** Subtitle texture position */
    public final Vec2i position;
    /** Subtitle texture dimension */
    public final Vec2i dimension;
    /** Subtitle texture or {@code null} if unused */
    public Texture texture;
    private final TextureOwner owner;

    /**
     * Texture Event ctor
     * @param codec the {@link CodecID}
     * @param pos texture position
     * @param dim texture dimension
     * @param tex the {@link Texture} or {@code null} if unused
     * @param pts_start pts start in ms
     * @param pts_end pts end in ms, often {@link #isEndDefined()} for bitmap'ed types see {@link #pts_end}
     * @param owner {@link Texture} owner code-stub to release the texture
     */
    public SubBitmapEvent(final CodecID codec, final Vec2i pos, final Vec2i dim, final Texture tex, final int pts_start, final int pts_end, final TextureOwner owner) {
        super(codec, pts_start, pts_end);
        position = pos;
        dimension = dim;
        texture = tex;
        this.owner = owner;
    }

    @Override
    public final boolean isTextASS() { return false; }
    @Override
    public final boolean isBitmap() { return true; }
    @Override
    public final boolean isEmpty() { return false; }

    /**
     * {@inheritDoc}
     * <p>
     * The {@link #texture} is released back to the owner
     * </p>
     */
    @Override
    public void release() {
        final Texture t = texture;
        texture = null;
        if( null != t ) {
            owner.release(t);
        }
    }
    @Override
    public String toString() {
        return getStartString()+", pos "+position+", dim "+dimension+", "+texture+"]";
    }
}
