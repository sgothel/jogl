#include <windows.h>
#include <stdlib.h>
#include <mmsystem.h>
#include <mmreg.h>
#include "com_sun_javafx_audio_windows_waveout_Mixer.h"

static HANDLE event = NULL;
static HWAVEOUT output = NULL;
// We use only two buffers to keep latency down
#define NUM_BUFFERS 2
//#define NUM_BUFFERS 4
// This is about 20 ms of data for WAVE_FORMAT_PCM:
// (44100 samples / sec) * (20 ms / 1000 ms) * (2 bytes / sample) * (2 channels)
//#define BUFFER_SIZE 3528

// This is about 50 ms of data for WAVE_FORMAT_PCM:
// (44100 samples / sec) * (50 ms / 1000 ms) * (2 bytes / sample) * (1 channel)
//#define BUFFER_SIZE 4410

// This is about 200 ms of data for WAVE_FORMAT_PCM:
// (44100 samples / sec) * (200 ms / 1000 ms) * (2 bytes / sample) * (1 channel)
//#define BUFFER_SIZE 17640

// This is about 200 ms of data for WAVE_FORMAT_PCM:
// (44100 samples / sec) * (200 ms / 1000 ms) * (2 bytes / sample) * (2 channel)
//#define BUFFER_SIZE 35280

// This is about 1000 ms of data for WAVE_FORMAT_PCM:
// (44100 samples / sec) * (1000 ms / 1000 ms) * (2 bytes / sample) * (1 channel)
//#define BUFFER_SIZE 88200

// This is about 50 ms of data for WAVE_FORMAT_PCM:
// (44100 samples / sec) * (50 ms / 1000 ms) * (2 bytes / sample) * (2 channels)
//#define BUFFER_SIZE 8820

// This is about 50 ms of data for WAVE_FORMAT_IEEE_FLOAT:
// (44100 samples / sec) * (50 ms / 1000 ms) * (4 bytes / sample) * (2 channels)
//#define BUFFER_SIZE 17640

// This is about 200 ms of data for WAVE_FORMAT_PCM:
// (11025 samples / sec) * (200 ms / 1000 ms) * (2 bytes / sample) * (2 channel)
#define BUFFER_SIZE 8820

//#define BUFFER_SIZE 8192
static WAVEHDR** buffers = NULL;

void CALLBACK playbackCallback(HWAVEOUT output,
                               UINT msg,
                               DWORD_PTR userData,
                               DWORD_PTR param1,
                               DWORD_PTR param2)
{
    if (msg == WOM_DONE) {
        WAVEHDR* hdr = (WAVEHDR*) param1;
        hdr->dwFlags |= WHDR_DONE;
        SetEvent(event);
    }
}

