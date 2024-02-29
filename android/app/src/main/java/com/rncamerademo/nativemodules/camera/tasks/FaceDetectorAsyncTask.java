package com.rncamerademo.nativemodules.camera.tasks;

import android.media.Image;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.mlkit.vision.face.Face;

import com.rncamerademo.nativemodules.camera.RNCameraView;
import com.rncamerademo.nativemodules.camera.RNFaceDetector;
import com.rncamerademo.nativemodules.camera.utils.ImageDimensions;
import com.rncamerademo.nativemodules.camera.utils.FaceDetectorUtils;
import com.rncamerademo.nativemodules.camera.utils.RNFrame;
import com.rncamerademo.nativemodules.camera.utils.RNFrameFactory;

import java.util.List;

public class FaceDetectorAsyncTask extends android.os.AsyncTask<Void, Void, List<Face>> {
  private static String TAG = "FaceDetectorAsyncTask";
  private Image mImage;
  private int mWidth;
  private int mHeight;
  private int mRotation;
  private RNFaceDetector mFaceDetector;
  private FaceDetectorAsyncTaskDelegate mDelegate;
  private ImageDimensions mImageDimensions;
  private double mScaleX;
  private double mScaleY;
  private int mPaddingLeft;
  private int mPaddingTop;

  public FaceDetectorAsyncTask(
      FaceDetectorAsyncTaskDelegate delegate,
      RNFaceDetector faceDetector,
      Image image,
      int width,
      int height,
      int rotation,
      float density,
      int facing,
      int viewWidth,
      int viewHeight,
      int viewPaddingLeft,
      int viewPaddingTop
  ) {
    mImage = image;
    mWidth = width;
    mHeight = height;
    mRotation = rotation;
    mDelegate = delegate;
    mFaceDetector = faceDetector;
    mImageDimensions = new ImageDimensions(width, height, rotation, facing);
    mScaleX = (double) (viewWidth) / (mImageDimensions.getWidth() * density);
    mScaleY = (double) (viewHeight) / (mImageDimensions.getHeight() * density);
    mPaddingLeft = viewPaddingLeft;
    mPaddingTop = viewPaddingTop;
  }

  @Override
  protected List<Face> doInBackground(Void... ignored) {
    if (isCancelled() || mDelegate == null || mFaceDetector == null || !mFaceDetector.isOperational()) {
      return null;
    }

    RNFrame frame = RNFrameFactory.buildFrame(mImage, mWidth, mHeight, mRotation);
//    Log.d(TAG, "mRotation:: " + mRotation);
    return mFaceDetector.detect(frame);
  }

  @Override
  protected void onPostExecute(List<Face> faces) {
    super.onPostExecute(faces);

    if (faces == null) {
      mDelegate.onFaceDetectionError(mFaceDetector);
    } else {
      if (faces.size() > 0) {
        mDelegate.onFacesDetected(serializeEventData(faces));
      }
      mDelegate.onFaceDetectingTaskCompleted();
    }
  }

  private WritableArray serializeEventData(List<Face> faces) {
    WritableArray facesList = Arguments.createArray();

    for(int i = 0; i < faces.size(); i++) {
      Face face = faces.get(i);
      WritableMap serializedFace = FaceDetectorUtils.serializeFace(face, mScaleX, mScaleY, mWidth, mHeight, mPaddingLeft, mPaddingTop);
      if (mImageDimensions.getFacing() == RNCameraView.FACING_FRONT) {
        serializedFace = FaceDetectorUtils.rotateFaceX(serializedFace, mImageDimensions.getWidth(), mScaleX);
      } else {
        serializedFace = FaceDetectorUtils.changeAnglesDirection(serializedFace);
      }
      facesList.pushMap(serializedFace);
    }

    return facesList;
  }
}
