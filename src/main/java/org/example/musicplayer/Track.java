package org.example.musicplayer;

public record Track(String id, String title, String artist,
                    String coverUrl, String streamUrl, int durationSeconds) {
}
