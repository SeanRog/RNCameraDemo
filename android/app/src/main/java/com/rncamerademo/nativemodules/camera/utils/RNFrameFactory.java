package com.rncamerademo.nativemodules.camera.utils;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;

import java.nio.ByteBuffer;
import java.util.Base64;

public class RNFrameFactory {
  public static RNFrame buildFrame(byte[] bitmapData, int width, int height, int rotation) {
    ByteBuffer byteBuffer = ByteBuffer.wrap(bitmapData);
    ImageDimensions dimensions = new ImageDimensions(width, height, rotation);
    InputImage image = InputImage.fromByteBuffer(byteBuffer, width, height, rotation, InputImage.IMAGE_FORMAT_NV21);
    return new RNFrame(image, dimensions);
  }

  public static RNFrame buildFrame(Bitmap bitmap) {
    ImageDimensions dimensions = new ImageDimensions(bitmap.getWidth(), bitmap.getHeight());
    InputImage image = InputImage.fromBitmap(bitmap, 0);
    return new RNFrame(image, dimensions);
  }
}
