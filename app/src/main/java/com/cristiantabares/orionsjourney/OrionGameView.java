package com.cristiantabares.orionsjourney;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;
import java.util.List;

public class OrionGameView extends SurfaceView implements SurfaceHolder.Callback, Runnable, ScaleGestureDetector.OnScaleGestureListener {

    public static final int STATE_MAIN_MENU = 0;
    public static final int STATE_WORLD_SELECT = 1;
    public static final int STATE_LEVEL_SELECT = 2;
    public static final int STATE_PLAYING = 3;
    public static final int STATE_PAUSED = 4;
    public static final int STATE_LEVEL_COMPLETE = 5;
    public static final int STATE_STORY_CUTSCENE = 6;

    private static final int ORION_FREE = 0;
    private static final int ORION_IN_CANNON = 1;
    private static final int ORION_IN_SLINGSHOT = 2;

    private final Context context;
    private final AudioController audio;
    private final SpriteCache sprites;
    private final SharedPreferences prefs;
    private final ScaleGestureDetector scaleDetector;

    private Thread gameThread;
    private volatile boolean running = false;

    private int gameState = STATE_MAIN_MENU;
    private int selectedWorld = 1; // 1..3
    private int currentLevelNum = 1; // 1..36
    private int selectedSuitIndex = 6; // Default Crimson Suit
    private boolean tiltControls = false;
    private boolean turboMode = false;
    private boolean langEs = true; // Español default, toggleable on Main Menu
    private boolean slowMoActive = false;
    private float tiltInputX = 0f;
    private float tiltInputY = 0f;
    private float parallaxX = 0f;
    private float parallaxY = 0f;

    // Progress persistence & 90% ring requirement
    private final int[] bestRings = new int[37];
    private final int[] levelTotalRings = new int[37];
    private final boolean[] levelCleared = new boolean[37];
    private String statusBannerText = "";
    private float statusBannerTimer = 0f;

    // Current Level & Orion state
    private LevelData level;
    private int orionState = ORION_FREE;
    private float orionX, orionY;
    private float orionVx, orionVy;
    private float orionAngleDeg = 0f;
    private float currentSpawnX, currentSpawnY;
    private LevelData.CannonEntity currentCannon = null;
    private LevelData.CannonEntity lastLaunchedCannon = null;
    private float cannonIgnoreTimer = 0f;
    private float autoLaunchDelay = 0f;

    // Slingshot state
    private LevelData.ObstacleEntity currentSlingshot = null;
    private boolean draggingSlingshot = false;
    private float slingPullDx = 0f, slingPullDy = 0f;
    private float dragStartScreenX = 0f, dragStartScreenY = 0f;

    // Purple controllable cannon drag state
    private boolean draggingPurpleCannon = false;
    private float purpleLastTouchAngle = 0f;
    private boolean purpleMovedEnough = false;

    // In-game stats
    private int collectedRings = 0;
    private int totalLevelRings = 0;
    private float levelTimerSec = 0f;
    private float animClock = 0f;

    // Camera state
    private float camX = 0f, camY = 0f;
    private float camZoom = 1.0f;
    private float userZoomMultiplier = 1.0f;
    private boolean touchLeftDown = false;
    private boolean touchRightDown = false;

    // Trail particles
    public static class TrailDot {
        public float x, y, life, size;
        public int color;
        public TrailDot() {}
    }
    private final List<TrailDot> trail = new ArrayList<>();
    private float trailSpawnAccum = 0f;

    // Reusable drawing objects
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix = new Matrix();
    private final RectF rect = new RectF();

    // UI hit-test rectangles
    private final RectF btnPlay = new RectF();
    private final RectF btnSuitSelect = new RectF();
    private final RectF btnControlMode = new RectF();
    private final RectF btnSound = new RectF();
    private final RectF btnLanguage = new RectF();
    private final RectF btnBack = new RectF();
    private final RectF btnTurboToggle = new RectF();
    private final RectF btnPauseTurbo = new RectF();
    private final RectF[] worldCards = new RectF[3];
    private final RectF[] levelButtons = new RectF[12];
    private final RectF btnHudPause = new RectF();
    private final RectF btnHudSlowMo = new RectF();
    private final RectF btnHudZoomReset = new RectF();
    private final RectF btnPauseResume = new RectF();
    private final RectF btnPauseRestart = new RectF();
    private final RectF btnPauseLevels = new RectF();
    private final RectF btnWinNext = new RectF();
    private final RectF btnWinReplay = new RectF();
    private final RectF btnWinLevels = new RectF();
    private final RectF btnReplayCutscene = new RectF();
    private final RectF btnStoryPrev = new RectF();
    private final RectF btnStoryNext = new RectF();
    private final RectF btnStorySkip = new RectF();

    // Intro Cutscene ("La Historia de Orion") state & 16 narrative beats across Planos 1..10
    private int storyBeatIndex = 0;
    private float storyTypedProgress = 0f;
    private int storyLastTickChar = 0;
    private float storySlideFade = 1.0f;

    private static final int[] STORY_PLANO_NUMS = {
        1, 2, 3, 4, 5, 6, 7, 8,
        9, 9,
        10, 10, 10, 10, 10, 10
    };
    private static final String[] STORY_LINES_ES = {
        "Cuando desperté, el universo ya estaba muerto.",
        "Los vientos solo arrastraban recuerdos.",
        "Sin embargo, una última llama seguía resistiendo.",
        "Allí, entre ruinas olvidadas, alguien esperaba.",
        "Un arquitecto de imposibles.",
        "Y yo era su última creación.",
        "Fragmentos de un pasado perdido cubrían aquellas paredes.",
        "Entonces la energía volvió a recorrer mis circuitos.",
        "Una voz quebrada atravesó la oscuridad.",
        "«Recuerda la canción.»",
        "Abrí los ojos.",
        "Y contemplé lo que quedaba del cosmos.",
        "No recordaba mi propósito.",
        "No recordaba mi historia.",
        "Solo aquella canción.",
        "Y la necesidad de seguirla."
    };
    private static final String[] STORY_LINES_EN = {
        "When I awoke, the universe was already dead.",
        "The winds carried only memories.",
        "Yet, one final flame continued to endure.",
        "There, among forgotten ruins, someone was waiting.",
        "An architect of the impossible.",
        "And I was his final creation.",
        "Fragments of a lost past covered those walls.",
        "Then energy flowed through my circuits once more.",
        "A broken voice pierced the darkness.",
        "\"Remember the song.\"",
        "I opened my eyes.",
        "And gazed upon what remained of the cosmos.",
        "I did not remember my purpose.",
        "I did not remember my story.",
        "Only that song.",
        "And the need to follow it."
    };

    private static final String[] SUIT_NAMES_EN = {
        "Solar", "Cosmic", "Frost", "Amber", "Terra",
        "Azure", "Crimson", "Starlight", "Onyx", "Emerald"
    };
    private static final String[] SUIT_NAMES_ES = {
        "Solar", "Cósmico", "Escarcha", "Ámbar", "Terra",
        "Azur", "Carmesí", "Estelar", "Ónix", "Esmeralda"
    };

    public OrionGameView(Context context, AudioController audio) {
        super(context);
        this.context = context;
        this.audio = audio;
        this.sprites = new SpriteCache(context);
        this.prefs = context.getSharedPreferences("orions_journey_prefs", Context.MODE_PRIVATE);

        for (int i = 0; i < 3; i++) worldCards[i] = new RectF();
        for (int i = 0; i < 12; i++) levelButtons[i] = new RectF();

        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        loadProgress();

        scaleDetector = new ScaleGestureDetector(context, this);

        getHolder().addCallback(this);
        setFocusable(true);
        audio.playMusic("menu_orion.wav", true);
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (gameState == STATE_PLAYING) {
            userZoomMultiplier *= detector.getScaleFactor();
            userZoomMultiplier = Math.max(0.45f, Math.min(1.75f, userZoomMultiplier));
            return true;
        }
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return gameState == STATE_PLAYING;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
    }

    private void loadProgress() {
        selectedSuitIndex = prefs.getInt("suit_idx", 6);
        tiltControls = prefs.getBoolean("tilt_controls", false);
        turboMode = prefs.getBoolean("turbo_mode", false);
        langEs = prefs.getBoolean("lang_es", true);
        for (int i = 1; i <= 36; i++) {
            bestRings[i] = prefs.getInt("best_ring_" + i, 0);
            levelCleared[i] = prefs.getBoolean("cleared_" + i, false);
            levelTotalRings[i] = LevelData.getLevelRingCount(context, i);
        }
    }

    private void saveLevelProgress(int lvl, int count) {
        levelCleared[lvl] = true;
        if (count > bestRings[lvl]) {
            bestRings[lvl] = count;
        }
        prefs.edit()
                .putBoolean("cleared_" + lvl, true)
                .putInt("best_ring_" + lvl, bestRings[lvl])
                .putInt("suit_idx", selectedSuitIndex)
                .putBoolean("tilt_controls", tiltControls)
                .putBoolean("turbo_mode", turboMode)
                .putBoolean("lang_es", langEs)
                .apply();
    }

    private int getRequiredRings90(int lvl) {
        int tot = levelTotalRings[lvl];
        if (tot <= 0) return 0;
        return (int) Math.ceil(tot * 0.90f);
    }

    private int getLevelPercent(int lvl) {
        int tot = levelTotalRings[lvl];
        if (tot <= 0) return levelCleared[lvl] ? 100 : 0;
        return Math.min(100, (bestRings[lvl] * 100) / tot);
    }

    private boolean hasMet90Percent(int lvl) {
        if (!levelCleared[lvl]) return false;
        return bestRings[lvl] >= getRequiredRings90(lvl);
    }

    private boolean isLevelUnlocked(int lvl) {
        if (lvl <= 1) return true;
        for (int p = 1; p < lvl; p++) {
            if (!hasMet90Percent(p)) return false;
        }
        return true;
    }

    private boolean isWorldUnlocked(int worldNum) {
        if (worldNum <= 1) return true;
        int prevWorldLastLevel = (worldNum - 1) * 12;
        return isLevelUnlocked(prevWorldLastLevel + 1);
    }

    private void showStatusBanner(String msg) {
        statusBannerText = msg;
        statusBannerTimer = 2.8f;
    }

    public void setTiltInput(float tiltX) {
        this.tiltInputX = Math.max(-1f, Math.min(1f, tiltX));
    }

    public void setTiltInput(float tiltX, float tiltY) {
        this.tiltInputX = Math.max(-1f, Math.min(1f, tiltX));
        this.tiltInputY = Math.max(-1f, Math.min(1f, tiltY));
    }

    public boolean handleBackPress() {
        if (gameState == STATE_STORY_CUTSCENE) {
            if (storyBeatIndex > 0) {
                int prevPlano = STORY_PLANO_NUMS[storyBeatIndex];
                storyBeatIndex--;
                storyTypedProgress = 0f;
                storyLastTickChar = 0;
                if (STORY_PLANO_NUMS[storyBeatIndex] != prevPlano) {
                    storySlideFade = 0.25f;
                }
                audio.playSfx("Menu_Click24.wav");
            } else {
                gameState = STATE_LEVEL_SELECT;
                audio.playMusic("menu_orion.wav", true);
                audio.playSfx("Menu_Click24.wav");
            }
            return true;
        } else if (gameState == STATE_PLAYING) {
            gameState = STATE_PAUSED;
            audio.playSfx("Menu_Click24.wav");
            return true;
        } else if (gameState == STATE_PAUSED) {
            gameState = STATE_PLAYING;
            audio.playSfx("Menu_Click24.wav");
            return true;
        } else if (gameState == STATE_LEVEL_COMPLETE) {
            gameState = STATE_LEVEL_SELECT;
            audio.playMusic("menu_orion.wav", true);
            return true;
        } else if (gameState == STATE_LEVEL_SELECT) {
            gameState = STATE_WORLD_SELECT;
            audio.playSfx("Menu_Click24.wav");
            return true;
        } else if (gameState == STATE_WORLD_SELECT) {
            gameState = STATE_MAIN_MENU;
            audio.playSfx("Menu_Click24.wav");
            return true;
        }
        return false;
    }

    public void startStoryCutscene() {
        this.storyBeatIndex = 0;
        this.storyTypedProgress = 0f;
        this.storyLastTickChar = 0;
        this.storySlideFade = 0.1f;
        this.gameState = STATE_STORY_CUTSCENE;
        audio.playMusic("story_intro.wav", true);
    }

    public void startLevel(int levelNum) {
        this.currentLevelNum = Math.max(1, Math.min(36, levelNum));
        this.level = LevelData.load(context, this.currentLevelNum);
        this.currentSpawnX = level.spawnX;
        this.currentSpawnY = level.spawnY;
        this.collectedRings = 0;
        this.totalLevelRings = level.rings.size();
        this.levelTimerSec = 0f;
        this.slowMoActive = false;
        this.userZoomMultiplier = 1.0f;
        this.trail.clear();

        respawnOrion(false);
        this.camX = orionX;
        this.camY = orionY;
        this.gameState = STATE_PLAYING;

        boolean hasBoss = false;
        for (LevelData.ObstacleEntity o : level.obstacles) {
            if (o.isBoss) {
                hasBoss = true;
                break;
            }
        }
        audio.playMusic(hasBoss ? "boss_music.mp3" : "gameplay_music.mp3", true);
    }

