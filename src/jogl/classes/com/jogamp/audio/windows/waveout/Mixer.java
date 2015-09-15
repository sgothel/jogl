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
import java.util.*;

import com.jogamp.common.util.InterruptSource;
import com.jogamp.common.util.InterruptedRuntimeException;

// Needed only for NIO workarounds on CVM
import java.lang.reflect.*;

public class Mixer {
    // This class is a singleton
    private static Mixer mixer;

    volatile boolean fillerAlive;
    volatile boolean mixerAlive;
    volatile boolean shutdown;
    volatile Object shutdownLock = new Object();

    // Windows Event object
    private final long event;

    private volatile ArrayList<Track> tracks = new ArrayList<Track>();

    private final Vec3f leftSpeakerPosition  = new Vec3f(-1, 0, 0);
    private final Vec3f rightSpeakerPosition = new Vec3f( 1, 0, 0);

    private float falloffFactor = 1.0f;

    static {
        mixer = new Mixer();
    }

    private Mixer() {
        event = CreateEvent();
        fillerAlive = false;
        mixerAlive = false;
        shutdown = false;
        new FillerThread().start();
        final MixerThread m = new MixerThread();
        m.setPriority(Thread.MAX_PRIORITY - 1);
        m.start();
    }

    public static Mixer getMixer() {
        return mixer;
    }

    synchronized void add(final Track track) {
        final ArrayList<Track> newTracks = new ArrayList<Track>(tracks);
        newTracks.add(track);
        tracks = newTracks;
    }

    synchronized void remove(final Track track) {
        final ArrayList<Track> newTracks = new ArrayList<Track>(tracks);
        newTracks.remove(track);
        tracks = newTracks;
    }

    // NOTE: due to a bug on the APX device, we only have mono sounds,
    // so we currently only pay attention to the position of the left
    // speaker
    public void setLeftSpeakerPosition(final float x, final float y, final float z) {
        leftSpeakerPosition.set(x, y, z);
    }

    // NOTE: due to a bug on the APX device, we only have mono sounds,
    // so we currently only pay attention to the position of the left
    // speaker
    public void setRightSpeakerPosition(final float x, final float y, final float z) {
        rightSpeakerPosition.set(x, y, z);
    }

    /** This defines a scale factor of sorts -- the higher the number,
        the larger an area the sound will affect. Default value is
        1.0f. Valid values are [1.0f, ...]. The formula for the gain
        for each channel is
<PRE>
     falloffFactor
  -------------------
  falloffFactor + r^2
</PRE>
*/
    public void setFalloffFactor(final float factor) {
        falloffFactor = factor;
    }

    public void shutdown() {
        synchronized(shutdownLock) {
            shutdown = true;
            SetEvent(event);
            try {
                while(fillerAlive || mixerAlive) {
                    shutdownLock.wait();
                }
            } catch (final InterruptedException e) {
                throw new InterruptedRuntimeException(e);
            }
        }
    }

    class FillerThread extends InterruptSource.Thread {
        FillerThread() {
            super(null, null, "Mixer Thread");
        }

        @Override
        public void run() {
            fillerAlive = true;
            try {
                while (!shutdown) {
                    final List<Track> curTracks = tracks;

                    for (final Iterator<Track> iter = curTracks.iterator(); iter.hasNext(); ) {
                        final Track track = iter.next();
                        try {
                            track.fill();
                        } catch (final IOException e) {
                            e.printStackTrace();
                            remove(track);
                        }
                    }
                    try {
                        // Run ten times per second
                        java.lang.Thread.sleep(100);
                    } catch (final InterruptedException e) {
                        throw new InterruptedRuntimeException(e);
                    }
                }
            } finally {
                fillerAlive = false;
            }
        }
    }

    class MixerThread extends InterruptSource.Thread {
        // Temporary mixing buffer
        // Interleaved left and right channels
        float[] mixingBuffer;
        private final Vec3f temp = new Vec3f();

        MixerThread() {
            super(null, null, "Mixer Thread");
            if (!initializeWaveOut(event)) {
                throw new InternalError("Error initializing waveout device");
            }
        }

