package com.rncamerademo.nativemodules.camera.tasks;

import com.facebook.react.bridge.WritableMap;

public interface PictureSavedDelegate {
    void onPictureSaved(WritableMap response);
}
