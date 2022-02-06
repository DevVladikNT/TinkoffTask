package com.vladiknt.vasilyev

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
    // Текущий кещ, номер поста и категория
    private var cache = ArrayList<Pair<String?, String?>>()
    private var iterator = 0
    private var cacheType = 0

    // Отдельный кеш и номер текущего поста под каждую категорию
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
        // Выбираем категорию random и загружаем первый пост
        findViewById<RadioButton>(R.id.radioRandom).isChecked = true
        load()
    }

    fun swapCache(view: View?) {
        // При смене категории сохраняем текущий кеш
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
        // Загружаем кеш выбранной категории чтобы продолжить с последнего поста
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
        // Если это первый загруженный пост, то кнопка previous ничего не делает
        if (iterator == 0) return

        // Иначе загружаем предыдущий пост из кеша
        try {
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
        } catch (e: Exception) {
            // Если ошибка, то обратно возвращаем номер поста
            iterator++
            gifDescription.clearComposingText()
        }
    }

    fun nextButton(view: View?) {
        // Формируем url для запроса json`а
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
        // Если пост уже был загружен ранее, то достаем ответ api из кеша
        try {
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
        } catch (e: Exception) {
            // Если ошибка, то обратно возвращаем номер поста
            iterator--
            gifDescription.clearComposingText()
            return
        }

        // Если нужен новый пост, то делаем запрос
        val httpAsync = req
            .httpGet()
            .responseString { request, response, result ->
                when (result) {
                    // Если ошибка запроса, то показываем логотип ошибки и выходим из запроса
                    is Result.Failure -> {
                        val ex = result.getException()
                        val error = BitmapFactory.decodeResource(resources, R.drawable.error_logo)
                        gifTab.setImageBitmap(error)
                        gifDescription.clearComposingText()
                        iterator--
                        return@responseString
                    }
                    // Если получили ответ на запрос
                    is Result.Success -> {
                        data = result.get()

                        try {
                            // Парсим json
                            val jsonObj = when (cacheType) {
                                0 -> Klaxon().parse<Note>(data)
                                else -> Klaxon().parse<NoteArray>(data)?.result?.get(0)
                            }

                            // Записываем ответ api в кеш
                            if (iterator == cache.size)
                                cache.add(Pair(jsonObj?.gifURL, jsonObj?.description))

                            // Загружаем гифку
                            Glide.with(this)
                                .asGif()
                                .load(jsonObj?.gifURL)
                                .fitCenter()
                                .placeholder(R.drawable.loading_logo)
                                .into(gifTab)

                            // Ставим описание к гифке
                            gifDescription.text = jsonObj?.description
                        } catch (e: Exception) {
                            // Если ошибка при загрузке гифки, то показываем логотип ошибки и выходим из запроса
                            val error = BitmapFactory.decodeResource(resources, R.drawable.error_logo)
                            gifTab.setImageBitmap(error)
                            gifDescription.clearComposingText()
                        }
                    }
                }
            }
        httpAsync.join()
    }

    // Классы для приведения полученного json к объекту kotlin
    class Note(val description: String, val gifURL: String)
    class NoteArray(val result: List<Note>)
}
