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

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLArrayData;
import javax.media.opengl.GLException;
import javax.media.opengl.GLUniformData;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.os.Platform;
import com.jogamp.oculusvr.OVR;
import com.jogamp.oculusvr.OVRException;
import com.jogamp.oculusvr.OvrHmdContext;
import com.jogamp.oculusvr.ovrDistortionMesh;
import com.jogamp.oculusvr.ovrDistortionVertex;
import com.jogamp.oculusvr.ovrEyeRenderDesc;
import com.jogamp.oculusvr.ovrFovPort;
import com.jogamp.oculusvr.ovrMatrix4f;
import com.jogamp.oculusvr.ovrPosef;
import com.jogamp.oculusvr.ovrRecti;
import com.jogamp.oculusvr.ovrSizei;
import com.jogamp.oculusvr.ovrVector2f;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.VectorUtil;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;

/**
 * OculusVR Distortion Data and OpenGL Renderer Utility
 */
public class OVRDistortion {
    public static final float[] VEC3_UP = { 0f, 1f, 0f };
    public static final float[] VEC3_FORWARD = { 0f, 0f, -1f };

    private static final String shaderPrefix01 = "dist01";
    private static final String shaderTimewarpSuffix = "_timewarp";
    private static final String shaderChromaSuffix = "_chroma";
    private static final String shaderPlainSuffix = "_plain";

    public static boolean useTimewarp(final int distortionCaps) { return 0 != ( distortionCaps & OVR.ovrDistortionCap_TimeWarp ) ; }
    public static boolean useChromatic(final int distortionCaps) { return 0 != ( distortionCaps & OVR.ovrDistortionCap_Chromatic ) ; }
    public static boolean useVignette(final int distortionCaps) { return 0 != ( distortionCaps & OVR.ovrDistortionCap_Vignette ) ; }

    public static class EyeData {
        public final int eyeName;
        public final int distortionCaps;
        public final int vertexCount;
        public final int indexCount;
        public final int[/*4*/] viewport;
        public final float[/*3*/] viewAdjust;

        public final GLUniformData eyeToSourceUVScale;
        public final GLUniformData eyeToSourceUVOffset;
        public final GLUniformData eyeRotationStart;
        public final GLUniformData eyeRotationEnd;

        /** 2+2+2+2+2: { vec2 position, vec2 color, vec2 texCoordR, vec2 texCoordG, vec2 texCoordB } */
        public final GLArrayDataServer iVBO;
        public final GLArrayData vboPos, vboParams, vboTexCoordsR, vboTexCoordsG, vboTexCoordsB;
        public final GLArrayDataServer indices;

        public final ovrEyeRenderDesc eyeRenderDesc;
        public final ovrFovPort eyeRenderFov;

        public ovrPosef eyeRenderPose;
        public final Quaternion eyeRenderPoseOrientation;
        public final float[] eyeRenderPosePosition;

        public final boolean useTimewarp() { return OVRDistortion.useTimewarp(distortionCaps); }
        public final boolean useChromatic() { return OVRDistortion.useChromatic(distortionCaps); }
        public final boolean useVignette() { return OVRDistortion.useVignette(distortionCaps); }

