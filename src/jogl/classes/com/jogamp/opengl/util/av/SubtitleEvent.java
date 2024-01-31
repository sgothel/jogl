package com.jogamp.opengl.util.av;

/**
 * Generic subtitle event
 * <p>
 * It is mandatory that the receiver {@link #release()} this instance
 * after processing, allowing the resource owner to free or reuse it.
 * </p>
 */
public abstract class SubtitleEvent {
    public enum Format {
        /** Denoting an {@link SubEmptyEvent}, usually used for PGS bitmap subtitle signaling end of previous {@link SubTextureEvent}. */
        EMPTY,
        /** Denoting {@link SubASSEventLine} using FFMpeg output w/o start, end:
         * <pre>
           0    1      2      3     4        5        6        7       8
           Seq, Layer, Style, Name, MarginL, MarginR, MarginV, Effect, TEXT
         * </pre>
         */
        ASS_FFMPEG,
        /** Denoting {@link SubASSEventLine}, just the plain text part */
        ASS_TEXT,
        /** Denoting {@link SubTextureEvent}, a bitmap'ed subtitle requiring a texture */
        TEXTURE
    };

    /** {@link Format} of this subtitle event. */
    public final Format type;
    /** PTS start time to start showing this subtitle event. */
    public final int pts_start;
    /**
     * PTS start time to end showing this subtitle event.
     * <p>
     * PGS {@link SubTextureEvent} have an infinite end-time, i.e. ({@link Integer#MAX_VALUE},
     * and shall be overwritten by the next one or {@link SubEmptyEvent}.
     * </p>
     * @see #isEndDefined()
     */
    public final int pts_end;

    public SubtitleEvent(final Format fmt, final int pts_start, final int pts_end) {
        this.type = fmt;
        this.pts_start = pts_start;
        this.pts_end = pts_end;
    }

    /** Release the resources, if any, back to the owner. */
    public abstract void release();

    public final int getDuration() { return pts_end - pts_start + 1; }

    /** See {@link #pts_end}. */
    public final boolean isEndDefined() { return pts_end < Integer.MAX_VALUE; }

    public final boolean isASS() { return Format.ASS_FFMPEG == type || Format.ASS_TEXT == type; }
    public final boolean isTexture() { return Format.TEXTURE == type; }
    public final boolean isEmpty() { return Format.EMPTY == type; }

    public final String getStartString() {
        final boolean ied = isEndDefined();
        return "Sub["+type+", ["+pts_start+".."+(ied?pts_end:"undef")+"] "+(ied?getDuration():"undef")+" ms";
    }


}