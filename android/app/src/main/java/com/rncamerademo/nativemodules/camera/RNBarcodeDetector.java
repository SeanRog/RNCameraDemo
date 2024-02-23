package com.rncamerademo.nativemodules.camera;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;

import com.rncamerademo.nativemodules.camera.utils.ImageDimensions;
import com.rncamerademo.nativemodules.camera.utils.RNFrame;

import java.util.ArrayList;
import java.util.List;

public class RNBarcodeDetector {
    private static final String TAG = "rncameranativemodule";
    public static int NORMAL_MODE = 0;
    public static int ALTERNATE_MODE = 1;
    public static int INVERTED_MODE = 2;
    public static int ALL_FORMATS = Barcode.FORMAT_ALL_FORMATS;

    private BarcodeScanner mBarcodeDetector = null;

    private boolean mIsDetecting = false;
    private ImageDimensions mPreviousDimensions;

    private int mBarcodeType = Barcode.FORMAT_ALL_FORMATS;
    private BarcodeScannerOptions.Builder mBuilder;

    public RNBarcodeDetector(Context context) {
        mBuilder = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(mBarcodeType);
    }

    // Public API

    public boolean isOperational() {
        if (mBarcodeDetector == null) {
            createBarcodeDetector();
        }

        return true;
    }

    public List<Barcode> detect(RNFrame frame) {
        if (mIsDetecting) {
            return new ArrayList<>();
        }
        mIsDetecting = true;
        // If the frame has different dimensions, create another barcode detector.
        // Otherwise we will most likely get nasty "inconsistent image dimensions" error from detector
        // and no barcode will be detected.
        if (!frame.getDimensions().equals(mPreviousDimensions)) {
            releaseBarcodeDetector();
        }

        if (mBarcodeDetector == null) {
            createBarcodeDetector();
            mPreviousDimensions = frame.getDimensions();
        }
        Log.d(TAG, "detecting");
        Log.d(TAG, "frame dimensions:: " + frame.getDimensions().getWidth() + " " + frame.getDimensions().getHeight());
        Task<List<Barcode>> detectBarcodesTask = mBarcodeDetector.process(frame.getFrame());
        handleBarcodeDetectionTask(detectBarcodesTask);
//        mIsDetecting = false;
//        Log.d(TAG, "arcodes.size():: " + barcodes.size());
        return new ArrayList<>();
    }

    private void handleBarcodeDetectionTask(Task<List<Barcode>> detectBarcodesTask) {
        detectBarcodesTask.addOnCompleteListener(completedTask -> {
            mIsDetecting = false;
            List<Barcode> barcodesResult = completedTask.getResult();
            Log.d(TAG, "completed. barcodesResult.size():: " + barcodesResult.size());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                barcodesResult.forEach(barcode -> {
                    Log.d(TAG, barcode.getRawValue());
                });
            }
        });
        detectBarcodesTask.addOnCanceledListener(() -> {
           Log.d(TAG, "canceled");
        });
        detectBarcodesTask.addOnFailureListener((exception) -> {
            Log.d(TAG, "failed. exception.message:: " + exception.getMessage());
        });
        detectBarcodesTask.addOnSuccessListener((barcodeList) -> {
            Log.d(TAG, "success. barcodesResult.size():: " + barcodeList.size());
        });
    }

    public void setBarcodeType(int barcodeType) {
        if (barcodeType != mBarcodeType) {
            release();
            mBuilder.setBarcodeFormats(barcodeType);
            mBarcodeType = barcodeType;
        }
    }


    public void release() {
        releaseBarcodeDetector();
        mPreviousDimensions = null;
    }

    // Lifecycle methods

    private void releaseBarcodeDetector() {
        if (mBarcodeDetector != null) {
            mBarcodeDetector.close();
            mBarcodeDetector = null;
        }
    }

    private void createBarcodeDetector() {
        mBarcodeDetector = BarcodeScanning.getClient(mBuilder.build());
    }
}
