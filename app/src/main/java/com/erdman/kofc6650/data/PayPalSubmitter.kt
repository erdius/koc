package com.erdman.kofc6650.data

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

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

        val submitPath = "/cgi-bin/webscr"
        val request = Request.Builder()
            .url("https://www.paypal.com$submitPath")
            .post(formBuilder.build())
            .build()

        client.newCall(request).execute().use { response ->
            val finalUrl = response.request.url
            // response.request.url is always *some* URL, even when PayPal
            // rejected the form outright -- a real success redirects away
            // from cgi-bin/webscr to a checkout page, so a non-2xx status
            // or landing back on that same script (regardless of query
            // string -- an error/cancel redirect often keeps the same path
            // with different params, which a plain full-URL string
            // comparison would miss) both mean the form was never accepted.
            if (!response.isSuccessful || finalUrl.encodedPath == submitPath) {
                throw IOException("PayPal did not return a checkout URL (HTTP ${response.code})")
            }
            return finalUrl.toString()
        }
    }
}