        private EyeData(final OvrHmdContext hmdCtx, final int distortionCaps,
                        final ovrEyeRenderDesc eyeRenderDesc, final ovrSizei ovrTextureSize, final int[] eyeRenderViewport) {
            this.eyeName = eyeRenderDesc.getEye();
            this.distortionCaps = distortionCaps;
            viewport = new int[4];
            viewAdjust = new float[3];
            System.arraycopy(eyeRenderViewport, 0, viewport, 0, 4);

            final FloatBuffer fstash = Buffers.newDirectFloatBuffer(2+2+16+26);

            eyeToSourceUVScale = new GLUniformData("ovr_EyeToSourceUVScale", 2, Buffers.slice2Float(fstash, 0, 2));
            eyeToSourceUVOffset = new GLUniformData("ovr_EyeToSourceUVOffset", 2, Buffers.slice2Float(fstash, 2, 2));

            if( useTimewarp() ) {
                eyeRotationStart = new GLUniformData("ovr_EyeRotationStart", 4, 4, Buffers.slice2Float(fstash, 4, 16));
                eyeRotationEnd = new GLUniformData("ovr_EyeRotationEnd", 4, 4, Buffers.slice2Float(fstash, 20, 16));
            } else {
                eyeRotationStart = null;
                eyeRotationEnd = null;
            }

            this.eyeRenderDesc = eyeRenderDesc;
            this.eyeRenderFov = eyeRenderDesc.getFov();

            this.eyeRenderPoseOrientation = new Quaternion();
            this.eyeRenderPosePosition = new float[3];


            final ovrDistortionMesh meshData = ovrDistortionMesh.create();
            final ovrFovPort fov = eyeRenderDesc.getFov();

            if( !OVR.ovrHmd_CreateDistortionMesh(hmdCtx, eyeName, fov, distortionCaps, meshData) ) {
                throw new OVRException("Failed to create meshData for eye "+eyeName+" and "+OVRUtil.toString(fov));
            }
            vertexCount = meshData.getVertexCount();
            indexCount = meshData.getIndexCount();

            /** 2+2+2+2+2: { vec2 position, vec2 color, vec2 texCoordR, vec2 texCoordG, vec2 texCoordB } */
            final boolean useChromatic = useChromatic();
            final boolean useVignette = useVignette();
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
            indices = GLArrayDataServer.createData(1, GL2ES2.GL_SHORT, indexCount, GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);
            OVRUtil.copyVec3fToFloat(eyeRenderDesc.getViewAdjust(), viewAdjust);

            // Setup: eyeToSourceUVScale, eyeToSourceUVOffset
            {
                final ovrVector2f[] uvScaleOffsetOut = new ovrVector2f[2];
                uvScaleOffsetOut[0] = ovrVector2f.create(); // FIXME: remove ctor / double check
                uvScaleOffsetOut[1] = ovrVector2f.create();

                final ovrRecti ovrEyeRenderViewport = OVRUtil.createOVRRecti(eyeRenderViewport);
                OVR.ovrHmd_GetRenderScaleAndOffset(fov, ovrTextureSize, ovrEyeRenderViewport, uvScaleOffsetOut);
                if( OVRUtil.DEBUG ) {
                    System.err.println("XXX."+eyeName+": fov "+OVRUtil.toString(fov));
                    System.err.println("XXX."+eyeName+": uvScale "+OVRUtil.toString(uvScaleOffsetOut[0]));
                    System.err.println("XXX."+eyeName+": uvOffset "+OVRUtil.toString(uvScaleOffsetOut[0]));
                    System.err.println("XXX."+eyeName+": textureSize "+OVRUtil.toString(ovrTextureSize));
                    System.err.println("XXX."+eyeName+": viewport "+OVRUtil.toString(ovrEyeRenderViewport));
                }
                final FloatBuffer eyeToSourceUVScaleFB = eyeToSourceUVScale.floatBufferValue();
                eyeToSourceUVScaleFB.put(0, uvScaleOffsetOut[0].getX());
                eyeToSourceUVScaleFB.put(1, uvScaleOffsetOut[0].getY());
                final FloatBuffer eyeToSourceUVOffsetFB = eyeToSourceUVOffset.floatBufferValue();
                eyeToSourceUVOffsetFB.put(0, uvScaleOffsetOut[1].getX());
                eyeToSourceUVOffsetFB.put(1, uvScaleOffsetOut[1].getY());
            }

            /** 2+2+2+2+2: { vec2 position, vec2 color, vec2 texCoordR, vec2 texCoordG, vec2 texCoordB } */
            final FloatBuffer iVBOFB = (FloatBuffer)iVBO.getBuffer();
            final ovrDistortionVertex[] ovRes = new ovrDistortionVertex[1];
            ovRes[0] = ovrDistortionVertex.create(); // FIXME: remove ctor / double check

            for ( int vertNum = 0; vertNum < vertexCount; vertNum++ ) {
                final ovrDistortionVertex ov = meshData.getPVertexData(vertNum, ovRes)[0];
                ovrVector2f v;

                // pos
                v = ov.getPos();
                iVBOFB.put(v.getX());
                iVBOFB.put(v.getY());

                // params
                if( useVignette ) {
                    iVBOFB.put(ov.getVignetteFactor());
                } else {
                    iVBOFB.put(1.0f);
                }
                iVBOFB.put(ov.getTimeWarpFactor());

                // texCoordR
                v = ov.getTexR();
                iVBOFB.put(v.getX());
                iVBOFB.put(v.getY());

                if( useChromatic ) {
                    // texCoordG
                    v = ov.getTexG();
                    iVBOFB.put(v.getX());
                    iVBOFB.put(v.getY());

                    // texCoordB
                    v = ov.getTexB();
                    iVBOFB.put(v.getX());
                    iVBOFB.put(v.getY());
                }
            }
            if( OVRUtil.DEBUG ) {
                System.err.println("XXX."+eyeName+": iVBO "+iVBO);
            }
            {
                final ShortBuffer in = meshData.getPIndexData();
                final ShortBuffer out = (ShortBuffer) indices.getBuffer();
                out.put(in);
            }
            if( OVRUtil.DEBUG ) {
                System.err.println("XXX."+eyeName+": idx "+indices);
                System.err.println("XXX."+eyeName+": distEye "+this);
            }
            OVR.ovrHmd_DestroyDistortionMesh(meshData);
        }

