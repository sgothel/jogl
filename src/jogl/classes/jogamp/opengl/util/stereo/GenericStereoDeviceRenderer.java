/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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
package jogamp.opengl.util.stereo;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.nativewindow.util.RectangleImmutable;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLArrayData;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLUniformData;

import jogamp.common.os.PlatformPropsImpl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.os.Platform;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.stereo.EyeParameter;
import com.jogamp.opengl.util.stereo.ViewerPose;
import com.jogamp.opengl.util.stereo.StereoDevice;
import com.jogamp.opengl.util.stereo.StereoDeviceRenderer;
import com.jogamp.opengl.util.stereo.StereoUtil;

/**
 * Generic Stereo Device Distortion and OpenGL Renderer Utility
 */
public class GenericStereoDeviceRenderer implements StereoDeviceRenderer {
    private static final String shaderPrefix01 = "dist01";
    private static final String shaderTimewarpSuffix = "_timewarp";
    private static final String shaderChromaSuffix = "_chroma";
    private static final String shaderPlainSuffix = "_plain";

    public static class GenericEye implements StereoDeviceRenderer.Eye {
        private final int eyeName;
        private final int distortionBits;
        private final int vertexCount;
        private final int indexCount;
        private final RectangleImmutable viewport;

        private final GLUniformData eyeToSourceUVScale;
        private final GLUniformData eyeToSourceUVOffset;
        private final GLUniformData eyeRotationStart;
        private final GLUniformData eyeRotationEnd;

        /** 2+2+2+2+2: { vec2 position, vec2 color, vec2 texCoordR, vec2 texCoordG, vec2 texCoordB } */
        private final GLArrayDataServer iVBO;
        private final GLArrayData vboPos, vboParams, vboTexCoordsR, vboTexCoordsG, vboTexCoordsB;
        private final GLArrayDataServer indices;

        private final EyeParameter eyeParameter;


        @Override
        public final RectangleImmutable getViewport() { return viewport; }

        @Override
        public final EyeParameter getEyeParameter() { return eyeParameter; }

