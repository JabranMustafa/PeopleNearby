package com.example.peoplenearby

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError

import java.util.UUID




class MainActivity2 : AppCompatActivity() , OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val database = FirebaseDatabase.getInstance().getReference("users")

    private val userId by lazy { getOrCreateUserId() }


    private val people = mutableListOf<User>()
    private lateinit var adapter: PeopleAdapter

    private var myLatitude = 0.0
    private var myLongitude = 0.0

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        setContentView(R.layout.activity_main)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        adapter = PeopleAdapter(people)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (!hasLocationPermission()) requestLocationPermission()
        else startLocationUpdates()

        listenForNearbyUsers()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1001
        )
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000
        ).build()

        fusedLocationClient.requestLocationUpdates(
            request,
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return

                    myLatitude = location.latitude
                    myLongitude = location.longitude

                    uploadLocation(myLatitude, myLongitude)

                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(myLatitude, myLongitude),
                            14f
                        )
                    )
                }
            },
            Looper.getMainLooper()
        )
    }

    private fun uploadLocation(lat: Double, lon: Double) {

        val blurredLat = blurLocation(lat)
        val blurredLon = blurLocation(lon)

        val user = User(
            id = userId,
            name = "You",
            avatarUrl = "https://i.pravatar.cc/150?u=$userId",
            latitude = blurredLat,
            longitude = blurredLon,
            timestamp = System.currentTimeMillis()
        )

        database.child(userId).setValue(user)
    }

    private fun listenForNearbyUsers() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                if (!::map.isInitialized) return   // Map not ready = no markers

                map.clear()  // remove old markers

                //  Always add your OWN marker
                val myLatLng = LatLng(myLatitude, myLongitude)
                map.addMarker(
                    MarkerOptions()
                        .position(myLatLng)
                        .title("Me")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                )

                map.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15f))

                people.clear()

                for (snap in snapshot.children) {
                    val user = snap.getValue(User::class.java) ?: continue
                    if (user.id == userId) continue  // skip myself

                    // Calculate distance
                    val distance = FloatArray(1)
                    Location.distanceBetween(
                        myLatitude, myLongitude,
                        user.latitude, user.longitude,
                        distance
                    )

                    if (distance[0] <= 200000){ // 200 km

                        people.add(user)

                        // ADD MARKER FOR OTHER USER
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(user.latitude, user.longitude))
                                .title(user.name)
                                .snippet("${distance[0].toInt()} meters away")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        )
                    }
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity2, "Firebase error", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun blurLocation(value: Double): Double {
        return String.format("%.2f", value).toDouble()
    }

    private fun getOrCreateUserId(): String {
        val prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE)
        val savedId = prefs.getString("USER_ID", null)

        if (savedId != null) {
            return savedId
        }

        val newId = UUID.randomUUID().toString()
        prefs.edit().putString("USER_ID", newId).apply()
        return newId
    }

}