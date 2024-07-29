package com.codecrafters.locationtracker

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.codecrafters.locationtracker.tracking.TrackingData
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StepsActivity : AppCompatActivity() {
    private var selectedButton: Button? = null

    private lateinit var database: DatabaseReference
    private lateinit var stepsChart: BarChart
    private lateinit var distanceChart: BarChart
    private lateinit var textViewDateRange: TextView
    private lateinit var linearLayoutWeeks: LinearLayout

    private var trackingData: List<TrackingData> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_steps)

        stepsChart = findViewById(R.id.stepsChart)
        distanceChart = findViewById(R.id.distanceChart)
        textViewDateRange = findViewById(R.id.textViewDateRange)
        linearLayoutWeeks = findViewById(R.id.linearLayoutWeeks)

        // Initialize the charts with zero values
        initializeCharts()

        database = FirebaseDatabase.getInstance().reference
        fetchTrackingData()
    }

    private fun initializeCharts() {
        val dummyEntries = List(7) { BarEntry(it.toFloat(), 0f) }
        val labels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        // Set up charts with zero data initially
        setupChart(stepsChart, dummyEntries, labels, "Steps")
        setupChart(distanceChart, dummyEntries, labels, "Distance")
    }

    private fun fetchTrackingData() {
        val userId = "od3G0AwS3aO8gnMQK4a68ASig8C3"  // Replace with the actual user ID
        database.child("trackingData").child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = mutableListOf<TrackingData>()
                for (dataSnapshot in snapshot.children) {
                    val trackingData = dataSnapshot.getValue(TrackingData::class.java)
                    trackingData?.let { data.add(it) }
                }
                trackingData = data
                updateWeekButtons()
                // Ensure the charts start with zero values until a week is selected
                updateCharts(getCurrentWeek())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("StepsActivity", "Failed to fetch tracking data", error.toException())
            }
        })
    }

    private fun updateWeekButtons() {
        linearLayoutWeeks.removeAllViews()
        val dateRanges = generateDateRanges()
        for (dateRange in dateRanges) {
            val button = Button(this)
            button.text = dateRange
            button.setOnClickListener {
                // Deselect previously selected button
                selectedButton?.setBackgroundResource(R.drawable.button_normal)

                // Select the new button
                button.setBackgroundResource(R.drawable.button_selected)
                selectedButton = button

                textViewDateRange.text = dateRange
                updateCharts(dateRange)
            }
            linearLayoutWeeks.addView(button)
        }
    }

    private fun generateDateRanges(): List<String> {
        val dateFormatter = SimpleDateFormat("dd MMM", Locale.getDefault())
        val calendar = Calendar.getInstance()

        val dateRanges = mutableListOf<String>()

        // Determine the earliest and latest dates from the tracking data
        val dates = trackingData.map { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(it.startTime) }
        val earliestDate = dates.minOrNull() ?: return emptyList()
        val latestDate = dates.maxOrNull() ?: return emptyList()

        // Start from the Sunday before the earliest date
        calendar.time = earliestDate
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        if (calendar.time.after(earliestDate)) {
            calendar.add(Calendar.DAY_OF_YEAR, -7)
        }

        while (calendar.time.before(latestDate) || calendar.time == latestDate) {
            val startDate = calendar.time
            calendar.add(Calendar.DAY_OF_YEAR, 6)
            val endDate = calendar.time
            dateRanges.add("${dateFormatter.format(startDate)} - ${dateFormatter.format(endDate)}")
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return dateRanges
    }

    private fun getCurrentWeek(): String {
        val dateFormatter = SimpleDateFormat("dd MMM", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val startDate = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, 6)
        val endDate = calendar.time
        return "${dateFormatter.format(startDate)} - ${dateFormatter.format(endDate)}"
    }

    private fun updateCharts(dateRange: String) {
        Log.d("StepsActivity", "Updating charts for date range: $dateRange")
        val (stepsEntries, distanceEntries, labels) = processDataForDateRange(trackingData, dateRange)

        if (stepsEntries.isEmpty() && distanceEntries.isEmpty()) {
            // Hide charts if there are no entries for the selected date range
            stepsChart.visibility = View.GONE
            distanceChart.visibility = View.GONE
        } else {
            // Show charts and update them if there are entries for the selected date range
            stepsChart.visibility = View.VISIBLE
            distanceChart.visibility = View.VISIBLE

            setupChart(stepsChart, stepsEntries, labels, "Steps")
            setupChart(distanceChart, distanceEntries, labels, "Distance")
        }
    }

    private fun processDataForDateRange(data: List<TrackingData>, dateRange: String): Triple<List<BarEntry>, List<BarEntry>, List<String>> {
        val stepsEntries = mutableListOf<BarEntry>()
        val distanceEntries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())  // Updated format
        val dateRangeParts = dateRange.split(" - ")
        val startDate = dateFormatter.parse(dateRangeParts[0] + " " + Calendar.getInstance().get(Calendar.YEAR))!!
        val endDate = dateFormatter.parse(dateRangeParts[1] + " " + Calendar.getInstance().get(Calendar.YEAR))!!

        Log.d("StepsActivity", "Processing data from $startDate to $endDate")

        val filteredData = data.filter {
            val startTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(it.startTime)!!
            startTime in startDate..endDate
        }

        Log.d("StepsActivity", "Filtered data size: ${filteredData.size}")

        val groupedData = filteredData.groupBy {
            val startTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(it.startTime)!!
            SimpleDateFormat("E", Locale.getDefault()).format(startTime)
        }

        val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        for (index in daysOfWeek.indices) {
            val dayData = groupedData[daysOfWeek[index]]
            val totalSteps = dayData?.sumOf { it.stepsTracked }?.toFloat() ?: 0f
            val totalDistance = dayData?.sumOf { it.distanceTraveled }?.toFloat() ?: 0f
            stepsEntries.add(BarEntry(index.toFloat(), totalSteps))
            distanceEntries.add(BarEntry(index.toFloat(), totalDistance))
            labels.add(daysOfWeek[index])
        }

        return Triple(stepsEntries, distanceEntries, labels)
    }

    private fun setupChart(chart: BarChart, entries: List<BarEntry>, labels: List<String>, label: String) {
        Log.d("StepsActivity", "Setting up chart for $label with entries: ${entries.size}")
        val dataSet = BarDataSet(entries, label)
        val barData = BarData(dataSet)
        chart.data = barData

        val xAxis = chart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)

        chart.axisLeft.granularity = 1f
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.invalidate()  // Refresh chart
    }
}
