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
package jogamp.opengl.oculusvr;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.media.nativewindow.util.DimensionImmutable;
import javax.media.nativewindow.util.RectangleImmutable;
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLArrayData;
import javax.media.opengl.GLException;
import javax.media.opengl.GLUniformData;

import jogamp.common.os.PlatformPropsImpl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.oculusvr.OVR;
import com.jogamp.oculusvr.OVRException;
import com.jogamp.oculusvr.OvrHmdContext;
import com.jogamp.oculusvr.ovrDistortionMesh;
import com.jogamp.oculusvr.ovrDistortionVertex;
import com.jogamp.oculusvr.ovrEyeRenderDesc;
import com.jogamp.oculusvr.ovrFovPort;
import com.jogamp.oculusvr.ovrFrameTiming;
import com.jogamp.oculusvr.ovrMatrix4f;
import com.jogamp.oculusvr.ovrPosef;
import com.jogamp.oculusvr.ovrRecti;
import com.jogamp.oculusvr.ovrSizei;
import com.jogamp.oculusvr.ovrVector2f;
import com.jogamp.oculusvr.ovrVector3f;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.stereo.EyeParameter;
import com.jogamp.opengl.util.stereo.EyePose;
import com.jogamp.opengl.util.stereo.StereoDevice;
import com.jogamp.opengl.util.stereo.StereoDeviceRenderer;
import com.jogamp.opengl.util.stereo.StereoUtil;

/**
 * OculusVR Stereo Device Distortion and OpenGL Renderer Utility
 */
public class OVRStereoDeviceRenderer implements StereoDeviceRenderer {
    private static final String shaderPrefix01 = "dist01";
    private static final String shaderTimewarpSuffix = "_timewarp";
    private static final String shaderChromaSuffix = "_chroma";
    private static final String shaderPlainSuffix = "_plain";

    public static class OVREye implements StereoDeviceRenderer.Eye {
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

        private final ovrEyeRenderDesc ovrEyeDesc;
        private final ovrFovPort ovrEyeFov;
        private final EyeParameter eyeParameter;

        private ovrPosef ovrEyePose;
        private final EyePose eyePose;

        @Override
        public final RectangleImmutable getViewport() { return viewport; }

        @Override
        public final EyeParameter getEyeParameter() { return eyeParameter; }

        @Override
        public final EyePose getLastEyePose() { return eyePose; }

