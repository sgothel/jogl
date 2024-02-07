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
package com.jogamp.graph.ui.shapes;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.ui.Group;
import com.jogamp.graph.ui.Scene;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.layout.Alignment;
import com.jogamp.graph.ui.layout.BoxLayout;
import com.jogamp.graph.ui.layout.Padding;
import com.jogamp.math.Vec2f;
import com.jogamp.math.Vec3f;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.util.PMVMatrix4f;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.TextureSequence;

import jogamp.graph.ui.TreeTool;

/**
 * A Head Up Display (HUD) {@link Shape} for a client {@link Shape} using `inner size and Mv position` to be displayed on top in a {@link Scene}.
 * <p>
 * Purpose of this class is to provide a convenient tool to create a HUD within {@link Scene}
 * using a Mv-coordinates and size of a target shape within the tree w/o manually transforming them to {@link Scene}.
 * </p>
 * <p>
 * Further, the client shape is wrapped in a layout group, not mutating it and hence allowing it for DAG usage.
 * </p>
 * <p>
 * This instance determines initial size and position in {@link #validate(GL2ES2)}, having a valid target shape.
 * </p>
 */
public class HUDShape extends Group {
    private final boolean hasFrame;
    private final Vec2f clientSize = new Vec2f();
    private final Vec3f clientPos = new Vec3f();
    private final Vec4f backColor = new Vec4f(0.9f, 0.9f, 0.9f, 0.9f);
    private final Vec4f frontColor = new Vec4f(0.1f, 0.1f, 0.1f, 0.9f);
    private final Rectangle frame;
    private final Scene scene;
    /** Target shape this HUD is put on top */
    private final Shape targetShape;
    /** Shape of this HUD */
    private Shape clientShape;

    private static final boolean DEBUG = false;

    /**
     * Ctor of {@link HUDShape}.
     * <p>
     * Adjust HUD position using {@code targetShape} object Mv-space coordinates via {@link #moveToHUDPos(Vec3f)} and {@link #moveHUDPos(Vec3f)}.
     * </p>
     * @param scene the {@link Scene} top-level container
     * @param clientWidth width of this HUD in given {@code targetShape} object Mv-space, not {@code scene}.
     * @param clientHeight height of this HUD in given {@code targetShape} object Mv-space, not {@code scene}.
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param targetShape target {@link Shape} this HUD is put on top, used to resolve the Mv matrix for HUD size and position
     * @param clientShape client {@link Shape} to be presented in the HUD tip
     */
    public HUDShape(final Scene scene, final float clientWidth, final float clientHeight,
                    final int renderModes, final Shape targetShape, final Shape clientShape) {
        this(scene, clientWidth, clientHeight, null, null, 0, null, renderModes, targetShape, clientShape);
    }
    /**
     * Ctor of {@link HUDShape}.
     * <p>
     * Adjust HUD position using {@code targetShape} object Mv-space coordinates via {@link #moveToHUDPos(Vec3f)} and {@link #moveHUDPos(Vec3f)}.
     * </p>
     * @param scene the {@link Scene} top-level container
     * @param clientWidth width of this HUD in given {@code targetShape} object Mv-space, not {@code scene}.
     * @param clientHeight height of this HUD in given {@code targetShape} object Mv-space, not {@code scene}.
     * @param backColor optional background color, will add a frame to this HUD if not {@code null}
     * @param borderColor optional border color, only used with {@code backColor}
     * @param borderThickness border thickness, only used with {@code backColor}
     * @param padding optional padding for the given {@code clientShape} for the internal wrapper group
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param targetShape target {@link Shape} this HUD is put on top, used to resolve the Mv matrix for HUD size and position
     * @param clientShape client {@link Shape} to be presented in the HUD tip
     */
    public HUDShape(final Scene scene, final float clientWidth, final float clientHeight,
                    final Vec4f backColor, final Vec4f borderColor, final float borderThickness,
                    final Padding padding, final int renderModes, final Shape targetShape, final Shape clientShape) {
        super();
        this.hasFrame = null != backColor;
        this.clientSize.set(clientWidth, clientHeight);
        if( hasFrame ) {
            this.backColor.set(backColor);
        }
        if( null != frontColor ) {
            this.frontColor.set(frontColor);
        }
        this.scene = scene;
        this.targetShape = targetShape;
        this.clientShape = clientShape;

        if( hasFrame ) {
            frame = (Rectangle) new Rectangle(renderModes, 1, 1, 0).setColor(backColor)
                              .setBorder(borderThickness).setBorderColor(frontColor)
                              .setName("HUD.frame").move(0, 0, -scene.getZEpsilon(16));
            addShape(frame.setInteractive(false));
        } else {
            frame = null;
        }

        // wrapper ensures user 'clientShape' won't get mutated (scale, move) for DAG
        final Group wrapper = new Group("HUD.wrapper", null, null, clientShape);
        if( null != padding ) {
            wrapper.setPaddding(padding);
        }
        addShape(wrapper);
        setName("HUD");
        markShapeDirty();
    }

