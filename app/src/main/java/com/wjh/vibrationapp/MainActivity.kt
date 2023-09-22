package com.wjh.vibrationapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wjh.vibrationapp.ui.theme.VibrationAppTheme

class MainActivity : ComponentActivity() {

    companion object {
        var shouldVibrate = true
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VibrationAppTheme {
                VibrationScreen({ minutes ->
                    setAlarm(minutes)
                }, this@MainActivity)
            }
        }
    }

//    private fun setAlarm(minutes: Float) {
//        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
//        val intent = Intent(this, AlarmReceiver::class.java)
//        intent.putExtra("INTERVAL", minutes)
//        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
//        alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (minutes * 60 * 1000).toLong(), pendingIntent)
//    }

    private fun setAlarm(minutes: Float) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("INTERVAL", minutes)
        }
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        // Cancel any existing alarms before setting a new one
        alarmManager.cancel(pendingIntent)
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (minutes * 60 * 1000).toLong(), pendingIntent)
    }



    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun VibrationScreen(onStartVibration: (Float) -> Unit, context: Context) {
        var minutes by remember { mutableStateOf("") }
        var countdownTime by remember { mutableStateOf(0) }
        var countdownTimer: CountDownTimer? = null
        fun startCountdown(minutes: Float) {
            val totalTimeMillis = (minutes * 60 * 1000).toLong()

            // Cancel any previously running timer (if you want to prevent multiple timers running at the same time)
            countdownTimer?.cancel()

            countdownTimer = object : CountDownTimer(totalTimeMillis, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    countdownTime = (millisUntilFinished / 1000).toInt()
                }

                override fun onFinish() {
                    countdownTime = 0
                    // Restart the timer once it finishes
                    startCountdown(minutes)
                }
            }.also { it.start() }
        }
        fun stopCountdown() {
            countdownTimer?.cancel()
            countdownTime = 0  // Reset the displayed countdown
        }
        fun cancelAlarm() {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE)
            alarmManager.cancel(pendingIntent)
        }
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Enter minutes to vibrate:", fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                var minutes by remember { mutableStateOf("") }

                TextField(
                    value = minutes,
                    onValueChange = { minutes = it }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    val min = minutes.toFloatOrNull()
                    if (min != null) {
                        shouldVibrate = true
                        onStartVibration(min)
                        startCountdown(min)
                    }
                }) {
                    Text("Start Vibration Timer")
                }


                Spacer(modifier = Modifier.height(16.dp))
//                Button(onClick = {
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                        vibratePhone(context)
//                    }
//                }) {
//                    Text("Vibrate Now")
//                }
                Spacer(modifier = Modifier.height(16.dp))
                if (countdownTime > 0) {
                    val mins = countdownTime / 60
                    val secs = countdownTime % 60
                    Text(text = "Next vibration in: ${String.format("%02d:%02d", mins, secs)}", fontSize = 18.sp)
                } else {
                    Text(text = "Timer is not running", fontSize = 18.sp) // Optional message when timer is not running
                }

                Button(onClick = {
                    shouldVibrate = false
                    stopCountdown()
                    cancelAlarm()
                }) {
                    Text("Stop Timer")
                }

            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun vibratePhone(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.let {
            if (it.hasVibrator()) {
                it.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }





    class AlarmReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (shouldVibrate) {
                val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(1000)
                    }
                }
                val interval = intent?.getFloatExtra("INTERVAL", 1f) ?: 1f
                setAlarm(context, interval)


            }

        }
        private fun setAlarm(context: Context?, minutes: Float) {
            val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("INTERVAL", minutes) // Passing interval to the receiver
            }
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            alarmManager?.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (minutes * 60 * 1000).toLong(), pendingIntent)
        }

    }
}

@Preview(showBackground = true)
@Composable
fun VibrationScreenPreview() {
    VibrationAppTheme {
        val mainActivity = MainActivity()
        mainActivity.VibrationScreen({}, mainActivity)
    }
}