        private OVREye(final OvrHmdContext hmdCtx, final int distortionBits,
                       final float[] eyePositionOffset, final ovrEyeRenderDesc eyeDesc,
                       final ovrSizei ovrTextureSize, final RectangleImmutable eyeViewport) {
            this.eyeName = eyeDesc.getEye();
            this.distortionBits = distortionBits;
            this.viewport = eyeViewport;

            final boolean usesTimewarp = StereoUtil.usesTimewarpDistortion(distortionBits);
            final FloatBuffer fstash = Buffers.newDirectFloatBuffer( 2 + 2 + ( usesTimewarp ? 16 + 16 : 0 ) ) ;

            eyeToSourceUVScale = new GLUniformData("ovr_EyeToSourceUVScale", 2, Buffers.slice2Float(fstash, 0, 2));
            eyeToSourceUVOffset = new GLUniformData("ovr_EyeToSourceUVOffset", 2, Buffers.slice2Float(fstash, 2, 2));

            if( usesTimewarp ) {
                eyeRotationStart = new GLUniformData("ovr_EyeRotationStart", 4, 4, Buffers.slice2Float(fstash, 4, 16));
                eyeRotationEnd = new GLUniformData("ovr_EyeRotationEnd", 4, 4, Buffers.slice2Float(fstash, 20, 16));
            } else {
                eyeRotationStart = null;
                eyeRotationEnd = null;
            }

            this.ovrEyeDesc = eyeDesc;
            this.ovrEyeFov = eyeDesc.getFov();

            final ovrVector3f eyeViewAdjust = eyeDesc.getViewAdjust();
            this.eyeParameter = new EyeParameter(eyeName, eyePositionOffset, OVRUtil.getFovHV(ovrEyeFov),
                                                 eyeViewAdjust.getX(), eyeViewAdjust.getY(), eyeViewAdjust.getZ());

            this.eyePose = new EyePose(eyeName);

            updateEyePose(hmdCtx); // 1st init

            // Setup: eyeToSourceUVScale, eyeToSourceUVOffset
            {
                final ovrVector2f[] uvScaleOffsetOut = new ovrVector2f[2];
                uvScaleOffsetOut[0] = ovrVector2f.create(); // FIXME: remove ctor / double check
                uvScaleOffsetOut[1] = ovrVector2f.create();

                final ovrRecti ovrEyeRenderViewport = OVRUtil.createOVRRecti(eyeViewport);
                OVR.ovrHmd_GetRenderScaleAndOffset(ovrEyeFov, ovrTextureSize, ovrEyeRenderViewport, uvScaleOffsetOut);
                if( StereoDevice.DEBUG ) {
                    System.err.println("XXX."+eyeName+": eyeParam      "+eyeParameter);
                    System.err.println("XXX."+eyeName+": uvScale       "+OVRUtil.toString(uvScaleOffsetOut[0]));
                    System.err.println("XXX."+eyeName+": uvOffset      "+OVRUtil.toString(uvScaleOffsetOut[1]));
                    System.err.println("XXX."+eyeName+": textureSize   "+OVRUtil.toString(ovrTextureSize));
                    System.err.println("XXX."+eyeName+": viewport      "+OVRUtil.toString(ovrEyeRenderViewport));
                }
                final FloatBuffer eyeToSourceUVScaleFB = eyeToSourceUVScale.floatBufferValue();
                eyeToSourceUVScaleFB.put(0, uvScaleOffsetOut[0].getX());
                eyeToSourceUVScaleFB.put(1, uvScaleOffsetOut[0].getY());
                final FloatBuffer eyeToSourceUVOffsetFB = eyeToSourceUVOffset.floatBufferValue();
                eyeToSourceUVOffsetFB.put(0, uvScaleOffsetOut[1].getX());
                eyeToSourceUVOffsetFB.put(1, uvScaleOffsetOut[1].getY());
            }

            final ovrDistortionMesh meshData = ovrDistortionMesh.create();

            final int ovrDistortionCaps = distBits2OVRDistCaps(distortionBits);
            if( !OVR.ovrHmd_CreateDistortionMesh(hmdCtx, eyeName, ovrEyeFov, ovrDistortionCaps, meshData) ) {
                throw new OVRException("Failed to create meshData for eye "+eyeName+", "+OVRUtil.toString(ovrEyeFov)+" and "+StereoUtil.distortionBitsToString(distortionBits));
            }
            vertexCount = meshData.getVertexCount();
            indexCount = meshData.getIndexCount();

            /** 2+2+2+2+2: { vec2 position, vec2 color, vec2 texCoordR, vec2 texCoordG, vec2 texCoordB } */
            final boolean useChromatic = StereoUtil.usesChromaticDistortion(distortionBits);
            final boolean useVignette = StereoUtil.usesVignetteDistortion(distortionBits);

            final int compsPerElement = 2+2+2+( useChromatic ? 2+2 /* texCoordG + texCoordB */: 0 );
            iVBO = GLArrayDataServer.createGLSLInterleaved(compsPerElement, GL.GL_FLOAT, false, vertexCount, GL.GL_STATIC_DRAW);
            vboPos = iVBO.addGLSLSubArray("ovr_Position", 2, GL.GL_ARRAY_BUFFER);
            vboParams = iVBO.addGLSLSubArray("ovr_Params", 2, GL.GL_ARRAY_BUFFER);
            vboTexCoordsR = iVBO.addGLSLSubArray("ovr_TexCoordR", 2, GL.GL_ARRAY_BUFFER);
            if( useChromatic ) {
                vboTexCoordsG = iVBO.addGLSLSubArray("ovr_TexCoordG", 2, GL.GL_ARRAY_BUFFER);
                vboTexCoordsB = iVBO.addGLSLSubArray("ovr_TexCoordB", 2, GL.GL_ARRAY_BUFFER);
            } else {
                vboTexCoordsG = null;
                vboTexCoordsB = null;
            }
            indices = GLArrayDataServer.createData(1, GL.GL_SHORT, indexCount, GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);

            /** 2+2+2+2+2: { vec2 position, vec2 color, vec2 texCoordR, vec2 texCoordG, vec2 texCoordB } */
            final FloatBuffer iVBOFB = (FloatBuffer)iVBO.getBuffer();
            final ovrDistortionVertex[] ovRes = new ovrDistortionVertex[1];
            ovRes[0] = ovrDistortionVertex.create(); // FIXME: remove ctor / double check

            for ( int vertNum = 0; vertNum < vertexCount; vertNum++ ) {
                final ovrDistortionVertex ov = meshData.getPVertexData(vertNum, ovRes)[0];
                ovrVector2f v;

                if( StereoDevice.DUMP_DATA ) {
                    System.err.println("XXX."+eyeName+": START VERTEX "+vertNum+" / "+vertexCount);
                }
                // pos
                v = ov.getPos();
                if( StereoDevice.DUMP_DATA ) {
                    System.err.println("XXX."+eyeName+": pos "+OVRUtil.toString(v));
                }
                iVBOFB.put(v.getX());
                iVBOFB.put(v.getY());

                // params
                if( useVignette ) {
                    if( StereoDevice.DUMP_DATA ) {
                        System.err.println("XXX."+eyeName+": vignette "+ov.getVignetteFactor());
                    }
                    iVBOFB.put(ov.getVignetteFactor());
                } else {
                    iVBOFB.put(1.0f);
                }
                if( StereoDevice.DUMP_DATA ) {
                    System.err.println("XXX."+eyeName+": timewarp "+ov.getTimeWarpFactor());
                }
                iVBOFB.put(ov.getTimeWarpFactor());

                // texCoordR
                v = ov.getTexR();
                if( StereoDevice.DUMP_DATA ) {
                    System.err.println("XXX."+eyeName+": texR "+OVRUtil.toString(v));
                }
                iVBOFB.put(v.getX());
                iVBOFB.put(v.getY());

                if( useChromatic ) {
                    // texCoordG
                    v = ov.getTexG();
                    if( StereoDevice.DUMP_DATA ) {
                        System.err.println("XXX."+eyeName+": texG "+OVRUtil.toString(v));
                    }
                    iVBOFB.put(v.getX());
                    iVBOFB.put(v.getY());

                    // texCoordB
                    v = ov.getTexB();
                    if( StereoDevice.DUMP_DATA ) {
                        System.err.println("XXX."+eyeName+": texB "+OVRUtil.toString(v));
                    }
                    iVBOFB.put(v.getX());
                    iVBOFB.put(v.getY());
                }
            }
            if( StereoDevice.DUMP_DATA ) {
                System.err.println("XXX."+eyeName+": iVBO "+iVBO);
            }
            {
                final ShortBuffer in = meshData.getPIndexData();
                if( StereoDevice.DUMP_DATA ) {
                    System.err.println("XXX."+eyeName+": idx "+indices+", count "+indexCount);
                    for(int i=0; i< indexCount; i++) {
                        if( 0 == i % 16 ) {
                            System.err.printf("%n%5d: ", i);
                        }
                        System.err.printf("%5d, ", (int)in.get(i));
                    }
                    System.err.println();
                }
                final ShortBuffer out = (ShortBuffer) indices.getBuffer();
                out.put(in);
            }
            if( StereoDevice.DEBUG ) {
                System.err.println("XXX."+eyeName+": "+this);
            }
            OVR.ovrHmd_DestroyDistortionMesh(meshData);
        }

