package jp.sawa_kai.sawakai.spigot.skyway

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class CookieJar: CookieJar {

    private val cookiesStore = mutableMapOf<String, List<Cookie>>()

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookiesStore[url.host] ?: listOf()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookiesStore[url.host] = cookies
    }
}