package com.jogamp.opengl.util.av;

/**
 * Generic subtitle event
 * <p>
 * It is mandatory that the receiver {@link #release()} this instance
 * after processing, allowing the resource owner to free or reuse it.
 * </p>
 */
public abstract class SubtitleEvent {
    /** {@link CodecID} of this subtitle event. */
    public final CodecID codec;
    /** PTS start time to start showing this subtitle event. */
    public final int pts_start;
    /**
     * PTS start time to end showing this subtitle event.
     * <p>
     * {@link SubBitmapEvent} often (e.g. {@link CodecID#HDMV_PGS}) have an infinite end-time, i.e. ({@link Integer#MAX_VALUE},
     * and shall be overwritten by the next one or {@link SubEmptyEvent}.
     * </p>
     * @see #isEndDefined()
     */
    public final int pts_end;

    public SubtitleEvent(final CodecID codec, final int pts_start, final int pts_end) {
        this.codec = codec;
        this.pts_start = pts_start;
        this.pts_end = pts_end;
    }

    /** Release the resources, if any, back to the owner. */
    public abstract void release();

    public final int getDuration() { return pts_end - pts_start + 1; }

    /** See {@link #pts_end}. */
    public final boolean isEndDefined() { return pts_end < Integer.MAX_VALUE; }

    /** Returns {@code true} if Text/ASS/SAA subtitle type, o.e. {@link SubTextEvent}. */
    public abstract boolean isTextASS();
    /** Returns {@code true} if bitmap subtitle type, o.e. {@link SubBitmapEvent}. */
    public abstract boolean isBitmap();
    /** Returns {@code true} if empty subtitle type, o.e. {@link SubEmptyEvent}. */
    public abstract boolean isEmpty();

    public final String getStartString() {
        final boolean ied = isEndDefined();
        return "Sub["+codec+", ["+pts_start+".."+(ied?pts_end:"undef")+"] "+(ied?getDuration():"undef")+" ms";
    }


}