package jogamp.graph.curve.opengl.shader;

public class AttributeNames {
    /** The vertices index in an OGL object
     */
    public static final int VERTEX_ATTR_IDX = 0; // FIXME: AMD needs this to be location 0 ? hu ?
    public static final String VERTEX_ATTR_NAME = "gca_Vertices";

    /** The Texture Coord index in an OGL object
     */
    public static final int TEXCOORD_ATTR_IDX = 1;
    public static final String TEXCOORD_ATTR_NAME = "gca_TexCoords";
    
    /** The color index in an OGL object
     */
    public static final int COLOR_ATTR_IDX = 2;
    public static final String COLOR_ATTR_NAME = "gca_Colors";    
}
