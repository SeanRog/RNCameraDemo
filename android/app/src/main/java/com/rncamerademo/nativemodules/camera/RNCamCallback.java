package com.rncamerademo.nativemodules.camera;

import android.media.Image;

public interface RNCamCallback {
    void onCameraOpened();

    void onCameraClosed();

    void onPictureTaken(byte[] data, int deviceOrientation, int softwareRotation);

    void onFramePreview(Image image, int width, int height, int orientation);

    void onMountError();
}
