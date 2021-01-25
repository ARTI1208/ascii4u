package ru.art2000.ascii4u

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.Layout
import android.text.Selection
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.preference.PreferenceManager
import ru.art2000.ascii4u.databinding.ActivityMainBinding
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

class MainActivity : Activity() {

    private val asciiFormat = "[Uu][0-9a-fA-F]{4}"
    private val backslash = '\\'
    private var convertDigits = false
    private var convertLatin = false
    private var keypad = true
    private var string2ascii = true
    private val pattern: Pattern = Pattern.compile(asciiFormat)
    private val prefs: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private lateinit var menuItemUseKeypad: MenuItem

    private val viewBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    companion object {

        private const val KEY_TRANSLATE_LATIN = "latin"
        private const val KEY_TRANSLATE_DIGITS = "digits"
        private const val KEY_USE_KEYPAD = "keypad"
        private const val KEY_MODE = "s2a"
        private const val KEY_INPUT_VALUE = "input"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        convertLatin = prefs.getBoolean(KEY_TRANSLATE_LATIN, false)
        convertDigits = prefs.getBoolean(KEY_TRANSLATE_DIGITS, false)
        keypad = prefs.getBoolean(KEY_USE_KEYPAD, true)
        string2ascii = prefs.getBoolean(KEY_MODE, true)

        if (string2ascii) {
            viewBinding.swap.setText(R.string.native2ascii)
            viewBinding.asciiBtns.visibility = View.GONE
        } else {
            viewBinding.swap.setText(R.string.ascii2native)
            viewBinding.asciiBtns.visibility = if (keypad) View.VISIBLE else View.GONE
        }

        viewBinding.inputLineIndicatorSv.setPadding(0, viewBinding.input.paddingTop, 0, viewBinding.input.paddingBottom)
        viewBinding.resultLineIndicatorSv.setPadding(0, viewBinding.result.paddingTop, 0, viewBinding.result.paddingBottom)

        viewBinding.buttonDEL.setOnLongClickListener {
            viewBinding.input.setText("")
            return@setOnLongClickListener true
        }

        viewBinding.result.setOnClickListener {
            hideKeyboard(viewBinding.result)
        }

        viewBinding.result.setOnClickListener { v ->
            v.requestFocus()
            hideKeyboard(viewBinding.result)
        }

        viewBinding.input.setOnClickListener { v ->
            if (!string2ascii || !keypad) {
                v.requestFocus()
                hideKeyboard(viewBinding.input)
            }
        }

        viewBinding.input.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            viewBinding.inputLineIndicatorSv.scrollY = scrollY
        }

