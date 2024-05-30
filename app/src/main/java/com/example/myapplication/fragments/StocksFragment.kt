package com.example.myapplication.fragments

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.views.SharedViewModel
import com.example.myapplication.views.SharedViewModelFactory
import java.io.File
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.example.myapplication.api.IngredientAPI
import com.example.myapplication.config.RetrofitInstance
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.FileInputStream
import java.io.FileOutputStream


class StocksFragment : Fragment() {
    private lateinit var sharedViewModel: SharedViewModel
    private val FILE_PICKER_REQUEST = 1
    private lateinit var ingredientAPI: IngredientAPI
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stocks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = SharedViewModelFactory()
        sharedViewModel = ViewModelProvider(requireActivity(), factory).get(SharedViewModel::class.java)

        sharedViewModel.refreshProductsTrigger.observe(viewLifecycleOwner) { shouldRefresh ->
            if (shouldRefresh) {
                fetchStocks()
            }
        }
        sharedViewModel.onBackPressed.observe(viewLifecycleOwner) {
            val searchBar = view.findViewById<EditText>(R.id.searchBar)
            if (searchBar.isFocused) {
                searchBar.clearFocus()
                val inputMethodManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                inputMethodManager?.hideSoftInputFromWindow(searchBar.windowToken, 0)
            }
        }

        val uploadButton = view.findViewById<Button>(R.id.uploadButton)
        uploadButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            startActivityForResult(intent, FILE_PICKER_REQUEST)
        }
    }

    private fun fetchStocks() {
//        TODO("Not yet implemented")
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_PICKER_REQUEST && resultCode == Activity.RESULT_OK) {
            val fileUri = data?.data
            if (fileUri != null) {
                uploadFile(fileUri)
            }
        }
    }

    private fun uploadFile(fileUri: Uri) {
        val parcelFileDescriptor = requireContext().contentResolver.openFileDescriptor(fileUri, "r", null)
        val inputStream = FileInputStream(parcelFileDescriptor?.fileDescriptor)
        val file = File(requireContext().cacheDir, requireContext().getFileName(fileUri))
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)

        val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        ingredientAPI = RetrofitInstance.getInstance(requireContext()).create(IngredientAPI::class.java)

        val call = ingredientAPI.uploadIngredientsFile(body)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "File uploaded successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "File upload failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(context, "File upload failed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun Context.getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                if (cut != null) {
                    result = result?.substring(cut + 1)
                }
            }
        }
        return result!!
    }
}