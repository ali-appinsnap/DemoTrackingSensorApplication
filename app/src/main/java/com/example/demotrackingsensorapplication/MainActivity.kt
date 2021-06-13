package com.example.demotrackingsensorapplication

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.demotrackingsensorapplication.databinding.ActivityMainBinding
import com.example.demotrackingsensorapplication.eventBus.ArrivedObjectListener
import com.example.demotrackingsensorapplication.eventBus.ObjectArrivalHelper
import com.example.demotrackingsensorapplication.models.TripLogHelper
import com.example.demotrackingsensorapplication.services.Actions
import com.example.demotrackingsensorapplication.services.DemoService.Companion.TRIP_HELPER
import com.example.demotrackingsensorapplication.services.actionOnService
import java.io.IOException
import java.util.ArrayList

class MainActivity : AppCompatActivity(), View.OnClickListener, ArrivedObjectListener {

    lateinit var binding: ActivityMainBinding
    lateinit var prefsHelper: SharePreferencesHelper
    private var logsList = ArrayList<String>()
    var adpter = LogsAdapter(logsList)
    private var tripLogHelper: TripLogHelper = TripLogHelper()

    private lateinit var objectArrivalHelper: ObjectArrivalHelper

    companion object {
        const val REQUEST_CODE_FINE_LOCATION = 99
    }

    var mPlayer: MediaPlayer? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefsHelper = SharePreferencesHelper.invoke(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.onclickList = this
        binding.logsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.logsRecyclerView.adapter = adpter
        objectArrivalHelper = ObjectArrivalHelper(this, this)
        objectArrivalHelper.registerBroadCast()
        setDetails(tripLogHelper)
        actionOnService(this@MainActivity, Actions.START, prefsHelper)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        /*objectArrivalHelper.unregisterBroadCast()
        actionOnService(this@MainActivity, Actions.STOP, prefsHelper)*/

    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.startBtn -> {
                actionOnService(this@MainActivity, Actions.START, prefsHelper)
            }
            R.id.endBtn -> {
                actionOnService(this@MainActivity, Actions.STOP, prefsHelper)

            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        when (requestCode) {
            REQUEST_CODE_FINE_LOCATION -> {
                if (grantResults.isNotEmpty()) {
                    if (checkAllGranted(grantResults)) {
                        actionOnService(this@MainActivity, Actions.START, prefsHelper)
                    }
                }
            }
        }

//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    }


    private fun checkAllGranted(grantResults: IntArray): Boolean {
        var granted = true
        for (result in grantResults) {
            if (result == PackageManager.PERMISSION_DENIED) {
                granted = false
                break
            }
        }
        return granted
    }

    override fun getIntent(context: Context?, intent: Intent?) {
        var obj: TripLogHelper? = intent?.getParcelableExtra<TripLogHelper>(TRIP_HELPER)
        obj?.let {
            adpter.insertItem(it.logMsg)
            setDetails(it)
            if (adpter.itemCount != 0)
                binding.logsRecyclerView.smoothScrollToPosition(adpter.itemCount - 1)
        }
    }

    private fun setDetails(tripLogHelper: TripLogHelper) {
        binding.tripStatusTv.text = "Trip Status :" + if (tripLogHelper.tripStarted) {
            "Started"
        } else {
            "End"
        }
        binding.tripDistance.text = "Trip Distance : ${tripLogHelper.tripDistance}"
        binding.maxSpeedTv.text = "Max Speed : ${tripLogHelper.tripStats.maxSpeed}"
        binding.overSpeedCounterTv.text =
            "Over Speed Counter : ${tripLogHelper.tripStats.overSpeedCounter}"
        binding.accelerationCounterTv.text =
            "Acceleration Counter :  ${tripLogHelper.tripStats.accelerationCounter}"
        binding.harshBreakingCounterTv.text =
            "Harsh Breaking Counter ${tripLogHelper.tripStats.harshBreakingCounter}"
        binding.corneringTv.text = "Cornering ${tripLogHelper.tripStats.corneringCounter}"
    }

    @Synchronized
    private fun playVoiceCommand(resource: Int) {
        if (mPlayer != null) {
            if (mPlayer!!.isPlaying) {
                return
            }
        }
        mPlayer = MediaPlayer.create(
            this@MainActivity,
            resource
        )
        try {
            mPlayer!!.prepare()
        } catch (e: IllegalStateException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

        mPlayer!!.start()
    }


}