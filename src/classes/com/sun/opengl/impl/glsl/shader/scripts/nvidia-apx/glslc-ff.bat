REM 
REM You have to call it from the 'shader' directory, e.g.:
REM   scripts\nvidia-apx\glslc-ff.bat 
REM
IF !"%JOGLDIR%"==""! GOTO YESPATH
set JOGLDIR=..
:YESPATH
   
java -cp %JOGLDIR%\jogl.core.jar;%JOGLDIR%\jogl.gles2.jar;%JOGLDIR%\jogl.fixed.jar;%JOGLDIR%\jogl.sdk.jar javax.media.opengl.sdk.glsl.CompileShaderNVidia redsquare.vp redsquare.fp
