package com.sliicy.gematria

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.text.Editable
import android.text.TextWatcher
import android.text.util.Linkify
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import java.io.BufferedReader
import java.util.*


class MainActivity : AppCompatActivity() {

    private val data = ArrayList<PasukModel>()
    private val pesukim = mutableListOf<Pair<Long, String>>()
    private val adapter: PasukAdapter = PasukAdapter(data)

    private lateinit var editTextSearch: EditText
    private lateinit var gematriaResult: TextView
    private lateinit var recyclerView: RecyclerView

    private var misparGadolChecked: Boolean = false
    private var showWordsChecked: Boolean = true
    private var showDaveningChecked: Boolean = true

    private lateinit var torah: List<String>
    private lateinit var neviim: List<String>
    private lateinit var kesuvim: List<String>
    private lateinit var allWords: List<String>
    private lateinit var rabbis: List<String>
    private lateinit var hebrewNames: List<String>
    private lateinit var commonWords: List<String>
    private lateinit var poskim: List<String>
    private lateinit var poskimSefarim: List<String>
    private lateinit var davening: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        editTextSearch = findViewById(R.id.editTextSearch)
        recyclerView = findViewById(R.id.recyclerView)
        gematriaResult = findViewById(R.id.gematriaValue)

        //val firstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("firstRun", true)
        //if (firstRun) {
        //    // Show alert:
        //    val alertBuilder = AlertDialog.Builder(this)
        //    alertBuilder.setTitle(R.string.privacy_policy)
        //    alertBuilder.setCancelable(false)
        //    alertBuilder.setPositiveButton("OK") { dialog, id ->
        //        // Save the state if user acknowledges privacy policy:
        //        getSharedPreferences("PREFERENCE", MODE_PRIVATE)
        //            .edit()
        //            .putBoolean("firstRun", false)
        //            .apply()
        //    }
        //    alertBuilder.setMessage(R.string.dialog_message)
        //    val alert: AlertDialog = alertBuilder.create()
        //    alert.show()
        //    Linkify.addLinks((alert.findViewById(android.R.id.message) as TextView), Linkify.WEB_URLS)
        //}

        // Initialize all Pesukim:
        torah = this.resources.openRawResource(R.raw.torah).bufferedReader().use(BufferedReader::readText).lines()
        neviim = this.resources.openRawResource(R.raw.neviim).bufferedReader().use(BufferedReader::readText).lines()
        kesuvim = this.resources.openRawResource(R.raw.kesuvim).bufferedReader().use(BufferedReader::readText).lines()
        allWords = this.resources.openRawResource(R.raw.all_words).bufferedReader().use(BufferedReader::readText).lines()
        rabbis = this.resources.openRawResource(R.raw.rabbis).bufferedReader().use(BufferedReader::readText).lines()
        hebrewNames = this.resources.openRawResource(R.raw.hebrew_names).bufferedReader().use(BufferedReader::readText).lines()
        commonWords = this.resources.openRawResource(R.raw.common_words).bufferedReader().use(BufferedReader::readText).lines()
        poskim = this.resources.openRawResource(R.raw.poskim).bufferedReader().use(BufferedReader::readText).lines()
        poskimSefarim = this.resources.openRawResource(R.raw.poskim_sefarim).bufferedReader().use(BufferedReader::readText).lines()
        davening = this.resources.openRawResource(R.raw.davening).bufferedReader().use(BufferedReader::readText).lines()

