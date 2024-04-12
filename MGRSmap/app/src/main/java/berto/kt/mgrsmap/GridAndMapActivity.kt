package berto.kt.mgrsmap

import android.Manifest
import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthWcdma
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import berto.kt.mgrsmap.databinding.ActivityGirdAndMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import mil.nga.color.Color
import mil.nga.mgrs.MGRS
import mil.nga.mgrs.grid.GridType
import mil.nga.mgrs.tile.MGRSTileProvider
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

enum class DbTable{
    ONE_M, TEN_M, HUNDRED_M, ONE_KM, TEN_KM
}
enum class ConnectionType{
    LTE, WIFI, NOISE
}

class GridAndMapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnCameraIdleListener {

    private val AUDIO_REQUEST_CODE = 2
    private val LOCATION_REQUEST_CODE = 1

    //Colors -------------
    private val CVBAD = 0x80FF0000.toInt()
    private val CBAD = 0x80FFA500.toInt()
    private val COK = 0x80FFFF00.toInt()
    private val CGOOD = 0x80008000.toInt()
    private val CVGOOD = 0x8090EE90.toInt()
    //--------------------
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityGirdAndMapBinding



    private lateinit var activeTiles: MGRSTileProvider
    private lateinit var activeTilesSize: DbTable

    // Specified grids
    private lateinit var tenMeterTiles: MGRSTileProvider
    private lateinit var hundredMeterTiles: MGRSTileProvider
    private lateinit var oneKilometerTiles: MGRSTileProvider
    private lateinit var oneMeterTiles: MGRSTileProvider
    private lateinit var tenKilometerTiles: MGRSTileProvider

    private var timer: Int = 0

    //Location stuff
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var followUserSwitch : Switch

    private lateinit var cordTextView: TextView
    private lateinit var userCordTextView: TextView

    private lateinit var valueType: Button

    private lateinit var userLocation: LatLng
    private lateinit var userCords: MGRS


    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationRequest = LocationRequest.Builder(1000).setPriority(Priority.PRIORITY_HIGH_ACCURACY).build()

