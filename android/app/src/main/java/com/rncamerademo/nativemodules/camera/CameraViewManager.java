package com.rncamerademo.nativemodules.camera;

import android.util.Log;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CameraViewManager extends ViewGroupManager<RNCameraView> {
  private String TAG = "rncamerademo";
  public enum Events {
    EVENT_CAMERA_READY("onCameraReady"),
    EVENT_ON_MOUNT_ERROR("onMountError"),
    EVENT_ON_BAR_CODE_READ("onBarCodeRead"),
    EVENT_ON_FACES_DETECTED("onFacesDetected"),
    EVENT_ON_BARCODES_DETECTED("onGoogleVisionBarcodesDetected"),
    EVENT_ON_FACE_DETECTION_ERROR("onFaceDetectionError"),
    EVENT_ON_BARCODE_DETECTION_ERROR("onGoogleVisionBarcodeDetectionError"),
    EVENT_ON_TEXT_RECOGNIZED("onTextRecognized"),
    EVENT_ON_PICTURE_TAKEN("onPictureTaken"),
    EVENT_ON_PICTURE_SAVED("onPictureSaved"),
    EVENT_ON_RECORDING_START("onRecordingStart"),
    EVENT_ON_RECORDING_END("onRecordingEnd"),
    EVENT_ON_TOUCH("onTouch");


    private final String mName;

    Events(final String name) {
      mName = name;
    }

    @Override
    public String toString() {
      return mName;
    }
  }

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

  @Override
  @Nullable
  public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
    MapBuilder.Builder<String, Object> builder = MapBuilder.builder();
    for (Events event : Events.values()) {
      builder.put(event.toString(), MapBuilder.of("registrationName", event.toString()));
    }
    return builder.build();
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

  @ReactProp(name = "useNativeZoom")
  public void setUseNativeZoom(RNCameraView view, boolean useNativeZoom) {
    view.setUseNativeZoom(useNativeZoom);
  }
  @ReactProp(name = "whiteBalance")
  public void setWhiteBalance(RNCameraView view, int whiteBalance) {
    view.setWhiteBalance(whiteBalance);
  }

  @ReactProp(name = "pictureSize")
  public void setPictureSize(RNCameraView view, String size) {
    view.setPictureSize(size.equals("None") ? null : Size.parse(size));
  }

  @ReactProp(name = "barCodeScannerEnabled")
  public void setBarCodeScanning(RNCameraView view, boolean barCodeScannerEnabled) {
    view.setShouldScanBarCodes(barCodeScannerEnabled);
  }

  @ReactProp(name = "touchDetectorEnabled")
  public void setTouchDetectorEnabled(RNCameraView view, boolean touchDetectorEnabled) {
    view.setShouldDetectTouches(touchDetectorEnabled);
  }

  @ReactProp(name = "faceDetectorEnabled")
  public void setFaceDetecting(RNCameraView view, boolean faceDetectorEnabled) {
    view.setShouldDetectFaces(faceDetectorEnabled);
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

  @ReactProp(name = "googleVisionBarcodeDetectorEnabled")
  public void setGoogleVisionBarcodeDetecting(RNCameraView view, boolean googleBarcodeDetectorEnabled) {
    view.setShouldGoogleDetectBarcodes(googleBarcodeDetectorEnabled);
  }

  @ReactProp(name = "textRecognizerEnabled")
  public void setTextRecognizing(RNCameraView view, boolean textRecognizerEnabled) {
    view.setShouldRecognizeText(textRecognizerEnabled);
  }
  /**---limit scan area addition---**/
}
