package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import com.example.myapplication.fragments.HomeFragment
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.config.NetworkChangeReceiver
import com.example.myapplication.config.RetrofitInstance
import com.example.myapplication.fragments.ClientsFragment.ClientSelectionListener
import com.example.myapplication.fragments.ClientsFragment
import com.example.myapplication.fragments.DailyOrderFragment
import com.example.myapplication.fragments.OrdersFragment
import com.example.myapplication.fragments.ProductDetailsFragment
import com.example.myapplication.fragments.ProductsFragment
import com.example.myapplication.fragments.SensorFragment
import com.example.myapplication.fragments.StocksFragment
import com.example.myapplication.fragments.SettingsFragment
import com.example.myapplication.views.SharedViewModel
import com.example.myapplication.views.SharedViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.LinkedList


class MainActivity : AppCompatActivity() {

    private var currentFragment: Fragment? = null
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var networkChangeReceiver: NetworkChangeReceiver
    private val REQUEST_READ_CONTACTS = 1
    private val fragmentToMenuItem = mapOf(
        HomeFragment::class.java to R.id.nav_home,
        ProductsFragment::class.java to R.id.nav_products,
        OrdersFragment::class.java to R.id.nav_orders,
        StocksFragment::class.java to R.id.nav_stocks,
        ClientsFragment::class.java to R.id.nav_clients
    )

    companion object {
        const val CHANNEL_ID = "pastry_central_channel"
    }

    private val fragmentHistory = LinkedList<Fragment>()

    private lateinit var webSocket: WebSocket

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentFragment?.let { supportFragmentManager.putFragment(outState, "currentFragment", it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val factory = SharedViewModelFactory()
        sharedViewModel = ViewModelProvider(this, factory)[SharedViewModel::class.java]

        networkChangeReceiver = NetworkChangeReceiver {
            RetrofitInstance.getInstance("http://", applicationContext, 8080)
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        /// CHECK PERMISSIONS -------------------------

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestReadContactsPermission()
        } else {
            // READ_CONTACTS permission is available
        }

        //---------------------------------------------

        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_products -> ProductsFragment().apply {
                    setProductSelectionListener(object : ProductsFragment.ProductsSelectionListener {
                        override fun onProductSelected(product: ProductsFragment.Product) {
                            sharedViewModel.selectProduct(product)
                            isProductSelectionListenerActive = false
                        }
                    })
                }
                R.id.nav_orders -> OrdersFragment()
                R.id.nav_stocks -> StocksFragment()
                R.id.nav_clients -> ClientsFragment().apply {
                    setClientSelectionListener(object : ClientSelectionListener {
                        override fun onClientSelected(client: ClientsFragment.Client) {
                            sharedViewModel.selectClient(client)
                            isClientSelectionListenerActive = false
                        }
                    })
                }
                else -> null
            }

            if (isChangingConfigurations) {
                return@setOnItemSelectedListener true
            }

            if (fragment != null) {
                switchFragment(fragment)
            }

            true
        }

        if (savedInstanceState != null) {
            currentFragment = supportFragmentManager.getFragment(savedInstanceState, "currentFragment")
        } else {
            switchFragment(HomeFragment())
        }

        connectWebSocket()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun connectWebSocket() {
        val client = OkHttpClient()
        val retrofit = RetrofitInstance.getInstance("http://", applicationContext, 8000)
        val httpUrl = retrofit.baseUrl()

        val request = Request.Builder()
            .url("ws://${httpUrl.host}:${httpUrl.port}/ws")
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                Log.i("WebSocket", "Connection opened")
                this@MainActivity.webSocket = webSocket
            }

            @SuppressLint("MissingPermission")
            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text == "Refetch orders") {
                    DailyOrderFragment().fetchDailyOrders()
                } else {
                    runOnUiThread {
                        Log.i("WebSocket", "Received message: $text")
                        val notificationId = 2
                        Toast.makeText(this@MainActivity, text, Toast.LENGTH_SHORT).show()
                        val builder = NotificationCompat.Builder(this@MainActivity, CHANNEL_ID)
                            .setSmallIcon(R.drawable.midday_100)
                            .setContentTitle("My notification")
                            .setContentText(text)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

                        with(NotificationManagerCompat.from(this@MainActivity)) {
                            notify(notificationId, builder.build())
                        }
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                Log.i("WebSocket", "Connection closed")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e("WebSocket", "Error: ${t.message}")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        client.newWebSocket(request, listener)
    }

    private fun updateTopNavTitle(title: String) {
        supportActionBar?.title = title
    }

    fun switchFragment(fragment: Fragment) {
        if (isChangingConfigurations) {
            return
        }

        if (fragmentHistory.isEmpty() || fragment::class.java != fragmentHistory.last::class.java) {
            fragmentHistory.add(fragment)
        }

        val fragmentTag = fragment::class.java.simpleName

        val transaction = supportFragmentManager.beginTransaction()

        if(currentFragment != null) {
            transaction.hide(currentFragment!!)
        }

        var newFragment = supportFragmentManager.findFragmentByTag(fragmentTag)

        if (newFragment != null && newFragment is ProductDetailsFragment) {
            transaction.remove(newFragment)
            newFragment = null
        }

        if (newFragment == null) {

            transaction.add(R.id.fragment_container, fragment, fragmentTag)

            newFragment = fragment
        } else {
            transaction.show(newFragment)
        }

        transaction.commit()

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

            is StocksFragment -> {
                sharedViewModel.refreshStocksTrigger.value = true
                updateTopNavTitle("Stocks")
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
            R.id.action_settings -> {
                switchFragment(SettingsFragment())
                true
            }
            R.id.action_monitoring -> {
                switchFragment(SensorFragment())
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
        sharedViewModel.refreshStocksTrigger.value = true
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
        if (fragmentHistory.size > 1) {

            fragmentHistory.removeLast()

            if(fragmentHistory.last is ProductDetailsFragment) {
                fragmentHistory.removeLast()
            }

            val previousFragment = fragmentHistory.last

            switchFragment(previousFragment)

            val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
            val menuItemId = fragmentToMenuItem[previousFragment::class.java]
            if (menuItemId != null) {
                bottomNavigation.selectedItemId = menuItemId
            }

        } else {
            super.onBackPressed()
        }
    }

    private fun requestReadContactsPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {
            Snackbar.make(findViewById(R.id.main), "Permission Contacts", Snackbar.LENGTH_INDEFINITE)
                .setAction("Ok") {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.READ_CONTACTS),
                        REQUEST_READ_CONTACTS
                    )
                }
                .show()
        } else {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.READ_CONTACTS), REQUEST_READ_CONTACTS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(findViewById(R.id.main), "Permission is Available for Contacts", Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(findViewById(R.id.main), "Permissions are Not Granted", Snackbar.LENGTH_SHORT).show()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}