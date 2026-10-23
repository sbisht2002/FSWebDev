package com.example.callrecorder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.callrecorder.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: RecordingsAdapter

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = result.values.all { it }
        if (allGranted) {
            Toast.makeText(this, R.string.perms_granted, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.perms_denied, Toast.LENGTH_LONG).show()
        }
        refreshStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = RecordingsAdapter()
        binding.recordingsList.layoutManager = LinearLayoutManager(this)
        binding.recordingsList.adapter = adapter

        binding.grantButton.setOnClickListener { requestPermissions() }
        binding.refreshButton.setOnClickListener { loadRecordings() }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        loadRecordings()
    }

    private fun requiredPermissions(): Array<String> {
        val perms = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return perms.toTypedArray()
    }

    private fun hasAllPermissions(): Boolean = requiredPermissions().all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        permissionLauncher.launch(requiredPermissions())
    }

    private fun refreshStatus() {
        if (hasAllPermissions()) {
            binding.statusText.setText(R.string.status_ready)
            binding.grantButton.isEnabled = false
        } else {
            binding.statusText.setText(R.string.status_need_perms)
            binding.grantButton.isEnabled = true
        }
    }

    private fun loadRecordings() {
        val dir = CallRecordingService.recordingsDir(this)
        val files = dir.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList<File>()
        adapter.submit(files)
        binding.emptyText.visibility =
            if (files.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }
}
