# 안드로이드 NDK 프로젝트  
<br>  

## Main화면  
> c++코드와 bitmap.h 를 이용하여 bitmap memory를 직접 수정한다.  
> color to gray, bright, dark 기능을 구현하였다.


## TensorFlow 화면  
> Tensorflowlite에서 제공하는 "ssd_mobilenet_v1_1_metadata_1.tflite"를 이용하여 실시간 객체감지를 구현했다.  
> 모델은 300 * 300 * 3 크기의 normalized된 buffer를 입력으로 요구한다.  
> CameraX 라이브러리를 사용하여 camera와 PreViewView를 이용하여 미리보기를 구현했다.  
> CameraProvider에 ImageAnalysis도 bind하여 이미지 분석에 사용하였다.  
> 이미지 분석과정 중 RGBA 형식을 RGB로 바꾸고 정규화하는 과정은 c++을 이용한다.  
> - ImageAnalysis 정의시 RGBA 형식으로 output을 내주고 이를 ARGB 형식의 bitmap에 buffer 그대로 복사해 넣는다. 때문에 bitmap을 Byte단위로 접근하여 전처리 할 때 RGBA 형식으로 생각해야 한다.  
<br>  

> 변환 과정을 kotlin으로 한 것과 c++로 한 것에 대해 속도비교를 해봤는데 약간 빠른 것을 확인하였다.  
> 또한 kotlin으로 이미지를 전처리 할 시 c++로 전처리 할때보다 감지 결과가 좋지 않았는데 이는 전처리 과정 중 Bitmap.CreateScaledBitmap에 문제가 있는 것으로 예상한다. 이에 c++의 논리와 같이 처음부터 Byte단위로 변환하여 테스트 예정이다.  
<br>  

## NDKCAMERA 기본예제 화면  
> NDK카메라 라이브러리가 있어 기본 예제를 그대로 적용해봤다.  