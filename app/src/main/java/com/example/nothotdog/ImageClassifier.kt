package com.example.nothotdog

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.ml.custom.FirebaseModelDataType
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions
import com.google.firebase.ml.custom.FirebaseModelInputs
import com.google.firebase.ml.custom.FirebaseModelInterpreter
import com.google.firebase.ml.custom.FirebaseModelManager
import com.google.firebase.ml.custom.FirebaseModelOptions
import com.google.firebase.ml.custom.model.FirebaseLocalModelSource
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Created by Vincent on 10/17/18
 */
class ImageClassifier {

  //  Interpreter that accept input and output data
  private var firebaseInterpreter: FirebaseModelInterpreter
  private var inputOutputOptions: FirebaseModelInputOutputOptions

  init {
    val modelSource = FirebaseLocalModelSource.Builder("NotHotDog")
      .setAssetFilePath("nothotdog.tflite")
      .build()

    FirebaseModelManager.getInstance()
      .registerLocalModelSource(modelSource)

    val modelOptions = FirebaseModelOptions.Builder()
      .setLocalModelName("NotHotDog")
      .build()

    firebaseInterpreter = FirebaseModelInterpreter.getInstance(modelOptions)!!

    inputOutputOptions = FirebaseModelInputOutputOptions.Builder()
      .setInputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, 224, 224, 3))
      .setOutputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, 2))
      .build()
  }

  //224x224 as defined by model
  private val IMAGE_HEIGHT = 224
  private val IMAGE_WIDTH = 224

  //Batch size
  private val BATCH_SIZE = 1

  //RGB
  private val PIXEL_SIZE = 3

  private val IMAGE_MEAN = 128
  private val IMAGE_STD = 128.0f

  fun detectHotdog(bitmap: Bitmap, callback: ((Boolean) -> (Unit))) {
    val scaledBitmap = Bitmap.createScaledBitmap(
      bitmap,
      IMAGE_WIDTH,
      IMAGE_HEIGHT,
      false
    )
    val byteBuffer = convertbitMapToByteBuffer(scaledBitmap)

    val modelInput = FirebaseModelInputs.Builder()
      .add(byteBuffer)
      .build()

    firebaseInterpreter.run(modelInput, inputOutputOptions)
      .addOnSuccessListener { output ->
        val result = output.getOutput<Array<FloatArray>>(0)[0]

        val hotDogProbability = result[0]
        val notHotDogProbability = 1 - hotDogProbability

        Log.d("CLASSIFY RESULT", "HOT DOG PROB : $hotDogProbability")
        Log.d("CLASSIFY RESULT", "NOT HOT DOG PROB : $notHotDogProbability")

        val minimumProbability = 0.8

        val isHotDog = hotDogProbability > minimumProbability

        callback(isHotDog)
      }.addOnFailureListener {
        it.printStackTrace()
      }
  }

  private fun convertbitMapToByteBuffer(bitmap: Bitmap): ByteBuffer {
//    // Convert the image to floating point.
    val imageByteBuffer = ByteBuffer.allocateDirect(
      IMAGE_HEIGHT
          * IMAGE_WIDTH
          * BATCH_SIZE
          * PIXEL_SIZE
          * 4
    )
    imageByteBuffer.order(ByteOrder.nativeOrder())

    val intValues = IntArray(IMAGE_HEIGHT * IMAGE_WIDTH)
    bitmap.getPixels(
      intValues,
      0,
      bitmap.width,
      0,
      0,
      bitmap.width,
      bitmap.height
    )

    var pixel = 0
    for (i in 0 until IMAGE_HEIGHT) {
      for (j in 0 until IMAGE_WIDTH) {
        val currPixel = intValues[pixel++]
        imageByteBuffer.putFloat(((currPixel shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
        imageByteBuffer.putFloat(((currPixel shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
        imageByteBuffer.putFloat(((currPixel and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
      }
    }

    return imageByteBuffer
  }
}