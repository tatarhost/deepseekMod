package com.tatarhost.dplab;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class GitHubClient {
    private static final String API = "https://api.github.com";
    private GitHubClient() {}

    static JSONObject user(String token) throws Exception {
        return new JSONObject(call("GET", API + "/user", token, null));
    }

    static List<String> repositories(String token) throws Exception {
        JSONArray items = new JSONArray(call("GET",
                API + "/user/repos?sort=updated&per_page=15", token, null));
        List<String> result = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            result.add(item.optString("full_name") + " · "
                    + (item.optBoolean("private") ? "private" : "public"));
        }
        return result;
    }

    static String createPrivateGist(String token, String filename, String description,
                                    String content) throws Exception {
        JSONObject file = new JSONObject();
        file.put("content", content);
        JSONObject files = new JSONObject();
        files.put(filename, file);
        JSONObject body = new JSONObject();
        body.put("description", description);
        body.put("public", false);
        body.put("files", files);
        JSONObject result = new JSONObject(call("POST", API + "/gists", token, body));
        return result.optString("html_url", "");
    }

    private static String call(String method, String url, String token, JSONObject body)
            throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
            connection.setRequestProperty("User-Agent", "dpLab-Android");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String response = read(stream);
            if (status < 200 || status >= 300) {
                throw new IOException("GitHub API HTTP " + status + ": "
                        + (response.length() > 200 ? response.substring(0, 200) : response));
            }
            return response;
        } finally {
            connection.disconnect();
        }
    }

    private static String read(InputStream input) throws IOException {
        if (input == null) return "";
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}
