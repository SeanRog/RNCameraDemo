import React from 'react';
import {View, useWindowDimensions} from 'react-native';
import RNCamera from './src/RNCamera';

export default function App() {
	const {width: screenWidth, height: screenHeight} = useWindowDimensions();
  return (
    <View style={{width: '100%', height: '100%', backgroundColor: 'purple'}}>
      <RNCamera
				style={{ height: '100%', width: '100%' }}
				type={0}
				camerId={'0'}
				ratio={'4:3'}
				flashMode={0}
				autoFocus={true}
				zoom={0}
				useNativeZoom={false}
				barCodeScannerEnabled={false}
				googleVisionBarcodeDetectorEnabled={true}
				faceDetectorEnabled={false}
				faceDetectionMode={0}
				barCodeTypes={['code39', 'code93', 'code128', 'qr']}
				faceDetectionLandmarks={0}
				faceDetectionClassifications={0}
				trackingEnabled={true}
				textRecognizerEnabled={false}
				cameraViewDimensions={{
					width: screenWidth,
					height: screenHeight
				}}
				onBarCodeRead={function ({ nativeEvent }: { nativeEvent: any; }): void {
					console.log('onBarcodeRead:: ', nativeEvent);
				}}
				onFacesDetected={function ({ nativeEvent }: { nativeEvent: any; }): void {
					console.log('onFacesDetected:: ', nativeEvent);
				}}
				onTextRecognized={function ({ nativeEvent }: { nativeEvent: any; }): void {
					console.log('onTextRecognized:: ', nativeEvent);
				}} />
    </View>
  );
}
