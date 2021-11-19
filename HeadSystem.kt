import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.*

class HeadManager(val type: HeadType, val value: String) {
    enum class HeadType {
        PLAYER_HEAD, BASE64
    }

    fun convert(): ItemStack {
        return if (type == HeadType.PLAYER_HEAD) {
            getSkullByTexture(getPlayerHeadTexture(value))
        } else {
            getSkullByTexture(value)
        }
    }

    fun getPlayerHeadTexture(username: String): String {
        if (getPlayerId(username) == "none") return "none"
        val url = "https://sessionserver.mojang.com/session/minecraft/profile/" + getPlayerId(username)
        return try {
            val jsonParser = JSONParser()
            val userData = readUrl(url)
            val parsedData = jsonParser.parse(userData)
            val jsonData = parsedData as JSONObject
            val streamArray = jsonData["properties"] as JSONArray
            val jsonMap = streamArray[0] as JSONObject
            jsonMap["value"].toString()
        } catch (ex: Exception) {
            ex.printStackTrace()
            "none"
        }
    }

    private fun readUrl(urlString: String): String {
        var reader: BufferedReader? = null
        return try {
            val url = URL(urlString)
            reader = BufferedReader(InputStreamReader(url.openStream()))
            val buffer = StringBuffer()
            var read: Int
            val chars = CharArray(1024)
            while (reader.read(chars).also { read = it } != -1) buffer.append(chars, 0, read)
            buffer.toString()
        } finally {
            reader?.close()
        }
    }

    private fun getPlayerId(playerName: String): String {
        return try {
            val url = "https://api.mojang.com/users/profiles/minecraft/$playerName"
            val inputStream = URL(url).openStream()
            val scanner = Scanner(inputStream)
            val data = scanner.nextLine()
            val jsonObject = JSONParser().parse(data) as JSONObject
            if (jsonObject.containsKey("id")) {
                jsonObject["id"].toString()
            } else {
                "none"
            }
        } catch (ignored: Exception) {
            "none"
        }
    }

    private fun getSkullByTexture(url: String): ItemStack {
        val head: ItemStack = getHead()
        if (url.isEmpty() || url == "none") return head
        val skullMeta = head.itemMeta as SkullMeta
        val profile = GameProfile(UUID.randomUUID(), null)
        profile.properties.put("textures", Property("textures", url))
        val method = skullMeta.javaClass.getDeclaredMethod("setProfile", GameProfile::class.java)
        method.isAccessible = true
        method.invoke(skullMeta, profile)
        head.itemMeta = skullMeta
        return head
    }

    private fun getHead(): ItemStack {
        var material: Material
        var data = 0
        try {
            material = Material.valueOf("PLAYER_HEAD")
        } catch (e: java.lang.Exception) {
            material = Material.valueOf("SKULL_ITEM")
            data = 3
        }
        return ItemStack(material, 1, data.toShort())
    }
}
