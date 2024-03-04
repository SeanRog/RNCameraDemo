package com.rncamerademo.nativemodules.camera;

public interface RNCamCallback {

    void onPictureTaken(byte[] data, int deviceOrientation, int softwareRotation);
}
