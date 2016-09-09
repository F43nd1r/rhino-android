[![Bintray](https://img.shields.io/bintray/v/f43nd1r/maven/rhino-android.svg)](https://bintray.com/f43nd1r/maven/rhino-android)
[![Maven Central](https://img.shields.io/maven-central/v/com.faendir.rhino/rhino-android.svg?maxAge=2592000)](http://search.maven.org/#search%7Cga%7C1%7Ccom.faendir.rhino)
[![Javadocs](http://www.javadoc.io/badge/com.faendir.rhino/rhino-android.svg)](http://www.javadoc.io/doc/com.faendir.rhino/rhino-android)

#Rhino
Find information here - http://www.mozilla.org/rhino/

#How to use

Import via gradle
```
compile 'com.faendir.rhino:rhino-android:1.3'
```

Then, instead of calling 
```
Context.enter()
```
use
```
RhinoAndroidHelper.enterContext()
```

An example project can be found [here](https://github.com/F43nd1r/rhino-android-example).

#dx
This uses google's dx tool which can be found here - https://android.googlesource.com/platform/dalvik