        /* pp */ GenericEye(final GenericStereoDevice device, final int distortionBits,
                            final float[] eyePositionOffset, final EyeParameter eyeParam,
                            final DimensionImmutable textureSize, final RectangleImmutable eyeViewport) {
            this.eyeName = eyeParam.number;
            this.distortionBits = distortionBits;
            this.viewport = eyeViewport;

            final boolean usePP = null != device.config.distortionMeshProducer && 0 != distortionBits;

            final boolean usesTimewarp = usePP && StereoUtil.usesTimewarpDistortion(distortionBits);
            final FloatBuffer fstash = Buffers.newDirectFloatBuffer( 2 + 2 + ( usesTimewarp ? 16 + 16 : 0 ) ) ;

            if( usePP ) {
                eyeToSourceUVScale = new GLUniformData("svr_EyeToSourceUVScale", 2, Buffers.slice2Float(fstash, 0, 2));
                eyeToSourceUVOffset = new GLUniformData("svr_EyeToSourceUVOffset", 2, Buffers.slice2Float(fstash, 2, 2));
            } else {
                eyeToSourceUVScale = null;
                eyeToSourceUVOffset = null;
            }

            if( usesTimewarp ) {
                eyeRotationStart = new GLUniformData("svr_EyeRotationStart", 4, 4, Buffers.slice2Float(fstash, 4, 16));
                eyeRotationEnd = new GLUniformData("svr_EyeRotationEnd", 4, 4, Buffers.slice2Float(fstash, 20, 16));
            } else {
                eyeRotationStart = null;
                eyeRotationEnd = null;
            }

            this.eyeParameter = eyeParam;

            // Setup: eyeToSourceUVScale, eyeToSourceUVOffset
            if( usePP ) {
                final ScaleAndOffset2D textureScaleAndOffset = new ScaleAndOffset2D(eyeParam.fovhv, textureSize, eyeViewport);
                if( StereoDevice.DEBUG ) {
                    System.err.println("XXX."+eyeName+": eyeParam      "+eyeParam);
                    System.err.println("XXX."+eyeName+": uvScaleOffset "+textureScaleAndOffset);
                    System.err.println("XXX."+eyeName+": textureSize   "+textureSize);
                    System.err.println("XXX."+eyeName+": viewport      "+eyeViewport);
                }
                final FloatBuffer eyeToSourceUVScaleFB = eyeToSourceUVScale.floatBufferValue();
                eyeToSourceUVScaleFB.put(0, textureScaleAndOffset.scale[0]);
                eyeToSourceUVScaleFB.put(1, textureScaleAndOffset.scale[1]);
                final FloatBuffer eyeToSourceUVOffsetFB = eyeToSourceUVOffset.floatBufferValue();
                eyeToSourceUVOffsetFB.put(0, textureScaleAndOffset.offset[0]);
                eyeToSourceUVOffsetFB.put(1, textureScaleAndOffset.offset[1]);
            } else {
                vertexCount = 0;
                indexCount = 0;
                iVBO = null;
                vboPos = null;
                vboParams = null;
                vboTexCoordsR = null;
                vboTexCoordsG = null;
                vboTexCoordsB = null;
                indices = null;
                if( StereoDevice.DEBUG ) {
                    System.err.println("XXX."+eyeName+": "+this);
                }
                return;
            }
            final DistortionMesh meshData = device.config.distortionMeshProducer.create(eyeParam, distortionBits);
            if( null == meshData ) {
                throw new GLException("Failed to create meshData for eye "+eyeParam+", and "+StereoUtil.distortionBitsToString(distortionBits));
            }

            vertexCount = meshData.vertexCount;
            indexCount = meshData.indexCount;

            /** 2+2+2+2+2: { vec2 position, vec2 color, vec2 texCoordR, vec2 texCoordG, vec2 texCoordB } */
            final boolean useChromatic = StereoUtil.usesChromaticDistortion(distortionBits);
            final boolean useVignette = StereoUtil.usesVignetteDistortion(distortionBits);

            final int compsPerElement = 2+2+2+( useChromatic ? 2+2 /* texCoordG + texCoordB */: 0 );
            iVBO = GLArrayDataServer.createGLSLInterleaved(compsPerElement, GL.GL_FLOAT, false, vertexCount, GL.GL_STATIC_DRAW);
            vboPos = iVBO.addGLSLSubArray("svr_Position", 2, GL.GL_ARRAY_BUFFER);
            vboParams = iVBO.addGLSLSubArray("svr_Params", 2, GL.GL_ARRAY_BUFFER);
            vboTexCoordsR = iVBO.addGLSLSubArray("svr_TexCoordR", 2, GL.GL_ARRAY_BUFFER);
            if( useChromatic ) {
                vboTexCoordsG = iVBO.addGLSLSubArray("svr_TexCoordG", 2, GL.GL_ARRAY_BUFFER);
                vboTexCoordsB = iVBO.addGLSLSubArray("svr_TexCoordB", 2, GL.GL_ARRAY_BUFFER);
            } else {
                vboTexCoordsG = null;
                vboTexCoordsB = null;
            }
            indices = GLArrayDataServer.createData(1, GL.GL_SHORT, indexCount, GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);

            /** 2+2+2+2+2: { vec2 position, vec2 color, vec2 texCoordR, vec2 texCoordG, vec2 texCoordB } */
            final FloatBuffer iVBOFB = (FloatBuffer)iVBO.getBuffer();

            for ( int vertNum = 0; vertNum < vertexCount; vertNum++ ) {
                final DistortionMesh.DistortionVertex v = meshData.vertices[vertNum];
                int dataIdx = 0;

                if( StereoDevice.DUMP_DATA ) {
                    System.err.println("XXX."+eyeName+": START VERTEX "+vertNum+" / "+vertexCount);
                }
                // pos
                if( v.pos_size >= 2 ) {
                    if( StereoDevice.DUMP_DATA ) {
                        System.err.println("XXX."+eyeName+": pos ["+v.data[dataIdx]+", "+v.data[dataIdx+1]+"]");
                    }
                    iVBOFB.put(v.data[dataIdx]);
                    iVBOFB.put(v.data[dataIdx+1]);
                } else {
                    iVBOFB.put(0f);
                    iVBOFB.put(0f);
                }
                dataIdx += v.pos_size;

                // params
                if( v.vignetteFactor_size >= 1 && useVignette ) {
                    if( StereoDevice.DUMP_DATA ) {
                        System.err.println("XXX."+eyeName+": vignette "+v.data[dataIdx]);
                    }
                    iVBOFB.put(v.data[dataIdx]);
                } else {
                    iVBOFB.put(1.0f);
                }
                dataIdx += v.vignetteFactor_size;

                if( v.timewarpFactor_size >= 1 ) {
                    if( StereoDevice.DUMP_DATA ) {
                        System.err.println("XXX."+eyeName+": timewarp "+v.data[dataIdx]);
                    }
                    iVBOFB.put(v.data[dataIdx]);
                } else {
                    iVBOFB.put(1.0f);
                }
                dataIdx += v.timewarpFactor_size;

                // texCoordR
                if( v.texR_size >= 2 ) {
                    if( StereoDevice.DUMP_DATA ) {
                        System.err.println("XXX."+eyeName+": texR ["+v.data[dataIdx]+", "+v.data[dataIdx+1]+"]");
                    }
                    iVBOFB.put(v.data[dataIdx]);
                    iVBOFB.put(v.data[dataIdx+1]);
                } else {
                    iVBOFB.put(1f);
                    iVBOFB.put(1f);
                }
                dataIdx += v.texR_size;

                if( useChromatic ) {
                    // texCoordG
                    if( v.texG_size >= 2 ) {
                        if( StereoDevice.DUMP_DATA ) {
                            System.err.println("XXX."+eyeName+": texG ["+v.data[dataIdx]+", "+v.data[dataIdx+1]+"]");
                        }
                        iVBOFB.put(v.data[dataIdx]);
                        iVBOFB.put(v.data[dataIdx+1]);
                    } else {
                        iVBOFB.put(1f);
                        iVBOFB.put(1f);
                    }
                    dataIdx += v.texG_size;

                    // texCoordB
                    if( v.texB_size >= 2 ) {
                        if( StereoDevice.DUMP_DATA ) {
                            System.err.println("XXX."+eyeName+": texB ["+v.data[dataIdx]+", "+v.data[dataIdx+1]+"]");
                        }
                        iVBOFB.put(v.data[dataIdx]);
                        iVBOFB.put(v.data[dataIdx+1]);
                    } else {
                        iVBOFB.put(1f);
                        iVBOFB.put(1f);
                    }
                    dataIdx += v.texB_size;
                } else {
                    dataIdx += v.texG_size;
                    dataIdx += v.texB_size;
                }
            }
            if( StereoDevice.DUMP_DATA ) {
                System.err.println("XXX."+eyeName+": iVBO "+iVBO);
            }
            {
                if( StereoDevice.DUMP_DATA ) {
                    System.err.println("XXX."+eyeName+": idx "+indices+", count "+indexCount);
                    for(int i=0; i< indexCount; i++) {
                        if( 0 == i % 16 ) {
                            System.err.printf("%n%5d: ", i);
                        }
                        System.err.printf("%5d, ", (int)meshData.indices[i]);
                    }
                    System.err.println();
                }
                final ShortBuffer out = (ShortBuffer) indices.getBuffer();
                out.put(meshData.indices, 0, meshData.indexCount);
            }
            if( StereoDevice.DEBUG ) {
                System.err.println("XXX."+eyeName+": "+this);
            }
        }

