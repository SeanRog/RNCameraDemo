package com.rncamerademo.nativemodules.camera.utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Base64;

import androidx.exifinterface.media.ExifInterface;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class HelperFunctions {

  private static final String ERROR_TAG = "E_TAKING_PICTURE_FAILED";

  public static final String[][] exifTags = new String[][]{
      {"string", ExifInterface.TAG_ARTIST},
      {"int", ExifInterface.TAG_BITS_PER_SAMPLE},
      {"int", ExifInterface.TAG_COMPRESSION},
      {"string", ExifInterface.TAG_COPYRIGHT},
      {"string", ExifInterface.TAG_DATETIME},
      {"string", ExifInterface.TAG_IMAGE_DESCRIPTION},
      {"int", ExifInterface.TAG_IMAGE_LENGTH},
      {"int", ExifInterface.TAG_IMAGE_WIDTH},
      {"int", ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT},
      {"int", ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH},
      {"string", ExifInterface.TAG_MAKE},
      {"string", ExifInterface.TAG_MODEL},
      {"int", ExifInterface.TAG_ORIENTATION},
      {"int", ExifInterface.TAG_PHOTOMETRIC_INTERPRETATION},
      {"int", ExifInterface.TAG_PLANAR_CONFIGURATION},
      {"double", ExifInterface.TAG_PRIMARY_CHROMATICITIES},
      {"double", ExifInterface.TAG_REFERENCE_BLACK_WHITE},
      {"int", ExifInterface.TAG_RESOLUTION_UNIT},
      {"int", ExifInterface.TAG_ROWS_PER_STRIP},
      {"int", ExifInterface.TAG_SAMPLES_PER_PIXEL},
      {"string", ExifInterface.TAG_SOFTWARE},
      {"int", ExifInterface.TAG_STRIP_BYTE_COUNTS},
      {"int", ExifInterface.TAG_STRIP_OFFSETS},
      {"int", ExifInterface.TAG_TRANSFER_FUNCTION},
      {"double", ExifInterface.TAG_WHITE_POINT},
      {"double", ExifInterface.TAG_X_RESOLUTION},
      {"double", ExifInterface.TAG_Y_CB_CR_COEFFICIENTS},
      {"int", ExifInterface.TAG_Y_CB_CR_POSITIONING},
      {"int", ExifInterface.TAG_Y_CB_CR_SUB_SAMPLING},
      {"double", ExifInterface.TAG_Y_RESOLUTION},
      {"double", ExifInterface.TAG_APERTURE_VALUE},
      {"double", ExifInterface.TAG_BRIGHTNESS_VALUE},
      {"string", ExifInterface.TAG_CFA_PATTERN},
      {"int", ExifInterface.TAG_COLOR_SPACE},
      {"string", ExifInterface.TAG_COMPONENTS_CONFIGURATION},
      {"double", ExifInterface.TAG_COMPRESSED_BITS_PER_PIXEL},
      {"int", ExifInterface.TAG_CONTRAST},
      {"int", ExifInterface.TAG_CUSTOM_RENDERED},
      {"string", ExifInterface.TAG_DATETIME_DIGITIZED},
      {"string", ExifInterface.TAG_DATETIME_ORIGINAL},
      {"string", ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION},
      {"double", ExifInterface.TAG_DIGITAL_ZOOM_RATIO},
      {"string", ExifInterface.TAG_EXIF_VERSION},
      {"double", ExifInterface.TAG_EXPOSURE_BIAS_VALUE},
      {"double", ExifInterface.TAG_EXPOSURE_INDEX},
      {"int", ExifInterface.TAG_EXPOSURE_MODE},
      {"int", ExifInterface.TAG_EXPOSURE_PROGRAM},
      {"double", ExifInterface.TAG_EXPOSURE_TIME},
      {"double", ExifInterface.TAG_F_NUMBER},
      {"string", ExifInterface.TAG_FILE_SOURCE},
      {"int", ExifInterface.TAG_FLASH},
      {"double", ExifInterface.TAG_FLASH_ENERGY},
      {"string", ExifInterface.TAG_FLASHPIX_VERSION},
      {"double", ExifInterface.TAG_FOCAL_LENGTH},
      {"int", ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM},
      {"int", ExifInterface.TAG_FOCAL_PLANE_RESOLUTION_UNIT},
      {"double", ExifInterface.TAG_FOCAL_PLANE_X_RESOLUTION},
      {"double", ExifInterface.TAG_FOCAL_PLANE_Y_RESOLUTION},
      {"int", ExifInterface.TAG_GAIN_CONTROL},
      {"int", ExifInterface.TAG_ISO_SPEED_RATINGS},
      {"string", ExifInterface.TAG_IMAGE_UNIQUE_ID},
      {"int", ExifInterface.TAG_LIGHT_SOURCE},
      {"string", ExifInterface.TAG_MAKER_NOTE},
      {"double", ExifInterface.TAG_MAX_APERTURE_VALUE},
      {"int", ExifInterface.TAG_METERING_MODE},
      {"int", ExifInterface.TAG_NEW_SUBFILE_TYPE},
      {"string", ExifInterface.TAG_OECF},
      {"int", ExifInterface.TAG_PIXEL_X_DIMENSION},
      {"int", ExifInterface.TAG_PIXEL_Y_DIMENSION},
      {"string", ExifInterface.TAG_RELATED_SOUND_FILE},
      {"int", ExifInterface.TAG_SATURATION},
      {"int", ExifInterface.TAG_SCENE_CAPTURE_TYPE},
      {"string", ExifInterface.TAG_SCENE_TYPE},
      {"int", ExifInterface.TAG_SENSING_METHOD},
      {"int", ExifInterface.TAG_SHARPNESS},
      {"double", ExifInterface.TAG_SHUTTER_SPEED_VALUE},
      {"string", ExifInterface.TAG_SPATIAL_FREQUENCY_RESPONSE},
      {"string", ExifInterface.TAG_SPECTRAL_SENSITIVITY},
      {"int", ExifInterface.TAG_SUBFILE_TYPE},
      {"string", ExifInterface.TAG_SUBSEC_TIME},
      {"string", ExifInterface.TAG_SUBSEC_TIME_DIGITIZED},
      {"string", ExifInterface.TAG_SUBSEC_TIME_ORIGINAL},
      {"int", ExifInterface.TAG_SUBJECT_AREA},
      {"double", ExifInterface.TAG_SUBJECT_DISTANCE},
      {"int", ExifInterface.TAG_SUBJECT_DISTANCE_RANGE},
      {"int", ExifInterface.TAG_SUBJECT_LOCATION},
      {"string", ExifInterface.TAG_USER_COMMENT},
      {"int", ExifInterface.TAG_WHITE_BALANCE},
      {"int", ExifInterface.TAG_GPS_ALTITUDE_REF},
      {"string", ExifInterface.TAG_GPS_AREA_INFORMATION},
      {"double", ExifInterface.TAG_GPS_DOP},
      {"string", ExifInterface.TAG_GPS_DATESTAMP},
      {"double", ExifInterface.TAG_GPS_DEST_BEARING},
      {"string", ExifInterface.TAG_GPS_DEST_BEARING_REF},
      {"double", ExifInterface.TAG_GPS_DEST_DISTANCE},
      {"string", ExifInterface.TAG_GPS_DEST_DISTANCE_REF},
      {"double", ExifInterface.TAG_GPS_DEST_LATITUDE},
      {"string", ExifInterface.TAG_GPS_DEST_LATITUDE_REF},
      {"double", ExifInterface.TAG_GPS_DEST_LONGITUDE},
      {"string", ExifInterface.TAG_GPS_DEST_LONGITUDE_REF},
      {"int", ExifInterface.TAG_GPS_DIFFERENTIAL},
      {"double", ExifInterface.TAG_GPS_IMG_DIRECTION},
      {"string", ExifInterface.TAG_GPS_IMG_DIRECTION_REF},
      {"string", ExifInterface.TAG_GPS_LATITUDE_REF},
      {"string", ExifInterface.TAG_GPS_LONGITUDE_REF},
      {"string", ExifInterface.TAG_GPS_MAP_DATUM},
      {"string", ExifInterface.TAG_GPS_MEASURE_MODE},
      {"string", ExifInterface.TAG_GPS_PROCESSING_METHOD},
      {"string", ExifInterface.TAG_GPS_SATELLITES},
      {"double", ExifInterface.TAG_GPS_SPEED},
      {"string", ExifInterface.TAG_GPS_SPEED_REF},
      {"string", ExifInterface.TAG_GPS_STATUS},
      {"string", ExifInterface.TAG_GPS_TIMESTAMP},
      {"double", ExifInterface.TAG_GPS_TRACK},
      {"string", ExifInterface.TAG_GPS_TRACK_REF},
      {"string", ExifInterface.TAG_GPS_VERSION_ID},
      {"string", ExifInterface.TAG_INTEROPERABILITY_INDEX},
      {"int", ExifInterface.TAG_THUMBNAIL_IMAGE_LENGTH},
      {"int", ExifInterface.TAG_THUMBNAIL_IMAGE_WIDTH},
      {"int", ExifInterface.TAG_DNG_VERSION},
      {"int", ExifInterface.TAG_DEFAULT_CROP_SIZE},
      {"int", ExifInterface.TAG_ORF_PREVIEW_IMAGE_START},
      {"int", ExifInterface.TAG_ORF_PREVIEW_IMAGE_LENGTH},
      {"int", ExifInterface.TAG_ORF_ASPECT_FRAME},
      {"int", ExifInterface.TAG_RW2_SENSOR_BOTTOM_BORDER},
      {"int", ExifInterface.TAG_RW2_SENSOR_LEFT_BORDER},
      {"int", ExifInterface.TAG_RW2_SENSOR_RIGHT_BORDER},
      {"int", ExifInterface.TAG_RW2_SENSOR_TOP_BORDER},
      {"int", ExifInterface.TAG_RW2_ISO},
  };

  // Run all events on native modules queue thread since they might be fired
  // from other non RN threads.

  // Utilities

  public static WritableMap takePictureHelper(byte[] mImageData, Promise mPromise, ReadableMap mOptions, File mCacheDirectory, int mDeviceOrientation, int mSoftwareRotation) {
    WritableMap response = Arguments.createMap();
    ByteArrayInputStream inputStream = null;
    ExifInterface exifInterface = null;
    WritableMap exifData = null;
    ReadableMap exifExtraData = null;
    Bitmap mBitmap = BitmapFactory.decodeByteArray(mImageData, 0, mImageData.length);

    boolean exifOrientationFixed = false;

    response.putInt("deviceOrientation", mDeviceOrientation);
    response.putInt("pictureOrientation", mOptions.hasKey("orientation") ? mOptions.getInt("orientation") : mDeviceOrientation);


    try{
      // this replaces the skipProcessing flag, we will process only if needed, and in
      // an orderly manner, so that skipProcessing is the default behaviour if no options are given
      // and this behaves more like the iOS version.
      // We will load all data lazily only when needed.

      // this should not incur in any overhead if not read/used
      inputStream = new ByteArrayInputStream(mImageData);

      if (mSoftwareRotation != 0) {
        mBitmap = getRotatedBitmap(mBitmap, mSoftwareRotation);
      }

      // Rotate the bitmap to the proper orientation if requested
      if(mOptions.hasKey("fixOrientation") && mOptions.getBoolean("fixOrientation")){
        exifInterface = new ExifInterface(inputStream);

        // Get orientation of the image from mImageData via inputStream
        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        if(orientation != ExifInterface.ORIENTATION_UNDEFINED && getImageRotation(orientation) != 0) {
          int angle = getImageRotation(orientation);
          mBitmap = getRotatedBitmap(mBitmap, angle);
          exifOrientationFixed = true;
        }
      }

      if (mOptions.hasKey("width")) {
        mBitmap = resizeBitmap(mBitmap, mOptions.getInt("width"));
      }

      if (mOptions.hasKey("mirrorImage") && mOptions.getBoolean("mirrorImage")) {
        mBitmap = flipHorizontally(mBitmap);
      }


      // EXIF code - we will adjust exif info later if we manipulated the bitmap
      boolean writeExifToResponse = mOptions.hasKey("exif") && mOptions.getBoolean("exif");

      // default to true if not provided so it is consistent with iOS and with what happens if no
      // processing is done and the image is saved as is.
      boolean writeExifToFile = true;

      if (mOptions.hasKey("writeExif")) {
        switch (mOptions.getType("writeExif")) {
          case Boolean:
            writeExifToFile = mOptions.getBoolean("writeExif");
            break;
          case Map:
            exifExtraData = mOptions.getMap("writeExif");
            writeExifToFile = true;
            break;
        }
      }

      // Read Exif data if needed
      if (writeExifToResponse || writeExifToFile) {

        // if we manipulated the image, or need to add extra data, or need to add it to the response,
        // then we need to load the actual exif data.
        // Otherwise we can just use w/e exif data we have right now in our byte array
        if(mBitmap != null || exifExtraData != null || writeExifToResponse){
          if(exifInterface == null){
            exifInterface = new ExifInterface(inputStream);
          }
          exifData = HelperFunctions.getExifData(exifInterface);

          if(exifExtraData != null){
            exifData.merge(exifExtraData);
          }
        }

        // if we did anything to the bitmap, adjust exif
        if(mBitmap != null){
          exifData.putInt("width", mBitmap.getWidth());
          exifData.putInt("height", mBitmap.getHeight());

          if(exifOrientationFixed){
            exifData.putInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
          }
        }

        // Write Exif data to the response if requested
        if (writeExifToResponse) {
          final WritableMap exifDataCopy = Arguments.createMap();
          exifDataCopy.merge(exifData);
          response.putMap("exif", exifDataCopy);
        }
      }



      // final processing
      // Based on whether or not we loaded the full bitmap into memory, final processing differs
      if(mBitmap == null){
        // set response dimensions. If we haven't read our bitmap, get it efficiently
        // without loading the actual bitmap into memory
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(mImageData, 0, mImageData.length, options);
        if(options != null){
          response.putInt("width", options.outWidth);
          response.putInt("height", options.outHeight);
        }


        // save to file if requested
        if (!mOptions.hasKey("doNotSave") || !mOptions.getBoolean("doNotSave")) {

          // Prepare file output
          File imageFile = new File(getImagePath(mOptions, mCacheDirectory));

          imageFile.createNewFile();

          FileOutputStream fOut = new FileOutputStream(imageFile);

          // Save byte array (it is already a JPEG)
          fOut.write(mImageData);
          fOut.flush();
          fOut.close();

          // update exif data if needed.
          // Since we didn't modify the image, we only update if we have extra exif info
          if (writeExifToFile && exifExtraData != null) {
            ExifInterface fileExifInterface = new ExifInterface(imageFile.getAbsolutePath());
            HelperFunctions.setExifData(fileExifInterface, exifExtraData);
            fileExifInterface.saveAttributes();
          }
          else if (!writeExifToFile){
            // if we were requested to NOT store exif, we actually need to
            // clear the exif tags
            ExifInterface fileExifInterface = new ExifInterface(imageFile.getAbsolutePath());
            HelperFunctions.clearExifData(fileExifInterface);
            fileExifInterface.saveAttributes();
          }
          // else: exif is unmodified, no need to update anything

          // Return file system URI
          String fileUri = Uri.fromFile(imageFile).toString();
          response.putString("uri", fileUri);
        }

        if (mOptions.hasKey("base64") && mOptions.getBoolean("base64")) {
          response.putString("base64", Base64.encodeToString(mImageData, Base64.NO_WRAP));
        }

      }
      else{
        // get response dimensions right from the bitmap if we have it
        response.putInt("width", mBitmap.getWidth());
        response.putInt("height", mBitmap.getHeight());

        // Cache compressed image in imageStream
        ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
        if (!mBitmap.compress(Bitmap.CompressFormat.JPEG, getQuality(mOptions), imageStream)) {
          mPromise.reject(ERROR_TAG, "Could not compress image to JPEG");
          return null;
        }

        // Write compressed image to file in cache directory unless otherwise specified
        if (!mOptions.hasKey("doNotSave") || !mOptions.getBoolean("doNotSave")) {
          String filePath = writeStreamToFile(imageStream, mOptions, mCacheDirectory);

          // since we lost any exif data on bitmap creation, we only need
          // to add it if requested
          if (writeExifToFile && exifData != null) {
            ExifInterface fileExifInterface = new ExifInterface(filePath);
            HelperFunctions.setExifData(fileExifInterface, exifData);
            fileExifInterface.saveAttributes();
          }
          File imageFile = new File(filePath);
          String fileUri = Uri.fromFile(imageFile).toString();
          response.putString("uri", fileUri);
        }

        // Write base64-encoded image to the response if requested
        if (mOptions.hasKey("base64") && mOptions.getBoolean("base64")) {
          response.putString("base64", Base64.encodeToString(imageStream.toByteArray(), Base64.NO_WRAP));
        }

      }

      return response;

    }
    catch (Resources.NotFoundException e) {
      mPromise.reject(ERROR_TAG, "Documents directory of the app could not be found.", e);
      e.printStackTrace();
    }
    catch (IOException e) {
      mPromise.reject(ERROR_TAG, "An unknown I/O exception has occurred.", e);
      e.printStackTrace();
    }
    finally {
      try {
        if (inputStream != null) {
          inputStream.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return null;
  }

  private static Bitmap getRotatedBitmap(Bitmap source, int angle) {
    Matrix matrix = new Matrix();
    matrix.postRotate(angle);
    return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
  }

  private static int getImageRotation(int orientation) {
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

  private static Bitmap resizeBitmap(Bitmap bm, int newWidth) {
    int width = bm.getWidth();
    int height = bm.getHeight();
    float scaleRatio = (float) newWidth / (float) width;

    return Bitmap.createScaledBitmap(bm, newWidth, (int) (height * scaleRatio), true);
  }

  private static Bitmap flipHorizontally(Bitmap source) {
    Matrix matrix = new Matrix();
    matrix.preScale(-1.0f, 1.0f);
    return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
  }

  private static String getImagePath(ReadableMap mOptions, File mCacheDirectory) throws IOException{
    if(mOptions.hasKey("path")){
      return mOptions.getString("path");
    }
    return RNFileUtils.getOutputFilePath(mCacheDirectory, ".jpg");
  }

  private static String writeStreamToFile(ByteArrayOutputStream imageDataStream, ReadableMap mOptions, File mChacheDirectory) throws IOException {
    String outputPath = null;
    IOException exception = null;
    FileOutputStream fileOutputStream = null;

    try {
      outputPath = getImagePath(mOptions, mChacheDirectory);
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

  public static WritableMap getExifData(ExifInterface exifInterface) {
    WritableMap exifMap = Arguments.createMap();
    for (String[] tagInfo : exifTags) {
      String name = tagInfo[1];
      if (exifInterface.getAttribute(name) != null) {
        String type = tagInfo[0];
        switch (type) {
          case "string":
            exifMap.putString(name, exifInterface.getAttribute(name));
            break;
          case "int":
            exifMap.putInt(name, exifInterface.getAttributeInt(name, 0));
            break;
          case "double":
            exifMap.putDouble(name, exifInterface.getAttributeDouble(name, 0));
            break;
        }
      }
    }

    double[] latLong = exifInterface.getLatLong();
    if (latLong != null) {
      exifMap.putDouble(ExifInterface.TAG_GPS_LATITUDE, latLong[0]);
      exifMap.putDouble(ExifInterface.TAG_GPS_LONGITUDE, latLong[1]);
      exifMap.putDouble(ExifInterface.TAG_GPS_ALTITUDE, exifInterface.getAltitude(0));
    }

    return exifMap;
  }

  private static int getQuality(ReadableMap mOptions) {
    return (int) (mOptions.getDouble("quality") * 100);
  }

  public static void setExifData(ExifInterface exifInterface, ReadableMap exifMap) {
    for (String[] tagInfo : exifTags) {
      String name = tagInfo[1];
      if (exifMap.hasKey(name)) {
        String type = tagInfo[0];
        switch (type) {
          case "string":
            exifInterface.setAttribute(name, exifMap.getString(name));
            break;
          case "int":
            exifInterface.setAttribute(name, Integer.toString(exifMap.getInt(name)));
            exifMap.getInt(name);
            break;
          case "double":
            exifInterface.setAttribute(name, Double.toString(exifMap.getDouble(name)));
            exifMap.getDouble(name);
            break;
        }
      }
    }

    if (exifMap.hasKey(ExifInterface.TAG_GPS_LATITUDE) && exifMap.hasKey(ExifInterface.TAG_GPS_LONGITUDE)) {
      exifInterface.setLatLong(exifMap.getDouble(ExifInterface.TAG_GPS_LATITUDE),
                               exifMap.getDouble(ExifInterface.TAG_GPS_LONGITUDE));
    }
    if(exifMap.hasKey(ExifInterface.TAG_GPS_ALTITUDE)){
      exifInterface.setAltitude(exifMap.getDouble(ExifInterface.TAG_GPS_ALTITUDE));
    }
  }

  // clears exif values in place
  public static void clearExifData(ExifInterface exifInterface) {
    for (String[] tagInfo : exifTags) {
      exifInterface.setAttribute(tagInfo[1], null);
    }

    // these are not part of our tag list, remove by hand
    exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null);
    exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null);
    exifInterface.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, null);
  }
}