    @Override
    protected void validateImpl(final GL2ES2 gl, final GLProfile glp) {
        if( isShapeDirty() ) {
            targetShape.validate(gl, glp);

            final PMVMatrix4f pmv = new PMVMatrix4f();

            tmpB0.reset().setSize(clientPos, tmpV0.set(clientPos).add(clientSize.x(), clientSize.y(), 0));
            final Vec3f hudSize = tmpV0;
            final Vec3f hudPos = tmpV1;
            final AABBox targetHUDBox = new AABBox();
            TreeTool.forOne(scene, pmv, targetShape, () -> {
                targetShape.getBounds().transform(pmv.getMv(), targetHUDBox);
                tmpB0.transform(pmv.getMv(), tmpB1);
                hudSize.set(tmpB1.getWidth(), tmpB1.getHeight(), 0);
                hudPos.set(tmpB1.getLow());
            });
            hudPos.add(0, 0, scene.getActiveTopLevelZOffsetScale()*scene.getZEpsilon(16));
            final AABBox sb = scene.getBounds();
            if( hudPos.x() < sb.getMinX() ) {
                hudPos.setX( sb.getMinX() );
            } else if( hudPos.x() + hudSize.x() > sb.getMaxX() ) {
                hudPos.setX( sb.getMaxX() - hudSize.x() );
            }
            if( hudPos.y() < sb.getMinY() ) {
                hudPos.setY( sb.getMinY() );
            } else if( hudPos.y() + hudSize.y() > sb.getMaxY() ) {
                hudPos.setY( sb.getMaxY() - hudSize.y() );
            }
            if( DEBUG ) {
                System.err.println("HUD validate");
                System.err.println("HUD Target b "+targetShape.getBounds());
                System.err.println("HUD Scene  b "+scene.getBounds());
            }

            if( !hudSizeOld.isEqual(hudSize) || null == this.getLayout() ) {
                if( DEBUG ) {
                    System.err.println("HUD size.1 "+clientSize+" -> "+hudSize);
                }
                this.setLayout(new BoxLayout(hudSize.x(), hudSize.y(), Alignment.FillCenter));
                hudSizeOld.set(hudSize);
            } else if( DEBUG ) {
                System.err.println("HUD size.0 "+clientSize+" -> "+hudSize);
            }
            this.moveTo(hudPos);

            super.validateImpl(gl, glp);
            if( DEBUG ) {
                System.err.println("HUD client b "+clientShape.getBounds());
                System.err.println("HUD this b "+this.getBounds());
                System.err.println("HUD pos "+clientPos+" -> "+hudPos);
            }
        }
    }
    private final Vec3f hudSizeOld = new Vec3f();
    private final AABBox tmpB0 = new AABBox();
    private final AABBox tmpB1 = new AABBox();
    private final Vec3f tmpV0 = new Vec3f();
    private final Vec3f tmpV1 = new Vec3f();

    /**
     * Move to scaled HUD position with given {@code clientPos} in {@code targetShape} object Mv-space coordinates. See {@link #moveTo(Vec3f)}.
     * @see #moveHUDPos(Vec3f)
     */
    public HUDShape moveToHUDPos(final Vec3f clientPos) {
        this.clientPos.set(clientPos);
        this.markShapeDirty();
        return this;
    }
    /**
     * Move about scaled HUD position with given {@code clientDelta} in {@code targetShape} object Mv-space coordinates. See {@link #move(Vec3f)}.
     * @see #moveToHUDPos(Vec3f)
     */
    public HUDShape moveHUDPos(final Vec3f clientDelta) {
        this.clientPos.add(clientDelta);
        this.markShapeDirty();
        return this;
    }

    /** Sets the client {@link Shape} size of this HUD in given {@code targetShape} object Mv-space, not {@link Scene}. */
    public HUDShape setClientSize(final float clientWidth, final float clientHeight) {
        this.clientSize.set(clientWidth, clientHeight);
        this.markShapeDirty();
        return this;
    }
    /** Returns the client {@link Shape} size of this HUD in given {@code targetShape} object Mv-space, not {@link Scene}. */
    public Vec2f getClientSize() { return this.clientSize; }
    /** Returns the client {@link Shape} position of this HUD in given {@code targetShape} object Mv-space, not {@link Scene}. */
    public Vec3f getClientPos() { return this.clientPos; }
    /** Returns the client {@link Shape} to be presented in the HUD tip */
    public Shape getClientShape() { return this.clientShape; }
    /** Returns the target {@link Shape} this HUD is put on top, used to resolve the Mv matrix for HUD size and position */
    public Shape getTargetShape() { return this.targetShape; }

    /**
     * Removed the user provided client {@link Shape} from this HUD.
     * <p>
     * This allows the user to release its own passed client {@link Shape} back, e.g. before destruction.
     * </p>
     * @param tip created tip {@link Shape} via {@link #createTip(Scene, AABBox)}
     * @return the user provided client {@link Shape}
     */
    public Shape removeClient() {
        final Shape cs = clientShape;
        clientShape = null;
        if( null != cs ) {
            final Group tipWrapper = (Group)getShapeByIdx(1);
            if( null == tipWrapper.removeShape(cs) ) {
                System.err.println("HUDShape.destroyTip: Warning: ClientShape "+cs.getName()+" not contained in "+tipWrapper.getName()+"; Internal Group: ");
                TreeTool.forAll(this, (final Shape s) -> {
                    System.err.println("- "+s.getName());
                    return false;
                });
            }
        }
        return cs;
    }

}
