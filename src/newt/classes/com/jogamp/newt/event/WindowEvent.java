/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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
 *
 */

package com.jogamp.newt.event;

/**
 * NEWT Window events are provided for notification purposes ONLY.<br>
 * NEWT will automatically handle component moves and resizes internally, regardless of whether a program is receiving these events or not. <br>
 * The actual event semantic, here move and resize, is processed before the event is send.<br>
 */
@SuppressWarnings("serial")
public class WindowEvent extends NEWTEvent {
    public static final short EVENT_WINDOW_RESIZED = 100;
    public static final short EVENT_WINDOW_MOVED   = 101;
    public static final short EVENT_WINDOW_DESTROY_NOTIFY = 102;
    public static final short EVENT_WINDOW_GAINED_FOCUS = 103;
    public static final short EVENT_WINDOW_LOST_FOCUS = 104;
    public static final short EVENT_WINDOW_REPAINT = 105;
    public static final short EVENT_WINDOW_DESTROYED = 106;

    public WindowEvent(final short eventType, final Object source, final long when) {
        super(eventType, source, when);
    }

    public static String getEventTypeString(final short type) {
        switch(type) {
            case EVENT_WINDOW_RESIZED: return "WINDOW_RESIZED";
            case EVENT_WINDOW_MOVED: return "WINDOW_MOVED";
            case EVENT_WINDOW_DESTROY_NOTIFY: return "EVENT_WINDOW_DESTROY_NOTIFY";
            case EVENT_WINDOW_GAINED_FOCUS: return "EVENT_WINDOW_GAINED_FOCUS";
            case EVENT_WINDOW_LOST_FOCUS: return "EVENT_WINDOW_LOST_FOCUS";
            case EVENT_WINDOW_REPAINT: return "EVENT_WINDOW_REPAINT";
            case EVENT_WINDOW_DESTROYED: return "EVENT_WINDOW_DESTROYED";
            default: return "unknown (" + type + ")";
        }
    }

    @Override
    public String toString() {
        return toString(null).toString();
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        if(null == sb) {
            sb = new StringBuilder();
        }
        sb.append("WindowEvent[").append(getEventTypeString(getEventType())).append(", ");
        return super.toString(sb).append("]");
    }
}
