package com.example.project

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.project.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import navigation.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var bundle: Bundle? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        binding.bottomNavigation.setOnItemSelectedListener {
            setToolbarDefault()
            when (it.itemId) {
                R.id.action_home -> {
                    val detailViewFragment = DetailViewFragment()
                    detailViewFragment.arguments = bundle
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_content, detailViewFragment).commit()
                    true
                }
                R.id.action_search -> {
                    val gridFragment = GridFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_content, gridFragment).commit()
                    true
                }
                R.id.action_add_photo -> {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        startActivity(Intent(this, AddPhotoActivity::class.java))
                    }
                    true
                }
                R.id.action_favorite_alarm -> {
                    val alarmFragment = AlarmFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_content, alarmFragment).commit()
                    true
                }
                R.id.action_account -> {
                    val userFragment = UserFragment()
                    val bundle = Bundle()
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    bundle.putString("destinationUid", uid)
                    userFragment.arguments = bundle
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_content, userFragment).commit()
                    true
                }
                else -> false
            }
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            1
        )

        binding.bottomNavigation.selectedItemId = R.id.action_home
        registerPushToken()
    }

    private fun registerPushToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                val map = mutableMapOf<String, Any>()
                map["pushToken"] = token
                FirebaseFirestore.getInstance().collection("pushtokens").document(uid!!).set(map)
            }
        }
    }

    private fun setToolbarDefault() {
        binding.toolbarUsername.visibility = View.GONE
        binding.toolbarBtnBack.visibility = View.GONE
        binding.toolbarTitleImage.visibility = View.VISIBLE
    }

    fun setToolbar(string: String?) {
        binding.toolbarUsername.text = string
        binding.toolbarBtnBack.setOnClickListener {
            binding.bottomNavigation.selectedItemId = R.id.action_home
        }
        binding.toolbarTitleImage.visibility = View.GONE
        binding.toolbarUsername.visibility = View.VISIBLE
        binding.toolbarBtnBack.visibility = View.VISIBLE
    }

    fun goHome(pos: Int) {
        bundle = Bundle()
        bundle!!.putInt("pos", pos)
        binding.bottomNavigation.selectedItemId = R.id.action_home
        Handler(Looper.getMainLooper()).postDelayed({
            bundle!!.clear()
        }, 300)

    }
}