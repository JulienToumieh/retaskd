package com.thatonedev.retaskd

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.thatonedev.retaskd.Components.TaskComponent
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.xml.KonfettiView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), TaskComponent.OnDataPass {
    private lateinit var konfettiView: KonfettiView
    private lateinit var mediaPlayer: MediaPlayer

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        konfettiView = findViewById(R.id.konfettiView)

        val mainLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main)
        val taskInputContainer = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.taskInputContainer)
        val newTaskInput = findViewById<EditText>(R.id.newTaskInput)
        val createTaskButton = findViewById<ImageButton>(R.id.createTaskButton)

        mediaPlayer = MediaPlayer.create(this, R.raw.completion_sound)

        // Handle window insets to adjust layout for keyboard
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)

            // Adjust bottom padding for the keyboard
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val isKeyboardVisible = imeInsets.bottom > 0
            val bottomPadding = if (isKeyboardVisible) imeInsets.bottom else 0

            // Apply padding to the taskInputContainer
            taskInputContainer.setPadding(taskInputContainer.paddingStart, taskInputContainer.paddingTop, taskInputContainer.paddingEnd, bottomPadding)

            insets
        }

        findViewById<Switch>(R.id.autoResetSwitch).isChecked = getSharedPreferences("app", MODE_PRIVATE).getBoolean("autoReset", true)
        if (getSharedPreferences("app", MODE_PRIVATE).getBoolean("autoReset", true)) {
            checkAndResetTasks()
        }

        if (getSharedPreferences("app", MODE_PRIVATE).getBoolean("firstRun", true)) {
            getSharedPreferences("app", MODE_PRIVATE).edit().putBoolean("firstRun", false).apply()

            val task1 = JSONObject().apply {
                put("title", "Click on the checkbox to complete task.")
                put("completed", false)
            }

            val task2 = JSONObject().apply {
                put("title", "Click and hold on a task to delete.")
                put("completed", true)
            }
            val task3 = JSONObject().apply {
                put("title", "Click on the 'Retaskd' text at the top to open the backup/restore popup.")
                put("completed", false)
            }


            val tasksArray = JSONArray().apply {
                put(task1)
                put(task2)
                put(task3)
            }

            saveTasksToFile(this, tasksArray)
        }

        createTaskButton.setOnClickListener {
            val taskTitle = newTaskInput.text.toString()
            if (taskTitle.isNotEmpty()) {
                createTask(this, taskTitle)
            }
            newTaskInput.text = null
        }

        findViewById<ImageButton>(R.id.resetTasksButton).setOnClickListener {
            val tasksArray = loadTasksFromFile(this) ?: JSONArray()
            for (i in 0 until tasksArray.length()) {
                val task = tasksArray.getJSONObject(i)
                if (task.getBoolean("completed")) {
                    task.put("completed", false)
                }
            }
            saveTasksToFile(this, tasksArray)
            refreshTasks()
        }

        findViewById<Switch>(R.id.autoResetSwitch).setOnCheckedChangeListener { _, isChecked ->
            getSharedPreferences("app", MODE_PRIVATE).edit().putBoolean("autoReset", isChecked).apply()
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            saveLastResetDate(currentDate)
        }

        findViewById<Button>(R.id.restoreButton).setOnClickListener { onRestoreButtonClick() }
        findViewById<Button>(R.id.backupButton).setOnClickListener { onBackupButtonClick() }
        findViewById<TextView>(R.id.textView).setOnClickListener {
            findViewById<CardView>(R.id.backupPopup).isVisible = true
        }
        findViewById<CardView>(R.id.backupPopup).setOnClickListener {
            findViewById<CardView>(R.id.backupPopup).isVisible = false
        }

        refreshTasks()
    }

    private fun refreshTasks(){
        val recyclerView = findViewById<RecyclerView>(R.id.TaskRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = loadTasksFromFile(this)?.let { TaskComponent(this, it) }
        recyclerView.adapter = adapter
    }

    private fun saveLastResetDate(date: String) {
        getSharedPreferences("app", MODE_PRIVATE).edit().putString("lastResetDate", date).apply()
    }

    private fun saveTasksToFile(context: Context, tasksArray: JSONArray) {
        val file = File(context.filesDir, "tasks.json")
        file.writeText(tasksArray.toString())
    }

    private fun loadTasksFromFile(context: Context): JSONArray? {
        val file = File(context.filesDir, "tasks.json")
        if (file.exists()) {
            val jsonString = file.readText()
            return JSONArray(jsonString)
        }
        return null
    }

    private fun createTask(context: Context, title: String) {
        val tasksArray = loadTasksFromFile(context) ?: JSONArray()

        val newTask = JSONObject().apply {
            put("title", title)
            put("completed", false)
        }
        tasksArray.put(newTask)

        saveTasksToFile(context, tasksArray)
        refreshTasks()
    }

    override fun toggleTask(position: Int) {
        val tasksArray = loadTasksFromFile(this) ?: return
        val task = tasksArray.getJSONObject(position)
        val newCompletedStatus = !task.getBoolean("completed")
        task.put("completed", newCompletedStatus)
        saveTasksToFile(this, tasksArray)
        //refreshTasks()
    }

    override fun deleteTask(position: Int) {
        val tasksArray = loadTasksFromFile(this) ?: return
        tasksArray.remove(position)
        saveTasksToFile(this, tasksArray)
        refreshTasks()
    }

    private fun checkAndResetTasks() {
        val sharedPrefs = getSharedPreferences("app", MODE_PRIVATE)
        val lastResetDate = sharedPrefs.getString("lastResetDate", null)
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (lastResetDate != currentDate) {
            // Reset tasks
            val tasksArray = loadTasksFromFile(this) ?: JSONArray()
            for (i in 0 until tasksArray.length()) {
                val task = tasksArray.getJSONObject(i)
                if (task.getBoolean("completed")) {
                    task.put("completed", false)
                }
            }
            saveTasksToFile(this, tasksArray)
            saveLastResetDate(currentDate)
        }
    }

    override fun showConfetti() {
        val party = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
            position = Position.Relative(0.5, 0.3)
        )
        konfettiView.bringToFront()
        konfettiView.start(party)
        playCompletionSound()
    }

    private fun playCompletionSound() {
        mediaPlayer = MediaPlayer.create(this, R.raw.completion_sound)
        mediaPlayer.start()
    }

    private val backupLauncher = registerForActivityResult(CreateDocument("todo/todo")) { uri: Uri? ->
        uri?.let {
            backupTasksToUri(it)
        }
    }

    private val restoreLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            restoreTasksFromUri(it)
        }
    }

    private fun backupTasksToUri(uri: Uri) {
        val tasksArray = loadTasksFromFile(this) ?: JSONArray()
        val jsonString = tasksArray.toString()
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(jsonString.toByteArray())
        }
        findViewById<CardView>(R.id.backupPopup).isVisible = false
    }

    private fun restoreTasksFromUri(uri: Uri) {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val tasksArray = JSONArray(jsonString)
            saveTasksToFile(this, tasksArray)
            refreshTasks()
            findViewById<CardView>(R.id.backupPopup).isVisible = false
        }
    }

    fun onBackupButtonClick() {
        backupLauncher.launch("Retaskd_Backup.json")
    }

    fun onRestoreButtonClick() {
        restoreLauncher.launch(arrayOf("*/*"))
    }

    override fun onBackPressed() {
        if (findViewById<CardView>(R.id.backupPopup).isVisible)
            findViewById<CardView>(R.id.backupPopup).isVisible = false
        else
            super.onBackPressed()
    }
}
