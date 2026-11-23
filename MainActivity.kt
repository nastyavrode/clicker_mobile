package com.example.myapplication

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvCounter: TextView
    private lateinit var tvInfo: TextView
    private lateinit var btnClick: Button
    private lateinit var btnUpgrade1: Button
    private lateinit var btnUpgrade2: Button

    // Счётчик и параметры улучшений
    private var counter = 0
    private var clickPower = 1          // сколько добавляется за клик
    private var autoPower = 0           // сколько добавляется автоматически в секунду

    // Цены улучшений
    private var clickUpgradeCost = 10
    private var autoUpgradeCost = 50

    // Таймер для автоклика
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var autoRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvCounter = findViewById(R.id.tvCounter)
        tvInfo = findViewById(R.id.tvInfo)
        btnClick = findViewById(R.id.btnClick)
        btnUpgrade1 = findViewById(R.id.btnUpgrade1)
        btnUpgrade2 = findViewById(R.id.btnUpgrade2)

        // Клик по главной кнопке
        btnClick.setOnClickListener {
            counter += clickPower
            updateUi()
        }

        // Покупка улучшения "сила клика"
        btnUpgrade1.setOnClickListener {
            if (counter >= clickUpgradeCost) {
                counter -= clickUpgradeCost
                clickPower++                       // +1 к силе клика
                clickUpgradeCost *= 2              // цена растёт в 2 раза
                updateUi()
            }
        }
        btnUpgrade2.setOnClickListener {
            if (counter >= autoUpgradeCost) {
                counter -= autoUpgradeCost
                autoPower++                       // +1 к автоклику
                autoUpgradeCost *= 2              // цена растёт в 2 раза
                updateUi()
            }
        }
        autoRunnable = object : Runnable {
            override fun run() {
                if (autoPower > 0) {
                    counter += autoPower
                    updateUi()
                } // +1 к
                handler.postDelayed(this, 1000)
            }
        }
        handler.postDelayed(autoRunnable, 1000)
        updateUi()
    }
    private fun updateUi() {
        tvCounter.text = counter.toString()
        tvInfo.text = "+$clickPower за клик | +$autoPower в секунду"
        btnUpgrade1.text = "Улучшение клика (${clickUpgradeCost})"
        btnUpgrade2.text = "Улучшение автоклика (${autoUpgradeCost})"
        btnUpgrade1.isEnabled = counter >= clickUpgradeCost
        btnUpgrade2.isEnabled = counter >= autoUpgradeCost
    }
    override fun onDestroy() {
    super.onDestroy()
        handler.removeCallbacks(autoRunnable)
    }
}
