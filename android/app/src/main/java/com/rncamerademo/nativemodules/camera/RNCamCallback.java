package com.rncamerademo.nativemodules.camera;

// todo remove after combining Camera2.java and RNCamrewView.java
public interface RNCamCallback {

    void onPictureTaken(byte[] data, int deviceOrientation, int softwareRotation);
}
