package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    // -------- UI --------
    private lateinit var tvCounter: TextView
    private lateinit var tvInfo: TextView
    private lateinit var btnClick: Button
    private lateinit var btnUpgradeClick: Button
    private lateinit var btnUpgradeAuto: Button
    private lateinit var btnUpgradeCrit: Button
    private lateinit var btnUpgradeMega: Button
    private lateinit var btnUpgradeTurbo: Button
    private lateinit var btnUpgradeGold: Button
    private lateinit var btnReset: Button

    // -------- Игровое состояние --------
    private var counter = 0L
    private var clickPower = 1L
    private var autoPower = 0L
    private var critChance = 0f          // 0..1
    private var critMultiplier = 5       // во сколько раз крит
    private var goldEvery = 0            // раз в сколько кликов срабатывает золотой бонус (0 = выкл)
    private var goldMultiplier = 10
    private var clickCount = 0L

    // Цены (Long, чтобы не переполнились быстро)
    private var clickCost = 10L
    private var autoCost = 50L
    private var critCost = 100L
    private var megaCost = 200L
    private var turboCost = 500L
    private var goldCost = 1000L

    // Автоклик
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var autoRunnable: Runnable

    private val prefs by lazy {
        getSharedPreferences("clicker_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Находим элементы
        tvCounter        = findViewById(R.id.tvCounter)
        tvInfo           = findViewById(R.id.tvInfo)
        btnClick         = findViewById(R.id.btnClick)
        btnUpgradeClick  = findViewById(R.id.btnUpgradeClick)
        btnUpgradeAuto   = findViewById(R.id.btnUpgradeAuto)
        btnUpgradeCrit   = findViewById(R.id.btnUpgradeCrit)
        btnUpgradeMega   = findViewById(R.id.btnUpgradeMega)
        btnUpgradeTurbo  = findViewById(R.id.btnUpgradeTurbo)
        btnUpgradeGold   = findViewById(R.id.btnUpgradeGold)
        btnReset         = findViewById(R.id.btnReset)

        loadState()

        // ---- Клик ----
        btnClick.setOnClickListener {
            clickCount++

            var gain = clickPower

            // Критический клик
            if (critChance > 0f && Random.nextFloat() < critChance) {
                gain *= critMultiplier
            }

            // Золотой клик (каждый N-й клик)
            if (goldEvery > 0 && clickCount % goldEvery == 0L) {
                gain *= goldMultiplier
            }

            counter += gain
            updateUi()
            saveState()
        }

        // ---- Улучшения ----
        btnUpgradeClick.setOnClickListener {
            if (counter >= clickCost) {
                counter -= clickCost
                clickPower++
                clickCost *= 2
                updateUi(); saveState()
            }
        }

        btnUpgradeAuto.setOnClickListener {
            if (counter >= autoCost) {
                counter -= autoCost
                autoPower++
                autoCost *= 2
                updateUi(); saveState()
            }
        }

        btnUpgradeCrit.setOnClickListener {
            if (counter >= critCost) {
                counter -= critCost
                // +10% шанс крита за покупку, макс 80%
                critChance = (critChance + 0.10f).coerceAtMost(0.8f)
                critCost *= 2
                updateUi(); saveState()
            }
        }

        btnUpgradeMega.setOnClickListener {
            if (counter >= megaCost) {
                counter -= megaCost
                clickPower += 5
                megaCost *= 2
                updateUi(); saveState()
            }
        }

        btnUpgradeTurbo.setOnClickListener {
            if (counter >= turboCost) {
                counter -= turboCost
                autoPower += 5
                turboCost *= 2
                updateUi(); saveState()
            }
        }

        btnUpgradeGold.setOnClickListener {
            if (counter >= goldCost) {
                counter -= goldCost
                // Первый раз ставим "каждый 10-й клик", потом уменьшаем до 5, 3...
                goldEvery = when {
                    goldEvery == 0 -> 10
                    goldEvery > 3  -> goldEvery / 2
                    else           -> goldEvery
                }
                goldCost *= 2
                updateUi(); saveState()
            }
        }

        // ---- Сброс ----
        btnReset.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Сбросить прогресс?")
                .setMessage("Весь прогресс будет удалён.")
                .setPositiveButton("Да") { _, _ -> resetAll() }
                .setNegativeButton("Отмена", null)
                .show()
        }

        // ---- Автоклик каждую секунду ----
        autoRunnable = object : Runnable {
            override fun run() {
                if (autoPower > 0) {
                    counter += autoPower
                    updateUi()
                    saveState()
                }
                handler.postDelayed(this, 1000)
            }
        }
        handler.postDelayed(autoRunnable, 1000)

        updateUi()
    }

    // -------- Обновление UI --------
    private fun updateUi() {
        tvCounter.text = counter.toString()

        val critText = if (critChance > 0f)
            " | крит ${(critChance * 100).toInt()}% x$critMultiplier" else ""
        val goldText = if (goldEvery > 0)
            " | золотой каждые $goldEvery" else ""

        tvInfo.text = "+$clickPower за клик | $autoPower в секунду$critText$goldText"

        btnUpgradeClick.text = "⚡ Сила клика ($clickCost)"
        btnUpgradeAuto.text  = "🤖 Автоклик ($autoCost)"
        btnUpgradeCrit.text  = "💥 Крит +10% ($critCost)"
        btnUpgradeMega.text  = "🔥 Мега-клик +5 ($megaCost)"
        btnUpgradeTurbo.text = "⏫ Турбо-авто +5/сек ($turboCost)"
        btnUpgradeGold.text  = "🌟 Золотой клик ($goldCost)"

        btnUpgradeClick.isEnabled = counter >= clickCost
        btnUpgradeAuto.isEnabled  = counter >= autoCost
        btnUpgradeCrit.isEnabled  = counter >= critCost && critChance < 0.8f
        btnUpgradeMega.isEnabled  = counter >= megaCost
        btnUpgradeTurbo.isEnabled = counter >= turboCost
        btnUpgradeGold.isEnabled  = counter >= goldCost
    }

    // -------- Сохранение / загрузка --------
    private fun saveState() {
        prefs.edit()
            .putLong("counter", counter)
            .putLong("clickPower", clickPower)
            .putLong("autoPower", autoPower)
            .putFloat("critChance", critChance)
            .putInt("goldEvery", goldEvery)
            .putLong("clickCount", clickCount)
            .putLong("clickCost", clickCost)
            .putLong("autoCost", autoCost)
            .putLong("critCost", critCost)
            .putLong("megaCost", megaCost)
            .putLong("turboCost", turboCost)
            .putLong("goldCost", goldCost)
            .apply()
    }

    private fun loadState() {
        counter      = prefs.getLong("counter", 0L)
        clickPower   = prefs.getLong("clickPower", 1L)
        autoPower    = prefs.getLong("autoPower", 0L)
        critChance   = prefs.getFloat("critChance", 0f)
        goldEvery    = prefs.getInt("goldEvery", 0)
        clickCount   = prefs.getLong("clickCount", 0L)
        clickCost    = prefs.getLong("clickCost", 10L)
        autoCost     = prefs.getLong("autoCost", 50L)
        critCost     = prefs.getLong("critCost", 100L)
        megaCost     = prefs.getLong("megaCost", 200L)
        turboCost    = prefs.getLong("turboCost", 500L)
        goldCost     = prefs.getLong("goldCost", 1000L)
    }

    private fun resetAll() {
        counter = 0; clickPower = 1; autoPower = 0
        critChance = 0f; goldEvery = 0; clickCount = 0
        clickCost = 10; autoCost = 50; critCost = 100
        megaCost = 200; turboCost = 500; goldCost = 1000
        prefs.edit().clear().apply()
        updateUi()
    }

    override fun onPause() {
        super.onPause()
        saveState()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(autoRunnable)
    }
}
