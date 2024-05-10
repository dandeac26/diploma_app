package com.example.myapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.activities.ProductActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
open class BaseActivity : AppCompatActivity() {

    protected fun setupBottomNavigation(selectedItemId: Int) {
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = selectedItemId
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (selectedItemId != R.id.nav_home) {
                        val intent = Intent(this, MainActivity::class.java)
                        intent.putExtra("selectedItemId", R.id.nav_home)
                        startActivity(intent)
                    }
                    true
                }
                R.id.nav_products -> {
                    if (selectedItemId != R.id.nav_products) {
                        val intent = Intent(this, ProductActivity::class.java)
                        intent.putExtra("selectedItemId", R.id.nav_products)
                        startActivity(intent)
                    }
                    true
                }
                R.id.nav_orders -> {
                    // Handle orders navigation
                    true
                }
                R.id.nav_recipes -> {
                    // Handle recipes navigation
                    true
                }
                else -> false
            }
        }
    }
}