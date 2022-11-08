package com.example.fusedlocationproviderclientkotlin

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.IntentSender
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationServices.getFusedLocationProviderClient

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.tasks.Task


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    // Variables
    private val SOLICITAR_ACCESS_FINE_LOCATION = 1000
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: MyLocationCallBack




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Iniciamos la localización del usuario
        iniciarGeolocalizacion()

    }

    // Verificamos los Permisos de ubicación del usuario
    private fun verificarPermisos(cancel:()-> Unit, ok:() ->Unit){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){

                // Se nego los permisos
                cancel()

            } else{
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),SOLICITAR_ACCESS_FINE_LOCATION)
            }
        } else {

            // Encendemos el GPS del usuario
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(2000)
                .setMaxUpdateDelayMillis(2000)
                .build()

            val builder = LocationSettingsRequest.Builder().addLocationRequest(request)
            val client: SettingsClient = LocationServices.getSettingsClient(this)
            val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
            task.addOnFailureListener {
                if (it is ResolvableApiException) {
                    try {
                        it.startResolutionForResult(this, 12345)
                    } catch (sendEx: IntentSender.SendIntentException) {
                    }
                }
            }.addOnSuccessListener {
                // Aca el GPS se enciende
            }

            // Se activo el GPS
            ok()
        }
    }

    private fun mostrarDialogoPermiso(){

        ActivityCompat.requestPermissions(this@MapsActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),SOLICITAR_ACCESS_FINE_LOCATION)

    }

    private fun iniciarGeolocalizacion(){

        fusedLocationProviderClient = getFusedLocationProviderClient(this)
        locationCallback = MyLocationCallBack()
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 5000

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    override fun onResume() {
        super.onResume()
        verificarPermisos(cancel = {

            // Mostramos el dialogo para solicitarle al usuario que encienda su GPS
            mostrarDialogoPermiso()

        }, ok = {

            // Iniciamos la Geolocalización del usuario con un Listener o Oyente
            agregarOyenteUbicacion()

        })
    }

    // Hacemos uso de Fused Location Provider (FLP)
    @SuppressLint("MissingPermission")
    private fun agregarOyenteUbicacion(){
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, null)
    }

    // Callback para mostrar un marcador en el Mapa de Google
    inner class MyLocationCallBack : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)

            val location = locationResult.lastLocation

            location?.run {
                val latLng = LatLng(latitude,longitude)

                mMap.addMarker(MarkerOptions().position(latLng).title("Mi Ubicación"))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
                Log.e("Ubicación","Latitud : $latitude, Longitud : $longitude")

            }
        }
    }

    override fun onPause() {
        super.onPause()
        removeLocationListener()
    }

    private fun removeLocationListener(){
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            SOLICITAR_ACCESS_FINE_LOCATION-> {
                if((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)){

                    // Este método hace uso de Fused Location Provider (FLP)
                    agregarOyenteUbicacion()

                } else{

                    // Si el usuario denego el permiso le mostramos el mensaje "Permiso denegado"
                    val toast = Toast.makeText(applicationContext, "Permiso denegado", Toast.LENGTH_SHORT)
                    toast.show()

                }
                return
            }
        }
    }

}