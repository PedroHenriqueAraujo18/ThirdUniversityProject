package com.example.lockit

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.example.lockit.databinding.CustomMarkerInfoBinding

class MarkerInfoAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter {

    override fun getInfoWindow(marker: Marker): View? = null

    override fun getInfoContents(marker: Marker): View? {

        val place = marker.tag as? MapActivity.Place ?: return null

        val binding = CustomMarkerInfoBinding.inflate(LayoutInflater.from(context))

        binding.txtTitle.text = place.name
        binding.txtDescription.text = place.description
        binding.txtAddress.text = place.address

        return binding.root
    }
}
