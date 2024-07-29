package com.codecrafters.locationtracker.progress

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.codecrafters.locationtracker.R
import com.codecrafters.locationtracker.tracking.TrackingData

class TrackingAdapter(private val trackingList: List<TrackingData>) :
    RecyclerView.Adapter<TrackingAdapter.TrackingViewHolder>() {

    class TrackingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val textStartTime: TextView = itemView.findViewById(R.id.textStartTime)
        val textStopTime: TextView = itemView.findViewById(R.id.textStopTime)
        val textStepsTracked: TextView = itemView.findViewById(R.id.textStepsTracked)
        val textDistanceTraveled: TextView = itemView.findViewById(R.id.textDistanceTraveled)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tracking, parent, false)
        return TrackingViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackingViewHolder, position: Int) {
        val trackingData = trackingList[position]
        Glide.with(holder.itemView.context).load(trackingData.imageUrl).into(holder.imageView)
        holder.textStartTime.text = "Start Time: ${trackingData.startTime}"
        holder.textStopTime.text = "Stop Time: ${trackingData.stopTime}"
        holder.textStepsTracked.text = "Steps: ${trackingData.stepsTracked}"
        holder.textDistanceTraveled.text = "Distance: ${trackingData.distanceTraveled}m"

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, TrackingDetailsActivity::class.java).apply {
                putExtra("imageUrl", trackingData.imageUrl)
                putExtra("startTime", trackingData.startTime)
                putExtra("stopTime", trackingData.stopTime)
                putExtra("stepsTracked", trackingData.stepsTracked)
                putExtra("distanceTraveled", trackingData.distanceTraveled)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return trackingList.size
    }
}
