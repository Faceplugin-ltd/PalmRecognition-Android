package com.faceplugin.palmrecognition;

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import com.faceplugin.palmrecognition.MyGlobal.context
import com.faceplugin.palm.PalmEngine
import com.faceplugin.palm.PalmResult
import java.io.ByteArrayOutputStream
import kotlin.random.Random


class MainActivity : AppCompatActivity() {
  companion object {
    private val SELECT_PHOTO_REQUEST_CODE = 1
    private val SELECT_ATTRIBUTE_REQUEST_CODE = 2
  }

  private lateinit var dbManager: DBManager
  private lateinit var textWarning: TextView
  private lateinit var personAdapter: PersonAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout
        .activity_main)

    textWarning = findViewById<TextView>(R.id.textWarning)

    dbManager = DBManager(this)
    dbManager.loadPerson()

    personAdapter = PersonAdapter(this, DBManager.personList)
    val listView: ListView = findViewById<View>(R.id.listPerson) as ListView
    listView.setAdapter(personAdapter)

    findViewById<Button>(R.id.btnEnroll).setOnClickListener {
      val intent = Intent()
      intent.setType("image/*")
      intent.setAction(Intent.ACTION_PICK)
      startActivityForResult(
        Intent.createChooser(intent, getString(R.string.select_picture)),
        SELECT_PHOTO_REQUEST_CODE
      )

    }

    setupFaceSDK()
    setupViews()
  }

  fun setupViews() {
    findViewById<Button>(R.id.btnVerify).setOnClickListener {
      startActivity(Intent(this, CameraActivity::class.java))
    }
  }

  fun setupDB() {
    dbManager.loadPerson()

    personAdapter = PersonAdapter(this, DBManager.personList)
    val listView: ListView = findViewById<View>(R.id.listPerson) as ListView
    listView.setAdapter(personAdapter)
  }

  fun setupFaceSDK() {
    val init_res = PalmEngine.createInstance(this).FacepluginPalm_init()
    if (init_res < 0) {
      Toast.makeText(this, "Engine Init Failed !", Toast.LENGTH_SHORT).show()
    }
  }


  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == SELECT_PHOTO_REQUEST_CODE && resultCode == RESULT_OK) {
      try {
        var bitmap: Bitmap = Utils.getCorrectlyOrientedImage(this, data?.data!!)

        val faceResults: List<PalmResult> =
          PalmEngine.getInstance().FacepluginPalm_detect(bitmap)

        if (faceResults.isNullOrEmpty()) {
          Toast.makeText(this, getString(R.string.no_face_detected), Toast.LENGTH_SHORT)
            .show()
        } else if (faceResults.size > 1) {
          Toast.makeText(
            this,
            getString(R.string.multiple_face_detected),
            Toast.LENGTH_SHORT
          ).show()
        } else {

          val faceImage = Utils.cropFace(bitmap, faceResults[0])
          val templates =
            PalmEngine.getInstance().FacepluginPalm_extract(bitmap, faceResults[0])

          var maxSimiarlity = 0f
          for (person in DBManager.personList) {

            val similarity = PalmEngine.getInstance().FacepluginPalm_similarity(templates, person.templates)
            if (similarity > maxSimiarlity) {
              maxSimiarlity = similarity
            }
          }

          val matchThreshold = 0.8f
          if (maxSimiarlity >= matchThreshold) {
            Toast.makeText(
              this,
              getString(R.string.duplicate_person),
              Toast.LENGTH_SHORT
            ).show()
          } else {

            dbManager.insertPerson(
              "Palm - " + Random.nextInt(10000, 20000),
              faceImage,
              templates
            )

            personAdapter.notifyDataSetChanged()
            Toast.makeText(
              this,
              getString(R.string.person_enrolled),
              Toast.LENGTH_SHORT
            ).show()
          }

        }
      } catch (e: java.lang.Exception) {
        //handle exception
        e.printStackTrace()
      }
    }
  }

}
