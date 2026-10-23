package com.cristiantabares.orionsjourney;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SpriteCache {
    private final Context context;
    private final Map<String, Bitmap> cache = new HashMap<>();

    public Bitmap orionDefault;
    public final Bitmap[] orionSuits = new Bitmap[10];
    public final Bitmap[] orionFireFrames = new Bitmap[6];
    public Bitmap cannonGreen;
    public Bitmap cannonBlue;
    public Bitmap cannonPurple;
    public Bitmap cannonRed;
    public Bitmap cannonCheckpoint;
    public Bitmap checkpointFlag;

    public Bitmap ringOrange;
    public Bitmap ringSilver;
    public Bitmap ringGold;
    public Bitmap ringFire;
    public Bitmap giantFinishRing;

    public Bitmap piano;
    public Bitmap anvil;
    public Bitmap cactus;
    public Bitmap plant;
    public final Bitmap[] fanFrames = new Bitmap[3];
    public Bitmap greenBalloon;
    public Bitmap redBalloon;
    public Bitmap slingshotBalloons;
    public Bitmap slingshotBarrel;
    public Bitmap slingshotRope;
    public Bitmap poofyCloudOrange;
    public Bitmap poofyCloudPuff;
    public Bitmap bossShip1;
    public Bitmap bossShip2;
    public Bitmap magnet;
    public Bitmap crabbyClaw;
    public final Bitmap[] bgClouds = new Bitmap[12];
    public final Bitmap[] worldScenes = new Bitmap[3];

    public Bitmap slowMoBtnActive;
    public Bitmap slowMoBtnInactive;
    public Bitmap btnBackImg;
    public Bitmap turboIcon;
    public Bitmap clockGold;
    public Bitmap totalPuffleOBg;
    public Bitmap totalTimeBg;
    public Bitmap playBtn;
    public final Bitmap[] levelSelectBtns = new Bitmap[3];
    public final Bitmap[] worldTabs = new Bitmap[3];
    public Bitmap endLevelPopup;
    public Bitmap endLevelNextBtn;
    public Bitmap endLevelReplayBtn;
    public Bitmap endLevelMenuBtn;
    public Bitmap pauseIconBtn;
    public Bitmap pauseResumeBtn;
    public Bitmap pauseRestartBtn;
    public Bitmap pauseQuitBtn;

    public SpriteCache(Context context) {
        this.context = context;
        loadAll();
    }

    private void loadAll() {
        // Load the 10 individual, centered, zero-residue Orion suits directly
        String[] suitFiles = {
            "orion_suit_01_solar.png",
            "orion_suit_02_cosmic.png",
            "orion_suit_03_frost.png",
            "orion_suit_04_amber.png",
            "orion_suit_05_terra.png",
            "orion_suit_06_azure.png",
            "orion_suit_07_crimson.png",
            "orion_suit_08_starlight.png",
            "orion_suit_09_onyx.png",
            "orion_suit_10_emerald.png"
        };
        for (int i = 0; i < 10; i++) {
            orionSuits[i] = loadRaw(suitFiles[i]);
        }
        orionDefault = orionSuits[5]; // Azure Suit default

        // Load the 6 individual flaming Orion animation frames directly
        for (int i = 0; i < 6; i++) {
            orionFireFrames[i] = loadRaw("orion_fire_0" + (i + 1) + ".png");
        }

        // Cannons & Checkpoint
        cannonGreen = loadRaw("cannon_green.png");
        cannonBlue = loadRaw("cannon_blue.png");
        cannonPurple = loadRaw("cannon_purple.png");
        cannonRed = loadRaw("cannon_red.png");
        cannonCheckpoint = loadRaw("cannon_checkpoint.png");
        checkpointFlag = loadRaw("checkpoint_flag.png");

        // Collectible Rings & Giant Finish Ring
        ringOrange = loadRaw("ring_orange.png");
        ringSilver = loadRaw("ring_silver.png");
        ringGold = loadRaw("ring_gold.png");
        ringFire = loadRaw("ring_fire.png");
        giantFinishRing = loadRaw("giant_ring.png");
        if (giantFinishRing == null) giantFinishRing = ringGold;

        // Obstacles & Interactive Objects
        piano = loadRaw("obstacle_piano.png");
        anvil = loadRaw("obstacle_anvil.png");
        cactus = loadRaw("obstacle_cactus.png");
        plant = loadRaw("plant_01.png");

        fanFrames[0] = loadRaw("fan_01.png");
        fanFrames[1] = loadRaw("fan_02.png");
        fanFrames[2] = loadRaw("fan_03.png");

        greenBalloon = loadRaw("balloon_green.png");
        redBalloon = loadRaw("balloon_red.png");
        slingshotBalloons = loadTrimmed("SlingshotBalloons_texture_01.png");
        slingshotBarrel = loadTrimmed("SlingshotBarrel_texture_01.png");
        slingshotRope = loadTrimmed("SlingshotRope_texture_01.png");

        poofyCloudOrange = loadRaw("cloud_poofy.png");
        poofyCloudPuff = poofyCloudOrange;

        bossShip1 = loadRaw("boss_ship_1.png");
        bossShip2 = loadRaw("boss_ship_2.png");
        magnet = loadRaw("boss_magnet.png");

        Bitmap crabSheet = loadRaw("Freefall_texture_01.png");
        if (crabSheet != null) {
            Bitmap trimmedCrab = trimTransparent(crabSheet);
            crabbyClaw = trimTransparent(safeCrop(trimmedCrab, 0, 0, Math.max(1, trimmedCrab.getWidth() / 12), Math.max(1, trimmedCrab.getHeight() / 4)));
        }

        // Background clouds & environment art
        String[] bgNames = {
            "bgArt01_texture_00.png", "bgArt02_texture_00.png", "bgArt03_texture_00.png", "bgArt04_texture_00.png",
            "bgArt07_texture_00.png", "bgArt09_texture_00.png", "bgArt18_texture_00.png", "bgArt25_texture_00.png",
            "bgArt28_texture_01.png", "bgArt32_texture_01.png", "bgArt42_texture_01.png", "bgArt53_texture_01.png"
        };
        for (int i = 0; i < bgNames.length; i++) {
            bgClouds[i] = loadTrimmed(bgNames[i]);
        }

        // World Scenes & UI Buttons
        worldScenes[0] = loadRaw("bluesky_loading.png");
        worldScenes[1] = loadRaw("sodasunset_loading.png");
        worldScenes[2] = loadRaw("bonus_loading.png");
        slowMoBtnActive = loadTrimmed("SlowMoButton_Active.png");
        slowMoBtnInactive = loadTrimmed("SlowMoButton_Inactive.png");
        btnBackImg = loadTrimmed("button_back.png");
        turboIcon = loadTrimmed("TurboMode.png");
        clockGold = loadTrimmed("Clock_Gold.png");
        totalPuffleOBg = loadTrimmed("total_puffle-o_bg.png");
        totalTimeBg = loadTrimmed("total_time_bg.png");
        playBtn = loadTrimmed("play_button.png");

        // Clean single-button Level Select buttons (zero neighbor bleed)
        levelSelectBtns[0] = loadRaw("level_btn_blue.png");
        levelSelectBtns[1] = loadRaw("level_btn_red.png");
        levelSelectBtns[2] = loadRaw("level_btn_yellow.png");

        worldTabs[0] = loadTrimmed("world_tab_blue-front.png");
        worldTabs[1] = loadTrimmed("world_tab_red-front.png");
        worldTabs[2] = loadTrimmed("world_tab_yellow-front.png");

        endLevelPopup = loadTrimmed("end-level_popup.png");
        endLevelNextBtn = loadTrimmed("end-level_popup_nextlevel-button.png");
        endLevelReplayBtn = loadTrimmed("end-level_replay_button.png");
        endLevelMenuBtn = loadTrimmed("end-level_menu_button.png");
        pauseIconBtn = loadTrimmed("pause_button.png");
        pauseResumeBtn = loadTrimmed("pause_resume.png");
        pauseRestartBtn = loadTrimmed("pause_restart.png");
        pauseQuitBtn = loadTrimmed("pause_quit.png");
    }

    public Bitmap getStorySlide(int planoNumber) {
        String key = "story_plano_" + planoNumber;
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        try (InputStream is = context.getAssets().open("story/plano" + planoNumber + ".jpg")) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bmp = BitmapFactory.decodeStream(is, null, opts);
            if (bmp != null) {
                cache.put(key, bmp);
            }
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap loadRaw(String name) {
        if (cache.containsKey(name)) {
            return cache.get(name);
        }
        try (InputStream is = context.getAssets().open("sprites/" + name)) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bmp = BitmapFactory.decodeStream(is, null, opts);
            if (bmp != null) {
                cache.put(name, bmp);
            }
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap loadTrimmed(String name) {
        String key = "trim_" + name;
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        Bitmap raw = loadRaw(name);
        if (raw == null) return null;
        Bitmap trimmed = trimTransparent(raw);
        cache.put(key, trimmed);
        return trimmed;
    }

    private static Bitmap safeCrop(Bitmap src, int x, int y, int w, int h) {
        if (src == null) return null;
        int x0 = Math.max(0, Math.min(src.getWidth() - 1, x));
        int y0 = Math.max(0, Math.min(src.getHeight() - 1, y));
        int cw = Math.max(1, Math.min(src.getWidth() - x0, w));
        int ch = Math.max(1, Math.min(src.getHeight() - y0, h));
        return Bitmap.createBitmap(src, x0, y0, cw, ch);
    }

    public static Bitmap trimTransparent(Bitmap src) {
        if (src == null) return null;
        int w = src.getWidth();
        int h = src.getHeight();
        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);

        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            int rowOffset = y * w;
            for (int x = 0; x < w; x++) {
                int alpha = (pixels[rowOffset + x] >>> 24) & 0xFF;
                if (alpha > 12) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return src;
        }
        return Bitmap.createBitmap(src, minX, minY, (maxX - minX + 1), (maxY - minY + 1));
    }
}
