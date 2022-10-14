package com.sliicy.gematria

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.text.Editable
import android.text.TextWatcher
import android.text.util.Linkify
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.BufferedReader
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var editTextSearch: EditText
    private lateinit var gematriaResult: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var misparSwitch: SwitchCompat
    private val data = ArrayList<PasukModel>()
    private val pesukim = mutableListOf<Pair<Long, String>>()
    private val adapter: PasukAdapter = PasukAdapter(data)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        editTextSearch = findViewById(R.id.editTextSearch)
        recyclerView = findViewById(R.id.recyclerView)
        misparSwitch = findViewById(R.id.misparSwitch)
        gematriaResult = findViewById(R.id.gematriaValue)

        val firstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("firstRun", true)
        if (firstRun) {

            // Show alert:
            val alertBuilder = AlertDialog.Builder(this)
            alertBuilder.setTitle(R.string.privacy_policy)
            alertBuilder.setCancelable(false)
            alertBuilder.setPositiveButton("OK") { dialog, id ->

                // Save the state if user acknowledges privacy policy:
                getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                    .edit()
                    .putBoolean("firstRun", false)
                    .apply()
            }
            alertBuilder.setMessage(R.string.dialog_message)
            val alert: AlertDialog = alertBuilder.create()
            alert.show()
            Linkify.addLinks((alert.findViewById(android.R.id.message) as TextView), Linkify.ALL)
        }

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
                true
            } else {
                false
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(this)

        misparSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            refreshPesukim()
            refreshResults()
        }
        recyclerView.adapter = adapter

        adapter.onItemClick = {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sefaria.org/search?q=" + it.text))
            startActivity(browserIntent)
        }
        recyclerView.setOnLongClickListener { true }

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

    private fun refreshPesukim() {
        pesukim.clear()
        val torah: List<String> = this.resources.openRawResource(R.raw.torah).bufferedReader().use(BufferedReader::readText).lines()
        val neviim: List<String> = this.resources.openRawResource(R.raw.neviim).bufferedReader().use(BufferedReader::readText).lines()
        val kesuvim: List<String> = this.resources.openRawResource(R.raw.kesuvim).bufferedReader().use(BufferedReader::readText).lines()

        for (pasuk in torah) {
            pesukim.add(Pair(getGematria(pasuk, misparSwitch.isChecked), pasuk))
        }
        for (pasuk in neviim) {
            pesukim.add(Pair(getGematria(pasuk, misparSwitch.isChecked), pasuk))
        }
        for (pasuk in kesuvim) {
            pesukim.add(Pair(getGematria(pasuk, misparSwitch.isChecked), pasuk))
        }
    }

    private fun refreshResults() {
        data.clear()
        for (word in searchGematria(editTextSearch.text.toString())) {
            data.add(PasukModel(word))
        }
        recyclerView.adapter = adapter
        if (editTextSearch.text.matches(Regex("^\\d+$"))) {
            gematriaResult.text = buildString {
                append("= ")
                append(editTextSearch.text.toString().toLong())
            }
        } else {
            gematriaResult.text = buildString {
                append("= ")
                append(getGematria(editTextSearch.text.toString(), misparSwitch.isChecked))
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
            sanitizedInput = getGematria(input, misparSwitch.isChecked)
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