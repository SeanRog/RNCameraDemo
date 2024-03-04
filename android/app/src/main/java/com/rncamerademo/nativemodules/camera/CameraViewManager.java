package com.rncamerademo.nativemodules.camera;

import android.util.Log;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;

public class CameraViewManager extends ViewGroupManager<RNCameraView> {
  private String TAG = "rncamerademo";

  private static final String REACT_CLASS = "RNCamera";

  @Override
  public void onDropViewInstance(RNCameraView view) {
    view.removeView(view);
    view.onHostDestroy();
    super.onDropViewInstance(view);
  }


  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @Override
  protected RNCameraView createViewInstance(ThemedReactContext themedReactContext) {
    Log.d(TAG, "create view instance");
    RNCameraView instance =  RNCameraView.getInstance(themedReactContext);
    Log.d(TAG, "end create view instance");
    return instance;
  }

  @ReactProp(name = "type")
  public void setType(RNCameraView view, int type) {
    view.setFacing(type);
  }

  @ReactProp(name = "cameraId")
  public void setCameraId(RNCameraView view, String id) {
    view.setCameraId(id);
  }

  @ReactProp(name = "ratio")
  public void setRatio(RNCameraView view, String ratio) {
    view.setAspectRatio(AspectRatio.parse(ratio));
  }

  @ReactProp(name = "flashMode")
  public void setFlashMode(RNCameraView view, int torchMode) {
    view.setFlash(torchMode);
  }

  @ReactProp(name = "autoFocus")
  public void setAutoFocus(RNCameraView view, boolean autoFocus) {
    view.setAutoFocus(autoFocus);
  }

  @ReactProp(name = "focusDepth")
  public void setFocusDepth(RNCameraView view, float depth) {
    view.setFocusDepth(depth);
  }

  @ReactProp(name = "autoFocusPointOfInterest")
  public void setAutoFocusPointOfInterest(RNCameraView view, ReadableMap coordinates) {
    if(coordinates != null){
      float x = (float) coordinates.getDouble("x");
      float y = (float) coordinates.getDouble("y");
      view.setAutoFocusPointOfInterest(x, y);
    }
  }

  @ReactProp(name = "zoom")
  public void setZoom(RNCameraView view, float zoom) {
    view.setZoom(zoom);
  }
  @ReactProp(name = "whiteBalance")
  public void setWhiteBalance(RNCameraView view, int whiteBalance) {
    view.setWhiteBalance(whiteBalance);
  }

  @ReactProp(name = "pictureSize")
  public void setPictureSize(RNCameraView view, String size) {
    view.setPictureSize(size.equals("None") ? null : Size.parse(size));
  }

  @ReactProp(name = "faceDetectionMode")
  public void setFaceDetectionMode(RNCameraView view, int mode) {
    view.setFaceDetectionMode(mode);
  }

  @ReactProp(name = "faceDetectionLandmarks")
  public void setFaceDetectionLandmarks(RNCameraView view, int landmarks) {
    view.setFaceDetectionLandmarks(landmarks);
  }

  @ReactProp(name = "faceDetectionClassifications")
  public void setFaceDetectionClassifications(RNCameraView view, int classifications) {
    view.setFaceDetectionClassifications(classifications);
  }

  @ReactProp(name = "trackingEnabled")
  public void setTracking(RNCameraView view, boolean trackingEnabled) {
    view.setTracking(trackingEnabled);
  }

  @ReactProp(name = "textRecognizerEnabled")
  public void setTextRecognizing(RNCameraView view, boolean textRecognizerEnabled) {
    view.setShouldRecognizeText(textRecognizerEnabled);
  }

  @ReactProp(name = "barcodeReaderEnabled")
  public void setShouldReadBarcodes(RNCameraView view, boolean barcodeReaderEnabled) {
    view.setShouldReadBarcodes(barcodeReaderEnabled);
  }

  @ReactProp(name = "faceDetectorEnabled")
  public void setShouldDetectFaces(RNCameraView view, boolean faceDetectorEnabled) {
    view.setShouldDetectFaces(faceDetectorEnabled);
  }
}
