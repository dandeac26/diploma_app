package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.net.ConnectivityManager
import com.example.myapplication.fragments.HomeFragment
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.config.NetworkChangeReceiver
import com.example.myapplication.config.RetrofitInstance
import com.example.myapplication.fragments.ClientsFragment
import com.example.myapplication.fragments.OrdersFragment
import com.example.myapplication.fragments.ProductsFragment
import com.example.myapplication.fragments.RecipesFragment
import com.example.myapplication.fragments.SettingsFragment
import com.example.myapplication.views.SharedViewModel
import com.example.myapplication.views.SharedViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {

    private var currentFragment: Fragment? = null
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var networkChangeReceiver: NetworkChangeReceiver
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val factory = SharedViewModelFactory()
        sharedViewModel = ViewModelProvider(this, factory).get(SharedViewModel::class.java)

        networkChangeReceiver = NetworkChangeReceiver {
            RetrofitInstance.getInstance(applicationContext)
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_products -> ProductsFragment()
                R.id.nav_orders -> OrdersFragment()
                R.id.nav_recipes -> RecipesFragment()
                R.id.nav_clients -> ClientsFragment()
                else -> null
            }

            if (fragment != null) {
                switchFragment(fragment)
            }

            true
        }

        if (savedInstanceState == null) {
            switchFragment(HomeFragment())
        }

    }

    fun updateTopNavTitle(title: String) {
        supportActionBar?.title = title
    }

    fun switchFragment(fragment: Fragment) {
        val fragmentTag = fragment::class.java.simpleName

        val transaction = supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, 0, 0, 0)

        // Hide the current fragment
        currentFragment?.let { transaction.hide(it) }

        // Try to find the fragment in the FragmentManager
        var newFragment = supportFragmentManager.findFragmentByTag(fragmentTag)

        if (newFragment == null) {
            // If not found, add it
            transaction.add(R.id.fragment_container, fragment, fragmentTag)
                .addToBackStack(null)
            newFragment = fragment
        } else {
            // If found, show it
            transaction.show(newFragment)
        }

        transaction.commit()

        // Update the current fragment
        currentFragment = newFragment

        when (newFragment) {
            is ClientsFragment -> {
                sharedViewModel.refreshClientsTrigger.value = true
                updateTopNavTitle("Clients")
            }

            is OrdersFragment -> {
                sharedViewModel.refreshOrdersTrigger.value = true
                updateTopNavTitle("Orders")
            }

            is RecipesFragment -> {
                sharedViewModel.refreshRecipesTrigger.value = true
                updateTopNavTitle("Recipes")
            }

            is ProductsFragment -> {
                sharedViewModel.refreshProductsTrigger.value = true
                updateTopNavTitle("Products")
            }

            is HomeFragment -> {
                sharedViewModel.refreshHomeTrigger.value = true
                updateTopNavTitle("Pastry Central")
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_products -> {
                // do sth
                true
            }
            R.id.action_settings -> {
                switchFragment(SettingsFragment())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        sharedViewModel.refreshClientsTrigger.value = true
        sharedViewModel.refreshProductsTrigger.value = true
        sharedViewModel.refreshOrdersTrigger.value = true
        sharedViewModel.refreshRecipesTrigger.value = true
        sharedViewModel.refreshHomeTrigger.value = true
        registerReceiver(networkChangeReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(networkChangeReceiver)
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        sharedViewModel.handleBackPress()
    }
}