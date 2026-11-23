package id.kantong.app.data

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** What a home-screen widget asked the app to open: Catat (optionally with a category) or the budget sheet. */
object LaunchAction {
    var add by mutableStateOf(false)
    var category by mutableLongStateOf(0L)
    var budget by mutableStateOf(false)
    var review by mutableStateOf(false)

    fun handle(intent: Intent?) {
        when (intent?.getStringExtra("catet_action")) {
            "add" -> { category = intent.getLongExtra("category", 0L); add = true }
            "budget" -> budget = true
            "review" -> review = true
        }
        intent?.removeExtra("catet_action")
    }
}
