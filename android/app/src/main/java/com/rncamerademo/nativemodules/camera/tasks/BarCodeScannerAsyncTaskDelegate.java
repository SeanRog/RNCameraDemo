package com.rncamerademo.nativemodules.camera.tasks;

import com.google.zxing.Result;

public interface BarCodeScannerAsyncTaskDelegate {
  void onBarCodeRead(Result barCode, int width, int height, byte[] imageData);
  void onBarCodeScanningTaskCompleted();
}
