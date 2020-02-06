package com.supriya.cropimage

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import android.provider.MediaStore
import android.graphics.Bitmap
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.io.IOException
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.OnProgressListener
import android.widget.Toast
import androidx.annotation.NonNull
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import java.util.UUID.randomUUID
import com.google.firebase.storage.StorageReference
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import java.io.File
import java.util.*


class MainActivity : AppCompatActivity() {
    val PICK_IMAGE_REQUEST =71
    val storage by lazy {
        FirebaseStorage.getInstance()
    }
    val storageReference by lazy {
        storage.reference
    }
    var filePath:Uri ?=null
  //  var file:File?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        btnChoose.setOnClickListener {
           chooseImage()
        }

        btnUpload.setOnClickListener {
           uploadImage()
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                123
            )
        }


        else
        {
            textureView.post { startCamera() }
        }
    }



    fun chooseImage()
    {
        val intent = Intent(Intent.ACTION_PICK)
        intent.setAction(Intent.ACTION_GET_CONTENT )
        intent.type = "image/*"
        startActivityForResult(Intent.createChooser(intent,"Select Picture"), PICK_IMAGE_REQUEST)
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK
            && data != null && data.data != null
        ) {
            filePath = data.data
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                imgView.setImageBitmap(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }



    private fun uploadImage( ) {
//        filePath=Uri.parse(file)


        if (filePath != null) {
            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle("Uploading...")
            progressDialog.show()

            val filename=UUID.randomUUID().toString()
            val ref = storageReference.child("images/$filename")
            ref.putFile(filePath!!)
                .addOnSuccessListener {
                    progressDialog.dismiss()
                    Toast.makeText(this@MainActivity, "Uploaded", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(this@MainActivity, "Failed " + e.message, Toast.LENGTH_SHORT).show()
                }
                .addOnProgressListener { taskSnapshot ->
                    val progress = 100.0 * taskSnapshot.bytesTransferred / taskSnapshot
                        .totalByteCount
                    progressDialog.setMessage("Uploaded " + progress.toInt() + "%")
                }
        }

    }
    fun startCamera(){


        val imageCaptureConfig=ImageCaptureConfig.Builder().apply {
            setTargetAspectRatio(Rational(1,1))
            setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
        }.build()


        val imageCapture=ImageCapture(imageCaptureConfig)

        clickPic.setOnClickListener {
            val file= File(externalMediaDirs.first(),"${System.currentTimeMillis()}.jpg")

            imageCapture.takePicture(file,object:ImageCapture.OnImageSavedListener{
                override fun onImageSaved(file: File) {
                    Toast.makeText(this@MainActivity,"PhotoCaptured${file.absolutePath}",Toast.LENGTH_LONG).show()
                }

                override fun onError(useCaseError: ImageCapture.UseCaseError, message: String, cause: Throwable?) {
                    Toast.makeText(this@MainActivity,"Error Capturing$message",Toast.LENGTH_LONG).show()
                }

            })
        }


        val previewConfig=PreviewConfig.Builder().apply {
            setTargetResolution((Size(1080,1080)))
            setTargetAspectRatio(Rational(1,1))
            setLensFacing(CameraX.LensFacing.BACK)
        }.build()



        val preview= Preview(previewConfig)

        preview.setOnPreviewOutputUpdateListener {
            val parent = textureView.parent as ViewGroup
            parent.removeView(textureView)
            parent.addView(textureView,0)
            updatePreview()
            textureView.surfaceTexture = it.surfaceTexture
        }
        CameraX.bindToLifecycle(this, preview,imageCapture)


    }


    private  fun updatePreview(){
        val matrix= Matrix()
        val centerX=textureView.width/2f
        val centerY=textureView.height/2f

        val rotation=when(textureView.display.rotation){
            Surface.ROTATION_0->0
            Surface.ROTATION_90->90
            Surface.ROTATION_180->180

            Surface.ROTATION_270->270
            else -> return
        }

        matrix.postRotate(-rotation.toFloat(),centerX,centerY)
        textureView.setTransform(matrix)
    }


}
