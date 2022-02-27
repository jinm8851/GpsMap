package org.study2.gpsmap

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper

import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import org.study2.gpsmap.databinding.ActivityMapsBinding

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

//    위치정보를 얻기위한 객체
    private val fusedLocationProviderClient by lazy {
        FusedLocationProviderClient(this)
}

//    위치 요청 정보
    private val locationRequest by lazy {
        LocationRequest.create().apply {
//            GPS 우선
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            /* 업데이트 인터벌
            * 위치 정보가 없을때는 업데이트 안함
            * 상황에 따라 짧아질 수 있음, 정확하지 않음
            * 다른 앱에서 짧은 인터벌로 위치 정보를 요청하면 짧아질 수 있음*/
            interval = 10000
//            정확함. 이것보다 짧은 업데이트는 하지 않음
            fastestInterval = 5000
        }
}

//    PolyLine 옵션 지도에 선그리기
    private val polylineOptions = PolylineOptions().width(5f).color(Color.RED)

//     위치 정보를 얻으면 해야할 행동이 정의된 콜백 객체
//    (위치정보늘 얻은후이 동작을 정의한 MyLocationCallBack()은 inner클래스로 정의했음)
    private val locationCallback = MyLocationCallBack()

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        isGranted ->
        if (isGranted) {
            addLocationListener()
        } else {
            Toast.makeText(this,"권한이 거부되었습니다",Toast.LENGTH_LONG).show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        화면이 꺼지지 않게 하기
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
//        세로 모드 화면 고정
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // SupportMapFragment를 받고 지도를 사용할 준비가 되면 알림을 받습니다.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }
// 위치요청은 액티비티가 활성화되는 onResume() 메서드에서 수행하며 앱이 동작중일때만 위치 정보를 갱신합니다.
    override fun onResume() {
        super.onResume()
//    권한요청
    checkPermission(cancel = {
//        위치 정보가 필요한 이유 다이얼로그 표시
        showPermissionInfoDialog()
    },
    ok = {
//        현재 위치를 주기적으로 요청 (권한이 필요한 부분
        addLocationListener()
    })
    }

    override fun onPause() {
        super.onPause()
        /*위치 요청을 취소합니다. 위치 요청을 취소하는 removeLocationListener() 메서드에서는
        remoteLocationUpdates() 메서드에 LocationCallback 객체를 전달하여 주기적인 위치 정보 갱신 요청을 삭제합니다.*/
        remoteLocationUpdates()
    }

    private fun remoteLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }
//    별도의 메서드로 작성합니다. 책하고 다름 null 안됨
    /* 안드로이드 스튜디오에서는 권한이 필요한 코드의 주변에 직접 작성한 원한 요청 코드만 인식 하기 때문입니다
    onResume() 에서 권한요청을 해서 에러가 뜨는거 같음
    권한요청코드를 제대로 작성했지만 별도의 메서드로 해당코드 블록을 분리하면 안드로이드 스튜디오가 에러로 판단합니다.
    * 이 메서드에서는 권한 요청 에러를 표시하지 않도록 하는 Suppress: Add @SuppressList("MissingPermission")
    * annotation을 클릭합니다 alt+enter*/
    @SuppressLint("MissingPermission")
    private fun addLocationListener() {
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    /**
     * 사용 가능한 맵을 조작합니다.
     * 지도를 사용할 준비가 되면 이 콜백이 트리거됩니다.
     * 여기서 마커나 라인을 추가하거나 청취기를 추가하거나 카메라를 이동할 수 있습니다. 이 경우,
     * 우리는 단지 호주 시드니 근처에 마커를 추가할 뿐이다.
     * Google Play 서비스가 장치에 설치되지 않은 경우 사용자에게 설치할지 묻는 메시지가 표시됩니다.
     * 그것은 SupportMapFragment 안에 있다. 이 메서드는 사용자가 다음을 수행한 경우에만 트리거됩니다.
     * 구글 플레이 서비스를 설치하고 앱으로 돌아갔습니다.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }

    /* requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        ) 메서드에 전달되는 인자중 locationCallback을 구현한 내부 클래스는 LocationResult 객체를
        반환하고 lastLocation 프로퍼티로 Location 객체를 얻습니다.*/
    inner class MyLocationCallBack : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)

            val location = locationResult?.lastLocation
            /* 기기의 GPS 설정이 꺼져 있거나 현재 위치 정보를 얻을 수 없는 경우에 Location객체가
            *  Null일수 있습니다. Location 객체가 null이 아닐때 해당 위도와 경도 위치로 카메라를 이동합니다.*/

            location?.run {
                //17 level로 확대하며 현재 위치로 카메라 이동
                val latLng = LatLng(latitude, longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,17f))

                Log.d("MapsActivity","위도 : $latitude, 경도: $longitude")

//                PolyLine 에 좌표 추가
                polylineOptions.add(latLng)
//                선그리기
                mMap.addPolyline(polylineOptions)
            }
        }
    }

    private fun checkPermission(cancel: () -> Unit, ok: () ->Unit) { // 이 메서드는 함수인자 두 개를 받습니다.두함수는 인자가 없고 반환값도 없습니다.
//       위치 권한이 없는지 검사
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
//            권한이 허용되지 않았을때
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
            )) {
//                이전에 권한을 한번 거부한 적인 있는 경우
                cancel()
            } else {
                // 권한요청
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
                return
        }
//            권한을 수락했을때 실행할 함수
            ok()
    }

    private fun showPermissionInfoDialog() {
//        다이얼로그에 권한이 필요한 이유를 설명
        AlertDialog.Builder(this).apply {
            setTitle("권한이 필요한 이유")
            setMessage("지도에 위치를 표시하려면 위치 정보 권한이 필요합니다.")
            setPositiveButton("권한 요청") {_,_ -> // 긍정버튼과 부정버튼
                // 권한요청
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            setNegativeButton("거부",null ) // 부정버튼을 누르면 아무것도 하지않고 다이얼로그가 닫힘니다.
        }.show()
    }
}