        private void linkData(final GL2ES2 gl, final ShaderProgram sp) {
            if( null == iVBO ) return;

            if( 0 > vboPos.setLocation(gl, sp.program()) ) {
                throw new GLException("Couldn't locate "+vboPos);
            }
            if( 0 > vboParams.setLocation(gl, sp.program()) ) {
                throw new GLException("Couldn't locate "+vboParams);
            }
            if( 0 > vboTexCoordsR.setLocation(gl, sp.program()) ) {
                throw new GLException("Couldn't locate "+vboTexCoordsR);
            }
            if( StereoUtil.usesChromaticDistortion(distortionBits) ) {
                if( 0 > vboTexCoordsG.setLocation(gl, sp.program()) ) {
                    throw new GLException("Couldn't locate "+vboTexCoordsG);
                }
                if( 0 > vboTexCoordsB.setLocation(gl, sp.program()) ) {
                    throw new GLException("Couldn't locate "+vboTexCoordsB);
                }
            }
            if( 0 > eyeToSourceUVScale.setLocation(gl, sp.program()) ) {
                throw new GLException("Couldn't locate "+eyeToSourceUVScale);
            }
            if( 0 > eyeToSourceUVOffset.setLocation(gl, sp.program()) ) {
                throw new GLException("Couldn't locate "+eyeToSourceUVOffset);
            }
            if( StereoUtil.usesTimewarpDistortion(distortionBits) ) {
                if( 0 > eyeRotationStart.setLocation(gl, sp.program()) ) {
                    throw new GLException("Couldn't locate "+eyeRotationStart);
                }
                if( 0 > eyeRotationEnd.setLocation(gl, sp.program()) ) {
                    throw new GLException("Couldn't locate "+eyeRotationEnd);
                }
            }
            iVBO.seal(gl, true);
            iVBO.enableBuffer(gl, false);
            indices.seal(gl, true);
            indices.enableBuffer(gl, false);
        }

