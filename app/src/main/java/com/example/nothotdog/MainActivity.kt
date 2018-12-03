package com.example.nothotdog

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.btnTakeImage
import kotlinx.android.synthetic.main.activity_main.cameraView

class MainActivity : AppCompatActivity() {

  var hasPermissionGiven = true
  val classifer = ImageClassifier()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    setupCameraView()
  }

  private fun setupCameraView() {
    btnTakeImage.setOnClickListener { _ ->
      cameraView.captureImage { resultImage ->
        runOnUiThread {
          classifer.detectHotdog(resultImage.bitmap, { isHotDog ->
            if (isHotDog) {
              AlertDialog.Builder(this@MainActivity)
                .setMessage("It's a Hot Dog")
                .show()
            } else {
              AlertDialog.Builder(this@MainActivity)
                .setMessage("It's not a Hot Dog")
                .show()
            }
          })
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    if (hasPermissionGiven) {
      cameraView.start()
    }
  }

  override fun onPause() {
    if (hasPermissionGiven) {
      cameraView.stop()
    }
    super.onPause()
  }
}