        // Handle keyboard input:
        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                refreshResults()
            }
        })

        // Handle pressing SEARCH:
        editTextSearch.setOnEditorActionListener { v, actionId, event ->
            if(actionId == EditorInfo.IME_ACTION_SEARCH){
                refreshResults()
                hideKeyboard()
                true
            } else {
                false
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(this)

        recyclerView.adapter = adapter

        adapter.onItemClick = {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.sefaria.org/search?q=" + it.text))
            startActivity(browserIntent)
        }
        recyclerView.setOnLongClickListener { true }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val hasStarted = newState == SCROLL_STATE_DRAGGING
                val hasEnded = newState == SCROLL_STATE_IDLE
                if (hasStarted && data.isNotEmpty()) {
                    hideKeyboard()
                }
                if (!recyclerView.canScrollVertically(-1) && data.isNotEmpty() && hasEnded) {
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(editTextSearch, 0)
                }
            }
        })

        adapter.onItemLongClick = {
            val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("text", it.text)
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(this, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
        }

        refreshPesukim()
        editTextSearch.requestFocus()

        // Change language to Hebrew:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            editTextSearch.imeHintLocales = LocaleList(Locale("he", "IL"))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        val settings = getSharedPreferences("settings", 0)
        val isCheckedMispar = settings.getBoolean("misparGadol", false)
        val isCheckedShowWords = settings.getBoolean("showWords", true)
        val isCheckedDavening = settings.getBoolean("showDavening", true)
        menu.findItem(R.id.menu_mispar_gadol).isChecked = isCheckedMispar
        menu.findItem(R.id.menu_show_words).isChecked = isCheckedShowWords
        menu.findItem(R.id.menu_davening).isChecked = isCheckedDavening
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        item.isChecked = !item.isChecked
        val settings = getSharedPreferences("settings", 0)
        val editor = settings.edit()
        when (item.itemId) {
            R.id.menu_mispar_gadol -> {
                misparGadolChecked = item.isChecked
                editor.putBoolean("misparGadol", item.isChecked)
            }
            R.id.menu_show_words -> {
                showWordsChecked = item.isChecked
                editor.putBoolean("showWords", item.isChecked)
            }
            R.id.menu_davening -> {
                showDaveningChecked = item.isChecked
                editor.putBoolean("showDavening", item.isChecked)
            }
        }
        editor.apply()
        refreshPesukim()
        refreshResults()
        return super.onOptionsItemSelected(item)
    }

    private fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }

    private fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun refreshPesukim() {
        pesukim.clear()
        for (pasuk in torah) {
            pesukim.add(Pair(getGematria(pasuk, misparGadolChecked), pasuk))
        }
        for (pasuk in neviim) {
            pesukim.add(Pair(getGematria(pasuk, misparGadolChecked), pasuk))
        }
        for (pasuk in kesuvim) {
            pesukim.add(Pair(getGematria(pasuk, misparGadolChecked), pasuk))
        }
        if (showWordsChecked) {
            for (word in allWords) {
                pesukim.add(Pair(getGematria(word, misparGadolChecked), word))
            }
        }
        for (name in rabbis) {
            pesukim.add(Pair(getGematria(name, misparGadolChecked), name))
        }
        for (name in hebrewNames) {
            pesukim.add(Pair(getGematria(name, misparGadolChecked), name))
        }
        for (word in commonWords) {
            pesukim.add(Pair(getGematria(word, misparGadolChecked), word))
        }
        for (name in poskim) {
            pesukim.add(Pair(getGematria(name, misparGadolChecked), name))
        }
        for (sefer in poskimSefarim) {
            pesukim.add(Pair(getGematria(sefer, misparGadolChecked), sefer))
        }
        if (showDaveningChecked) {
            for (line in davening) {
                pesukim.add(Pair(getGematria(line, misparGadolChecked), line))
            }
        }
    }

    private fun refreshResults() {
        data.clear()
        for (word in searchGematria(editTextSearch.text.toString())) {
            data.add(PasukModel(word))
        }
        recyclerView.adapter = adapter
        if (!editTextSearch.text.matches(Regex("^\\d+$"))) {
            gematriaResult.text = buildString {
                append("= ")
                append(getGematria(editTextSearch.text.toString(), misparGadolChecked))
            }
        }
        if (gematriaResult.text == "= 0") {
            gematriaResult.text = ""
        }
    }

    private fun searchGematria(input: String): List<String> {
        val output = mutableListOf<String>()
        var sanitizedInput = 0L
        if (input.isNotEmpty() && input.matches(Regex("^\\d+$"))) {
            sanitizedInput = input.toLong()
        } else if (input.isNotEmpty()) {
            sanitizedInput = getGematria(input, misparGadolChecked)
        }
        for (pasuk in pesukim) {
            if (sanitizedInput == pasuk.first) {
                output.add(pasuk.second)
            }
        }
        return output.distinct()
    }

    private fun getGematria(input: String, misparGadol : Boolean): Long {
        var value = 0L
        for (c in input) {
            if (c == 'א') value += 1
            if (c == 'ב') value += 2
            if (c == 'ג') value += 3
            if (c == 'ד') value += 4
            if (c == 'ה') value += 5
            if (c == 'ו') value += 6
            if (c == 'ז') value += 7
            if (c == 'ח') value += 8
            if (c == 'ט') value += 9
            if (c == 'י') value += 10
            if (c == 'כ') value += 20
            if (c == 'ך' && misparGadol) value += 500
            if (c == 'ך' && !misparGadol) value += 20
            if (c == 'ל') value += 30
            if (c == 'מ') value += 40
            if (c == 'ם' && misparGadol) value += 600
            if (c == 'ם' && !misparGadol) value += 40
            if (c == 'נ') value += 50
            if (c == 'ן' && misparGadol) value += 700
            if (c == 'ן' && !misparGadol) value += 50
            if (c == 'ס') value += 60
            if (c == 'ע') value += 70
            if (c == 'פ') value += 80
            if (c == 'ף' && misparGadol) value += 800
            if (c == 'ף' && !misparGadol) value += 80
            if (c == 'צ') value += 90
            if (c == 'ץ' && misparGadol) value += 900
            if (c == 'ץ' && !misparGadol) value += 90
            if (c == 'ק') value += 100
            if (c == 'ר') value += 200
            if (c == 'ש') value += 300
            if (c == 'ת') value += 400
        }
        return value
    }
}