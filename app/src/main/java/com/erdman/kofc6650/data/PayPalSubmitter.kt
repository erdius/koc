package com.erdman.kofc6650.data

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Submits a PayPal hosted-button "Buy Now" form the same way a browser
 * would (POST to cgi-bin/webscr with the option fields), then follows
 * PayPal's redirect to the actual checkout URL -- OkHttp follows
 * redirects by default, so this needs no WebView at all. We never touch
 * card/payment entry ourselves; the resulting URL is handed off to the
 * system browser to finish checkout.
 */
object PayPalSubmitter {
    private val client = OkHttpClient.Builder().build()

    fun submit(hostedButtonId: String, fields: Map<String, String>): String {
        val formBuilder = FormBody.Builder()
            .add("cmd", "_s-xclick")
            .add("hosted_button_id", hostedButtonId)
        fields.forEach { (key, value) -> formBuilder.add(key, value) }

        val request = Request.Builder()
            .url("https://www.paypal.com/cgi-bin/webscr")
            .post(formBuilder.build())
            .build()

        client.newCall(request).execute().use { response ->
            return response.request.url.toString()
        }
    }
}
