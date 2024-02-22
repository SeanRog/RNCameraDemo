package com.rncamerademo.nativemodules.camera.tasks;

import com.facebook.react.bridge.WritableArray;

import com.rncamerademo.nativemodules.camera.RNBarcodeDetector;

public interface BarcodeDetectorAsyncTaskDelegate {

    void onBarcodesDetected(WritableArray barcodes, int width, int height, byte[] imageData);

    void onBarcodeDetectionError(RNBarcodeDetector barcodeDetector);

    void onBarcodeDetectingTaskCompleted();
}
