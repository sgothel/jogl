Unimplemented functionality
    - tesselation and callbacks
    - trimming
    - setting NURBS properties (-> sampling etc.)
Differences from C++ source
    - no pooling
    - pointers to arrays are replaced by CArrayOf... classes and their methods
Unimplemented or incomplete "calltree top" methods (according to glu.def in Mesa 6.5)
    gluBeginTrim
    gluDeleteNurbsRenderer - won't be needed
    gluEndTrim
    gluGetNurbsProperty
    gluLoadSamplingMatrices
    gluNurbsCallback
    gluNurbsCallbackData
    gluNurbsCallbackDataEXT
    gluNurbsCurve - TODO type switch
    gluNurbsProperty
    gluPwlCurve
    gluQuadricCallback - not a NURBS method
As of files
    - Arc[ST]dirSorter.java - unimplemented (part of tesselation)
    - Backend.java:194 - wireframe quads - part of tesselation/callback
    - Curve.java:141-204 - culling
    - DisplayList.java:57 - append to DL - not sure whether it will be needed
    - GLUnurbs.java    :443,484 - error values
            :445 - trimming
            :512 - error handling (callback)
            :530 - loadGLmatrices
            :786 - nuid - nurbs object id - won't be needed I think
            :803 - end trim
    - GLUwNURBS.java:68,176 - NUBRS properties
    - Knotspec.java    :371 - copying in general case (more than 4 coords)
            :517 - copying with more than 4 coords
            :556 - pt_oo_sum default
    - Knotvector.java:165 - show method (probably debugging)
    - Mapdesc.java    :354 - get property
            :435 - xFormMat - change param cp to CArrayOfFloats; probably sampling functionality
    - Maplist.java:68 - clear ?
    - OpenGLCurveEvaluator.java    :132 - tess./callback code
                    :168 - mapgrid1f
                    :190 - tess./callback code (output triangles)
    - OpenGLSurfaceEvaluator.java    :77 . tess./callback code
                    :81 - glGetIntegerValue
                    :114 - tess./callback code
                    :117 - Level of detail
                    :144,161,201 - tess./callback code - output triangles
    - Patch.java:55 - constructor stuff ?
    - Patchlist.java:55 - constructor stuff ?
            :97 - cull check
            :105 - step size
            :115 - need of sampling subdivision
            :126 - need of subdivision
            :137 - need of non sampling subd.
            :146 - bbox (??)
    -Quilt.java    :254 - culling
            :282 - rates
    -Subdivider.java - all TODOs - it's stuff about trimming probably
             :545 - jumpbuffer - not sure purpose it exactly served in original source
