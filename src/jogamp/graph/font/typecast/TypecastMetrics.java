/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

import net.java.dev.typecast.ot.table.HeadTable;
import net.java.dev.typecast.ot.table.HheaTable;

import com.jogamp.graph.font.Font.Metrics;
import com.jogamp.graph.geom.AABBox;

class TypecastMetrics implements Metrics {
	private final TypecastFont fontImpl;
	
	// HeadTable
	private final float unitsPerEM_Inv;
	private final float unitsPerEM_Inv_sized;
	private final AABBox bbox_sized;
	
	// HheaTable
	private final float ascend_sized;
	private final float descent_sized;
    private final float linegap_sized;
	
	public TypecastMetrics(TypecastFont fontImpl) {
		this.fontImpl = fontImpl;
		final HeadTable headTable = this.fontImpl.font.getHeadTable();
		unitsPerEM_Inv = 1.0f / ( (float) headTable.getUnitsPerEm() );
		unitsPerEM_Inv_sized = this.fontImpl.size * unitsPerEM_Inv;
		int maxWidth = headTable.getXMax() - headTable.getXMin();
		int maxHeight = headTable.getYMax() - headTable.getYMin();    			
		float lowx= headTable.getXMin();
		float lowy = -(headTable.getYMin()+maxHeight);
		float highx = lowx + maxWidth;
		float highy = lowy + maxHeight;
		bbox_sized = new AABBox(lowx, lowy, 0, highx, highy, 0); // invert
		bbox_sized.scale(unitsPerEM_Inv_sized);
    	
    	final HheaTable hheaTable = this.fontImpl.font.getHheaTable();
    	ascend_sized = unitsPerEM_Inv_sized * -hheaTable.getAscender(); // invert
    	descent_sized = unitsPerEM_Inv_sized * -hheaTable.getDescender(); // invert
    	linegap_sized = unitsPerEM_Inv_sized * -hheaTable.getLineGap(); // invert
	}
	
    public final float getAscent() {
        return ascend_sized;
    }
    public final float getDescent() {
        return descent_sized;
    }
    public final float getLineGap() {
        return linegap_sized;
    }
    public final float getScale() {
		return unitsPerEM_Inv_sized;
	}
    public float getScaleForPixelSize(float pixelSize) {
		return pixelSize * unitsPerEM_Inv;
	}                                    
    public final AABBox getBBox() {
    	return bbox_sized;
    }
}