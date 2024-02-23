/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rncamerademo.nativemodules.camera;

import android.content.Context;
import android.os.Build;
import android.os.HandlerThread;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.ParcelableCompat;
import androidx.core.os.ParcelableCompatCreatorCallbacks;
import androidx.core.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.graphics.SurfaceTexture;

import com.facebook.react.bridge.ReadableMap;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;

public class CameraView extends FrameLayout {

    /** The camera device faces the opposite direction as the device's screen. */
    public static final int FACING_BACK = Constants.FACING_BACK;

    /** The camera device faces the same direction as the device's screen. */
    public static final int FACING_FRONT = Constants.FACING_FRONT;

    /** Direction the camera faces relative to device screen. */
    @IntDef({FACING_BACK, FACING_FRONT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Facing {
    }

    /** Flash will not be fired. */
    public static final int FLASH_OFF = Constants.FLASH_OFF;

    /** Flash will always be fired during snapshot. */
    public static final int FLASH_ON = Constants.FLASH_ON;

    /** Constant emission of light during preview, auto-focus and snapshot. */
    public static final int FLASH_TORCH = Constants.FLASH_TORCH;

    /** Flash will be fired automatically when required. */
    public static final int FLASH_AUTO = Constants.FLASH_AUTO;

    /** Flash will be fired in red-eye reduction mode. */
    public static final int FLASH_RED_EYE = Constants.FLASH_RED_EYE;

    /** The mode for for the camera device's flash control */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FLASH_OFF, FLASH_ON, FLASH_TORCH, FLASH_AUTO, FLASH_RED_EYE})
    public @interface Flash {
    }

    Camera2 camera2;

    private final CallbackBridge mCallbacks;

    private boolean mAdjustViewBounds;

    private Context mContext;

    private final DisplayOrientationDetector mDisplayOrientationDetector;

    protected HandlerThread mBgThread;
    protected Handler mBgHandler;


    public CameraView(Context context, boolean fallbackToOldApi) {
        this(context, null, fallbackToOldApi);
    }

    public CameraView(Context context, AttributeSet attrs, boolean fallbackToOldApi) {
        this(context, attrs, 0, fallbackToOldApi);
    }

    @SuppressWarnings("WrongConstant")
    public CameraView(Context context, AttributeSet attrs, int defStyleAttr, boolean fallbackToOldApi) {
        super(context, attrs, defStyleAttr);

        // bg hanadler for non UI heavy work
        mBgThread = new HandlerThread("RNCamera-Handler-Thread");
        mBgThread.start();
        mBgHandler = new Handler(mBgThread.getLooper());


        if (isInEditMode()){
            mCallbacks = null;
            mDisplayOrientationDetector = null;
            return;
        }
        mAdjustViewBounds = true;
        mContext = context;

        // Internal setup
        // Refactor remove Camera 1 logic
        final TextureViewPreview preview = createPreview(context);
        mCallbacks = new CallbackBridge();
				
				camera2 = new Camera2(mCallbacks, preview, context, mBgHandler);

        // Display orientation detector
        mDisplayOrientationDetector = new DisplayOrientationDetector(context) {
            @Override
            public void onDisplayOrientationChanged(int displayOrientation, int deviceOrientation) {
                camera2.setDisplayOrientation(displayOrientation);
                camera2.setDeviceOrientation(deviceOrientation);
            }
        };
    }

    public void cleanup(){
        if(mBgThread != null){
            if(Build.VERSION.SDK_INT < 18){
                mBgThread.quit();
            }
            else{
                mBgThread.quitSafely();
            }

            mBgThread = null;
        }
    }

