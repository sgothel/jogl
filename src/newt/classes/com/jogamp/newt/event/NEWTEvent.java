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
 * NEWT events are provided for notification purposes ONLY;<br>
 * The NEWT will automatically handle the event semantics internally, regardless of whether a program is receiving these events or not.<br>
 * The actual event semantic is processed before the event is send.<br>
 *
 * Event type registry:<br>
 * <ul>
 *   <li> WindowEvent  <code>100..10x</code></li>
 *   <li> MouseEvent   <code>200..20x</code></li>
 *   <li> KeyEvent     <code>300..30x</code></li>
 *   <li> GestureEvent <code>400..5xx</code></li>
 *   <li> MonitorEvent <code>600..60x</code></li>
 * </ul><br>
 */
@SuppressWarnings("serial")
public class NEWTEvent extends java.util.EventObject {
    /**
     * See {@link #setConsumed(boolean)} for description.
     */
    public static final Object consumedTag = new Object();

    private final short eventType;
    private final long when;
    private Object attachment;

    static final boolean DEBUG = false;

    protected NEWTEvent(final short eventType, final Object source, final long when) {
        super(source);
        this.eventType = eventType;
        this.when = when;
        this.attachment=null;
    }

    /** Returns the event type of this event. */
    public final short getEventType() {
        return eventType;
    }

    /** Returns the timestamp, in milliseconds, of this event. */
    public final long getWhen()  {
        return when;
    }

    /**
     * Attach the passed object to this event.<br>
     * If an object was previously attached, it will be replaced.<br>
     * Attachments to NEWT events allow users to pass on information
     * from one custom listener to another, ie custom listener to listener
     * communication.
     * @param attachment User application specific object
     */
    public final void setAttachment(final Object attachment) {
        this.attachment = attachment;
    }

    /**
     * @return The user application specific attachment, or null
     */
    public final Object getAttachment() {
        return attachment;
    }

    /**
     * Returns <code>true</code> if this events has been {@link #setConsumed(boolean) consumed},
     * otherwise <code>false</code>.
     * @see #setConsumed(boolean)
     */
    public final boolean isConsumed() {
        return consumedTag == attachment;
    }

    /**
     * If <code>consumed</code> is <code>true</code>, this event is marked as consumed,
     * ie. the event will not be propagated any further to potential <i>other</i> event listener.
     * Otherwise the event will be propagated to other event listener, the default.
     * <p>
     * The event is marked as being consumed while {@link #setAttachment(Object) attaching}
     * the {@link #consumedTag}.
     * </p>
     * <p>
     * Events with platform specific actions will be supressed if marked as consumed.
     * Examples are:
     * <ul>
     *   <li>{@link KeyEvent#VK_ESCAPE} on Android's BACK button w/ Activity::finish()</li>
     *   <li>{@link KeyEvent#VK_HOME} on Android's HOME button w/ Intend.ACTION_MAIN[Intend.CATEGORY_HOME]</li>
     * </ul>
     * </p>
     */
    public final void setConsumed(final boolean consumed) {
        if( consumed ) {
            setAttachment( consumedTag );
        } else if( consumedTag == attachment ) {
            setAttachment( null );
        }
    }

    @Override
    public String toString() {
        return toString(null).toString();
    }

    public StringBuilder toString(StringBuilder sb) {
        if(null == sb) {
            sb = new StringBuilder();
        }
        return sb.append("NEWTEvent[source:").append(getSource().getClass().getName()).append(", consumed ").append(isConsumed()).append(", when:").append(getWhen()).append(" d ").append((System.currentTimeMillis()-getWhen())).append("ms]");
    }

    public static String toHexString(final short hex) {
        return "0x" + Integer.toHexString( hex & 0x0000FFFF );
    }
}
