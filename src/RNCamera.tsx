// @flow
import React, { useEffect } from 'react';
import {
	DeviceEventEmitter,
  Platform,
  NativeModules,
  ViewProps,
  requireNativeComponent,
  View,
  StyleSheet,
	findNodeHandle,
	HostComponent,
	EmitterSubscription,
} from 'react-native';

type Orientation = 'auto' | 'landscapeLeft' | 'landscapeRight' | 'portrait' | 'portraitUpsideDown';
type OrientationNumber = 1 | 2 | 3 | 4;

type PictureOptions = {
  quality?: number,
  orientation?: Orientation | OrientationNumber,
  base64?: boolean,
  mirrorImage?: boolean,
  exif?: boolean,
  writeExif?: boolean | { [name: string]: any },
  width?: number,
  fixOrientation?: boolean,
  forceUpOrientation?: boolean,
  pauseAfterCapture?: boolean,
};

type Point = { x: number, y: number };

type FaceFeature = {
  bounds: {
    size: {
      width: number,
      height: number,
    },
    origin: Point,
  },
  smilingProbability?: number,
  leftEarPosition?: Point,
  rightEarPosition?: Point,
  leftEyePosition?: Point,
  leftEyeOpenProbability?: number,
  rightEyePosition?: Point,
  rightEyeOpenProbability?: number,
  leftCheekPosition?: Point,
  rightCheekPosition?: Point,
  leftMouthPosition?: Point,
  mouthPosition?: Point,
  rightMouthPosition?: Point,
  bottomMouthPosition?: Point,
  noseBasePosition?: Point,
  yawAngle?: number,
  rollAngle?: number,
};

// refactor any
type EventCallbackArgumentsType = {
  nativeEvent: any,
};

type CameraIds = {
  id: string,
  type: number
}[];

type CameraViewProps = ViewProps & {
  type: number,
  camerId: string,
  ratio: string,
  flashMode: number,
  autoFocus: boolean,
  zoom: number,
  barcodeReaderEnabled: boolean,
  faceDetectorEnabled: boolean,
  faceDetectionMode: number,
  faceDetectionLandmarks: number,
  faceDetectionClassifications: number,
  trackingEnabled: boolean,
  textRecognizerEnabled: boolean,
	onCameraReady?: ({ nativeEvent }: EventCallbackArgumentsType) => void,
	onBarCodeRead: (event: barcodeEventData) => void,
	onFacesDetected: (event: faceDetectionEventData) => void,
	onTextRecognized: (event: textRecognizedEventData) => void,
};

export enum BarcodeFormatsFromEvent {
	FORMAT_UNKNOWN = -1,
	FORMAT_ALL_FORMATS = 0,
	FORMAT_CODE_128 = 1,
	FORMAT_CODE_39 = 2,
	FORMAT_CODE_93 = 4,
	FORMAT_CODABAR = 8,
	FORMAT_DATA_MATRIX = 16,
	FORMAT_EAN_13 = 32,
	FORMAT_EAN_8 = 64,
	FORMAT_ITF = 128,
	FORMAT_QR_CODE = 256,
	FORMAT_UPC_A = 512,
	FORMAT_UPC_E = 1024,
	FORMAT_PDF417 = 2048,
	FORMAT_AZTEC = 4096,
	TYPE_UNKNOWN = 0,
	TYPE_CONTACT_INFO = 1,
	TYPE_EMAIL = 2,
	TYPE_ISBN = 3,
	TYPE_PHONE = 4,
	TYPE_PRODUCT = 5,
	TYPE_SMS = 6,
	TYPE_TEXT = 7,
	TYPE_URL = 8,
	TYPE_WIFI = 9,
	TYPE_GEO = 10,
	TYPE_CALENDAR_EVENT = 11,
	TYPE_DRIVER_LICENSE = 12,
};

type barcodeEventData = {
	format: number,
	rawValue: string,
};

type faceDetectionEventData = {
	top: number,
	left: number,
	centerX: number,
	centerY: number,
	width: number,
	height: number,
};

type textRecognizedEventData = {
	textDetected: string,
};