    @NonNull
    private TextureViewPreview createPreview(Context context) {
        TextureViewPreview preview;
        if (Build.VERSION.SDK_INT < 14) {
            // preview = new SurfaceViewPreview(context, this);
            throw new RuntimeException("can't create preview. SDK version is too old. Build.VERSION.SDK_INT:: " + Build.VERSION.SDK_INT);
        } else {
            preview = new TextureViewPreview(context, this);
        }
        return preview;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            mDisplayOrientationDetector.enable(ViewCompat.getDisplay(this));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            mDisplayOrientationDetector.disable();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (isInEditMode()){
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        // Handle android:adjustViewBounds
        if (mAdjustViewBounds) {
            if (!isCameraOpened()) {
                mCallbacks.reserveRequestLayoutOnOpen();
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }
            final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
                final AspectRatio ratio = getAspectRatio();
                assert ratio != null;
                int height = (int) (MeasureSpec.getSize(widthMeasureSpec) * ratio.toFloat());
                if (heightMode == MeasureSpec.AT_MOST) {
                    height = Math.min(height, MeasureSpec.getSize(heightMeasureSpec));
                }
                super.onMeasure(widthMeasureSpec,
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            } else if (widthMode != MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
                final AspectRatio ratio = getAspectRatio();
                assert ratio != null;
                int width = (int) (MeasureSpec.getSize(heightMeasureSpec) * ratio.toFloat());
                if (widthMode == MeasureSpec.AT_MOST) {
                    width = Math.min(width, MeasureSpec.getSize(widthMeasureSpec));
                }
                super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                        heightMeasureSpec);
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
        // Measure the TextureView
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        AspectRatio ratio = getAspectRatio();
        if (mDisplayOrientationDetector.getLastKnownDisplayOrientation() % 180 == 0) {
            ratio = ratio.inverse();
        }
        assert ratio != null;
        if (height < width * ratio.getY() / ratio.getX()) {
            camera2.getView().measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(width * ratio.getY() / ratio.getX(),
                            MeasureSpec.EXACTLY));
        } else {
            camera2.getView().measure(
                    MeasureSpec.makeMeasureSpec(height * ratio.getX() / ratio.getY(),
                            MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.facing = getFacing();
        state.cameraId = getCameraId();
        state.ratio = getAspectRatio();
        state.autoFocus = getAutoFocus();
        state.flash = getFlash();
        state.focusDepth = getFocusDepth();
        state.zoom = getZoom();
        state.whiteBalance = getWhiteBalance();
        state.playSoundOnCapture = getPlaySoundOnCapture();
        state.playSoundOnRecord = getPlaySoundOnRecord();
        state.scanning = getScanning();
        state.pictureSize = getPictureSize();
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setFacing(ss.facing);
        setCameraId(ss.cameraId);
        setAspectRatio(ss.ratio);
        setAutoFocus(ss.autoFocus);
        setFlash(ss.flash);
        setFocusDepth(ss.focusDepth);
        setZoom(ss.zoom);
        setWhiteBalance(ss.whiteBalance);
        setPlaySoundOnCapture(ss.playSoundOnCapture);
        setPlaySoundOnRecord(ss.playSoundOnRecord);
        setScanning(ss.scanning);
        setPictureSize(ss.pictureSize);
    }

    public void setUsingCamera2Api(boolean useCamera2) {
        if (Build.VERSION.SDK_INT < 21) {
            return;
        }

        boolean wasOpened = isCameraOpened();
        Parcelable state = onSaveInstanceState();

        if (wasOpened) {
            stop();
        }
        camera2 = new Camera2(mCallbacks, camera2.mPreview, mContext, mBgHandler);

        onRestoreInstanceState(state);
        if(wasOpened){
            start();
        }
    }

    /**
     * Open a camera device and start showing camera preview. This is typically called from
     * Activity#onResume().
     */
    public void start() {
        camera2.start();
    }

    /**
     * Stop camera preview and close the device. This is typically called from
     * Activity#onPause().
     */
    public void stop() {
        camera2.stop();
    }

    /**
     * @return {@code true} if the camera is opened.
     */
    public boolean isCameraOpened() {
        return camera2.isCameraOpened();
    }

    /**
     * Add a new callback.
     *
     * @param callback The {@link Callback} to add.
     * @see #removeCallback(Callback)
     */
    public void addCallback(@NonNull Callback callback) {
        mCallbacks.add(callback);
    }

    /**
     * Remove a callback.
     *
     * @param callback The {@link Callback} to remove.
     * @see #addCallback(Callback)
     */
    public void removeCallback(@NonNull Callback callback) {
        mCallbacks.remove(callback);
    }

    /**
     * @param adjustViewBounds {@code true} if you want the CameraView to adjust its bounds to
     *                         preserve the aspect ratio of camera.
     * @see #getAdjustViewBounds()
     */
    public void setAdjustViewBounds(boolean adjustViewBounds) {
        if (mAdjustViewBounds != adjustViewBounds) {
            mAdjustViewBounds = adjustViewBounds;
            requestLayout();
        }
    }

    /**
     * @return True when this CameraView is adjusting its bounds to preserve the aspect ratio of
     * camera.
     * @see #setAdjustViewBounds(boolean)
     */
    public boolean getAdjustViewBounds() {
        return mAdjustViewBounds;
    }

    public View getView() {
      if (camera2 != null) {
        return camera2.getView();
      }
      return null;
    }

    /**
     * Chooses camera by the direction it faces.
     *
     * @param facing The camera facing. Must be either {@link #FACING_BACK} or
     *               {@link #FACING_FRONT}.
     */
    public void setFacing(@Facing int facing) {
        camera2.setFacing(facing);
    }

    /**
     * Gets the direction that the current camera faces.
     *
     * @return The camera facing.
     */
    @Facing
    public int getFacing() {
        //noinspection WrongConstant
        return camera2.getFacing();
    }

     /**
     * Chooses camera by its camera iD
     *
     * @param id The camera ID
     */
    public void setCameraId(String id) {
      camera2.setCameraId(id);
    }

    /**
     * Gets the currently set camera ID
     *
     * @return The camera facing.
     */
    public String getCameraId() {
      return camera2.getCameraId();
    }

    /**
     * Gets all the aspect ratios supported by the current camera.
     */
    public Set<AspectRatio> getSupportedAspectRatios() {
        return camera2.getSupportedAspectRatios();
    }

    /**
     * Gets all the camera IDs supported by the phone as a String
     */
    public List<Properties> getCameraIds() {
        return camera2.getCameraIds();
    }

    /**
     * Sets the aspect ratio of camera.
     *
     * @param ratio The {@link AspectRatio} to be set.
     */
    public void setAspectRatio(@NonNull AspectRatio ratio) {
        if (camera2.setAspectRatio(ratio)) {
            requestLayout();
        }
    }

    /**
     * Gets the current aspect ratio of camera.
     *
     * @return The current {@link AspectRatio}. Can be {@code null} if no camera is opened yet.
     */
    @Nullable
    public AspectRatio getAspectRatio() {
        return camera2.getAspectRatio();
    }

    /**
     * Gets all the picture sizes for particular ratio supported by the current camera.
     *
     * @param ratio {@link AspectRatio} for which the available image sizes will be returned.
     */
    public SortedSet<Size> getAvailablePictureSizes(@NonNull AspectRatio ratio) {
        return camera2.getAvailablePictureSizes(ratio);
    }

    /**
     * Sets the size of taken pictures.
     *
     * @param size The {@link Size} to be set.
     */
    public void setPictureSize(@NonNull Size size) {
        camera2.setPictureSize(size);
    }

    /**
     * Gets the size of pictures that will be taken.
     */
    public Size getPictureSize() {
        return camera2.getPictureSize();
    }

    /**
     * Enables or disables the continuous auto-focus mode. When the current camera doesn't support
     * auto-focus, calling this method will be ignored.
     *
     * @param autoFocus {@code true} to enable continuous auto-focus mode. {@code false} to
     *                  disable it.
     */
    public void setAutoFocus(boolean autoFocus) {
        camera2.setAutoFocus(autoFocus);
    }

    /**
     * Returns whether the continuous auto-focus mode is enabled.
     *
     * @return {@code true} if the continuous auto-focus mode is enabled. {@code false} if it is
     * disabled, or if it is not supported by the current camera.
     */
    public boolean getAutoFocus() {
        return camera2.getAutoFocus();
    }

    /**
     * Sets the flash mode.
     *
     * @param flash The desired flash mode.
     */
    public void setFlash(@Flash int flash) {
        camera2.setFlash(flash);
    }

    public ArrayList<int[]> getSupportedPreviewFpsRange() {
      return camera2.getSupportedPreviewFpsRange();
    }

    /**
     * Gets the current flash mode.
     *
     * @return The current flash mode.
     */
    @Flash
    public int getFlash() {
        //noinspection WrongConstant
        return camera2.getFlash();
    }


    /**
     * Gets the camera orientation relative to the devices native orientation.
     *
     * @return The orientation of the camera.
     */
    public int getCameraOrientation() {
        return camera2.getCameraOrientation();
    }

    /**
     * Sets the auto focus point.
     *
     * @param x sets the x coordinate for camera auto focus
     * @param y sets the y coordinate for camera auto focus
     */
    public void setAutoFocusPointOfInterest(float x, float y) {
        camera2.setFocusArea(x, y);
    }

    public void setFocusDepth(float value) {
        camera2.setFocusDepth(value);
    }

    public float getFocusDepth() { return camera2.getFocusDepth(); }

    public void setZoom(float zoom) {
      camera2.setZoom(zoom);
    }

    public float getZoom() {
      return camera2.getZoom();
    }

    public void setWhiteBalance(int whiteBalance) {
      camera2.setWhiteBalance(whiteBalance);
    }

    public int getWhiteBalance() {
      return camera2.getWhiteBalance();
    }

    public void setPlaySoundOnCapture(boolean playSoundOnCapture) {
      camera2.setPlaySoundOnCapture(playSoundOnCapture);
    }

    public boolean getPlaySoundOnCapture() {
      return camera2.getPlaySoundOnCapture();
    }

    public void setPlaySoundOnRecord(boolean playSoundOnRecord) {
        camera2.setPlaySoundOnRecord(playSoundOnRecord);
    }

    public boolean getPlaySoundOnRecord() {
        return camera2.getPlaySoundOnRecord();
    }

    public void setScanning(boolean isScanning) { camera2.setScanning(isScanning);}

    public boolean getScanning() { return camera2.getScanning(); }

    /**
     * Take a picture. The result will be returned to
     * Callback#onPictureTaken(CameraView, byte[], int).
     */
    public void takePicture(ReadableMap options) {
        camera2.takePicture(options);
    }

    public void resumePreview() {
        camera2.resumePreview();
    }

    public void pausePreview() {
        camera2.pausePreview();
    }

    public void setPreviewTexture(SurfaceTexture surfaceTexture) {
        camera2.setPreviewTexture(surfaceTexture);
    }

    public Size getPreviewSize() {
        return camera2.getPreviewSize();
    }

    private class CallbackBridge implements Camera2.Callback {

        private final ArrayList<Callback> mCallbacks = new ArrayList<>();

        private boolean mRequestLayoutOnOpen;

        CallbackBridge() {
        }

        public void add(Callback callback) {
            mCallbacks.add(callback);
        }

        public void remove(Callback callback) {
            mCallbacks.remove(callback);
        }

        @Override
        public void onCameraOpened() {
            if (mRequestLayoutOnOpen) {
                mRequestLayoutOnOpen = false;
                requestLayout();
            }
            for (Callback callback : mCallbacks) {
                callback.onCameraOpened(CameraView.this);
            }
        }

        @Override
        public void onCameraClosed() {
            for (Callback callback : mCallbacks) {
                callback.onCameraClosed(CameraView.this);
            }
        }

        @Override
        public void onPictureTaken(byte[] data, int deviceOrientation, int softwareRotation) {
            for (Callback callback : mCallbacks) {
                callback.onPictureTaken(CameraView.this, data, deviceOrientation, softwareRotation);
            }
        }

        @Override
        public void onFramePreview(byte[] data, int width, int height, int orientation) {
            for (Callback callback : mCallbacks) {
                callback.onFramePreview(CameraView.this, data, width, height, orientation);
            }
        }

        @Override
        public void onMountError() {
            for (Callback callback : mCallbacks) {
                callback.onMountError(CameraView.this);
            }
        }

        public void reserveRequestLayoutOnOpen() {
            mRequestLayoutOnOpen = true;
        }
    }

    protected static class SavedState extends BaseSavedState {

        @Facing
        int facing;

        String cameraId;

        AspectRatio ratio;

        boolean autoFocus;

        @Flash
        int flash;

        float exposure;

        float focusDepth;

        float zoom;

        int whiteBalance;

        boolean playSoundOnCapture;

        boolean playSoundOnRecord;

        boolean scanning;

        Size pictureSize;

        @SuppressWarnings("WrongConstant")
        public SavedState(Parcel source, ClassLoader loader) {
            super(source);
            facing = source.readInt();
            cameraId = source.readString();
            ratio = source.readParcelable(loader);
            autoFocus = source.readByte() != 0;
            flash = source.readInt();
            exposure = source.readFloat();
            focusDepth = source.readFloat();
            zoom = source.readFloat();
            whiteBalance = source.readInt();
            playSoundOnCapture = source.readByte() != 0;
            playSoundOnRecord = source.readByte() != 0;
            scanning = source.readByte() != 0;
            pictureSize = source.readParcelable(loader);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(facing);
            out.writeString(cameraId);
            out.writeParcelable(ratio, 0);
            out.writeByte((byte) (autoFocus ? 1 : 0));
            out.writeInt(flash);
            out.writeFloat(exposure);
            out.writeFloat(focusDepth);
            out.writeFloat(zoom);
            out.writeInt(whiteBalance);
            out.writeByte((byte) (playSoundOnCapture ? 1 : 0));
            out.writeByte((byte) (playSoundOnRecord ? 1 : 0));
            out.writeByte((byte) (scanning ? 1 : 0));
            out.writeParcelable(pictureSize, flags);
        }

        public static final Creator<SavedState> CREATOR
                = ParcelableCompat.newCreator(new ParcelableCompatCreatorCallbacks<SavedState>() {

            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }

        });

    }

    /**
     * Callback for monitoring events about {@link CameraView}.
     */
    @SuppressWarnings("UnusedParameters")
    public abstract static class Callback {

        /**
         * Called when camera is opened.
         *
         * @param cameraView The associated {@link CameraView}.
         */
        public void onCameraOpened(CameraView cameraView) {}

        /**
         * Called when camera is closed.
         *
         * @param cameraView The associated {@link CameraView}.
         */
        public void onCameraClosed(CameraView cameraView) {}

        /**
         * Called when a picture is taken.
         *
         * @param cameraView The associated {@link CameraView}.
         * @param data       JPEG data.
         */
        public void onPictureTaken(CameraView cameraView, byte[] data, int deviceOrientation, int softwareRotation) {}

        public void onFramePreview(CameraView cameraView, byte[] data, int width, int height, int orientation) {}

        public void onMountError(CameraView cameraView) {}
    }

}
