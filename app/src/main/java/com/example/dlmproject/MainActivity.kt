package com.example.dlmproject

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class MainActivity : AppCompatActivity() {

    private lateinit var myBitmap: Bitmap
    private lateinit var result: TextView
    private lateinit var chosenImage: ImageView
    private lateinit var addButton: Button
    private lateinit var checkButton: Button
    private lateinit var captureButton: Button
    private lateinit var switchMain: ImageView

    private val REQUEST_CODE_PERMISSION_CAMERA = 11
    private val REQUEST_CODE_PERMISSION_STORAGE = 12
    private val REQUIRED_PERMISSIONS_CAMERA = arrayOf(Manifest.permission.CAMERA)
    private val REQUIRED_PERMISSIONS_STORAGE = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    private lateinit var interpreter: Interpreter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        chosenImage = findViewById(R.id.AddedImage)
        result = findViewById(R.id.ResultText)
        addButton = findViewById(R.id.AddButton)
        captureButton = findViewById(R.id.CaptureButton)
        checkButton = findViewById(R.id.MainCheckButton)
        switchMain = findViewById(R.id.switchMain)

        // Load the TensorFlow Lite model (.tflite file)
        interpreter = loadModelFile("model.tflite")

        // Set click listeners
        addButton.setOnClickListener {
            if (checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                openGallery()
            } else {
                requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_CODE_PERMISSION_STORAGE)
            }
        }

        captureButton.setOnClickListener {
            if (checkPermission(Manifest.permission.CAMERA)) {
                openCamera()
            } else {
                requestPermission(Manifest.permission.CAMERA, REQUEST_CODE_PERMISSION_CAMERA)
            }
        }

        switchMain.setOnClickListener{
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        checkButton.setOnClickListener {
            if (::myBitmap.isInitialized) {
                try {
                    // Resize bitmap to match model input size
                    val scaledBitmap = Bitmap.createScaledBitmap(myBitmap, 256, 256, true)

                    // Convert bitmap to float array and normalize
                    val input = preprocessBitmap(scaledBitmap)

                    // Create TensorBuffer for input tensor
                    val inputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 256, 256, 3), DataType.FLOAT32)
                    inputBuffer.loadArray(input)

                    // Create TensorBuffer for output tensor
                    val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 1), DataType.FLOAT32)

                    // Run model inference
                    interpreter.run(inputBuffer.buffer.rewind(), outputBuffer.buffer.rewind())

                    // Get the prediction result
                    val prediction = outputBuffer.floatArray[0]
                    result.text = if (prediction > 0.5) "The lesion shows no signs of melanoma" else "The lesion shows signs of melanoma"

                    // Display success message
                    Toast.makeText(
                        baseContext,
                        "Prediction successful.",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        baseContext,
                        "Exception occurred: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    baseContext,
                    "Bitmap not initialized.",
                    Toast.LENGTH_SHORT
                ).show()
                result.text = "Bitmap not initialized"
            }
        }

        // Request permissions if not granted
        if (!allPermissionsGranted()) {
            requestPermissions()
        }
    }

    private fun preprocessBitmap(bitmap: Bitmap): FloatArray {
        val input = FloatArray(256 * 256 * 3)
        for (y in 0 until 256) {
            for (x in 0 until 256) {
                val pixel = bitmap.getPixel(x, y)
                val index = (y * 256 + x) * 3
                input[index] = ((pixel shr 16 and 0xFF) / 255.0f)
                input[index + 1] = ((pixel shr 8 and 0xFF) / 255.0f)
                input[index + 2] = ((pixel and 0xFF) / 255.0f)
            }
        }
        return input
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in REQUIRED_PERMISSIONS_STORAGE) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS_STORAGE, REQUEST_CODE_PERMISSION_STORAGE)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, 10)
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, 12)
    }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(baseContext, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_PERMISSION_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_CODE_PERMISSION_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    Toast.makeText(this, "Storage permission denied.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                12 -> {
                    val bitmap = data?.extras?.get("data") as Bitmap?
                    bitmap?.let {
                        myBitmap = it
                        chosenImage.setImageBitmap(it)
                    }
                }
                10 -> {
                    val uri = data?.data
                    try {
                        uri?.let {
                            myBitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                            chosenImage.setImageBitmap(myBitmap)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun loadModelFile(modelPath: String): Interpreter {
        val assetFileDescriptor = assets.openFd(modelPath)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val mappedByteBuffer: MappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        return Interpreter(mappedByteBuffer)
    }
}