        /* pp */ void dispose(final GL2ES2 gl) {
            if( null == iVBO ) return;
            iVBO.destroy(gl);
            indices.destroy(gl);
        }
        /* pp */ void enableVBO(final GL2ES2 gl, final boolean enable) {
            if( null == iVBO ) return;
            iVBO.enableBuffer(gl, enable);
            indices.bindBuffer(gl, enable); // keeps VBO binding if enable:=true
        }

        /* pp */ void updateUniform(final GL2ES2 gl, final ShaderProgram sp) {
            if( null == iVBO ) return;
            gl.glUniform(eyeToSourceUVScale);
            gl.glUniform(eyeToSourceUVOffset);
            if( StereoUtil.usesTimewarpDistortion(distortionBits) ) {
                gl.glUniform(eyeRotationStart);
                gl.glUniform(eyeRotationEnd);
            }
        }

        @Override
        public String toString() {
            final String ppTxt = null == iVBO ? ", no post-processing" :
                        ", uvScale["+eyeToSourceUVScale.floatBufferValue().get(0)+", "+eyeToSourceUVScale.floatBufferValue().get(1)+
                        "], uvOffset["+eyeToSourceUVOffset.floatBufferValue().get(0)+", "+eyeToSourceUVOffset.floatBufferValue().get(1)+"]";

            return "Eye["+eyeName+", viewport "+viewport+
                        ", "+eyeParameter+
                        ", vertices "+vertexCount+", indices "+indexCount+
                        ppTxt+
                        ", desc "+eyeParameter+"]";
        }
    }

    private final GenericStereoDevice device;
    private final GenericEye[] eyes;
    private final ViewerPose viewerPose;
    private final int distortionBits;
    private final int textureCount;
    private final DimensionImmutable[] eyeTextureSizes;
    private final DimensionImmutable totalTextureSize;
    /** if texUnit0 is null: no post-processing */
    private final GLUniformData texUnit0;

    private ShaderProgram sp;
    private long frameStart = 0;

    @Override
    public String toString() {
        return "GenericStereo[distortion["+StereoUtil.distortionBitsToString(distortionBits)+
                             "], eyeTexSize "+Arrays.toString(eyeTextureSizes)+
                             ", sbsSize "+totalTextureSize+
                             ", texCount "+textureCount+", texUnit "+(null != texUnit0 ? texUnit0.intValue() : "n/a")+
                             ", "+PlatformPropsImpl.NEWLINE+"  "+(0 < eyes.length ? eyes[0] : "none")+
                             ", "+PlatformPropsImpl.NEWLINE+"  "+(1 < eyes.length ? eyes[1] : "none")+"]";
    }


    private static final DimensionImmutable zeroSize = new Dimension(0, 0);

