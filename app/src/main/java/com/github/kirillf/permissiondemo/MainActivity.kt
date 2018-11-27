package com.github.kirillf.permissiondemo

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.Toast

import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 9993
        private const val REQUEST_IMAGE_CODE = 9997
    }

    private var imageView: ImageView? = null

    private var fileUri: Uri? = null

    private var shouldRequestPermission: Boolean = false

    private val pickerIntent: Intent
        get() {
            val packageManager = packageManager
            val intents = ArrayList<Intent>()
            val capture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val cams = packageManager.queryIntentActivities(capture, 0)
            if (isCameraGranted) {
                for (info in cams) {
                    if (fileUri == null) {
                        fileUri = cameraImageUri
                    }
                    val intent = Intent(capture)
                    intent.component = ComponentName(info.activityInfo.packageName, info.activityInfo.name)
                    intent.setPackage(info.activityInfo.packageName)
                    if (fileUri != null) {
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
                    }
                    intents.add(intent)
                }
            }
            val gallery = Intent(Intent.ACTION_GET_CONTENT)
            gallery.type = "image/*"
            val galleries = packageManager.queryIntentActivities(gallery, 0)
            for (info in galleries) {
                if (isPermitted(info)) {
                    val intent = Intent(gallery)
                    intent.component = ComponentName(info.activityInfo.packageName, info.activityInfo.name)
                    intent.setPackage(info.activityInfo.packageName)
                    intents.add(intent)
                }
            }

            var mainIntent = intents[intents.size - 1]
            for (intent in intents) {
                if (intent.component!!.className == "com.android.documentsui.DocumentsActivity") {
                    mainIntent = intent
                    break
                }
            }
            intents.remove(mainIntent)

            val chooser = Intent.createChooser(mainIntent, "Select source")
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toTypedArray<Parcelable>())

            return chooser
        }

    private val isCameraGranted: Boolean
        get() = !shouldRequestPermission || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private val cameraImageUri: Uri?
        get() {
            return try {
                val imgFile = createTempFile()
                Uri.fromFile(imgFile)
            } catch (e: IOException) {
                null
            }

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val checkBox = findViewById<View>(R.id.check_permission) as CheckBox
        val button = findViewById<View>(R.id.pick_image) as Button
        imageView = findViewById<View>(R.id.image_view) as ImageView

        button.setOnClickListener {
            if (shouldRequestPermission && !isCameraGranted) {
                checkPermission()
            } else {
                startPickerActivity()
            }
        }

        checkBox.setOnCheckedChangeListener { _, isChecked -> shouldRequestPermission = isChecked }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        startPickerActivity()
    }

    private fun startPickerActivity() {
        startActivityForResult(pickerIntent, REQUEST_IMAGE_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val uri = getUri(data)
                imageView!!.setImageURI(uri)
            } else {
                Toast.makeText(this, "Failed to pick image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermission() {
        if (!isCameraGranted) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CAMERA_PERMISSION)
        }
    }

    private fun isPermitted(info: ResolveInfo): Boolean {
        var permission: String? = info.activityInfo.permission
        if (permission == null) {
            permission = info.activityInfo.applicationInfo.permission
        }
        return permission == null || ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun getUri(data: Intent?): Uri? {
        var isCamera = true
        if (data != null && data.data != null) {
            val action = data.action
            isCamera = action != null && action == MediaStore.ACTION_IMAGE_CAPTURE
        }
        return if (isCamera) fileUri else data!!.data
    }

    @Throws(IOException::class)
    private fun createTempFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val fileName = "JPEG_" + timestamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", storageDir)
    }
}