        private void linkData(final GL2ES2 gl, final ShaderProgram sp) {
            if( 0 > vboPos.setLocation(gl, sp.program()) ) {
                throw new OVRException("Couldn't locate "+vboPos);
            }
            if( 0 > vboParams.setLocation(gl, sp.program()) ) {
                throw new OVRException("Couldn't locate "+vboParams);
            }
            if( 0 > vboTexCoordsR.setLocation(gl, sp.program()) ) {
                throw new OVRException("Couldn't locate "+vboTexCoordsR);
            }
            if( useChromatic() ) {
                if( 0 > vboTexCoordsG.setLocation(gl, sp.program()) ) {
                    throw new OVRException("Couldn't locate "+vboTexCoordsG);
                }
                if( 0 > vboTexCoordsB.setLocation(gl, sp.program()) ) {
                    throw new OVRException("Couldn't locate "+vboTexCoordsB);
                }
            }
            if( 0 > eyeToSourceUVScale.setLocation(gl, sp.program()) ) {
                throw new OVRException("Couldn't locate "+eyeToSourceUVScale);
            }
            if( 0 > eyeToSourceUVOffset.setLocation(gl, sp.program()) ) {
                throw new OVRException("Couldn't locate "+eyeToSourceUVOffset);
            }
            if( useTimewarp() ) {
                if( 0 > eyeRotationStart.setLocation(gl, sp.program()) ) {
                    throw new OVRException("Couldn't locate "+eyeRotationStart);
                }
                if( 0 > eyeRotationEnd.setLocation(gl, sp.program()) ) {
                    throw new OVRException("Couldn't locate "+eyeRotationEnd);
                }
            }
            iVBO.seal(gl, true);
            iVBO.enableBuffer(gl, false);
            indices.seal(gl, true);
            indices.enableBuffer(gl, false);
        }

        public void dispose(final GL2ES2 gl) {
            iVBO.destroy(gl);
            indices.destroy(gl);
        }
        public void enableVBO(final GL2ES2 gl, final boolean enable) {
            iVBO.enableBuffer(gl, enable);
            indices.bindBuffer(gl, enable); // keeps VBO binding if enable:=true
        }

        public void updateUniform(final GL2ES2 gl, final ShaderProgram sp) {
            gl.glUniform(eyeToSourceUVScale);
            gl.glUniform(eyeToSourceUVOffset);
            if( useTimewarp() ) {
                gl.glUniform(eyeRotationStart);
                gl.glUniform(eyeRotationEnd);
            }
        }

