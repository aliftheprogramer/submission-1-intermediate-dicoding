package com.example.myapplication.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.Manifest
import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.FileProvider
import com.example.myapplication.data.AuthRepository
import com.example.myapplication.databinding.ActivityAddStoryBinding
import com.example.myapplication.response.AddStoryResponse
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import android.graphics.Bitmap
import android.graphics.BitmapFactory


@AndroidEntryPoint
class AddStoryActivity : AppCompatActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    private lateinit var binding: ActivityAddStoryBinding
    private lateinit var sharedPreferences: SharedPreferences
    private var selectedImageUri: Uri? = null
    private var currentPhotoPath: String? = null

    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var imageCapture: ImageCapture? = null


    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            captureImage() // All permissions granted
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        setupWindowInsets()
        setupSharedPreferences()
        setupButtonListeners()
        startCamera() // Initialize the camera when the activity starts

        Log.d("AddStoryActivity", "onCreate: Photo Path: $currentPhotoPath")
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupSharedPreferences() {
        sharedPreferences = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)
        val name = sharedPreferences.getString("name", null)

        if (token == null) {
            Log.e("AddStoryActivity", "setupSharedPreferences: Token not found")
        } else {
            Log.d("AddStoryActivity", "setupSharedPreferences: Token retrieved: $token")
        }

        if (name == null) {
            Log.e("AddStoryActivity", "setupSharedPreferences: Name not found")
        } else {
            Log.d("AddStoryActivity", "setupSharedPreferences: Name retrieved: $name")
        }
    }


    private fun setupButtonListeners() {
        binding.buttonSelectImage.setOnClickListener { selectImage() }
        binding.buttonCamera.setOnClickListener { captureImage() }
        binding.buttonAddStory.setOnClickListener { addStory() }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        selectImageLauncher.launch(intent)
    }

    private fun startCamera() {
        Log.d("AddStoryActivity", "startCamera: Initializing camera")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                Log.d("AddStoryActivity", "startCamera: Camera provider retrieved")

                val preview = Preview.Builder().build()

                binding.textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                        Log.d("AddStoryActivity", "startCamera: Surface texture available")
                    }

                    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                        Log.d("AddStoryActivity", "startCamera: Surface texture destroyed")
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
                    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {}
                }

                imageCapture = ImageCapture.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                Log.d("AddStoryActivity", "startCamera: Camera bound to lifecycle")
            } catch (exc: Exception) {
                Log.e("AddStoryActivity", "startCamera: Camera initialization failed: ${exc.message}", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }


    private fun captureImage() {
        Log.d("AddStoryActivity", "captureImage: Checking camera permission")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.d("AddStoryActivity", "captureImage: Camera permission not granted")
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            Log.d("AddStoryActivity", "captureImage: Permission granted")
            val photoFile: File? = createImageFile()
            if (photoFile != null) {
                val photoURI: Uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", photoFile)
                Log.d("AddStoryActivity", "captureImage: Photo URI created: $photoURI")

                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                }
                requestImageCapture.launch(intent)
            } else {
                Log.e("AddStoryActivity", "captureImage: Failed to create photo file")
            }
        }
    }


    private fun addStory() {
        val description = binding.edStoryDescription.text.toString()
        if (description.isBlank()) {
            Log.e("AddStoryActivity", "addStory: Description is empty")
            Toast.makeText(this, "Description cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val token = sharedPreferences.getString("token", null)
        if (token == null) {
            Log.e("AddStoryActivity", "addStory: Token is null")
            return
        }

        if (currentPhotoPath == null || selectedImageUri == null) {
            Log.e("AddStoryActivity", "addStory: No image selected")
            return
        }

        val file = File(currentPhotoPath!!)
        if (!file.exists()) {
            Log.e("AddStoryActivity", "File not found at path: $currentPhotoPath")
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
            return
        } else {
            Log.d("AddStoryActivity", "File found at path: $currentPhotoPath")
        }

        val compressedFile = compressImage(file.path)
        if (compressedFile == null || !compressedFile.exists()) {
            Log.e("AddStoryActivity", "Failed to compress image")
            Toast.makeText(this, "Failed to compress image", Toast.LENGTH_SHORT).show()
            return
        }

        val descriptionPart = RequestBody.create("text/plain".toMediaTypeOrNull(), description)
        val filePart = MultipartBody.Part.createFormData(
            "photo",
            compressedFile.name,
            RequestBody.create("image/*".toMediaTypeOrNull(), compressedFile)
        )

        authRepository.addStory(token, descriptionPart, filePart)
            .enqueue(object : Callback<AddStoryResponse> {
                override fun onResponse(
                    call: Call<AddStoryResponse>,
                    response: Response<AddStoryResponse>
                ) {
                    if (response.isSuccessful && response.body()?.error == false) {
                        Toast.makeText(this@AddStoryActivity, "Story added", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@AddStoryActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Log.e("AddStoryActivity", "Error: ${response.errorBody()?.string()}")
                        Toast.makeText(this@AddStoryActivity, "Failed to add story", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<AddStoryResponse>, t: Throwable) {
                    Log.e("AddStoryActivity", "Error: ${t.message}", t)
                    Toast.makeText(this@AddStoryActivity, "Failed to add story: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun createImageFile(): File? {
        return try {
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
            Log.d("AddStoryActivity", "createImageFile: Storage directory: $storageDir")

            if (!storageDir.exists()) {
                if (!storageDir.mkdirs()) {
                    Log.e("AddStoryActivity", "createImageFile: Failed to create directory")
                    return null
                }
            }

            File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            ).apply {
                currentPhotoPath = absolutePath
                Log.d("AddStoryActivity", "createImageFile: File created at $currentPhotoPath")
            }
        } catch (ex: IOException) {
            Log.e("AddStoryActivity", "createImageFile: Error creating file: ${ex.message}", ex)
            null
        }
    }


    private val requestImageCapture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = Uri.parse(currentPhotoPath)
            selectedImageUri = imageUri
            binding.imageView.setImageURI(imageUri)
            if (selectedImageUri == null) {
                Log.e("AddStoryActivity", "Selected Image URI is null")
            } else {
                Log.d("AddStoryActivity", "Selected Image URI: $selectedImageUri")
            }
        } else {
            Log.d("AddStoryActivity", "Image capture failed or cancelled")
        }
    }

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let { uri ->
                val photoFile = createImageFile()
                if (photoFile != null) {
                    val inputStream = contentResolver.openInputStream(uri)
                    val outputStream = FileOutputStream(photoFile)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()
                    currentPhotoPath = photoFile.absolutePath
                    binding.imageView.setImageURI(Uri.fromFile(photoFile))
                    Log.d("AddStoryActivity", "Selected Image URI: $selectedImageUri")
                } else {
                    Log.e("AddStoryActivity", "Failed to create image file")
                }
            }
        } else {
            Log.d("AddStoryActivity", "No image selected")
        }
    }

    private fun compressImage(filePath: String, maxSize: Long = 1 * 1024 * 1024): File? {
        val file = File(filePath)
        if (!file.exists()) return null

        // Check if the file size is already below the maximum size
        if (file.length() <= maxSize) {
            return file
        }

        // Decode the image file into a Bitmap
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        if (bitmap == null) {
            Log.e("AddStoryActivity", "Failed to decode file into Bitmap")
            return null
        }

        // Create a temporary file to save the compressed image
        val tempFile = File(file.parent, "compressed_${file.name}")

        var quality = 100
        var fileSize: Long

        do {
            val outputStream = FileOutputStream(tempFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.flush()
            outputStream.close()
            fileSize = tempFile.length()

            quality -= 10
        } while (fileSize > maxSize && quality > 10)

        return if (fileSize <= maxSize) tempFile else null
    }

}