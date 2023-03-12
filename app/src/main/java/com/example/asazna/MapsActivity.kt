package com.example.asazna

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.asazna.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private  val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    lateinit var toggle:ActionBarDrawerToggle
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private lateinit var currentLocation: Location
    private lateinit var geocoder: Geocoder
    private lateinit var realm : Realm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //サインインしてない場合、サインイン画面へ
        val user = auth.currentUser?.uid
        if(user == null){
            val intent = Intent(this, LogIn::class.java)
            startActivity(intent)
        }else{
            Log.d("AsaznaLog","${user}がログイン中でっせ")
        }

        // ナビゲーションの表示
        binding.apply {
            toggle = ActionBarDrawerToggle(this@MapsActivity,drawer,R.string.open,R.string.close)
            drawer.addDrawerListener(toggle)
            toggle.syncState()

            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
        //ユーザー情報の取得　＋　ナビゲーションドロワーへの表示
        getFireStoreActivity()

        binding.navigation.setNavigationItemSelectedListener {
            when(it.itemId){
                R.id.logout ->{
                    auth.signOut()
                    val intent = Intent(this, LogIn::class.java)
                    startActivity(intent)
                }
            }
            true
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_frag) as SupportMapFragment
        mapFragment.getMapAsync(this)
        
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.d("AsaznaLog","Mapの準備ができましたが？")

        mMap = googleMap
        realm  = Realm.getDefaultInstance()
        geocoder = Geocoder(this, Locale.getDefault())


        addMarkerInMaps()
        checkPermission()

        //マーカーの登録
        mMap.setOnMapLongClickListener{
            Log.d("AsaznaLog","LatLng:$it")
            val spotlist = arrayOf("SmokeShop","FoodShop","ChillSpot")

            AlertDialog.Builder(this)
                .setTitle("スポットのジャンルを選んでください")
                .setSingleChoiceItems(spotlist,0) { dialog, which ->
                    dialog.dismiss()

                    val spotgenre = "${spotlist[which]}"
                    val dialoglayout = LayoutInflater.from(this)
                        .inflate(R.layout.marker_edit_alartdialog, null)
                    val adedittext = dialoglayout.findViewById<EditText>(R.id.adedittext)

                    AlertDialog.Builder(this)
                        .setTitle(spotgenre)
                        .setView(dialoglayout)
                        .setPositiveButton("保存") { dialog,_->
                            val userID = auth.currentUser?.uid.toString()
                            val spotname = adedittext.text.toString()
                            val markerLat = it.latitude
                            val markerLng = it.longitude
                            val geoinfo = geocoder.getFromLocation(markerLat, markerLng, 1)
                            val geotext = geoinfo?.get(0)?.getAddressLine(0).toString()
                            realm.executeTransaction {

                                val rCurrentId = realm.where<RealmData>().max("id")
                                val nextId = (rCurrentId?.toLong() ?: 0L) + 1L
                                val myModel = realm.createObject<RealmData>(nextId)

                                myModel.name = spotname
                                myModel.genre = spotgenre
                                myModel.lat = markerLat
                                myModel.lng = markerLng
                                myModel.adress = geotext
                                myModel.info = ""
                                myModel.editterID = userID

                                Log.d("AsaznaLog","$myModel")


                                val newRmarkerLatLng = LatLng(markerLat,markerLng)
                                mMap.addMarker(MarkerOptions().position(newRmarkerLatLng).title(spotname)
                                    .icon(
                                        BitmapDescriptorFactory.defaultMarker(
                                            BitmapDescriptorFactory.HUE_GREEN)))
                            }
                        }
                        .setNegativeButton("キャンセル") { dialog, which ->

                            dialog.dismiss()

                        }
                        .create().show()
                }
                .create().show()
        }

        mMap.setOnMarkerClickListener {
            Log.d("AsaznaLog","マーカーをクリックしたんだね")
            showBottomSheetDialog(it)
            false
        }

        val navigationView = binding.navigation
        val header = navigationView.getHeaderView(0)
        var profiletext: TextView = header.findViewById(R.id.profiletext)

        profiletext.setOnClickListener {
            val intent = Intent(this,Mypage::class.java)
            intent.putExtra("editorname",profiletext.text.toString())
            startActivity(intent)
        }

    }

    private fun checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED){

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return

        }else{
            mMap.isMyLocationEnabled = true
            myLocationEnable()

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this,"許可されました", Toast.LENGTH_LONG).show()

        }else{

            Toast.makeText(this,"拒否されました", Toast.LENGTH_LONG).show()
        }
    }

    private fun myLocationEnable() {

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return

        } else {

            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->

                if (location != null) {
                    var locationLatLng = LatLng(location.latitude,location.longitude)
                    Log.d("AsaznaLog","$locationLatLng")

                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationLatLng, 13F))
                    //mMap.addMarker(markOptions)

                }
            }
        }
    }

    private fun getFireStoreActivity() {
        val user = auth.currentUser?.uid.toString()
        db.collection("users").document(user)
            .get()
            .addOnCompleteListener {
                val getFresult = it.result

                if(getFresult != null && getFresult.data != null){
                    Log.d("AsaznaLog","URL→" + getFresult.data?.get("profileImageUrl"))

                    val profileurl = getFresult.data?.get("profileImageUrl").toString()
                    val profilename = getFresult.data?.get("username")
                    val follow = getFresult.data?.get("followingCount")
                    val follower= getFresult.data?.get("beingFollowedCount")

                    //ナビゲーションの中の画像の参照 ※ボトムシートのようにinflateを使う？ 参照：SpotBottomSheet.kt
                    val navigationView = binding.navigation
                    val header = navigationView.getHeaderView(0)
                    var profileimage : ImageView = header.findViewById(R.id.profileimage)
                    var profiletext: TextView = header.findViewById(R.id.profiletext)
                    val followNumber: TextView = header.findViewById(R.id.follownumber)
                    val followerNumber: TextView = header.findViewById(R.id.followernumber)

                    Picasso.get().load(profileurl).into(profileimage)
                    profiletext.text = profilename.toString()

                    followNumber.text = follow.toString()
                    followerNumber.text = follower.toString()

                }

            }
    }


    private fun markerFirestoreActivity(markerLatLng:LatLng,spotgenre:String,shopname:String){
        val userID = auth.currentUser?.uid.toString()

        val spotinfo = mapOf(
            "name" to shopname,
            "lat" to markerLatLng.latitude,
            "lng" to markerLatLng.longitude,
            "editterID" to userID
        )

        db.collection("spot")
            .document(shopname)
            .set(spotinfo)
            .addOnSuccessListener {
                Log.d("AsaznaLog", "Document Written!$shopname")
            }
            .addOnFailureListener { e ->
                Log.w("AsaznaLog", "Document NOT Written!",e)
            }

    }

    private fun addMarkerInMaps(){
        Log.d("AsaznaLog","ほなマーカー表示していきまっせ")
        val spotRef = db.collection("smokeStores")

        realm  = Realm.getDefaultInstance()
        //Firestoreに登録されている情報の表示

        if(spotRef != null) {
            spotRef
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        val markername = document.data["storeName"]
                        val markernameString = markername.toString()
                        val markerlat = document.data["latitude"].toString().toDouble()
                        val markerlng = document.data["longitude"].toString().toDouble()

                        val markerLatLng = LatLng(markerlat, markerlng)
                        Log.d("AsaznaLog", "Lat:$markerlat" + "Lng:$markerlng")
                        mMap.addMarker(MarkerOptions().position(markerLatLng).title(markernameString))
                    }
                }
                .addOnFailureListener {
                    Log.d("AsaznaLog", "Fail to Load LatLng!")
                }
        }else   return

        //Realmに登録されている情報の表示
        val refRealm = realm.where(RealmData::class.java).findAll()

        if(refRealm != null){
            refRealm.forEach {
                val rMarkerName = it.name
                val rMaerkerLat = it.lat
                val rMaerkerLng = it.lng
                val rMarkerInfo = it.info
                val rMarkerEID = it.editterID
                val rMarkerLatLng = LatLng(rMaerkerLat, rMaerkerLng)

                mMap.addMarker(
                    MarkerOptions().position(rMarkerLatLng).title(rMarkerName)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                )
            }

        } else   return

    }


    private fun showBottomSheetDialog(it: Marker) {
        geocoder = Geocoder(this, Locale.getDefault())
        realm = Realm.getDefaultInstance()

        val mLat = it.position.latitude
        val mLng = it.position.longitude
        val geoinfo = geocoder.getFromLocation(mLat, mLng, 1)
        val geotext = geoinfo?.get(0)?.getAddressLine(0).toString()
        var nameinfo = it.title.toString()
        val refRealm = realm.where(RealmData::class.java)
            .equalTo("lat", mLat)
            .equalTo("lng", mLng)
            .findFirst()

        val rMarkerName =refRealm?.name
        val spgenre = refRealm?.genre

        Log.d("AsaznaLog","${mLat},${mLng}のデータ持ってきますわ")

        val spotbottomsheet = SpotBottomSheet(nameinfo, geotext, spgenre, mLat, mLng)
        spotbottomsheet.show(supportFragmentManager, "GoogleMAps")

    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }


}