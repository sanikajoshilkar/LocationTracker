package com.codecrafters.locationtracker.progress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.codecrafters.locationtracker.R
import com.codecrafters.locationtracker.tracking.TrackingData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProgressFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingAnimation: LottieAnimationView
    private lateinit var trackingAdapter: TrackingAdapter
    private lateinit var trackingList: ArrayList<TrackingData>
    private lateinit var databaseReference: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_progress, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        loadingAnimation = view.findViewById(R.id.loadingAnimation)

        recyclerView.layoutManager = LinearLayoutManager(context)
        trackingList = ArrayList()
        trackingAdapter = TrackingAdapter(trackingList)
        recyclerView.adapter = trackingAdapter

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        databaseReference = FirebaseDatabase.getInstance().reference.child("trackingData").child(userId!!)

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trackingList.clear()
                for (dataSnapshot in snapshot.children) {
                    val trackingData = dataSnapshot.getValue(TrackingData::class.java)
                    if (trackingData != null) {
                        trackingList.add(trackingData)
                    }
                }
                trackingAdapter.notifyDataSetChanged()
                loadingAnimation.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors.
            }
        })

        return view
    }
}
