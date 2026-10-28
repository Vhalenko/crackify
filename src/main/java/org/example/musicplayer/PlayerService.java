package org.example.musicplayer;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.util.List;

public class PlayerService {

    private final ObservableList<Track> queue = FXCollections.observableArrayList();
    private final IntegerProperty currentIndex = new SimpleIntegerProperty(-1);
    private final ObjectProperty<Track> currentTrack = new SimpleObjectProperty<>();
    private final BooleanProperty playing = new SimpleBooleanProperty(false);
    private final DoubleProperty currentSeconds = new SimpleDoubleProperty();
    private final DoubleProperty totalSeconds = new SimpleDoubleProperty();
    private final DoubleProperty volume = new SimpleDoubleProperty(0.8);

    private MediaPlayer player;

    public ObservableList<Track> queue() { return queue; }
    public IntegerProperty currentIndexProperty() { return currentIndex; }
    public ObjectProperty<Track> currentTrackProperty() { return currentTrack; }
    public BooleanProperty playingProperty() { return playing; }
    public DoubleProperty currentSecondsProperty() { return currentSeconds; }
    public DoubleProperty totalSecondsProperty() { return totalSeconds; }
    public DoubleProperty volumeProperty() { return volume; }

    public void setQueue(List<Track> tracks) {
        currentIndex.set(-1);
        queue.setAll(tracks);
        if (!queue.isEmpty()) playAt(0);
    }

    public void playAt(int i) {
        if (i < 0 || i >= queue.size()) return;
        if (player != null) {
            player.stop();
            player.dispose();
        }
        Track track = queue.get(i);
        currentIndex.set(i);
        currentTrack.set(track);
        currentSeconds.set(0);
        totalSeconds.set(track.durationSeconds());

        MediaPlayer mp = new MediaPlayer(new Media(track.streamUrl()));
        player = mp;
        mp.volumeProperty().bind(volume);
        mp.currentTimeProperty().addListener((o, a, v) -> currentSeconds.set(v.toSeconds()));
        mp.statusProperty().addListener((o, a, s) -> playing.set(s == MediaPlayer.Status.PLAYING));
        mp.setOnReady(() -> totalSeconds.set(mp.getTotalDuration().toSeconds()));
        mp.setOnEndOfMedia(this::next);
        mp.setOnError(() -> System.err.println("Media error: " + mp.getError()));
        mp.setAutoPlay(true);
    }

    public void togglePause() {
        if (player == null) return;
        if (playing.get()) player.pause(); else player.play();
    }

    public void next() {
        playAt(currentIndex.get() + 1);
    }

    public void previous() {
        if (player == null) return;
        if (currentSeconds.get() > 3 || currentIndex.get() == 0) {
            player.seek(Duration.ZERO);
        } else {
            playAt(currentIndex.get() - 1);
        }
    }

    public void seek(double seconds) {
        if (player != null) player.seek(Duration.seconds(seconds));
    }
}