        public void updateTimewarp(final OvrHmdContext hmdCtx, final ovrPosef eyeRenderPose, final float[] mat4Tmp1, final float[] mat4Tmp2) {
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
         * Updates {@link #eyeRenderPose} and it's extracted
         * {@link #eyeRenderPoseOrientation} and {@link #eyeRenderPosePosition}.
         * @param hmdCtx used get the {@link #eyeRenderPose} via {@link OVR#ovrHmd_GetEyePose(OvrHmdContext, int)}
         */
        public void updateEyePose(final OvrHmdContext hmdCtx) {
            eyeRenderPose = OVR.ovrHmd_GetEyePose(hmdCtx, eyeName);
            OVRUtil.copyToQuaternion(eyeRenderPose.getOrientation(), eyeRenderPoseOrientation);
            OVRUtil.copyVec3fToFloat(eyeRenderPose.getPosition(), eyeRenderPosePosition);
        }

        @Override
        public String toString() {
            return "Eye["+eyeName+", viewport "+viewport[0]+"/"+viewport[1]+" "+viewport[2]+"x"+viewport[3]+
                        " viewAdjust["+viewAdjust[0]+", "+viewAdjust[1]+", "+viewAdjust[2]+
                        "], vertices "+vertexCount+", indices "+indexCount+
                        ", uvScale["+eyeToSourceUVScale.floatBufferValue().get(0)+", "+eyeToSourceUVScale.floatBufferValue().get(1)+
                        "], uvOffset["+eyeToSourceUVOffset.floatBufferValue().get(0)+", "+eyeToSourceUVOffset.floatBufferValue().get(1)+
                        "], desc"+OVRUtil.toString(eyeRenderDesc)+"]";
        }
    }

    public final OvrHmdContext hmdCtx;
    public final EyeData[] eyes;
    public final int distortionCaps;
    public final int[/*2*/] textureSize;
    public final GLUniformData texUnit0;
    public final boolean usesDistMesh;

    private final float[] mat4Tmp1 = new float[16];
    private final float[] mat4Tmp2 = new float[16];
    private final float[] vec3Tmp1 = new float[3];
    private final float[] vec3Tmp2 = new float[3];
    private final float[] vec3Tmp3 = new float[3];

    private ShaderProgram sp;

    @Override
    public String toString() {
        return "OVRDist[caps 0x"+Integer.toHexString(distortionCaps)+", "+
                       ", tex "+textureSize[0]+"x"+textureSize[1]+
                       ", vignette "+useVignette()+", chromatic "+useChromatic()+", timewarp "+useTimewarp()+
                       ", "+Platform.NEWLINE+"  "+eyes[0]+", "+Platform.NEWLINE+"  "+eyes[1]+"]";
    }

    public static OVRDistortion create(final OvrHmdContext hmdCtx, final boolean sbsSingleTexture,
                                       final ovrFovPort[] eyeFov, final float pixelsPerDisplayPixel, final int distortionCaps) {
        final ovrEyeRenderDesc[] eyeRenderDesc = new ovrEyeRenderDesc[2];
        eyeRenderDesc[0] = OVR.ovrHmd_GetRenderDesc(hmdCtx, OVR.ovrEye_Left, eyeFov[0]);
        eyeRenderDesc[1] = OVR.ovrHmd_GetRenderDesc(hmdCtx, OVR.ovrEye_Right, eyeFov[1]);
        if( OVRUtil.DEBUG ) {
            System.err.println("XXX: eyeRenderDesc[0] "+OVRUtil.toString(eyeRenderDesc[0]));
            System.err.println("XXX: eyeRenderDesc[1] "+OVRUtil.toString(eyeRenderDesc[1]));
        }

        final ovrSizei recommenedTex0Size = OVR.ovrHmd_GetFovTextureSize(hmdCtx, OVR.ovrEye_Left,  eyeRenderDesc[0].getFov(), pixelsPerDisplayPixel);
        final ovrSizei recommenedTex1Size = OVR.ovrHmd_GetFovTextureSize(hmdCtx, OVR.ovrEye_Right, eyeRenderDesc[1].getFov(), pixelsPerDisplayPixel);
        if( OVRUtil.DEBUG ) {
            System.err.println("XXX: recommenedTex0Size "+OVRUtil.toString(recommenedTex0Size));
            System.err.println("XXX: recommenedTex1Size "+OVRUtil.toString(recommenedTex1Size));
        }
        final int[] textureSize = new int[2];
        if( sbsSingleTexture ) {
            textureSize[0] = recommenedTex0Size.getW() + recommenedTex1Size.getW();
        } else {
            textureSize[0] = Math.max(recommenedTex0Size.getW(), recommenedTex1Size.getW());
        }
        textureSize[1] = Math.max(recommenedTex0Size.getH(), recommenedTex1Size.getH());
        if( OVRUtil.DEBUG ) {
            System.err.println("XXX: textureSize "+textureSize[0]+"x"+textureSize[1]);
        }

        final int[][] eyeRenderViewports = new int[2][4];
        if( sbsSingleTexture ) {
            eyeRenderViewports[0][0] = 0;
            eyeRenderViewports[0][1] = 0;
            eyeRenderViewports[0][2] = textureSize[0] / 2;
            eyeRenderViewports[0][3] = textureSize[1];
            eyeRenderViewports[1][0] = (textureSize[0] + 1) / 2;
            eyeRenderViewports[1][1] = 0;
            eyeRenderViewports[1][2] = textureSize[0] / 2;
            eyeRenderViewports[1][3] = textureSize[1];
        } else {
            eyeRenderViewports[0][0] = 0;
            eyeRenderViewports[0][1] = 0;
            eyeRenderViewports[0][2] = textureSize[0];
            eyeRenderViewports[0][3] = textureSize[1];
            eyeRenderViewports[1][0] = 0;
            eyeRenderViewports[1][1] = 0;
            eyeRenderViewports[1][2] = textureSize[0];
            eyeRenderViewports[1][3] = textureSize[1];
        }
        return new OVRDistortion(hmdCtx, sbsSingleTexture, eyeRenderDesc, textureSize, eyeRenderViewports, distortionCaps, 0);
    }

    public OVRDistortion(final OvrHmdContext hmdCtx, final boolean sbsSingleTexture, final ovrEyeRenderDesc[] eyeRenderDescs,
                         final int[] textureSize, final int[][] eyeRenderViewports,
                         final int distortionCaps, final int textureUnit) {
        this.hmdCtx = hmdCtx;
        this.eyes = new EyeData[2];
        this.distortionCaps = distortionCaps;
        this.textureSize = new int[2];
        System.arraycopy(textureSize, 0, this.textureSize, 0, 2);

        texUnit0 = new GLUniformData("ovr_Texture0", textureUnit);
        usesDistMesh = true;

        final ovrSizei ovrTextureSize = OVRUtil.createOVRSizei(textureSize);
        eyes[0] = new EyeData(hmdCtx, distortionCaps, eyeRenderDescs[0], ovrTextureSize, eyeRenderViewports[0]);
        eyes[1] = new EyeData(hmdCtx, distortionCaps, eyeRenderDescs[1], ovrTextureSize, eyeRenderViewports[1]);
        sp = null;
    }

    public final boolean useTimewarp() { return useTimewarp(distortionCaps); }
    public final boolean useChromatic() { return useChromatic(distortionCaps); }
    public final boolean useVignette() { return useVignette(distortionCaps); }

    public void updateTimewarp(final ovrPosef eyeRenderPose, final int eyeNum) {
        eyes[eyeNum].updateTimewarp(hmdCtx, eyeRenderPose, mat4Tmp1, mat4Tmp2);
    }
    public void updateTimewarp(final ovrPosef[] eyeRenderPoses) {
        eyes[0].updateTimewarp(hmdCtx, eyeRenderPoses[0], mat4Tmp1, mat4Tmp2);
        eyes[1].updateTimewarp(hmdCtx, eyeRenderPoses[1], mat4Tmp1, mat4Tmp2);
    }

    public void enableVBO(final GL2ES2 gl, final boolean enable, final int eyeNum) {
        if( null == sp ) {
            throw new IllegalStateException("Not initialized");
        }
        eyes[eyeNum].enableVBO(gl, enable);
    }

    public void updateUniforms(final GL2ES2 gl, final int eyeNum) {
        if( null == sp ) {
            throw new IllegalStateException("Not initialized");
        }
        gl.glUniform(texUnit0);
        eyes[eyeNum].updateUniform(gl, sp);
    }

    public final ShaderProgram getShaderProgram() { return sp; }

    public void init(final GL2ES2 gl) {
        if( OVRUtil.DEBUG ) {
            System.err.println(JoglVersion.getGLInfo(gl, null).toString());
        }
        if( null != sp ) {
            throw new IllegalStateException("Already initialized");
        }
        final String vertexShaderBasename;
        final String fragmentShaderBasename;
        {
            final StringBuilder sb = new StringBuilder();
            sb.append(shaderPrefix01);
            if( !useChromatic() && !useTimewarp() ) {
                sb.append(shaderPlainSuffix);
            } else if( useChromatic() && !useTimewarp() ) {
                sb.append(shaderChromaSuffix);
            } else if( useTimewarp() ) {
                sb.append(shaderTimewarpSuffix);
                if( useChromatic() ) {
                    sb.append(shaderChromaSuffix);
                }
            }
            vertexShaderBasename = sb.toString();
            sb.setLength(0);
            sb.append(shaderPrefix01);
            if( useChromatic() ) {
                sb.append(shaderChromaSuffix);
            } else {
                sb.append(shaderPlainSuffix);
            }
            fragmentShaderBasename = sb.toString();
        }
        final ShaderCode vp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, OVRDistortion.class, "shader",
                "shader/bin", vertexShaderBasename, true);
        final ShaderCode fp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, OVRDistortion.class, "shader",
                "shader/bin", fragmentShaderBasename, true);
        vp0.defaultShaderCustomization(gl, true, true);
        fp0.defaultShaderCustomization(gl, true, true);

        sp = new ShaderProgram();
        sp.add(gl, vp0, System.err);
        sp.add(gl, fp0, System.err);
        if(!sp.link(gl, System.err)) {
            throw new GLException("could not link program: "+sp);
        }
        sp.useProgram(gl, true);
        if( 0 > texUnit0.setLocation(gl, sp.program()) ) {
            throw new OVRException("Couldn't locate "+texUnit0);
        }
        eyes[0].linkData(gl, sp);
        eyes[1].linkData(gl, sp);
        sp.useProgram(gl, false);
    }

    public void dispose(final GL2ES2 gl) {
        sp.useProgram(gl, false);
        eyes[0].dispose(gl);
        eyes[1].dispose(gl);
        sp.destroy(gl);
    }

    public void display(final GL2ES2 gl, final double timewarpPointSeconds) {
        if( null == sp ) {
            throw new IllegalStateException("Not initialized");
        }
        if( useTimewarp() ) {
            OVR.ovr_WaitTillTime(timewarpPointSeconds);
        }
        gl.glDisable(GL.GL_CULL_FACE);
        gl.glDisable(GL.GL_DEPTH_TEST);
        gl.glDisable(GL.GL_BLEND);

        if( !gl.isGLcore() ) {
            gl.glEnable(GL.GL_TEXTURE_2D);
        }

        sp.useProgram(gl, true);

        gl.glUniform(texUnit0);

        for(int eyeNum=0; eyeNum<2; eyeNum++) {
            final EyeData eye = eyes[eyeNum];
            if( useTimewarp() ) {
                eye.updateTimewarp(hmdCtx, eye.eyeRenderPose, mat4Tmp1, mat4Tmp2);
            }
            eye.updateUniform(gl, sp);
            eye.enableVBO(gl, true);
            if( usesDistMesh ) {
                gl.glDrawElements(GL.GL_TRIANGLES, eye.indexCount, GL2ES2.GL_UNSIGNED_SHORT, 0);
            } else {
                gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, eye.vertexCount);
            }
            eyes[eyeNum].enableVBO(gl, false);
        }

        sp.useProgram(gl, false);
    }

