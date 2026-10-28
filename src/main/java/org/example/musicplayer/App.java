package org.example.musicplayer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.List;
import java.util.Locale;

public class App extends Application {

    private static final PseudoClass CURRENT = PseudoClass.getPseudoClass("current");

    private final MusicProvider provider = new JamendoProvider();
    private final PlayerService player = new PlayerService();

    @Override
    public void start(Stage stage) {
        // ---------- верхняя панель ----------
        Label prompt = new Label(">");
        prompt.getStyleClass().add("prompt");

        TextField search = new TextField();
        search.setId("search");
        search.setPromptText("search tracks");
        HBox.setHgrow(search, Priority.ALWAYS);

        Label status = new Label("[enter] search");
        status.getStyleClass().add("muted");
        status.setMaxWidth(260);

        HBox top = new HBox(10, prompt, search, status);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("top-bar");

        // ---------- обложка ----------
        ImageView coverView = new ImageView();
        coverView.setPreserveRatio(true);
        coverView.setSmooth(true);

        Label noCover = new Label("[ no cover ]");
        noCover.getStyleClass().add("muted");
        noCover.visibleProperty().bind(coverView.imageProperty().isNull());

        StackPane cover = new StackPane(noCover, coverView);
        cover.getStyleClass().add("panel");

        // ---------- now playing ----------
        Label nowTitle = new Label("[ now playing ]");
        nowTitle.getStyleClass().add("panel-title");

        Label title = new Label("nothing playing");
        title.setId("title");
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);

        Label artist = new Label("");
        artist.setId("artist");
        artist.setWrapText(true);
        artist.setMaxWidth(Double.MAX_VALUE);

        Label counter = new Label("");
        counter.getStyleClass().add("muted");

        VBox nowPlaying = new VBox(8, nowTitle, title, artist, counter);
        nowPlaying.getStyleClass().addAll("panel", "pad");

        // ---------- очередь ----------
        Label queueTitle = new Label("[ queue ]");
        queueTitle.getStyleClass().add("panel-title");

