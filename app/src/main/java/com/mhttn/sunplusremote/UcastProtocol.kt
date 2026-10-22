package com.mhttn.sunplusremote

import com.example.ucast.LocalApplication
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Reverse-engineered from the "Ucast" companion app (v5.0.1) that ships with
 * Sunplus-chipset satellite receivers. Everything here was found by
 * decompiling that APK - it is NOT documented anywhere by the vendor.
 *
 * Wire format, confirmed from the original app's code:
 *   - Every payload is a JSON object, serialized to a string, then run
 *     through LocalApplication.encodeString() (native, reused as-is - we
 *     don't know what it does internally), then sent as UDP bytes prefixed
 *     with the literal string "json=".
 *   - Incoming broadcast/response payloads are the same but in reverse:
 *     strip the "json=" prefix (5 chars), decodeString(), then it's plain
 *     JSON text.
 *
 * Two ports matter, both learned per-device via the discovery broadcast
 * (there is no fixed/global port - it's assigned by the box and announced):
 *   - dvbSPort: used for DVB channel-list operations (not implemented here)
 *   - sPort:    used for remote-control key presses (this file) and for
 *               IPTV/app-list operations
 *
 * Discovery itself is a fixed, well-known port: the box broadcasts on
 * UDP 8998 periodically; the app just listens there.
 */
data class UcastDevice(
    val ip: String,
    val name: String,
    val sPort: Int,
    val dvbSPort: Int,
    val appstore: Int
)

object UcastProtocol {

    const val DISCOVERY_PORT = 8998

    /**
     * Every remote-control button in the original app, mapped to its
     * "/api/ir/..." command string. Confirmed exhaustively from
     * RemoteFragment.java (every button in the app's Remote tab is listed
     * here - nothing was left out or guessed).
     */
    object Keys {
        const val UP = "/api/ir/up"
        const val DOWN = "/api/ir/down"
        const val LEFT = "/api/ir/left"
        const val RIGHT = "/api/ir/right"
        const val ENTER = "/api/ir/enter"
        const val CHANNEL_UP = "/api/ir/channel_up"
        const val CHANNEL_DOWN = "/api/ir/channel_down"
        const val VOLUME_UP = "/api/ir/volumn_up"   // "volumn" typo is in the original app, kept as-is
        const val VOLUME_DOWN = "/api/ir/volumn_down"
        const val POWER = "/api/ir/power"
        const val MUTE = "/api/ir/mute"
        const val MENU = "/api/ir/menu"
        const val EXIT = "/api/ir/exit"
        const val RETURN = "/api/ir/return"
        const val INFO = "/api/ir/info"
        const val EPG = "/api/ir/epg"
        const val FAV = "/api/ir/fav"
        const val AUDIO = "/api/ir/audio"
        const val SUBTITLE = "/api/ir/subtitle"
        const val SOURCE = "/api/ir/source"
        const val DEV = "/api/ir/dev"
        const val TV_RADIO = "/api/ir/tv_radio"
        const val PAGE_UP = "/api/ir/pageup"
        const val PAGE_DOWN = "/api/ir/pagedown"
        const val RED = "/api/ir/red"
        const val GREEN = "/api/ir/green"
        const val YELLOW = "/api/ir/yellow"
        const val BLUE = "/api/ir/blue"
        const val PLAY = "/api/ir/play"
        const val PAUSE = "/api/ir/pause"
        const val STOP = "/api/ir/stop"
        const val REWIND = "/api/ir/rewind"
        const val FAST_FORWARD = "/api/ir/fast_forward"
        const val PREVIOUS = "/api/ir/previous"
        const val NEXT = "/api/ir/next"
        const val RECORD = "/api/ir/record"
        const val REPEAT = "/api/ir/repeat"
        const val ZERO = "/api/ir/zero"
        const val ONE = "/api/ir/one"
        const val TWO = "/api/ir/two"
        const val THREE = "/api/ir/three"
        const val FOUR = "/api/ir/four"
        const val FIVE = "/api/ir/five"
        const val SIX = "/api/ir/six"
        const val SEVEN = "/api/ir/seven"
        const val EIGHT = "/api/ir/eight"
        const val NINE = "/api/ir/nine"
    }

    /**
     * Listens for the box's discovery broadcast on UDP 8998 until [running]
     * is set to false or [onDevice] has fired once. Runs its own background
     * thread - call from a non-UI context, or just let it manage its own
     * thread as written here. [onDevice] is invoked on that background
     * thread, not the UI thread - hop back with runOnUiThread if updating
     * views directly.
     */
    fun startDiscovery(onDevice: (UcastDevice) -> Unit, onError: (Exception) -> Unit): AtomicBoolean {
        val running = AtomicBoolean(true)
        Thread {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(DISCOVERY_PORT))
                    soTimeout = 30_000
                }
                val buf = ByteArray(4096)
                while (running.get()) {
                    val packet = DatagramPacket(buf, buf.size)
                    socket.receive(packet)
                    val raw = String(packet.data, packet.offset, packet.length)
                    if (raw.length <= 5) continue
                    val decoded = LocalApplication.decodeString(raw.substring(5))
                    val json = JSONObject(decoded)
                    val device = UcastDevice(
                        ip = packet.address.hostAddress ?: continue,
                        name = json.optString("name"),
                        sPort = json.optInt("s_port"),
                        dvbSPort = json.optInt("dvb_s_port"),
                        appstore = json.optInt("appstore")
                    )
                    onDevice(device)
                }
            } catch (e: Exception) {
                if (running.get()) onError(e)
            } finally {
                socket?.close()
            }
        }.start()
        return running
    }

    /**
     * Fire-and-forget remote-key press. Matches SendToUcastBean's shape
     * exactly: {"url":"<cmd>","name":"","len":<cmd.length>,"type":"key","use":"post"}
     * then json= + encodeString(...), sent as one UDP datagram to
     * device.ip : device.sPort. No response is read - the original app
     * doesn't wait for one for key presses either.
     */
    fun sendKey(device: UcastDevice, command: String) {
        Thread {
            try {
                val payload = JSONObject().apply {
                    put("url", command)
                    put("name", "")
                    put("len", command.length)
                    put("type", "key")
                    put("use", "post")
                }
                val jsonString = payload.toString()
                val encoded = "json=" + LocalApplication.encodeString(jsonString)
                DatagramSocket().use { socket ->
                    val bytes = encoded.toByteArray()
                    socket.send(DatagramPacket(bytes, bytes.size, InetAddress.getByName(device.ip), device.sPort))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