    private void respawnOrion(boolean playSplash) {
        if (playSplash) {
            audio.playSfx("Splash.wav");
        }
        orionX = currentSpawnX;
        orionY = currentSpawnY;
        orionVx = 0f;
        orionVy = 0f;
        orionAngleDeg = 0f;
        orionState = ORION_FREE;
        currentCannon = null;
        lastLaunchedCannon = null;
        cannonIgnoreTimer = 0f;
        currentSlingshot = null;
        draggingSlingshot = false;
        draggingPurpleCannon = false;
        touchLeftDown = false;
        touchRightDown = false;
        if (level != null) {
            for (LevelData.CannonEntity c : level.cannons) {
                if (c.hasMover && !c.alwaysActiveMover) {
                    c.moverActive = false;
                    c.moverProgress = 0f;
                    c.moverDir = 1;
                    c.x = c.baseX;
                    c.y = c.baseY;
                }
            }
            for (LevelData.ObstacleEntity o : level.obstacles) {
                if (!o.isBoss) {
                    o.active = true;
                    o.cloudFadeTimer = 0f;
                }
            }
        }
        trail.clear();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        gameThread = new Thread(this, "OrionGameThread");
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        if (gameThread != null) {
            try {
                gameThread.join(600);
            } catch (InterruptedException ignored) {
            }
            gameThread = null;
        }
    }

