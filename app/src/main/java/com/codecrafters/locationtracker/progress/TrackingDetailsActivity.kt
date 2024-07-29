package com.codecrafters.locationtracker.progress

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.codecrafters.locationtracker.R
import java.io.File
import java.io.FileOutputStream

class TrackingDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking_details)


        val imageView: ImageView = findViewById(R.id.detailsImageView)
        val textStartTime: TextView = findViewById(R.id.detailsStartTime)
        val textStopTime: TextView = findViewById(R.id.detailsStopTime)
        val textStepsTracked: TextView = findViewById(R.id.detailsStepsTracked)
        val textDistanceTraveled: TextView = findViewById(R.id.detailsDistanceTraveled)
        val shareButton: Button = findViewById(R.id.shareButton)

        val imageUrl = intent.getStringExtra("imageUrl")
        val startTime = intent.getStringExtra("startTime")
        val stopTime = intent.getStringExtra("stopTime")
        val stepsTracked = intent.getIntExtra("stepsTracked", 0)
        val distanceTraveled = intent.getDoubleExtra("distanceTraveled", 0.0)

        Glide.with(this).load(imageUrl).into(imageView)
        textStartTime.text = "Start Time: $startTime"
        textStopTime.text = "Stop Time: $stopTime"
        textStepsTracked.text = "Steps: $stepsTracked"
        textDistanceTraveled.text = "Distance: $distanceTraveled km"

        val backButton: Button = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed()
        }

        shareButton.setOnClickListener {
            Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        // Save the bitmap to a file
                        val file = File(cacheDir, "shared_image.png")
                        val fos = FileOutputStream(file)
                        resource.compress(Bitmap.CompressFormat.PNG, 100, fos)
                        fos.close()

                        // Get the URI for the file using FileProvider
                        val uri = FileProvider.getUriForFile(this@TrackingDetailsActivity, "${packageName}.fileprovider", file)

                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "image/*"
                            putExtra(Intent.EXTRA_TEXT, "Tracking Details:\nStart Time: $startTime\nStop Time: $stopTime\nSteps: $stepsTracked\nDistance: $distanceTraveled km")
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, "Share via"))
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        // Handle if needed
                    }
                })
        }
    }
    override fun onBackPressed() {
        super.onBackPressed()
        // Any additional logic if needed
    }
}
