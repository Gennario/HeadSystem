import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Scanner;
import java.util.UUID;

@Getter
public class HeadManager {

    public enum HeadType {
        PLAYER_HEAD,
        BASE64;
    }

    private HeadType type;
    private String value;

    public HeadManager(HeadType type, String value) {
        this.type = type;
        this.value = value;
    }

    public ItemStack convert() {
        if (type.equals(HeadType.PLAYER_HEAD)) {
            return getSkullByTexture(getPlayerHeadTexture(value));
        } else {
            return getSkullByTexture(value);
        }
    }

    private static ItemStack getSkullByTexture(String url) {
        ItemStack head = getHead();
        if (url.isEmpty() || url.equals("none")) return head;

        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);

        profile.getProperties().put("textures", new Property("textures", url));

        try {
            Method mtd = skullMeta.getClass().getDeclaredMethod("setProfile", GameProfile.class);
            mtd.setAccessible(true);
            mtd.invoke(skullMeta, profile);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        head.setItemMeta(skullMeta);
        return head;
    }

    public CompleteableFuture<String> getPlayerHeadTextureAsync(String username) {
        return CompletableFuture.supplyAsync(() -> getPlayerHeadTexture(username));
    }
    
    public String getPlayerHeadTexture(String username) {
        if (getPlayerId(username).equals("none")) return "none";
        String url = "https://sessionserver.mojang.com/session/minecraft/profile/" + getPlayerId(username);
        try {
            JSONParser jsonParser = new JSONParser();
            String userData = readUrl(url);
            Object parsedData = jsonParser.parse(userData);

            JSONObject jsonData = (JSONObject) parsedData;
            JSONArray streamArray = (JSONArray) jsonData.get("properties");
            JSONObject jsonMap = (JSONObject) streamArray.get(0);

            return jsonMap.get("value").toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "none";
    }

    private String readUrl(String urlString) throws Exception {
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buffer = new StringBuffer();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1) buffer.append(chars, 0, read);
            return buffer.toString();
        } finally {
            if (reader != null) reader.close();
        }
    }


    private String getPlayerId(String playerName) {
        try {
            String url = "https://api.mojang.com/users/profiles/minecraft/" + playerName;
            InputStream inputStream = new URL(url).openStream();
            Scanner scanner = new Scanner(inputStream);
            String data = scanner.nextLine();
            JSONObject object = (JSONObject) new JSONParser().parse(data);
            if (object.containsKey("id")) {
                return object.get("id").toString();
            }
        } catch (Exception ignored) {
            return "none";
        }
        return "none";
    }

    public ItemStack getHead() {
        ItemStack headStack = null;
        Material material = null;
        int data = 0;
        try {
            material = Material.valueOf("PLAYER_HEAD");
        } catch (Exception e) {
            material = Material.valueOf("SKULL_ITEM");
            data = 3;
        }
        headStack = new ItemStack(material, 1, (byte)data);
        return headStack;
    }

}
