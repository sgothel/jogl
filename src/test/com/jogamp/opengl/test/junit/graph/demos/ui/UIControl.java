package com.jogamp.opengl.test.junit.graph.demos.ui;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.geom.AABBox;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;

public abstract class UIControl {
	protected Font font = null;
	protected OutlineShape shape = null;
	protected String label = "Label";
	protected Factory<? extends Vertex> factory;
	
	protected boolean dirty = true;

	public UIControl(Factory<? extends Vertex> factory){
		this.factory = factory;
	}
	
	public abstract void generate(AABBox lbox);

	public Font getFont() {
		return font;
	}

	public void setFont(Font font) {
		this.font = font;
	}

	public OutlineShape getShape(AABBox lbox) {
		if(isDirty()){
			generate(lbox);
		}
		return shape;
	}
	
	public String getLabel(){
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
		setDirty(true);
	}
	
	protected boolean isDirty() {
		return dirty;
	}

	protected void setDirty(boolean dirty) {
		this.dirty = dirty;
	}
}
