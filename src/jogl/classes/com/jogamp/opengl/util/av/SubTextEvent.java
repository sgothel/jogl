/**
 * Copyright 2024 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.util.av;

import java.time.format.DateTimeParseException;

import com.jogamp.common.av.PTS;

/**
 * Text Event Line including ASS/SAA of {@link SubtitleEvent}
 * <p>
 * See http://www.tcax.org/docs/ass-specs.htm
 * </p>
 */
public class SubTextEvent extends SubtitleEvent {
    /** Text formatting */
    public enum TextFormat {
        /** Multiple ASS formats may be passed, see {@link ASSType}. */
        ASS,
        /** Just plain text */
        TEXT,
    };
    /** ASS Formatting Type */
    public enum ASSType {
        /**
         * ASS dialogue-line output w/ start and end (Given by FFmpeg 4.*)
         * <pre>
           0       1      2    3      4     5        6        7        8       9
           Marked, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text

           'Dialogue: 0,0:02:02.15,0:02:02.16,Default,,0,0,0,,trying to force him to travel to that'
         * </pre>
         */
        DIALOGUE,
        /**
         * FFMpeg ASS event-line output w/o start, end (Given by FFmpeg 5.*, 6.*, ..)
         * <pre>
           0    1      2      3     4        5        6        7       8
           Seq, Layer, Style, Name, MarginL, MarginR, MarginV, Effect, TEXT
         * </pre>
         */
        EVENT,
        /** Just plain text */
        TEXT
    }
    /** {@link TextFormat} of this text subtitle event. */
    public final TextFormat textFormat;
    /** {@link ASSType} sub-type */
    public final ASSType assType;
    /** Start time in milliseconds, or -1. */
    public final int start;
    /** End time in milliseconds, or -1. */
    public final int end;
    public final String style;

    public final int seqnr;
    public final int layer;

    public final String name;
    public final String effect;
    /** Actual subtitle text */
    public final String text;
    /** Number of lines of {@link #text}, i.e. occurrence of {@code \n} + 1. */
    public final int lines;

    private static boolean DEBUG = false;

    /**
     * ASS/SAA Event Line ctor
     * @param codec the {@link CodecID}
     * @param lang language code, supposed to be 3-letters of `ISO 639-2 language codes`
     * @param fmt input format of {@code ass}
     * @param ass ASS/SAA compatible event line according to {@link ASSType}
     * @param pts_start pts start in ms
     * @param pts_end pts end in ms
     */
    public SubTextEvent(final CodecID codec, final String lang, final TextFormat fmt, final String ass, final int pts_start, final int pts_end) {
        super(SubtitleEvent.Type.Text, codec, lang, pts_start, pts_end);
        this.textFormat = fmt;
        ASSType assType = ASSType.TEXT;
        int start = -1;
        int end = -1;
        int seqnr = 0;
        int layer = 0;
        String style = "Default";
        String name = "";
        String effect = "";
        String text = "";
        boolean done = false;
        if( TextFormat.ASS == fmt ) {
            final int len = null != ass ? ass.length() : 0;
            {
                // ASSType.DIALOGUE
                int part = 0;
                for(int i=0; 10 > part && len > i; ) {
                    if( 9 == part ) {
                        text = ass.substring(i);
                        done = true;
                        assType = ASSType.DIALOGUE;
                    } else {
                        final int j = ass.indexOf(',', i);
                        if( 0 > j ) {
                            break;
                        }
                        final String v = ass.substring(i, j);
                        try {
                            switch(part) {
                                case 1:
                                    start = PTS.toMillis(v, true);
                                    break;
                                case 2:
                                    end = PTS.toMillis(v, true);
                                    break;
                                case 3:
                                    style = v;
                                    break;
                                case 4:
                                    name = v;
                                    break;
                                case 8:
                                    effect = v;
                                    break;
                            }
                        } catch(final DateTimeParseException pe) {
                            if( DEBUG ) {
                                System.err.println("ASS.DIALG parsing error of part "+part+" '"+v+"' of '"+ass+"'");
                            }
                            break;
                        }
                        i = j + 1;
                    }
                    ++part;
                }
            }
            if( !done ) {
                // ASSType.EVENT
                int part = 0;
                for(int i=0; 9 > part && len > i; ) {
                    if( 8 == part ) {
                        text = ass.substring(i);
                        done = true;
                        assType = ASSType.EVENT;
                    } else {
                        final int j = ass.indexOf(',', i);
                        if( 0 > j ) {
                            break;
                        }
                        final String v = ass.substring(i, j);
                        try {
                            switch(part) {
                                case 0:
                                    seqnr = Integer.valueOf(v);
                                    break;
                                case 1:
                                    layer = Integer.valueOf(v);
                                    break;
                                case 2:
                                    style = v;
                                    break;
                                case 3:
                                    name = v;
                                    break;
                                case 7:
                                    effect = v;
                                    break;
                            }
                        } catch(final NumberFormatException nfe) {
                            if( DEBUG ) {
                                System.err.println("ASS.EVENT parsing error of part "+part+" '"+v+"' of '"+ass+"'");
                            }
                            break;
                        }
                        i = j + 1;
                    }
                    ++part;
                }
            }
        }
        if( !done && TextFormat.TEXT == fmt ) {
            text = ass;
            done = true;
            assType = ASSType.TEXT;
        }
        this.assType = assType;
        this.start = start;
        this.end = end;
        this.seqnr = seqnr;
        this.layer = layer;
        this.style = style;
        this.name = name;
        this.effect = effect;
        this.text = text.replace("\\N", "\n");
        {
            final int len = this.text.length();
            int lc = 1;
            for(int i=0; len > i; ) {
                final int j = this.text.indexOf("\n", i);
                if( 0 > j ) {
                    break;
                }
                ++lc;
                i = j + 1;
            }
            this.lines = lc;
        }
    }

    @Override
    public void release() {} // nothing to be released back to the owner

    @Override
    public String toString() {
        final String start_s = 0 <= start ? PTS.toTimeStr(start, true) : "undef";
        final String end_s = 0 <= end ? PTS.toTimeStr(end, true) : "undef";
        final String fms_s = TextFormat.ASS == textFormat ? "ASS("+assType+")" : textFormat.toString();
        return getStartString()+", "+fms_s+", #"+seqnr+", l_"+layer+
               ", ["+start_s+".."+end_s+"], style "+style+", name '"+name+"', effect '"+effect+"': '"+text+"' ("+lines+")]";
    }
}