    public void displayOneEyePre(final GL2ES2 gl, final double timewarpPointSeconds) {
        if( null == sp ) {
            throw new IllegalStateException("Not initialized");
        }
        if( useTimewarp() ) {
            OVR.ovr_WaitTillTime(timewarpPointSeconds);
        }
        gl.glDisable(GL.GL_CULL_FACE);
        gl.glDisable(GL.GL_DEPTH_TEST);
        gl.glDisable(GL.GL_BLEND);

        if( !gl.isGLcore() ) {
            gl.glEnable(GL.GL_TEXTURE_2D);
        }

        sp.useProgram(gl, true);

        gl.glUniform(texUnit0);
    }

    public void displayOneEye(final GL2ES2 gl, final int eyeNum) {
        if( null == sp ) {
            throw new IllegalStateException("Not initialized");
        }
        final EyeData eye = eyes[eyeNum];
        if( useTimewarp() ) {
            eye.updateTimewarp(hmdCtx, eye.eyeRenderPose, mat4Tmp1, mat4Tmp2);
        }
        eye.updateUniform(gl, sp);
        eye.enableVBO(gl, true);
        if( usesDistMesh ) {
            gl.glDrawElements(GL.GL_TRIANGLES, eye.indexCount, GL2ES2.GL_UNSIGNED_SHORT, 0);
        } else {
            gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, eye.vertexCount);
        }
        eyes[eyeNum].enableVBO(gl, false);
    }

