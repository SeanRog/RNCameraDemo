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

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.rncamerademo.R;

@TargetApi(14)
class TextureViewPreview {
    interface Callback {
        void onSurfaceChanged();

        void onSurfaceDestroyed();
    }

    private final TextureView mTextureView;

    private Callback mCallback;

    private int mWidth;

    private int mHeight;

    void setCallback(Callback callback) {
        mCallback = callback;
    }

    private int mDisplayOrientation;

    TextureViewPreview(Context context, ViewGroup parent) {
        final View view = View.inflate(context, R.layout.texture_view, parent);
        mTextureView = view.findViewById(R.id.texture_view);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                setDimensions(width, height);
                configureTransform();
                dispatchSurfaceChanged();
            }

            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                setDimensions(width, height);
                configureTransform();
                dispatchSurfaceChanged();
            }

            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                setDimensions(0, 0);
                dispatchSurfaceDestroyed();
                return true;
            }

            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
    }

    // This method is called only from Camera2.
    @TargetApi(15)
    void setBufferSize(int width, int height) {
        mTextureView.getSurfaceTexture().setDefaultBufferSize(width, height);
    }

    void setDimensions(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    int getWidth() {
        return mWidth;
    }

    int getHeight() {
        return mHeight;
    }

    Surface getSurface() {
        return new Surface(mTextureView.getSurfaceTexture());
    }

    SurfaceTexture getSurfaceTexture() {
        return mTextureView.getSurfaceTexture();
    }

    View getView() {
        return mTextureView;
    }

    Class getOutputClass() {
        return SurfaceTexture.class;
    }

    void setDisplayOrientation(int displayOrientation) {
        mDisplayOrientation = displayOrientation;
        configureTransform();
    }

    boolean isReady() {
        return mTextureView.getSurfaceTexture() != null;
    }

    protected void dispatchSurfaceChanged() {
        mCallback.onSurfaceChanged();
    }

    protected void dispatchSurfaceDestroyed() {
        mCallback.onSurfaceDestroyed();
    }

    /**
     * Configures the transform matrix for TextureView based on {@link #mDisplayOrientation} and
     * the surface size.
     */
    void configureTransform() {
        Matrix matrix = new Matrix();
        if (mDisplayOrientation % 180 == 90) {
            final int width = getWidth();
            final int height = getHeight();
            // Rotate the camera preview when the screen is landscape.
            matrix.setPolyToPoly(
                    new float[]{
                            0.f, 0.f, // top left
                            width, 0.f, // top right
                            0.f, height, // bottom left
                            width, height, // bottom right
                    }, 0,
                    mDisplayOrientation == 90 ?
                            // Clockwise
                            new float[]{
                                    0.f, height, // top left
                                    0.f, 0.f, // top right
                                    width, height, // bottom left
                                    width, 0.f, // bottom right
                            } : // mDisplayOrientation == 270
                            // Counter-clockwise
                            new float[]{
                                    width, 0.f, // top left
                                    width, height, // top right
                                    0.f, 0.f, // bottom left
                                    0.f, height, // bottom right
                            }, 0,
                    4);
        } else if (mDisplayOrientation == 180) {
            matrix.postRotate(180, getWidth() / 2, getHeight() / 2);
        }
        mTextureView.setTransform(matrix);
    }

}
