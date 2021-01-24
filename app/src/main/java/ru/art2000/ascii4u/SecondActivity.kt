package ru.art2000.ascii4u

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.text.Layout
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

class SecondActivity : Activity() {

    private var string2ascii = true
    private var asciiFormat = "[Uu][0-9a-fA-F]{4}"
    private var backslash = '\\'
    private var convertDigits = false
    private var convertLatin = false
    private var keypad = true
    private var pattern: Pattern = Pattern.compile(asciiFormat)
    private lateinit var prefs: SharedPreferences
    private lateinit var menuItem: MenuItem
    private lateinit var inputLayout: Layout
    private lateinit var resultLayout: Layout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        convertLatin = prefs.getBoolean("latin", false)
        convertDigits = prefs.getBoolean("digits", false)
        keypad = prefs.getBoolean("keypad", true)
        string2ascii = prefs.getBoolean("s2a", true)

        if (string2ascii) {
            swap.setText(R.string.native2ascii)
            asciiBtns.visibility = View.GONE
        } else {
            swap.setText(R.string.ascii2native)
            if (keypad)
                asciiBtns.visibility = View.VISIBLE
            else
                asciiBtns.visibility = View.GONE
        }

        input_line_indicator_sv.setPadding(0, input.paddingTop, 0, input.paddingBottom)
        result_line_indicator_sv.setPadding(0, result.paddingTop, 0, result.paddingBottom)
        input_line_indicator.setPadding(result_line_indicator.paddingLeft,
                result_line_indicator.paddingTop, result_line_indicator.paddingRight,
                result_line_indicator.paddingBottom)
        buttonDEL.setOnLongClickListener {
            input.setText("")
            return@setOnLongClickListener true
        }

        result.setOnClickListener {
            hideKeyboard(result)
        }

        result.setOnTouchListener { v, _ ->
            kotlin.run {
                v.requestFocus()
                hideKeyboard(result)
                return@setOnTouchListener false
            }
        }

        input.setOnTouchListener { v, _ ->
            kotlin.run {
                if (!string2ascii) {
                    v.requestFocus()
                    hideKeyboard(input)
                }
                return@setOnTouchListener false
            }
        }

