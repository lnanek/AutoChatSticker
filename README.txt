DualLensDemo Notes:

- copy the sample image file to your sd card's root folder e.g.
    adb push imageSample/dualLensSample.jpg /sdcard/

- See the dependencies for the APIs used in this example under the libs folder
    if accessing the DimensionPlusUtility class, you will also need to add to the libs folder:
    armeabi-v7a/libDimensionPlusLib.so
    if accessing DimensionPlusView, you will also need 
    libMatrix and armeabi-v7a/libHMSGallery_libMatrix.jar
    
- This sample requires an HTC One (M8) to run
