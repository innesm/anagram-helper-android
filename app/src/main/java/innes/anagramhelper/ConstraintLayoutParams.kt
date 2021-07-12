package innes.anagramhelper

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout

val View.ConstraintLayoutParams: ConstraintLayout.LayoutParams
    get() {
        return this.layoutParams as ConstraintLayout.LayoutParams
    }