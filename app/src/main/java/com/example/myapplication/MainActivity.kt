package com.example.myapplication

import android.annotation.SuppressLint
import android.content.IntentFilter
import android.net.ConnectivityManager
import com.example.myapplication.fragments.HomeFragment
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.config.NetworkChangeReceiver
import com.example.myapplication.config.RetrofitInstance
import com.example.myapplication.fragments.ClientsFragment.ClientSelectionListener
import com.example.myapplication.fragments.ClientsFragment
import com.example.myapplication.fragments.CreateOrderFragment
import com.example.myapplication.fragments.OrdersFragment
import com.example.myapplication.fragments.ProductDetailsFragment
import com.example.myapplication.fragments.ProductsFragment
import com.example.myapplication.fragments.StocksFragment
import com.example.myapplication.fragments.SettingsFragment
import com.example.myapplication.views.SharedViewModel
import com.example.myapplication.views.SharedViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.LinkedList


class MainActivity : AppCompatActivity() {

    private var currentFragment: Fragment? = null
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var networkChangeReceiver: NetworkChangeReceiver
    private val fragmentToMenuItem = mapOf(
        HomeFragment::class.java to R.id.nav_home,
        ProductsFragment::class.java to R.id.nav_products,
        OrdersFragment::class.java to R.id.nav_orders,
        StocksFragment::class.java to R.id.nav_stocks,
        ClientsFragment::class.java to R.id.nav_clients
    )

    private val fragmentHistory = LinkedList<Fragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val factory = SharedViewModelFactory()
        sharedViewModel = ViewModelProvider(this, factory).get(SharedViewModel::class.java)

        networkChangeReceiver = NetworkChangeReceiver {
            RetrofitInstance.getInstance(applicationContext, 8080)
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

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
//                            // Create a new instance of CreateOrderFragment
//                            val createOrderFragment = CreateOrderFragment()
//                            (activity as MainActivity).switchFragment(createOrderFragment)
                        }
                    })
                }
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

    fun addFragmentToHistory(fragment: Fragment) {
        fragmentHistory.add(fragment)
    }
    fun updateTopNavTitle(title: String) {
        supportActionBar?.title = title
    }
//    private var removedLast = false
    fun switchFragment(fragment: Fragment) {
        if (fragmentHistory.isEmpty() || fragment::class.java != fragmentHistory.last::class.java) {
            // If the fragment is not the current fragment, add it to the history
            fragmentHistory.add(fragment)
        }

        val fragmentTag = fragment::class.java.simpleName

        val transaction = supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, 0, 0, 0)

        // Hide the current fragment
        currentFragment?.let { transaction.hide(it)
        it.userVisibleHint = false
        }

        // Try to find the fragment in the FragmentManager
        var newFragment = supportFragmentManager.findFragmentByTag(fragmentTag)

        if (newFragment != null && newFragment is ProductDetailsFragment) {
            // If the fragment is a ProductDetailsFragment, remove it
            transaction.remove(newFragment)
            newFragment = null
        }

        if (newFragment == null) {
            // If not found, add it
            transaction.add(R.id.fragment_container, fragment, fragmentTag)

            newFragment = fragment
        } else {
            // If found, show it
            transaction.show(newFragment)
            newFragment.userVisibleHint = true
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

            // Remove the current fragment from the history

            fragmentHistory.removeLast()

            if(fragmentHistory.last is ProductDetailsFragment) {
                fragmentHistory.removeLast()
            }
            // Get the previous fragment
            val previousFragment = fragmentHistory.last

            switchFragment(previousFragment)



            val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
            val menuItemId = fragmentToMenuItem[previousFragment::class.java]
            if (menuItemId != null) {
                bottomNavigation.selectedItemId = menuItemId
            }

        } else {
            // If there's only one fragment in the history, let the system handle the back press
            super.onBackPressed()
        }
    }
}