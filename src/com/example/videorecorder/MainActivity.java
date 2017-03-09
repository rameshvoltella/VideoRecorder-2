package com.example.videorecorder;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.example.videorecorder.R.id;

public class MainActivity extends ActionBarActivity {
	private static final String TAG = "MainActivity";
	private Camera mCamera;
	private SurfaceView mPreview;//视频预览
	private MediaRecorder mMediaRecorder;
	private boolean isRecording = false;

	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	private Button captureButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉标题栏
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏
		// 设置横屏显示
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		// 选择支持半透明模式,在有surfaceview的activity中使用。
		getWindow().setFormat(PixelFormat.TRANSLUCENT);
		setContentView(R.layout.activity_main);

		mPreview = (SurfaceView) findViewById(R.id.camera_preview);
		captureButton = (Button) findViewById(id.button_capture);
		captureButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isRecording) {
					// stop recording and release camera

					if (mMediaRecorder != null) {
						try {
							// 下面三个参数必须加，不加的话会奔溃，在mediarecorder.stop();
							// 报错为：RuntimeException:stop failed
							mMediaRecorder.setOnErrorListener(null);
							mMediaRecorder.setOnInfoListener(null);
							mMediaRecorder.setPreviewDisplay(null);
							mMediaRecorder.stop();
						} catch (IllegalStateException e) {
							// TODO: handle exception
							Log.i("Exception", Log.getStackTraceString(e));
						} catch (RuntimeException e) {
							// TODO: handle exception
							Log.i("Exception", Log.getStackTraceString(e));
						} catch (Exception e) {
							// TODO: handle exception
							Log.i("Exception", Log.getStackTraceString(e));
						}

					}
					// mMediaRecorder.stop(); // stop the recording
					releaseMediaRecorder(); // release the MediaRecorder object
					mCamera.lock(); // take camera access back from
									// MediaRecorder

					// inform the user that recording has stopped
					setCaptureButtonText("Capture");
					isRecording = false;
				} else {
					if (!checkCameraHardware(MainActivity.this)) {
						Log.d(TAG, "不支持");
						Toast.makeText(MainActivity.this, "不支持",
								Toast.LENGTH_SHORT).show();
						return;
					}

					// initialize video camera
					if (prepareVideoRecorder()) {
						// Camera is available and unlocked, MediaRecorder is
						// prepared,
						// now you can start recording
						mMediaRecorder.start();

						// inform the user that recording has started
						setCaptureButtonText("Stop");
						isRecording = true;
					} else {
						// prepare didn't work, release the camera
						releaseMediaRecorder();
						// inform user
					}
				}
			}
		});
	}

	protected void setCaptureButtonText(String string) {
		captureButton.setText(string);
	}

	/** A safe way to get an instance of the Camera object. */
	public Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
		}
		return c; // returns null if camera is unavailable
	}

	/**
	 * 录像机初始化
	 * @return
	 */
	@SuppressLint({ "InlinedApi", "NewApi" })
	private boolean prepareVideoRecorder() {
		mMediaRecorder = new MediaRecorder();
		mCamera = getCameraInstance();
		// Step 1: Unlock and set camera to MediaRecorder
		mCamera.unlock();
		mMediaRecorder.setCamera(mCamera);

		// Step 2: Set sources
		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		// 输出格式和编码格式
		mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

		mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		//
		mMediaRecorder.setVideoSize(640, 480);
		//
		mMediaRecorder.setVideoEncodingBitRate(1 * 1024 * 1024);

		mMediaRecorder.setVideoFrameRate(30);
		mMediaRecorder.setOrientationHint(90);
		// 设置记录会话的最大持续时间（毫秒）
		mMediaRecorder.setMaxDuration(30 * 1000);
		// 保存的地址
		mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO)
				.toString());

		// 设置预览
		mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

		// 准备录制
		try {
			mMediaRecorder.prepare();
		} catch (IllegalStateException e) {
			Log.d(TAG,
					"IllegalStateException preparing MediaRecorder: "
							+ e.getMessage());
			releaseMediaRecorder();
			return false;
		} catch (IOException e) {
			Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder();
			return false;
		}
		return true;
	}

	//检查设备是否有照相机
	private boolean checkCameraHardware(Context context) {
		if (context.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA)) {
			// this device has a camera
			return true;
		} else {
			// no camera on this device
			return false;
		}
	}

	

	/** Create a file Uri for saving an image or video */
	private static Uri getOutputMediaFileUri(int type) {
		return Uri.fromFile(getOutputMediaFile(type));
	}

	//创建文件保存视频或图片
	private static File getOutputMediaFile(int type) {
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.

		File mediaStorageDir = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				"MyCameraApp");
		// This location works best if you want the created images to be shared
		// between applications and persist after your app has been uninstalled.

		// Create the storage directory if it does not exist
		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				Log.d("MyCameraApp", "failed to create directory");
				return null;
			}
		}

		// Create a media file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());
		File mediaFile;
		if (type == MEDIA_TYPE_IMAGE) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator
					+ "IMG_" + timeStamp + ".jpg");
		} else if (type == MEDIA_TYPE_VIDEO) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator
					+ "VID_" + timeStamp + ".mp4");
		} else {
			return null;
		}

		return mediaFile;
	}

	@Override
	protected void onPause() {
		super.onPause();
		releaseMediaRecorder(); // if you are using MediaRecorder, release it
		releaseCamera(); // release the camera immediately on pause event
	}

	/**
	 * 释放资源
	 */
	private void releaseMediaRecorder() {
		if (mMediaRecorder != null) {
			mMediaRecorder.reset(); // clear recorder configuration
			mMediaRecorder.release(); // release the recorder object
			mMediaRecorder = null;
			mCamera.lock(); // lock camera for later use
		}
	}

	private void releaseCamera() {
		if (mCamera != null) {
			mCamera.release(); // release the camera for other applications
			mCamera = null;
		}
	}
}
