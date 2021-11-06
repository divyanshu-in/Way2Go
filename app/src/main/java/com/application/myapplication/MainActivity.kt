package com.application.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
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
import android.widget.Toast
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import timber.log.Timber
import com.google.android.gms.maps.model.MarkerOptions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.atan2


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private val roomId = FirebaseAuth.getInstance().currentUser?.phoneNumber!!
    private val database = FirebaseDatabase.getInstance("https://path-finder-801db-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val ref = database.reference
    private var polyline: Polyline? = null
    private lateinit var viewModel: MainViewModel
    private var secondUsername: String = ""
    private var marker1: Marker? = null
    private var marker2: Marker? = null
    private var isCameraAnimated = false

    private var arrayOfMarkers = arrayListOf<Marker>()




    //location pair of this user and next user.
    private val locationPairLD = MutableLiveData<Pair<Coords?, Coords?>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))




        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        binding.cvClose.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.cvLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, SplashActivity::class.java))
        }

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




                viewModel.getDirectionsForCoords("${it.first?.long.toString()}, ${it.first?.lat.toString()}", "${it.second?.long.toString()}, ${it.second?.lat.toString()}")
            }
        }

        viewModel.directionsLD.observe(this){ routeDirections ->
            Timber.e(routeDirections.toString() + "Dirs")
            drawMapRoute(routeDirections.features?.get(0)?.geometry?.coordinates!!, mapFragment)

            routeDirections.features.firstOrNull()?.properties?.summary?.let{
                binding.llDistance.visible()

                val roundedDistance = "%.${2}f".format(it.distance?.div(1000)).toDouble()

                binding.tvDist.text = roundedDistance.toString() + "Km"
            }


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
            binding.cvClose.visible()
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
                Toast.makeText(this, "this permission is necessary!", Toast.LENGTH_SHORT).show()
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
            Timber.e(location.toString() + "cur-userloc")
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle?) {
            Timber.e("""${extras.toString()} $status""")
        }

        override fun onProviderEnabled(provider: String) {
            Timber.e(provider.toString())
        }

        override fun onProviderDisabled(provider: String) {
            Timber.e(provider.toString())
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
        secondUsername = firstUsername
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

//        val criteria = Criteria()
//        criteria.accuracy = Criteria.ACCURACY_FINE //default
//
//        criteria.isCostAllowed = false

        // get the best provider depending on the criteria

//        val provider = locationManager.getBestProvider(criteria, false)

        val locationListener = MyLocationListener()




        val childRef = ref.child("+91$firstUsername")

        childRef.child("lat").setValue(null)
        childRef.child("long").setValue(null)

        childRef.addValueEventListener(userLocationListener)

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 0.0F, locationListener)
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 0.0F, locationListener)


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

        arrayOfMarkers.forEach {
            it.remove()
        }



        mapFragment?.getMapAsync { googleMap ->

            coordinates.indices.forEach {

                if(it > 0 && it < coordinates.size && coordinates.size > 1){
                    val firstLatLng = LatLng(coordinates[it - 1][0], coordinates[it - 1][1])
                    val secondLatLng = LatLng(coordinates[it][0], coordinates[it][1])
                    val rotationDegrees = getAngleOfLine(firstLatLng, secondLatLng)





                    val matrix = Matrix()
                    matrix.postRotate(rotationDegrees.toFloat())

                    val originalBitmap = bitmapDescriptorFromVector(this, R.drawable.ic_navigation)


                    val position = LatLng((coordinates[it - 1][1] + coordinates[it][1])/2, (coordinates[it - 1][0] + coordinates[it][0])/2)

                    val mArrowhead = googleMap.addMarker(
                        MarkerOptions()
                            .position(position)
                            .icon(originalBitmap).rotation(rotationDegrees.toFloat())
                    )

                    mArrowhead?.let { it1 -> arrayOfMarkers.add(it1) }

                }

                polyLineOptions.add(LatLng(coordinates[it][1], coordinates[it][0]))
            }


            polyline?.let {
                marker1?.remove()
                marker2?.remove()
                it.remove()
            }
            polyline = googleMap.addPolyline(polyLineOptions)

            val avgLat = (coordinates.first()[1] + coordinates.last()[1]) / 2
            val avgLong = (coordinates.first()[0] + coordinates.last()[0]) / 2



            marker1 = googleMap.addMarker(MarkerOptions().title("You").position(LatLng(coordinates.first()[1], coordinates.first()[0])))
            marker2 = googleMap.addMarker(MarkerOptions().title(secondUsername).position(LatLng(coordinates.last()[1], coordinates.last()[0])))

            if(!isCameraAnimated){
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(avgLat, avgLong), 20.0F))
                isCameraAnimated = true
            }

        }


    }

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        return ContextCompat.getDrawable(context, vectorResId)?.run {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }


    private fun getAngleOfLine(firstCoord: LatLng, secondCoord: LatLng)  = Math.toDegrees(atan2(secondCoord.latitude - firstCoord.latitude, secondCoord.longitude - firstCoord.longitude))

    override fun onMapReady(p0: GoogleMap) {

    }


//    viewModel.getRouteDirection("77.4607,23.2571", "77.5247," +
//    "23" +
//    ".2512")



}