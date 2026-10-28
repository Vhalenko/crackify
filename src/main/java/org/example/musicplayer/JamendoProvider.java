package org.example.musicplayer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class JamendoProvider implements MusicProvider {

    private static final String CLIENT_ID = "764a2f77";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<Track> search(String query) throws Exception {
        String url = "https://api.jamendo.com/v3.0/tracks/"
                + "?client_id=" + CLIENT_ID
                + "&format=json&limit=30&imagesize=600"
                + "&search=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Jamendo: HTTP " + response.statusCode());
        }

        List<Track> tracks = new ArrayList<>();
        for (JsonNode n : mapper.readTree(response.body()).path("results")) {
            String audio = n.path("audio").asText("");
            if (audio.isEmpty()) continue;
            tracks.add(new Track(
                    n.path("id").asText(),
                    n.path("name").asText("Unknown"),
                    n.path("artist_name").asText("Unknown"),
                    n.path("image").asText(""),
                    audio,
                    n.path("duration").asInt()));
        }
        return tracks;
    }
}