        input.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            kotlin.run {
                input_line_indicator_sv.scrollY = scrollY
                Log.d("uuuu", "scrolled")
            }
        }

        input_line_indicator_sv.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            kotlin.run {
                input.scrollY = scrollY
            }
        }

        result.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            kotlin.run {
                result_line_indicator_sv.scrollY = scrollY
            }
        }

        result_line_indicator_sv.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            kotlin.run {
                result.scrollY = scrollY
            }
        }

        input.setOnClickListener {
            if (!string2ascii)
                hideKeyboard(input)
        }

        swap.setOnClickListener {
            if (string2ascii) {
                input.hint = resources.getString(R.string.input_ascii)
                result.hint = resources.getString(R.string.result_native)
                string2ascii = false
            } else {
                input.hint = resources.getString(R.string.input_native)
                result.hint = resources.getString(R.string.result_ascii)
                string2ascii = true
            }
            if (result.text.toString() != "")
                input.text = result.text
            prefs.edit().putBoolean("s2a", string2ascii).apply()
            if (string2ascii) {
                swap.setText(R.string.native2ascii)
                asciiBtns.visibility = View.GONE
                menuItem.isVisible = false
            } else {
                swap.setText(R.string.ascii2native)
                hideKeyboard(input)
                menuItem.isVisible = true
                if (keypad)
                    asciiBtns.visibility = View.VISIBLE
                else
                    asciiBtns.visibility = View.GONE
            }
        }

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (string2ascii)
                    result.setText(toASCII(input.text.toString()))
                else
                    result.setText(toNormalString(input.text.toString()))
            }


            override fun afterTextChanged(s: Editable?) {
                countLines()
            }
        })

    }

    private fun countLines() {
        prefs.edit().putString("input", input.text.toString()).apply()
        var rCounter = 1
        var iCounter = 1
        val rScanner = Scanner(result.text.toString())
        val iScanner = Scanner(input.text.toString())
        var rline = ""
        var iline = ""
        if (result.text.isNotEmpty())
            rline = rScanner.nextLine()
        if (input.text.isNotEmpty())
            iline = iScanner.nextLine()
        var rAppend: Boolean
        var iAppend: Boolean
        if (result.layout == null) {
            Log.d("WARNING", "EditText layout is null! Trying to load backuped...")
            if (inputLayout == null || resultLayout == null) {
                Log.d("ERROR", "One of EditTexts layout is null! Aborting...")
                return
            }
            val tmpLayout = inputLayout
            inputLayout = resultLayout
            resultLayout = tmpLayout
        } else {
            resultLayout = result.layout
            inputLayout = input.layout
        }
        input_line_indicator.text = ""
        result_line_indicator.text = ""
        val count = if (resultLayout.lineCount > inputLayout.lineCount) resultLayout.lineCount else inputLayout.lineCount
        for (ind in 0 until count) {
            if (ind < resultLayout.lineCount) {
                val rstart = resultLayout.getLineStart(ind)
                val rend = resultLayout.getLineEnd(ind)
                var rVisStr = result.text.substring(rstart, rend)
                if (rVisStr.endsWith("\n")) {
                    rVisStr = rVisStr.substring(0, rVisStr.length - 1)
                }
                if (rline.startsWith(rVisStr)) {
                    if (ind == resultLayout.lineCount - 1)
                        result_line_indicator.append(rCounter.toString())
                    else
                        result_line_indicator.append(rCounter.toString() + "\n")
                    rAppend = true
                    rCounter++
                } else {
                    if (ind != resultLayout.lineCount - 1)
                        result_line_indicator.append("\n")
                    rAppend = false
                }
                if (rScanner.hasNextLine() && rAppend)
                    rline = rScanner.nextLine()
            }
            if (ind < inputLayout.lineCount) {
                val istart = inputLayout.getLineStart(ind)
                val iend = inputLayout.getLineEnd(ind)
                var iVisStr = input.text.substring(istart, iend)
                if (iVisStr.endsWith("\n"))
                    iVisStr = iVisStr.substring(0, iVisStr.length - 1)
                if (iline.startsWith(iVisStr)) {
                    if (ind == inputLayout.lineCount - 1)
                        input_line_indicator.append(iCounter.toString())
                    else
                        input_line_indicator.append(iCounter.toString() + "\n")
                    iAppend = true
                    iCounter++
                } else {
                    if (ind != inputLayout.lineCount - 1)
                        input_line_indicator.append("\n")
                    iAppend = false
                }
                if (iScanner.hasNextLine() && iAppend)
                    iline = iScanner.nextLine()
            }
        }
        rScanner.close()
        iScanner.close()
    }

    private fun toASCII(input: String): String {
        val asciiString = StringBuilder()
        for (c in input) {
            val hexValue = dec2hex(c.toInt())
            if (isSkippedSymbol2(c) or (hexValue == "000a"))
                asciiString.append(c)
            else
                asciiString.append("\\u$hexValue")
        }

        return asciiString.toString()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        for (i in 1 until menu!!.size()) {
            val item = menu.getItem(i)
            if (item.isCheckable) {
                if (item.itemId == R.id.ascii_keypad)
                    menuItem = item
                item.isChecked = when (item.itemId) {
                    R.id.latin -> {
                        convertLatin
                    }
                    R.id.digits -> {
                        convertDigits
                    }
                    R.id.ascii_keypad -> {
                        keypad
                    }
                    else -> false
                }
            }
        }
        val inputTxt = prefs.getString("input", "")
        if (inputTxt != "")
            input.setText(inputTxt)
        menu.getItem(3)?.isVisible = !string2ascii
        return super.onCreateOptionsMenu(menu)
    }

    override fun onMenuItemSelected(featureId: Int, item: MenuItem?): Boolean {
        if (item?.isCheckable!!)
            item.isChecked = !item.isChecked
        when (item.itemId) {
            R.id.latin -> {
                convertLatin = item.isChecked
                prefs.edit().putBoolean("latin", convertLatin).apply()
            }
            R.id.digits -> {
                convertDigits = item.isChecked
                prefs.edit().putBoolean("digits", convertDigits).apply()
            }
            R.id.ascii_keypad -> {
                keypad = item.isChecked
                prefs.edit().putBoolean("keypad", keypad).apply()
                if (keypad)
                    asciiBtns.visibility = View.VISIBLE
                else
                    asciiBtns.visibility = View.GONE
            }
            R.id.clear -> {
                input.setText("")
            }
        }


        if (string2ascii)
            result.setText(toASCII(input.text.toString()))
        else
            result.setText(toNormalString(input.text.toString()))
        countLines()
        return super.onMenuItemSelected(featureId, item)
    }

    private fun isSkippedSymbol2(c: Char): Boolean {
        return (c == ' ') or (!convertLatin and isLatinChar(c)) or (!convertDigits and c.isDigit())
    }

    private fun isSkippedSymbol(c: Char): Boolean {
        return (c == ' ') or (c.isLetterOrDigit() && c != backslash) or (convertLatin and isLatinChar(c))
    }

    private fun toNormalString(asciiString: String): String {
        val normalString = StringBuilder()
        normalString.append("")
        var tmp = asciiString
        while (tmp != "") {
            if (tmp.length > 5) {
                if (isSkippedSymbol(tmp[0]) or (dec2hex(tmp[0].toInt()) == "000a")) {
                    normalString.append(tmp[0])
                    tmp = tmp.substring(1, tmp.length)
                } else {
                    val sub: String = tmp.substring(1, 6)
                    if (tmp.startsWith(backslash)) {
                        val matcher: Matcher = pattern.matcher(sub)
                        if (matcher.find())
                            normalString.append(hex2dex(tmp.substring(2, 6)))
                        else
                            normalString.append("ERROR")
                        tmp = tmp.substring(6, tmp.length)
                    } else {
                        tmp = ""
                        normalString.append("ERROR")
                    }
                }
            } else {
                if (isSkippedSymbol(tmp[0]) or (dec2hex(tmp[0].toInt()) == "000a")) {
                    normalString.append(tmp[0])
                } else {
                    normalString.append("ERROR")
                }
                tmp = tmp.substring(1, tmp.length)
            }
        }
        if (normalString.toString().contains("ERROR")) {
            result.setTextColor(Color.RED)
        } else {
            result.setTextColor(input.currentTextColor)
        }

        return if (normalString.toString().contains("ERROR"))
            "ERROR! "
        else
            normalString.toString()
    }

    private fun dec2hex(dec: Int): String {
        var hex = ""
        var tmp: Int = dec
        while (tmp > 0) {
            hex = hexLetter(tmp.rem(16).toString(), "to") + hex
            tmp = tmp.div(16)
        }
        while (hex.length < 4) {
            hex = "0$hex"
        }

        return hex
    }

    private fun hexLetter(c: String, action: String): String {
        if (action == "to") {
            return when (c.toInt()) {
                10 -> "a"
                11 -> "b"
                12 -> "c"
                13 -> "d"
                14 -> "e"
                15 -> "f"
                else -> c
            }
        } else
            return when (c) {
                "a", "A" -> "10"
                "b", "B" -> "11"
                "c", "C" -> "12"
                "d", "D" -> "13"
                "e", "E" -> "14"
                "f", "F" -> "15"
                else -> c
            }
    }

    private fun hex2dex(hex: String): String {
        var tmp = hex
        if (tmp[0] == '0')
            tmp = tmp.substring(1, tmp.length)
        var dec = 0
        for ((index, value) in tmp.withIndex()) {
            if (value.isDigit() or isLatinChar(value)) {
                val i = hexLetter(value.toString(), "from").toInt()
                if (index != tmp.length - 1)
                    dec = (dec + i) * 16
                else
                    dec += i
            } else {
                return "ERROR!"
            }

        }
        return dec.toChar().toString()
    }

    private fun isLatinChar(c: Char): Boolean {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return alphabet.contains(c) or alphabet.toLowerCase().contains(c)
    }

    fun btnClick(v: View) {
        if (!input.isFocused) {
            input.requestFocus()
            input.setSelection(input.text.length)
        }
        val id = v.id
        val newInp: String
        newInp = when (id) {
            R.id.buttonSp -> {
                " "
            }
            R.id.buttonNL -> {
                "\n"
            }
            else -> {
                findViewById<Button>(id).text.toString()
            }
        }
        val text = input.text
        val pos = input.selectionStart
        var newPos = pos + 1
        if (id == R.id.buttonU)
            newPos = pos + 2
        var p = 0
        var str = newInp
        if (id == R.id.buttonDEL) {
            p = 1
            str = ""
            newPos = pos - 1
            if (text.substring(pos - 1, pos) == "u") {
                p = 2
                newPos = pos - 2
            }
        }
        val newInpTxt = text.substring(0, pos - p) + str + text.substring(pos, text.length)
        input.setText(newInpTxt)
        input.setSelection(newPos)
    }

    private fun hideKeyboard(et: EditText) {
        val imm: InputMethodManager = this.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(et.windowToken, 0)
    }
}