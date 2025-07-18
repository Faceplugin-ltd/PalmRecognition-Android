package com.faceplugin.palmrecognition

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
//import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.faceplugin.palm.PalmEngine
import com.faceplugin.palm.PalmResult
import io.fotoapparat.Fotoapparat
import io.fotoapparat.parameter.Resolution
import io.fotoapparat.preview.Frame
import io.fotoapparat.preview.FrameProcessor
import io.fotoapparat.selector.front
import io.fotoapparat.view.CameraView

class CameraActivity : AppCompatActivity() {
  val PREVIEW_WIDTH = 720
  val PREVIEW_HEIGHT = 1280

  private lateinit var cameraView: CameraView
  private lateinit var faceView: FaceView
  private lateinit var fotoapparat: Fotoapparat
  private lateinit var context: Context

  private var recognized = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_camera)

    context = this
    cameraView = findViewById(R.id.preview)
    faceView = findViewById(R.id.faceView)

    fotoapparat = Fotoapparat.with(this)
      .into(cameraView)
      .lensPosition(front())
      .frameProcessor(FaceFrameProcessor())
      .previewResolution { Resolution(PREVIEW_HEIGHT,PREVIEW_WIDTH) }
      .build()

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
      == PackageManager.PERMISSION_DENIED
    ) {
      ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
    } else {
      fotoapparat.start()
    }
  }

  override fun onResume() {
    super.onResume()
    recognized = false
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
      == PackageManager.PERMISSION_GRANTED
    ) {
      fotoapparat.start()
    }
  }

  override fun onPause() {
    super.onPause()
    fotoapparat.stop()
    faceView.setFaceBoxes(null, null)
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == 1) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        == PackageManager.PERMISSION_GRANTED
      ) {
        fotoapparat.start()
      }
    }
  }

  inner class FaceFrameProcessor : FrameProcessor {

    override fun process(frame: Frame) {

      var cameraMode = 7

      val bitmap = PalmEngine.getInstance().yuv2Bitmap(frame.image, frame.size.width, frame.size.height, cameraMode)

      val palmBoxes: List<PalmResult> = PalmEngine.getInstance().FacepluginPalm_detect(bitmap)

      runOnUiThread {
        faceView.setFrameSize(Size(bitmap.width, bitmap.height))
        faceView.setFaceBoxes(palmBoxes, null)
      }

      if(palmBoxes.size > 0) {

        val faceBox = palmBoxes[0]

        val templates = PalmEngine.getInstance().FacepluginPalm_extract(bitmap, faceBox)

        var maxSimiarlity = 0f
        var maximiarlityPerson: Person? = null
        for (person in DBManager.personList) {

          val similarity = PalmEngine.getInstance().FacepluginPalm_similarity(templates, person.templates)


          if (similarity > maxSimiarlity) {
            maxSimiarlity = similarity
            maximiarlityPerson = person
          }
        }

        // default threshold = 0.8
        if (maxSimiarlity > 0.8) {
          recognized = true
          val identifiedPerson = maximiarlityPerson
          val identifiedSimilarity = maxSimiarlity

          if (identifiedPerson != null)
            runOnUiThread {
              faceView.setFrameSize(Size(bitmap.width, bitmap.height))
              faceView.setFaceBoxes(palmBoxes, identifiedPerson.name)
            }
        }
      }
    }
  }
}