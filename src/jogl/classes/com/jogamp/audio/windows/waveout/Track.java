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
import java.nio.*;

public class Track {
    // Default number of samples per buffer
    private static final int BUFFER_SIZE = 32768;
    // Number of bytes per sample (FIXME: dependence on audio format)
    static final int BYTES_PER_SAMPLE = 2;
    // Whether we need byte swapping (FIXME: dependence on audio format)
    static final boolean NEEDS_BYTE_SWAP = true;

    // This is the buffer this track is currently playing from
    private SoundBuffer activeBuffer;
    // This is the sample position in the active buffer
    private int samplePosition;
    // This is the total number of samples in the file
    private int totalSamples;
    // This is the total number of samples we have read
    private int samplesRead;
    // This is the buffer that the background filler thread may be filling
    private SoundBuffer fillingBuffer;
    // If we're playing the file, this is its input stream
    private InputStream input;
    // Keep around the file name
    private final File file;
    // Whether we're playing this sound
    private boolean playing;
    // Whether we're looping this sound
    private boolean looping;
    // The position of this sound; defaults to being at the origin
    private volatile Vec3f position = new Vec3f();

    Track(final File file) throws IOException {
        if (!file.getName().endsWith(".rawsound")) {
            throw new IOException("Unsupported file format (currently supports only raw sounds)");
        }

        this.file = file;
        openInput();

        // Allocate the buffers
        activeBuffer  = new SoundBuffer(BUFFER_SIZE, BYTES_PER_SAMPLE, NEEDS_BYTE_SWAP);
        fillingBuffer = new SoundBuffer(BUFFER_SIZE, BYTES_PER_SAMPLE, NEEDS_BYTE_SWAP);

        // Fill the first buffer immediately
        fill();
        swapBuffers();
    }

    private void openInput() throws IOException {
        input = new BufferedInputStream(new FileInputStream(file));
        totalSamples = (int) file.length() / BYTES_PER_SAMPLE;
    }

    public File getFile() {
        return file;
    }

    public synchronized void play() {
        if (input == null) {
            try {
                openInput();
                // Fill it immediately
                fill();
            } catch (final IOException e) {
                e.printStackTrace();
                return;
            }
        }

        playing = true;
    }

    public synchronized boolean isPlaying() {
        return playing;
    }

    public synchronized void setLooping(final boolean looping) {
        this.looping = looping;
    }

    public synchronized boolean isLooping() {
        return looping;
    }

    public void setPosition(final float x, final float y, final float z) {
        position = new Vec3f(x, y, z);
    }

    synchronized void fill() throws IOException {
        if (input == null) {
            return;
        }
        final SoundBuffer curBuffer = fillingBuffer;
        if (!curBuffer.empty()) {
            return;
        }
        curBuffer.fill(input);
        if (curBuffer.empty()) {
            // End of file
            InputStream tmp = null;
            synchronized(this) {
                tmp = input;
                input = null;
            }
            tmp.close();

            // If looping, re-open
            if (isLooping()) {
                openInput();
                // and fill
                fill();
            }
        }
    }

    // These are only for use by the Mixer
    private float leftGain;
    private float rightGain;

    void setLeftGain(final float leftGain) {
        this.leftGain = leftGain;
    }

    float getLeftGain() {
        return leftGain;
    }

    void setRightGain(final float rightGain) {
        this.rightGain = rightGain;
    }

    float getRightGain() {
        return rightGain;
    }

    Vec3f getPosition() {
        return position;
    }

    // This is called by the mixer and must be extremely fast
    // Note this assumes mono sounds (FIXME)
    boolean hasNextSample() {
        return (!activeBuffer.empty() && samplePosition < activeBuffer.numSamples());
    }

    // This is called by the mixer and must be extremely fast
    float nextSample() {
        final float res = activeBuffer.getSample(samplePosition++);
        ++samplesRead;
        if (!hasNextSample()) {
            swapBuffers();
            samplePosition = 0;
            if (done()) {
                playing = false;
            }
        }
        return res;
    }

    synchronized void swapBuffers() {
        final SoundBuffer tmp = activeBuffer;
        activeBuffer = fillingBuffer;
        fillingBuffer = tmp;
        fillingBuffer.empty(true);
    }

    // This provides a more robust termination condition
    boolean done() {
        return (samplesRead == totalSamples) && !looping;
    }
}
