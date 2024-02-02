package com.jogamp.opengl.util.av;

import com.jogamp.common.av.PTS;

/**
 * Generic subtitle event
 * <p>
 * It is mandatory that the receiver {@link #release()} this instance
 * after processing, allowing the resource owner to free or reuse it.
 * </p>
 */
public abstract class SubtitleEvent {
    /** {@link SubtitleEvent} Implementation Type */
    public enum Type {
        /** {@link SubTextEvent} */
        Text,
        /** {@link SubBitmapEvent} */
        Bitmap,
        /** {@link SubEmptyEvent} */
        Empty
    };
    /** Implementation {@link Type} of this instance. */
    public final Type type;
    /** {@link CodecID} of this subtitle event. */
    public final CodecID codec;
    /** Language code, supposed to be 3-letters of `ISO 639-2 language codes` */
    public final String lang;
    /** PTS start time in milliseconds to start showing this subtitle event. */
    public final int pts_start;
    /**
     * PTS end time in milliseconds to end showing this subtitle event.
     * <p>
     * {@link SubBitmapEvent} often (e.g. {@link CodecID#HDMV_PGS}) have an infinite end-time, i.e. ({@link Integer#MAX_VALUE},
     * and shall be overwritten by the next one or {@link SubEmptyEvent}.
     * </p>
     * @see #isEndDefined()
     */
    public final int pts_end;

    /**
     *
     * @param type
     * @param codec the {@link CodecID}
     * @param lang language code, supposed to be 3-letters of `ISO 639-2 language codes`
     * @param pts_start pts start in ms, see {@link #pts_start}
     * @param pts_end pts end in ms, see {@link #pts_end}
     */
    public SubtitleEvent(final Type type, final CodecID codec, final String lang, final int pts_start, final int pts_end) {
        this.type = type;
        this.codec = codec;
        this.lang = lang;
        this.pts_start = pts_start;
        this.pts_end = pts_end;
    }

    /** Release the resources, if any, back to the owner. */
    public abstract void release();

    public final int getDuration() { return pts_end - pts_start + 1; }

    /** See {@link #pts_end}. */
    public final boolean isEndDefined() { return pts_end < Integer.MAX_VALUE; }

    public final String getStartString() {
        final boolean ied = isEndDefined();
        final String pts_start_s = 0 <= pts_start ? PTS.toTimeStr(pts_start, true) : "undef";
        final String pts_end_s = 0 <= pts_end && ied ? PTS.toTimeStr(pts_end, true) : "undef";
        return "Sub[codec "+codec+", lang '"+lang+"', type "+type+", ["+pts_start_s+".."+pts_end_s+"] "+(ied?getDuration():"undef")+" ms";
    }
}
