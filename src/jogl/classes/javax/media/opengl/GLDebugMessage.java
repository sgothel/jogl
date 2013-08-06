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
package javax.media.opengl;

import com.jogamp.common.os.Platform;

public class GLDebugMessage {
    final GLContext source;
    final long when;    
    final int dbgSource;
    final int dbgType;
    final int dbgId;
    final int dbgSeverity;
    final String dbgMsg;
    
    /**
     * @param source The source of the event
     * @param when The time of the event
     * @param dbgSource The ARB source
     * @param dbgType The ARB type
     * @param dbgId The ARB id
     * @param dbgSeverity The ARB severity level
     * @param dbgMsg The debug message
     */
    public GLDebugMessage(GLContext source, long when, int dbgSource, int dbgType, int dbgId, int dbgSeverity, String dbgMsg) {
        this.source = source;
        this.when = when;
        this.dbgSource = dbgSource;
        this.dbgType = dbgType;
        this.dbgId = dbgId;
        this.dbgSeverity = dbgSeverity;
        this.dbgMsg = dbgMsg;
    }
    
    /**
     * 
     * @param source
     * @param when
     * @param dbgId
     * @param amdDbgCategory
     * @param dbgSeverity AMD severity level equals ARB severity level (value and semantic)
     * @param dbgMsg
     * @return
     */
    public static GLDebugMessage translateAMDEvent(GLContext source, long when, int dbgId, int amdDbgCategory, int dbgSeverity, String dbgMsg) {
        int dbgSource, dbgType;
        
        // AMD category == ARB source/type
        switch(amdDbgCategory) {
            case GL2GL3.GL_DEBUG_CATEGORY_API_ERROR_AMD: 
                dbgSource = GL2GL3.GL_DEBUG_SOURCE_API;
                dbgType = GL2GL3.GL_DEBUG_TYPE_ERROR; 
                break;

            //
            // def source / other type
            //
                
            case GL2GL3.GL_DEBUG_CATEGORY_WINDOW_SYSTEM_AMD: 
                dbgSource = GL2GL3.GL_DEBUG_SOURCE_WINDOW_SYSTEM;
                dbgType = GL2GL3.GL_DEBUG_TYPE_OTHER; 
                break;
                
            case GL2GL3.GL_DEBUG_CATEGORY_SHADER_COMPILER_AMD:
                dbgSource = GL2GL3.GL_DEBUG_SOURCE_SHADER_COMPILER;
                dbgType = GL2GL3.GL_DEBUG_TYPE_OTHER; 
                break;
                
            case GL2GL3.GL_DEBUG_CATEGORY_APPLICATION_AMD:
                dbgSource = GL2GL3.GL_DEBUG_SOURCE_APPLICATION;
                dbgType = GL2GL3.GL_DEBUG_TYPE_OTHER;
                break;
                
                
            //
            // other source / def type
            //
                
            case GL2GL3.GL_DEBUG_CATEGORY_DEPRECATION_AMD:
                dbgSource = GL2GL3.GL_DEBUG_SOURCE_OTHER;
                dbgType = GL2GL3.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR; 
                break;
                
            case GL2GL3.GL_DEBUG_CATEGORY_UNDEFINED_BEHAVIOR_AMD:
                dbgSource = GL2GL3.GL_DEBUG_SOURCE_OTHER;
                dbgType = GL2GL3.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR; 
                break;
                
            case GL2GL3.GL_DEBUG_CATEGORY_PERFORMANCE_AMD:
                dbgSource = GL2GL3.GL_DEBUG_SOURCE_OTHER;
                dbgType = GL2GL3.GL_DEBUG_TYPE_PERFORMANCE; 
                break;
                
            case GL2GL3.GL_DEBUG_CATEGORY_OTHER_AMD: 
            default:
                dbgSource = GL2GL3.GL_DEBUG_SOURCE_OTHER;
                dbgType = GL2GL3.GL_DEBUG_TYPE_OTHER;
        }
        
        return new GLDebugMessage(source, when, dbgSource, dbgType, dbgId, dbgSeverity, dbgMsg);        
    }

