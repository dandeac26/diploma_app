package com.example.myapplication.dialog

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.myapplication.R

class ImagePreviewDialog : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_image_preview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageUrl = arguments?.getString("imageUrl")
        Log.d("ImagePreviewDialog", "Image URL: $imageUrl")
        val imageView = view.findViewById<ImageView>(R.id.previewImage)

        if (imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(R.drawable.placeholder_50)
                .into(imageView)

        }else{
            Glide.with(this)
                .load(imageUrl)
                .into(imageView)
        }
    }

    companion object {
        fun newInstance(imageUrl: String): ImagePreviewDialog {
            val fragment = ImagePreviewDialog()
            val args = Bundle()
            args.putString("imageUrl", imageUrl)
            fragment.arguments = args
            return fragment
        }
    }
}