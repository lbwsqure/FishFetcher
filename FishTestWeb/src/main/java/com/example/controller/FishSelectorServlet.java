package com.example.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
@WebServlet("/fishselector/*")
public class FishSelectorServlet extends HttpServlet {

    private static final String SPECIES_API_BASE_URL = "https://api.gbif.org/v1/species";
    private static final String OCCURRENCE_API_BASE_URL = "https://api.gbif.org/v1/occurrence/search";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String path = request.getPathInfo(); // 获取路径，例如 /fishname

        if (path == null || path.length() <= 1) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Fish name is required in the URL path.\"}");
            return;
        }

        String fishName = path.substring(1); // 提取鱼的科学名称

        try {
            Integer taxonKey = fetchTaxonKey(fishName);
            if (taxonKey == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\": \"No taxon key found for fish: " + fishName + "\"}");
                return;
            }

            JsonArray distributionData = fetchDistributionData(taxonKey);
            String mapHtml = generateLeafletMapHtml(distributionData, fishName);

            JsonObject jsonResponse = new JsonObject();
            jsonResponse.addProperty("fishName", fishName);
            jsonResponse.add("distributionData", distributionData);
            jsonResponse.addProperty("mapHtml", mapHtml);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(new Gson().toJson(jsonResponse));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Failed to fetch fish distribution data\"}");
            e.printStackTrace();
        }
    }

    private Integer fetchTaxonKey(String fishName) throws IOException {
        String queryUrl = SPECIES_API_BASE_URL + "?name=" + fishName.replace(" ", "%20");
        URL url = new URL(queryUrl);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }
        conn.disconnect();

        JsonObject jsonResponse = new Gson().fromJson(response.toString(), JsonObject.class);
        JsonArray results = jsonResponse.getAsJsonArray("results");
        if (results != null && results.size() > 0) {
            return results.get(0).getAsJsonObject().get("key").getAsInt();
        }
        return null;
    }

    private JsonArray fetchDistributionData(Integer taxonKey) throws IOException {
        String queryUrl = OCCURRENCE_API_BASE_URL + "?taxonKey=" + taxonKey + "&limit=300";
        URL url = new URL(queryUrl);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }
        conn.disconnect();

        JsonObject jsonResponse = new Gson().fromJson(response.toString(), JsonObject.class);
        return jsonResponse.getAsJsonArray("results");
    }

    private String generateLeafletMapHtml(JsonArray locations, String fishName) {
        StringBuilder markers = new StringBuilder();
        for (int i = 0; i < locations.size(); i++) {
            JsonObject location = locations.get(i).getAsJsonObject();
            if (location.has("decimalLatitude") && location.has("decimalLongitude")) {
                double latitude = location.get("decimalLatitude").getAsDouble();
                double longitude = location.get("decimalLongitude").getAsDouble();
                markers.append("L.marker([").append(latitude).append(", ").append(longitude).append("]).addTo(map);\n");
            }
        }

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "  <title>Fish Distribution - " + fishName + "</title>" +
                "  <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />" +
                "  <script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>" +
                "  <style>#map { width: 100%; height: 100%; }</style>" +
                "</head>" +
                "<body>" +
                "  <h1>Distribution of " + fishName + "</h1>" +
                "  <div id=\"map\" style=\"width: 100%; height: 500px;\"></div>" +
                "  <script>" +
                "    var map = L.map('map').setView([39.8283, -98.5795], 3);" +
                "    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);" +
                markers +
                "  </script>" +
                "</body>" +
                "</html>";
    }
}
