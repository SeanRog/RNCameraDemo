// @flow
import React from 'react';
import {
  Platform,
  NativeModules,
  ViewProps,
  requireNativeComponent,
  View,
  StyleSheet,
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

type TrackedFaceFeature = FaceFeature & {
  faceID?: number,
};

type TrackedTextFeature = {
  type: string,
  bounds: {
    size: {
      width: number,
      height: number,
    },
    origin: {
      x: number,
      y: number,
    },
  },
  value: string,
  components: Array<TrackedTextFeature>,
};

type TrackedBarcodeFeature = {
  bounds: {
    size: {
      width: number,
      height: number,
    },
    origin: {
      x: number,
      y: number,
    },
  },
  data: string,
  dataRaw: string,
  type: BarcodeType,
  format?: string,
  addresses?: {
    addressesType?: 'UNKNOWN' | 'Work' | 'Home',
    addressLines?: string[],
  }[],
  emails?: Email[],
  phones?: Phone[],
  urls?: (string[]),
  name?: {
    firstName?: string,
    lastName?: string,
    middleName?: string,
    prefix?: string,
    pronounciation?: string,
    suffix?: string,
    formattedName?: string,
  },
  phone?: Phone,
  organization?: string,
  latitude?: number,
  longitude?: number,
  ssid?: string,
  password?: string,
  encryptionType?: string,
  title?: string,
  url?: string,
  firstName?: string,
  middleName?: string,
  lastName?: string,
  gender?: string,
  addressCity?: string,
  addressState?: string,
  addressStreet?: string,
  addressZip?: string,
  birthDate?: string,
  documentType?: string,
  licenseNumber?: string,
  expiryDate?: string,
  issuingDate?: string,
  issuingCountry?: string,
  eventDescription?: string,
  location?: string,
  organizer?: string,
  status?: string,
  summary?: string,
  start?: string,
  end?: string,
  email?: Email,
  phoneNumber?: string,
  message?: string,
};

type BarcodeType =
  | 'EMAIL'
  | 'PHONE'
  | 'CALENDAR_EVENT'
  | 'DRIVER_LICENSE'
  | 'GEO'
  | 'SMS'
  | 'CONTACT_INFO'
  | 'WIFI'
  | 'TEXT'
  | 'ISBN'
  | 'PRODUCT'
  | 'URL';

type BarcodeFormats = 
	| 'aztec'
  | 'ean13'
	| 'ean8'
	| 'qr'
	| 'pdf417'
	| 'upc_e'
	| 'datamatrix'
	| 'code39'
	| 'code93'
	| 'interleaved2of5'
	| 'codabar'
	| 'code128'
	| 'maxicode'
	| 'rss14'
	| 'rssexpanded'
	| 'upc_a'
	| 'upc_ean'

type Email = {
  address?: string,
  body?: string,
  subject?: string,
  emailType?: 'UNKNOWN' | 'Work' | 'Home',
};

type Phone = {
  number?: string,
  phoneType?: 'UNKNOWN' | 'Work' | 'Home' | 'Fax' | 'Mobile',
};

// refactor any
type EventCallbackArgumentsType = {
  nativeEvent: any,
};

type Rect = {
  x: number,
  y: number,
  width: number,
  height: number,
};

type CameraIds = {
  id: string,
  type: number
}[];

type CameraViewDimensions = {
  width: number,
  height: number,
};

type CameraViewProps = ViewProps & {
  type: number,
  camerId: string,
  ratio: string,
  flashMode: number,
  autoFocus: boolean,
  zoom: number,
  useNativeZoom: boolean,
  barCodeScannerEnabled: boolean,
	googleVisionBarcodeDetectorEnabled: boolean,
  faceDetectorEnabled: boolean,
  faceDetectionMode: number,
  faceDetectionLandmarks: number,
  faceDetectionClassifications: number,
  trackingEnabled: boolean,
  textRecognizerEnabled: boolean,
  rectOfInterest?: Rect, // limits scanning area
  cameraViewDimensions: CameraViewDimensions,
	barCodeTypes?: BarcodeFormats[],
	onCameraReady: ({ nativeEvent }: EventCallbackArgumentsType) => void,
  onBarCodeRead: ({ nativeEvent }: EventCallbackArgumentsType) => void,
  onFacesDetected: ({ nativeEvent }: EventCallbackArgumentsType) => void,
  onTextRecognized: ({ nativeEvent }: EventCallbackArgumentsType) => void,
};

type CameraModuleProps = {
  takePictureAsync: (options: PictureOptions, viewTag: number) => Promise<any>, // refactor any type
  getSupportedRatios: (viewTag: number) => Promise<string[]>,
  getCameraIds: (viewTag: number) => Promise<CameraIds>,
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

export const takePictureAsync = async (options?: PictureOptions) => {
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

	return await CameraModule.takePictureAsync(options, 0);
};

const EventThrottleMs = 500;

const RNCameraView = requireNativeComponent('RNCamera');

const Camera = (props: CameraViewProps) => {
  const _lastEvents: { [event: string]: string } = {};
  const _lastEventsTimes: { [event: string]: number } = {};

  const styles = StyleSheet.create({
    cameraViewContainer: 
    {
      height: '100%',
      width: '100%',
    },
  });

  const getSupportedRatiosAsync = async () => {
    if (Platform.OS === 'android') {
      return await CameraModule.getSupportedRatios(0);
    } else {
      throw new Error('Ratio is not supported on iOS');
    }
  };

  const getCameraIdsAsync = async () => {
    return await CameraModule.getCameraIds(0); // refactor remove paramater
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
