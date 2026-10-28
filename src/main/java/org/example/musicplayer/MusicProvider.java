package org.example.musicplayer;

import java.util.List;

public interface MusicProvider {
    List<Track> search(String query) throws Exception;
}