JNIEXPORT jboolean JNICALL Java_com_sun_javafx_audio_windows_waveout_Mixer_initializeWaveOut
  (JNIEnv *env, jclass unused, jlong eventObject)
{
    event = (HANDLE) eventObject;

    // Note the hard requirements on the RawSoundConverter's output format
    WAVEFORMATEX format;
    format.wFormatTag = WAVE_FORMAT_PCM;
    //    format.wFormatTag = WAVE_FORMAT_IEEE_FLOAT;
    format.nChannels = 2;
    //    format.nChannels = 1;
    //    format.nSamplesPerSec = 44100;
    format.nSamplesPerSec = 11025;
    format.wBitsPerSample = 16;
    //    format.wBitsPerSample = 32;
    format.nBlockAlign = format.nChannels * format.wBitsPerSample / 8;
    format.nAvgBytesPerSec = format.nBlockAlign * format.nSamplesPerSec;
    format.cbSize = 0;
    MMRESULT res = waveOutOpen(&output,
                               WAVE_MAPPER,
                               &format,
                               /* NULL, */ (DWORD_PTR) &playbackCallback,
                               NULL, // No user data right now
                               /* CALLBACK_NULL */ CALLBACK_FUNCTION);
    if (res != MMSYSERR_NOERROR) {
        return JNI_FALSE;
    }

    buffers = (WAVEHDR**) calloc(NUM_BUFFERS, sizeof(WAVEHDR));
    for (int i = 0; i < NUM_BUFFERS; i++) {
        char* data = (char*) calloc(BUFFER_SIZE, 1);
        WAVEHDR* hdr = (WAVEHDR*) calloc(1, sizeof(WAVEHDR));
        hdr->lpData = data;
        hdr->dwBufferLength = BUFFER_SIZE;
        hdr->dwFlags |= WHDR_DONE;
        buffers[i] = hdr;
    }

    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_sun_javafx_audio_windows_waveout_Mixer_shutdownWaveOut
  (JNIEnv *env, jclass unused)
{
    //    writeString("Pausing\n");
    waveOutPause(output);
    //    writeString("Resetting\n");
    waveOutReset(output);
    //    writeString("Closing output\n");
    waveOutClose(output);
}

JNIEXPORT jlong JNICALL Java_com_sun_javafx_audio_windows_waveout_Mixer_getNextMixerBuffer
  (JNIEnv *env, jclass unused)
{
    WAVEHDR* hdr = NULL;
    for (int i = 0; i < NUM_BUFFERS; i++) {
        if (buffers[i] != NULL && ((buffers[i]->dwFlags & WHDR_DONE) != 0)) {
            hdr = buffers[i];
            hdr->dwFlags &= ~WHDR_DONE;
            break;
        }
    }
    return (jlong) hdr;
}

JNIEXPORT jobject JNICALL Java_com_sun_javafx_audio_windows_waveout_Mixer_getMixerBufferData
  (JNIEnv *env, jclass unused, jlong mixerBuffer)
{
    WAVEHDR* hdr = (WAVEHDR*) mixerBuffer;
    return env->NewDirectByteBuffer(hdr->lpData, hdr->dwBufferLength);
}

JNIEXPORT jlong JNICALL Java_com_sun_javafx_audio_windows_waveout_Mixer_getMixerBufferDataAddress
  (JNIEnv *env, jclass unused, jlong mixerBuffer)
{
    WAVEHDR* hdr = (WAVEHDR*) mixerBuffer;
    return (jlong) hdr->lpData;
}

JNIEXPORT jint JNICALL Java_com_sun_javafx_audio_windows_waveout_Mixer_getMixerBufferDataCapacity
  (JNIEnv *env, jclass unused, jlong mixerBuffer)
{
    WAVEHDR* hdr = (WAVEHDR*) mixerBuffer;
    return (jint) hdr->dwBufferLength;
}

JNIEXPORT jboolean JNICALL Java_com_sun_javafx_audio_windows_waveout_Mixer_prepareMixerBuffer
  (JNIEnv *env, jclass unused, jlong mixerBuffer)
{
    MMRESULT res = waveOutPrepareHeader(output,
                                        (WAVEHDR*) mixerBuffer,
                                        sizeof(WAVEHDR));
    if (res == MMSYSERR_NOERROR) {
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_sun_javafx_audio_windows_waveout_Mixer_writeMixerBuffer
  (JNIEnv *env, jclass unused, jlong mixerBuffer)
{
    MMRESULT res = waveOutWrite(output,
                                (WAVEHDR*) mixerBuffer,
                                sizeof(WAVEHDR));
    if (res == MMSYSERR_NOERROR) {
        waveOutRestart(output);

        return JNI_TRUE;
    }
    return JNI_FALSE;
}

JNIEXPORT jlong JNICALL Java_com_sun_javafx_audio_windows_waveout_Mixer_CreateEvent
  (JNIEnv *env, jclass unused)
{
    return (jlong) CreateEvent(NULL, FALSE, TRUE, NULL);
}

JNIEXPORT jboolean JNICALL Java_com_sun_javafx_audio_windows_waveout_Mixer_WaitForSingleObject
  (JNIEnv *env, jclass unused, jlong eventObject)
{
    DWORD res = WaitForSingleObject((HANDLE) eventObject, INFINITE);
    if (res == WAIT_OBJECT_0) {
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_sun_javafx_audio_windows_waveout_Mixer_SetEvent
  (JNIEnv *env, jclass unused, jlong eventObject)
{
    SetEvent((HANDLE) eventObject);
}

JNIEXPORT void JNICALL Java_com_sun_javafx_audio_windows_waveout_Mixer_CloseHandle
  (JNIEnv *env, jclass unused, jlong eventObject)
{
    CloseHandle((HANDLE) eventObject);
}