        private void linkData(final GL2ES2 gl, final ShaderProgram sp) {
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

        private void dispose(final GL2ES2 gl) {
            iVBO.destroy(gl);
            indices.destroy(gl);
        }
        private void enableVBO(final GL2ES2 gl, final boolean enable) {
            iVBO.enableBuffer(gl, enable);
            indices.bindBuffer(gl, enable); // keeps VBO binding if enable:=true
        }

        private void updateUniform(final GL2ES2 gl, final ShaderProgram sp) {
            gl.glUniform(eyeToSourceUVScale);
            gl.glUniform(eyeToSourceUVOffset);
            if( StereoUtil.usesTimewarpDistortion(distortionBits) ) {
                gl.glUniform(eyeRotationStart);
                gl.glUniform(eyeRotationEnd);
            }
        }

        private void updateTimewarp(final OvrHmdContext hmdCtx, final ovrPosef eyeRenderPose, final float[] mat4Tmp1, final float[] mat4Tmp2) {
            final ovrMatrix4f[] timeWarpMatrices = new ovrMatrix4f[2];
            timeWarpMatrices[0] = ovrMatrix4f.create(); // FIXME: remove ctor / double check
            timeWarpMatrices[1] = ovrMatrix4f.create();
            OVR.ovrHmd_GetEyeTimewarpMatrices(hmdCtx, eyeName, eyeRenderPose, timeWarpMatrices);

            final float[] eyeRotationStartM = FloatUtil.transposeMatrix(timeWarpMatrices[0].getM(0, mat4Tmp1), mat4Tmp2);
            final FloatBuffer eyeRotationStartU = eyeRotationStart.floatBufferValue();
            eyeRotationStartU.put(eyeRotationStartM);
            eyeRotationStartU.rewind();

            final float[] eyeRotationEndM = FloatUtil.transposeMatrix(timeWarpMatrices[1].getM(0, mat4Tmp1), mat4Tmp2);
            final FloatBuffer eyeRotationEndU = eyeRotationEnd.floatBufferValue();
            eyeRotationEndU.put(eyeRotationEndM);
            eyeRotationEndU.rewind();
        }

        /**
         * Updates {@link #ovrEyePose} and it's extracted
         * {@link #eyeRenderPoseOrientation} and {@link #eyeRenderPosePosition}.
         * @param hmdCtx used get the {@link #ovrEyePose} via {@link OVR#ovrHmd_GetEyePose(OvrHmdContext, int)}
         */
        private EyePose updateEyePose(final OvrHmdContext hmdCtx) {
            ovrEyePose = OVR.ovrHmd_GetEyePose(hmdCtx, eyeName);
            final ovrVector3f pos = ovrEyePose.getPosition();
            eyePose.setPosition(pos.getX(), pos.getY(), pos.getZ());
            OVRUtil.copyToQuaternion(ovrEyePose.getOrientation(), eyePose.orientation);
            return eyePose;
        }

        @Override
        public String toString() {
            return "Eye["+eyeName+", viewport "+viewport+
                        ", "+eyeParameter+
                        ", vertices "+vertexCount+", indices "+indexCount+
                        ", uvScale["+eyeToSourceUVScale.floatBufferValue().get(0)+", "+eyeToSourceUVScale.floatBufferValue().get(1)+
                        "], uvOffset["+eyeToSourceUVOffset.floatBufferValue().get(0)+", "+eyeToSourceUVOffset.floatBufferValue().get(1)+
                        "], desc"+OVRUtil.toString(ovrEyeDesc)+", "+eyePose+"]";
        }
    }

