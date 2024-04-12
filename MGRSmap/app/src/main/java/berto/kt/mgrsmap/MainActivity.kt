package berto.kt.mgrsmap


import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.HandlerThread
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Switch
import android.widget.TableLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import mil.nga.mgrs.MGRS
import mil.nga.mgrs.grid.GridType

@Entity
data class OneM(
    @PrimaryKey val mgrsLocation: String,

    val strengthLTE: Float,
    val strengthWIFI: Float,
    val noise: Float,
    val numLTE: Int,
    val numWIFI: Int,
    val numNoise: Int,

)
@Entity
data class TenM(
    @PrimaryKey val mgrsLocation: String,

    val strengthLTE: Float,
    val strengthWIFI: Float,
    val noise: Float,
    val numLTE: Int,
    val numWIFI: Int,
    val numNoise: Int,

)
@Entity
data class HundredM(
    @PrimaryKey val mgrsLocation: String,

    val strengthLTE: Float,
    val strengthWIFI: Float,
    val noise: Float,
    val numLTE: Int,
    val numWIFI: Int,
    val numNoise: Int,

)
@Entity
data class OneKM(
    @PrimaryKey val mgrsLocation: String,

    val strengthLTE: Float,
    val strengthWIFI: Float,
    val noise: Float,
    val numLTE: Int,
    val numWIFI: Int,
    val numNoise: Int,

)
@Entity
data class TenKM(
    @PrimaryKey val mgrsLocation: String,

    val strengthLTE: Float,
    val strengthWIFI: Float,
    val noise: Float,
    val numLTE: Int,
    val numWIFI: Int,
    val numNoise: Int,

)


@Dao
interface MGRSDao {
    //-----------------------------------1 M-----------------------------------
    @Insert
    fun insertOneM(entity: OneM)

    @Query("SELECT * FROM OneM")
    fun getAllOneM(): List<OneM>

    @Update
    fun updateOneM(entity: OneM)

    @Query("SELECT * FROM OneM WHERE mgrsLocation = :mgrsLocation")
    fun getOneM(mgrsLocation: String): OneM?

    //-----------------------------------10 M-----------------------------------
    @Insert
    fun insertTenM(entity: TenM)

    @Query("SELECT * FROM TenM")
    fun getAllTenM(): List<TenM>

    @Update
    fun updateTenM(entity: TenM)

    @Query("SELECT * FROM TenM WHERE mgrsLocation = :mgrsLocation")
    fun getTenM(mgrsLocation: String): TenM?

    //-----------------------------------100 M-----------------------------------
    @Insert
    fun insertHundredM(entity: HundredM)

    @Query("SELECT * FROM HundredM")
    fun getAllHundredM(): List<HundredM>

    @Update
    fun updateHundredM(entity: HundredM)

    @Query("SELECT * FROM HundredM WHERE mgrsLocation = :mgrsLocation")
    fun getHundredM(mgrsLocation: String): HundredM?

    //-----------------------------------1 KM-----------------------------------
    @Insert
    fun insertOneKM(entity: OneKM)

    @Query("SELECT * FROM OneKM")
    fun getAllOneKM(): List<OneKM>

    @Update
    fun updateOneKM(entity: OneKM)

    @Query("SELECT * FROM OneKM WHERE mgrsLocation = :mgrsLocation")
    fun getOneKM(mgrsLocation: String): OneKM?

    //-----------------------------------10 KM-----------------------------------
    @Insert
    fun insertTenKM(entity: TenKM)

    @Query("SELECT * FROM TenKM")
    fun getAllTenKM(): List<TenKM>

    @Update
    fun updateTenKM(entity: TenKM)

    @Query("SELECT * FROM TenKM WHERE mgrsLocation = :mgrsLocation")
    fun getTenKM(mgrsLocation: String): TenKM?

    //-----------------------------------THE NUKES-----------------------------------
    @Query("DELETE FROM OneM")
    fun deleteAllOneM()

    @Query("DELETE FROM TenM")
    fun deleteAllTenM()

    @Query("DELETE FROM HundredM")
    fun deleteAllHundredM()

    @Query("DELETE FROM OneKM")
    fun deleteAllOneKM()

    @Query("DELETE FROM TenKM")
    fun deleteAllTenKM()

    //-------------------------------------------------------------------------------

    @Query("DELETE FROM OneM WHERE mgrsLocation = :mgrsLocation")
    fun deleteOneM(mgrsLocation: String)

    @Query("DELETE FROM TenM WHERE mgrsLocation = :mgrsLocation")
    fun deleteTenM(mgrsLocation: String)

    @Query("DELETE FROM HundredM WHERE mgrsLocation = :mgrsLocation")
    fun deleteHundredM(mgrsLocation: String)

