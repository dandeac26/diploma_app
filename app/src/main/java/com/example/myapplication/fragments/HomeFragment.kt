package com.example.myapplication.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider

import com.example.myapplication.R
import com.example.myapplication.views.SharedViewModel
import com.example.myapplication.views.SharedViewModelFactory

class HomeFragment : Fragment() {
    private lateinit var sharedViewModel: SharedViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = SharedViewModelFactory()
        sharedViewModel = ViewModelProvider(requireActivity(), factory).get(SharedViewModel::class.java)

        sharedViewModel.refreshProductsTrigger.observe(viewLifecycleOwner) { shouldRefresh ->
            if (shouldRefresh) {
                fetchHome()
            }
        }

        sharedViewModel.refreshHomeTrigger.observe(viewLifecycleOwner, Observer {
//            recycleview.adapter = homeAdapter
        })

    }

    private fun fetchHome() {
//        TODO("Not yet implemented")
    }
}