    /* pp */ GenericStereoDeviceRenderer(final GenericStereoDevice context, final int distortionBits,
                                       final int textureCount, final float[] eyePositionOffset,
                                       final EyeParameter[] eyeParam, final float pixelsPerDisplayPixel, final int textureUnit,
                                       final DimensionImmutable[] eyeTextureSizes, final DimensionImmutable totalTextureSize,
                                       final RectangleImmutable[] eyeViewports) {
        if( eyeParam.length != eyeTextureSizes.length ||
            eyeParam.length != eyeViewports.length ) {
            throw new IllegalArgumentException("eye arrays of different length");
        }
        this.device = context;
        this.eyes = new GenericEye[eyeParam.length];
        this.distortionBits = ( distortionBits | context.getMinimumDistortionBits() ) & context.getSupportedDistortionBits();
        final boolean usePP = null != device.config.distortionMeshProducer && 0 != this.distortionBits;
        final DimensionImmutable[] textureSizes;

        if( usePP ) {
            if( 1 > textureCount || 2 < textureCount ) {
                this.textureCount = 2;
            } else {
                this.textureCount = textureCount;
            }
            this.eyeTextureSizes = eyeTextureSizes;
            this.totalTextureSize = totalTextureSize;
            if( 1 == textureCount ) {
                textureSizes = new DimensionImmutable[eyeParam.length];
                for(int i=0; i<eyeParam.length; i++) {
                    textureSizes[i] = totalTextureSize;
                }
            } else {
                textureSizes = eyeTextureSizes;
            }
            texUnit0 = new GLUniformData("svr_Texture0", textureUnit);
        } else {
            this.textureCount = 0;
            this.eyeTextureSizes = new DimensionImmutable[eyeParam.length];
            textureSizes = new DimensionImmutable[eyeParam.length];
            for(int i=0; i<eyeParam.length; i++) {
                this.eyeTextureSizes[i] = zeroSize;
                textureSizes[i] = zeroSize;
            }
            this.totalTextureSize = zeroSize;
            texUnit0 = null;
        }
        viewerPose = new ViewerPose();
        for(int i=0; i<eyeParam.length; i++) {
            eyes[i] = new GenericEye(context, this.distortionBits, eyePositionOffset, eyeParam[i], textureSizes[i], eyeViewports[i]);
        }

        sp = null;
    }

    @Override
    public StereoDevice getDevice() {  return device; }

    @Override
    public final int getDistortionBits() { return distortionBits; }

    @Override
    public final boolean usesSideBySideStereo() { return true; }

    @Override
    public final DimensionImmutable[] getEyeSurfaceSize() { return eyeTextureSizes; }

    @Override
    public final DimensionImmutable getTotalSurfaceSize() { return totalTextureSize; }

    @Override
    public final int getTextureCount() { return textureCount; }

    @Override
    public final int getTextureUnit() { return ppAvailable() ? texUnit0.intValue() : 0; }

    @Override
    public final boolean ppAvailable() { return null != texUnit0; }

