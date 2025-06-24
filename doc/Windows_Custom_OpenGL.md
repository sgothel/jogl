# Using a custom OpenGL Library under Windows

## Tested OpenGL Libraries
- [Mesa3D by pal1000](https://github.com/pal1000/mesa-dist-win/releases)

## Common
To allow using software rendering (not D3D) w/ Mesa3D:
- `set LIBGL_ALWAYS_SOFTWARE=true`

## AWT's Direct Draw Usage
To disable Direct Draw usage within AWT's Java2D:
- pass JVM argument `"-Dsun.java2d.noddraw=true"`

## AWT's OpenGL Usage
To disable OpenGL usage within AWT's Java2D:
- pass JVM argument `"-Dsun.java2d.opengl=false"`

This is required if using the zero deployment method,
i.e. avoiding having the JVM load a different OpenGL library.

## Systemwide Deployment
To using software rendering (not D3D) w/ Mesa3D and Java's AWT:
- `set LIBGL_ALWAYS_SOFTWARE=true`
- pass JVM argument `"-Dsun.java2d.noddraw=true"`

## Zero Deployment
To pick up the custom library from its random path and 
using software rendering (not D3D) w/ Mesa3D and Java's AWT:
- `set PATH=C:\Mesa3D\x64;%PATH%`
- `set LIBGL_ALWAYS_SOFTWARE=true`
- pass JVM argument `"-Dsun.java2d.noddraw=true" "-Dsun.java2d.opengl=false"`

See `AWT's OpenGL Usage` remarks above.

## Per Application Deployment
Same as `Zero Deployment`, but `PATH` setting can be skipped.
