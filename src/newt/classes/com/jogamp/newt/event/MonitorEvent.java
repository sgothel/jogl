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

package com.jogamp.newt.event;

import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.MonitorMode;

@SuppressWarnings("serial")
public class MonitorEvent extends OutputEvent {
    public static final short EVENT_MONITOR_MODE_CHANGE_NOTIFY = 600;
    public static final short EVENT_MONITOR_MODE_CHANGED       = 601;

    private final MonitorMode mode;

    public MonitorEvent (final short eventType, final MonitorDevice source, final long when, final MonitorMode mode) {
        super(eventType, source, when);
        this.mode = mode;
    }

    /** Returns the {@link #getSource() source}, which is a {@link MonitorDevice}. */
    public final MonitorDevice getMonitor() { return (MonitorDevice)source; }

    public final MonitorMode getMode() { return mode; }

    public static String getEventTypeString(final short type) {
        switch(type) {
        case EVENT_MONITOR_MODE_CHANGE_NOTIFY: return "EVENT_MONITOR_MODE_CHANGE_NOTIFY";
        case EVENT_MONITOR_MODE_CHANGED: return "EVENT_MONITOR_MODE_CHANGED";
        default: return "unknown (" + type + ")";
        }
    }

    @Override
    public final String toString() {
        return toString(null).toString();
    }

    @Override
    public final StringBuilder toString(StringBuilder sb) {
        if(null == sb) {
            sb = new StringBuilder();
        }
        sb.append("MonitorEvent[").append(getEventTypeString(getEventType())).append(", source ").append(source)
        .append(", mode ").append(mode).append(", ");
        return super.toString(sb).append("]");
    }
}
