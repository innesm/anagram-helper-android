package innes.anagramhelper

import android.content.ClipData
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.TEXT_ALIGNMENT_CENTER
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModel
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*
import kotlin.collections.ArrayList
import kotlin.random.Random

data class AnagramLetter(val letter: Char, var assignedIndex: Int = -1)

class AnagramViewModel : ViewModel() {
    val anagramLetters = ArrayList<AnagramLetter>()
}

class MainActivity : AppCompatActivity() {

    private lateinit var handwriting: Typeface
    private val circleLetters = ArrayList<TextView>()
    private val topLetters = ArrayList<TextView>()
    private val topLetterDragTargets = ArrayList<TextView>()
    private val topLetterLayouts = ArrayList<LinearLayout>()
    private val words = ArrayList<String>()

    private lateinit var model : AnagramViewModel

    private fun createTextView(text: String): TextView {
        val newWidget = TextView(this)
        newWidget.setTextAppearance(R.style.TextAppearance_AppCompat_Large)
        newWidget.textSize = 30.0f
        val layoutParams =
            ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        newWidget.layoutParams = layoutParams
        newWidget.id = View.generateViewId()
        newWidget.text = text
        newWidget.tag = "anagram_letter"
        newWidget.textAlignment = TEXT_ALIGNMENT_CENTER
        newWidget.typeface = handwriting
        return newWidget
    }

    private fun buildUI() {
        val parentLayout = findViewById<View>(R.id.parentLayout) as ConstraintLayout
        val topLettersLayout = findViewById<View>(R.id.topLettersLayout) as LinearLayout
        val txtCentred = findViewById<View>(R.id.txtCentred) as TextView
        val txtCandidateCount = findViewById<View>(R.id.txtCandidateCount) as TextView

        val candidateCount = findCandidateWords()
        txtCandidateCount.text = candidateCount.toString()

        val unassignedChars =
            model.anagramLetters.asIterable().filter { it.assignedIndex == -1 }.sortedBy { Random.Default.nextDouble() }
                .toTypedArray()

        // remove existing
        circleLetters.forEach { child ->
            parentLayout.removeView(child)
        }

        topLetterLayouts.forEach { child ->
            topLettersLayout.removeView(child)
        }

        circleLetters.clear()
        topLetterLayouts.clear()
        topLetters.clear()
        topLetterDragTargets.clear()

        var angle = 90.0f
        val delta = 360.0f / unassignedChars.size
        for (c in unassignedChars) {

            // add circle letter
            val circleLetter = createTextView(c.letter.toString())
            circleLetter.ConstraintLayoutParams.circleAngle = angle
            circleLetter.ConstraintLayoutParams.circleConstraint = txtCentred.id
            circleLetter.ConstraintLayoutParams.circleRadius = 300
            circleLetter.tag = c
            circleLetter.scaleX = 2.0f
            circleLetter.scaleY = 2.0f

            angle += delta

            parentLayout.addView(circleLetter)
            circleLetters.add(circleLetter)

            circleLetter.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val data = ClipData("blah", arrayOf("text/plain"), ClipData.Item("blah"))
                    v.startDragAndDrop(data, View.DragShadowBuilder(v), v, 0)
                    v.visibility = View.INVISIBLE
                    for (topLetterDragTarget in topLetterDragTargets) {
                        topLetterDragTarget.visibility = View.VISIBLE
                    }
                }
                true
            }

