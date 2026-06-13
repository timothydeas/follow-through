package com.ideasinc.followthrough.feedback

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * In-app review + feedback entry points, kept honest per the ethical guardrails:
 *  - The Play prompt fires only via Google's **official In-App Review API**,
 *    after genuine use (a few reminders created), and is **never sentiment-gated**
 *    (we never ask "happy? → review / unhappy? → form").
 *  - The feedback email link is independent and always available from Settings.
 */
object AppReview {

    private const val PREFS = "review_prefs"
    private const val KEY_REMINDER_COUNT = "reminder_count"
    private const val KEY_ASKED = "review_asked"

    // Ask only after the user has genuinely used the app a few times. Google
    // additionally rate-limits how often the prompt can actually appear.
    private const val REMINDER_THRESHOLD = 3

    /** Public feedback address — opt-in, opens the user's mail app, no tracking. */
    const val FEEDBACK_EMAIL = "ideas@insightsemerge.com"

    /**
     * Call after a reminder is saved. Counts genuine use and, once the
     * threshold is reached (once ever), asks the Play In-App Review API to
     * show its prompt. No-op if the Play flow is unavailable.
     */
    fun onReminderSaved(activity: Activity) {
        val prefs = activity.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ASKED, false)) return
        val count = prefs.getInt(KEY_REMINDER_COUNT, 0) + 1
        prefs.edit().putInt(KEY_REMINDER_COUNT, count).apply()
        if (count < REMINDER_THRESHOLD) return

        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful) {
                manager.launchReviewFlow(activity, request.result)
                    .addOnCompleteListener {
                        // Mark asked regardless of outcome (Google never tells us
                        // whether the user reviewed — and we must not condition on it).
                        prefs.edit().putBoolean(KEY_ASKED, true).apply()
                    }
            }
        }
    }

    /** Opens the user's email app to send feedback. Always available; no tracking. */
    fun sendFeedback(context: Context) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$FEEDBACK_EMAIL")
            putExtra(Intent.EXTRA_SUBJECT, "FollowThru feedback")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            /* No email app available — nothing more to do. */
        }
    }
}