    @Query("DELETE FROM OneKM WHERE mgrsLocation = :mgrsLocation")
    fun deleteOneKM(mgrsLocation: String)

    @Query("DELETE FROM TenKM WHERE mgrsLocation = :mgrsLocation")
    fun deleteTenKM(mgrsLocation: String)

}

@Database(entities = [OneM::class, TenM::class , HundredM::class, OneKM::class, TenKM::class ], version = 1)
abstract class MGRSDatabase : RoomDatabase() {

    abstract fun MGRSDao(): MGRSDao

    companion object {
        @Volatile
        private var INSTANCE: MGRSDatabase? = null

        fun getDatabase(context: Context): MGRSDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MGRSDatabase::class.java,
                    "MGRSDatabase"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }

}


class MainActivity : AppCompatActivity() {

    private val BACKGROUND_LOCATION_REQUEST_CODE = 42
    private val POST_NOTIFICATIONS_REQUEST_CODE = 30
    private val LOCATION_REQUEST_CODE = 1

    private lateinit var db: MGRSDatabase

    private fun cancelJobScheduler(){
        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.cancel(0)
    }

    override fun onResume() {
        super.onResume()
        cancelJobScheduler()
        val notiSwitch = findViewById<Switch>(R.id.notificationSwitch)
        notiSwitch.isChecked = checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val sharedPref = getSharedPreferences("mgrsSettings", Context.MODE_PRIVATE)

        Thread{
            setNotificationResetDaily(this)
            cancelJobScheduler()
        }.start()

        val goToMapButton = findViewById<FloatingActionButton>(R.id.goToMapButton)
        val deleteDataFab = findViewById<FloatingActionButton>(R.id.deleteDataFab)
        val xnTable = findViewById<TableLayout>(R.id.xnTable)
        val xText = findViewById<TextView>(R.id.xTextView)
        val nText = findViewById<TextView>(R.id.nTextView)
        val notiLayout = findViewById<LinearLayout>(R.id.notiLayout)
        val notiSwitch = findViewById<Switch>(R.id.notificationSwitch)
        notiSwitch.isChecked = sharedPref.getBoolean("notificationSwitch", false)
        notiSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked){
                if(checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED){
                    requestLocation()
                }else{
                    requestBackgroundLocation()
                }
            }
            else{
                if(checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    AlertDialog.Builder(this)
                        .setTitle("Rimuovi accesso alla posizione")
                        .setMessage("É possibile rimuovere l'accesso dell'app alla posizione quando é chiusa\n" +
                                "Sarai reindirizzato alle impostazioni, rimuovi il permesso se lo ritieni opportuno")
                        .setPositiveButton("Vai") { _, _ ->
                            val intent = Intent().apply {
                                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                val uri = Uri.fromParts("package", packageName, null)
                                data = uri
                            }
                            startActivity(intent)
                            sharedPref.edit().putBoolean("shouldSendNoti", false).apply()
                        }
                        .setNegativeButton("Non ora") { _, _ ->
                            sharedPref.edit().putBoolean("shouldSendNoti", false).apply()
                        }
                        .show()
                }
            }
        }

        val colorLayout = findViewById<LinearLayout>(R.id.colorLayout)
        when(sharedPref.getString("gridColor", "black")){
            "black" -> findViewById<RadioGroup>(R.id.radioGroup).check(R.id.radioBlack)
            "gray" -> findViewById<RadioGroup>(R.id.radioGroup).check(R.id.radioGray)
            "purple" -> findViewById<RadioGroup>(R.id.radioGroup).check(R.id.radioPurple)
        }

        val saveBtn = findViewById<Button>(R.id.saveButton)
        val resetBtn = findViewById<Button>(R.id.resetDefaultButton)


        val x1m = findViewById<EditText>(R.id.x1m)
        x1m.setText(sharedPref.getInt("x1m", 1).toString())
        val x10m = findViewById<EditText>(R.id.x10m)
        x10m.setText(sharedPref.getInt("x10m", 3).toString())
        val x100m = findViewById<EditText>(R.id.x100m)
        x100m.setText(sharedPref.getInt("x100m", 5).toString())
        val x1km = findViewById<EditText>(R.id.x1km)
        x1km.setText(sharedPref.getInt("x1km", 10).toString())
        val x10km = findViewById<EditText>(R.id.x10km)
        x10km.setText(sharedPref.getInt("x10km", 20).toString())

        val n1m = findViewById<EditText>(R.id.n1m)
        n1m.setText(sharedPref.getInt("n1m", 60).toString())
        val n10m = findViewById<EditText>(R.id.n10m)
        n10m.setText(sharedPref.getInt("n10m", 300).toString())
        val n100m = findViewById<EditText>(R.id.n100m)
        n100m.setText(sharedPref.getInt("n100m", 600).toString())
        val n1km = findViewById<EditText>(R.id.n1km)
        n1km.setText(sharedPref.getInt("n1km", 900).toString())
        val n10km = findViewById<EditText>(R.id.n10km)
        n10km.setText(sharedPref.getInt("n10km", 1200).toString())

        val logoImage = findViewById<ImageView>(R.id.logoImageView)
        val heightInPixels = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            150f,
            resources.displayMetrics
        ).toInt()


        val settingsFab = findViewById<FloatingActionButton>(R.id.settingsFab)
        settingsFab.isActivated = true
        settingsFab.setOnClickListener {
            if (!settingsFab.isActivated){

                val params = logoImage.layoutParams
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                logoImage.layoutParams = params

                settingsFab.setImageDrawable(getDrawable(R.drawable.ic_settings))
                settingsFab.isActivated = true
                xnTable.visibility = TableLayout.INVISIBLE
                xText.visibility = TextView.INVISIBLE
                nText.visibility = TextView.INVISIBLE
                notiLayout.visibility = LinearLayout.INVISIBLE
                colorLayout.visibility = LinearLayout.INVISIBLE
                saveBtn.visibility = Button.INVISIBLE
                resetBtn.visibility = Button.INVISIBLE



                goToMapButton.show()
                deleteDataFab.show()
            }
            else{

                val params = logoImage.layoutParams
                params.height = heightInPixels
                logoImage.layoutParams = params

                goToMapButton.hide()
                deleteDataFab.hide()

                settingsFab.setImageDrawable(getDrawable(R.drawable.ic_close_x))
                settingsFab.isActivated = false
                xnTable.visibility = TableLayout.VISIBLE
                xText.visibility = TextView.VISIBLE
                nText.visibility = TextView.VISIBLE
                notiLayout.visibility = LinearLayout.VISIBLE
                colorLayout.visibility = LinearLayout.VISIBLE
                saveBtn.visibility = Button.VISIBLE
                resetBtn.visibility = Button.VISIBLE



            }

        }

        saveBtn.setOnClickListener {
            Thread{
                val radioGroup: RadioGroup = findViewById(R.id.radioGroup)
                val radioColor = radioGroup.checkedRadioButtonId
                var gridColor = "black"
                when(radioColor){
                    R.id.radioBlack -> gridColor = "black"
                    R.id.radioGray -> gridColor = "gray"
                    R.id.radioPurple -> gridColor = "purple"
                }
                with(sharedPref.edit()) {
                    if (x1m.text.toString().toInt() < 1) putInt("x1m", 1)
                    else putInt("x1m", x1m.text.toString().toInt())
                    if (x10m.text.toString().toInt() < 1) putInt("x10m", 3)
                    else putInt("x10m", x10m.text.toString().toInt())
                    if (x100m.text.toString().toInt() < 1) putInt("x100m", 5)
                    else putInt("x100m", x100m.text.toString().toInt())
                    if (x1km.text.toString().toInt() < 1) putInt("x1km", 10)
                    else putInt("x1km", x1km.text.toString().toInt())
                    if (x10km.text.toString().toInt() < 1) putInt("x10km", 20)
                    else putInt("x10km", x10km.text.toString().toInt())

                    if(n1m.text.toString().toInt() < 1) putInt("n1m", 60)
                    else putInt("n1m", n1m.text.toString().toInt())
                    if(n10m.text.toString().toInt() < 1) putInt("n10m", 300)
                    else putInt("n10m", n10m.text.toString().toInt())
                    if(n100m.text.toString().toInt() < 1) putInt("n100m", 600)
                    else putInt("n100m", n100m.text.toString().toInt())
                    if(n1km.text.toString().toInt() < 1) putInt("n1km", 900)
                    else putInt("n1km", n1km.text.toString().toInt())
                    if(n10km.text.toString().toInt() < 1) putInt("n10km", 1200)
                    else putInt("n10km", n10km.text.toString().toInt())

                    putString("gridColor", gridColor)
                    putBoolean("notificationSwitch", notiSwitch.isChecked)
                    apply()
                }
            }.start()
            Snackbar.make(findViewById(R.id.snackbarView), "Salvataggio completato", Snackbar.LENGTH_SHORT).show()
        }



        goToMapButton.setOnClickListener {
            val intent = Intent(this, GridAndMapActivity::class.java)
            intent.putExtra("x1m", sharedPref.getInt("x1m", 1))
            intent.putExtra("x10m", sharedPref.getInt("x10m", 3))
            intent.putExtra("x100m", sharedPref.getInt("x100m", 5))
            intent.putExtra("x1km", sharedPref.getInt("x1km", 10))
            intent.putExtra("x10km", sharedPref.getInt("x10km", 20))
            intent.putExtra("n1m", sharedPref.getInt("n1m", 60))
            intent.putExtra("n10m", sharedPref.getInt("n10m", 300))
            intent.putExtra("n100m", sharedPref.getInt("n100m", 600))
            intent.putExtra("n1km", sharedPref.getInt("n1km", 900))
            intent.putExtra("n10km", sharedPref.getInt("n10km", 1200))
            intent.putExtra("gridColor", sharedPref.getString("gridColor", "black"))
            startActivity(intent)
        }


        resetBtn.setOnClickListener{
            Thread{
                with(sharedPref.edit()) {
                    putInt("x1m", 1)
                    putInt("x10m", 3)
                    putInt("x100m", 5)
                    putInt("x1km", 10)
                    putInt("x10km", 20)
                    putInt("n1m", 60)
                    putInt("n10m", 300)
                    putInt("n100m", 600)
                    putInt("n1km", 900)
                    putInt("n10km", 1200)
                    putString("gridColor", "black")
                    putBoolean("notificationSwitch", false)
                    apply()
                }
            }.start()

            findViewById<RadioGroup>(R.id.radioGroup).check(R.id.radioBlack)
            notiSwitch.isChecked = false
            x1m.setText(sharedPref.getInt("x1m", 1).toString())
            x10m.setText(sharedPref.getInt("x10m", 3).toString())
            x100m.setText(sharedPref.getInt("x100m", 5).toString())
            x1km.setText(sharedPref.getInt("x1km", 10).toString())
            x10km.setText(sharedPref.getInt("x10km", 20).toString())

            n1m.setText(sharedPref.getInt("n1m", 60).toString())
            n10m.setText(sharedPref.getInt("n10m", 300).toString())
            n100m.setText(sharedPref.getInt("n100m", 600).toString())
            n1km.setText(sharedPref.getInt("n1km", 900).toString())
            n10km.setText(sharedPref.getInt("n10km", 1200).toString())
            Snackbar.make(findViewById(R.id.snackbarView), "Impostazioni ripristinate", Snackbar.LENGTH_SHORT).show()
        }

        Thread{db = MGRSDatabase.getDatabase(this)}.start()


        val deleteDataLayout = findViewById<LinearLayout>(R.id.deleteDataLayout)
        val deleteCellLayout = findViewById<LinearLayout>(R.id.deleteCellLayout)
        val deleteGridLayout = findViewById<LinearLayout>(R.id.deleteTableLayout)


        val gridSpinner = findViewById<Spinner>(R.id.gridSpinner)
        val tableNames = arrayOf("", "1m", "10m", "100m", "1Km", "10Km")
        ArrayAdapter(this, android.R.layout.simple_spinner_item, tableNames).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            gridSpinner.adapter = adapter
        }
        var selectedSpinnerItem = ""
        gridSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                when(parent.getItemAtPosition(position).toString()){
                    "1m" ->{selectedSpinnerItem = "1m"}
                    "10m" ->{selectedSpinnerItem = "10m"}
                    "100m" ->{selectedSpinnerItem = "100m"}
                    "1Km" ->{selectedSpinnerItem = "1Km"}
                    "10Km" ->{selectedSpinnerItem = "10Km"}
                }

            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedSpinnerItem = "imtiredboss"
            }
        }

        val nukeDbButton = findViewById<Button>(R.id.nukeDbButton)
        val deleteCellButton = findViewById<Button>(R.id.deleteCellButton)
        val deleteGridButton = findViewById<Button>(R.id.deleteTableButton)

        deleteGridButton.setOnClickListener {
            if (selectedSpinnerItem == "1m" || selectedSpinnerItem == "10m" || selectedSpinnerItem == "100m" || selectedSpinnerItem == "1Km" || selectedSpinnerItem == "10Km"){
                AlertDialog.Builder(this)
                    .setTitle("Cancella dati")
                    .setMessage("Sicuro di voler eliminare i dati della griglia $selectedSpinnerItem?"+
                            "Questa azione é irreversibile")
                    .setPositiveButton("Si") { _, _ ->
                        deleteGridData(selectedSpinnerItem)
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }

        }


        deleteDataFab.setOnClickListener {
            if (deleteDataFab.isActivated){
                val params = logoImage.layoutParams
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT

                deleteDataFab.setImageDrawable(getDrawable(R.drawable.baseline_delete_sweep_24))
                goToMapButton.show()
                settingsFab.show()

                logoImage.layoutParams = params
                deleteDataFab.isActivated = false
                deleteCellLayout.visibility = LinearLayout.INVISIBLE
                deleteGridLayout.visibility = LinearLayout.INVISIBLE
                deleteDataLayout.visibility = LinearLayout.INVISIBLE


            }else{
                val params = logoImage.layoutParams
                params.height = heightInPixels
                logoImage.layoutParams = params

                deleteDataFab.setImageDrawable(getDrawable(R.drawable.ic_close_x))
                goToMapButton.hide()
                settingsFab.hide()

                deleteDataFab.isActivated = true
                deleteCellLayout.visibility = LinearLayout.VISIBLE
                deleteGridLayout.visibility = LinearLayout.VISIBLE
                deleteDataLayout.visibility = LinearLayout.VISIBLE
                nukeDbButton.text = "CANCELLA DATI"
            }
        }

        deleteCellButton.setOnClickListener {
            closeKeyboard()
            deleteCellData(findViewById<EditText>(R.id.cellToDeleteTextView).text.toString())
        }

        nukeDbButton.setOnClickListener{
            if(nukeDbButton.text == "CONFERMA"){
                Thread{
                    ProjectManhattan()
                }.start()
                Snackbar.make(findViewById(R.id.snackbarView), "Database resettato", Snackbar.LENGTH_SHORT).show()
                nukeDbButton.text = "CANCELLA DATI"
            }
            else{
                nukeDbButton.text = "CONFERMA"
            }

        }

        settingsFab.setOnLongClickListener {
            Snackbar.make(findViewById(R.id.snackbarView), "Clicca per aprire le impostazioni", Snackbar.LENGTH_SHORT).show()
            true
        }
        deleteDataFab.setOnLongClickListener {
            Snackbar.make(findViewById(R.id.snackbarView), "Clicca per aprire il menu di cancellazione dati", Snackbar.LENGTH_SHORT).show()
            true
        }
        goToMapButton.setOnLongClickListener {
            Snackbar.make(findViewById(R.id.snackbarView), "Clicca per aprire la mappa", Snackbar.LENGTH_SHORT).show()
            true
        }
    }
    private fun deleteCellData(cell: String){
        if(cell=="") return
        else{
            AlertDialog.Builder(this)
                .setTitle("Cancella dati")
                .setMessage("Sicuro di voler eliminare i dati della cella $cell?"+
                        "Questa azione é irreversibile")
                .setPositiveButton("Si") { _, _ ->
                    Thread{
                        when(cell.length){
                            7 ->{ //10KM
                                db.MGRSDao().getTenKM(cell).let {
                                    if(it != null){
                                        db.MGRSDao().deleteTenKM(cell)
                                        Snackbar.make(findViewById(R.id.snackbarView), "Cella cancellata (10KM)", Snackbar.LENGTH_SHORT).show()
                                    }
                                    else{
                                        Snackbar.make(findViewById(R.id.snackbarView), "Cella non valida o senza misurazioni (10KM)", Snackbar.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            9 ->{ //1KM
                                db.MGRSDao().getOneKM(cell).let {
                                    if(it != null){
                                        db.MGRSDao().deleteOneKM(cell)
                                        Snackbar.make(findViewById(R.id.snackbarView), "Cella cancellata (1KM)", Snackbar.LENGTH_SHORT).show()
                                    }
                                    else{
                                        Snackbar.make(findViewById(R.id.snackbarView), "Cella non valida o senza misurazioni (1KM)", Snackbar.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            11 ->{ //100M
                                db.MGRSDao().getHundredM(cell).let {
                                    if(it != null){
                                        db.MGRSDao().deleteHundredM(cell)
                                        Snackbar.make(findViewById(R.id.snackbarView), "Cella cancellata (100M)", Snackbar.LENGTH_SHORT).show()
                                    }
                                    else{
                                        Snackbar.make(findViewById(R.id.snackbarView), "Cella non valida o senza misurazioni (100M)", Snackbar.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            13 ->{ //10M
                                db.MGRSDao().getTenM(cell).let {
                                    if(it != null){
                                        db.MGRSDao().deleteTenM(cell)
                                        Snackbar.make(findViewById(R.id.snackbarView), "Cella cancellata (10M)", Snackbar.LENGTH_SHORT).show()
                                    }
                                    else{
                                        Snackbar.make(findViewById(R.id.snackbarView), "Cella non valida o senza misurazioni (10M)", Snackbar.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            15 ->{ //1M
                                db.MGRSDao().getOneM(cell).let {
                                    if(it != null){
                                        db.MGRSDao().deleteOneM(cell)
                                        Snackbar.make(findViewById(R.id.snackbarView), "Cella cancellata (1M)", Snackbar.LENGTH_SHORT).show()
                                    }
                                    else{
                                        Snackbar.make(findViewById(R.id.snackbarView), "Cella non valida o senza misurazioni (1M)", Snackbar.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }

                    }.start()
                }
                .setNegativeButton("No") { _, _ ->

                }
                .create()
                .show()
        }



    }

    private fun deleteGridData(grid:String){
        when(grid){
            "imtiredboss" -> return
            "1m" ->{
                Thread{
                    db.MGRSDao().deleteAllOneM()
                }.start()
                Snackbar.make(findViewById(R.id.snackbarView), "Dati cancellati (OneM)", Snackbar.LENGTH_SHORT).show()
            }
            "10m" ->{
                Thread{
                    db.MGRSDao().deleteAllTenM()
                }.start()
                Snackbar.make(findViewById(R.id.snackbarView), "Dati cancellati (TenM)", Snackbar.LENGTH_SHORT).show()
            }
            "100m" ->{
                Thread{
                    db.MGRSDao().deleteAllHundredM()
                }.start()
                Snackbar.make(findViewById(R.id.snackbarView), "Dati cancellati (HundredM)", Snackbar.LENGTH_SHORT).show()
            }
            "1KM" ->{
                Thread{
                    db.MGRSDao().deleteAllOneKM()
                }.start()
                Snackbar.make(findViewById(R.id.snackbarView), "Dati cancellati (OneKm)", Snackbar.LENGTH_SHORT).show()
            }
            "10KM" ->{
                Thread{
                    db.MGRSDao().deleteAllTenKM()
                }.start()
                Snackbar.make(findViewById(R.id.snackbarView), "Dati cancellati (TenKm)", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    private fun ProjectManhattan(){
        AlertDialog.Builder(this)
            .setTitle("Cancella dati")
            .setMessage("Sicuro di voler eliminare TUTTI i dati?\n" +
                    "Questa azione é irreversibile")
            .setPositiveButton("Si") { _, _ ->
                Thread{
                    db.MGRSDao().deleteAllOneM()
                    db.MGRSDao().deleteAllTenM()
                    db.MGRSDao().deleteAllHundredM()
                    db.MGRSDao().deleteAllOneKM()
                    db.MGRSDao().deleteAllTenKM()
                }.start()
            }
            .setNegativeButton("No") { _, _ ->

            }
            .create()
            .show()
    }
    private fun closeKeyboard(){
        val view = this.currentFocus
        if(view != null){
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)

        }
    }
    private fun requestLocation(){
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                LOCATION_REQUEST_CODE
            )

        }else{
            //permission granted
            AlertDialog.Builder(this)
                .setTitle("Accesso alla posizione")
                .setMessage("L'applicazione deve poter accedere alla tua posizione quando non é aperta per inviarti notifiche correttamente.\n" +
                        "Sarai reindirizzato alle impostazioni, concedi il permesso e torna indietro per continuare")
                .setPositiveButton("Vai") { _, _ ->
                    requestBackgroundLocation()
                }
                .setNegativeButton("Annulla") { _, _ ->
                    val notiSwitch = findViewById<Switch>(R.id.notificationSwitch)
                    notiSwitch.isChecked = false
                }
                .show()
        }
    }
    private fun requestBackgroundLocation(){
        if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ),
                BACKGROUND_LOCATION_REQUEST_CODE
            )

        }else{
            //permission granted
            requestPostNotifications()
        }
    }
    private fun requestPostNotifications(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS
                    ),
                    POST_NOTIFICATIONS_REQUEST_CODE
                )
            }else{
                //permission granted
                val sharedPref = getSharedPreferences("mgrsSettings", Context.MODE_PRIVATE)
                sharedPref.edit().putBoolean("shouldSendNoti", true).apply()
                return
            }

        }else{
            val sharedPref = getSharedPreferences("mgrsSettings", Context.MODE_PRIVATE)
            sharedPref.edit().putBoolean("shouldSendNoti", true).apply()
            return
        }

    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            LOCATION_REQUEST_CODE ->{
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    AlertDialog.Builder(this)
                        .setTitle("Accesso alla posizione")
                        .setMessage("L'applicazione deve poter accedere alla tua posizione quando non é aperta per inviarti notifiche correttamente.\n" +
                                "Sarai reindirizzato alle impostazioni, concedi il permesso e torna indietro per continuare")
                        .setPositiveButton("Vai") { _, _ ->
                            requestBackgroundLocation()
                        }
                        .setNegativeButton("Annulla") { _, _ ->
                            val notiSwitch = findViewById<Switch>(R.id.notificationSwitch)
                            notiSwitch.isChecked = false
                        }
                        .show()
                    return
                }else{
                    if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
                        Snackbar.make(
                            findViewById(R.id.snackbarView),
                            "Devi concedere il permesso per poter ricevere notifiche",
                            Snackbar.LENGTH_LONG
                        ).setAction("Ok") {
                            requestPermissions(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ),
                                LOCATION_REQUEST_CODE
                            )
                        }.show()
                    }else{
                        Snackbar.make(
                            findViewById(R.id.snackbarView),
                            "Hai negato i permessi permanentemente, vai alle impostazioni per concederli",
                            Snackbar.LENGTH_LONG
                        ).setAction("Vai") {
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        }
                    }
                }
            }
            BACKGROUND_LOCATION_REQUEST_CODE -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //permission granted
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                        requestPostNotifications()
                    }else{
                        val sharedPref = getSharedPreferences("mgrsSettings", Context.MODE_PRIVATE)
                        sharedPref.edit().putBoolean("shouldSendNoti", true).apply()
                    }
                }else{
                    val notiSwitch = findViewById<Switch>(R.id.notificationSwitch)
                    notiSwitch.isChecked = false
                    if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)){
                        requestBackgroundLocation()
                    }else{
                        AlertDialog.Builder(this)
                            .setTitle("Accesso alla posizione")
                            .setMessage("Hai negato il permesso numerose volte, vai alle impostazioni per concederlo manualmente")
                            .setPositiveButton("Vai") { _, _ ->
                                val intent = Intent()
                                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                val uri = Uri.fromParts("package", packageName, null)
                                intent.data = uri
                                startActivity(intent)
                            }
                            .setNegativeButton("No") { _, _ ->
                                val notiSwitch = findViewById<Switch>(R.id.notificationSwitch)
                                notiSwitch.isChecked = false
                            }
                            .show()

                    }
                }
            }
            POST_NOTIFICATIONS_REQUEST_CODE -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //permission granted
                    val sharedPref = getSharedPreferences("mgrsSettings", Context.MODE_PRIVATE)
                    sharedPref.edit().putBoolean("shouldSendNoti", true).apply()
                    return
                }else{
                    if(shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)){
                        Snackbar.make(
                            findViewById(R.id.snackbarView),
                            "Devi concedere il permesso per poter ricevere le notifiche!",
                            Snackbar.LENGTH_LONG
                        ).setAction("Ok") {
                            requestPermissions(
                                arrayOf(
                                    Manifest.permission.POST_NOTIFICATIONS
                                ),
                                POST_NOTIFICATIONS_REQUEST_CODE
                            )
                        }.show()
                    }else{
                        Snackbar.make(
                            findViewById(R.id.snackbarView),
                            "Hai negato i permessi permanentemente, vai alle impostazioni per concederli",
                            Snackbar.LENGTH_LONG
                        ).setAction("Vai") {
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        }
                    }
                }
            }
        }
    }

    private fun scheduleJob() {
        println("Notification job scheduled - MGRSMap")
        val serviceComponent = ComponentName(this, LocationCheckJob::class.java)
        val builder = JobInfo.Builder(0, serviceComponent)
        builder.setPeriodic(60 * 60 * 1000)  // 1 hour
        val jobScheduler = getSystemService(JobScheduler::class.java)
        jobScheduler?.schedule(builder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        val sharedPref = getSharedPreferences("mgrsSettings", Context.MODE_PRIVATE)
        if (sharedPref.getBoolean("shouldSendNoti",false)){
            scheduleJob()
        }
    }

    private fun setNotificationResetDaily(context: Context) {
        val sharedPref = context.getSharedPreferences("mgrsSettings", Context.MODE_PRIVATE)

        // If the current time is greater than the last alarm time, it means the last alarm has passed
        if (System.currentTimeMillis() > sharedPref.getLong("lastAlarmTime", 0)) {
            // reset the alarmSet flag
            sharedPref.edit().putBoolean("alarmSet", false).apply()
        }

        if (sharedPref.getBoolean("alarmSet", false)) {
            return
        }

        val calendar = Calendar.getInstance()
        val timeLastNotification = sharedPref.getLong("timeLastNotification", 0)
        val oneDayInMillis = 24 * 60 * 60 * 1000 // 24h in ms

        if (System.currentTimeMillis() - timeLastNotification < oneDayInMillis) {
            calendar.apply {
                set(Calendar.HOUR_OF_DAY, 6)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, 1) // set to tomorrow
            }
            sharedPref.edit()
                .putBoolean("notiSentAlready", true)
                .putLong("lastAlarmTime", calendar.timeInMillis)
                .apply()
        } else {
            //set the alarm for 10 min from now
            calendar.add(Calendar.MINUTE, 10)
            sharedPref.edit()
                .putBoolean("notiSentAlready", false)
                .putLong("lastAlarmTime", calendar.timeInMillis)
                .apply()
        }


        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReset::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )

        sharedPref.edit().putBoolean("alarmSet", true).apply()
    }

}

