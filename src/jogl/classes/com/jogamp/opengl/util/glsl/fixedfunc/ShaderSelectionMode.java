package com.jogamp.opengl.util.glsl.fixedfunc;

/**
 * Shader selection mode
 *
 * @see ShaderSelectionMode#AUTO
 * @see ShaderSelectionMode#COLOR
 * @see ShaderSelectionMode#COLOR_LIGHT_PER_VERTEX
 * @see ShaderSelectionMode#COLOR_TEXTURE
 * @see ShaderSelectionMode#COLOR_TEXTURE_LIGHT_PER_VERTEX
 */
public enum ShaderSelectionMode {
    /** Auto shader selection, based upon FFP states. */
    AUTO,
    /** Fixed shader selection: Simple color. */
    COLOR,
    /** Fixed shader selection: Multi-Textured color. 2 texture units. */
    COLOR_TEXTURE2,
    /** Fixed shader selection: Multi-Textured color. 4 texture units. */
    COLOR_TEXTURE4,
    /** Fixed shader selection: Multi-Textured color. 8 texture units. */
    COLOR_TEXTURE8,
    /** Fixed shader selection: Color with vertex-lighting. */
    COLOR_LIGHT_PER_VERTEX,
    /** Fixed shader selection: Multi-Textured color with vertex-lighting. 8 texture units.*/
    COLOR_TEXTURE8_LIGHT_PER_VERTEX
}