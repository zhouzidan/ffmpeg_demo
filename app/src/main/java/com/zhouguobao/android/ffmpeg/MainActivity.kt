package com.zhouguobao.android.ffmpeg

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tbruyelle.rxpermissions3.RxPermissions
import io.microshow.rxffmpeg.RxFFmpegInvoke
import io.microshow.rxffmpeg.RxFFmpegSubscriber


class MainActivity : AppCompatActivity() {

    val rxPermissions = RxPermissions(this) // where this is an Activity or Fragment instance

    var lastPath:String = ""

    private final val tag = "ffmpeg"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        RxFFmpegInvoke.getInstance().setDebug(true);

        findViewById<View>(R.id.selectVideoBtn).setOnClickListener{
            rxPermissions
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe { granted ->
                    if (granted) { // Always true pre-M
                        val intent = Intent();
                        intent.type = "video/*"; //选择视频 （mp4 3gp 是android支持的视频格式）
                        intent.action = Intent.ACTION_GET_CONTENT;
                        /* 取得相片后返回本画面 */
                        startActivityForResult(intent, 1);
                    } else {
                    }
                }
        }

        findViewById<View>(R.id.buildImageBtn).setOnClickListener{


            val outDir = getExternalFilesDir("ffmpeg")?.absolutePath ?: cacheDir.absolutePath
            Log.e(tag, outDir)

            val text = "ffmpeg -i $lastPath -vf fps=10 ${outDir}/out%d.png"

            val commands = text.split(" ").toTypedArray()

            //开始执行FFmpeg命令
            RxFFmpegInvoke.getInstance()
                .runCommandRxJava(commands)
                .subscribe(object : RxFFmpegSubscriber(){
                    override fun onError(message: String?) {
                        Log.e(tag, "onError $message")
                    }

                    override fun onFinish() {
                        Log.e(tag, "onFinish ")
                    }

                    override fun onProgress(progress: Int, progressTime: Long) {
                        Log.e(tag, "onProgress $progress $progressTime")
                    }

                    override fun onCancel() {
                        Log.e(tag, "onCancel")
                    }

                })


        }

// ffmpeg -threads2 -y -r 10 -i /tmpdir/image%04d.jpg -i audio.mp3 -absf aac_adtstoasc output.mp4
        /**
         * 参数的解释含义：

        -threads 2 以两个线程进行运行， 加快处理的速度。

        -y 对输出文件进行覆盖

        -r 10 fps设置为10帧/秒（不同位置有不同含义，后面再解释）

        -i /tmpdir/image%04d.jpg 输入图片文件，图片文件保存为 image0001.jpg image0002.jpg ….

        -i audio.mp3 输入的音频文件

        https://blog.csdn.net/wangshuainan/article/details/77914508

        ffmpeg -loop 1 -f image2 -i /tmpdir/image%04d.jpg -vcodec libx264 -r 10 -t 10 test.mp4
         */
        findViewById<View>(R.id.buildVideoBtn).setOnClickListener{
            val outDir = getExternalFilesDir("ffmpeg")?.absolutePath ?: cacheDir.absolutePath
            val inputFile = "${outDir}/out%d.png"

            val text = "ffmpeg -i $inputFile $outDir.avi"
            val commands = text.split(" ").toTypedArray()

            //开始执行FFmpeg命令
            RxFFmpegInvoke.getInstance()
                .runCommandRxJava(commands)
                .subscribe(object : RxFFmpegSubscriber(){
                    override fun onError(message: String?) {
                        Log.e(tag, "onError $message")
                    }

                    override fun onFinish() {
                        Log.e(tag, "onFinish ")
                    }

                    override fun onProgress(progress: Int, progressTime: Long) {
                        Log.e(tag, "onProgress $progress $progressTime")
                    }

                    override fun onCancel() {
                        Log.e(tag, "onCancel")
                    }

                })
        }

    }



    override fun onDestroy() {
        super.onDestroy()
        RxFFmpegInvoke.getInstance().exit();

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if(data!=null){
                val uri = data.data ?: return
                val cursor = contentResolver.query(uri, null, null,
                    null, null);
                cursor?.moveToFirst();
                // String imgNo = cursor.getString(0); // 图片编号
                val filePath = cursor?.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA) ?: 0); // 图片文件路径
                cursor?.close()
                findViewById<TextView>(R.id.videoPathTv).text = filePath
                lastPath = filePath ?: ""
            }
        }
    }
}