        viewBinding.inputLineIndicatorSv.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            viewBinding.input.scrollY = scrollY
        }

        viewBinding.result.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            viewBinding.resultLineIndicatorSv.scrollY = scrollY
        }

        viewBinding.resultLineIndicatorSv.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            viewBinding.result.scrollY = scrollY
        }

        viewBinding.input.setOnClickListener {
            if (!string2ascii)
                hideKeyboard(viewBinding.input)
        }

        viewBinding.swap.setOnClickListener {
            if (string2ascii) {
                viewBinding.input.hint = resources.getString(R.string.input_ascii)
                viewBinding.result.hint = resources.getString(R.string.result_native)
                string2ascii = false
            } else {
                viewBinding.input.hint = resources.getString(R.string.input_native)
                viewBinding.result.hint = resources.getString(R.string.result_ascii)
                string2ascii = true
            }
            if (viewBinding.result.text.toString() != "")
                viewBinding.input.text = viewBinding.result.text
            prefs.edit().putBoolean(KEY_MODE, string2ascii).apply()
            if (string2ascii) {
                viewBinding.swap.setText(R.string.native2ascii)
                viewBinding.asciiBtns.visibility = View.GONE
                menuItemUseKeypad.isVisible = false
            } else {
                viewBinding.swap.setText(R.string.ascii2native)
                hideKeyboard(viewBinding.input)
                menuItemUseKeypad.isVisible = true
                viewBinding.asciiBtns.visibility = if (keypad) View.VISIBLE else View.GONE
            }
            viewBinding.input.maxHeight = inputMaxHeight()
        }

        viewBinding.result.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            // Not the vest way but does the trick
            if (left == oldLeft && top == oldTop && right == oldRight && bottom == oldBottom) {
                return@addOnLayoutChangeListener
            }

            updateLinesCountAndScroll()
        }

        viewBinding.input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val resultText = if (string2ascii)
                    toASCII(viewBinding.input.text.toString())
                else
                    toNormalString(viewBinding.input.text.toString())

                viewBinding.result.setText(resultText)
            }

            override fun afterTextChanged(s: Editable?) {
                prefs.edit().putString(KEY_INPUT_VALUE, s?.toString() ?: "").apply()
            }
        })
    }

    private fun updateLinesCountAndScroll() {
        countLines()

        val currentLineHeight = getCurLineNum() * viewBinding.result.lineHeight

        if (currentLineHeight != 0) {
            viewBinding.result.scrollY = currentLineHeight
            viewBinding.resultLineIndicatorSv.scrollY = currentLineHeight
        }
    }

    private fun countLines() {
        var rCounter = 1
        var iCounter = 1
        var rAppend = true
        var iAppend = true

        viewBinding.inputLineIndicator.text = ""
        viewBinding.resultLineIndicator.text = ""
        val count = if (viewBinding.result.layout.lineCount > viewBinding.input.layout.lineCount) viewBinding.result.layout.lineCount else viewBinding.input.layout.lineCount
        for (ind in 0 until count) {
            if (ind < viewBinding.result.layout.lineCount) {
                val rStart = viewBinding.result.layout.getLineStart(ind)
                val rEnd = viewBinding.result.layout.getLineEnd(ind)
                val rVisStr = viewBinding.result.text.substring(rStart, rEnd)
                if (rAppend) {
                    viewBinding.resultLineIndicator.append(rCounter.toString() + "\n")
                    rCounter++
                } else
                    viewBinding.resultLineIndicator.append("\n")
                rAppend = rVisStr.endsWith("\n")
            }
            if (ind < viewBinding.input.layout.lineCount) {
                val iVisStr = getVisLine(ind, viewBinding.input.layout)
                if (iAppend) {
                    viewBinding.inputLineIndicator.append(" $iCounter\n")
                    iCounter++
                } else
                    viewBinding.inputLineIndicator.append("\n")
                iAppend = iVisStr.endsWith("\n")
            }
        }

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
                    menuItemUseKeypad = item
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
        val inputTxt = prefs.getString(KEY_INPUT_VALUE, "")
        if (inputTxt != "")
            viewBinding.input.setText(inputTxt)
        menuItemUseKeypad.isVisible = !string2ascii
        viewBinding.input.maxHeight = inputMaxHeight()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
        if (item.isCheckable)
            item.isChecked = !item.isChecked
        when (item.itemId) {
            R.id.latin -> {
                convertLatin = item.isChecked
                prefs.edit().putBoolean(KEY_TRANSLATE_LATIN, convertLatin).apply()
            }
            R.id.digits -> {
                convertDigits = item.isChecked
                prefs.edit().putBoolean(KEY_TRANSLATE_DIGITS, convertDigits).apply()
            }
            R.id.ascii_keypad -> {
                keypad = item.isChecked
                prefs.edit().putBoolean(KEY_USE_KEYPAD, keypad).apply()
                if (keypad)
                    viewBinding.asciiBtns.visibility = View.VISIBLE
                else
                    viewBinding.asciiBtns.visibility = View.GONE
                viewBinding.input.maxHeight = inputMaxHeight()
            }
            R.id.clear -> {
                viewBinding.input.setText("")
            }
        }

        val resultText = if (string2ascii)
            toASCII(viewBinding.input.text.toString())
        else
            toNormalString(viewBinding.input.text.toString())

        viewBinding.result.setText(resultText)

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
            viewBinding.result.setTextColor(Color.RED)
        } else {
            viewBinding.result.setTextColor(viewBinding.input.currentTextColor)
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
        return alphabet.contains(c) or alphabet.toLowerCase(Locale.ROOT).contains(c)
    }

    fun btnClick(v: View) {
        if (!viewBinding.input.isFocused) {
            viewBinding.input.requestFocus()
            viewBinding.input.setSelection(viewBinding.input.text.length)
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
        val text = viewBinding.input.text
        val pos = viewBinding.input.selectionStart
        var newPos = pos + 1
        if (id == R.id.buttonU)
            newPos = pos + 2
        var p = 0
        var str = newInp
        if (id == R.id.buttonDEL) {
            if (text.isEmpty())
                return
            p = 1
            str = ""
            newPos = pos - 1
            if (text.substring(pos - 1, pos) == "u") {
                p = 2
                newPos = pos - 2
            }
        }
        val newInpTxt = text.substring(0, pos - p) + str + text.substring(pos, text.length)
        viewBinding.input.setText(newInpTxt)
        viewBinding.input.setSelection(newPos)
    }

    private fun hideKeyboard(et: EditText) {
        val imm: InputMethodManager = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(et.windowToken, 0)
    }

    private fun inputMaxHeight(): Int {
        var newHeight = (viewBinding.mainLayout.height - viewBinding.swap.height) / 2
        if (viewBinding.asciiBtns.visibility == View.VISIBLE)
            newHeight = (viewBinding.mainLayout.height - viewBinding.swap.height - viewBinding.asciiBtns.height) / 2
        return newHeight
    }

    private fun getCurLineNum(): Int {
        val selStart = Selection.getSelectionStart(viewBinding.input.text)
        return if (selStart != -1) viewBinding.input.layout.getLineForOffset(selStart) else -1
    }

    private fun getVisLine(i: Int, etLayout: Layout): String {
        val start = etLayout.getLineStart(i)
        val end = etLayout.getLineEnd(i)
        return etLayout.text.substring(start, end)
    }
}