class NotificationReset : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // reset the alarm and saves the fact that the notification was sent so no more are sent for the day
        if (context != null) {
            val sharedPref = context.getSharedPreferences("mgrsSettings", Context.MODE_PRIVATE)
            sharedPref.edit().putBoolean("notiSentAlready", true).apply()
            sharedPref.edit().putBoolean("alarmSet", false).apply()
        }

    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED && context != null) {
            val sharedPref = context.getSharedPreferences("mgrsSettings", Context.MODE_PRIVATE)

            sharedPref.edit().putBoolean("notiSentAlready", false).apply() //Might be annoying to user if he reboots his phone multiple times a day (rare use case)

            if(sharedPref.getBoolean("shouldSendNoti", false)){
                val serviceComponent = ComponentName(context, LocationCheckJob::class.java)
                val builder = JobInfo.Builder(0, serviceComponent)
                builder.setPeriodic( 60 * 60 * 1000)  // 1 hour
                val jobScheduler = context.getSystemService(JobScheduler::class.java)
                jobScheduler?.schedule(builder.build())
                //job scheduled
            }

        }
    }
}

class LocationCheckJob : JobService() {
    val handlerThread = HandlerThread("NotificationThread") //need this to replace Looper.getMainLooper()
    var shoudlStop = false

