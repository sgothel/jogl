/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */

package com.jogamp.audio.windows.waveout;

import java.io.*;

class SoundBuffer {
    private final byte[] data;
    private final boolean needsByteSwap;
    private int numBytes;
    private final int bytesPerSample;
    private int numSamples;
    private boolean playing;
    private boolean empty;

    // Note: needsByteSwap argument makes assumptions about the format
    SoundBuffer(final int size, final int bytesPerSample, final boolean needsByteSwap) {
        this.bytesPerSample = bytesPerSample;
        this.needsByteSwap = needsByteSwap;
        data = new byte[size * bytesPerSample];
        empty = true;
    }

    boolean playing() {
        return playing;
    }

    void playing(final boolean playing) {
        this.playing = playing;
    }

    boolean empty() {
        return empty;
    }

    void empty(final boolean empty) {
        this.empty = empty;
    }

    void fill(final InputStream input) throws IOException {
        synchronized(this) {
            if (playing) {
                throw new IllegalStateException("Can not fill a buffer that is playing");
            }
        }

        empty(true);
        final int num = input.read(data);
        if (num > 0) {
            numBytes = num;
            numSamples = numBytes / bytesPerSample;
            empty(false);
            if ((numBytes % bytesPerSample) != 0) {
                System.out.println("WARNING: needed integral multiple of " + bytesPerSample +
                                   " bytes, but read " + numBytes + " bytes");
            }
        } else {
            numBytes = 0;
        }
    }

    int numSamples() {
        return numSamples;
    }

    // This is called by the mixer and must be extremely fast
    // FIXME: may want to reconsider use of floating point at this point
    // FIXME: assumes all sounds are of the same format to avoid normalization
    float getSample(final int sample) {
        final int startByte = sample * bytesPerSample;
        // FIXME: assumes no more than 4 bytes per sample
        int res = 0;
        if (needsByteSwap) {
            for (int i = startByte + bytesPerSample - 1; i >= startByte; i--) {
                res <<= 8;
                res |= (data[i] & 0xff);
            }
        } else {
            final int endByte = startByte + bytesPerSample - 1;
            for (int i = startByte; i <= endByte; i++) {
                res <<= 8;
                res |= (data[i] & 0xff);
            }
        }
        // Sign extend
        if (bytesPerSample == 2) {
            res = (short) res;
        } else if (bytesPerSample == 1) {
            res = (byte) res;
        }

        return res;
    }
}
