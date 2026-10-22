package com.mhttn.sunplusremote

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var device: UcastDevice? = null
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)

        // Discovery runs once at startup and keeps listening in the
        // background; the box announces itself periodically, so this
        // updates statusText as soon as a broadcast is seen.
        UcastProtocol.startDiscovery(
            onDevice = { found ->
                device = found
                runOnUiThread {
                    statusText.text = "Bağlandı: ${found.name}  (${found.ip}  sPort=${found.sPort} dvbSPort=${found.dvbSPort})"
                }
            },
            onError = { e ->
                runOnUiThread {
                    statusText.text = "Keşif hatası: ${e.message}\n(8998 portu meşgul olabilir, ya da aynı ağda değilsiniz)"
                }
            }
        )

        bindKey(R.id.btnPower, UcastProtocol.Keys.POWER)
        bindKey(R.id.btnMute, UcastProtocol.Keys.MUTE)
        bindKey(R.id.btnMenu, UcastProtocol.Keys.MENU)
        bindKey(R.id.btnUp, UcastProtocol.Keys.UP)
        bindKey(R.id.btnDown, UcastProtocol.Keys.DOWN)
        bindKey(R.id.btnLeft, UcastProtocol.Keys.LEFT)
        bindKey(R.id.btnRight, UcastProtocol.Keys.RIGHT)
        bindKey(R.id.btnOk, UcastProtocol.Keys.ENTER)
        bindKey(R.id.btnReturn, UcastProtocol.Keys.RETURN)
        bindKey(R.id.btnExit, UcastProtocol.Keys.EXIT)
        bindKey(R.id.btnInfo, UcastProtocol.Keys.INFO)
        bindKey(R.id.btnChUp, UcastProtocol.Keys.CHANNEL_UP)
        bindKey(R.id.btnChDown, UcastProtocol.Keys.CHANNEL_DOWN)
        bindKey(R.id.btnVolUp, UcastProtocol.Keys.VOLUME_UP)
        bindKey(R.id.btnVolDown, UcastProtocol.Keys.VOLUME_DOWN)
        bindKey(R.id.btnEpg, UcastProtocol.Keys.EPG)
        bindKey(R.id.btnFav, UcastProtocol.Keys.FAV)
        bindKey(R.id.btn0, UcastProtocol.Keys.ZERO)
        bindKey(R.id.btn1, UcastProtocol.Keys.ONE)
        bindKey(R.id.btn2, UcastProtocol.Keys.TWO)
        bindKey(R.id.btn3, UcastProtocol.Keys.THREE)
        bindKey(R.id.btn4, UcastProtocol.Keys.FOUR)
        bindKey(R.id.btn5, UcastProtocol.Keys.FIVE)
        bindKey(R.id.btn6, UcastProtocol.Keys.SIX)
        bindKey(R.id.btn7, UcastProtocol.Keys.SEVEN)
        bindKey(R.id.btn8, UcastProtocol.Keys.EIGHT)
        bindKey(R.id.btn9, UcastProtocol.Keys.NINE)
    }

    private fun bindKey(viewId: Int, command: String) {
        findViewById<Button>(viewId).setOnClickListener {
            val d = device
            if (d == null) {
                statusText.text = "Henüz cihaz bulunamadı - kutunun aynı Wi-Fi ağında olduğundan emin olun"
            } else {
                UcastProtocol.sendKey(d, command)
            }
        }
    }
}
