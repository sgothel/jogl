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
 * 
 */

package com.sun.javafx.newt;

public class WindowEvent extends Event {
    public static final int EVENT_WINDOW_RESIZED = 100;
    public static final int EVENT_WINDOW_MOVED   = 101; 
    public static final int EVENT_WINDOW_DESTROY_NOTIFY = 102;
    public static final int EVENT_WINDOW_GAINED_FOCUS = 103;
    public static final int EVENT_WINDOW_LOST_FOCUS = 104;
    // public static final int EVENT_WINDOW_REPAINT = 105; // TODO

    public WindowEvent(int eventType, Window source, long when) {
        this(false, eventType, source, when);
    }

    WindowEvent(boolean isSystemEvent, int eventType, Window source, long when) {
        super(isSystemEvent, eventType, source, when);
    }

    public static String getEventTypeString(int type) {
        switch(type) {
            case EVENT_WINDOW_RESIZED: return "WINDOW_RESIZED";
            case EVENT_WINDOW_MOVED:   return "WINDOW_MOVED";
            case EVENT_WINDOW_DESTROY_NOTIFY:   return "EVENT_WINDOW_DESTROY_NOTIFY";
            case EVENT_WINDOW_GAINED_FOCUS:   return "EVENT_WINDOW_GAINED_FOCUS";
            case EVENT_WINDOW_LOST_FOCUS:   return "EVENT_WINDOW_LOST_FOCUS";
            // case EVENT_WINDOW_REPAINT:   return "EVENT_WINDOW_REPAINT";
            default: return "unknown (" + type + ")";
        }
    }
    public String toString() {
        return "WindowEvent["+getEventTypeString(getEventType()) +
            ", " + super.toString() + "]";
    }
}