    public void displayOneEyePost(final GL2ES2 gl) {
        sp.useProgram(gl, false);
    }

    /**
     * Calculates the <i>Side By Side</i>, SBS, projection- and modelview matrix for one eye.
     * <p>
     * Method also issues {@link EyeData#updateEyePose(OvrHmdContext)}.
     * </p>
     * @param eyeNum eye denominator
     * @param eyePos float[3] eye postion vector
     * @param eyeYaw eye direction, i.e. {@link FloatUtil#PI} for 180 degrees
     * @param near frustum near value
     * @param far frustum far value
     * @param mat4Projection float[16] projection matrix result
     * @param mat4Modelview float[16] modelview matrix result
     */
    public void getSBSUpstreamPMV(final int eyeNum, final float[] eyePos, final float eyeYaw, final float near, final float far,
                                  final float[] mat4Projection, final float[] mat4Modelview) {
        final EyeData eyeDist = eyes[eyeNum];

        eyeDist.updateEyePose(hmdCtx);

        //
        // Projection
        //
        final ovrMatrix4f pm = OVR.ovrMatrix4f_Projection(eyeDist.eyeRenderFov, near, far, true /* rightHanded*/);
        /* final float[] mat4Projection = */ FloatUtil.transposeMatrix(pm.getM(0, mat4Tmp1), mat4Projection);

        //
        // Modelview
        //
        final Quaternion rollPitchYaw = new Quaternion();
        rollPitchYaw.rotateByAngleY(eyeYaw);
        final float[] shiftedEyePos = rollPitchYaw.rotateVector(vec3Tmp1, 0, eyeDist.eyeRenderPosePosition, 0);
        VectorUtil.addVec3(shiftedEyePos, shiftedEyePos, eyePos);

        rollPitchYaw.mult(eyeDist.eyeRenderPoseOrientation);
        final float[] up = rollPitchYaw.rotateVector(vec3Tmp2, 0, VEC3_UP, 0);
        final float[] forward = rollPitchYaw.rotateVector(vec3Tmp3, 0, VEC3_FORWARD, 0);
        final float[] center = VectorUtil.addVec3(forward, shiftedEyePos, forward);

        final float[] mLookAt = FloatUtil.makeLookAt(mat4Tmp2, 0, shiftedEyePos, 0, center, 0, up, 0, mat4Tmp1);
        final float[] mViewAdjust = FloatUtil.makeTranslation(mat4Modelview, true, eyeDist.viewAdjust[0], eyeDist.viewAdjust[1], eyeDist.viewAdjust[2]);

        /* mat4Modelview = */ FloatUtil.multMatrix(mViewAdjust, mLookAt);
    }


}
