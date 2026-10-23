package com.cristiantabares.orionsjourney;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import java.util.HashMap;
import java.util.Map;

public class AudioController {
    private final Context context;
    private final SoundPool soundPool;
    private final Map<String, Integer> sfxMap = new HashMap<>();
    private MediaPlayer musicPlayer;
    private String currentMusic = "";
    private boolean muted = false;

    public AudioController(Context context) {
        this.context = context;
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        this.soundPool = new SoundPool.Builder()
                .setMaxStreams(10)
                .setAudioAttributes(attrs)
                .build();

        preloadSfx("CannonLaunch.wav");
        preloadSfx("puffleOcollect.wav");
        preloadSfx("GiantPuffleOReached.wav");
        preloadSfx("Splash.wav");
        preloadSfx("CloudFall.wav");
        preloadSfx("PianoHit.wav");
        preloadSfx("AnvilHit.wav");
        preloadSfx("CactusHit.wav");
        preloadSfx("GreenBalloonBump.wav");
        preloadSfx("PinkBalloonBump.wav");
        preloadSfx("BossHit.wav");
        preloadSfx("BossExplosion.wav");
        preloadSfx("BossThruster.wav");
        preloadSfx("SwingAnvil.wav");
        preloadSfx("Menu_Click24.wav");
        preloadSfx("CPW_PuffleCannon_SlingShot_Release01_x3_TwangRubber_CTE02_71.2-TiSh_01-01_sfx.wav");
        preloadSfx("CPW_PuffleCannon_SlingShot_Stretch01_x2_Stretch_Twist2-TiSh_01-01_sfx.wav");
    }

    private void preloadSfx(String fileName) {
        try {
            AssetFileDescriptor afd = context.getAssets().openFd("audio/" + fileName);
            int id = soundPool.load(afd, 1);
            sfxMap.put(fileName, id);
            afd.close();
        } catch (Exception ignored) {
        }
    }

    public void playSfx(String fileName) {
        if (muted) return;
        Integer id = sfxMap.get(fileName);
        if (id != null) {
            soundPool.play(id, 0.9f, 0.9f, 1, 0, 1.0f);
        }
    }

    public void playSfxPitch(String fileName, float rate) {
        if (muted) return;
        Integer id = sfxMap.get(fileName);
        if (id != null) {
            float clamped = Math.max(0.5f, Math.min(2.0f, rate));
            soundPool.play(id, 0.85f, 0.85f, 1, 0, clamped);
        }
    }

    public synchronized void playMusic(String musicFile, boolean loop) {
        if (muted) {
            currentMusic = musicFile;
            stopMusicInternal();
            return;
        }
        if (musicFile.equals(currentMusic) && musicPlayer != null && musicPlayer.isPlaying()) {
            return;
        }
        stopMusicInternal();
        currentMusic = musicFile;
        try {
            AssetFileDescriptor afd = context.getAssets().openFd("audio/" + musicFile);
            musicPlayer = new MediaPlayer();
            musicPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            musicPlayer.setLooping(loop);
            musicPlayer.setVolume(0.65f, 0.65f);
            musicPlayer.prepare();
            musicPlayer.start();
        } catch (Exception ignored) {
        }
    }

    private void stopMusicInternal() {
        if (musicPlayer != null) {
            try {
                if (musicPlayer.isPlaying()) {
                    musicPlayer.stop();
                }
            } catch (Exception ignored) {
            }
            try {
                musicPlayer.release();
            } catch (Exception ignored) {
            }
            musicPlayer = null;
        }
    }

    public synchronized void pauseMusic() {
        if (musicPlayer != null && musicPlayer.isPlaying()) {
            try {
                musicPlayer.pause();
            } catch (Exception ignored) {
            }
        }
    }

    public synchronized void resumeMusic() {
        if (!muted && musicPlayer != null && !musicPlayer.isPlaying()) {
            try {
                musicPlayer.start();
            } catch (Exception ignored) {
            }
        } else if (!muted && musicPlayer == null && !currentMusic.isEmpty()) {
            playMusic(currentMusic, true);
        }
    }

    public synchronized void setMuted(boolean muted) {
        this.muted = muted;
        if (muted) {
            stopMusicInternal();
        } else if (!currentMusic.isEmpty()) {
            String target = currentMusic;
            currentMusic = "";
            playMusic(target, true);
        }
    }

    public boolean isMuted() {
        return muted;
    }

    public synchronized void release() {
        stopMusicInternal();
        soundPool.release();
    }
}
