/**
 * Copyright 2011-2023 JogAmp Community. All rights reserved.
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
package jogamp.graph.font.typecast;

import jogamp.graph.font.typecast.ot.table.HeadTable;
import jogamp.graph.font.typecast.ot.table.HheaTable;

import com.jogamp.graph.font.Font.Metrics;
import com.jogamp.opengl.math.geom.AABBox;

final class TypecastHMetrics implements Metrics {
    private final TypecastFont fontImpl;

    // HeadTable
    private final HeadTable headTable;
    private final int unitsPerEM;
    private final float unitsPerEM_inv;
    private final AABBox bbox;
    // HheaTable
    private final HheaTable hheaTable;
    // VheaTable (for horizontal fonts)
    // private final VheaTable vheaTable;

    public TypecastHMetrics(final TypecastFont fontImpl) {
        this.fontImpl = fontImpl;
        headTable = this.fontImpl.font.getHeadTable();
        hheaTable = this.fontImpl.font.getHheaTable();
        // vheaTable = this.fontImpl.font.getVheaTable();
        unitsPerEM = headTable.getUnitsPerEm();
        unitsPerEM_inv = 1.0f / unitsPerEM;

        final int maxWidth = headTable.getXMax() - headTable.getXMin();
        final int maxHeight = headTable.getYMax() - headTable.getYMin();
        final int lowx= headTable.getXMin();
        final int lowy = -(headTable.getYMin()+maxHeight);
        final int highx = lowx + maxWidth;
        final int highy = lowy + maxHeight;
        bbox = new AABBox(lowx, lowy, 0, highx, highy, 0); // invert
    }

    @Override
    public int getAscentFU() {
        return hheaTable.getAscender();
    }

    @Override
    public float getAscent() {
        return getScale( getAscentFU() );
    }

    @Override
    public int getDescentFU() {
        return hheaTable.getDescender();
    }

    @Override
    public float getDescent() {
        return getScale( getDescentFU() );
    }

    @Override
    public int getLineGapFU() {
        return hheaTable.getLineGap();
    }

    @Override
    public float getLineGap() {
        return getScale( getLineGapFU() );
    }

    @Override
    public int getMaxExtendFU() {
        return hheaTable.getXMaxExtent();
    }

    @Override
    public float getMaxExtend() {
        return getScale( getMaxExtendFU() );
    }

    @Override
    public final int getUnitsPerEM() {
        return unitsPerEM;
    }

    @Override
    public final float getScale(final int funits) {
        return funits * unitsPerEM_inv;
    }

    @Override
    public final AABBox getBoundsFU(final AABBox dest) {
        return dest.copy(bbox);
    }

    @Override
    public AABBox getBounds(final AABBox dest) {
        return dest.copy(bbox).scale2(unitsPerEM_inv);
    }
}