    override fun onStartJob(params: JobParameters?): Boolean {
        val sharedPref = getSharedPreferences("mgrsSettings", Context.MODE_PRIVATE)
        if (!sharedPref.getBoolean("notiSentAlready", false)) {
            shoudlStop = false
            checkLocation(params)

        }
        return true  // true = job is performed in the background
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        shoudlStop = true
        return true  // true = retire job
    }

    private fun checkLocation(params: JobParameters?) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        handlerThread.start()
        val looper = handlerThread.looper
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = LocationRequest.Builder(5*60*1000).setPriority(Priority.PRIORITY_HIGH_ACCURACY).build() //5 min to reduce battery usage

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {

                for (location in locationResult.locations) {
                    if (location != null && !shoudlStop) {
                        if (isUserInZone(location)) {
                            // send notification
                            shoudlStop = true
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                                if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED){
                                    triggerNotification()
                                }
                            }else{
                                triggerNotification()
                            }
                            val sharedPref = getSharedPreferences("mgrsSettings", Context.MODE_PRIVATE)
                            sharedPref.edit().putBoolean("notiSentAlready", true).apply() //no more notification will be sent until the alarm triggers
                            sharedPref.edit().putLong("timeLastNotification", System.currentTimeMillis()).apply()
                            sharedPref.edit().putBoolean("alarmSet", false).apply()
                            fusedLocationProviderClient.removeLocationUpdates(this)
                            jobFinished(params, false) //don't reschedule
                            handlerThread.quitSafely()

                        }
                    }else{
                        //location is null or should stop
                        jobFinished(params, true) //reschedule
                        handlerThread.quitSafely()
                    }
                    if(shoudlStop){
                        fusedLocationProviderClient.removeLocationUpdates(this)
                        jobFinished(params, false) //don't reschedule
                        handlerThread.quitSafely()
                    }
                }
            }
        }

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            looper
        ).addOnFailureListener { exception ->
            println("Request failed: $exception")
        }

    }

    private fun isUserInZone(location: Location): Boolean {
        //get all 1km mgrs from db
        val locationsList = MGRSDatabase.getDatabase(this).MGRSDao().getAllOneKM()

        if(locationsList.isEmpty()){ return true } //if the db is empty this function always returns false without this

        //get user location and convert
        val userMgrs = MGRS.from(location.longitude, location.latitude)
        val userMgrsString = userMgrs.coordinate(GridType.KILOMETER)

        for (mgrs in locationsList){
            if (mgrs.mgrsLocation == userMgrsString){
                return false
            }
        }

        return true //location was not found in db
    }

    private fun triggerNotification() {

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notiChannelId = "MGRSMapNotificationChannel"
        if (notificationManager.getNotificationChannel(notiChannelId) == null) {
            val notiChannelName = "MGRSMap Notifications"
            val notiChanneldescriptionText = "MGRSMap notification channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val notiChannel = NotificationChannel(notiChannelId, notiChannelName, importance).apply {
                description = notiChanneldescriptionText
            }
            notificationManager.createNotificationChannel(notiChannel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = NotificationCompat.Builder(this, "MGRSMapNotificationChannel")
            .setSmallIcon(R.mipmap.mgrs_icon_round)
            .setContentTitle("MGRSMap")
            .setContentText("Stai esplorando una zona senza misurazioni!")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Sei in una zona nella quale non hai effettuato misurazioni.\nNon perdere l'occasione!"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(1, notificationBuilder.build())
    }

}