        @Override
        public void run() {
            mixerAlive = true;
            try {
                while (!shutdown) {
                    // Get the next buffer
                    final long mixerBuffer = getNextMixerBuffer();
                    if (mixerBuffer != 0) {
                        ByteBuffer buf = getMixerBufferData(mixerBuffer);

                        if (buf == null) {
                            // This is happening on CVM because
                            // JNI_NewDirectByteBuffer isn't implemented
                            // by default and isn't compatible with the
                            // JSR-239 NIO implementation (apparently)
                            buf = newDirectByteBuffer(getMixerBufferDataAddress(mixerBuffer),
                                                      getMixerBufferDataCapacity(mixerBuffer));
                        }

                        if (buf == null) {
                            throw new InternalError("Couldn't wrap the native address with a direct byte buffer");
                        }

                        // System.out.println("Mixing buffer");

                        // If we don't have enough samples in our mixing buffer, expand it
                        // FIXME: knowledge of native output rendering format
                        if ((mixingBuffer == null) || (mixingBuffer.length < (buf.capacity() / 2 /* bytes / sample */))) {
                            mixingBuffer = new float[buf.capacity() / 2];
                        } else {
                            // Zap it
                            for (int i = 0; i < mixingBuffer.length; i++) {
                                mixingBuffer[i] = 0.0f;
                            }
                        }

                        // This assertion should be in place if we have stereo
                        if ((mixingBuffer.length % 2) != 0) {
                            final String msg = "FATAL ERROR: odd number of samples in the mixing buffer";
                            System.out.println(msg);
                            throw new InternalError(msg);
                        }

                        // Run down all of the registered tracks mixing them in
                        final List<Track> curTracks = tracks;

                        for (final Iterator<Track> iter = curTracks.iterator(); iter.hasNext(); ) {
                            final Track track = iter.next();
                            // Consider only playing tracks
                            if (track.isPlaying()) {
                                // First recompute its gain
                                final Vec3f pos = track.getPosition();
                                final float leftGain  = gain(pos, leftSpeakerPosition);
                                final float rightGain = gain(pos, rightSpeakerPosition);
                                // Now mix it in
                                int i = 0;
                                while (i < mixingBuffer.length) {
                                    if (track.hasNextSample()) {
                                        final float sample = track.nextSample();
                                        mixingBuffer[i++] = sample * leftGain;
                                        mixingBuffer[i++] = sample * rightGain;
                                    } else {
                                        // This allows tracks to stall without being abruptly cancelled
                                        if (track.done()) {
                                            remove(track);
                                        }
                                        break;
                                    }
                                }
                            }
                        }

                        // Now that we have our data, send it down to the card
                        int outPos = 0;
                        for (int i = 0; i < mixingBuffer.length; i++) {
                            final short val = (short) mixingBuffer[i];
                            buf.put(outPos++, (byte)  val);
                            buf.put(outPos++, (byte) (val >> 8));
                        }
                        if (!prepareMixerBuffer(mixerBuffer)) {
                            throw new RuntimeException("Error preparing mixer buffer");
                        }
                        if (!writeMixerBuffer(mixerBuffer)) {
                            throw new RuntimeException("Error writing mixer buffer to device");
                        }
                    } else {
                        // System.out.println("No mixer buffer available");

                        // Wait for a buffer to become available
                        if (!WaitForSingleObject(event)) {
                            throw new RuntimeException("Error while waiting for event object");
                        }

                        /*
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            throw new InterruptedRuntimeException(e);
                        }
                        */
                    }
                }
            } finally {
                mixerAlive = false;
                // Need to shut down
                shutdownWaveOut();
                synchronized(shutdownLock) {
                    shutdownLock.notifyAll();
                }
            }
        }

        // This defines the 3D spatialization gain function.
        // The function is defined as:
        //    falloffFactor
        // -------------------
        // falloffFactor + r^2
        private float gain(final Vec3f pos, final Vec3f speakerPos) {
            temp.sub(pos, speakerPos);
            final float dotp = temp.dot(temp);
            return (falloffFactor / (falloffFactor + dotp));
        }
    }

    // Initializes waveout device
    private static native boolean initializeWaveOut(long eventObject);
    // Shuts down waveout device
    private static native void shutdownWaveOut();

    // Gets the next (opaque) buffer of data to fill from the native
    // code, or 0 if none was available yet (it should not happen that
    // none is available the way the code is written).
    private static native long getNextMixerBuffer();
    // Gets the next ByteBuffer to fill out of the mixer buffer. It
    // requires interleaved left and right channel samples, 16 signed
    // bits per sample, little endian. Implicit 44.1 kHz sample rate.
    private static native ByteBuffer getMixerBufferData(long mixerBuffer);
    // We need these to work around the lack of
    // JNI_NewDirectByteBuffer in CVM + the JSR 239 NIO classes
    private static native long getMixerBufferDataAddress(long mixerBuffer);
    private static native int  getMixerBufferDataCapacity(long mixerBuffer);
    // Prepares this mixer buffer for writing to the device.
    private static native boolean prepareMixerBuffer(long mixerBuffer);
    // Writes this mixer buffer to the device.
    private static native boolean writeMixerBuffer(long mixerBuffer);

    // Helpers to prevent mixer thread from busy waiting
    private static native long CreateEvent();
    private static native boolean WaitForSingleObject(long event);
    private static native void SetEvent(long event);
    private static native void CloseHandle(long handle);

    // We need a reflective hack to wrap a direct ByteBuffer around
    // the native memory because JNI_NewDirectByteBuffer doesn't work
    // in CVM + JSR-239 NIO
    private static Class directByteBufferClass;
    private static Constructor directByteBufferConstructor;
    private static Map createdBuffers = new HashMap(); // Map Long, ByteBuffer

    private static ByteBuffer newDirectByteBuffer(final long address, final long capacity) {
        final Long key = Long.valueOf(address);
        ByteBuffer buf = (ByteBuffer) createdBuffers.get(key);
        if (buf == null) {
            buf = newDirectByteBufferImpl(address, capacity);
            if (buf != null) {
                createdBuffers.put(key, buf);
            }
        }
        return buf;
    }
    private static ByteBuffer newDirectByteBufferImpl(final long address, final long capacity) {
        if (directByteBufferClass == null) {
            try {
                directByteBufferClass = Class.forName("java.nio.DirectByteBuffer");
                final byte[] tmp = new byte[0];
                directByteBufferConstructor =
                    directByteBufferClass.getDeclaredConstructor(new Class[] { Integer.TYPE,
                                                                               tmp.getClass(),
                                                                               Integer.TYPE });
                directByteBufferConstructor.setAccessible(true);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        if (directByteBufferConstructor != null) {
            try {
                return (ByteBuffer)
                    directByteBufferConstructor.newInstance(new Object[] {
                            Integer.valueOf((int) capacity),
                            null,
                            Integer.valueOf((int) address)
                        });
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