    private final OVRStereoDevice context;
    private final OVREye[] eyes;
    private final int distortionBits;
    private final int textureCount;
    private final DimensionImmutable singleTextureSize;
    private final DimensionImmutable totalTextureSize;
    private final GLUniformData texUnit0;


    private final float[] mat4Tmp1 = new float[16];
    private final float[] mat4Tmp2 = new float[16];

    private ShaderProgram sp;
    private ovrFrameTiming frameTiming;

    @Override
    public String toString() {
        return "OVRDist[distortion["+StereoUtil.distortionBitsToString(distortionBits)+
                       "], singleSize "+singleTextureSize+
                       ", sbsSize "+totalTextureSize+
                       ", texCount "+textureCount+", texUnit "+getTextureUnit()+
                       ", "+PlatformPropsImpl.NEWLINE+"  "+eyes[0]+", "+PlatformPropsImpl.NEWLINE+"  "+eyes[1]+"]";
    }


    private static int distBits2OVRDistCaps(final int distortionBits) {
        int caps = 0;
        if( StereoUtil.usesTimewarpDistortion(distortionBits) ) {
            caps |= OVR.ovrDistortionCap_TimeWarp;
        }
        if( StereoUtil.usesChromaticDistortion(distortionBits) ) {
            caps |= OVR.ovrDistortionCap_Chromatic;
        }
        if( StereoUtil.usesVignetteDistortion(distortionBits) ) {
            caps |= OVR.ovrDistortionCap_Vignette;
        }
        return caps;
    }

