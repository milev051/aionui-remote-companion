package rs.milev051.aionuiremote

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import java.io.ByteArrayInputStream

class MainActivity : Activity() {
    private val port = 25808
    private val prefs by lazy { getSharedPreferences("connection", Context.MODE_PRIVATE) }
    private lateinit var root: FrameLayout
    private lateinit var hostInput: EditText
    private lateinit var errorText: TextView
    private lateinit var progress: ProgressBar
    private lateinit var webView: WebView
    private var activeHost: String? = null
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null

    companion object {
        private const val FILE_CHOOSER_REQUEST = 7301
        private const val PREF_HOST = "host"
        private const val BACKGROUND = 0xFF10131A.toInt()
        private const val CARD = 0xFF191E28.toInt()
        private const val FIELD = 0xFF232A36.toInt()
        private const val PRIMARY = 0xFF829EFF.toInt()
        private const val TEXT = 0xFFF3F5FA.toInt()
        private const val MUTED = 0xFFA3ADBD.toInt()
        private const val ERROR = 0xFFFF817D.toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BACKGROUND
        window.navigationBarColor = BACKGROUND
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        CookieManager.getInstance().setAcceptCookie(true)
        root = FrameLayout(this).apply { setBackgroundColor(BACKGROUND) }
        setContentView(root)
        buildConnectScreen()
    }

