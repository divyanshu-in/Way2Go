package com.application.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.application.myapplication.databinding.ActivityMainBinding
import com.application.myapplication.databinding.EnterUsernamesPopupLayoutBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import timber.log.Timber

import com.google.android.gms.maps.SupportMapFragment





class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    val roomId = FirebaseAuth.getInstance().currentUser?.phoneNumber!!
    val database = FirebaseDatabase.getInstance("https://path-finder-801db-default-rtdb.asia-southeast1.firebasedatabase.app")
    val ref = database.reference

    private lateinit var viewModel: MainViewModel

    //location pair of this user and next user.
    private val locationPairLD = MutableLiveData<Pair<Coords?, Coords?>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        binding.button.setOnClickListener {

            Timber.e(checkLocationPermission().toString())

            if(checkLocationPermission()){
                turnGPSOn()
            }else{
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }

        }

        val mapFragment = supportFragmentManager
            .findFragmentById(binding.map.id) as SupportMapFragment?




        mapFragment?.getMapAsync(this)


        locationPairLD.observe(this){
            Timber.e(it.toString() )
            if(it.first != null && it.second != null){
                binding.includedProgressLayout.root.gone()

//                viewModel.getDirectionsForCoords("23.2599", )
                viewModel.getDirectionsForCoords("${it.first?.long.toString()}, ${it.first?.lat.toString()}", "${it.second?.long.toString()}, ${it.second?.lat.toString()}")
            }
        }

        viewModel.directionsLD.observe(this){
            Timber.e(it.toString() + "Dirs")
            drawMapRoute(it.features?.get(0)?.geometry?.coordinates!!, mapFragment)
        }


        setContentView(binding.root)
    }


    private fun showPopup(){
        val popupBinding = EnterUsernamesPopupLayoutBinding.inflate(LayoutInflater.from(this))

        val popup = PopupWindow(popupBinding.root, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true)


        popupBinding.buttonFindPath.setOnClickListener {

            val firstUsername = popupBinding.etFirstUsername.text.toString()
            startUpdatingLocation(firstUsername)
            binding.includedProgressLayout.root.visible()
            binding.button.gone()
            popup.dismiss()
//            createRoom(firstUsername)
        }

        popup.showAtLocation(binding.root, Gravity.CENTER, 0, 0)

        popup.showPopupDimBehind()
    }


    private fun updateLocOnFirebase(location: Location){

        ref.child(roomId).child("lat").setValue(location.latitude)
        ref.child(roomId).child("long").setValue(location.longitude)
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->

            if(isGranted){
                turnGPSOn()
            }else{

            }
        }

    private fun checkLocationPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                )
    }

    inner class MyLocationListener : LocationListener {

        override fun onLocationChanged(location: Location) {


            locationPairLD.postValue(Pair(Coords(location.latitude, location.longitude), locationPairLD.value?.second))

            updateLocOnFirebase(location)
            Timber.e(location.toString())
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle?) {

        }

        override fun onProviderEnabled(provider: String) {

        }

        override fun onProviderDisabled(provider: String) {

        }
    }

    private fun turnGPSOn() {

        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val task = LocationServices.getSettingsClient(this)
            .checkLocationSettings(builder.build())

        task.addOnSuccessListener { response ->
            val states = response.locationSettingsStates
            if (states?.isLocationPresent == true) {
                showPopup()
            }
        }

        task.addOnFailureListener { e ->
            if (e is ResolvableApiException) {
                try {
                    // Handle result in activity launcher
                    activityLauncherGPS.launch(IntentSenderRequest.Builder(e.resolution).build())

                } catch (sendEx: IntentSender.SendIntentException) {
                    Timber.d(sendEx)
                }
            }
        }
    }

    private val activityLauncherGPS =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            when (it.resultCode) {
                Activity.RESULT_OK -> {
                    showPopup()
                }
                else -> {
                    Snackbar.make(binding.root, "Please Turn On GPS To Continue!", Snackbar.LENGTH_SHORT).show()
                }

            }
        }

    @SuppressLint("MissingPermission")
    private fun startUpdatingLocation(firstUsername: String) {

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val criteria = Criteria()
        criteria.accuracy = Criteria.ACCURACY_FINE //default

        criteria.isCostAllowed = false

        // get the best provider depending on the criteria

        val provider = locationManager.getBestProvider(criteria, false)

        val locationListener = MyLocationListener()

        locationManager.requestLocationUpdates(provider!!, 5000L, 0.0F, locationListener)



        val childRef = ref.child("+91$firstUsername")

        childRef.addValueEventListener(userLocationListener)


    }

    private val userLocationListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            // Get Post object and use the values to update the UI
            val location = dataSnapshot.getValue(Coords::class.java)
            location?.lat?.let {
                locationPairLD.postValue(Pair(locationPairLD.value?.first, location) as Pair<Coords?, Coords?>?)
            }
            Timber.e(location.toString())
            // ...
        }

        override fun onCancelled(databaseError: DatabaseError) {
            // Getting Post failed, log a message
        }
    }

    private fun drawMapRoute(coordinates: List<List<Double>>, mapFragment: SupportMapFragment?) {

        val polyLineOptions = PolylineOptions()
        polyLineOptions.color(Color.parseColor("#4a89f3"))
        polyLineOptions.width(25F)
        polyLineOptions.visible(true)
        polyLineOptions.jointType(JointType.ROUND)


        coordinates.forEach {
            polyLineOptions.add(LatLng(it[1], it[0]))
        }

        mapFragment?.getMapAsync {
            val polyline = it.addPolyline(polyLineOptions)

            val avgLat = (coordinates.first()[1] + coordinates.last()[1]) / 2
            val avgLong = (coordinates.first()[0] + coordinates.last()[0]) / 2

            it.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(avgLat, avgLong), 20.0F))
        }


    }

    override fun onMapReady(p0: GoogleMap) {

    }


//    viewModel.getRouteDirection("77.4607,23.2571", "77.5247," +
//    "23" +
//    ".2512")



}