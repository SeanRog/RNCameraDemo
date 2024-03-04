package com.rncamerademo.nativemodules.camera.tasks;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import androidx.exifinterface.media.ExifInterface;
import android.util.Base64;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import com.rncamerademo.nativemodules.camera.utils.HelperFunctions;
import com.rncamerademo.nativemodules.camera.utils.RNFileUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ResolveTakenPictureAsyncTask extends AsyncTask<Void, Void, WritableMap> {
    private static final String ERROR_TAG = "E_TAKING_PICTURE_FAILED";
    private Promise mPromise;
    private Bitmap mBitmap;
    private byte[] mImageData;
    private ReadableMap mOptions;
    private File mCacheDirectory;
    private int mDeviceOrientation;
    private int mSoftwareRotation;
    private PictureSavedDelegate mPictureSavedDelegate;

    public ResolveTakenPictureAsyncTask(byte[] imageData, Promise promise, ReadableMap options, File cacheDirectory, int deviceOrientation, int softwareRotation, PictureSavedDelegate delegate) {
        mPromise = promise;
        mOptions = options;
        mImageData = imageData;
        mCacheDirectory = cacheDirectory;
        mDeviceOrientation = deviceOrientation;
        mSoftwareRotation = softwareRotation;
        mPictureSavedDelegate = delegate;
    }

    private int getQuality() {
        return (int) (mOptions.getDouble("quality") * 100);
    }

    // loads bitmap only if necessary
    private void loadBitmap() throws IOException {
        if(mBitmap == null){
            mBitmap = BitmapFactory.decodeByteArray(mImageData, 0, mImageData.length);
        }
        if(mBitmap == null){
            throw new IOException("Failed to decode Image Bitmap");
        }
    }

    // todo delete
    @Override
    protected WritableMap doInBackground(Void... voids) {
        return null;
    }

    private Bitmap rotateBitmap(Bitmap source, int angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private Bitmap resizeBitmap(Bitmap bm, int newWidth) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleRatio = (float) newWidth / (float) width;

        return Bitmap.createScaledBitmap(bm, newWidth, (int) (height * scaleRatio), true);
    }

    private Bitmap flipHorizontally(Bitmap source) {
        Matrix matrix = new Matrix();
        matrix.preScale(-1.0f, 1.0f);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    // Get rotation degrees from Exif orientation enum

    private int getImageRotation(int orientation) {
        int rotationDegrees = 0;
        switch (orientation) {
        case ExifInterface.ORIENTATION_ROTATE_90:
            rotationDegrees = 90;
            break;
        case ExifInterface.ORIENTATION_ROTATE_180:
            rotationDegrees = 180;
            break;
        case ExifInterface.ORIENTATION_ROTATE_270:
            rotationDegrees = 270;
            break;
        }
        return rotationDegrees;
    }

    private String getImagePath() throws IOException{
        if(mOptions.hasKey("path")){
            return mOptions.getString("path");
        }
        return RNFileUtils.getOutputFilePath(mCacheDirectory, ".jpg");
    }

    private String writeStreamToFile(ByteArrayOutputStream imageDataStream) throws IOException {
        String outputPath = null;
        IOException exception = null;
        FileOutputStream fileOutputStream = null;

        try {
            outputPath = getImagePath();
            fileOutputStream = new FileOutputStream(outputPath);
            imageDataStream.writeTo(fileOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
            exception = e;
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (exception != null) {
            throw exception;
        }

        return outputPath;
    }

    @Override
    protected void onPostExecute(WritableMap response) {
        super.onPostExecute(response);

        // If the response is not null everything went well and we can resolve the promise.
        if (response != null) {
            if (mOptions.hasKey("fastMode") && mOptions.getBoolean("fastMode")) {
                WritableMap wrapper = Arguments.createMap();
                wrapper.putInt("id", mOptions.getInt("id"));
                wrapper.putMap("data", response);
                mPictureSavedDelegate.onPictureSaved(wrapper);
            } else {
                mPromise.resolve(response);
            }
        }
    }

}
