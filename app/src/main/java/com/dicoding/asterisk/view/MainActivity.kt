package com.dicoding.asterisk.view

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.asterisk.R
import com.dicoding.asterisk.databinding.ActivityMainBinding
import com.dicoding.asterisk.view.adapter.RestaurantAdapter
import com.dicoding.asterisk.view.model.MainViewModel
import com.dicoding.asterisk.view.model.MainViewModelFactory
import android.Manifest
import android.annotation.SuppressLint
import android.view.MenuItem
import android.widget.SearchView
import android.widget.Toast
import androidx.core.view.iterator
import androidx.lifecycle.ViewModelProvider
import com.dicoding.asterisk.data.remote.ApiConfig
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: RestaurantAdapter
    private lateinit var viewModel: MainViewModel
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Asterisk)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val initialApiService = ApiConfig.getApiService("")
        viewModel = ViewModelProvider(this, MainViewModelFactory.getInstance(this, initialApiService, fusedLocationClient)).get(MainViewModel::class.java)

        viewModel.getSession().observe(this) { user ->
            if (!user.isLoggedIn) {
                startActivity(Intent(this, WelcomeActivity::class.java))
                finish()
            } else {
                val token = user.token
                val apiService = ApiConfig.getApiService(token)

                binding.tvName.text = user.fullName + " \uD83D\uDC4B"
                viewModel = ViewModelProvider(this, MainViewModelFactory.getInstance(this, apiService, fusedLocationClient)).get(MainViewModel::class.java)
                adapter = RestaurantAdapter()

                setupRecyclerView()
                setupSearchView()
                checkLocationPermission()
                setupBottomNavigation()
            }
        }

        viewModel.showLoading.observe(this) {
            showLoading(it)
        }
        viewModel.restaurants.observe(this) { restaurants ->
            adapter.submitList(restaurants)
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    viewModel.fetchLocation()
                } else {
                    viewModel.searchRestaurants(newText)
                }
                return true
            }
        })
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(this)
        binding.rvRestaurant.layoutManager = layoutManager
        binding.rvRestaurant.adapter = adapter
        val itemDecoration = DividerItemDecoration(this, layoutManager.orientation)
        binding.rvRestaurant.addItemDecoration(itemDecoration)
    }

    private fun moveToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun moveToMyReviewActivity() {
        startActivity(Intent(this, MyReviewActivity::class.java))
    }

    private fun moveToProfileActivity() {
        startActivity(Intent(this, ProfileActivity::class.java))
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_home -> {
                    updateNavIcon(item, R.drawable.ic_baseline_home_24_blue)
                    moveToMainActivity()
                }

                R.id.action_review -> {
                    updateNavIcon(item, R.drawable.ic_baseline_review_24_blue)
                    moveToMyReviewActivity()
                }

                R.id.action_profile -> {
                    updateNavIcon(item, R.drawable.ic_baseline_account_24_blue)
                    moveToProfileActivity()
                }
            }
            true
        }
    }

    private fun updateNavIcon(item: MenuItem, iconResId: Int) {
        item.icon = ContextCompat.getDrawable(this, iconResId)
        resetNavIconsExcept(item)
    }

    private fun resetNavIconsExcept(exceptItem: MenuItem) {
        val menu = binding.bottomNavigation.menu
        for (item in menu) {
            if (item!= exceptItem) {
                when (item.itemId) {
                    R.id.action_home -> item.icon = ContextCompat.getDrawable(this, R.drawable.ic_baseline_home_24)
                    R.id.action_review -> item.icon = ContextCompat.getDrawable(this, R.drawable.ic_baseline_review_24)
                    R.id.action_profile -> item.icon = ContextCompat.getDrawable(this, R.drawable.ic_baseline_account_24)
                }
            }
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            viewModel.fetchLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            viewModel.fetchLocation()
        } else {
            Toast.makeText(this, "Location permission is needed to run this application", Toast.LENGTH_SHORT).show()
        }
    }
}