    private fun buildConnectScreen() {
        root.removeAllViews()
        val scroll = android.widget.ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(28))
        }
        scroll.addView(content, FrameLayout.LayoutParams(-1, -1))

        val brandRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val mark = TextView(this).apply {
            text = "A"
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(TEXT)
            background = rounded(PRIMARY, 16)
        }
        brandRow.addView(mark, LinearLayout.LayoutParams(dp(52), dp(52)))
        val brandText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
        }
        brandText.addView(label("AionUi Remote", 23f, TEXT, true))
        brandText.addView(label("PRIVATNI WEBUI KLIJENT", 10f, MUTED, true).apply {
            letterSpacing = 0.12f
            setPadding(0, dp(4), 0, 0)
        })
        brandRow.addView(brandText)
        content.addView(brandRow)

        content.addView(label("Poveži se sa svojim AionUi računarom", 25f, TEXT, true).apply {
            setPadding(0, dp(34), 0, dp(10))
        })
        content.addView(label("Tailscale mora biti povezan na telefonu, a WebUI pokrenut na Macu.", 15f, MUTED, false))

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(CARD, 20)
            setPadding(dp(18), dp(20), dp(18), dp(18))
        }
        card.addView(label("IP adresa ili MagicDNS ime", 14f, TEXT, true))
        hostInput = EditText(this).apply {
            hint = "npr. 100.x.x.x"
            setHintTextColor(0xFF7E899A.toInt())
            setTextColor(TEXT)
            textSize = 16f
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_GO
            background = rounded(FIELD, 12)
            setPadding(dp(14), 0, dp(14), 0)
            setText(prefs.getString(PREF_HOST, "") ?: "")
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                    connect()
                    true
                } else false
            }
        }
        card.addView(hostInput, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(10) })
        card.addView(label("Port 25808 dodajem automatski. Unesi samo privatnu IP adresu ili ime uređaja.", 12f, MUTED, false).apply {
            setPadding(0, dp(9), 0, 0)
        })
        val connectButton = Button(this).apply {
            text = "Poveži se"
            textSize = 16f
            isAllCaps = false
            setTextColor(0xFF101725.toInt())
            background = rounded(PRIMARY, 14)
            setOnClickListener { connect() }
        }
        card.addView(connectButton, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(18) })
        errorText = label("", 13f, ERROR, false).apply { setPadding(0, dp(10), 0, 0) }
        card.addView(errorText)
        content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(28) })
        content.addView(label("Ako veza ne uspe, proveri da li Tailscale piše Connected, da li je Mac budan i da li je AionUi WebUI uključen.", 13f, MUTED, false).apply {
            setPadding(dp(2), dp(18), dp(2), 0)
        })
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
    }

    private fun connect() {
        val host = normalizeHost(hostInput.text?.toString().orEmpty())
        if (host == null) {
            errorText.text = "Unesi Tailscale IP, privatnu LAN IP ili MagicDNS ime (.ts.net)."
            return
        }
        activeHost = host
        prefs.edit().putString(PREF_HOST, host).apply()
        buildWebScreen(host)
        webView.loadUrl("http://$host:$port/")
    }

    private fun buildWebScreen(host: String) {
        root.removeAllViews()
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BACKGROUND)
        }
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(12), dp(4))
        }
        val changeButton = Button(this).apply {
            text = "‹"
            textSize = 30f
            isAllCaps = false
            minWidth = 0
            minimumWidth = 0
            setPadding(0, 0, 0, dp(5))
            setTextColor(TEXT)
            background = transparentDrawable()
            contentDescription = "Promeni adresu"
            setOnClickListener { buildConnectScreen() }
        }
        toolbar.addView(changeButton, LinearLayout.LayoutParams(dp(48), dp(48)))
        val titleArea = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleArea.addView(label("AionUi", 16f, TEXT, true))
        titleArea.addView(label(host, 11f, MUTED, false).apply { setPadding(0, dp(2), 0, 0) })
        toolbar.addView(titleArea, LinearLayout.LayoutParams(0, -2, 1f))
        val reloadButton = Button(this).apply {
            text = "↻"
            textSize = 22f
            isAllCaps = false
            minWidth = 0
            minimumWidth = 0
            setTextColor(TEXT)
            background = transparentDrawable()
            contentDescription = "Osveži"
            setOnClickListener { webView.reload() }
        }
        toolbar.addView(reloadButton, LinearLayout.LayoutParams(dp(48), dp(48)))
        panel.addView(toolbar, LinearLayout.LayoutParams(-1, dp(56)))

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            progressTintList = android.content.res.ColorStateList.valueOf(PRIMARY)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(BACKGROUND)
        }
        panel.addView(progress, LinearLayout.LayoutParams(-1, dp(2)))

        webView = WebView(this).apply { setBackgroundColor(Color.WHITE) }
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            loadsImagesAutomatically = true
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) safeBrowsingEnabled = true
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean = routeUrl(request.url)

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return true
                return routeUrl(uri)
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
                val uri = request.url
                if (uri.scheme.equals("http", true) && !isAionUiOrigin(uri)) {
                    return WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", emptyMap(), ByteArrayInputStream("External HTTP blocked".toByteArray()))
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                progress.visibility = View.VISIBLE
                progress.progress = 5
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progress.progress = 100
                progress.postDelayed({ progress.visibility = View.GONE }, 250)
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true) {
                    Toast.makeText(this@MainActivity, "Ne mogu da otvorim AionUi. Proveri Tailscale i da li je Mac dostupan.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
                handler?.cancel()
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress.progress = newProgress
                progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.fileChooserCallback?.onReceiveValue(null)
                this@MainActivity.fileChooserCallback = filePathCallback
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                return try {
                    startActivityForResult(Intent.createChooser(intent, "Izaberi dokument"), FILE_CHOOSER_REQUEST)
                    true
                } catch (_: Exception) {
                    this@MainActivity.fileChooserCallback?.onReceiveValue(null)
                    this@MainActivity.fileChooserCallback = null
                    false
                }
            }
        }
        webView.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            val uri = runCatching { Uri.parse(url) }.getOrNull()
            if (uri == null || !isAionUiOrigin(uri)) {
                Toast.makeText(this, "Preuzimanje je dozvoljeno samo sa AionUi servera.", Toast.LENGTH_SHORT).show()
            } else {
                enqueueDownload(uri, userAgent, contentDisposition, mimeType)
            }
        })
        panel.addView(webView, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(panel, FrameLayout.LayoutParams(-1, -1))
    }

    private fun routeUrl(uri: Uri): Boolean {
        if (isAionUiOrigin(uri)) return false
        if (uri.scheme.equals("https", true)) {
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            return true
        }
        Toast.makeText(this, "Aplikacija otvara samo AionUi i bezbedne HTTPS linkove.", Toast.LENGTH_SHORT).show()
        return true
    }

    private fun isAionUiOrigin(uri: Uri): Boolean {
        val host = activeHost ?: return false
        return uri.scheme.equals("http", true) && uri.host.equals(host, true) && (uri.port == -1 || uri.port == port)
    }

    private fun enqueueDownload(uri: Uri, userAgent: String?, contentDisposition: String?, mimeType: String?) {
        try {
            val filename = URLUtil.guessFileName(uri.toString(), contentDisposition, mimeType)
            val request = DownloadManager.Request(uri).apply {
                setTitle(filename)
                setDescription("Preuzimanje iz AionUi-ja")
                setMimeType(mimeType)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                CookieManager.getInstance().getCookie(uri.toString())?.let { addRequestHeader("Cookie", it) }
                userAgent?.let { addRequestHeader("User-Agent", it) }
            }
            (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            Toast.makeText(this, "Preuzimanje je pokrenuto.", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Preuzimanje nije uspelo.", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Android file picker callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != FILE_CHOOSER_REQUEST) return
        val callback = fileChooserCallback ?: return
        fileChooserCallback = null
        if (resultCode != RESULT_OK || data == null) {
            callback.onReceiveValue(null)
            return
        }
        val results = mutableListOf<Uri>()
        data.clipData?.let { clip ->
            for (index in 0 until clip.itemCount) results.add(clip.getItemAt(index).uri)
        }
        if (results.isEmpty()) data.data?.let(results::add)
        callback.onReceiveValue(results.takeIf { it.isNotEmpty() }?.toTypedArray())
    }

    private fun normalizeHost(raw: String): String? {
        val host = raw.trim().lowercase()
        if (host.isEmpty() || host.any { it.isWhitespace() } || host.contains('/') || host.contains(':') || host.contains('?') || host.contains('#') || host.contains('@')) return null
        if (!host.matches(Regex("^[a-z0-9.-]+$")) || host.startsWith('.') || host.endsWith('.') || host.contains("..")) return null

        val parts = host.split('.')
        val isIpv4 = parts.size == 4 && parts.all { it.toIntOrNull() in 0..255 }
        if (isIpv4) {
            val octets = parts.map { it.toInt() }
            val first = octets[0]
            val second = octets[1]
            val privateLan = first == 10 || (first == 172 && second in 16..31) || (first == 192 && second == 168)
            val tailnet = first == 100 && second in 64..127
            return host.takeIf { privateLan || tailnet }
        }

        val magicDns = host.endsWith(".ts.net") && parts.size >= 3
        val shortName = parts.size == 1 && host.length <= 63
        val localName = host.endsWith(".local")
        return host.takeIf { magicDns || shortName || localName }
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean): TextView = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
    }

    private fun rounded(color: Int, radiusDp: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun transparentDrawable(): GradientDrawable = GradientDrawable().apply {
        setColor(Color.TRANSPARENT)
        cornerRadius = dp(12).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else if (::webView.isInitialized && activeHost != null) {
            buildConnectScreen()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        fileChooserCallback?.onReceiveValue(null)
        fileChooserCallback = null
        super.onDestroy()
    }
}
