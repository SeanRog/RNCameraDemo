package com.rncamerademo.nativemodules.camera;

public interface RNCamCallback {
    void onCameraOpened();

    void onCameraClosed();

    void onPictureTaken(byte[] data, int deviceOrientation, int softwareRotation);

    void onFramePreview(byte[] data, int width, int height, int orientation);

    void onMountError();
}
