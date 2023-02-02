package com.example.cameraapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.media.ImageReader
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Handler.Callback
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var capReq: CaptureRequest.Builder
    private lateinit var cameraManager: CameraManager
    private lateinit var handlerThread : HandlerThread
    private lateinit var handler : Handler
    private lateinit var textureView : TextureView
    private lateinit var cameraCaptureSession : CameraCaptureSession
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureRequest: CaptureRequest
    private lateinit var imageReader: ImageReader


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getPermissions()

        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        handlerThread = HandlerThread("cameraThread")
        handlerThread.start()
        handler = Handler((handlerThread as HandlerThread).looper)



        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
            }

        }

        imageReader = ImageReader.newInstance(1080,1920,ImageFormat.JPEG,1)
        imageReader.setOnImageAvailableListener(object : ImageReader.OnImageAvailableListener{
            override fun onImageAvailable(p0: ImageReader?) {
                var image = p0?.acquireLatestImage()
                var buffer = image!!.planes[0].buffer
                var bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)

                var file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),"img1 .jpeg")
                var opStream = FileOutputStream(file)
                opStream.write(bytes)


                image.close()
                Toast.makeText(baseContext, "hello", Toast.LENGTH_SHORT).show()
            }

        },handler)

        findViewById<Button>(R.id.capture).apply {
            setOnClickListener{
                capReq = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                capReq.addTarget(imageReader.surface)
                cameraCaptureSession.capture(capReq.build(),null,null)

            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        cameraManager.openCamera(cameraManager.cameraIdList.get(0), object: CameraDevice.StateCallback(){
            override fun onOpened(p0: CameraDevice) {
                 cameraDevice = p0
                 capReq = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                 var surface = Surface(textureView.surfaceTexture)
                capReq.addTarget(surface)
                cameraDevice.createCaptureSession(listOf(surface,imageReader.surface),object : CameraCaptureSession.StateCallback(){
                    override fun onConfigured(p0: CameraCaptureSession) {
                        cameraCaptureSession = p0
                        cameraCaptureSession.setRepeatingRequest(capReq.build(),null,null)

                    }

                    override fun onConfigureFailed(p0: CameraCaptureSession) {
                    }
                },handler)
            }

            override fun onDisconnected(p0: CameraDevice) {

            }

            override fun onError(p0: CameraDevice, p1: Int) {
            }
        },handler)

    }

    private fun getPermissions() {
        var permissionList = mutableListOf<String>()
        if(checkSelfPermission(android.Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(android.Manifest.permission.CAMERA)
        }
        if(checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if(checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissionList.size>0){
            requestPermissions(permissionList.toTypedArray(),101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        grantResults.forEach {
            if(it !=PackageManager.PERMISSION_GRANTED){
                getPermissions()
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraDevice.close()
        handler.removeCallbacksAndMessages(null)
        handlerThread.quitSafely()
    }
}