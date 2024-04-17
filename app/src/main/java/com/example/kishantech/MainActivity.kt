package com.example.kishantech

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import com.example.kishantech.databinding.ActivityMainBinding
import com.example.kishantech.ml.Model
import com.github.dhaval2404.imagepicker.ImagePicker
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    lateinit var bitmap: Bitmap
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //image processor
//        var imageProcessor=ImageProcessor.Builder()
//            .add(ResizeOp(256, 256, ResizeOp.ResizeMethod.BILINEAR))
//            .build()
        binding.addphoto.setOnClickListener{
            ImagePicker.with(this)
                .crop()	    			//Crop image(Optional), Check Customization for more option
                .compress(1024)			//Final image size will be less than 1 MB(Optional)
                .maxResultSize(256, 256)	//Final image resolution will be less than 1080 x 1080(Optional)
                .start()
        }
    }
    private fun classifyImage(bitmap: Bitmap) {
        val model = Model.newInstance(applicationContext)

    // Creates inputs for reference.
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 256, 256, 3), DataType.FLOAT32)
        val byteBuffer=ByteBuffer.allocateDirect(4*256*256*3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(256 * 256)
        var pixel = 0
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in 0 until 256) {
            for (j in 0 until 256) {
                val v = intValues[pixel++] // RGB
                byteBuffer.putFloat(((v shr 16) and 0xFF) * (1.0f / 1))
                byteBuffer.putFloat(((v shr 8) and 0xFF) * (1.0f / 1))
                byteBuffer.putFloat((v and 0xFF) * (1.0f / 1))
            }
        }

        inputFeature0.loadBuffer(byteBuffer)

    // Runs model inference and gets result.
        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer

        val confidences = outputFeature0.floatArray
    // find the index of the class with the biggest confidence.
        var maxPos = 0
        var maxConfidence = 0f
        for (i in confidences.indices) {
            if (confidences[i] > maxConfidence) {
                maxConfidence = confidences[i]
                maxPos = i
            }
        }
        val classes = arrayOf("potato_Early_blight", "potato_healthy", "potato_late_blight")
        binding.result1.text = classes[maxPos]
    // Releases model resources if no longer used.
        model.close()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        var uri=data?.data
        bitmap=MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
        binding.image.setImageBitmap(bitmap)
        //val bitmap=getBitmapfromview(binding.image)
        classifyImage(bitmap)
    }
}