    public static int translateARB2AMDCategory(int dbgSource, int dbgType) {
        switch (dbgSource) {
            case GL2GL3.GL_DEBUG_SOURCE_WINDOW_SYSTEM:
                return GL2GL3.GL_DEBUG_CATEGORY_WINDOW_SYSTEM_AMD; 
                
            case GL2GL3.GL_DEBUG_SOURCE_SHADER_COMPILER:
                return GL2GL3.GL_DEBUG_CATEGORY_SHADER_COMPILER_AMD;
                
            case GL2GL3.GL_DEBUG_SOURCE_APPLICATION:
                return GL2GL3.GL_DEBUG_CATEGORY_APPLICATION_AMD;
        }
        
        switch(dbgType) {
            case GL2GL3.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR:
                return GL2GL3.GL_DEBUG_CATEGORY_DEPRECATION_AMD;
                
            case GL2GL3.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR:
                return GL2GL3.GL_DEBUG_CATEGORY_UNDEFINED_BEHAVIOR_AMD;
                
            case GL2GL3.GL_DEBUG_TYPE_PERFORMANCE: 
                return GL2GL3.GL_DEBUG_CATEGORY_PERFORMANCE_AMD;
        }
                
        return GL2GL3.GL_DEBUG_CATEGORY_OTHER_AMD;        
    }
    
    public GLContext getSource() {
        return source;
    }

    public long getWhen() {
        return when;
    }
    
    public int getDbgSource() {
        return dbgSource;
    }

    public int getDbgType() {
        return dbgType;
    }

    public int getDbgId() {
        return dbgId;
    }

    public int getDbgSeverity() {
        return dbgSeverity;
    }

    public String getDbgMsg() {
        return dbgMsg;
    }
    
    public StringBuilder toString(StringBuilder sb) {
        final String crtab = Platform.getNewline()+"\t";        
        if(null==sb) {
            sb = new StringBuilder();
        }        
        sb.append("GLDebugEvent[ id ");
        toHexString(sb, dbgId)        
        .append(crtab).append("type ").append(getDbgTypeString(dbgType))
        .append(crtab).append("severity ").append(getDbgSeverityString(dbgSeverity))
        .append(crtab).append("source ").append(getDbgSourceString(dbgSource))
        .append(crtab).append("msg ").append(dbgMsg)
        .append(crtab).append("when ").append(when);
        if(null != source) {
            sb.append(crtab).append("source ").append(source.getGLVersion()).append(" - hash 0x").append(Integer.toHexString(source.hashCode()));
        }
        sb.append("]");
        return sb;        
    }

    public String toString() {
        return toString(null).toString();
    }
        
    public static String getDbgSourceString(int dbgSource) {
        switch(dbgSource) {
            case GL2GL3.GL_DEBUG_SOURCE_API: return "GL API";
            case GL2GL3.GL_DEBUG_SOURCE_SHADER_COMPILER: return "GLSL or extension compiler";                                          
            case GL2GL3.GL_DEBUG_SOURCE_WINDOW_SYSTEM: return "Native Windowing binding";                                          
            case GL2GL3.GL_DEBUG_SOURCE_THIRD_PARTY: return "Third party";
            case GL2GL3.GL_DEBUG_SOURCE_APPLICATION: return "Application";
            case GL2GL3.GL_DEBUG_SOURCE_OTHER: return "generic";
            default: return "Unknown (" + toHexString(dbgSource) + ")";
        }
    }
    
    public static String getDbgTypeString(int dbgType) {
        switch(dbgType) {
            case GL2GL3.GL_DEBUG_TYPE_ERROR: return "Error";
            case GL2GL3.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR: return "Warning: marked for deprecation";                                            
            case GL2GL3.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR: return "Warning: undefined behavior";
            case GL2GL3.GL_DEBUG_TYPE_PERFORMANCE: return "Warning: implementation dependent performance";   
            case GL2GL3.GL_DEBUG_TYPE_PORTABILITY: return "Warning: vendor-specific extension use";   
            case GL2GL3.GL_DEBUG_TYPE_OTHER: return "Warning: generic";            
            default: return "Unknown (" + toHexString(dbgType) + ")";
        }
    }
    
    public static String getDbgSeverityString(int dbgSeverity) {
        switch(dbgSeverity) {
            case GL2GL3.GL_DEBUG_SEVERITY_HIGH: return "High: dangerous undefined behavior";                                          
            case GL2GL3.GL_DEBUG_SEVERITY_MEDIUM: return "Medium: Severe performance/deprecation/other warnings";    
            case GL2GL3.GL_DEBUG_SEVERITY_LOW: return "Low: Performance warnings (redundancy/undefined)";        
            default: return "Unknown (" + toHexString(dbgSeverity) + ")";
        }
    }
    
    public static StringBuilder toHexString(StringBuilder sb, int i) {
        if(null==sb) {
            sb = new StringBuilder();
        }
        return sb.append("0x").append(Integer.toHexString(i));
    }
    public static String toHexString(int i) {
        return "0x"+Integer.toHexString(i);
    }    
    
}