    /* pp */ OVRStereoDeviceRenderer(final OVRStereoDevice context, final int distortionBits,
                             final int textureCount, final float[] eyePositionOffset,
                             final ovrEyeRenderDesc[] eyeRenderDescs, final DimensionImmutable singleTextureSize, final DimensionImmutable totalTextureSize,
                             final RectangleImmutable[] eyeViewports, final int textureUnit) {
        if( 1 > textureCount || 2 < textureCount ) {
            throw new IllegalArgumentException("textureCount can only be 1 or 2, has "+textureCount);
        }
        this.context = context;
        this.eyes = new OVREye[2];
        this.distortionBits = ( distortionBits | context.getMinimumDistortionBits() ) & context.getSupportedDistortionBits();
        this.textureCount = textureCount;
        this.singleTextureSize = singleTextureSize;
        this.totalTextureSize = totalTextureSize;

        texUnit0 = new GLUniformData("ovr_Texture0", textureUnit);

        final ovrSizei ovrTextureSize = OVRUtil.createOVRSizei( 1 == textureCount ? totalTextureSize : singleTextureSize );
        eyes[0] = new OVREye(context.handle, this.distortionBits, eyePositionOffset, eyeRenderDescs[0], ovrTextureSize, eyeViewports[0]);
        eyes[1] = new OVREye(context.handle, this.distortionBits, eyePositionOffset, eyeRenderDescs[1], ovrTextureSize, eyeViewports[1]);
        sp = null;
        frameTiming = null;
    }

    @Override
    public StereoDevice getDevice() {
        return context;
    }

    @Override
    public final int getDistortionBits() { return distortionBits; }

    @Override
    public final boolean usesSideBySideStereo() { return true; }

    @Override
    public final DimensionImmutable getSingleSurfaceSize() { return singleTextureSize; }

    @Override
    public final DimensionImmutable getTotalSurfaceSize() { return totalTextureSize; }

    @Override
    public final int getTextureCount() { return textureCount; }

    @Override
    public final int getTextureUnit() { return texUnit0.intValue(); }

    @Override
    public final boolean ppAvailable() { return 0 != distortionBits; }

    @Override
    public final void init(final GL gl) {
        if( StereoDevice.DEBUG ) {
            System.err.println(JoglVersion.getGLInfo(gl, null).toString());
        }
        if( null != sp ) {
            throw new IllegalStateException("Already initialized");
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
        final ShaderCode vp0 = ShaderCode.create(gl2es2, GL2ES2.GL_VERTEX_SHADER, OVRStereoDeviceRenderer.class, "shader",
                "shader/bin", vertexShaderBasename, true);
        final ShaderCode fp0 = ShaderCode.create(gl2es2, GL2ES2.GL_FRAGMENT_SHADER, OVRStereoDeviceRenderer.class, "shader",
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
            throw new OVRException("Couldn't locate "+texUnit0);
        }
        eyes[0].linkData(gl2es2, sp);
        eyes[1].linkData(gl2es2, sp);
        sp.useProgram(gl2es2, false);
    }

    @Override
    public final void dispose(final GL gl) {
        final GL2ES2 gl2es2 = gl.getGL2ES2();
        sp.useProgram(gl2es2, false);
        eyes[0].dispose(gl2es2);
        eyes[1].dispose(gl2es2);
        sp.destroy(gl2es2);
        frameTiming = null;
    }

    @Override
    public final Eye getEye(final int eyeNum) {
        return eyes[eyeNum];
    }

    @Override
    public final EyePose updateEyePose(final int eyeNum) {
        return eyes[eyeNum].updateEyePose(context.handle);
    }

    @Override
    public final void beginFrame(final GL gl) {
        frameTiming = OVR.ovrHmd_BeginFrameTiming(context.handle, 0);
    }

    @Override
    public final void endFrame(final GL gl) {
        if( null == frameTiming ) {
            throw new IllegalStateException("beginFrame not called");
        }
        OVR.ovrHmd_EndFrameTiming(context.handle);
        frameTiming = null;
    }

    @Override
    public final void ppBegin(final GL gl) {
        if( null == sp ) {
            throw new IllegalStateException("Not initialized");
        }
        if( null == frameTiming ) {
            throw new IllegalStateException("beginFrame not called");
        }
        if( StereoUtil.usesTimewarpDistortion(distortionBits) ) {
            OVR.ovr_WaitTillTime(frameTiming.getTimewarpPointSeconds());
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
        final OVREye eye = eyes[eyeNum];
        if( StereoUtil.usesTimewarpDistortion(distortionBits) ) {
            eye.updateTimewarp(context.handle, eye.ovrEyePose, mat4Tmp1, mat4Tmp2);
        }
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
