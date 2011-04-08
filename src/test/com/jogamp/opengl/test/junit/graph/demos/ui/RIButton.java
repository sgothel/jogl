package com.jogamp.opengl.test.junit.graph.demos.ui;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.geom.AABBox;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;

public class RIButton extends UIControl{
	private float width = 4.0f, height= 3.0f;
	private float spacing = 0.3f;
	private float[] scale = new float[]{1.0f,1.0f};
	private float corner = 0.5f;
	private float labelZOffset = -0.05f;
	
	private float[] buttonColor = {0.0f, 0.0f, 0.0f};
	private float[] labelColor = {1.0f, 1.0f, 1.0f};
	
	public RIButton(Factory<? extends Vertex> factory, String label){
		super(factory);
		this.label = label;
		setFont(FontFactory.get(FontFactory.UBUNTU).getDefault());
	}

	public RIButton(Factory<? extends Vertex> factory, String label, Font font){
		super(factory);
		setLabel(label);
		setFont(font);
	}
	
	public float getWidth() {
		return width;
	}

	public void setDimensions(float width, float height) {
		this.width = width;
		this.height = height;
		setDirty(true);
	}

	public float getHeight() {
		return height;
	}

	public Font getFont() {
		return font;
	}
	
	public void generate(AABBox lbox) {
//		AABBox lbox = font.getStringBounds(label, 10);
		createOutline(factory, lbox);
		scale[0] = getWidth()/(2*spacing + lbox.getWidth());
		scale[1] = getHeight()/(2*spacing + lbox.getHeight());
		
		//FIXME: generate GlyphString to manipulate before rendering
		setDirty(false);
	}
	
	
	public float[] getScale() {
		return scale;
	}

	private void createOutline(Factory<? extends Vertex> factory, AABBox lbox){
		shape = new OutlineShape(factory);
		if(corner == 0.0f){
			createSharpOutline(lbox);
		}
		else{
			createCurvedOutline(lbox);
		}
	}
	private void createSharpOutline(AABBox lbox){
		float th = (2.0f*spacing) + lbox.getHeight();
		float tw = (2.0f*spacing) + lbox.getWidth();
		float minX = lbox.getMinX()-spacing;
		float minY = lbox.getMinY()-spacing;
		
		shape.addVertex(minX, minY, labelZOffset,  true);
		shape.addVertex(minX+tw, minY,  labelZOffset, true);
		shape.addVertex(minX+tw, minY + th, labelZOffset,  true);
		shape.addVertex(minX, minY + th, labelZOffset,  true);
		shape.closeLastOutline();
	}
	
	private void createCurvedOutline(AABBox lbox){
		float th = 2.0f*spacing + lbox.getHeight();
		float tw = 2.0f*spacing + lbox.getWidth();
		
		float cw = 0.5f*corner*Math.min(tw, th);
		float ch = 0.5f*corner*Math.min(tw, th);
		
		float minX = lbox.getMinX()-spacing;
		float minY = lbox.getMinY()-spacing;
		
		shape.addVertex(minX, minY + ch, labelZOffset, true);
		shape.addVertex(minX, minY,  labelZOffset, false);
		shape.addVertex(minX + cw, minY, labelZOffset,  true);
		shape.addVertex(minX + tw - cw, minY,  labelZOffset, true);
		shape.addVertex(minX + tw, minY, labelZOffset,  false);
		shape.addVertex(minX + tw, minY + ch, labelZOffset,  true);
		shape.addVertex(minX + tw, minY + th- ch, labelZOffset,  true);
		shape.addVertex(minX + tw, minY + th, labelZOffset,  false);
		shape.addVertex(minX + tw - cw, minY + th, labelZOffset,  true);
		shape.addVertex(minX + cw, minY + th, labelZOffset,  true);
		shape.addVertex(minX, minY + th, labelZOffset,  false);
		shape.addVertex(minX, minY + th - ch, labelZOffset,  true);
		shape.closeLastOutline();
	}

	public float getCorner() {
		return corner;
	}

	public void setCorner(float corner) {
		if(corner > 1.0f){
			this.corner = 1.0f;
		}
		else if(corner < 0.01f){
			this.corner = 0.0f;
		}
		else{
			this.corner = corner;
		}
		setDirty(true);
	}
	
	public float getLabelZOffset() {
		return labelZOffset;
	}

	public void setLabelZOffset(float labelZOffset) {
		this.labelZOffset = -labelZOffset;
		setDirty(true);
	}
	public float getSpacing() {
		return spacing;
	}

	public void setSpacing(float spacing) {
		if(spacing < 0.0f){
			this.spacing = 0.0f;
		}
		else{
			this.spacing = spacing;
		}
		setDirty(true);
	}
	
	public float[] getButtonColor() {
		return buttonColor;
	}

	public void setButtonColor(float r, float g, float b) {
		this.buttonColor[0] = r;
		this.buttonColor[1] = g;
		this.buttonColor[2] = b;
	}

	public float[] getLabelColor() {
		return labelColor;
	}

	public void setLabelColor(float r, float g, float b) {
		this.labelColor[0] = r;
		this.labelColor[1] = g;
		this.labelColor[2] = b;
	}
	
	public String toString(){
		return "RIButton [ label: " + getLabel() + "," + "spacing: " + spacing
			+ ", " + "corner: " + corner + ", " + "shapeOffset: " + labelZOffset + " ]";
	}
}
