package com.vladiknt.vasilyev

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import com.beust.klaxon.Klaxon
import com.bumptech.glide.Glide
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import java.lang.Exception


class MainActivity : AppCompatActivity() {
    private var req = "https://developerslife.ru/random?json=true"
    private var data = ""
    private lateinit var gifTab: ImageView
    private lateinit var gifDescription: TextView
    private var cache = ArrayList<Pair<String?, String?>>()
    private var iterator = 0
    private var cacheType = 0

    private var cacheRandom = ArrayList<Pair<String?, String?>>()
    private var iteratorRandom = 0
    private var cacheLatest = ArrayList<Pair<String?, String?>>()
    private var iteratorLatest = 0
    private var cacheTop = ArrayList<Pair<String?, String?>>()
    private var iteratorTop = 0
    private var cacheHot = ArrayList<Pair<String?, String?>>()
    private var iteratorHot = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gifTab = findViewById(R.id.loadingGif)
        gifDescription = findViewById(R.id.description)
        findViewById<RadioButton>(R.id.radioRandom).isChecked = true
        load()
    }

    fun swapCache(view: View?) {
        when (cacheType) {
            0 -> {
                cacheRandom = cache
                iteratorRandom = iterator
            }
            1 -> {
                cacheLatest = cache
                iteratorLatest = iterator
            }
            2 -> {
                cacheTop = cache
                iteratorTop = iterator
            }
            3 -> {
                cacheHot = cache
                iteratorHot = iterator
            }
        }
        when (view?.id) {
            R.id.radioRandom -> {
                cache = cacheRandom
                iterator = iteratorRandom
                cacheType = 0
                req = "https://developerslife.ru/random?json=true"
            }
            R.id.radioLatest -> {
                cache = cacheLatest
                iterator = iteratorLatest
                cacheType = 1
                req = "https://developerslife.ru/latest/$iterator?json=true"
            }
            R.id.radioTop -> {
                cache = cacheTop
                iterator = iteratorTop
                cacheType = 2
                req = "https://developerslife.ru/top/$iterator?json=true"
            }
            R.id.radioHot -> {
                cache = cacheHot
                iterator = iteratorHot
                cacheType = 3
                req = "https://developerslife.ru/hot/$iterator?json=true"
            }
        }
        load()
    }

    fun previousButton(view: View?) {
        if (iterator == 0) return

        iterator--
        val previousGif = cache[iterator]
        Glide.with(this)
            .asGif()
            .load(previousGif.first)
            .fitCenter()
            .placeholder(R.drawable.loading_logo)
            .error(R.drawable.error_logo)
            .into(gifTab)
        gifDescription.text = previousGif.second
    }

    fun nextButton(view: View?) {
        iterator++
        req = when (cacheType) {
            0 -> "https://developerslife.ru/random?json=true"
            1 -> "https://developerslife.ru/latest/$iterator?json=true"
            2 -> "https://developerslife.ru/top/$iterator?json=true"
            3 -> "https://developerslife.ru/hot/$iterator?json=true"
            else -> ""
        }

        load()
    }

    private fun load() {
        if (iterator < cache.size) {
            val nextGif = cache[iterator]
            Glide.with(this)
                .asGif()
                .load(nextGif.first)
                .fitCenter()
                .placeholder(R.drawable.loading_logo)
                .error(R.drawable.error_logo)
                .into(gifTab)
            gifDescription.text = nextGif.second
            return
        }

        val httpAsync = req
            .httpGet()
            .responseString { request, response, result ->
                when (result) {
                    is Result.Failure -> {
                        val ex = result.getException()
                        Toast.makeText(this, ex.message, Toast.LENGTH_SHORT).show()
                    }
                    is Result.Success -> {
                        data = result.get()

                        try {
                            val jsonObj = when (cacheType) {
                                0 -> Klaxon().parse<Note>(data)
                                else -> Klaxon().parse<NoteArray>(data)?.result?.get(0)
                            }

                            if (iterator == cache.size)
                                cache.add(Pair(jsonObj?.gifURL, jsonObj?.description))

                            Glide.with(this)
                                .asGif()
                                .load(jsonObj?.gifURL)
                                .fitCenter()
                                .placeholder(R.drawable.loading_logo)
                                .error(R.drawable.error_logo)
                                .into(gifTab)

                            gifDescription.text = jsonObj?.description
                        } catch (e: Exception) {
                            Toast.makeText(this, "Error!\n${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        httpAsync.join()
    }

    class Note(val description: String, val gifURL: String)
    class NoteArray(val result: List<Note>)
}
