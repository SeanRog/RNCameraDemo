import React from 'react';
import {Button, Image, View, useWindowDimensions} from 'react-native';
import Camera, {BarcodeFormatsFromEvent, takePictureAsync} from './src/RNCamera';

export default function App() {
	
  return (
    <View ref={undefined} style={{width: '100%', height: '100%', backgroundColor: 'purple'}}>
			<Button title='click me' onPress={async () => console.log(await takePictureAsync({base64: true}))}></Button>
      <Camera
				style={{ height: '100%', width: '100%' }}
				type={0}
				camerId={'0'}
				ratio={'4:3'}
				flashMode={0}
				autoFocus={true}
				zoom={0}
				barcodeReaderEnabled={true}
				faceDetectorEnabled={true}
				faceDetectionMode={1}
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
				{/* <Image style={{width: 400, height: 400}} src='file:///data/user/0/com.rncamerademo/cache/Camera/13ce604e-7f38-412c-9e01-0d21a37e8161.jpg'></Image> */}

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
