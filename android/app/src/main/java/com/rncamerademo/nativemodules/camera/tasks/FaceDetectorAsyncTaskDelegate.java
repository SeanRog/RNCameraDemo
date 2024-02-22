package com.rncamerademo.nativemodules.camera.tasks;

import com.facebook.react.bridge.WritableArray;

import com.rncamerademo.nativemodules.camera.RNFaceDetector;

public interface FaceDetectorAsyncTaskDelegate {
  void onFacesDetected(WritableArray faces);
  void onFaceDetectionError(RNFaceDetector faceDetector);
  void onFaceDetectingTaskCompleted();
}