    private val locationCallback = object : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {

            for (location in locationResult.locations) {
                // Update UI with location data
                // Changing the text for user cords, also moving the camera if the option is enabled
                followUserSwitch = findViewById<Switch>(R.id.followUserSwitch)

                if(::mMap.isInitialized) {
                    if (mMap.isMyLocationEnabled) {
                        if(followUserSwitch.isChecked){
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), mMap.cameraPosition.zoom))

                        }
                        userLocation = LatLng(location.latitude, location.longitude)
                        userCords = activeTiles.getMGRS(userLocation)
                    }
                }

                userCordTextView = findViewById<TextView>(R.id.userCords)
                userCordTextView.text = userCords.toString()

            }
        }
    }

    //settings
    private lateinit var gridColor: Color
    //numero di misurazioni necessarie per colorare la mappa per ogni griglia
    private var x1m : Int = 1
    private var x10m : Int = 3
    private var x100m : Int = 5
    private var x1km : Int = 10
    private var x10km : Int = 20
    //cooldown tasto registra dati
    private var n1m : Int = 60
    private var n10m : Int = 300
    private var n100m : Int = 600
    private var n1km : Int = 900
    private var n10km : Int = 1200

    //db
    private lateinit var db: MGRSDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        gridColor = when(intent.extras!!.getString("gridColor")){
            "black" -> Color.black()
            "gray" -> Color.gray()
            "purple" -> Color.purple()
            else -> Color.black()
        }
        x1m = intent.extras!!.getInt("x1m")
        x10m = intent.extras!!.getInt("x10m")
        x100m = intent.extras!!.getInt("x100m")
        x1km = intent.extras!!.getInt("x1km")
        x10km = intent.extras!!.getInt("x10km")
        n1m = intent.extras!!.getInt("n1m")
        n10m = intent.extras!!.getInt("n10m")
        n100m = intent.extras!!.getInt("n100m")
        n1km = intent.extras!!.getInt("n1km")
        n10km = intent.extras!!.getInt("n10km")

        binding = ActivityGirdAndMapBinding.inflate(layoutInflater)
        setContentView(binding.root)



        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


        Thread{ //reduce the workload on the main thread
            setOnClickListeners()
            db = MGRSDatabase.getDatabase(this)
        }.start()

    }
    private fun scheduleJob() {
        println("Notification job scheduled - MGRSMap")
        val serviceComponent = ComponentName(this, LocationCheckJob::class.java)
        val builder = JobInfo.Builder(0, serviceComponent)
        builder.setPeriodic(60 * 60 * 1000)  // 1 hour
        val jobScheduler = getSystemService(JobScheduler::class.java)
        jobScheduler?.schedule(builder.build())
    }
    private fun cancelJobScheduler(){
        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.cancel(0)
    }

    override fun onResume() {
        super.onResume()

        //if the user has given permission to use location, enable it
        //if mMap has been initialized or everything crashes
        if(::mMap.isInitialized){
            if (!mMap.isMyLocationEnabled) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    enableLocation()
            }
        }

        if(::fusedLocationClient.isInitialized)
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        Thread{
            cancelJobScheduler()
        }.start()

    }

    override fun onPause() {
        super.onPause()
        if(::fusedLocationClient.isInitialized)
            fusedLocationClient.removeLocationUpdates(locationCallback)

    }

    override fun onStop(){
        super.onStop()
        if(::fusedLocationClient.isInitialized)
            fusedLocationClient.removeLocationUpdates(locationCallback)
        val sharedPref = getSharedPreferences("mgrsSettings", Context.MODE_PRIVATE)
        if (sharedPref.getBoolean("shouldSendNoti",false)){
            scheduleJob()
        }
    }

    override fun onCameraIdle() {
        val cameraPosition: CameraPosition = mMap.cameraPosition

        val cameraCords: String = activeTiles.getCoordinate(cameraPosition.target, cameraPosition.zoom.toInt())

        cordTextView = findViewById<TextView>(R.id.cameraCords)
        cordTextView.text = cameraCords

    }

    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
        mMap.setOnCameraIdleListener(this)

        requestLocationRuntime() //request permissions

        //val almaMater = LatLng(44.498955, 11.327591)

        //zoom in on italy
        val italy = LatLngBounds(LatLng(36.71703,  7.05809), LatLng(46.99623, 18.37819))

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(italy.center, 5.0f))

        val context = this

        // Specified grids
        tenMeterTiles = MGRSTileProvider.create(
            context,
            GridType.TEN_METER
        )
        hundredMeterTiles = MGRSTileProvider.create(
            context,
            GridType.HUNDRED_METER
        )
        oneKilometerTiles = MGRSTileProvider.create(
            context,
            GridType.KILOMETER,
        )


        oneMeterTiles = MGRSTileProvider.create(
            context,
            GridType.METER
        )

        tenKilometerTiles = MGRSTileProvider.create(
            context,
            GridType.TEN_KILOMETER
        )

        activeTiles = hundredMeterTiles
        activeTilesSize = DbTable.HUNDRED_M
        activeTiles.getGrid(GridType.HUNDRED_METER).setColor(GridType.HUNDRED_METER, gridColor)
        activeTiles.getGrid(GridType.HUNDRED_METER).linesMaxZoom = null


        mMap.addTileOverlay(TileOverlayOptions().tileProvider(activeTiles))



        //set up the location client


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }



    }

    private fun setOnClickListeners(){
        var gridIsOn = true


        //setting up zoom FABs
        val zoomInFab = findViewById<FloatingActionButton>(R.id.zoomInFab)
        zoomInFab.setOnClickListener { mMap.animateCamera(CameraUpdateFactory.zoomIn()) }
        val zoomOutFab = findViewById<FloatingActionButton>(R.id.zoomOutFab)
        zoomOutFab.setOnClickListener { mMap.animateCamera(CameraUpdateFactory.zoomOut()) }

        //setting up copy to clipboard for MGRS cords
        cordTextView = findViewById<TextView>(R.id.cameraCords)
        userCordTextView = findViewById<TextView>(R.id.userCords)
        userCordTextView.setOnLongClickListener(){
            copyToClipBoardText(userCordTextView.text.toString())
            true
        }
        cordTextView.setOnLongClickListener(){
            copyToClipBoardText(cordTextView.text.toString())
            true
        }

        valueType = findViewById<Button>(R.id.valueTypeButton)

        val recordFab = findViewById<FloatingActionButton>(R.id.recordDataButton)
        recordFab.setOnClickListener{
            if(!recordFab.isEnabled){
                Snackbar.make(
                    binding.root,
                    "Aspetta $timer minuti prima di registrare di nuovo",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            when(valueType.text){
                "MOBILE" -> {
                    recordDataLTE()
                }
                "WIFI" -> {
                    recordDataWIFI()
                }
                "AUDIO" -> {
                    recordDataNoise()
                }
            }

        }

        valueType.setOnClickListener(){
            when(valueType.text){
                "MOBILE" -> valueType.text = "WIFI"
                "WIFI" -> valueType.text = "AUDIO"
                "AUDIO" -> valueType.text = "MOBILE"
            }
        }
        //setting up options menu FABs
        val gridFab = findViewById<FloatingActionButton>(R.id.gridOnOffFab)
        val mapTypeFab = findViewById<FloatingActionButton>(R.id.mapTypeFab)
        val gridSizeFab = findViewById<FloatingActionButton>(R.id.gridSizeFab)
        val optionFab = findViewById<FloatingActionButton>(R.id.optionFab)
        val fab1m = findViewById<FloatingActionButton>(R.id.fab1M)
        val fab10m = findViewById<FloatingActionButton>(R.id.fab10M)
        val fab100m = findViewById<FloatingActionButton>(R.id.fab100M)
        val fab1km = findViewById<FloatingActionButton>(R.id.fab1Km)
        val fab10km = findViewById<FloatingActionButton>(R.id.fab10Km)
        val backArrowFab = findViewById<FloatingActionButton>(R.id.backArrowFab)
        val colorMapFab = findViewById<FloatingActionButton>(R.id.colorMapFab)


        val followUserLayout = findViewById<LinearLayout>(R.id.followUserLayout)
        val followUserImage = findViewById<ImageView>(R.id.followUserImage)
        val followUserSwitch = findViewById<Switch>(R.id.followUserSwitch)

        optionFab.setOnClickListener {
            if(optionFab.isActivated){
                optionFab.isActivated = false
                optionFab.setImageDrawable(getDrawable(R.drawable.ic_settings))
                zoomInFab.hide()
                zoomOutFab.hide()
                gridFab.hide()
                mapTypeFab.hide()
                gridSizeFab.hide()
                followUserLayout.visibility = LinearLayout.INVISIBLE
                followUserImage.visibility = ImageView.INVISIBLE
                followUserSwitch.visibility = Switch.INVISIBLE
                recordFab.show()
                valueType.visibility = Button.INVISIBLE
                colorMapFab.hide()
            }else{
                optionFab.isActivated = true
                optionFab.setImageDrawable(getDrawable(R.drawable.ic_close_x))
                zoomInFab.show()
                zoomOutFab.show()
                gridFab.show()
                mapTypeFab.show()
                gridSizeFab.show()
                followUserLayout.visibility = LinearLayout.VISIBLE
                followUserImage.visibility = ImageView.VISIBLE
                followUserSwitch.visibility = Switch.VISIBLE
                recordFab.hide()
                valueType.visibility = Button.VISIBLE
                colorMapFab.show()
            }
        }

        backArrowFab.setOnClickListener{
            backArrowFab.hide()
            fab1m.hide()
            fab10m.hide()
            fab100m.hide()
            fab1km.hide()
            fab10km.hide()

            gridFab.show()
            mapTypeFab.show()
            gridSizeFab.show()
            optionFab.show()
            optionFab.isActivated = true
        }

        var mapIsColored = false
        fun resetColorMapFab(){
            mapIsColored = false
            colorMapFab.setImageDrawable(getDrawable(R.drawable.baseline_format_color_fill_24))
        }
        colorMapFab.setOnClickListener{
            if(!mapIsColored){
                mapIsColored = true
                colorMapFab.setImageDrawable(getDrawable(R.drawable.baseline_layers_clear_24))
                colorMap(activeTilesSize)
            }else{
                resetColorMapFab()
                when(activeTiles){
                    oneMeterTiles -> oneMGrid()
                    tenMeterTiles -> tenMGrid()
                    hundredMeterTiles -> hundredMGrid()
                    oneKilometerTiles -> oneKMGrid()
                    tenKilometerTiles -> tenKMGrid()
                }
            }
        }

        mapTypeFab.setOnClickListener{
            if(mMap.mapType == GoogleMap.MAP_TYPE_HYBRID){
                mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                mapTypeFab.setImageDrawable(getDrawable(R.drawable.ic_satellite))
            }else{
                mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
                mapTypeFab.setImageDrawable(getDrawable(R.drawable.ic_street))
            }
        }

        gridFab.setOnClickListener {
            mapIsColored = false
            if(gridIsOn){
                gridIsOn = false
                gridFab.setImageDrawable(getDrawable(R.drawable.ic_grid_on))
                mMap.clear()
            }else{
                gridIsOn = true
                gridFab.setImageDrawable(getDrawable(R.drawable.ic_grid_off))
                when(activeTiles){
                    oneMeterTiles -> oneMGrid()
                    tenMeterTiles -> tenMGrid()
                    hundredMeterTiles -> hundredMGrid()
                    oneKilometerTiles -> oneKMGrid()
                    tenKilometerTiles -> tenKMGrid()
                }
            }
        }

        gridSizeFab.setOnClickListener{
            gridFab.hide()
            mapTypeFab.hide()
            gridSizeFab.hide()
            optionFab.hide()
            optionFab.isActivated = false

            backArrowFab.show()
            fab1m.show()
            fab10m.show()
            fab100m.show()
            fab1km.show()
            fab10km.show()

        }

        fab1m.setOnClickListener{
            mapIsColored = false
            oneMGrid()
            fab1m.isEnabled = false
            fab10m.isEnabled = true
            fab100m.isEnabled = true
            fab1km.isEnabled = true
            fab10km.isEnabled = true
            resetColorMapFab()
        }
        fab10m.setOnClickListener{
            mapIsColored = false
            tenMGrid()
            fab1m.isEnabled = true
            fab10m.isEnabled = false
            fab100m.isEnabled = true
            fab1km.isEnabled = true
            fab10km.isEnabled = true
            resetColorMapFab()
        }
        fab100m.setOnClickListener{
            mapIsColored = false
            hundredMGrid()
            fab1m.isEnabled = true
            fab10m.isEnabled = true
            fab100m.isEnabled = false
            fab1km.isEnabled = true
            fab10km.isEnabled = true
            resetColorMapFab()
        }
        fab1km.setOnClickListener{
            mapIsColored = false
            oneKMGrid()
            fab1m.isEnabled = true
            fab10m.isEnabled = true
            fab100m.isEnabled = true
            fab1km.isEnabled = false
            fab10km.isEnabled = true
            resetColorMapFab()
        }
        fab10km.setOnClickListener{
            mapIsColored = false
            tenKMGrid()
            fab1m.isEnabled = true
            fab10m.isEnabled = true
            fab100m.isEnabled = true
            fab1km.isEnabled = true
            fab10km.isEnabled = false
            resetColorMapFab()
        }

        //setting up long click listeners for FABs

        colorMapFab.setOnLongClickListener{
            if (mapIsColored){
                Snackbar.make(
                    binding.root,
                    "Rimuove la colorazione delle zone della mappa",
                    Snackbar.LENGTH_SHORT
                ).show()
            }else{
                val snackbar = Snackbar.make(
                    binding.root,
                    "Colora la mappa in base alla qualitá dei valori registrati nelle zone con sufficienti misurazioni",
                    Snackbar.LENGTH_SHORT
                )
                val snackbarView = snackbar.view
                val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                textView.maxLines = 3
                val params = snackbarView.layoutParams as ViewGroup.LayoutParams
                params.height = 200
                snackbarView.layoutParams = params
                snackbar.show()
            }

            true
        }
        mapTypeFab.setOnLongClickListener {
            Snackbar.make(
                binding.root,
                "Cambia il tipo di mappa",
                Snackbar.LENGTH_SHORT
            ).show()
            true
        }

        gridFab.setOnLongClickListener {
            Snackbar.make(
                binding.root,
                "Attiva/Disattiva la griglia MGRS",
                Snackbar.LENGTH_SHORT
            ).show()
            true
        }

        gridSizeFab.setOnLongClickListener {
            Snackbar.make(
                binding.root,
                "Cambia la dimensione della griglia MGRS",
                Snackbar.LENGTH_SHORT
            ).show()
            true
        }


        followUserImage.setOnLongClickListener {
            Snackbar.make(
                binding.root,
                "Segui la posizione dell'utente quando si sposta",
                Snackbar.LENGTH_SHORT
            ).show()
            true
        }
        followUserSwitch.setOnLongClickListener {
            Snackbar.make(
                binding.root,
                "Segui la posizione dell'utente quando si sposta",
                Snackbar.LENGTH_SHORT
            ).show()
            true
        }

    }

    private fun copyToClipBoardText(text: String){

        if(text=="") return

        val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText("", text)
        clipboardManager.setPrimaryClip(clip)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
            Snackbar.make(binding.root, "Copied", Snackbar.LENGTH_SHORT).show()
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //DATABASE FUNCTIONS -----------------------------------------------
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun checkExisting(location: String, table: DbTable): Any? {
        when(table){
            DbTable.ONE_M -> {
                val oneM = db.MGRSDao().getOneM(location)
                if(oneM != null){
                    return oneM
                }
            }
            DbTable.TEN_M -> {
                val tenM = db.MGRSDao().getTenM(location)
                if(tenM != null){
                    return tenM
                }
            }
            DbTable.HUNDRED_M -> {
                val hundredM = db.MGRSDao().getHundredM(location)
                if(hundredM != null){
                    return hundredM
                }
            }
            DbTable.ONE_KM -> {
                val oneKM = db.MGRSDao().getOneKM(location)
                if(oneKM != null){
                    return oneKM
                }
            }
            DbTable.TEN_KM -> {
                val tenKM = db.MGRSDao().getTenKM(location)
                if(tenKM != null){
                    return tenKM
                }
            }
        }
        return null
    }

    private fun checkExistingAndAdd(location: String, value: Float, table:DbTable, connection: ConnectionType){
        val exists = checkExisting(location, table)
        if(exists != null) {
            updateDatabase(table, connection, location, value)  //Could change updateDatabase to take as argument exists to reduce the number of queries
        }else{
            insertDatabase(table, location)
            updateDatabase(table, connection, location, value)
        }
    }

    private fun handleAddDataToDatabase(location: String, value: Float, table:DbTable, connection: ConnectionType, gridSize: GridType){
        when(gridSize){
            GridType.METER -> {
                checkExistingAndAdd(location, value, table, connection)

                var locationMGRS = MGRS.parse(location)
                var newLocation = locationMGRS.coordinate(GridType.TEN_METER)
                checkExistingAndAdd(newLocation, value, DbTable.TEN_M, connection)

                locationMGRS = MGRS.parse(location)
                newLocation = locationMGRS.coordinate(GridType.HUNDRED_METER)
                checkExistingAndAdd(newLocation, value, DbTable.HUNDRED_M, connection)

                locationMGRS = MGRS.parse(location)
                newLocation = locationMGRS.coordinate(GridType.KILOMETER)
                checkExistingAndAdd(newLocation, value, DbTable.ONE_KM, connection)

                locationMGRS = MGRS.parse(location)
                newLocation = locationMGRS.coordinate(GridType.TEN_KILOMETER)
                checkExistingAndAdd(newLocation, value, DbTable.TEN_KM, connection)
                
            }
            GridType.TEN_METER -> {
                checkExistingAndAdd(location, value, table, connection)

                var locationMGRS = MGRS.parse(location)
                var newLocation = locationMGRS.coordinate(GridType.HUNDRED_METER)
                checkExistingAndAdd(newLocation, value, DbTable.HUNDRED_M, connection)

                locationMGRS = MGRS.parse(location)
                newLocation = locationMGRS.coordinate(GridType.KILOMETER)
                checkExistingAndAdd(newLocation, value, DbTable.ONE_KM, connection)

                locationMGRS = MGRS.parse(location)
                newLocation = locationMGRS.coordinate(GridType.TEN_KILOMETER)
                checkExistingAndAdd(newLocation, value, DbTable.TEN_KM, connection)
            }
            GridType.HUNDRED_METER -> {
                checkExistingAndAdd(location, value, table, connection)

                var locationMGRS = MGRS.parse(location)
                var newLocation = locationMGRS.coordinate(GridType.KILOMETER)
                checkExistingAndAdd(newLocation, value, DbTable.ONE_KM, connection)

                locationMGRS = MGRS.parse(location)
                newLocation = locationMGRS.coordinate(GridType.TEN_KILOMETER)
                checkExistingAndAdd(newLocation, value, DbTable.TEN_KM, connection)
            }
            GridType.KILOMETER -> {
                checkExistingAndAdd(location, value, table, connection)

                val locationMGRS = MGRS.parse(location)
                val newLocation = locationMGRS.coordinate(GridType.TEN_KILOMETER)
                checkExistingAndAdd(newLocation, value, DbTable.TEN_KM, connection)
            }
            GridType.TEN_KILOMETER -> {checkExistingAndAdd(location, value, table, connection)}
            else -> { return }
        }
    }

    private fun insertDatabase(table: DbTable, location: String){
        when(table){
            DbTable.ONE_M -> {
                val newOneM = OneM(location, 0f, 0f, 0f, 0, 0, 0)
                db.MGRSDao().insertOneM(newOneM)
            }
            DbTable.TEN_M -> {
                val newTenM = TenM(location, 0f, 0f, 0f, 0, 0, 0)
                db.MGRSDao().insertTenM(newTenM)
            }
            DbTable.HUNDRED_M-> {
                val newHundredM = HundredM(location, 0f, 0f, 0f, 0, 0, 0)
                db.MGRSDao().insertHundredM(newHundredM)
            }
            DbTable.ONE_KM -> {
                val newOneKM = OneKM(location, 0f, 0f, 0f, 0, 0, 0)
                db.MGRSDao().insertOneKM(newOneKM)
            }
            DbTable.TEN_KM -> {
                val newTenKM = TenKM(location, 0f, 0f, 0f, 0, 0, 0)
                db.MGRSDao().insertTenKM(newTenKM)
            }
        }
    }

    private fun updateDatabase(table: DbTable, connection: ConnectionType, mgrsLocation: String, signalStrength: Float){
        when(table){
            DbTable.ONE_M -> {
                val oneM = db.MGRSDao().getOneM(mgrsLocation)!!
                when(connection){
                    ConnectionType.LTE -> {
                        var count = oneM.numLTE
                        var value = oneM.strengthLTE
                        value *= count
                        value += signalStrength
                        count++
                        value /= count
                        val newOneM = OneM(mgrsLocation, value, oneM.strengthWIFI, oneM.noise, count, oneM.numWIFI, oneM.numNoise)
                        db.MGRSDao().updateOneM(newOneM)
                    }
                    ConnectionType.WIFI -> {
                        var count = oneM.numWIFI
                        var value = oneM.strengthWIFI
                        value *= count
                        value += signalStrength
                        count++
                        value /= count
                        val newOneM = OneM(mgrsLocation, oneM.strengthLTE, value, oneM.noise, oneM.numLTE, count, oneM.numNoise)
                        db.MGRSDao().updateOneM(newOneM)
                    }
                    ConnectionType.NOISE -> {
                        var count = oneM.numNoise
                        var value = oneM.noise
                        value *= count
                        value += signalStrength
                        count++
                        value /= count
                        val newOneM = OneM(mgrsLocation, oneM.strengthLTE, oneM.strengthWIFI, value, oneM.numLTE, oneM.numWIFI, count)
                        db.MGRSDao().updateOneM(newOneM)
                    }
                }
                return
            }
            DbTable.TEN_M -> {
                val tenM = db.MGRSDao().getTenM(mgrsLocation)!!
                when(connection){
                    ConnectionType.LTE -> {
                        var count = tenM.numLTE
                        var value = tenM.strengthLTE
                        value *= count
                        value += signalStrength
                        count++
                        value /= count
                        val newTenM = TenM(mgrsLocation, value, tenM.strengthWIFI, tenM.noise, count, tenM.numWIFI, tenM.numNoise)
                        db.MGRSDao().updateTenM(newTenM)
                    }
                    ConnectionType.WIFI -> {
                        var count = tenM.numWIFI
                        var value = tenM.strengthWIFI
                        value *= count
                        value += signalStrength
                        count++
                        value /= count
                        val newTenM = TenM(mgrsLocation, tenM.strengthLTE, value, tenM.noise, tenM.numLTE, count, tenM.numNoise)
                        db.MGRSDao().updateTenM(newTenM)
                    }
                    ConnectionType.NOISE -> {
                        var count = tenM.numNoise
                        var value = tenM.noise
                        value *= count
                        value += signalStrength
                        count++
                        value /= count
                        val newTenM = TenM(mgrsLocation, tenM.strengthLTE, tenM.strengthWIFI, value, tenM.numLTE, tenM.numWIFI, count)
                        db.MGRSDao().updateTenM(newTenM)
                    }
                }
                return
            }
            DbTable.HUNDRED_M -> {
                val hundredM = db.MGRSDao().getHundredM(mgrsLocation)!!
                when(connection){
                    ConnectionType.LTE -> {
                        var count = hundredM.numLTE
                        var value = hundredM.strengthLTE
                        value *= count
                        value += signalStrength
                        count++
                        value /= count
                        val newHundredM = HundredM(mgrsLocation, value, hundredM.strengthWIFI, hundredM.noise, count, hundredM.numWIFI, hundredM.numNoise)
                        db.MGRSDao().updateHundredM(newHundredM)
                    }
                    ConnectionType.WIFI -> {
                        var count = hundredM.numWIFI
                        var value = hundredM.strengthWIFI
                        value *= count
                        value += signalStrength
                        count++
                        value /= count
                        val newHundredM = HundredM(mgrsLocation, hundredM.strengthLTE, value, hundredM.noise, hundredM.numLTE, count, hundredM.numNoise)
                        db.MGRSDao().updateHundredM(newHundredM)
                    }
                    ConnectionType.NOISE -> {
                        var count = hundredM.numNoise
                        var value = hundredM.noise
                        value *= count
                        value += signalStrength
                        count++
                        value /= count
                        val newHundredM = HundredM(mgrsLocation, hundredM.strengthLTE, hundredM.strengthWIFI, value, hundredM.numLTE, hundredM.numWIFI, count)
                        db.MGRSDao().updateHundredM(newHundredM)
                    }
                }
                return
            }
            DbTable.ONE_KM -> {
                val oneKM = db.MGRSDao().getOneKM(mgrsLocation)!!
                when(connection){
                    ConnectionType.LTE -> {
                        var count = oneKM.numLTE
                        var value = oneKM.strengthLTE
                        value *= count
                        value += signalStrength
                        count++
                        value /= count
                        val newOneKM = OneKM(mgrsLocation, value, oneKM.strengthWIFI, oneKM.noise, count, oneKM.numWIFI, oneKM.numNoise)
                        db.MGRSDao().updateOneKM(newOneKM)
                    }
                    ConnectionType.WIFI -> {
                        var count = oneKM.numWIFI
                        var value = oneKM.strengthWIFI
                        value *= count
                        value += signalStrength
                        count++
                        value /= count
                        val newOneKM = OneKM(mgrsLocation, oneKM.strengthLTE, value, oneKM.noise, oneKM.numLTE, count, oneKM.numNoise)
                        db.MGRSDao().updateOneKM(newOneKM)
                    }
                    ConnectionType.NOISE -> {
                        var count = oneKM.numNoise
                        var value = oneKM.noise
                        value *= count
                        value += signalStrength
                        count++
                        value /= count
                        val newOneKM = OneKM(mgrsLocation, oneKM.strengthLTE, oneKM.strengthWIFI, value, oneKM.numLTE, oneKM.numWIFI, count)
                        db.MGRSDao().updateOneKM(newOneKM)
                    }
                }
                return
            }
            DbTable.TEN_KM -> {
                val tenKM = db.MGRSDao().getTenKM(mgrsLocation)!!
                when(connection){
                    ConnectionType.LTE -> {
                        var count = tenKM.numLTE
                        var value = tenKM.strengthLTE
                        value *= count
                        value += signalStrength
                        count++
                        value /= count
                        val newTenKM = TenKM(mgrsLocation, value, tenKM.strengthWIFI, tenKM.noise, count, tenKM.numWIFI, tenKM.numNoise)
                        db.MGRSDao().updateTenKM(newTenKM)
                    }
                    ConnectionType.WIFI -> {
                        var count = tenKM.numWIFI
                        var value = tenKM.strengthWIFI
                        value *= count
                        value += signalStrength
                        count++
                        value /= count
                        val newTenKM = TenKM(mgrsLocation, tenKM.strengthLTE, value, tenKM.noise, tenKM.numLTE, count, tenKM.numNoise)
                        db.MGRSDao().updateTenKM(newTenKM)
                    }
                    ConnectionType.NOISE -> {
                        var count = tenKM.numNoise
                        var value = tenKM.noise
                        value *= count
                        value += signalStrength
                        count++
                        value /= count
                        val newTenKM = TenKM(mgrsLocation, tenKM.strengthLTE, tenKM.strengthWIFI, value, tenKM.numLTE, tenKM.numWIFI, count)
                        db.MGRSDao().updateTenKM(newTenKM)
                    }
                }
                return
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //FUNCTIONS TO GET SIGNAL STRENGTH -----------------------------------------------
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private fun recordFabCooldown(){
        val recordFab = findViewById<FloatingActionButton>(R.id.recordDataButton)
        recordFab.isEnabled = false
        when(activeTilesSize){
            DbTable.ONE_M ->{
                Handler(Looper.getMainLooper()).postDelayed({
                    recordFab.isEnabled = true
                }, n1m.toLong()*1000)
                timer = 1
                val coolDownText = findViewById<TextView>(R.id.cooldownTextView)
                val countDownTimer = object : CountDownTimer(n1m.toLong()*1000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        coolDownText.visibility = TextView.VISIBLE
                        coolDownText.text = (millisUntilFinished / 1000).toString()
                    }

                    override fun onFinish() {
                        coolDownText.visibility = TextView.INVISIBLE
                    }
                }.start()
            }
            DbTable.TEN_M ->{
                Handler(Looper.getMainLooper()).postDelayed({
                    recordFab.isEnabled = true
                }, n10m.toLong()*1000)
                timer = 5
                val coolDownText = findViewById<TextView>(R.id.cooldownTextView)
                val countDownTimer = object : CountDownTimer(n10m.toLong()*1000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        coolDownText.visibility = TextView.VISIBLE
                        coolDownText.text = (millisUntilFinished / 1000).toString()
                    }

                    override fun onFinish() {
                        coolDownText.visibility = TextView.INVISIBLE
                    }
                }.start()
            }
            DbTable.HUNDRED_M ->{
                Handler(Looper.getMainLooper()).postDelayed({
                    recordFab.isEnabled = true
                }, n100m.toLong()*1000)
                timer = 10
                val coolDownText = findViewById<TextView>(R.id.cooldownTextView)
                val countDownTimer = object : CountDownTimer(n100m.toLong()*1000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        coolDownText.visibility = TextView.VISIBLE
                        coolDownText.text = (millisUntilFinished / 1000).toString()
                    }

                    override fun onFinish() {
                        coolDownText.visibility = TextView.INVISIBLE
                    }
                }.start()
            }
            DbTable.ONE_KM ->{
                Handler(Looper.getMainLooper()).postDelayed({
                    recordFab.isEnabled = true
                }, n1km.toLong()*1000)
                timer = 15
                val coolDownText = findViewById<TextView>(R.id.cooldownTextView)
                val countDownTimer = object : CountDownTimer(n1km.toLong()*1000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        coolDownText.visibility = TextView.VISIBLE
                        coolDownText.text = (millisUntilFinished / 1000).toString()
                    }

                    override fun onFinish() {
                        coolDownText.visibility = TextView.INVISIBLE
                    }
                }.start()
            }
            DbTable.TEN_KM ->{
                Handler(Looper.getMainLooper()).postDelayed({
                    recordFab.isEnabled = true
                }, n10km.toLong()*1000)
                timer = 20
                val coolDownText = findViewById<TextView>(R.id.cooldownTextView)
                val countDownTimer = object : CountDownTimer(n10km.toLong()*1000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        coolDownText.visibility = TextView.VISIBLE
                        coolDownText.text = (millisUntilFinished / 1000).toString()
                    }

                    override fun onFinish() {
                        coolDownText.visibility = TextView.INVISIBLE
                    }
                }.start()
            }
        }
    }


    private fun recordDataLTE(){
        recordCellularSignalStrength(this) {
            if(it > -120 && it < -20){
                Snackbar.make(binding.root, "Valore registrato: $it dBm", Snackbar.LENGTH_SHORT).show()
                val value = it
                Thread{
                    when(activeTilesSize){
                        DbTable.ONE_M -> {
                            handleAddDataToDatabase(userCords.coordinate(GridType.METER), value.toFloat(), DbTable.ONE_M, ConnectionType.LTE, GridType.METER)
                        }
                        DbTable.TEN_M -> {
                            handleAddDataToDatabase(userCords.coordinate(GridType.TEN_METER), value.toFloat(), DbTable.TEN_M, ConnectionType.LTE, GridType.TEN_METER)
                        }
                        DbTable.HUNDRED_M -> {
                            handleAddDataToDatabase(userCords.coordinate(GridType.HUNDRED_METER), value.toFloat(), DbTable.HUNDRED_M, ConnectionType.LTE, GridType.HUNDRED_METER)
                        }
                        DbTable.ONE_KM -> {
                            handleAddDataToDatabase(userCords.coordinate(GridType.KILOMETER), value.toFloat(), DbTable.ONE_KM, ConnectionType.LTE, GridType.KILOMETER)
                        }
                        DbTable.TEN_KM -> {
                            handleAddDataToDatabase(userCords.coordinate(GridType.TEN_KILOMETER), value.toFloat(), DbTable.TEN_KM, ConnectionType.LTE, GridType.TEN_KILOMETER)
                        }
                    }
                }.start()
                recordFabCooldown()
            }else{
                //something went wrong
                Snackbar.make(binding.root, "Errore nel rilevamento del segnale, valore ricevuto inadatto", Snackbar.LENGTH_SHORT).show()
            }

        }

    }

    private fun recordDataWIFI(){
        var value: Int
        recordWifiSignalStrength(this, object : SignalStrengthCallback {
            override fun onSignalStrengthReceived(strength: Int) {
                if (strength != -1){
                    if(strength > -120 && strength < -20){
                        Snackbar.make(binding.root, "Valore registrato: $strength dBm", Snackbar.LENGTH_SHORT).show()
                        value = strength
                        Thread{
                            when(activeTilesSize){
                                DbTable.ONE_M -> {
                                    handleAddDataToDatabase(userCords.coordinate(GridType.METER), value.toFloat(), DbTable.ONE_M, ConnectionType.WIFI, GridType.METER)
                                }
                                DbTable.TEN_M -> {
                                    handleAddDataToDatabase(userCords.coordinate(GridType.TEN_METER), value.toFloat(), DbTable.TEN_M, ConnectionType.WIFI, GridType.TEN_METER)
                                }
                                DbTable.HUNDRED_M -> {
                                    handleAddDataToDatabase(userCords.coordinate(GridType.HUNDRED_METER), value.toFloat(), DbTable.HUNDRED_M, ConnectionType.WIFI, GridType.HUNDRED_METER)
                                }
                                DbTable.ONE_KM -> {
                                    handleAddDataToDatabase(userCords.coordinate(GridType.KILOMETER), value.toFloat(), DbTable.ONE_KM, ConnectionType.WIFI, GridType.KILOMETER)
                                }
                                DbTable.TEN_KM -> {
                                    handleAddDataToDatabase(userCords.coordinate(GridType.TEN_KILOMETER), value.toFloat(), DbTable.TEN_KM, ConnectionType.WIFI, GridType.TEN_KILOMETER)
                                }
                            }
                        }.start()
                        recordFabCooldown()
                    }else{
                        //something went wrong
                        Snackbar.make(binding.root, "Errore nel rilevamento del segnale, valore ricevuto inadatto", Snackbar.LENGTH_SHORT).show()
                    }
                }else{
                    Snackbar.make(binding.root, "Wifi non disponibile", Snackbar.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun recordDataNoise(){
        captureNoiseLevel(object : NoiseLevelCallback {
            override fun onNoiseLevelReceived(noiseLevel: Double) {
                if(noiseLevel <= 0 && noiseLevel >= -160){
                    Snackbar.make(binding.root, "Valore relativo registrato: $noiseLevel dB", Snackbar.LENGTH_SHORT).show()
                    val value = noiseLevel.toFloat()
                    Thread{
                        when(activeTilesSize){
                            DbTable.ONE_M -> {
                                handleAddDataToDatabase(userCords.coordinate(GridType.METER), value, DbTable.ONE_M, ConnectionType.NOISE, GridType.METER)
                            }
                            DbTable.TEN_M -> {
                                handleAddDataToDatabase(userCords.coordinate(GridType.TEN_METER), value, DbTable.TEN_M, ConnectionType.NOISE, GridType.TEN_METER)
                            }
                            DbTable.HUNDRED_M -> {
                                handleAddDataToDatabase(userCords.coordinate(GridType.HUNDRED_METER), value, DbTable.HUNDRED_M, ConnectionType.NOISE, GridType.HUNDRED_METER)
                            }
                            DbTable.ONE_KM -> {
                                handleAddDataToDatabase(userCords.coordinate(GridType.KILOMETER), value, DbTable.ONE_KM, ConnectionType.NOISE, GridType.KILOMETER)
                            }
                            DbTable.TEN_KM -> {
                                handleAddDataToDatabase(userCords.coordinate(GridType.TEN_KILOMETER), value, DbTable.TEN_KM, ConnectionType.NOISE, GridType.TEN_KILOMETER)
                            }
                        }
                    }.start()
                    runOnUiThread {recordFabCooldown()}
                }else{
                    //something went wrong
                    Snackbar.make(binding.root, "Errore nel rilevamento del rumore, valore ricevuto inadatto", Snackbar.LENGTH_SHORT).show()
                }

            }
        })

    }

    private fun recordCellularSignalStrength(context: Context, onSignalStrengthReceived: (Int) -> Unit) {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val telephonyCallback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {

                    val lteStrength = signalStrength.cellSignalStrengths.filterIsInstance<CellSignalStrengthLte>().firstOrNull()
                    lteStrength?.let {
                        onSignalStrengthReceived(it.rssi)
                    }

                    val wcdmaStrength = signalStrength.cellSignalStrengths.filterIsInstance<CellSignalStrengthWcdma>().firstOrNull()
                    wcdmaStrength?.let {
                        onSignalStrengthReceived(it.dbm)
                    }

                    telephonyManager.unregisterTelephonyCallback(this)
                }
            }

            telephonyManager.registerTelephonyCallback(context.mainExecutor, telephonyCallback)

        } else {
            val phoneStateListener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {

                    val lteStrength = signalStrength.cellSignalStrengths.filterIsInstance<CellSignalStrengthLte>().firstOrNull()
                    lteStrength?.let {
                        onSignalStrengthReceived(it.rssi)
                    }


                    val wcdmaStrength = signalStrength.cellSignalStrengths.filterIsInstance<CellSignalStrengthWcdma>().firstOrNull()
                    wcdmaStrength?.let {
                        onSignalStrengthReceived(it.dbm)
                    }

                    telephonyManager.listen(this, PhoneStateListener.LISTEN_NONE)
                }
            }

            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
        }
    }



    interface SignalStrengthCallback {
        fun onSignalStrengthReceived(strength: Int)
    }

    private fun recordWifiSignalStrength(context: Context, callback: SignalStrengthCallback) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            if (wifiManager.isWifiEnabled) {
                callback.onSignalStrengthReceived(wifiInfo.rssi)
            } else {
                callback.onSignalStrengthReceived(-1)
            }
        } else {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

            if (!wifiManager.isWifiEnabled) {
                callback.onSignalStrengthReceived(-1)
                return
            }
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        callback.onSignalStrengthReceived(networkCapabilities.signalStrength)
                    } else {
                        callback.onSignalStrengthReceived(-1)
                    }
                    connectivityManager.unregisterNetworkCallback(this)
                }
            }
            val networkRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        }
    }

    interface NoiseLevelCallback {
        fun onNoiseLevelReceived(noiseLevel: Double)
    }

    private fun captureNoiseLevel(callback: NoiseLevelCallback) {
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            requestAudioRecordRuntime()
            return
        }
        val rate = 8000 //8kHz
        val channel = AudioFormat.CHANNEL_IN_MONO
        val format = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(rate, channel, format)

        val audioBuffer = ShortArray(bufferSize)

        val recording = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            rate,
            channel,
            format,
            bufferSize
        )

        recording.startRecording()

        Thread {
            recording.read(audioBuffer, 0, bufferSize)

            var sum = 0.0
            for (i in audioBuffer) {
                sum += i * i
            }

            //best i could find that doesn't require calibration
            val amplitude = sqrt(sum / audioBuffer.size)
            val amplitudeMax = (2.0.pow(15) - 1) //max amplitude for 16-bit audio
            val noise = 20 * log10(amplitude / amplitudeMax)

            recording.stop()
            recording.release()

            callback.onNoiseLevelReceived(noise)
        }.start()
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //PERMISSIONS FUNCTIONS -----------------------------------------------
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun requestAudioRecordRuntime(){
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO
                ),
                AUDIO_REQUEST_CODE
            )
        } else {
            return
        }
    }

    private fun requestLocationRuntime(){
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                LOCATION_REQUEST_CODE
            )

        } else {
            enableLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            AUDIO_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    //permission granted
                } else {
                    if(shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)){
                        val snackbar = Snackbar.make(
                            binding.root,
                            "Devi concedere i permessi per poter utilizzare la funzione di registrazione del rumore!",
                            Snackbar.LENGTH_LONG
                        ).setAction("Ok") {
                            requestPermissions(
                                arrayOf(
                                    Manifest.permission.RECORD_AUDIO
                                ),
                                AUDIO_REQUEST_CODE
                            )
                        }
                        val snackbarView = snackbar.view
                        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                        textView.maxLines = 3
                        val params = snackbarView.layoutParams as ViewGroup.LayoutParams
                        params.height = 200
                        snackbarView.layoutParams = params
                        snackbar.show()
                    }else{
                        val snackbar = Snackbar.make(
                            binding.root,
                            "Hai negato i permessi permanentemente, vai alle impostazioni per concederli",
                            Snackbar.LENGTH_LONG
                        ).setAction("Vai") {
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        }
                        val snackbarView = snackbar.view
                        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                        textView.maxLines = 3
                        val params = snackbarView.layoutParams as ViewGroup.LayoutParams
                        params.height = 200
                        snackbarView.layoutParams = params
                        snackbar.show()
                    }
                }
                return
            }
            LOCATION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    //permission granted
                    enableLocation()
                } else {
                    if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
                        val snackbar = Snackbar.make(
                            binding.root,
                            "Devi concedere i permessi per poter utilizzare la mappa!",
                            Snackbar.LENGTH_INDEFINITE
                        ).setAction("Ok") {
                            requestPermissions(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ),
                                LOCATION_REQUEST_CODE
                            )
                        }
                        val snackbarView = snackbar.view
                        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                        textView.maxLines = 3
                        val params = snackbarView.layoutParams as ViewGroup.LayoutParams
                        params.height = 200
                        snackbarView.layoutParams = params
                        snackbar.show()
                    }else{
                        val snackbar = Snackbar.make(
                            binding.root,
                            "Hai negato i permessi permanentemente, vai alle impostazioni per concederli",
                            Snackbar.LENGTH_INDEFINITE
                        ).setAction("Vai") {
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        }
                        val snackbarView = snackbar.view
                        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                        textView.maxLines = 3
                        val params = snackbarView.layoutParams as ViewGroup.LayoutParams
                        params.height = 200
                        snackbarView.layoutParams = params
                        snackbar.show()
                    }
                }
                return
            }
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //MAP RELEVANT FUNCTIONS -----------------------------------------------
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun oneMGrid(){
        activeTiles = oneMeterTiles
        activeTilesSize = DbTable.ONE_M
        activeTiles.getGrid(GridType.METER).setColor(GridType.METER, gridColor)
        activeTiles.getGrid(GridType.METER).linesMaxZoom = null
        mMap.clear()
        mMap.addTileOverlay(TileOverlayOptions().tileProvider(activeTiles))
        mMap.moveCamera(CameraUpdateFactory.zoomTo(21f))
    }
    private fun tenMGrid(){
        activeTiles = tenMeterTiles
        activeTilesSize = DbTable.TEN_M
        activeTiles.getGrid(GridType.TEN_METER).setColor(GridType.TEN_METER, gridColor)
        activeTiles.getGrid(GridType.TEN_METER).linesMaxZoom = null
        mMap.clear()
        mMap.addTileOverlay(TileOverlayOptions().tileProvider(activeTiles))
        mMap.moveCamera(CameraUpdateFactory.zoomTo(19f))
    }
    private fun hundredMGrid(){
        activeTiles = hundredMeterTiles
        activeTilesSize = DbTable.HUNDRED_M
        activeTiles.getGrid(GridType.HUNDRED_METER).setColor(GridType.HUNDRED_METER, gridColor)
        activeTiles.getGrid(GridType.HUNDRED_METER).linesMaxZoom = null
        mMap.clear()
        mMap.addTileOverlay(TileOverlayOptions().tileProvider(activeTiles))
        mMap.moveCamera(CameraUpdateFactory.zoomTo(17f))
    }
    private fun oneKMGrid(){
        activeTiles = oneKilometerTiles
        activeTilesSize = DbTable.ONE_KM
        activeTiles.getGrid(GridType.KILOMETER).setColor(GridType.KILOMETER, gridColor)
        activeTiles.getGrid(GridType.KILOMETER).linesMaxZoom = null
        mMap.clear()
        mMap.addTileOverlay(TileOverlayOptions().tileProvider(activeTiles))
        mMap.moveCamera(CameraUpdateFactory.zoomTo(14f))
    }
    private fun tenKMGrid(){
        activeTiles = tenKilometerTiles
        activeTilesSize = DbTable.TEN_KM
        activeTiles.getGrid(GridType.TEN_KILOMETER).setColor(GridType.TEN_KILOMETER, gridColor)
        activeTiles.getGrid(GridType.TEN_KILOMETER).linesMaxZoom = null
        mMap.clear()
        mMap.addTileOverlay(TileOverlayOptions().tileProvider(activeTiles))
        mMap.moveCamera(CameraUpdateFactory.zoomTo(10f))
    }

    private fun metersToLat(metersLat: Double): Double {
        return metersLat / 111000.0

    }
    private fun metersToLon(metersLon: Double, currentLatitude: Double): Double {
        //  m / 111,320*cos(latitude)
        return metersLon / (111320.0 * Math.cos(Math.toRadians(currentLatitude)))
    }
    private fun mgrsToLatLngSquare(mgrsString: String, gridSize: DbTable): List<LatLng> {

        val meters = when(gridSize){
            DbTable.ONE_M -> 1.0
            DbTable.TEN_M -> 10.0
            DbTable.HUNDRED_M -> 100.0
            DbTable.ONE_KM -> 1000.0
            DbTable.TEN_KM -> 10000.0
        }

        val swMGRS = MGRS.parse(mgrsString)
        var swCorner = LatLng(swMGRS.toPoint().latitude, swMGRS.toPoint().longitude)
        swCorner = LatLng(swCorner.latitude, swCorner.longitude)

        val seCorner = LatLng(swCorner.latitude, swCorner.longitude + metersToLon(meters,swCorner.latitude))
        val neCorner = LatLng(swCorner.latitude + metersToLat(meters), swCorner.longitude + metersToLon(meters,swCorner.latitude))
        val nwCorner = LatLng(swCorner.latitude + metersToLat(meters), swCorner.longitude)

        return listOf(swCorner, seCorner, neCorner, nwCorner)
    }

    private fun colorSquare(location: String, color: Int){
        val polygonOptions = PolygonOptions()
        var tileSize: DbTable = DbTable.ONE_M
        when(location.length){
            7->{tileSize= DbTable.TEN_KM}
            9->{tileSize= DbTable.ONE_KM}
            11->{tileSize= DbTable.HUNDRED_M}
            13->{tileSize= DbTable.TEN_M}
            15->{tileSize= DbTable.ONE_M}
        }
        polygonOptions.addAll(mgrsToLatLngSquare(location, tileSize))
        polygonOptions.strokeColor(color)
        polygonOptions.strokeWidth(1f)
        polygonOptions.fillColor(color)
        runOnUiThread {
            mMap.addPolygon(polygonOptions)
        }

    }

    private fun colorMap(tileSize: DbTable){
        Thread{
            val valType = findViewById<Button>(R.id.valueTypeButton).text.toString()
            when(tileSize){
                DbTable.ONE_M -> {
                    val oneMList = db.MGRSDao().getAllOneM()
                    when(valType){
                        "MOBILE" ->{
                            for (i in oneMList){
                                if(i.numLTE >= x1m){
                                    when(i.strengthLTE){
                                        in -110f..-100f ->{colorSquare(i.mgrsLocation, CVBAD)}
                                        in -100f..-90f ->{colorSquare(i.mgrsLocation, CBAD)}
                                        in -90f..-80f ->{colorSquare(i.mgrsLocation, COK)}
                                        in -80f..-60f ->{colorSquare(i.mgrsLocation, CGOOD) }
                                        else ->{colorSquare(i.mgrsLocation, CVGOOD)}
                                    }
                                }
                            }
                        }
                        "WIFI" ->{
                            for (i in oneMList){
                                if(i.numWIFI >= x1m){
                                    when(i.strengthWIFI){
                                        in -110f..-100f ->{colorSquare(i.mgrsLocation, CVBAD)}
                                        in -100f..-90f ->{colorSquare(i.mgrsLocation, CBAD)}
                                        in -90f..-80f ->{colorSquare(i.mgrsLocation, COK)}
                                        in -80f..-60f ->{colorSquare(i.mgrsLocation, CGOOD) }
                                        else ->{colorSquare(i.mgrsLocation, CVGOOD)}
                                    }
                                }
                            }
                        }
                        "AUDIO" ->{
                            for (i in oneMList){
                                if(i.numNoise >= x1m){
                                    when(i.noise){
                                        in -3f..0f ->{colorSquare(i.mgrsLocation, CVBAD)}
                                        in -10f..-3f ->{colorSquare(i.mgrsLocation, CBAD)}
                                        in -30f..-10f ->{colorSquare(i.mgrsLocation, COK)}
                                        in -60f..-30f ->{colorSquare(i.mgrsLocation, CGOOD) }
                                        else ->{colorSquare(i.mgrsLocation, CVGOOD)}
                                    }
                                }
                            }
                        }

                    }

                }
                DbTable.TEN_M -> {
                    val tenMList = db.MGRSDao().getAllTenM()
                    when(valType){
                        "MOBILE" ->{
                            for (i in tenMList){
                                if(i.numLTE >= x10m){
                                    when(i.strengthLTE){
                                        in -110f..-100f ->{colorSquare(i.mgrsLocation, CVBAD)}
                                        in -100f..-90f ->{colorSquare(i.mgrsLocation, CBAD)}
                                        in -90f..-80f ->{colorSquare(i.mgrsLocation, COK)}
                                        in -80f..-60f ->{colorSquare(i.mgrsLocation, CGOOD) }
                                        else ->{colorSquare(i.mgrsLocation, CVGOOD)}
                                    }
                                }
                            }
                        }
                        "WIFI" ->{
                            for (i in tenMList){
                                if(i.numWIFI >= x10m){
                                    when(i.strengthWIFI){
                                        in -110f..-100f ->{colorSquare(i.mgrsLocation, CVBAD)}
                                        in -100f..-90f ->{colorSquare(i.mgrsLocation, CBAD)}
                                        in -90f..-80f ->{colorSquare(i.mgrsLocation, COK)}
                                        in -80f..-60f ->{colorSquare(i.mgrsLocation, CGOOD) }
                                        else ->{colorSquare(i.mgrsLocation, CVGOOD)}
                                    }
                                }
                            }
                        }
                        "AUDIO" ->{
                            for (i in tenMList){
                                if(i.numNoise >= x10m){
                                    when(i.noise){
                                        in -3f..0f ->{colorSquare(i.mgrsLocation, CVBAD)}
                                        in -10f..-3f ->{colorSquare(i.mgrsLocation, CBAD)}
                                        in -30f..-10f ->{colorSquare(i.mgrsLocation, COK)}
                                        in -60f..-30f ->{colorSquare(i.mgrsLocation, CGOOD) }
                                        else ->{colorSquare(i.mgrsLocation, CVGOOD)}
                                    }
                                }
                            }
                        }
                    }
                }
                DbTable.HUNDRED_M -> {
                    val hundredMList = db.MGRSDao().getAllHundredM()
                    when(valType){
                        "MOBILE" ->{
                            for (i in hundredMList){
                                if(i.numLTE >= x100m){
                                    when(i.strengthLTE){
                                        in -110f..-100f ->{colorSquare(i.mgrsLocation, CVBAD)}
                                        in -100f..-90f ->{colorSquare(i.mgrsLocation, CBAD)}
                                        in -90f..-80f ->{colorSquare(i.mgrsLocation, COK)}
                                        in -80f..-60f ->{colorSquare(i.mgrsLocation, CGOOD) }
                                        else ->{colorSquare(i.mgrsLocation, CVGOOD)}
                                    }
                                }
                            }
                        }
                        "WIFI" ->{
                            for (i in hundredMList){
                                if(i.numWIFI >= x100m){
                                    when(i.strengthWIFI){
                                        in -110f..-100f ->{colorSquare(i.mgrsLocation, CVBAD)}
                                        in -100f..-90f ->{colorSquare(i.mgrsLocation, CBAD)}
                                        in -90f..-80f ->{colorSquare(i.mgrsLocation, COK)}
                                        in -80f..-60f ->{colorSquare(i.mgrsLocation, CGOOD) }
                                        else ->{colorSquare(i.mgrsLocation, CVGOOD)}
                                    }
                                }
                            }
                        }
                        "AUDIO" ->{
                            for (i in hundredMList){
                                if(i.numNoise >= x100m){
                                    when(i.noise){
                                        in -3f..0f ->{colorSquare(i.mgrsLocation, CVBAD)}
                                        in -10f..-3f ->{colorSquare(i.mgrsLocation, CBAD)}
                                        in -30f..-10f ->{colorSquare(i.mgrsLocation, COK)}
                                        in -60f..-30f ->{colorSquare(i.mgrsLocation, CGOOD) }
                                        else ->{colorSquare(i.mgrsLocation, CVGOOD)}
                                    }
                                }
                            }
                        }
                    }
                }
                DbTable.ONE_KM -> {
                    val oneKMList = db.MGRSDao().getAllOneKM()
                    when(valType){
                        "MOBILE" ->{
                            for (i in oneKMList){
                                if(i.numLTE >= x1km){
                                    when(i.strengthLTE){
                                        in -110f..-100f ->{colorSquare(i.mgrsLocation, CVBAD)}
                                        in -100f..-90f ->{colorSquare(i.mgrsLocation, CBAD)}
                                        in -90f..-80f ->{colorSquare(i.mgrsLocation, COK)}
                                        in -80f..-60f ->{colorSquare(i.mgrsLocation, CGOOD) }
                                        else ->{colorSquare(i.mgrsLocation, CVGOOD)}
                                    }
                                }
                            }
                        }
                        "WIFI" ->{
                            for (i in oneKMList){
                                if(i.numWIFI >= x1km){
                                    when(i.strengthWIFI){
                                        in -110f..-100f ->{colorSquare(i.mgrsLocation, CVBAD)}
                                        in -100f..-90f ->{colorSquare(i.mgrsLocation, CBAD)}
                                        in -90f..-80f ->{colorSquare(i.mgrsLocation, COK)}
                                        in -80f..-60f ->{colorSquare(i.mgrsLocation, CGOOD) }
                                        else ->{colorSquare(i.mgrsLocation, CVGOOD)}
                                    }
                                }
                            }
                        }
                        "AUDIO" ->{
                            for (i in oneKMList){
                                if(i.numNoise >= x1km){
                                    when(i.noise){
                                        in -3f..0f ->{colorSquare(i.mgrsLocation, CVBAD)}
                                        in -10f..-3f ->{colorSquare(i.mgrsLocation, CBAD)}
                                        in -30f..-10f ->{colorSquare(i.mgrsLocation, COK)}
                                        in -60f..-30f ->{colorSquare(i.mgrsLocation, CGOOD) }
                                        else ->{colorSquare(i.mgrsLocation, CVGOOD)}
                                    }
                                }
                            }
                        }
                    }
                }
                DbTable.TEN_KM -> {
                    val tenKMList = db.MGRSDao().getAllTenKM()
                    when(valType){
                        "MOBILE" ->{
                            for (i in tenKMList){
                                if(i.numLTE >= x10km){
                                    when(i.strengthLTE){
                                        in -110f..-100f ->{colorSquare(i.mgrsLocation, CVBAD)}
                                        in -100f..-90f ->{colorSquare(i.mgrsLocation, CBAD)}
                                        in -90f..-80f ->{colorSquare(i.mgrsLocation, COK)}
                                        in -80f..-60f ->{colorSquare(i.mgrsLocation, CGOOD) }
                                        else ->{colorSquare(i.mgrsLocation, CVGOOD)}
                                    }
                                }
                            }
                        }
                        "WIFI" ->{
                            for (i in tenKMList){
                                if(i.numWIFI >= x10km){
                                    when(i.strengthWIFI){
                                        in -110f..-100f ->{colorSquare(i.mgrsLocation, CVBAD)}
                                        in -100f..-90f ->{colorSquare(i.mgrsLocation, CBAD)}
                                        in -90f..-80f ->{colorSquare(i.mgrsLocation, COK)}
                                        in -80f..-60f ->{colorSquare(i.mgrsLocation, CGOOD) }
                                        else ->{colorSquare(i.mgrsLocation, CVGOOD)}
                                    }
                                }
                            }
                        }
                        "AUDIO" ->{
                            for (i in tenKMList){
                                if(i.numNoise >= x10km){
                                    when(i.noise){
                                        in -3f..0f ->{colorSquare(i.mgrsLocation, CVBAD)}
                                        in -10f..-3f ->{colorSquare(i.mgrsLocation, CBAD)}
                                        in -30f..-10f ->{colorSquare(i.mgrsLocation, COK)}
                                        in -60f..-30f ->{colorSquare(i.mgrsLocation, CGOOD) }
                                        else ->{colorSquare(i.mgrsLocation, CVGOOD)}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.start()

    }

    private fun enableLocation(){
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(::mMap.isInitialized)
                mMap.isMyLocationEnabled = true

            return
        }

    }

}