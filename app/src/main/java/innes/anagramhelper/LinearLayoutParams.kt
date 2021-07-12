package innes.anagramhelper

import android.view.View
import android.widget.LinearLayout

public val View.LinearLayoutParams: LinearLayout.LayoutParams
    get() {
        return this.layoutParams as LinearLayout.LayoutParams
    }