type CameraModuleProps = {
  takePictureAsync: (options: PictureOptions) => Promise<any>, // refactor any type
  getSupportedRatios: () => Promise<string[]>,
  getCameraIds: () => Promise<CameraIds>,
  hasTorch: () => Promise<boolean>,
}

export type RecordAudioPermissionStatus = 'AUTHORIZED' | 'NOT_AUTHORIZED' | 'PENDING_AUTHORIZATION';

const RecordAudioPermissionStatusEnum: {
  [key in RecordAudioPermissionStatus]: RecordAudioPermissionStatus
} = {
  AUTHORIZED: 'AUTHORIZED',
  PENDING_AUTHORIZATION: 'PENDING_AUTHORIZATION',
  NOT_AUTHORIZED: 'NOT_AUTHORIZED',
};

if (!NativeModules.CameraModule) {
  throw Error('NativeModules.RNCameraModule is undefined.');
}
const CameraModule: CameraModuleProps = NativeModules.CameraModule;

export const takePictureAsync = async (options: PictureOptions) => {
	if (!options) {
		options = {};
	}
	if (!options.quality) {
		options.quality = 1;
	}

	if (options.orientation) {
		if (typeof options.orientation !== 'number') {
			const { orientation } = options;
			options.orientation = 1;
			if (__DEV__) {
				if (typeof options.orientation !== 'number') {
					// eslint-disable-next-line no-console
					console.warn(`Orientation '${orientation}' is invalid.`);
				}
			}
		}
	}

	if (options.pauseAfterCapture === undefined) {
		options.pauseAfterCapture = false;
	}

	return await CameraModule.takePictureAsync(options);
};

const EventThrottleMs = 500;
let RNCameraView: HostComponent<unknown> | string;

try {
	RNCameraView = requireNativeComponent('RNCamera');
} catch (e) {
	console.log(e);
	RNCameraView = 'RNCamera';
}

const Camera = (props: CameraViewProps) => {
  const _lastEvents: { [event: string]: string } = {};
  const _lastEventsTimes: { [event: string]: number } = {};

	// start/stop barcode/text/face detect event listeners
	useEffect(() => {
		let barcodeReadListener: EmitterSubscription;
		let facesDetectedListener: EmitterSubscription;
		let textDetectedListener: EmitterSubscription;
		if (props.barcodeReaderEnabled) {
			barcodeReadListener = DeviceEventEmitter.addListener('onBarCodeRead', props.onBarCodeRead);
		}
		if (props.faceDetectorEnabled) {
			facesDetectedListener = DeviceEventEmitter.addListener('onFacesDetected', props.onFacesDetected);
		}
		if (props.textRecognizerEnabled) {
			textDetectedListener = DeviceEventEmitter.addListener('onTextRecognized', props.onTextRecognized);
		}
		
		return () => {
			if (props.barcodeReaderEnabled) {
				barcodeReadListener.remove();
			}
			if (props.faceDetectorEnabled) {
				facesDetectedListener.remove();
			}
			if (props.textRecognizerEnabled) {
				textDetectedListener.remove();
			}
		}
	}, []);
	

  const styles = StyleSheet.create({
    cameraViewContainer: 
    {
      height: '100%',
      width: '100%',
    },
  });

  const getSupportedRatiosAsync = async () => {
    if (Platform.OS === 'android') {
      return await CameraModule.getSupportedRatios();
    } else {
      throw new Error('Ratio is not supported on iOS');
    }
  };

  const getCameraIdsAsync = async () => {
    return await CameraModule.getCameraIds();
  };

  // refactor
  const onObjectDetected = (callback?: Function) => ({ nativeEvent }: EventCallbackArgumentsType) => {
    const { type } = nativeEvent;
    if (
      JSON.stringify(nativeEvent) === _lastEvents[type] && Date.now() - _lastEventsTimes[type] < EventThrottleMs
    ) {
      return;
    }
    if (callback) {
      callback(nativeEvent);
      _lastEventsTimes[type] = Date.now();
      _lastEvents[type] = JSON.stringify(nativeEvent);
    }
  };

  return (
    <View style={styles.cameraViewContainer}>
      <RNCameraView
        {...props}
      />
      {props.children}
    </View>
  )
}

export default Camera;
