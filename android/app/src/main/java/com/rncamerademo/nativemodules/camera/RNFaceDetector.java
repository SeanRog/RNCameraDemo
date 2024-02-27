package com.rncamerademo.nativemodules.camera;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import com.rncamerademo.nativemodules.camera.utils.ImageDimensions;
import com.rncamerademo.nativemodules.camera.utils.RNFrame;

import java.util.ArrayList;
import java.util.List;

public class RNFaceDetector {
  private static String TAG = "RNFaceDetector";
  public static int ALL_CLASSIFICATIONS = FaceDetectorOptions.CLASSIFICATION_MODE_ALL;
  public static int NO_CLASSIFICATIONS = FaceDetectorOptions.CLASSIFICATION_MODE_NONE;
  public static int ALL_LANDMARKS = FaceDetectorOptions.LANDMARK_MODE_ALL;
  public static int NO_LANDMARKS = FaceDetectorOptions.LANDMARK_MODE_NONE;
  public static int ACCURATE_MODE = FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE;
  public static int FAST_MODE = FaceDetectorOptions.PERFORMANCE_MODE_FAST;

  private FaceDetector mFaceDetector = null;
  private boolean mIsDetecting = false;
  private ImageDimensions mPreviousDimensions;
  private FaceDetectorOptions.Builder mFaceDetectorOptionsBuilder = null;

  private int mClassificationType = NO_CLASSIFICATIONS;
  private int mLandmarkType = NO_LANDMARKS;
  private float mMinFaceSize = 0.15f;
  private int mMode = FAST_MODE;

  public RNFaceDetector(Context context) {
    mFaceDetectorOptionsBuilder = new FaceDetectorOptions.Builder();
    mFaceDetectorOptionsBuilder.setMinFaceSize(mMinFaceSize);
    mFaceDetectorOptionsBuilder.setPerformanceMode(mMode);
    mFaceDetectorOptionsBuilder.setLandmarkMode(mLandmarkType);
    mFaceDetectorOptionsBuilder.setClassificationMode(mClassificationType);
  }

  // Public API

  public boolean isOperational() {
    if (mFaceDetector == null) {
      createFaceDetector();
    }

    return true;
  }

  public List<Face> detect(RNFrame frame) {
    if (mIsDetecting) {
      return new ArrayList<>();
    }
    mIsDetecting = true;
    // If the frame has different dimensions, create another face detector.
    // Otherwise we will get nasty "inconsistent image dimensions" error from detector
    // and no face will be detected.
    if (!frame.getDimensions().equals(mPreviousDimensions)) {
      releaseFaceDetector();
    }

    if (mFaceDetector == null) {
      createFaceDetector();
      mPreviousDimensions = frame.getDimensions();
    }

//    Log.d(TAG, "Dimensions:: " + frame.getDimensions().getHeight());
//    Log.d(TAG, "Dimensions:: " + frame.getDimensions().getWidth());
//    Log.d(TAG, "Dimensions:: " + frame.getDimensions().getRotation());
//    Log.d(TAG, "Dimensions:: " + frame.getDimensions().getFacing());

    Task<List<Face>> detectFacesTask = mFaceDetector.process(frame.getFrame());
    handleFaceDetectionTask(detectFacesTask);
    return new ArrayList<>();
//    return mFaceDetector.process(frame.getFrame()).getResult();
  }

  public void handleFaceDetectionTask(Task<List<Face>> detectFacesTask) {
    detectFacesTask.addOnCompleteListener(completedTask -> {
      mIsDetecting = false;
      List<Face> facesResult = completedTask.getResult();
      if (facesResult.size() > 0) {
        Log.d(TAG, "completed. facesResult.size():: " + facesResult.size());
      }

      facesResult.forEach(face -> {
        Log.d(TAG, "" + face.getBoundingBox().centerX());
        Log.d(TAG, "" + face.getBoundingBox().centerY());
      });
    });
    detectFacesTask.addOnFailureListener(e -> {
      Log.d(TAG, "Face detection task failed:: " + e.getMessage());
    });
    detectFacesTask.addOnCanceledListener(() -> {
      Log.d(TAG, "Face detection canceled");
    });
  }

  public void setTracking(boolean trackingEnabled) {
    release();
    if (trackingEnabled) {
      mFaceDetectorOptionsBuilder.enableTracking();
    }
  }

  public void setClassificationType(int classificationType) {
    if (classificationType != mClassificationType) {
      release();
      mFaceDetectorOptionsBuilder.setClassificationMode(classificationType);
      mClassificationType = classificationType;
    }
  }

  public void setLandmarkType(int landmarkType) {
    if (landmarkType != mLandmarkType) {
      release();
      mFaceDetectorOptionsBuilder.setLandmarkMode(landmarkType);
      mLandmarkType = landmarkType;
    }
  }

  public void setMode(int mode) {
    if (mode != mMode) {
      release();
      mFaceDetectorOptionsBuilder.setPerformanceMode(mode);
      mMode = mode;
    }
  }

  public void release() {
    releaseFaceDetector();
    mPreviousDimensions = null;
  }

  // Lifecycle methods

  private void releaseFaceDetector() {
    if (mFaceDetector != null) {
      mFaceDetector.close();
      mFaceDetector = null;
    }
  }

  private void createFaceDetector() {
    mFaceDetector = FaceDetection.getClient(mFaceDetectorOptionsBuilder.build());
  }
}
