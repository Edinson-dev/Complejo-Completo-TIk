package com.example.tikdownloader

import com.example.tikdownloader.extractor.VideoExtractor
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class VideoExtractorTest {

    @Test
    fun testTikTokExtractionHD() = runBlocking {
        // Usar un link real de TikTok para probar (puedes cambiarlo si expira)
        val url = "https://www.tiktok.com/@tiktok/video/7304192131908750623"
        val result = VideoExtractor.extract(url, highQuality = true)
        
        assertNotNull("La extracción falló", result)
        assertTrue("La URL de descarga no es válida", result?.downloadUrl?.startsWith("http") == true)
        assertEquals("TikTok", result?.source)
        println("HD Download URL: ${result?.downloadUrl}")
    }

    @Test
    fun testTikTokExtractionSD() = runBlocking {
        val url = "https://www.tiktok.com/@tiktok/video/7304192131908750623"
        val result = VideoExtractor.extract(url, highQuality = false)
        
        assertNotNull("La extracción falló", result)
        assertTrue("La URL de descarga no es válida", result?.downloadUrl?.startsWith("http") == true)
        println("SD Download URL: ${result?.downloadUrl}")
    }
}
