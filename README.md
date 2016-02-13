#Rhino
Find information here - http://www.mozilla.org/rhino/

#How to use

Import via gradle
```
compile 'com.faendir.rhino:rhino-android:1.1'
```

Then, instead of calling 
```
Context.enter()
```
use
```
RhinoAndroidHelper.prepareContext()
```

#dx
This uses google's dx tool which can be found here - https://android.googlesource.com/platform/dalvik
