package com.nglandeyan.videodownloader

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    // Deklarasi semua variabel UI di sini
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgress: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var etUrl: EditText
    private lateinit var btnDownload: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Hubungkan variabel dengan ID di XML
        etUrl = findViewById(R.id.etUrl)
        btnDownload = findViewById(R.id.btnDownload)
        progressBar = findViewById(R.id.progressBar)
        tvProgress = findViewById(R.id.tvProgress)
        tvSpeed = findViewById(R.id.tvSpeed)

        btnDownload.setOnClickListener {
            var userUrl = etUrl.text.toString().trim()
            if (userUrl.isEmpty()) return@setOnClickListener

            if (!userUrl.startsWith("http")) userUrl = "https://$userUrl"

            if (userUrl.endsWith(".mp4") || userUrl.contains(".mp4?")) {
                downloadFile(userUrl, "Video_${System.currentTimeMillis()}.mp4", "video/mp4")
            } else {
                progressBar.visibility = View.VISIBLE
                btnDownload.isEnabled = false

                val retrofit = retrofit2.Retrofit.Builder()
                    .baseUrl("https://social-download-all-in-one.p.rapidapi.com")
                    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                    .build()

                val apiService = retrofit.create(ApiService::class.java)
                val requestBody = VideoRequest(url = userUrl)

                apiService.extractVideo("2f71e13154msh51cad448f369792p1d8003jsn652c6904bad5", "social-download-all-in-one.p.rapidapi.com", requestBody)
                    .enqueue(object : retrofit2.Callback<VideoResponse> {
                        override fun onResponse(call: retrofit2.Call<VideoResponse>, response: retrofit2.Response<VideoResponse>) {
                            progressBar.visibility = View.GONE
                            btnDownload.isEnabled = true

                            val mediaList = response.body()?.medias?.filter { !it.url.isNullOrEmpty() }
                            if (!mediaList.isNullOrEmpty()) {
                                val builder = AlertDialog.Builder(this@MainActivity)
                                builder.setTitle("Pilih Kualitas")
                                builder.setItems(mediaList.map { "${it.quality ?: "Standard"} - ${it.type}" }.toTypedArray()) { _, which ->
                                    val item = mediaList[which]
                                    val ext = if (item.type == "audio") ".mp3" else ".mp4"
                                    val mime = if (item.type == "audio") "audio/mp3" else "video/mp4"
                                    downloadFile(item.url!!, "Download_${System.currentTimeMillis()}$ext", mime)
                                }
                                builder.show()
                            }
                        }
                        override fun onFailure(call: retrofit2.Call<VideoResponse>, t: Throwable) {
                            progressBar.visibility = View.GONE
                            btnDownload.isEnabled = true
                        }
                    })
            }
        }
    }

    private fun downloadFile(url: String, fileName: String, mimeType: String) {
        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(fileName)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setMimeType(mimeType)
            addRequestHeader("User-Agent", "Mozilla/5.0")
        }
        val downloadId = manager.enqueue(request)

        progressBar.visibility = View.VISIBLE
        tvProgress.visibility = View.VISIBLE
        tvSpeed.visibility = View.VISIBLE

        var lastBytes = 0L
        var lastTime = System.currentTimeMillis()

        val runnable = object : Runnable {
            override fun run() {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = manager.query(query)
                if (cursor.moveToFirst()) {
                    val downloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))

                    val currentTime = System.currentTimeMillis()
                    val timeDiff = (currentTime - lastTime) / 1000.0
                    if (timeDiff > 0.5) {
                        val speed = (downloaded - lastBytes) / timeDiff
                        tvSpeed.text = "Kecepatan: ${String.format("%.1f", speed / 1024)} KB/s"
                        lastBytes = downloaded
                        lastTime = currentTime
                    }

                    if (total > 0) {
                        val progress = (downloaded * 100 / total).toInt()
                        progressBar.progress = progress
                        tvProgress.text = "$progress%"
                    }

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        progressBar.visibility = View.GONE
                        tvProgress.visibility = View.GONE
                        tvSpeed.text = "Selesai!"
                        handler.removeCallbacks(this)
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        tvSpeed.text = "Gagal!"
                        handler.removeCallbacks(this)
                    } else {
                        handler.postDelayed(this, 500)
                    }
                }
                cursor.close()
            }
        }
        handler.post(runnable)
    }
}