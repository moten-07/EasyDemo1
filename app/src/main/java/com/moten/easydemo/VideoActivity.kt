package com.moten.easydemo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.moten.easydemo.databinding.ActivityVideoBinding
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration

/**
 * 视频通话界面
 */
class VideoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVideoBinding
    private lateinit var channelName: String

    private var mRtcEngine: RtcEngine? = null
    private val mRtcEventHandler = object : IRtcEngineEventHandler() {
        // 加入频道
        override fun onUserJoined(uid: Int, elapsed: Int) = runOnUiThread { setupRemoteVideo(uid) }

        // 离开频道
        override fun onUserOffline(uid: Int, reason: Int) =
            runOnUiThread { binding.remoteViewContainer.removeAllViews() }

        // 静音
        override fun onUserMuteVideo(uid: Int, muted: Boolean) =
            runOnUiThread { onRemoteUserVideoMuted(uid, muted) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取频道名
        channelName = intent.getStringExtra("channelName").toString()

        // 权限检查，无录音和摄像头权限不启动
        if (checkSelfPermission(
                Manifest.permission.RECORD_AUDIO,
                PERMISSION_REQ_ID_RECORD_AUDIO
            ) && checkSelfPermission(Manifest.permission.CAMERA, PERMISSION_REQ_ID_CAMERA)
        ) {
            initView()
            initAgoraEngineAndJoinChannel()
        }
    }

    private fun initView() {
        // 控件点击事件，也可使用view.onClickListener，此处使用viewBinding觉得更简洁
        binding.apply {
            localVideoMute.setOnClickListener(this@VideoActivity::onLocalVideoMuteClicked)
            localMute.setOnClickListener(this@VideoActivity::onLocalAudioMuteClicked)
            switchCamera.setOnClickListener { mRtcEngine!!.switchCamera() }
            endVideoCall.setOnClickListener { finish() }
        }
    }

    private fun initAgoraEngineAndJoinChannel() {
        initializeAgoraEngine()
        setupVideoProfile()
        setupLocalVideo()
        joinChannel()
    }

    // 权限检查
    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(permission),
                requestCode
            )
            return false
        }
        return true
    }

    // 检查结果
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        Log.i("VideoActivity", "onRequestPermissionsResult " + grantResults[0] + " " + requestCode)
        when (requestCode) {
            PERMISSION_REQ_ID_RECORD_AUDIO -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    checkSelfPermission(Manifest.permission.CAMERA, PERMISSION_REQ_ID_CAMERA)
                else finish()
            }
            PERMISSION_REQ_ID_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) initAgoraEngineAndJoinChannel()
                else finish()
            }
        }
    }


    // 资源释放
    override fun onDestroy() {
        super.onDestroy()
        mRtcEngine?.leaveChannel()
        RtcEngine.destroy()
        mRtcEngine = null
    }

    // 关闭本地视频推流
    private fun onLocalVideoMuteClicked(view: View) {
        val iv = view as ImageView
        // 设置按钮状态，添加/去除红色遮罩，下同
        if (iv.isSelected) iv.clearColorFilter()
        else iv.setColorFilter(
            ContextCompat.getColor(
                applicationContext,
                R.color.red
            ), PorterDuff.Mode.MULTIPLY
        )
        iv.isSelected = !iv.isSelected

        mRtcEngine!!.muteLocalVideoStream(iv.isSelected)

        val container = findViewById<FrameLayout>(R.id.locale_view_container)
        val surfaceView = container.getChildAt(0) as SurfaceView
        surfaceView.setZOrderMediaOverlay(!iv.isSelected)
        surfaceView.visibility = if (iv.isSelected) View.GONE else View.VISIBLE
    }

    // 关闭本地音频推流
    private fun onLocalAudioMuteClicked(view: View) {
        val iv = view as ImageView
        if (iv.isSelected) iv.clearColorFilter()
        else iv.setColorFilter(
            ContextCompat.getColor(applicationContext, R.color.red),
            PorterDuff.Mode.MULTIPLY
        )
        iv.isSelected = !iv.isSelected

        mRtcEngine!!.muteLocalAudioStream(iv.isSelected)
    }

    // 初始化agoraEngine
    private fun initializeAgoraEngine() =
        try {
            mRtcEngine =
                RtcEngine.create(baseContext, getString(R.string.agora_app_id), mRtcEventHandler)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    // 视频配置
    private fun setupVideoProfile() {
        mRtcEngine!!.enableVideo()
        mRtcEngine!!.setVideoEncoderConfiguration(
            VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_640x360,                   // 分辨率
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15, // 帧率
                VideoEncoderConfiguration.STANDARD_BITRATE,             // 比特率
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
            )
        )
    }

    // 本地视频预览
    private fun setupLocalVideo() {
        val container = findViewById<FrameLayout>(R.id.locale_view_container)
        val surfaceView = RtcEngine.CreateRendererView(baseContext)
        surfaceView.setZOrderMediaOverlay(true)
        container.addView(surfaceView)
        mRtcEngine!!.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, 0))
    }

    // 加入频道
    // token为空时此项目不可开启token鉴权
    // 官方建议使用token鉴权，使用自建服务端生成token
    // 详见：https://docs.agora.io/cn/Video/token_server?platform=Android
    private fun joinChannel() {
        var token: String? = null
        mRtcEngine!!.joinChannel(
            token,
            channelName,
            "Extra Optional Data",
            0
        )
    }

    // 对方视频设置
    private fun setupRemoteVideo(uid: Int) {
        val container = findViewById<FrameLayout>(R.id.remote_view_container)

        if (container.childCount >= 1) {
            return
        }
        val surfaceView = RtcEngine.CreateRendererView(baseContext)
        container.addView(surfaceView)
        // 第二个参数为缩放模式
        // 参考：https://docs.agora.io/cn/Interactive%20Broadcast/API%20Reference/java/classio_1_1agora_1_1rtc_1_1video_1_1_video_canvas.html
        mRtcEngine!!.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid))

        surfaceView.tag = uid
    }

    private fun onRemoteUserVideoMuted(uid: Int, muted: Boolean) {
        val container = findViewById<FrameLayout>(R.id.remote_view_container)

        val surfaceView = container.getChildAt(0) as SurfaceView

        val tag = surfaceView.tag
        if (tag != null && tag as Int == uid) {
            surfaceView.visibility = if (muted) View.GONE else View.VISIBLE
        }
    }

    companion object {
        // 权限常数
        private const val PERMISSION_REQ_ID_RECORD_AUDIO = 22
        private const val PERMISSION_REQ_ID_CAMERA = PERMISSION_REQ_ID_RECORD_AUDIO + 1
    }
}
