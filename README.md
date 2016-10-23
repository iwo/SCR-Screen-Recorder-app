# SCR Screen Recorder
The most popular screen recording app for Android version prior to Lollipop. Requires root.

This app evolved from the quick-and-dirty prototype and was never properly rewritten/redesigned.
You won't find sophisticated architecture or beautiful code here.
You may find some interesting hacks and workarounds for Android limitations tho.

#### This app is no longer maintained.
Since this app got removed from Google Play (for manipulating SELinux settings)
I'm not able to support it. Lollipop introduced a MediaProjection API that
is used by a growing number of screen recorders that don't require root.

#### First full build takes DAYS!
SCR Screen Recorder heavily depends on Android private APIs and is rather complex to build.
Building whole app from source requires almost 1TB of disk space and takes couple *DAYS* for the initial build.
See my [blog post](http://www.iwobanas.com/2015/06/accessing-android-internal-apis-from-apps/) for more details.
If you wish to only modify the Java code you can download prebuilt native binaries from 
[here](http://scr-screen-recorder.com/native_binaries.zip).


## Code structure

SCR source code is divided into 3 repositories
* [SCR Screen Recorder app](https://github.com/iwo/SCR-Screen-Recorder-app) - app UI code in Java.
Built with Gradle/Android Studio. Requires native binaries created from the following two projects
or downloaded from [here](http://scr-screen-recorder.com/native_binaries.zip).
* [SCR Screen Recorder native](https://github.com/iwo/SCR-Screen-Recorder-native) - native 
commandline executable started with root access by SCR app. Depends on Android private APIs 
and requires AOSP to build. (see [Building native code](#building-native-code))
* [SCR audio driver](https://github.com/iwo/SCR-Audio-Driver) - Android HAL audio driver 
used to record audio output. Requires AOSP to build (see [Building native code](#building-native-code))

## Build variants
This project has two [build variants] configured in Gradle file.
* root - the original SCR app with internal audio recording and support for ICS+. Uses private Android APIs and requires root.
* nonRoot - (SCR 5+) the version of the app targeting Lollipop and higher.
It has a limited functionality but it uses only official APIs and doesn't require root.


## Building native code
Native SCR code uses private Android APIs which are not part of SDK/NDK.
To build it you need to build whole AOSP tree first.

Separate binary needs to be built for each Android version and placed under `ScreenRecorder/src/root/res/raw-vX`
resource folder where `X` is na Android version code.
The last released version of the app supported platforms Ice Cream Sandwich to Lollipop
and required building 8 AOSP source treas for two architectures (arm and x86).
See my [blog post](http://www.iwobanas.com/2015/06/accessing-android-internal-apis-from-apps/)
for further details regarding using private Android APIs.