            circleLetter.setOnLongClickListener {
                val data = ClipData("blah", arrayOf("text/plain"), ClipData.Item("blah"))
                it.startDragAndDrop(data, View.DragShadowBuilder(it), it, 0)
                it.visibility = View.INVISIBLE
                true
            }
        }

        // add top letters
        for (i in 0 until model.anagramLetters.size) {

            // add top letter
            val topLetter = createTextView("_")
            topLetter.tag = i

            val dragTarget = createTextView("▲")

            dragTarget.alpha = 0.3f
            dragTarget.visibility = View.INVISIBLE
            dragTarget.tag = i

            val assignedLetter = model.anagramLetters.find { it.assignedIndex == i }
            if (assignedLetter != null) {
                topLetter.text = assignedLetter.letter.toString()
            }

            val topLetterLayout = LinearLayout(this)
            topLetterLayout.orientation = VERTICAL
            topLetterLayout.gravity = Gravity.CENTER
            topLetterLayout.addView(topLetter)
            topLetterLayout.addView(dragTarget)
            topLettersLayout.addView(topLetterLayout)

            topLetterLayouts.add(topLetterLayout)
            topLetterDragTargets.add(dragTarget)
            topLetters.add(topLetter)

            //clicking on an assigned letter un-assigns it
            topLetter.setOnClickListener {
                val index = it.tag as Int
                val assignedLetter2 = model.anagramLetters.find { al -> al.assignedIndex == index }
                assignedLetter2?.assignedIndex = -1
                buildUI()
            }

            topLetter.width = 60
            topLetter.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            topLetter.LinearLayoutParams.leftMargin = 10
            topLetter.LinearLayoutParams.rightMargin = 10

            // implement drag and drop behaviour
            // dragging and dropping an unassigned letter onto a position assigns it
            dragTarget.setOnDragListener { v, event -> dragHandler(v, event) }
            topLetter.setOnDragListener { v, event -> dragHandler(v, event) }
        }
    }

    private var tempOverriddenDropText = ""
    private fun dragHandler(view: View, event: DragEvent): Boolean {
        val draggedTV = event.localState as TextView
        val letter = draggedTV.tag as AnagramLetter

        val dropTV = view as TextView
        val index = dropTV.tag as Int

        val targetTV = topLetters[index]

        when (event.action) {
            DragEvent.ACTION_DRAG_LOCATION -> {
                Log.d("dnd", "Drag location $index")
            }

            DragEvent.ACTION_DRAG_STARTED -> {
                Log.d("dnd", "Drag started $index")
            }

            DragEvent.ACTION_DRAG_ENTERED -> {
                tempOverriddenDropText = targetTV.text.toString()
                targetTV.text = letter.letter.toString()
                Log.d("dnd", "Drag entered $index")
            }

            DragEvent.ACTION_DRAG_EXITED -> {
                targetTV.text = tempOverriddenDropText
                Log.d("dnd", "Drag exited $index")
            }

            DragEvent.ACTION_DROP -> {
                Log.d("dnd", "Drop $index")

                // de-assign if assigned
                val assignedLetter = model.anagramLetters.find { it.assignedIndex == index }
                assignedLetter?.assignedIndex = -1

                letter.assignedIndex = index
                draggedTV.visibility = View.VISIBLE

                buildUI()

            }

            DragEvent.ACTION_DRAG_ENDED -> {
                Log.d("dnd", "Drag ended $index")
                draggedTV.visibility = View.VISIBLE
                for (topLetterDragTarget in topLetterDragTargets) {
                    topLetterDragTarget.visibility = View.INVISIBLE
                }
            }
        }

        return true
    }

    private fun readWordList() {
        val `is`: InputStream = this.resources.openRawResource(R.raw.words_alpha)
        val br = BufferedReader(InputStreamReader(`is`))
        var readLine: String?

        try {
            while (br.readLine().also { readLine = it } != null) {
                if (readLine != null) {
                    val word = readLine as String
                    if (word.length in 1..12) {
                        words.add(word.toUpperCase(Locale.getDefault()))
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun findCandidateWords(): Int {

        var candidateCount = 0
        var anyLettersAssigned = false

        val setLetters = ArrayList<Pair<Int, Char>>()
        val sortedLetters = model.anagramLetters.map { it.letter }.sorted()

        for (anagramLetter in model.anagramLetters) {
            if (anagramLetter.assignedIndex != -1) {
                anyLettersAssigned = true
                setLetters.add(Pair(anagramLetter.assignedIndex, anagramLetter.letter))
            }
        }

        if (!anyLettersAssigned) {
            return 0
        }

        for (word in words.filter { it.length == model.anagramLetters.size }) {
            var isMatch = true
            for (letter in setLetters) {
                if (word[letter.first] != letter.second) {
                    isMatch = false
                    break
                }
            }

            if (isMatch) {
                val wordSortedLetters = word.asIterable().sorted()
                if (wordSortedLetters == sortedLetters) {
                    candidateCount++
                }
            }
        }
        return candidateCount
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val m: AnagramViewModel by viewModels()
        model = m

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        readWordList()

        handwriting = resources.getFont(R.font.neucha)

        // the current value of the input text
        val txtAnagramLetters = findViewById<View>(R.id.txtAnagramLetters) as EditText

        txtAnagramLetters.doAfterTextChanged {
            val inputText = it.toString()

            // forget trying to preserve existing letters just replace
            model.anagramLetters.clear()
            model.anagramLetters.addAll(inputText.map { c -> AnagramLetter(c.toUpperCase(), -1) })
            buildUI()
        }

        model.anagramLetters.addAll(txtAnagramLetters.text.map { AnagramLetter(it, -1) })
        buildUI()
    }
}