    @Override
    public void run() {
        long lastNano = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dtSec = (now - lastNano) / 1_000_000_000f;
            lastNano = now;
            if (dtSec > 0.05f) dtSec = 0.05f;

            update(dtSec);

            Canvas canvas = null;
            SurfaceHolder holder = getHolder();
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    synchronized (holder) {
                        render(canvas);
                    }
                }
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas);
                    } catch (Exception ignored) {
                    }
                }
            }

            long frameElapsedMs = (System.nanoTime() - now) / 1_000_000L;
            if (frameElapsedMs < 14) {
                try {
                    Thread.sleep(14 - frameElapsedMs);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private void update(float dtSec) {
        animClock += dtSec;
        float smooth = Math.min(1f, dtSec * 6.5f);
        parallaxX += (tiltInputX - parallaxX) * smooth;
        parallaxY += (tiltInputY - parallaxY) * smooth;
        if (statusBannerTimer > 0f) {
            statusBannerTimer = Math.max(0f, statusBannerTimer - dtSec);
        }
        if (gameState == STATE_STORY_CUTSCENE) {
            if (storySlideFade < 1.0f) {
                storySlideFade = Math.min(1.0f, storySlideFade + dtSec * 2.2f);
            }
            String fullLine = (langEs ? STORY_LINES_ES : STORY_LINES_EN)[storyBeatIndex];
            if (storyTypedProgress < fullLine.length()) {
                storyTypedProgress = Math.min((float) fullLine.length(), storyTypedProgress + dtSec * 21f);
                int curChar = (int) storyTypedProgress;
                if (curChar > storyLastTickChar && curChar <= fullLine.length()) {
                    char c = fullLine.charAt(curChar - 1);
                    if (c != ' ' && (curChar - storyLastTickChar >= 2 || curChar == fullLine.length())) {
                        storyLastTickChar = curChar;
                        float pitch = 0.94f + ((curChar % 5) * 0.03f);
                        audio.playSfxPitch("typewriter_tick.wav", pitch);
                    }
                }
            }
            return;
        }
        if (gameState != STATE_PLAYING || level == null) {
            return;
        }

        levelTimerSec += dtSec;

        float tickRate = turboMode ? 40f : 30f;
        if (slowMoActive) {
            tickRate *= 0.42f;
        }
        float dtTicks = dtSec * tickRate;

        if (cannonIgnoreTimer > 0f) {
            cannonIgnoreTimer = Math.max(0f, cannonIgnoreTimer - dtSec);
        }

        // 1. Update all cannons
        for (LevelData.CannonEntity c : level.cannons) {
            if (c.recoilAnim > 0f) {
                c.recoilAnim = Math.max(0f, c.recoilAnim - dtSec * 4f);
            }

            if (c.hasMover && (c.alwaysActiveMover || c.moverActive)) {
                c.moverProgress += c.moverDir * c.moveSpeed * dtSec;
                if (c.moverProgress >= 1f) {
                    c.moverProgress = 1f;
                    c.moverDir = -1;
                } else if (c.moverProgress <= 0f) {
                    c.moverProgress = 0f;
                    c.moverDir = 1;
                }
                c.x = c.baseX + c.moveOffsetX * c.moverProgress;
                c.y = c.baseY + c.moveOffsetY * c.moverProgress;
            }

            if (c.hasRotator && c.numSteps > 0) {
                if (c.pauseTimer > 0f) {
                    c.pauseTimer -= dtSec;
                } else {
                    float rotSpeedDegPerSec = c.pauseBetweenSteps ? 210f : 140f;
                    float diff = c.targetAngleDeg - c.currentAngleDeg;
                    float maxDelta = rotSpeedDegPerSec * dtSec;
                    if (Math.abs(diff) <= maxDelta) {
                        c.currentAngleDeg = c.targetAngleDeg;
                        if (c.pingPong) {
                            c.currentStepIndex += c.stepDirection;
                            if (c.currentStepIndex >= c.numSteps) {
                                c.currentStepIndex = c.numSteps;
                                c.stepDirection = -1;
                            } else if (c.currentStepIndex <= 0) {
                                c.currentStepIndex = 0;
                                c.stepDirection = 1;
                            }
                            c.targetAngleDeg = c.baseAngleDeg + c.currentStepIndex * c.rotStepCsvDeg;
                        } else {
                            c.currentStepIndex++;
                            c.targetAngleDeg = c.baseAngleDeg + c.currentStepIndex * c.rotStepCsvDeg;
                        }
                        if (c.pauseBetweenSteps) {
                            c.pauseTimer = 0.42f;
                        }
                    } else {
                        c.currentAngleDeg += Math.signum(diff) * maxDelta;
                    }
                }
            }
        }

        // 2. Update obstacles & bosses
        for (LevelData.ObstacleEntity o : level.obstacles) {
            if (o.hitAnim > 0f) {
                o.hitAnim = Math.max(0f, o.hitAnim - dtSec * 3.5f);
            }
            if (o.cloudFadeTimer > 0f) {
                o.cloudFadeTimer += dtSec;
                if (o.cloudFadeTimer > 0.45f) {
                    o.active = false;
                }
            }
            if (!o.active) continue;

            if (o.hasMover) {
                o.moverProgress += o.moverDir * o.moveSpeed * dtSec;
                if (o.moverProgress >= 1f) {
                    o.moverProgress = 1f;
                    o.moverDir = -1;
                } else if (o.moverProgress <= 0f) {
                    o.moverProgress = 0f;
                    o.moverDir = 1;
                }
                o.x = o.baseX + o.moveOffsetX * o.moverProgress;
                o.y = o.baseY + o.moveOffsetY * o.moverProgress;
            }

            if (o.isBoss) {
                if (o.bossHitCooldown > 0f) {
                    o.bossHitCooldown = Math.max(0f, o.bossHitCooldown - dtSec);
                }
                o.bossSwingPhase += dtSec * 2.4f;
                o.bossMoveTimer -= dtTicks;
                if (o.bossMoveTimer <= 0f) {
                    o.bossMoveTimer = 42f;
                    float randOffsetX = ((float) Math.random() - 0.5f) * 780f;
                    float randOffsetY = (o.type == 27) ? (((float) Math.random() - 0.5f) * 320f) : 0f;
                    o.bossTargetX = o.baseX + randOffsetX;
                    o.bossTargetY = o.baseY + randOffsetY;
                }
                o.x += (o.bossTargetX - o.x) * Math.min(1f, dtSec * 2.2f);
                o.y += (o.bossTargetY - o.y) * Math.min(1f, dtSec * 2.2f);
            }
        }

        // 3. Update Orion state & physics
        if (orionState == ORION_IN_CANNON && currentCannon != null) {
            orionX = currentCannon.x;
            orionY = currentCannon.y;
            orionVx = 0f;
            orionVy = 0f;
            if (currentCannon.autoLaunch) {
                autoLaunchDelay -= dtSec;
                if (autoLaunchDelay <= 0f) {
                    fireFromCurrentCannon();
                }
            }
        } else if (orionState == ORION_IN_SLINGSHOT && currentSlingshot != null) {
            orionX = currentSlingshot.x + slingPullDx;
            orionY = currentSlingshot.y + slingPullDy;
            orionVx = 0f;
            orionVy = 0f;
        } else {
            orionVy += 1.0f * dtTicks;

            float steer = 0f;
            if (tiltControls) {
                steer = tiltInputX;
            } else {
                if (touchLeftDown && !touchRightDown) steer = -1f;
                else if (touchRightDown && !touchLeftDown) steer = 1f;
            }
            orionVx += steer * 0.8f * dtTicks;

            float drag = Math.max(0.85f, 1.0f - 0.02f * dtTicks);
            orionVx *= drag;
            orionVy *= drag;

            int subSteps = 3;
            float subDt = dtTicks / subSteps;
            for (int s = 0; s < subSteps; s++) {
                orionX += orionVx * subDt;
                orionY += orionVy * subDt;
                checkGameplayCollisions(dtTicks / subSteps);
                if (orionState != ORION_FREE || gameState != STATE_PLAYING) {
                    break;
                }
            }

            orionAngleDeg += (orionVx * 0.65f + 6f) * dtTicks;

            trailSpawnAccum += dtSec;
            if (trailSpawnAccum >= 0.028f) {
                trailSpawnAccum = 0f;
                TrailDot dot = new TrailDot();
                dot.x = orionX;
                dot.y = orionY;
                dot.life = 1.0f;
                dot.size = turboMode ? 18f : 13f;
                dot.color = turboMode ? Color.rgb(255, 140, 20) : Color.WHITE;
                trail.add(dot);
                if (trail.size() > 36) {
                    trail.remove(0);
                }
            }

            if (orionY > level.groundY || orionY < level.ceilingY) {
                respawnOrion(true);
            }
        }

        for (int i = trail.size() - 1; i >= 0; i--) {
            TrailDot d = trail.get(i);
            d.life -= dtSec * 1.9f;
            if (d.life <= 0f) {
                trail.remove(i);
            }
        }

        for (LevelData.RingItem item : level.rings) {
            if (item.collected && item.collectAnim < 1f) {
                item.collectAnim = Math.min(1f, item.collectAnim + dtSec * 3.5f);
            }
        }

        float targetCamX = orionX + orionVx * 4.5f;
        float targetCamY = orionY + orionVy * 3.0f;
        camX += (targetCamX - camX) * Math.min(1f, dtSec * 7.5f);
        camY += (targetCamY - camY) * Math.min(1f, dtSec * 7.5f);
    }

    private void checkGameplayCollisions(float subDtTicks) {
        if (level == null) return;

        // 1. Collect Rings
        for (LevelData.RingItem item : level.rings) {
            if (!item.collected) {
                float dx = orionX - item.x;
                float dy = orionY - item.y;
                if (dx * dx + dy * dy <= 48f * 48f) {
                    item.collected = true;
                    item.collectAnim = 0.01f;
                    collectedRings++;
                    float pitch = 0.95f + Math.min(0.45f, collectedRings * 0.008f);
                    audio.playSfxPitch("puffleOcollect.wav", pitch);
                }
            }
        }

        // 2. Check Giant Finish Ring
        if (level.finishActive) {
            float fdx = orionX - level.finishX;
            float fdy = orionY - level.finishY;
            if (fdx * fdx + fdy * fdy <= 82f * 82f) {
                audio.playSfx("GiantPuffleOReached.wav");
                audio.playMusic("win_music.mp3", false);
                saveLevelProgress(currentLevelNum, collectedRings);
                gameState = STATE_LEVEL_COMPLETE;
                return;
            }
        }

        // 3. Check Cannons
        for (LevelData.CannonEntity c : level.cannons) {
            if (c == lastLaunchedCannon && cannonIgnoreTimer > 0f) {
                continue;
            }
            float dx = orionX - c.x;
            float dy = orionY - c.y;
            if (dx * dx + dy * dy <= 52f * 52f) {
                enterCannon(c);
                return;
            }
        }

        // 4. Check Obstacles & Bosses
        for (LevelData.ObstacleEntity o : level.obstacles) {
            if (!o.active) continue;
            float dx = orionX - o.x;
            float dy = orionY - o.y;
            float distSq = dx * dx + dy * dy;

            if (o.isBoss) {
                if (o.type == 27) {
                    float swingAngle = (float) Math.sin(o.bossSwingPhase) * 1.05f;
                    float anvilX = o.x + (float) Math.sin(swingAngle) * 195f;
                    float anvilY = o.y + (float) Math.cos(swingAngle) * 195f;
                    float adx = orionX - anvilX;
                    float ady = orionY - anvilY;
                    if (adx * adx + ady * ady <= 58f * 58f) {
                        audio.playSfx("AnvilHit.wav");
                        float len = Math.max(1f, (float) Math.hypot(adx, ady));
                        orionVx = (adx / len) * 28f;
                        orionVy = (ady / len) * 28f;
                    }
                }

                if (distSq <= 115f * 115f && o.bossHitCooldown <= 0f) {
                    o.bossHp--;
                    o.bossHitCooldown = 0.55f;
                    o.hitAnim = 1.0f;
                    float len = Math.max(1f, (float) Math.hypot(dx, dy));
                    orionVx = (dx / len) * 32f;
                    orionVy = -26f;

                    if (o.bossHp <= 0) {
                        o.active = false;
                        level.finishX = o.x;
                        level.finishY = o.y;
                        level.finishActive = true;
                        audio.playSfx("BossExplosion.wav");
                    } else {
                        audio.playSfx("BossHit.wav");
                    }
                }
                continue;
            }

            switch (o.type) {
                case 3:
                    if (distSq <= 58f * 58f) {
                        o.hitAnim = 1.0f;
                        audio.playSfx("GreenBalloonBump.wav");
                        orionVx *= 0.55f;
                        orionVy = -28f;
                        orionY = o.y - 59f;
                    }
                    break;
                case 10:
                    if (distSq <= 60f * 60f) {
                        o.hitAnim = 1.0f;
                        audio.playSfx("PinkBalloonBump.wav");
                        orionVx *= 0.65f;
                        orionVy = -38f;
                        orionY = o.y - 61f;
                    }
                    break;
                case 9:
                    if (o.cloudFadeTimer == 0f && distSq <= 75f * 75f) {
                        o.cloudFadeTimer = 0.01f;
                        audio.playSfx("CloudFall.wav");
                    }
                    break;
                case 11:
                    if (distSq <= 66f * 66f && cannonIgnoreTimer <= 0f) {
                        orionState = ORION_IN_SLINGSHOT;
                        currentSlingshot = o;
                        slingPullDx = 0f;
                        slingPullDy = 45f;
                        orionX = o.x;
                        orionY = o.y + 45f;
                        orionVx = 0f;
                        orionVy = 0f;
                        audio.playSfx("CPW_PuffleCannon_SlingShot_Stretch01_x2_Stretch_Twist2-TiSh_01-01_sfx.wav");
                        return;
                    }
                    break;
                case 12:
                case 13:
                case 14:
                case 15:
                case 16: {
                    double rad = Math.toRadians(o.angleDeg);
                    float blowX = (float) Math.sin(rad);
                    float blowY = -(float) Math.cos(rad);
                    float relX = orionX - o.x;
                    float relY = orionY - o.y;
                    float along = relX * blowX + relY * blowY;
                    float perp = Math.abs(-relX * blowY + relY * blowX);
                    if (along >= -25f && along <= 330f && perp <= 78f) {
                        float alongVel = orionVx * blowX + orionVy * blowY;
                        if (alongVel < 0f) {
                            // Smoothly cushion incoming speed against the fan so Orion doesn't slam into it
                            float damp = Math.min(0.45f, 0.14f * subDtTicks);
                            orionVx -= blowX * alongVel * damp;
                            orionVy -= blowY * alongVel * damp;
                        }
                        if (alongVel < 13.0f) {
                            float strength = 1.12f * (1.0f - (along / 350f));
                            orionVx += blowX * strength * subDtTicks;
                            orionVy += blowY * strength * subDtTicks;
                        }
                    }
                    break;
                }
                case 1:
                case 4:
                case 5:
                case 6:
                case 2:
                case 17:
                case 19:
                case 20:
                case 21:
                case 22:
                    if (distSq <= 62f * 62f) {
                        o.hitAnim = 1.0f;
                        if (o.type == 2 || o.type == 17) {
                            audio.playSfx("CactusHit.wav");
                        } else if (o.type >= 19) {
                            audio.playSfx("AnvilHit.wav");
                        } else {
                            audio.playSfx("PianoHit.wav");
                        }
                        float len = Math.max(1f, (float) Math.hypot(dx, dy));
                        float nx = dx / len;
                        float ny = dy / len;
                        orionX = o.x + nx * 64f;
                        orionY = o.y + ny * 64f;
                        orionVx = nx * 28f;
                        orionVy = ny * 28f - 6f;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void enterCannon(LevelData.CannonEntity c) {
        orionState = ORION_IN_CANNON;
        currentCannon = c;
        orionX = c.x;
        orionY = c.y;
        orionVx = 0f;
        orionVy = 0f;
        autoLaunchDelay = 0.08f;

        if (c.hasMover && !c.alwaysActiveMover) {
            c.moverActive = true;
        }
        if (c.isCheckpoint && !c.checkpointTriggered) {
            c.checkpointTriggered = true;
            currentSpawnX = c.baseX;
            currentSpawnY = c.baseY;
            audio.playSfx("GiantPuffleOReached.wav");
        }
    }

    private void fireFromCurrentCannon() {
        if (currentCannon == null) return;
        double rad = Math.toRadians(currentCannon.currentAngleDeg);
        float launchSpeed = 40.0f;
        orionVx = (float) Math.cos(rad) * launchSpeed;
        orionVy = (float) Math.sin(rad) * launchSpeed;
        orionX = currentCannon.x + (float) Math.cos(rad) * 32f;
        orionY = currentCannon.y + (float) Math.sin(rad) * 32f;

        currentCannon.recoilAnim = 1.0f;
        lastLaunchedCannon = currentCannon;
        cannonIgnoreTimer = 0.22f;
        currentCannon = null;
        orionState = ORION_FREE;
        draggingPurpleCannon = false;

        audio.playSfx("CannonLaunch.wav");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        int action = event.getActionMasked();
        float tx = event.getX();
        float ty = event.getY();

        if (gameState == STATE_MAIN_MENU) {
            if (action == MotionEvent.ACTION_DOWN) {
                if (btnPlay.contains(tx, ty)) {
                    audio.playSfx("Menu_Click24.wav");
                    gameState = STATE_WORLD_SELECT;
                } else if (btnSuitSelect.contains(tx, ty)) {
                    audio.playSfx("Menu_Click24.wav");
                    selectedSuitIndex = (selectedSuitIndex + 1) % 10;
                    prefs.edit().putInt("suit_idx", selectedSuitIndex).apply();
                } else if (btnTurboToggle.contains(tx, ty)) {
                    audio.playSfx("Menu_Click24.wav");
                    turboMode = !turboMode;
                    prefs.edit().putBoolean("turbo_mode", turboMode).apply();
                } else if (btnLanguage.contains(tx, ty)) {
                    audio.playSfx("Menu_Click24.wav");
                    langEs = !langEs;
                    prefs.edit().putBoolean("lang_es", langEs).apply();
                } else if (btnControlMode.contains(tx, ty)) {
                    audio.playSfx("Menu_Click24.wav");
                    tiltControls = !tiltControls;
                    prefs.edit().putBoolean("tilt_controls", tiltControls).apply();
                } else if (btnSound.contains(tx, ty)) {
                    audio.setMuted(!audio.isMuted());
                    audio.playSfx("Menu_Click24.wav");
                }
            }
            return true;
        }

        if (gameState == STATE_WORLD_SELECT) {
            if (action == MotionEvent.ACTION_DOWN) {
                if (btnBack.contains(tx, ty)) {
                    audio.playSfx("Menu_Click24.wav");
                    gameState = STATE_MAIN_MENU;
                    return true;
                }
                if (btnTurboToggle.contains(tx, ty)) {
                    audio.playSfx("Menu_Click24.wav");
                    turboMode = !turboMode;
                    prefs.edit().putBoolean("turbo_mode", turboMode).apply();
                    return true;
                }
                for (int i = 0; i < 3; i++) {
                    if (worldCards[i].contains(tx, ty)) {
                        audio.playSfx("Menu_Click24.wav");
                        int wNum = i + 1;
                        if (isWorldUnlocked(wNum)) {
                            selectedWorld = wNum;
                            gameState = STATE_LEVEL_SELECT;
                        } else {
                            showStatusBanner(langEs
                                    ? "🔒 Completa todos los niveles del Mundo " + (wNum - 1) + " al 90% para desbloquear"
                                    : "🔒 Complete all World " + (wNum - 1) + " levels at 90% to unlock");
                        }
                        return true;
                    }
                }
            }
            return true;
        }

        if (gameState == STATE_STORY_CUTSCENE) {
            if (action == MotionEvent.ACTION_DOWN) {
                if (btnStorySkip.contains(tx, ty)) {
                    audio.playSfx("Menu_Click24.wav");
                    prefs.edit().putBoolean("intro_seen", true).apply();
                    startLevel(1);
                    return true;
                }
                if (btnStoryPrev.contains(tx, ty)) {
                    audio.playSfx("Menu_Click24.wav");
                    if (storyBeatIndex > 0) {
                        int prevPlano = STORY_PLANO_NUMS[storyBeatIndex];
                        storyBeatIndex--;
                        storyTypedProgress = 0f;
                        storyLastTickChar = 0;
                        if (STORY_PLANO_NUMS[storyBeatIndex] != prevPlano) {
                            storySlideFade = 0.25f;
                        }
                    } else {
                        gameState = STATE_LEVEL_SELECT;
                        audio.playMusic("menu_orion.wav", true);
                    }
                    return true;
                }

                // Tapping Next button OR anywhere on the screen/slide advances the typewriter or slide
                String fullLine = (langEs ? STORY_LINES_ES : STORY_LINES_EN)[storyBeatIndex];
                if (storyTypedProgress < fullLine.length()) {
                    // Complete the typewriter text immediately on first tap
                    storyTypedProgress = fullLine.length();
                    storyLastTickChar = fullLine.length();
                    audio.playSfxPitch("typewriter_tick.wav", 1.05f);
                } else {
                    audio.playSfx("Menu_Click24.wav");
                    if (storyBeatIndex < STORY_PLANO_NUMS.length - 1) {
                        int prevPlano = STORY_PLANO_NUMS[storyBeatIndex];
                        storyBeatIndex++;
                        storyTypedProgress = 0f;
                        storyLastTickChar = 0;
                        if (STORY_PLANO_NUMS[storyBeatIndex] != prevPlano) {
                            storySlideFade = 0.2f;
                        }
                    } else {
                        // Final slide button: Start Level 1
                        prefs.edit().putBoolean("intro_seen", true).apply();
                        startLevel(1);
                    }
                }
            }
            return true;
        }

        if (gameState == STATE_LEVEL_SELECT) {
            if (action == MotionEvent.ACTION_DOWN) {
                if (btnBack.contains(tx, ty)) {
                    audio.playSfx("Menu_Click24.wav");
                    gameState = STATE_WORLD_SELECT;
                    return true;
                }
                if (btnReplayCutscene.contains(tx, ty)) {
                    audio.playSfx("Menu_Click24.wav");
                    startStoryCutscene();
                    return true;
                }
                if (btnTurboToggle.contains(tx, ty)) {
                    audio.playSfx("Menu_Click24.wav");
                    turboMode = !turboMode;
                    prefs.edit().putBoolean("turbo_mode", turboMode).apply();
                    return true;
                }
                for (int i = 0; i < 12; i++) {
                    if (levelButtons[i].contains(tx, ty)) {
                        audio.playSfx("Menu_Click24.wav");
                        int lvl = (selectedWorld - 1) * 12 + (i + 1);
                        if (isLevelUnlocked(lvl)) {
                            if (lvl == 1) {
                                startStoryCutscene();
                            } else {
                                startLevel(lvl);
                            }
                        } else {
                            int reqLvl = lvl - 1;
                            for (int p = 1; p < lvl; p++) {
                                if (!hasMet90Percent(p)) {
                                    reqLvl = p;
                                    break;
                                }
                            }
                            int reqCount = getRequiredRings90(reqLvl);
                            showStatusBanner(langEs
                                    ? ("🔒 Requiere completar el Nivel " + reqLvl + " al 90% (" + reqCount + "/" + levelTotalRings[reqLvl] + " aros)")
                                    : ("🔒 Requires completing Level " + reqLvl + " at 90% (" + reqCount + "/" + levelTotalRings[reqLvl] + " rings)"));
                        }
                        return true;
                    }
                }
            }
            return true;
        }

        if (gameState == STATE_PAUSED) {
            if (action == MotionEvent.ACTION_DOWN) {
                if (btnPauseResume.contains(tx, ty)) {
                    audio.playSfx("Menu_Click24.wav");
                    gameState = STATE_PLAYING;
                } else if (btnPauseRestart.contains(tx, ty)) {
                    audio.playSfx("Menu_Click24.wav");
                    startLevel(currentLevelNum);
                } else if (btnPauseTurbo.contains(tx, ty)) {
                    audio.playSfx("Menu_Click24.wav");
                    turboMode = !turboMode;
                    prefs.edit().putBoolean("turbo_mode", turboMode).apply();
                } else if (btnPauseLevels.contains(tx, ty)) {
                    audio.playSfx("Menu_Click24.wav");
                    audio.playMusic("menu_orion.wav", true);
                    gameState = STATE_LEVEL_SELECT;
                }
            }
            return true;
        }

        if (gameState == STATE_LEVEL_COMPLETE) {
            if (action == MotionEvent.ACTION_DOWN) {
                if (btnWinNext.contains(tx, ty)) {
                    audio.playSfx("Menu_Click24.wav");
                    int next = (currentLevelNum < 36) ? (currentLevelNum + 1) : 1;
                    if (isLevelUnlocked(next)) {
                        selectedWorld = ((next - 1) / 12) + 1;
                        startLevel(next);
                    } else {
                        int reqCount = getRequiredRings90(currentLevelNum);
                        showStatusBanner(langEs
                                ? ("🔒 Recoge mínimo el 90% (" + reqCount + " aros) en este nivel para avanzar")
                                : ("🔒 Collect at least 90% (" + reqCount + " rings) in this level to advance"));
                    }
                } else if (btnWinReplay.contains(tx, ty)) {
                    audio.playSfx("Menu_Click24.wav");
                    startLevel(currentLevelNum);
                } else if (btnWinLevels.contains(tx, ty)) {
                    audio.playSfx("Menu_Click24.wav");
                    audio.playMusic("menu_orion.wav", true);
                    gameState = STATE_LEVEL_SELECT;
                }
            }
            return true;
        }

        if (gameState == STATE_PLAYING) {
            if (action == MotionEvent.ACTION_DOWN) {
                if (btnHudPause.contains(tx, ty)) {
                    audio.playSfx("Menu_Click24.wav");
                    gameState = STATE_PAUSED;
                    return true;
                }
                if (btnHudSlowMo.contains(tx, ty)) {
                    audio.playSfx("Menu_Click24.wav");
                    slowMoActive = !slowMoActive;
                    return true;
                }
                if (btnHudZoomReset.contains(tx, ty)) {
                    audio.playSfx("Menu_Click24.wav");
                    userZoomMultiplier = (userZoomMultiplier < 0.8f) ? 1.0f : 0.55f;
                    return true;
                }

                if (orionState == ORION_IN_SLINGSHOT && currentSlingshot != null) {
                    draggingSlingshot = true;
                    dragStartScreenX = tx;
                    dragStartScreenY = ty;
                    return true;
                }

                if (orionState == ORION_IN_CANNON && currentCannon != null) {
                    if (currentCannon.playerRotatable) {
                        draggingPurpleCannon = true;
                        purpleMovedEnough = false;
                        float cx = getWidth() * 0.5f + (currentCannon.x - camX) * camZoom;
                        float cy = getHeight() * 0.5f + (currentCannon.y - camY) * camZoom;
                        purpleLastTouchAngle = (float) Math.toDegrees(Math.atan2(ty - cy, tx - cx));
                    } else if (!currentCannon.autoLaunch) {
                        fireFromCurrentCannon();
                    }
                    return true;
                }
            } else if (action == MotionEvent.ACTION_MOVE) {
                if (orionState == ORION_IN_SLINGSHOT && draggingSlingshot && currentSlingshot != null) {
                    float dx = (tx - dragStartScreenX) / Math.max(0.4f, camZoom);
                    float dy = (ty - dragStartScreenY) / Math.max(0.4f, camZoom);
                    slingPullDx = Math.max(-98f, Math.min(98f, dx));
                    slingPullDy = Math.max(20f, Math.min(166f, 45f + dy));
                    return true;
                }
                if (orionState == ORION_IN_CANNON && currentCannon != null && currentCannon.playerRotatable && draggingPurpleCannon) {
                    float cx = getWidth() * 0.5f + (currentCannon.x - camX) * camZoom;
                    float cy = getHeight() * 0.5f + (currentCannon.y - camY) * camZoom;
                    float ang = (float) Math.toDegrees(Math.atan2(ty - cy, tx - cx));
                    float delta = ang - purpleLastTouchAngle;
                    if (delta > 180f) delta -= 360f;
                    if (delta < -180f) delta += 360f;
                    if (Math.abs(delta) > 3f) {
                        purpleMovedEnough = true;
                    }
                    currentCannon.currentAngleDeg += delta;
                    purpleLastTouchAngle = ang;
                    return true;
                }
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (orionState == ORION_IN_SLINGSHOT && draggingSlingshot && currentSlingshot != null) {
                    draggingSlingshot = false;
                    orionVx = (-slingPullDx / 2.2f) * 0.52f;
                    orionVy = (-slingPullDy) * 0.34f;
                    cannonIgnoreTimer = 0.35f;
                    currentSlingshot = null;
                    orionState = ORION_FREE;
                    audio.playSfx("CPW_PuffleCannon_SlingShot_Release01_x3_TwangRubber_CTE02_71.2-TiSh_01-01_sfx.wav");
                    return true;
                }
                if (orionState == ORION_IN_CANNON && currentCannon != null && currentCannon.playerRotatable && draggingPurpleCannon) {
                    draggingPurpleCannon = false;
                    if (!purpleMovedEnough) {
                        fireFromCurrentCannon();
                    }
                    return true;
                }
            }

            touchLeftDown = false;
            touchRightDown = false;
            if (!scaleDetector.isInProgress() && (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_POINTER_DOWN)) {
                int count = event.getPointerCount();
                for (int i = 0; i < count; i++) {
                    float px = event.getX(i);
                    float py = event.getY(i);
                    if (btnHudPause.contains(px, py) || btnHudSlowMo.contains(px, py) || btnHudZoomReset.contains(px, py)) {
                        continue;
                    }
                    if (px < getWidth() * 0.5f) {
                        touchLeftDown = true;
                    } else {
                        touchRightDown = true;
                    }
                }
            }
        }
        return true;
    }

    private void render(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();

        if (gameState == STATE_MAIN_MENU) {
            renderMainMenu(canvas, w, h);
        } else if (gameState == STATE_WORLD_SELECT) {
            renderWorldSelect(canvas, w, h);
        } else if (gameState == STATE_LEVEL_SELECT) {
            renderLevelSelect(canvas, w, h);
        } else if (gameState == STATE_STORY_CUTSCENE) {
            renderStoryCutscene(canvas, w, h);
        } else {
            renderGameplay(canvas, w, h);
            if (gameState == STATE_PAUSED) {
                renderPauseOverlay(canvas, w, h);
            } else if (gameState == STATE_LEVEL_COMPLETE) {
                renderLevelCompleteOverlay(canvas, w, h);
            }
        }

        if (statusBannerTimer > 0f && statusBannerText != null && !statusBannerText.isEmpty()) {
            RectF banner = new RectF(w * 0.14f, h * 0.85f, w * 0.86f, h * 0.95f);
            paint.setColor(Color.argb(225, 18, 28, 48));
            canvas.drawRoundRect(banner, 20f, 20f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3.5f);
            paint.setColor(Color.rgb(255, 205, 70));
            canvas.drawRoundRect(banner, 20f, 20f, paint);
            paint.setStyle(Paint.Style.FILL);

            textPaint.setColor(Color.rgb(255, 235, 130));
            textPaint.setTextSize(h * 0.030f);
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(statusBannerText, banner.centerX(), banner.centerY() + h * 0.011f, textPaint);
        }
    }

    private void drawParallaxBackdrop(Canvas canvas, int w, int h, int wallpaperAlpha, boolean includeMidRuins) {
        canvas.drawColor(Color.rgb(18, 15, 13));

        // Layer 0: Deep Shattered-Moon & Desert Wallpaper (Static, clean framing)
        if (sprites.menuWallpaper != null) {
            rect.set(0, 0, w, h);
            paint.setAlpha(wallpaperAlpha);
            canvas.drawBitmap(sprites.menuWallpaper, null, rect, paint);
            paint.setAlpha(255);
        } else {
            paint.setShader(new LinearGradient(0, 0, 0, h,
                    Color.rgb(42, 34, 28), Color.rgb(18, 14, 12), Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(null);
        }

        if (includeMidRuins) {
            // Layer 1: Horizon Levitating Ruined Structures (calm, stable zero-gravity float)
            Bitmap r1 = sprites.menuRuins[1];
            if (r1 != null) {
                float sc = (h * 0.42f) / r1.getHeight();
                float bob = (float) Math.sin(animClock * 0.85f + 0.5f) * (h * 0.010f);
                float cx = w * 0.24f;
                float cy = h * 0.43f + bob;
                float rot = -6f + (float) Math.sin(animClock * 0.55f) * 1.6f;
                drawBitmapCentered(canvas, r1, cx, cy, sc, sc, rot, 195);
            }
            Bitmap r3 = sprites.menuRuins[3];
            if (r3 != null) {
                float sc = (h * 0.38f) / r3.getHeight();
                float bob = (float) Math.sin(animClock * 0.78f + 2.1f) * (h * 0.011f);
                float cx = w * 0.79f;
                float cy = h * 0.41f + bob;
                float rot = 4f + (float) Math.cos(animClock * 0.60f) * 1.5f;
                drawBitmapCentered(canvas, r3, cx, cy, sc, sc, rot, 205);
            }
            Bitmap r2 = sprites.menuRuins[2];
            if (r2 != null) {
                float sc = (h * 0.25f) / r2.getHeight();
                float bob = (float) Math.sin(animClock * 1.05f + 3.4f) * (h * 0.012f);
                float cx = w * 0.34f;
                float cy = h * 0.22f + bob;
                float rot = -11f + (float) Math.sin(animClock * 0.70f) * 2.0f;
                drawBitmapCentered(canvas, r2, cx, cy, sc, sc, rot, 175);
            }

            // Layer 2: Mid-ground Flanking Levitating Ruined Tower (left tower r0, right-side defective building removed)
            Bitmap r0 = sprites.menuRuins[0];
            if (r0 != null) {
                float sc = (h * 0.62f) / r0.getHeight();
                float bob = (float) Math.sin(animClock * 1.10f) * (h * 0.014f);
                float cx = w * 0.075f;
                float cy = h * 0.50f + bob;
                float rot = 5f + (float) Math.sin(animClock * 0.65f) * 1.8f;
                drawBitmapCentered(canvas, r0, cx, cy, sc, sc, rot, 235);
            }
        }

        // Layer 3: Foreground Zero-Gravity Levitating Rocks & Rubble Shards (stable zero-gravity float)
        float[] rockNormX = {0.16f, 0.31f, 0.46f, 0.66f, 0.74f, 0.85f, 0.21f, 0.81f};
        float[] rockNormY = {0.19f, 0.34f, 0.14f, 0.17f, 0.31f, 0.20f, 0.68f, 0.73f};
        float[] rockScale = {0.52f, 0.38f, 0.32f, 0.44f, 0.36f, 0.54f, 0.48f, 0.50f};
        float[] rockBaseRot = {-14f, 22f, -28f, 15f, -8f, 19f, 12f, -21f};

        for (int i = 0; i < 8; i++) {
            Bitmap rock = sprites.menuRocks[i % 4];
            if (rock != null) {
                float bobY = (float) Math.sin(animClock * (1.25f + (i % 3) * 0.22f) + i * 1.3f) * (h * 0.016f);
                float bobX = (float) Math.cos(animClock * (0.85f + (i % 2) * 0.18f) + i * 0.9f) * (w * 0.006f);
                float cx = w * rockNormX[i] + bobX;
                float cy = h * rockNormY[i] + bobY;
                float sc = ((h * 0.12f) / rock.getHeight()) * rockScale[i];
                float rot = rockBaseRot[i] + (float) Math.sin(animClock * 0.8f + i) * 5.5f;
                int alpha = includeMidRuins ? 225 : 135;
                drawBitmapCentered(canvas, rock, cx, cy, sc, sc, rot, alpha);
            }
        }

        // Layer 4: Foreground Cosmic Amber & Cyan Dust Motes
        for (int i = 0; i < 14; i++) {
            float seedX = ((i * 197 + 43) % 100) / 100f;
            float seedY = ((i * 131 + 19) % 100) / 100f;
            float driftY = (float) Math.sin(animClock * 0.9f + i) * (h * 0.02f);
            float dx = w * seedX;
            float dy = h * seedY + driftY;
            boolean cyanMote = (i % 4 == 0);
            int moteAlpha = 85 + (int) (55f * Math.sin(animClock * 2.1f + i));
            paint.setColor(cyanMote
                    ? Color.argb(moteAlpha, 76, 232, 245)
                    : Color.argb(moteAlpha, 232, 192, 125));
            canvas.drawCircle(dx, dy, h * (0.0035f + (i % 3) * 0.0018f), paint);
        }
    }

    private void renderMainMenu(Canvas canvas, int w, int h) {
        // Multi-layer Parallax Wallpaper + Levitating Ruined Buildings & Zero-Gravity Rocks
        drawParallaxBackdrop(canvas, w, h, 255, true);

        // Subtle dark atmospheric vignette behind UI so text and buttons read with high contrast
        paint.setShader(new LinearGradient(0, 0, 0, h,
                Color.argb(115, 14, 11, 9),
                Color.argb(165, 16, 13, 11),
                Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        // Stylized title: ORION'S JOURNEY (Static & crisp)
        float uiShiftX = 0f;
        float uiShiftY = 0f;

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(h * 0.104f);
        textPaint.setColor(Color.argb(215, 12, 9, 7));
        canvas.drawText("ORION'S JOURNEY", w * 0.5f + uiShiftX + 4f, h * 0.21f + uiShiftY + 5f, textPaint);
        textPaint.setColor(Color.rgb(242, 234, 220)); // Aged Ceramic Ivory (#F2EADC)
        canvas.drawText("ORION'S JOURNEY", w * 0.5f + uiShiftX, h * 0.21f + uiShiftY, textPaint);

        // Subtitle in Bioluminescent Cyan (#4CE8F5) with dark shadow
        textPaint.setTextSize(h * 0.033f);
        textPaint.setColor(Color.argb(200, 10, 14, 18));
        canvas.drawText(langEs ? "EL VIAJE DE ORION" : "THE JOURNEY OF ORION",
                w * 0.5f + uiShiftX + 2f, h * 0.28f + uiShiftY + 3f, textPaint);
        textPaint.setColor(Color.rgb(76, 232, 245));
        canvas.drawText(langEs ? "EL VIAJE DE ORION" : "THE JOURNEY OF ORION",
                w * 0.5f + uiShiftX, h * 0.28f + uiShiftY, textPaint);

        // Primary Action Button: PLAY / JUGAR (Bioluminescent Emerald-Teal + Brass/Cyan rim)
        float btnW = w * 0.29f;
        float btnH = h * 0.122f;
        btnPlay.set(w * 0.5f - btnW * 0.5f, h * 0.35f, w * 0.5f + btnW * 0.5f, h * 0.35f + btnH);
        String playLabel = langEs ? "▶  JUGAR" : "▶  PLAY";
        drawStylizedButton(canvas, btnPlay, Color.rgb(30, 134, 112), playLabel, h * 0.048f);

        // Row 1 buttons: Suit Change, Turbo Mode, Language (Cutscene Obsidian & Bronze Palette)
        float subW = w * 0.27f;
        float subH = h * 0.106f;
        float row1Y = h * 0.535f;

        String suitName = langEs ? SUIT_NAMES_ES[selectedSuitIndex] : SUIT_NAMES_EN[selectedSuitIndex];
        btnSuitSelect.set(w * 0.07f, row1Y, w * 0.07f + subW, row1Y + subH);
        drawStylizedButton(canvas, btnSuitSelect, Color.rgb(122, 76, 42),
                (langEs ? "TRAJE: " : "SUIT: ") + suitName, h * 0.028f);
        Bitmap suitBmp = sprites.orionSuits[selectedSuitIndex];
        if (suitBmp != null) {
            float iconScale = (subH * 0.68f) / suitBmp.getHeight();
            drawBitmapCentered(canvas, suitBmp, btnSuitSelect.left + subH * 0.44f, btnSuitSelect.centerY(), iconScale, iconScale, 0f, 255);
        }

        btnTurboToggle.set(w * 0.5f - subW * 0.5f, row1Y, w * 0.5f + subW * 0.5f, row1Y + subH);
        drawStylizedButton(canvas, btnTurboToggle,
                turboMode ? Color.rgb(184, 96, 32) : Color.rgb(42, 48, 54),
                langEs ? (turboMode ? "TURBO: SÍ 🔥" : "TURBO: NO") : (turboMode ? "TURBO: ON 🔥" : "TURBO: OFF"),
                h * 0.028f);

        btnLanguage.set(w * 0.93f - subW, row1Y, w * 0.93f, row1Y + subH);
        drawStylizedButton(canvas, btnLanguage, Color.rgb(38, 68, 86),
                langEs ? "IDIOMA: ESPAÑOL 🌐" : "LANG: ENGLISH 🌐", h * 0.028f);

        // Row 2 buttons: Control Mode & Sound
        float row2Y = h * 0.68f;
        btnControlMode.set(w * 0.20f, row2Y, w * 0.20f + subW, row2Y + subH);
        drawStylizedButton(canvas, btnControlMode, Color.rgb(64, 52, 44),
                langEs ? ("CONTROL: " + (tiltControls ? "INCLINAR" : "TÁCTIL"))
                       : ("CONTROL: " + (tiltControls ? "TILT" : "TOUCH")), h * 0.028f);

        btnSound.set(w * 0.80f - subW, row2Y, w * 0.80f, row2Y + subH);
        drawStylizedButton(canvas, btnSound, audio.isMuted() ? Color.rgb(118, 42, 36) : Color.rgb(86, 62, 44),
                langEs ? (audio.isMuted() ? "SONIDO: NO" : "SONIDO: SÍ")
                       : (audio.isMuted() ? "SOUND: OFF" : "SOUND: ON"), h * 0.028f);

        // Director & studio credit pill footer
        RectF creditBar = new RectF(w * 0.16f, h * 0.875f, w * 0.84f, h * 0.955f);
        paint.setColor(Color.argb(190, 20, 16, 14));
        canvas.drawRoundRect(creditBar, 16f, 16f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(Color.argb(160, 175, 132, 85));
        canvas.drawRoundRect(creditBar, 16f, 16f, paint);
        paint.setStyle(Paint.Style.FILL);

        textPaint.setColor(Color.rgb(230, 218, 198));
        textPaint.setTextSize(h * 0.025f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(langEs
                ? "Dirigido por Cristian Camilo Tabares  •  Cristian Camilo Tabares Development"
                : "Directed by Cristian Camilo Tabares  •  Cristian Camilo Tabares Development",
                w * 0.5f, creditBar.centerY() + h * 0.009f, textPaint);
    }

    private void renderWorldSelect(Canvas canvas, int w, int h) {
        // Atmospheric parallax background with deep obsidian tint
        drawParallaxBackdrop(canvas, w, h, 90, false);
        canvas.drawColor(Color.argb(175, 16, 13, 11));

        btnBack.set(w * 0.03f, h * 0.05f, w * 0.17f, h * 0.15f);
        drawStylizedButton(canvas, btnBack, Color.rgb(112, 48, 38), langEs ? "← MENÚ" : "← MENU", h * 0.031f);

        btnTurboToggle.set(w * 0.73f, h * 0.05f, w * 0.97f, h * 0.15f);
        drawStylizedButton(canvas, btnTurboToggle,
                turboMode ? Color.rgb(184, 96, 32) : Color.rgb(42, 48, 54),
                langEs ? (turboMode ? "MODO TURBO: SÍ 🔥" : "MODO TURBO: NO")
                       : (turboMode ? "TURBO MODE: ON 🔥" : "TURBO MODE: OFF"), h * 0.028f);

        textPaint.setColor(Color.rgb(242, 234, 220));
        textPaint.setTextSize(h * 0.050f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(langEs ? "CRÓNICAS DEL COSMOS" : "CHRONICLES OF THE COSMOS", w * 0.45f, h * 0.125f, textPaint);

        String[] titles = langEs
                ? new String[]{"1. DESIERTO DE CENIZA", "2. EL TALLER OLVIDADO", "3. COSMOS SILENCIOSO"}
                : new String[]{"1. ASHEN DESERT", "2. FORGOTTEN WORKSHOP", "3. SILENT COSMOS"};
        String[] subtitles = langEs
                ? new String[]{"Niveles 1 – 12 • Jefe I", "Niveles 13 – 24 • Jefe II", "Niveles 25 – 36 • Etapas Extra"}
                : new String[]{"Levels 1 – 12 • Boss Stage I", "Levels 13 – 24 • Boss Stage II", "Levels 25 – 36 • Bonus Stages"};
        int[] cardTopColors = {
            Color.rgb(62, 44, 30),  // World 1: Rusted Ochre / Desert Iron
            Color.rgb(46, 34, 28),  // World 2: Architect's Umber Workshop
            Color.rgb(24, 38, 50)   // World 3: Deep Abyssal Starlight
        };
        int[] cardBotColors = {
            Color.rgb(28, 20, 15),
            Color.rgb(22, 17, 14),
            Color.rgb(14, 22, 30)
        };

        float cardW = w * 0.28f;
        float cardH = h * 0.68f;
        float gap = w * 0.035f;
        float startX = (w - (3 * cardW + 2 * gap)) * 0.5f;

        for (int i = 0; i < 3; i++) {
            float x0 = startX + i * (cardW + gap);
            float y0 = h * 0.21f;
            worldCards[i].set(x0, y0, x0 + cardW, y0 + cardH);
            boolean unlocked = isWorldUnlocked(i + 1);

            paint.setShader(new LinearGradient(0, y0, 0, y0 + cardH,
                    unlocked ? cardTopColors[i] : Color.rgb(30, 28, 26),
                    unlocked ? cardBotColors[i] : Color.rgb(18, 16, 15),
                    Shader.TileMode.CLAMP));
            canvas.drawRoundRect(worldCards[i], 24f, 24f, paint);
            paint.setShader(null);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4.5f);
            paint.setColor(unlocked ? Color.rgb(185, 142, 92) : Color.rgb(85, 78, 72));
            canvas.drawRoundRect(worldCards[i], 24f, 24f, paint);
            paint.setStyle(Paint.Style.FILL);

            textPaint.setColor(unlocked ? Color.rgb(245, 238, 225) : Color.rgb(155, 148, 140));
            textPaint.setTextSize(h * 0.032f);
            canvas.drawText(titles[i], worldCards[i].centerX(), y0 + cardH * 0.13f, textPaint);

            textPaint.setColor(unlocked ? Color.rgb(210, 180, 140) : Color.rgb(125, 118, 112));
            textPaint.setTextSize(h * 0.023f);
            canvas.drawText(subtitles[i], worldCards[i].centerX(), y0 + cardH * 0.22f, textPaint);

            Bitmap scene = sprites.worldScenes[i];
            if (scene != null) {
                RectF imgRect = new RectF(x0 + 14f, y0 + cardH * 0.27f, x0 + cardW - 14f, y0 + cardH * 0.70f);
                canvas.drawBitmap(scene, null, imgRect, paint);
                if (!unlocked) {
                    paint.setColor(Color.argb(185, 14, 12, 10));
                    canvas.drawRect(imgRect, paint);
                    textPaint.setColor(Color.rgb(225, 185, 115));
                    textPaint.setTextSize(h * 0.032f);
                    canvas.drawText(langEs ? "🔒 BLOQUEADO (90%)" : "🔒 LOCKED (90%)", imgRect.centerX(), imgRect.centerY() + h * 0.012f, textPaint);
                }
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2.5f);
                paint.setColor(unlocked ? Color.rgb(185, 142, 92) : Color.rgb(90, 82, 75));
                canvas.drawRect(imgRect, paint);
                paint.setStyle(Paint.Style.FILL);
            }

            int worldRings = 0;
            int worldCleared90 = 0;
            for (int l = i * 12 + 1; l <= (i + 1) * 12; l++) {
                worldRings += bestRings[l];
                if (hasMet90Percent(l)) worldCleared90++;
            }
            textPaint.setColor(worldCleared90 == 12 ? Color.rgb(76, 232, 245) : Color.rgb(232, 195, 120));
            textPaint.setTextSize(h * 0.027f);
            canvas.drawText((langEs ? "Completados (≥90%): " : "Cleared (≥90%): ") + worldCleared90 + " / 12",
                    worldCards[i].centerX(), y0 + cardH * 0.82f, textPaint);
            textPaint.setColor(Color.rgb(215, 205, 190));
            canvas.drawText((langEs ? "Aros recolectados: " : "Rings collected: ") + worldRings,
                    worldCards[i].centerX(), y0 + cardH * 0.92f, textPaint);
        }
    }

    private void renderLevelSelect(Canvas canvas, int w, int h) {
        // Atmospheric parallax background tinted per world using the Cutscene Color Palette
        drawParallaxBackdrop(canvas, w, h, 85, false);
        int[] worldTint = {
            Color.argb(180, 28, 20, 15), // World 1: Ashen Desert Umber
            Color.argb(185, 22, 16, 14), // World 2: Architect's Workshop Obsidian
            Color.argb(180, 14, 22, 30)  // World 3: Silent Cosmos Abyssal Blue
        };
        canvas.drawColor(worldTint[(selectedWorld - 1) % 3]);

        btnBack.set(w * 0.03f, h * 0.05f, w * 0.17f, h * 0.15f);
        drawStylizedButton(canvas, btnBack, Color.rgb(112, 48, 38), langEs ? "← MUNDOS" : "← WORLDS", h * 0.029f);

        btnReplayCutscene.set(w * 0.185f, h * 0.05f, w * 0.345f, h * 0.15f);
        drawStylizedButton(canvas, btnReplayCutscene, Color.rgb(122, 76, 42), langEs ? "🎬 HISTORIA" : "🎬 STORY", h * 0.027f);

        btnTurboToggle.set(w * 0.75f, h * 0.05f, w * 0.97f, h * 0.15f);
        drawStylizedButton(canvas, btnTurboToggle,
                turboMode ? Color.rgb(184, 96, 32) : Color.rgb(42, 48, 54),
                langEs ? (turboMode ? "MODO TURBO: SÍ 🔥" : "MODO TURBO: NO")
                       : (turboMode ? "TURBO MODE: ON 🔥" : "TURBO MODE: OFF"), h * 0.027f);

        String[] worldNames = langEs
                ? new String[]{"MUNDO 1: DESIERTO DE CENIZA", "MUNDO 2: TALLER OLVIDADO", "MUNDO 3: COSMOS SILENCIOSO"}
                : new String[]{"WORLD 1: ASHEN DESERT", "WORLD 2: FORGOTTEN WORKSHOP", "WORLD 3: SILENT COSMOS"};
        textPaint.setColor(Color.rgb(242, 234, 220));
        textPaint.setTextSize(h * 0.038f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(worldNames[(selectedWorld - 1) % 3], w * 0.545f, h * 0.118f, textPaint);

        int cols = 4;
        int rows = 3;
        float btnH = h * 0.225f;
        float btnW = btnH * 1.12f;
        float gapX = w * 0.050f;
        float gapY = h * 0.035f;
        float startX = (w - (cols * btnW + (cols - 1) * gapX)) * 0.5f;
        float startY = h * 0.20f;

        for (int i = 0; i < 12; i++) {
            int r = i / cols;
            int c = i % cols;
            float x0 = startX + c * (btnW + gapX);
            float y0 = startY + r * (btnH + gapY);
            levelButtons[i].set(x0, y0, x0 + btnW, y0 + btnH);

            int lvl = (selectedWorld - 1) * 12 + (i + 1);
            boolean isBossLvl = (lvl == 12 || lvl == 24);
            boolean unlocked = isLevelUnlocked(lvl);
            boolean met90 = hasMet90Percent(lvl);
            int pct = getLevelPercent(lvl);

            // Custom Orion Metallic / Observatory Level Card (replaces cartoon square buttons)
            RectF sqRect = new RectF(
                    levelButtons[i].left + btnW * 0.06f,
                    y0,
                    levelButtons[i].right - btnW * 0.06f,
                    y0 + btnH * 0.74f);

            int topCol, botCol, borderCol;
            if (!unlocked) {
                topCol = Color.rgb(32, 29, 27);
                botCol = Color.rgb(20, 18, 17);
                borderCol = Color.rgb(78, 72, 66);
            } else if (met90) {
                topCol = Color.rgb(28, 72, 74);
                botCol = Color.rgb(16, 42, 46);
                borderCol = Color.rgb(76, 232, 245); // Bioluminescent Cyan rim when ≥90% cleared!
            } else if (isBossLvl) {
                topCol = Color.rgb(92, 36, 30);
                botCol = Color.rgb(48, 18, 15);
                borderCol = Color.rgb(225, 140, 85);
            } else {
                topCol = Color.rgb(58, 44, 34);
                botCol = Color.rgb(32, 24, 19);
                borderCol = Color.rgb(185, 142, 92); // Weathered Brass rim
            }

            paint.setShader(new LinearGradient(0, sqRect.top, 0, sqRect.bottom, topCol, botCol, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(sqRect, 18f, 18f, paint);
            paint.setShader(null);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(unlocked && met90 ? 4.0f : 3.2f);
            paint.setColor(borderCol);
            canvas.drawRoundRect(sqRect, 18f, 18f, paint);
            paint.setStyle(Paint.Style.FILL);

            String lbl = !unlocked ? (lvl + " 🔒") : isBossLvl ? (lvl + " ★") : String.valueOf(lvl);
            textPaint.setTextSize(h * 0.044f);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(Color.argb(180, 10, 8, 6));
            canvas.drawText(lbl, sqRect.centerX() + 2f, sqRect.centerY() + h * 0.016f + 3f, textPaint);
            textPaint.setColor(unlocked ? Color.rgb(245, 238, 225) : Color.rgb(145, 138, 132));
            canvas.drawText(lbl, sqRect.centerX(), sqRect.centerY() + h * 0.016f, textPaint);

            // Clean bottom score pill below each card
            RectF pillRect = new RectF(
                    levelButtons[i].left + btnW * 0.04f,
                    y0 + btnH * 0.72f,
                    levelButtons[i].right - btnW * 0.04f,
                    y0 + btnH * 0.98f);
            paint.setColor(Color.argb(225, 18, 15, 13));
            canvas.drawRoundRect(pillRect, 12f, 12f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f);
            paint.setColor(unlocked && met90 ? Color.rgb(76, 232, 245) : Color.rgb(130, 102, 72));
            canvas.drawRoundRect(pillRect, 12f, 12f, paint);
            paint.setStyle(Paint.Style.FILL);

            if (unlocked) {
                Bitmap ring = met90 ? sprites.ringGold : sprites.ringOrange;
                if (ring != null) {
                    float sc = (pillRect.height() * 0.74f) / ring.getHeight();
                    drawBitmapCentered(canvas, ring, pillRect.left + pillRect.width() * 0.20f, pillRect.centerY(), sc, sc, 0f, 255);
                }
                textPaint.setColor(met90 ? Color.rgb(76, 232, 245) : Color.rgb(232, 195, 120));
                textPaint.setTextSize(h * 0.024f);
                textPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(pct + "%" + (met90 ? " ✓" : ""), pillRect.left + pillRect.width() * 0.62f, pillRect.centerY() + h * 0.009f, textPaint);
            } else {
                textPaint.setColor(Color.rgb(160, 150, 140));
                textPaint.setTextSize(h * 0.022f);
                textPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("REQ 90%", pillRect.centerX(), pillRect.centerY() + h * 0.008f, textPaint);
            }
        }
    }

    private void renderStoryCutscene(Canvas canvas, int w, int h) {
        // Deep melancholic observatory/cinema backdrop
        canvas.drawColor(Color.rgb(16, 14, 13));

        int beatIdx = Math.max(0, Math.min(STORY_PLANO_NUMS.length - 1, storyBeatIndex));
        int planoNum = STORY_PLANO_NUMS[beatIdx];
        Bitmap slideBmp = sprites.getStorySlide(planoNum);

        // Top section: 16:9 Scene Image Frame (h * 0.03f to h * 0.71f)
        float maxImgH = h * 0.67f;
        float maxImgW = w * 0.88f;
        float imgH = maxImgH;
        float imgW = imgH * (16f / 9f);
        if (imgW > maxImgW) {
            imgW = maxImgW;
            imgH = imgW * (9f / 16f);
        }
        float imgLeft = (w - imgW) * 0.5f;
        float imgTop = h * 0.035f + (maxImgH - imgH) * 0.5f;
        RectF imgFrame = new RectF(imgLeft, imgTop, imgLeft + imgW, imgTop + imgH);

        // Subtle outer glow & shadow around the artwork frame
        RectF outerFrame = new RectF(imgFrame.left - 5f, imgFrame.top - 5f, imgFrame.right + 5f, imgFrame.bottom + 5f);
        paint.setColor(Color.rgb(34, 28, 24));
        canvas.drawRoundRect(outerFrame, 16f, 16f, paint);

        if (slideBmp != null) {
            int alpha = Math.max(0, Math.min(255, (int) (storySlideFade * 255f)));
            paint.setAlpha(alpha);
            canvas.drawBitmap(slideBmp, null, imgFrame, paint);
            paint.setAlpha(255);
        }

        // Warm weathered-bronze border around the scene image
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3.5f);
        paint.setColor(Color.rgb(175, 132, 85));
        canvas.drawRoundRect(imgFrame, 10f, 10f, paint);
        paint.setStyle(Paint.Style.FILL);

        // Top-left Plano indicator pill
        RectF planoBadge = new RectF(w * 0.03f, h * 0.04f, w * 0.17f, h * 0.115f);
        paint.setColor(Color.argb(210, 24, 20, 18));
        canvas.drawRoundRect(planoBadge, 14f, 14f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(Color.rgb(175, 132, 85));
        canvas.drawRoundRect(planoBadge, 14f, 14f, paint);
        paint.setStyle(Paint.Style.FILL);

        textPaint.setColor(Color.rgb(235, 215, 185));
        textPaint.setTextSize(h * 0.025f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText((langEs ? "PLANO " : "SCENE ") + planoNum + " / 10",
                planoBadge.centerX(), planoBadge.centerY() + h * 0.009f, textPaint);

        // Top-right Skip button
        btnStorySkip.set(w * 0.83f, h * 0.04f, w * 0.97f, h * 0.115f);
        drawStylizedButton(canvas, btnStorySkip, Color.rgb(68, 58, 52),
                langEs ? "SALTAR ⏭" : "SKIP ⏭", h * 0.024f);

        // Bottom section: Rosalina-style storybook subtitle box & navigation controls
        RectF subBox = new RectF(w * 0.03f, h * 0.725f, w * 0.97f, h * 0.965f);
        paint.setColor(Color.argb(242, 24, 20, 18));
        canvas.drawRoundRect(subBox, 22f, 22f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(Color.rgb(185, 142, 92));
        canvas.drawRoundRect(subBox, 22f, 22f, paint);
        paint.setStyle(Paint.Style.FILL);

        // Left button: Previous slide / Exit
        btnStoryPrev.set(w * 0.045f, h * 0.765f, w * 0.205f, h * 0.925f);
        String prevLabel = (beatIdx > 0)
                ? (langEs ? "← ANTERIOR" : "← BACK")
                : (langEs ? "← SALIR" : "← EXIT");
        drawStylizedButton(canvas, btnStoryPrev,
                (beatIdx > 0) ? Color.rgb(92, 74, 60) : Color.rgb(70, 56, 48),
                prevLabel, h * 0.025f);

        // Right button: Next slide OR Start Level 1 on the final beat
        boolean isLastBeat = (beatIdx == STORY_PLANO_NUMS.length - 1);
        btnStoryNext.set(w * 0.785f, h * 0.765f, w * 0.955f, h * 0.925f);
        String nextLabel = isLastBeat
                ? (langEs ? "▶ INICIAR NIVEL" : "▶ START LEVEL")
                : (langEs ? "SIGUIENTE →" : "NEXT →");
        int nextColor = isLastBeat ? Color.rgb(34, 153, 120) : Color.rgb(176, 112, 52);
        drawStylizedButton(canvas, btnStoryNext, nextColor, nextLabel, h * 0.025f);

        // Typewriter subtitle text in the center area (between Prev and Next buttons)
        String fullLine = (langEs ? STORY_LINES_ES : STORY_LINES_EN)[beatIdx];
        int charCount = Math.max(0, Math.min(fullLine.length(), (int) storyTypedProgress));
        String visibleLine = fullLine.substring(0, charCount);
        boolean typingDone = (charCount >= fullLine.length());
        String cursor = !typingDone ? "▌" : (((int) (animClock * 2.5f) % 2 == 0) ? "  ✦" : "");

        textPaint.setColor(Color.rgb(245, 238, 225));
        textPaint.setTextSize(h * 0.034f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        float textCenterX = (btnStoryPrev.right + btnStoryNext.left) * 0.5f;
        float maxTextW = (btnStoryNext.left - btnStoryPrev.right) - w * 0.04f;

        if (textPaint.measureText(fullLine + "  ✦") <= maxTextW) {
            canvas.drawText(visibleLine + cursor, textCenterX, subBox.top + subBox.height() * 0.48f, textPaint);
        } else {
            // Split fullLine into 2 balanced lines at word boundary, then reveal up to charCount
            int splitPos = fullLine.length() / 2;
            int spaceBefore = fullLine.lastIndexOf(' ', splitPos);
            int spaceAfter = fullLine.indexOf(' ', splitPos);
            if (spaceBefore > 0 && (spaceAfter < 0 || (splitPos - spaceBefore) <= (spaceAfter - splitPos))) {
                splitPos = spaceBefore;
            } else if (spaceAfter > 0) {
                splitPos = spaceAfter;
            }
            if (charCount <= splitPos) {
                canvas.drawText(visibleLine + cursor, textCenterX, subBox.top + subBox.height() * 0.36f, textPaint);
            } else {
                String line1 = fullLine.substring(0, splitPos);
                String line2 = fullLine.substring(Math.min(fullLine.length(), splitPos + 1), charCount);
                canvas.drawText(line1, textCenterX, subBox.top + subBox.height() * 0.34f, textPaint);
                canvas.drawText(line2 + cursor, textCenterX, subBox.top + subBox.height() * 0.57f, textPaint);
            }
        }

        // Subtle footer prompt inside subtitle panel
        textPaint.setColor(Color.rgb(165, 148, 130));
        textPaint.setTextSize(h * 0.022f);
        String footerPrompt = isLastBeat
                ? (langEs ? "Fin del prólogo • Pulsa ▶ INICIAR NIVEL para comenzar" : "End of prologue • Tap ▶ START LEVEL to begin")
                : (langEs ? ("Fragmento " + (beatIdx + 1) + " de " + STORY_PLANO_NUMS.length + " • Toca la pantalla o SIGUIENTE para avanzar")
                          : ("Part " + (beatIdx + 1) + " of " + STORY_PLANO_NUMS.length + " • Tap screen or NEXT to continue"));
        canvas.drawText(footerPrompt, textCenterX, subBox.bottom - h * 0.026f, textPaint);
    }

    private void renderGameplay(Canvas canvas, int w, int h) {
        if (level == null) return;

        if (level.worldNum == 1 && sprites.world1Sky != null) {
            // World 1: Cosmic deep space sky with smooth planetary camera parallax
            float skyScale = Math.max((float) w / sprites.world1Sky.getWidth(), (float) h / sprites.world1Sky.getHeight()) * 1.35f;
            float skyW = sprites.world1Sky.getWidth() * skyScale;
            float skyH = sprites.world1Sky.getHeight() * skyScale;
            float shiftX = -(camX * 0.045f) % skyW;
            if (shiftX > 0f) shiftX -= skyW;
            float shiftY = (h - skyH) * 0.5f - Math.max(-skyH * 0.15f, Math.min(skyH * 0.15f, (camY - level.groundY * 0.5f) * 0.02f));
            for (float px = shiftX; px < w; px += skyW) {
                rect.set(px, shiftY, px + skyW, shiftY + skyH);
                canvas.drawBitmap(sprites.world1Sky, null, rect, null);
            }
        } else {
            int bgColor = (level.worldNum == 2) ? Color.rgb(247, 138, 73) : Color.rgb(111, 23, 157);
            canvas.drawColor(bgColor);
        }

        camZoom = (h / 680f) * userZoomMultiplier;

        canvas.save();
        canvas.translate(w * 0.5f, h * 0.5f);
        canvas.scale(camZoom, camZoom);
        canvas.translate(-camX, -camY);

        float viewHalfW = (w * 0.5f) / camZoom;
        float camLeft = camX - viewHalfW - 120f;
        float camRight = camX + viewHalfW + 120f;

        // Background Elements: Static Grounded Ruined Towers & Floating Rubble for World 1, or Clouds for World 2/3
        if (level.worldNum == 1) {
            // A) Static Ruined Buildings: Only 2 (or 3 in wide levels) widely separated structures emerging from the ground
            float minLevelX = Math.min(level.spawnX, level.finishX) - 450f;
            float maxLevelX = Math.max(level.spawnX, level.finishX) + 450f;
            float span = Math.max(1200f, maxLevelX - minLevelX);

            float[] bFractions = (span > 1800f) ? new float[]{0.18f, 0.52f, 0.84f} : new float[]{0.25f, 0.75f};
            Bitmap[] bRuins = { sprites.world1Ruins[0], sprites.world1Ruins[1], sprites.world1Ruins[2] };

            for (int b = 0; b < bFractions.length; b++) {
                Bitmap bBmp = bRuins[b % bRuins.length];
                if (bBmp != null) {
                    float bx = minLevelX + span * bFractions[b];
                    float bH = 680f;
                    float bW = bH * ((float) bBmp.getWidth() / bBmp.getHeight());
                    float bL = bx - bW * 0.5f;
                    float bR = bL + bW;
                    // Frustum culling: only draw if in camera view
                    if (bR >= camLeft && bL <= camRight) {
                        float bBottom = level.groundY + 45f; // Buried in dunes so the bottom edge is hidden naturally
                        float bTop = bBottom - bH;
                        rect.set(bL, bTop, bR, bBottom);
                        // Static, completely still
                        canvas.drawBitmap(bBmp, null, rect, null);
                    }
                }
            }

            // B) Zero-Gravity Debris: Only 4 small floating rock shards, gently bobbing in cosmic space
            for (int r = 0; r < 4; r++) {
                float rx = minLevelX + span * (0.16f + r * 0.23f);
                float ry = Math.min(level.spawnY, level.finishY) + ((r * 170f) % 450f) - 60f;
                Bitmap rockBmp = sprites.menuRocks[r % sprites.menuRocks.length];
                if (rockBmp != null) {
                    if (rx + 45f >= camLeft && rx - 45f <= camRight) {
                        float bobY = (float) Math.sin(animClock * 1.05f + r * 1.6f) * 10f;
                        float bobRot = (float) Math.sin(animClock * 0.55f + r * 1.1f) * 5f;
                        drawBitmapCentered(canvas, rockBmp, rx, ry + bobY, 0.42f, 0.42f, bobRot, 205);
                    }
                }
            }
        } else {
            for (LevelData.BgElement bg : level.backgrounds) {
                if (bg.x + 150f >= camLeft && bg.x - 150f <= camRight) {
                    Bitmap bmp = sprites.bgClouds[Math.abs(bg.type - 1) % sprites.bgClouds.length];
                    if (bmp != null) {
                        drawBitmapCentered(canvas, bmp, bg.x, bg.y, bg.scaleX * 0.85f, bg.scaleY * 0.85f, 0f, 195);
                    }
                }
            }
        }

        // Ground / Ceiling FallZone
        if (level.worldNum == 1 && sprites.world1DeathDunes != null) {
            // Ashen Dunes Death Zone: Organic, seamless sand dunes undulating naturally
            // The crest of the sand dunes sits around level.groundY
            float duneH = 460f;
            float duneW = duneH * ((float) sprites.world1DeathDunes.getWidth() / sprites.world1DeathDunes.getHeight());
            float topY = level.groundY - 35f;
            float startTile = (float) Math.floor(camLeft / duneW) * duneW;
            float endTile = camRight + duneW;

            // Deep sand bedrock below the dune crests (culled to visible viewport)
            paint.setColor(Color.rgb(138, 118, 92));
            canvas.drawRect(camLeft - 100f, topY + duneH - 4f, camRight + 100f, topY + duneH + 1200f, paint);

            // Undulating, seamless dunes along the death horizon (strictly visible tiles)
            for (float tx = startTile; tx < endTile; tx += duneW) {
                rect.set(tx, topY, tx + duneW, topY + duneH);
                canvas.drawBitmap(sprites.world1DeathDunes, null, rect, null);
            }
            // Note: NO artificial horizontal line in World 1; the natural dunes form the boundary
        } else {
            paint.setColor(level.worldNum == 2 ? Color.argb(190, 180, 60, 20)
                    : Color.argb(190, 55, 10, 90));
            canvas.drawRect(camLeft - 200f, level.groundY, camRight + 200f, level.groundY + 1200f, paint);
            paint.setColor(Color.argb(210, 255, 255, 255));
            paint.setStrokeWidth(6f);
            canvas.drawLine(camLeft - 200f, level.groundY, camRight + 200f, level.groundY, paint);
        }

        if (level.worldNum >= 3) {
            paint.setColor(Color.argb(140, 255, 80, 180));
            canvas.drawLine(camX - 4000f, level.ceilingY, camX + 4000f, level.ceilingY, paint);
        }

        // Rings
        for (LevelData.RingItem item : level.rings) {
            if (!item.collected) {
                Bitmap obmp = (item.type == 3) ? sprites.ringGold
                        : (item.type == 2) ? sprites.ringSilver
                        : turboMode ? sprites.ringFire : sprites.ringOrange;
                drawBitmapCentered(canvas, obmp, item.x, item.y, 1.15f, 1.15f, 0f, 255);
            } else if (item.collectAnim < 1f) {
                float sc = 1.15f + item.collectAnim * 1.1f;
                int alpha = (int) ((1f - item.collectAnim) * 255f);
                drawBitmapCentered(canvas, sprites.ringGold, item.x, item.y - item.collectAnim * 30f, sc, sc, 0f, alpha);
            }
        }

        // Giant Finish Ring (upright, no axis rotation)
        if (level.finishActive && sprites.giantFinishRing != null) {
            float pulse = 1.0f + 0.06f * (float) Math.sin(animClock * 4.5f);
            drawBitmapCentered(canvas, sprites.giantFinishRing, level.finishX, level.finishY, pulse * 1.1f, pulse * 1.1f, 0f, 255);
        }

        // Obstacles & Bosses
        int fanFrameIdx = ((int) (animClock * 18f)) % 3;
        for (LevelData.ObstacleEntity o : level.obstacles) {
            if (!o.active) continue;
            float hitScale = 1.0f + o.hitAnim * 0.25f;

            if (o.isBoss) {
                if (o.type == 27) {
                    float swingAngle = (float) Math.sin(o.bossSwingPhase) * 1.05f;
                    float anvilX = o.x + (float) Math.sin(swingAngle) * 195f;
                    float anvilY = o.y + (float) Math.cos(swingAngle) * 195f;
                    paint.setColor(Color.rgb(70, 70, 75));
                    paint.setStrokeWidth(10f);
                    canvas.drawLine(o.x, o.y, anvilX, anvilY, paint);
                    Bitmap weightBmp = (sprites.magnet != null) ? sprites.magnet : sprites.anvil;
                    drawBitmapCentered(canvas, weightBmp, anvilX, anvilY, 0.85f, 0.85f, (float) Math.toDegrees(-swingAngle), 255);
                }

                Bitmap shipBmp = (o.type == 27 && sprites.bossShip2 != null) ? sprites.bossShip2 : sprites.bossShip1;
                drawBitmapCentered(canvas, shipBmp, o.x, o.y, 0.72f * hitScale, 0.72f * hitScale, 0f, 255);

                float barW = 160f;
                float barH = 16f;
                rect.set(o.x - barW * 0.5f, o.y - 120f, o.x + barW * 0.5f, o.y - 120f + barH);
                paint.setColor(Color.rgb(60, 20, 20));
                canvas.drawRoundRect(rect, 6f, 6f, paint);
                rect.set(o.x - barW * 0.5f, o.y - 120f, o.x - barW * 0.5f + barW * (o.bossHp / (float) o.bossMaxHp), o.y - 120f + barH);
                paint.setColor(Color.rgb(46, 204, 113));
                canvas.drawRoundRect(rect, 6f, 6f, paint);
                continue;
            }

            switch (o.type) {
                case 1:
                case 4:
                case 5:
                case 6:
                    drawBitmapCentered(canvas, sprites.piano, o.x, o.y, 0.70f * hitScale, 0.70f * hitScale, o.angleDeg, 255);
                    break;
                case 2:
                    drawBitmapCentered(canvas, sprites.cactus, o.x, o.y, 0.72f * hitScale, 0.72f * hitScale, o.angleDeg, 255);
                    break;
                case 3:
                    drawBitmapCentered(canvas, sprites.greenBalloon, o.x, o.y, 0.90f * hitScale, 0.90f * hitScale, 0f, 255);
                    break;
                case 9:
                    if (o.cloudFadeTimer > 0f) {
                        int alpha = (int) Math.max(0f, (1f - o.cloudFadeTimer * 2.2f) * 255f);
                        drawBitmapCentered(canvas, sprites.poofyCloudPuff != null ? sprites.poofyCloudPuff : sprites.poofyCloudOrange,
                                o.x, o.y, 0.85f + o.cloudFadeTimer * 0.5f, 0.85f + o.cloudFadeTimer * 0.5f, 0f, alpha);
                    } else {
                        drawBitmapCentered(canvas, sprites.poofyCloudOrange, o.x, o.y, 0.75f, 0.75f, 0f, 240);
                    }
                    break;
                case 10:
                    drawBitmapCentered(canvas, sprites.redBalloon, o.x, o.y, 0.90f * hitScale, 0.90f * hitScale, 0f, 255);
                    break;
                case 11:
                    drawBitmapCentered(canvas, sprites.slingshotBalloons, o.x, o.y - 45f, 0.85f, 0.85f, 0f, 255);
                    float bucketX = (orionState == ORION_IN_SLINGSHOT && currentSlingshot == o) ? orionX : o.x;
                    float bucketY = (orionState == ORION_IN_SLINGSHOT && currentSlingshot == o) ? orionY : (o.y + 45f);
                    paint.setColor(Color.rgb(210, 170, 95));
                    paint.setStrokeWidth(7f);
                    canvas.drawLine(o.x - 45f, o.y - 10f, bucketX - 20f, bucketY, paint);
                    canvas.drawLine(o.x + 45f, o.y - 10f, bucketX + 20f, bucketY, paint);
                    drawBitmapCentered(canvas, sprites.slingshotBarrel, bucketX, bucketY, 0.85f, 0.85f, 0f, 255);
                    break;
                case 12:
                case 13:
                case 14:
                case 15:
                case 16: {
                    double rad = Math.toRadians(o.angleDeg);
                    float blowX = (float) Math.sin(rad);
                    float blowY = -(float) Math.cos(rad);
                    paint.setColor(Color.argb(95, 255, 255, 255));
                    paint.setStrokeWidth(4f);
                    for (int s = -1; s <= 1; s++) {
                        float perpX = -blowY * (s * 34f);
                        float perpY = blowX * (s * 34f);
                        float offset = ((animClock * 260f + s * 60f) % 140f);
                        float sx = o.x + perpX + blowX * (30f + offset);
                        float sy = o.y + perpY + blowY * (30f + offset);
                        canvas.drawLine(sx, sy, sx + blowX * 150f, sy + blowY * 150f, paint);
                    }
                    Bitmap fanBmp = sprites.fanFrames[fanFrameIdx] != null ? sprites.fanFrames[fanFrameIdx] : sprites.fanFrames[0];
                    drawBitmapCentered(canvas, fanBmp, o.x, o.y, 0.72f, 0.72f, o.angleDeg, 255);
                    break;
                }
                case 17:
                    drawBitmapCentered(canvas, sprites.plant, o.x, o.y, 0.80f * hitScale, 0.80f * hitScale, o.angleDeg, 255);
                    break;
                case 19:
                case 20:
                case 21:
                case 22:
                    drawBitmapCentered(canvas, sprites.anvil, o.x, o.y, 0.70f * hitScale, 0.70f * hitScale, o.angleDeg, 255);
                    break;
                case 23:
                case 24:
                case 26:
                    drawBitmapCentered(canvas, sprites.crabbyClaw != null ? sprites.crabbyClaw : sprites.bossShip1,
                            o.x, o.y + (float) Math.sin(animClock * 3f) * 18f, 0.9f, 0.9f, 0f, 255);
                    break;
                default:
                    break;
            }
        }

        // Trail
        for (TrailDot d : trail) {
            paint.setColor(d.color);
            paint.setAlpha((int) (d.life * 190f));
            canvas.drawCircle(d.x, d.y, d.size * d.life, paint);
        }
        paint.setAlpha(255);

        // Render Orion (always using the selected character suit, with flame aura in Turbo Mode)
        Bitmap activeOrionBmp = sprites.orionSuits[selectedSuitIndex];
        float orionScale = 1.0f;
        if (activeOrionBmp != null && activeOrionBmp.getHeight() > 0) {
            orionScale = 48f / activeOrionBmp.getHeight();
        }
        if (orionState == ORION_IN_CANNON && currentCannon != null && activeOrionBmp != null) {
            double rad = Math.toRadians(currentCannon.currentAngleDeg);
            float peekX = currentCannon.x + (float) Math.cos(rad) * 26f;
            float peekY = currentCannon.y + (float) Math.sin(rad) * 26f;
            if (turboMode) {
                paint.setColor(Color.argb(150, 255, 110, 15));
                canvas.drawCircle(peekX, peekY, 26f + (float) Math.sin(animClock * 20f) * 3f, paint);
            }
            drawBitmapCentered(canvas, activeOrionBmp, peekX, peekY, orionScale * 0.88f, orionScale * 0.88f, currentCannon.currentAngleDeg + 90f, 255);
        } else if (orionState != ORION_IN_CANNON && activeOrionBmp != null) {
            if (turboMode) {
                float flicker = (float) Math.sin(animClock * 24f) * 4f;
                paint.setColor(Color.argb(145, 255, 90, 10));
                canvas.drawCircle(orionX - orionVx * 0.35f, orionY - orionVy * 0.35f, 31f + flicker, paint);
                paint.setColor(Color.argb(195, 255, 210, 45));
                canvas.drawCircle(orionX, orionY, 25f - flicker * 0.5f, paint);
                paint.setAlpha(255);
            }
            drawBitmapCentered(canvas, activeOrionBmp, orionX, orionY, orionScale, orionScale, orionAngleDeg, 255);
        }

        // Render Cannons
        for (LevelData.CannonEntity c : level.cannons) {
            Bitmap cbmp;
            if (c.isCheckpoint) {
                cbmp = sprites.cannonCheckpoint;
            } else if (c.playerRotatable) {
                cbmp = sprites.cannonPurple;
            } else if (c.type == 14) {
                cbmp = sprites.cannonRed;
            } else if (c.autoLaunch) {
                cbmp = sprites.cannonGreen;
            } else {
                cbmp = sprites.cannonBlue;
            }

            if (orionState == ORION_IN_CANNON && currentCannon == c && !c.autoLaunch) {
                double rad = Math.toRadians(c.currentAngleDeg);
                float dx = (float) Math.cos(rad);
                float dy = (float) Math.sin(rad);
                paint.setColor(Color.argb(210, 255, 245, 120));
                for (int d = 1; d <= 6; d++) {
                    canvas.drawCircle(c.x + dx * (d * 34f), c.y + dy * (d * 34f), 6f, paint);
                }
            }

            float recoilScale = 1.0f + c.recoilAnim * 0.22f;
            float cannonScale = 0.75f * recoilScale;
            drawBitmapCentered(canvas, cbmp, c.x, c.y, cannonScale, cannonScale, c.currentAngleDeg, 255);

            if (c.isCheckpoint && c.checkpointTriggered) {
                float auraPulse = 1.0f + 0.05f * (float) Math.sin(animClock * 4.5f);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2.5f);
                paint.setColor(Color.argb(145, 255, 215, 60));
                canvas.drawCircle(c.x, c.y, 46f * auraPulse, paint);
                paint.setColor(Color.argb(60, 255, 235, 120));
                canvas.drawCircle(c.x, c.y, 55f * auraPulse, paint);
                paint.setStyle(Paint.Style.FILL);

                if (sprites.checkpointFlag != null) {
                    drawBitmapCentered(canvas, sprites.checkpointFlag, c.x, c.y - 54f, 0.85f, 0.85f, 0f, 255);
                }
            }
        }

        canvas.restore();

        renderHud(canvas, w, h);
    }

    private void renderHud(Canvas canvas, int w, int h) {
        RectF badge = new RectF(w * 0.025f, h * 0.035f, w * 0.30f, h * 0.145f);
        paint.setColor(Color.argb(165, 10, 25, 50));
        canvas.drawRoundRect(badge, 22f, 22f, paint);
        if (sprites.ringOrange != null) {
            float sc = (badge.height() * 0.72f) / sprites.ringOrange.getHeight();
            drawBitmapCentered(canvas, sprites.ringOrange, badge.left + badge.height() * 0.55f, badge.centerY(), sc, sc, 0f, 255);
        }
        int curPct = totalLevelRings > 0 ? (collectedRings * 100) / totalLevelRings : 100;
        textPaint.setColor(curPct >= 90 ? Color.rgb(110, 255, 145) : Color.rgb(255, 230, 90));
        textPaint.setTextSize(h * 0.038f);
        textPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(collectedRings + "/" + totalLevelRings + " (" + curPct + "%)", badge.left + badge.height() * 1.02f, badge.centerY() + h * 0.014f, textPaint);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(h * 0.035f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        int mins = ((int) levelTimerSec) / 60;
        int secs = ((int) levelTimerSec) % 60;
        canvas.drawText(String.format((langEs ? "NIVEL %d  •  %02d:%02d" : "LEVEL %d  •  %02d:%02d"), currentLevelNum, mins, secs), w * 0.47f, h * 0.09f, textPaint);

        if (orionState == ORION_IN_CANNON && currentCannon != null && !currentCannon.autoLaunch) {
            textPaint.setColor(Color.rgb(255, 245, 140));
            textPaint.setTextSize(h * 0.032f);
            String msg;
            if (currentCannon.playerRotatable) {
                msg = langEs ? "ARRASTRA PARA APUNTAR EL CAÑÓN MORADO • TOCA PARA DISPARAR"
                             : "DRAG TO AIM PURPLE CANNON • TAP TO LAUNCH ORION";
            } else {
                msg = langEs ? "TOCA LA PANTALLA PARA DISPARAR A ORION"
                             : "TAP SCREEN TO LAUNCH ORION";
            }
            canvas.drawText(msg, w * 0.5f, h * 0.93f, textPaint);
        } else if (orionState == ORION_IN_SLINGSHOT) {
            textPaint.setColor(Color.rgb(255, 245, 140));
            textPaint.setTextSize(h * 0.032f);
            canvas.drawText(langEs
                    ? "HALA LA RESORTERA HACIA ABAJO Y SUELTA PARA LANZAR A ORION"
                    : "PULL DOWN THE SLINGSHOT AND RELEASE TO LAUNCH ORION", w * 0.5f, h * 0.93f, textPaint);
        }

        btnHudPause.set(w * 0.89f, h * 0.035f, w * 0.975f, h * 0.145f);
        drawStylizedButton(canvas, btnHudPause, Color.rgb(52, 152, 219), "II", h * 0.042f);

        btnHudSlowMo.set(w * 0.74f, h * 0.035f, w * 0.875f, h * 0.145f);
        drawStylizedButton(canvas, btnHudSlowMo,
                slowMoActive ? Color.rgb(241, 196, 15) : Color.rgb(44, 62, 80),
                langEs ? (slowMoActive ? "LENTO: SÍ" : "LENTO") : (slowMoActive ? "SLOW-MO ON" : "SLOW-MO"), h * 0.024f);
        Bitmap slowIcon = slowMoActive ? sprites.slowMoBtnActive : sprites.slowMoBtnInactive;
        if (slowIcon != null) {
            float sc = (btnHudSlowMo.height() * 0.68f) / slowIcon.getHeight();
            drawBitmapCentered(canvas, slowIcon, btnHudSlowMo.left + btnHudSlowMo.height() * 0.42f, btnHudSlowMo.centerY(), sc, sc, 0f, 255);
        }

        btnHudZoomReset.set(w * 0.62f, h * 0.035f, w * 0.725f, h * 0.145f);
        drawStylizedButton(canvas, btnHudZoomReset, Color.rgb(44, 62, 80), "ZOOM", h * 0.028f);
    }

    private void renderPauseOverlay(Canvas canvas, int w, int h) {
        canvas.drawColor(Color.argb(195, 14, 11, 9));

        RectF panel = new RectF(w * 0.24f, h * 0.10f, w * 0.76f, h * 0.90f);
        paint.setShader(new LinearGradient(0, panel.top, 0, panel.bottom,
                Color.rgb(34, 28, 24), Color.rgb(20, 16, 14), Shader.TileMode.CLAMP));
        canvas.drawRoundRect(panel, 28f, 28f, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4.5f);
        paint.setColor(Color.rgb(185, 142, 92));
        canvas.drawRoundRect(panel, 28f, 28f, paint);
        paint.setStyle(Paint.Style.FILL);

        textPaint.setColor(Color.rgb(245, 238, 225));
        textPaint.setTextSize(h * 0.052f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(langEs ? "JUEGO EN PAUSA" : "GAME PAUSED", w * 0.5f, h * 0.22f, textPaint);

        float bw = w * 0.36f;
        float bh = h * 0.11f;
        btnPauseResume.set(w * 0.5f - bw * 0.5f, h * 0.28f, w * 0.5f + bw * 0.5f, h * 0.28f + bh);
        drawStylizedButton(canvas, btnPauseResume, Color.rgb(30, 134, 112), langEs ? "▶ CONTINUAR" : "▶ RESUME", h * 0.034f);

        btnPauseRestart.set(w * 0.5f - bw * 0.5f, h * 0.43f, w * 0.5f + bw * 0.5f, h * 0.43f + bh);
        drawStylizedButton(canvas, btnPauseRestart, Color.rgb(148, 92, 42), langEs ? "↻ REINICIAR NIVEL" : "↻ RESTART LEVEL", h * 0.034f);

        btnPauseTurbo.set(w * 0.5f - bw * 0.5f, h * 0.58f, w * 0.5f + bw * 0.5f, h * 0.58f + bh);
        drawStylizedButton(canvas, btnPauseTurbo,
                turboMode ? Color.rgb(184, 96, 32) : Color.rgb(42, 48, 54),
                langEs ? (turboMode ? "MODO TURBO: SÍ 🔥" : "MODO TURBO: NO")
                       : (turboMode ? "TURBO MODE: ON 🔥" : "TURBO MODE: OFF"), h * 0.032f);

        btnPauseLevels.set(w * 0.5f - bw * 0.5f, h * 0.73f, w * 0.5f + bw * 0.5f, h * 0.73f + bh);
        drawStylizedButton(canvas, btnPauseLevels, Color.rgb(112, 48, 38), langEs ? "← SALIR A NIVELES" : "← EXIT TO LEVELS", h * 0.034f);
    }

    private void renderLevelCompleteOverlay(Canvas canvas, int w, int h) {
        canvas.drawColor(Color.argb(200, 14, 11, 9));

        // Clean structured dialog card in the Orion Cutscene Palette
        RectF panel = new RectF(w * 0.17f, h * 0.10f, w * 0.83f, h * 0.88f);
        paint.setShader(new LinearGradient(0, panel.top, 0, panel.bottom,
                Color.rgb(36, 28, 24), Color.rgb(20, 16, 14), Shader.TileMode.CLAMP));
        canvas.drawRoundRect(panel, 28f, 28f, paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4.5f);
        paint.setColor(Color.rgb(185, 142, 92));
        canvas.drawRoundRect(panel, 28f, 28f, paint);
        paint.setStyle(Paint.Style.FILL);

        // Title header
        textPaint.setColor(Color.rgb(76, 232, 245));
        textPaint.setTextSize(h * 0.050f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(langEs
                ? ("¡NIVEL " + currentLevelNum + " COMPLETADO!")
                : ("LEVEL " + currentLevelNum + " COMPLETED!"), w * 0.5f, h * 0.22f, textPaint);

        // Left column: Upright Giant Finish Ring (NO rotation on its axis, gentle scale pulse only)
        if (sprites.giantFinishRing != null) {
            float pulse = 1.0f + 0.04f * (float) Math.sin(animClock * 4f);
            float sc = ((h * 0.26f) / sprites.giantFinishRing.getHeight()) * pulse;
            drawBitmapCentered(canvas, sprites.giantFinishRing, w * 0.31f, h * 0.45f, sc, sc, 0f, 255);
        }

        // Right column: Well-organized stats & 90% progression feedback
        int runPct = totalLevelRings > 0 ? (collectedRings * 100) / totalLevelRings : 100;
        int bestPct = getLevelPercent(currentLevelNum);
        int reqRings = getRequiredRings90(currentLevelNum);
        boolean nextUnlocked = (currentLevelNum < 36) && isLevelUnlocked(currentLevelNum + 1);

        textPaint.setTextAlign(Paint.Align.LEFT);
        float statX = w * 0.43f;

        textPaint.setColor(Color.rgb(245, 238, 225));
        textPaint.setTextSize(h * 0.040f);
        canvas.drawText((langEs ? "Aros: " : "Rings: ") + collectedRings + " / " + totalLevelRings + " (" + runPct + "%)",
                statX, h * 0.38f, textPaint);

        int mins = ((int) levelTimerSec) / 60;
        int secs = ((int) levelTimerSec) % 60;
        textPaint.setColor(Color.rgb(215, 195, 168));
        textPaint.setTextSize(h * 0.036f);
        canvas.drawText(String.format((langEs ? "Tiempo: %02d:%02d" : "Time: %02d:%02d"), mins, secs),
                statX, h * 0.47f, textPaint);

        textPaint.setTextSize(h * 0.028f);
        if (bestPct >= 90) {
            textPaint.setColor(Color.rgb(76, 232, 245));
            canvas.drawText(langEs
                    ? "✓ Meta del 90% alcanzada (Siguiente nivel desbloqueado)"
                    : "✓ 90% goal reached (Next level unlocked)", statX, h * 0.56f, textPaint);
        } else {
            textPaint.setColor(Color.rgb(232, 185, 105));
            canvas.drawText(langEs
                    ? ("⚠ Requiere 90% (" + reqRings + " aros) para desbloquear el Nivel " + (currentLevelNum + 1))
                    : ("⚠ Requires 90% (" + reqRings + " rings) to unlock Level " + (currentLevelNum + 1)),
                    statX, h * 0.56f, textPaint);
        }

        // Bottom row: 3 compact, well-spaced action buttons with clear labels
        float bw = w * 0.185f;
        float bh = h * 0.105f;
        float y0 = h * 0.70f;

        btnWinLevels.set(w * 0.21f, y0, w * 0.21f + bw, y0 + bh);
        drawStylizedButton(canvas, btnWinLevels, Color.rgb(64, 52, 44),
                langEs ? "← NIVELES" : "← LEVELS", h * 0.030f);

        btnWinReplay.set(w * 0.5f - bw * 0.5f, y0, w * 0.5f + bw * 0.5f, y0 + bh);
        drawStylizedButton(canvas, btnWinReplay, Color.rgb(148, 92, 42),
                langEs ? "↻ REINICIAR" : "↻ RESTART", h * 0.030f);

        btnWinNext.set(w * 0.79f - bw, y0, w * 0.79f, y0 + bh);
        drawStylizedButton(canvas, btnWinNext,
                nextUnlocked ? Color.rgb(30, 134, 112) : Color.rgb(55, 50, 46),
                nextUnlocked
                        ? (langEs ? "SIGUIENTE →" : "NEXT →")
                        : (langEs ? "🔒 REQ 90%" : "🔒 90% REQ"),
                h * 0.030f);
    }

    private void drawStylizedButton(Canvas canvas, RectF r, int bgColor, String label, float textSize) {
        // Subtle drop shadow
        rect.set(r.left + 2f, r.top + 4f, r.right + 2f, r.bottom + 4f);
        paint.setColor(Color.argb(145, 8, 6, 5));
        canvas.drawRoundRect(rect, 18f, 18f, paint);

        // Rich vertical metallic gradient derived from bgColor
        int rTop = Math.min(255, Color.red(bgColor) + 18);
        int gTop = Math.min(255, Color.green(bgColor) + 16);
        int bTop = Math.min(255, Color.blue(bgColor) + 14);
        int rBot = Math.max(0, Color.red(bgColor) - 22);
        int gBot = Math.max(0, Color.green(bgColor) - 20);
        int bBot = Math.max(0, Color.blue(bgColor) - 18);

        paint.setShader(new LinearGradient(0, r.top, 0, r.bottom,
                Color.rgb(rTop, gTop, bTop), Color.rgb(rBot, gBot, bBot), Shader.TileMode.CLAMP));
        canvas.drawRoundRect(r, 18f, 18f, paint);
        paint.setShader(null);

        // Weathered Brass / Bioluminescent Cyan border depending on button tone
        boolean isTealAction = (Color.green(bgColor) > Color.red(bgColor) + 25);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3.5f);
        paint.setColor(isTealAction ? Color.rgb(76, 232, 245) : Color.rgb(185, 142, 92));
        canvas.drawRoundRect(r, 18f, 18f, paint);
        paint.setStyle(Paint.Style.FILL);

        // Warm Ceramic Ivory label with subtle dark text shadow
        textPaint.setTextSize(textSize);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.argb(180, 10, 8, 6));
        canvas.drawText(label, r.centerX() + 1.5f, r.centerY() + textSize * 0.36f + 2f, textPaint);
        textPaint.setColor(Color.rgb(245, 238, 225));
        canvas.drawText(label, r.centerX(), r.centerY() + textSize * 0.36f, textPaint);
    }

    private void drawBitmapCentered(Canvas canvas, Bitmap bmp, float cx, float cy, float sx, float sy, float rotDeg, int alpha) {
        if (bmp == null) return;
        matrix.reset();
        matrix.postTranslate(-bmp.getWidth() * 0.5f, -bmp.getHeight() * 0.5f);
        matrix.postScale(sx, sy);
        if (rotDeg != 0f) {
            matrix.postRotate(rotDeg);
        }
        matrix.postTranslate(cx, cy);
        paint.setAlpha(alpha);
        canvas.drawBitmap(bmp, matrix, paint);
        paint.setAlpha(255);
    }
}
