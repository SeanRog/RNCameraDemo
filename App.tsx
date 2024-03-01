import React from 'react';
import {Button, Image, View, useWindowDimensions} from 'react-native';
import Camera, {BarcodeFormatsFromEvent, takePictureAsync} from './src/RNCamera';

export default function App() {
	const {width: screenWidth, height: screenHeight} = useWindowDimensions();
	
  return (
    <View ref={undefined} style={{width: '100%', height: '100%', backgroundColor: 'purple'}}>
			<Button title='click me' onPress={async () => console.log(await takePictureAsync())}></Button>
      <Camera
				style={{ height: '100%', width: '100%' }}
				type={0}
				camerId={'0'}
				ratio={'4:3'}
				flashMode={0}
				autoFocus={true}
				zoom={0}
				useNativeZoom={false}
				barCodeScannerEnabled={true}
				googleVisionBarcodeDetectorEnabled={false}
				faceDetectorEnabled={true}
				faceDetectionMode={1}
				barCodeTypes={['code39', 'code93', 'code128', 'qr']}
				onCameraReady={() => {
					console.log('camera ready');
				}}
				faceDetectionLandmarks={1}
				faceDetectionClassifications={1}
				trackingEnabled={true}
				textRecognizerEnabled={true}
				onBarCodeRead={({format, rawValue}) => {
					console.log('barcode event from app.tsx:: ', BarcodeFormatsFromEvent[format] + " " + rawValue);
				}}
				onFacesDetected={(event) => {
					console.log('onFacesDetected event from app.tsx:: ', event);
				}}
				onTextRecognized={(event) => {
					console.log('onTextRecognized event from app.tsx:: ', event);
				}} />
				{/* <Image style={{width: 400, height: 400}} src='file:///data/user/0/com.rncamerademo/cache/Camera/2f874763-9978-4828-a57b-84ad7de583bc.jpg'></Image> */}

				{/* <TouchableOpacity
					onPress={() => console.log('take picture pressed')}
					style={{position: 'absolute', top: 0, width: 200, height: 50, backgroundColor: 'blue'}}>
					<Text>
						TakePicture
					</Text>
				</TouchableOpacity> */}
				
    </View>
  );
}
