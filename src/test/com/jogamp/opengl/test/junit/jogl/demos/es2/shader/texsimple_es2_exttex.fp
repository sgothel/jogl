//Copyright 2010 JogAmp Community. All rights reserved.

#version 100

#extension GL_OES_EGL_image_external : require

precision mediump float;

uniform  samplerExternalOES  mgl_ActiveTexture;

#include texsimple_xxx.fp
