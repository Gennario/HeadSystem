import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

    fun convertAsync(): CompletableFuture<ItemStack> {
        return if (type == HeadType.PLAYER_HEAD) {
            getPlayerHeadTextureAsync(value).thenApply { url: String ->
                getSkullByTexture(
                    url
                )
            }
        } else {
            CompletableFuture.supplyAsync { getSkullByTexture(value) }
        }
    }

    private fun getSkullByTexture(url: String): ItemStack {
        val head: ItemStack = head
        if (url.isEmpty() || url == "none") return head
        val skullMeta: SkullMeta = head.itemMeta as SkullMeta
        val profile = GameProfile(UUID.randomUUID(), null)
        profile.getProperties().put("textures", Property("textures", url))
        try {
            val mtd: Method = skullMeta.javaClass.getDeclaredMethod("setProfile", GameProfile::class.java)
            mtd.isAccessible = true
            mtd.invoke(skullMeta, profile)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        head.itemMeta = skullMeta
        return head
    }

    private fun getPlayerHeadTextureAsync(username: String): CompletableFuture<String> {
        return CompletableFuture.supplyAsync { getPlayerHeadTexture(username) }
    }

    private fun getPlayerHeadTexture(username: String): String {
        if (getPlayerId(username) == "none") return "none"
        val url = "https://sessionserver.mojang.com/session/minecraft/profile/" + getPlayerId(username)
        try {
            val jsonParser = JSONParser()
            val userData = readUrl(url)
            val parsedData: Any = jsonParser.parse(userData)
            val jsonData: JSONObject = parsedData as JSONObject
            val streamArray: JSONArray = jsonData.get("properties") as JSONArray
            val jsonMap: JSONObject = streamArray.get(0) as JSONObject
            return jsonMap.get("value").toString()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return "none"
    }

    @Throws(Exception::class)
    private fun readUrl(urlString: String): String {
        var reader: BufferedReader? = null
        return try {
            val url = URL(urlString)
            reader = BufferedReader(InputStreamReader(url.openStream()))
            val buffer = StringBuilder()
            var read: Int
            val chars = CharArray(1024)
            while (reader.read(chars).also { read = it } != -1) buffer.append(chars, 0, read)
            buffer.toString()
        } finally {
            reader?.close()
        }
    }

    private fun getPlayerId(playerName: String?): String {
        try {
            val url = "https://api.mojang.com/users/profiles/minecraft/$playerName"
            val inputStream = URL(url).openStream()
            val scanner = Scanner(inputStream)
            val data = scanner.nextLine()
            val `object`: JSONObject = JSONParser().parse(data) as JSONObject
            if (`object`.containsKey("id")) {
                return `object`.get("id").toString()
            }
        } catch (ignored: Exception) {
            return "none"
        }
        return "none"
    }

    private val head: ItemStack
        get() {
            val headStack: ItemStack
            var material: Material
            var data = 0
            try {
                material = Material.valueOf("PLAYER_HEAD")
            } catch (e: Exception) {
                material = Material.valueOf("SKULL_ITEM")
                data = 3
            }
            headStack = ItemStack(material, 1, data.toShort())
            return headStack
        }
}
