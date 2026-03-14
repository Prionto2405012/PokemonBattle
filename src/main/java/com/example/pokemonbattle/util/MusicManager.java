package com.example.pokemonbattle.util;

import java.net.URL;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class MusicManager {
    private static final String[] BGM_TRACKS = {
        "/com/example/pokemonbattle/audio/gen1.mp3",
        "/com/example/pokemonbattle/audio/gen2.mp3",
        "/com/example/pokemonbattle/audio/gen3.mp3"
    };
    private static final String CLICK= "/com/example/pokemonbattle/audio/click.mp3";
    private static final String VICTORY= "/com/example/pokemonbattle/audio/victory.mp3";
    private static final ExecutorService BGM_OP = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MusicManager-BGMThread");
        t.setDaemon(true);
        return t;
    });

    private static MusicManager instance;

    private final AtomicReference<MediaPlayer> bgmRef = new AtomicReference<>();
    private AudioClip clickClip;
    private AudioClip victoryClip;
    private volatile double  masterVolume  = 0.2;
    private volatile boolean soundEnabled  = true;
    private volatile String  currentTrackPath;

    private MusicManager() {
        clickClip   = buildAudioClip(CLICK);
        victoryClip = buildAudioClip(VICTORY);
    }
    public static MusicManager getInstance() {
        if (instance == null) instance = new MusicManager();
        return instance;
    }
    public void playRandomBGM() {
        switchBGM(BGM_TRACKS[new Random().nextInt(BGM_TRACKS.length)]);
    }
    public void switchBGM(String resourcePath) {
        currentTrackPath = resourcePath;
        if (soundEnabled) startBGM(resourcePath);
    }
    public void playBGM(String resourcePath) { switchBGM(resourcePath); }

    private void startBGM(String path) {
        BGM_OP.execute(() -> {
            MediaPlayer old = bgmRef.getAndSet(null);
            if (old != null) { old.stop(); old.dispose(); }

            URL url = getClass().getResource(path);
            if (url == null) { System.err.println("[MusicManager] BGM not found: " + path); return; }
            MediaPlayer player = new MediaPlayer(new Media(url.toExternalForm()));
            player.setVolume(masterVolume);
            player.setCycleCount(MediaPlayer.INDEFINITE);
            bgmRef.set(player);
            Runnable tryPlay = () -> {
                if (soundEnabled && bgmRef.get() == player) player.play();
            };
            player.setOnReady(tryPlay);
            if (player.getStatus() == MediaPlayer.Status.READY) tryPlay.run();
        });
    }
    public void stopBGM() {
        BGM_OP.execute(() -> {
            MediaPlayer old = bgmRef.getAndSet(null);
            if (old != null) { old.stop(); old.dispose(); }
        });
    }
    public void pauseBGM() {
        MediaPlayer p = bgmRef.get();
        if (p != null) p.pause();
    }
    public void resumeBGM() {
        MediaPlayer p = bgmRef.get();
        if (p != null && soundEnabled) p.play();
    }
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
        if (!enabled) stopBGM();
        else if (currentTrackPath != null) startBGM(currentTrackPath);
        else playRandomBGM();
    }
    public void setMasterVolume(double volume) {
        this.masterVolume = Math.max(0.0, Math.min(1.0, volume));
        MediaPlayer p = bgmRef.get();
        if (p != null) p.setVolume(this.masterVolume);
    }
    public void setBGMVolume(double v) { setMasterVolume(v); }
    public void playClickSFX() {
        if (!soundEnabled || clickClip == null) return;
        clickClip.setVolume(masterVolume * 0.5);
        clickClip.play();   
    }

    public void playVictorySFX() {
        if (!soundEnabled || victoryClip == null) return;
        victoryClip.setVolume(masterVolume);
        victoryClip.play();
    }
    public void playSFX(String resourcePath) {
        if (!soundEnabled) return;
        AudioClip clip = buildAudioClip(resourcePath);
        if (clip != null) { clip.setVolume(masterVolume); clip.play(); }
    }

    private AudioClip buildAudioClip(String resourcePath) {
        URL url = getClass().getResource(resourcePath);
        if (url == null) { System.err.println("[MusicManager] SFX not found: " + resourcePath); return null; }
        return new AudioClip(url.toExternalForm());
    }
    public void attachClickSounds(Parent root) {
        walkButtons(root);
    }

    private void walkButtons(Parent parent) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Button btn && !"click-wired".equals(btn.getUserData())) {
                var existing = btn.getOnAction();
                btn.setOnAction(e -> {
                    playClickSFX();        
                    if (existing != null) existing.handle(e);
                });
                if (btn.getUserData() == null) btn.setUserData("click-wired");
            }
            if (child instanceof Parent p) walkButtons(p);
        }
    }
    public boolean isSoundEnabled(){return soundEnabled;}
    public double getMasterVolume(){return masterVolume;}
    public double getBGMVolume(){return masterVolume;}
    public String getCurrentTrack(){return currentTrackPath;}
}