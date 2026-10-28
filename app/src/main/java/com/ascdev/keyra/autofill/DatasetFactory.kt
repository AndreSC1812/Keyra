package com.ascdev.keyra.autofill

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.service.autofill.Dataset
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.ascdev.keyra.R
import com.ascdev.keyra.domain.model.VaultEntry

@Suppress("DEPRECATION") // RemoteViews presentations still work everywhere; inline ones need API 30+.
object DatasetFactory {

    fun forEntry(context: Context, entry: VaultEntry, fields: ParsedFields): Dataset {
        val presentation = presentation(context, entry.title, entry.username)
        return Dataset.Builder(presentation).apply {
            fields.usernameIds.forEach { setValue(it, AutofillValue.forText(entry.username)) }
            fields.passwordIds.forEach { setValue(it, AutofillValue.forText(entry.password)) }
        }.build()
    }

    /** Placeholder dataset that opens Keyra to unlock the vault and pick an entry. */
    fun unlockDataset(context: Context, fields: ParsedFields, requestCode: Int): Dataset {
        val presentation = presentation(
            context,
            context.getString(R.string.autofill_unlock_title),
            context.getString(R.string.autofill_unlock_subtitle),
        )
        return Dataset.Builder(presentation).apply {
            (fields.usernameIds + fields.passwordIds).forEach { setValue(it, null, presentation) }
            setAuthentication(authIntentSender(context, fields, requestCode))
        }.build()
    }

    private fun authIntentSender(context: Context, fields: ParsedFields, requestCode: Int): IntentSender {
        val intent = Intent(context, AutofillAuthActivity::class.java).apply {
            putParcelableArrayListExtra(AutofillAuthActivity.EXTRA_USERNAME_IDS, ArrayList<AutofillId>(fields.usernameIds))
            putParcelableArrayListExtra(AutofillAuthActivity.EXTRA_PASSWORD_IDS, ArrayList<AutofillId>(fields.passwordIds))
            putExtra(AutofillAuthActivity.EXTRA_PACKAGE_NAME, fields.packageName)
            putExtra(AutofillAuthActivity.EXTRA_WEB_DOMAIN, fields.webDomain)
        }
        // The autofill framework adds its own extras to the intent, so it has to be mutable.
        val flags = PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE
        return PendingIntent.getActivity(context, requestCode, intent, flags).intentSender
    }

    private fun presentation(context: Context, title: String, subtitle: String) =
        RemoteViews(context.packageName, R.layout.autofill_item).apply {
            setTextViewText(R.id.autofill_title, title)
            setTextViewText(R.id.autofill_subtitle, subtitle)
        }
}
