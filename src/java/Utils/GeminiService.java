package Utils;

import Model.DTO.ChatMessage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GeminiService {
    private static final String API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

    public String getGeminiResponse(List<ChatMessage> messageHistory) throws Exception {
        // The key goes in a header, not the URL, so it can't leak through logs or exception messages.
        String apiKey = AppConfig.require("GEMINI_API_KEY");
        String model = AppConfig.get("GEMINI_MODEL", "gemini-3.1-flash-lite");
        URL url = new URL(API_BASE + model + ":generateContent");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("x-goog-api-key", apiKey);
        conn.setDoOutput(true);

        // 1. DÙNG GSON ĐỂ BUILD PAYLOAD JSON AN TOÀN
        JsonArray contentsArray = new JsonArray();

        for (ChatMessage msg : messageHistory) {
            JsonObject contentObject = new JsonObject();

            // Xử lý Role: Quy đổi sender của bạn thành chuẩn của Gemini
            String role = "user";
            if (msg.getSender().equalsIgnoreCase("BOT")) {
                role = "model";
            }
            contentObject.addProperty("role", role);

            // Xây dựng mảng "parts"
            JsonArray partsArray = new JsonArray();
            JsonObject textObject = new JsonObject();
            textObject.addProperty("text", msg.getMessageContent()); // Gson tự động escape các ký tự \n, " ...
            partsArray.add(textObject);

            contentObject.add("parts", partsArray);

            // Đưa vào mảng tổng "contents"
            contentsArray.add(contentObject);
        }

        // Tạo root Object bọc ngoài cùng
        JsonObject rootObject = new JsonObject();
        rootObject.add("contents", contentsArray);

        // Xuất ra chuỗi JSON hoàn chỉnh
        String jsonInputString = rootObject.toString();

        // 2. GỬI REQUEST
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // 3. ĐỌC VÀ TRẢ VỀ RESPONSE
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
        StringBuilder response = new StringBuilder();
        String responseLine;
        while ((responseLine = br.readLine()) != null) {
            response.append(responseLine.trim());
        }

        JsonObject responseJson = JsonParser.parseString(response.toString()).getAsJsonObject();
        String aiResponse = responseJson.getAsJsonArray("candidates")
                .get(0).getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0).getAsJsonObject()
                .get("text").getAsString();

        return aiResponse;
    }
}