        ListView<Track> queueView = new ListView<>(player.queue());
        queueView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Track t, boolean empty) {
                super.updateItem(t, empty);
                boolean current = !empty && t != null
                        && getIndex() == player.currentIndexProperty().get();
                pseudoClassStateChanged(CURRENT, current);
                if (empty || t == null) {
                    setText(null);
                    return;
                }
                setText("%s %02d  %s - %s".formatted(
                        current ? ">" : " ", getIndex() + 1, t.title(), t.artist()));
            }
        });
        Runnable playSelected = () -> player.playAt(queueView.getSelectionModel().getSelectedIndex());
        queueView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) playSelected.run();
        });
        queueView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) playSelected.run();
        });
        VBox.setVgrow(queueView, Priority.ALWAYS);

        VBox queuePanel = new VBox(8, queueTitle, queueView);
        queuePanel.getStyleClass().addAll("panel", "pad");
        VBox.setVgrow(queuePanel, Priority.ALWAYS);

        VBox side = new VBox(12, nowPlaying, queuePanel);
        side.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(side, Priority.ALWAYS);

        // ---------- тело ----------
        HBox body = new HBox(20, cover, side);
        body.setPadding(new Insets(20));
        body.setAlignment(Pos.CENTER_LEFT);

        // ---------- нижняя панель ----------
        Button prev = new Button("[ << ]");
        Button playPause = new Button("[ > ]");
        playPause.setId("play");
        Button next = new Button("[ >> ]");
        HBox controls = new HBox(8, prev, playPause, next);
        controls.setAlignment(Pos.CENTER_LEFT);

        Label elapsed = new Label("0:00");
        Label total = new Label("0:00");
        elapsed.getStyleClass().add("muted");
        total.getStyleClass().add("muted");
        Slider seek = new Slider(0, 1, 0);
        HBox.setHgrow(seek, Priority.ALWAYS);
        HBox timeRow = new HBox(10, elapsed, seek, total);
        timeRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(timeRow, Priority.ALWAYS);

        Label volLabel = new Label("vol");
        volLabel.getStyleClass().add("muted");
        Slider volume = new Slider(0, 1, 0.8);
        volume.setPrefWidth(90);
        volume.setMinWidth(90);
        volume.valueProperty().bindBidirectional(player.volumeProperty());
        HBox volumeBox = new HBox(8, volLabel, volume);
        volumeBox.setAlignment(Pos.CENTER);

        HBox bottom = new HBox(20, controls, timeRow, volumeBox);
        bottom.setAlignment(Pos.CENTER);
        bottom.getStyleClass().add("bottom-bar");

        paintProgress(seek);
        paintProgress(volume);

        // ---------- сцена ----------
        BorderPane root = new BorderPane(body, top, null, bottom, null);
        root.setStyle("-fx-background-color: #0b0d10; -fx-font-family: '"
                + pickMonoFont() + "'; -fx-font-size: 13px;");

        Scene scene = new Scene(root, 1000, 600);
        scene.setFill(Color.web("#0b0d10"));
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        // ---------- адаптивность ----------
        DoubleBinding coverSize = Bindings.createDoubleBinding(
                () -> Math.max(160, Math.min(body.getHeight() - 40, body.getWidth() * 0.42)),
                body.widthProperty(), body.heightProperty());
        for (var p : List.of(cover.minWidthProperty(), cover.prefWidthProperty(), cover.maxWidthProperty(),
                cover.minHeightProperty(), cover.prefHeightProperty(), cover.maxHeightProperty())) {
            p.bind(coverSize);
        }
        coverView.fitWidthProperty().bind(coverSize.subtract(2));
        coverView.fitHeightProperty().bind(coverSize.subtract(2));

        queuePanel.visibleProperty().bind(scene.widthProperty().greaterThan(820));
        queuePanel.managedProperty().bind(queuePanel.visibleProperty());
        volumeBox.visibleProperty().bind(scene.widthProperty().greaterThan(640));
        volumeBox.managedProperty().bind(volumeBox.visibleProperty());

        // ---------- поиск ----------
        search.setOnAction(e -> {
            String query = search.getText().trim();
            if (query.isEmpty()) return;
            status.setText("searching...");

            Thread worker = new Thread(() -> {
                try {
                    List<Track> tracks = provider.search(query);
                    Platform.runLater(() -> {
                        if (tracks.isEmpty()) {
                            status.setText("nothing found");
                        } else {
                            status.setText(tracks.size() + " tracks");
                            player.setQueue(tracks);
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> status.setText("error: " + ex.getMessage()));
                }
            }, "search");
            worker.setDaemon(true);
            worker.start();
        });

        // ---------- плеер -> интерфейс ----------
        player.currentTrackProperty().addListener((o, old, t) -> {
            if (t == null) return;
            title.setText(t.title());
            artist.setText(t.artist());
            coverView.setImage(t.coverUrl().isEmpty() ? null : new Image(t.coverUrl(), true));
        });
        player.currentIndexProperty().addListener((o, a, v) -> {
            int i = v.intValue();
            queueView.refresh();
            if (i >= 0) {
                counter.setText("track %02d / %02d".formatted(i + 1, player.queue().size()));
                queueView.scrollTo(i);
            } else {
                counter.setText("");
            }
        });
        player.playingProperty().addListener((o, a, isPlaying) ->
                playPause.setText(isPlaying ? "[ || ]" : "[ > ]"));
        player.totalSecondsProperty().addListener((o, a, v) -> {
            seek.setMax(Math.max(1, v.doubleValue()));
            total.setText(formatTime(v.doubleValue()));
        });
        player.currentSecondsProperty().addListener((o, a, v) -> {
            if (!seek.isPressed()) seek.setValue(v.doubleValue());
            elapsed.setText(formatTime(v.doubleValue()));
        });

        // ---------- интерфейс -> плеер ----------
        prev.setOnAction(e -> player.previous());
        next.setOnAction(e -> player.next());
        playPause.setOnAction(e -> player.togglePause());
        seek.pressedProperty().addListener((o, was, pressed) -> {
            if (!pressed) player.seek(seek.getValue());
        });

        stage.setTitle("music-player");
        stage.setScene(scene);
        stage.setMinWidth(520);
        stage.setMinHeight(380);
        stage.show();
    }

    /** Подсвечивает пройденную часть дорожки ползунка. */
    private static void paintProgress(Slider s) {
        Runnable update = () -> {
            Node track = s.lookup(".track");
            if (track == null) return;
            double range = s.getMax() - s.getMin();
            double pct = range <= 0 ? 0 : (s.getValue() - s.getMin()) / range * 100;
            track.setStyle(String.format(Locale.ROOT,
                    "-fx-background-color: linear-gradient(to right, #5eead4 %1$.1f%%, #222831 %1$.1f%%);",
                    pct));
        };
        s.valueProperty().addListener((o, a, v) -> update.run());
        s.maxProperty().addListener((o, a, v) -> update.run());
        s.skinProperty().addListener((o, a, v) -> Platform.runLater(update));
    }

    /** Выбирает первый установленный моноширинный шрифт: так выглядит одинаково на Windows и Linux. */
    private static String pickMonoFont() {
        List<String> available = Font.getFamilies();
        for (String f : List.of("JetBrains Mono", "Cascadia Mono", "Consolas",
                "DejaVu Sans Mono", "Menlo", "Liberation Mono")) {
            if (available.contains(f)) return f;
        }
        return "Monospaced";
    }

    private static String formatTime(double seconds) {
        int t = (int) seconds;
        return "%d:%02d".formatted(t / 60, t % 60);
    }

    public static void main(String[] args) {
        launch(args);
    }
}