    @Override
    public final void init(final GL gl) {
        if( StereoDevice.DEBUG ) {
            System.err.println(JoglVersion.getGLInfo(gl, null).toString());
        }
        if( null != sp ) {
            throw new IllegalStateException("Already initialized");
        }
        if( !ppAvailable() ) {
            return;
        }
        final GL2ES2 gl2es2 = gl.getGL2ES2();

        final String vertexShaderBasename;
        final String fragmentShaderBasename;
        {
            final boolean usesTimewarp = StereoUtil.usesTimewarpDistortion(distortionBits);
            final boolean usesChromatic = StereoUtil.usesChromaticDistortion(distortionBits);

            final StringBuilder sb = new StringBuilder();
            sb.append(shaderPrefix01);
            if( !usesChromatic && !usesTimewarp ) {
                sb.append(shaderPlainSuffix);
            } else if( usesChromatic && !usesTimewarp ) {
                sb.append(shaderChromaSuffix);
            } else if( usesTimewarp ) {
                sb.append(shaderTimewarpSuffix);
                if( usesChromatic ) {
                    sb.append(shaderChromaSuffix);
                }
            }
            vertexShaderBasename = sb.toString();
            sb.setLength(0);
            sb.append(shaderPrefix01);
            if( usesChromatic ) {
                sb.append(shaderChromaSuffix);
            } else {
                sb.append(shaderPlainSuffix);
            }
            fragmentShaderBasename = sb.toString();
        }
        final ShaderCode vp0 = ShaderCode.create(gl2es2, GL2ES2.GL_VERTEX_SHADER, GenericStereoDeviceRenderer.class, "shader",
                "shader/bin", vertexShaderBasename, true);
        final ShaderCode fp0 = ShaderCode.create(gl2es2, GL2ES2.GL_FRAGMENT_SHADER, GenericStereoDeviceRenderer.class, "shader",
                "shader/bin", fragmentShaderBasename, true);
        vp0.defaultShaderCustomization(gl2es2, true, true);
        fp0.defaultShaderCustomization(gl2es2, true, true);

        sp = new ShaderProgram();
        sp.add(gl2es2, vp0, System.err);
        sp.add(gl2es2, fp0, System.err);
        if(!sp.link(gl2es2, System.err)) {
            throw new GLException("could not link program: "+sp);
        }
        sp.useProgram(gl2es2, true);
        if( 0 > texUnit0.setLocation(gl2es2, sp.program()) ) {
            throw new GLException("Couldn't locate "+texUnit0);
        }
        for(int i=0; i<eyes.length; i++) {
            eyes[i].linkData(gl2es2, sp);
        }
        sp.useProgram(gl2es2, false);
    }

    @Override
    public final void dispose(final GL gl) {
        final GL2ES2 gl2es2 = gl.getGL2ES2();
        if( null != sp ) {
            sp.useProgram(gl2es2, false);
        }
        for(int i=0; i<eyes.length; i++) {
            eyes[i].dispose(gl2es2);
        }
        if( null != sp ) {
            sp.destroy(gl2es2);
        }
    }

    @Override
    public final Eye getEye(final int eyeNum) {
        return eyes[eyeNum];
    }

    @Override
    public final ViewerPose updateViewerPose() {
        // NOP
        return viewerPose;
    }

    @Override
    public final ViewerPose getLastViewerPose() {
        return viewerPose;
    }

    @Override
    public final void beginFrame(final GL gl) {
        frameStart = Platform.currentTimeMillis();
    }

    @Override
    public final void endFrame(final GL gl) {
        if( 0 == frameStart ) {
            throw new IllegalStateException("beginFrame not called");
        }
        frameStart = 0;
    }

    @Override
    public final void ppBegin(final GL gl) {
        if( null == sp ) {
            throw new IllegalStateException("Not initialized");
        }
        if( 0 == frameStart ) {
            throw new IllegalStateException("beginFrame not called");
        }
        final GL2ES2 gl2es2 = gl.getGL2ES2();

        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        gl.glActiveTexture(GL.GL_TEXTURE0 + getTextureUnit());

        gl2es2.glDisable(GL.GL_CULL_FACE);
        gl2es2.glDisable(GL.GL_DEPTH_TEST);
        gl2es2.glDisable(GL.GL_BLEND);

        if( !gl2es2.isGLcore() ) {
            gl2es2.glEnable(GL.GL_TEXTURE_2D);
        }

        sp.useProgram(gl2es2, true);

        gl2es2.glUniform(texUnit0);
    }

    @Override
    public final void ppOneEye(final GL gl, final int eyeNum) {
        final GenericEye eye = eyes[eyeNum];
        final GL2ES2 gl2es2 = gl.getGL2ES2();

        eye.updateUniform(gl2es2, sp);
        eye.enableVBO(gl2es2, true);
        gl2es2.glDrawElements(GL.GL_TRIANGLES, eye.indexCount, GL.GL_UNSIGNED_SHORT, 0);
        eyes[eyeNum].enableVBO(gl2es2, false);
    }

    @Override
    public final void ppEnd(final GL gl) {
        sp.useProgram(gl.getGL2ES2(), false);
    }
}
