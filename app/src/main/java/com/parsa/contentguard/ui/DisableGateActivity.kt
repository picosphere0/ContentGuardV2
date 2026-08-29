package com.parsa.contentguard.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.parsa.contentguard.admin.GuardDeviceAdminReceiver

/**
 * The entire disable gate, by design: read a fixed scripture passage on sin
 * and lust, then type out a fixed confession paragraph exactly. No cooldown,
 * no passphrase, no third party - just the two things asked for. Deactivation
 * is only possible once the typed text matches exactly.
 */
class DisableGateActivity : AppCompatActivity() {

    companion object {
        // King James Version - public domain, quoted in full deliberately.
        private const val VERSE = "James 1:14-15 (KJV)\n\n" +
            "\"But every man is tempted, when he is drawn away of his own lust, " +
            "and enticed. Then when lust hath conceived, it bringeth forth sin: " +
            "and sin, when it is finished, bringeth forth death.\""

        private const val REQUIRED_PARAGRAPH =
            "I hereby state that I am betraying Jesus, who bled for me while I made excuses for my sin, and betraying the family I have not yet met — a wife who deserves a man who kept his word before he ever met her, children who deserve a father who did not build his character on secrets. When I knowingly sin, I am not weak in some passive way — I am actively choosing it. If I fail, there is no one else to blame: not stress, not circumstance, not the way I was made. I alone reached for it, and I alone am responsible."
    }

    private var disableButtonRef: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildLayout())
    }

    private fun buildLayout(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 96)
        }

        root.addView(TextView(this).apply {
            text = VERSE
            textSize = 16f
            setPadding(0, 0, 0, 48)
        })

        root.addView(TextView(this).apply {
            text = "Type the paragraph below exactly, word for word, to continue:"
            setPadding(0, 0, 0, 16)
        })

        root.addView(TextView(this).apply {
            text = REQUIRED_PARAGRAPH
            setPadding(24, 24, 24, 24)
            setBackgroundColor(0xFFEEEEEE.toInt())
        })

        val input = EditText(this).apply {
            minLines = 6
            hint = "Type it here"
        }
        root.addView(input)

        val disableButton = Button(this).apply {
            text = "Deactivate protection"
            isEnabled = false
            setOnClickListener {
                if (normalize(input.text.toString()) == normalize(REQUIRED_PARAGRAPH)) {
                    deactivateAdmin()
                } else {
                    Toast.makeText(
                        this@DisableGateActivity,
                        "Doesn't match yet - check it word for word.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        root.addView(disableButton)
        disableButtonRef = disableButton

        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                disableButtonRef?.isEnabled = normalize(s?.toString() ?: "") == normalize(REQUIRED_PARAGRAPH)
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        return root
    }

    /** Collapses whitespace differences (line wraps, double spaces) so only the actual words matter. */
    private fun normalize(text: String): String =
        text.trim().replace(Regex("\\s+"), " ")

    private fun deactivateAdmin() {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, GuardDeviceAdminReceiver::class.java)
        dpm.removeActiveAdmin(admin)
        Toast.makeText(this, "Protection deactivated.", Toast.LENGTH_LONG).show()
        finish()
    }
}
