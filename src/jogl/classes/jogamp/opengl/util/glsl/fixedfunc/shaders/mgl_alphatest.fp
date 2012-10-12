
void alphaTest(inout vec4 color) {
    if( MGL_GREATER == mgl_AlphaTestFunc ) {
        if ( color.a <= mgl_AlphaTestRef ) {
            DISCARD(color);
        }
    } else if( MGL_LESS == mgl_AlphaTestFunc ) {
        if ( color.a >= mgl_AlphaTestRef ) {
            DISCARD(color);
        }
    } else if( MGL_LEQUAL == mgl_AlphaTestFunc ) {
        if ( color.a > mgl_AlphaTestRef ) {
            DISCARD(color);
        }
    } else if( MGL_GEQUAL == mgl_AlphaTestFunc ) {
        if ( color.a < mgl_AlphaTestRef ) {
            DISCARD(color);
        }
    } else if( MGL_EQUAL == mgl_AlphaTestFunc ) {
        if ( abs( color.a - mgl_AlphaTestRef ) > EPSILON ) {
            DISCARD(color);
        }
    } else if( MGL_NOTEQUAL == mgl_AlphaTestFunc ) {
        if ( abs( color.a - mgl_AlphaTestRef ) <= EPSILON ) {
            DISCARD(color);
        }
    } else if( MGL_NEVER == mgl_AlphaTestFunc ) {
        DISCARD(color);
    } /* else if( MGL_ALWAYS == mgl_AlphaTestFunc ) {
      // NOP
    } */
}

