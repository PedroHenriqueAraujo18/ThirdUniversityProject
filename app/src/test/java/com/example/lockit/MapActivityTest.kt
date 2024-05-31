//package com.example.lockit
//
//import android.location.Location
//import com.google.android.gms.maps.model.LatLng
//import org.junit.Assert.assertEquals
//import org.junit.Test
//import org.mockito.Mockito.mock
//import org.mockito.Mockito.`when`
//
//class MapActivityTest {
//
//    @Test
//    fun testLocationUpdates() {
//        // Crie um mock para a Location
//        val mockLocation = mock(Location::class.java)
//
//        // Defina o comportamento do mock
//        `when`(mockLocation.latitude).thenReturn(-22.8179682)
//        `when`(mockLocation.longitude).thenReturn(-47.0698738)
//
//        // Crie uma instância da MapActivity
//        val mapActivity = MapActivity()
//
//        // Inicialize o campo lastKnownLocation na MapActivity
//        mapActivity.lastKnownLocation = mockLocation
//
//        // Chame o método que você deseja testar
//        mapActivity.locationListener.onLocationChanged(mockLocation)
//
//        // Verifique se a última localização conhecida é a esperada
//        assertEquals(LatLng(-22.8179682, -47.0698738), LatLng(mapActivity.lastKnownLocation.latitude, mapActivity.lastKnownLocation.longitude))
//    }
//}
