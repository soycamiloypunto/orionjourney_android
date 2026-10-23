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
    private boolean slowMoActive = false;
    private float tiltInputX = 0f;

    // Progress persistence
    private final int[] bestRings = new int[37];
    private final boolean[] levelCleared = new boolean[37];

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
    private final RectF btnBack = new RectF();
    private final RectF btnTurboToggle = new RectF();
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

    private static final String[] SUIT_NAMES = {
        "Solar", "Cosmic", "Frost", "Amber", "Terra",
        "Azure", "Crimson", "Starlight", "Onyx", "Emerald"
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
        audio.playMusic("menu.mp3", true);
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
        for (int i = 1; i <= 36; i++) {
            bestRings[i] = prefs.getInt("best_ring_" + i, 0);
            levelCleared[i] = prefs.getBoolean("cleared_" + i, false);
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
                .apply();
    }

    public void setTiltInput(float tiltX) {
        this.tiltInputX = Math.max(-1f, Math.min(1f, tiltX));
    }

    public boolean handleBackPress() {
        if (gameState == STATE_PLAYING) {
            gameState = STATE_PAUSED;
            audio.playSfx("Menu_Click24.wav");
            return true;
        } else if (gameState == STATE_PAUSED) {
            gameState = STATE_PLAYING;
            audio.playSfx("Menu_Click24.wav");
            return true;
        } else if (gameState == STATE_LEVEL_COMPLETE) {
            gameState = STATE_LEVEL_SELECT;
            audio.playMusic("menu.mp3", true);
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
                    if (along >= -30f && along <= 380f && perp <= 80f) {
                        float strength = 2.0f * (1.0f - (along / 480f));
                        orionVx += blowX * strength * subDtTicks;
                        orionVy += blowY * strength * subDtTicks;
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
                for (int i = 0; i < 3; i++) {
                    if (worldCards[i].contains(tx, ty)) {
                        audio.playSfx("Menu_Click24.wav");
                        selectedWorld = i + 1;
                        gameState = STATE_LEVEL_SELECT;
                        return true;
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
                        startLevel(lvl);
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
                } else if (btnPauseLevels.contains(tx, ty)) {
                    audio.playSfx("Menu_Click24.wav");
                    audio.playMusic("menu.mp3", true);
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
                    selectedWorld = ((next - 1) / 12) + 1;
                    startLevel(next);
                } else if (btnWinReplay.contains(tx, ty)) {
                    audio.playSfx("Menu_Click24.wav");
                    startLevel(currentLevelNum);
                } else if (btnWinLevels.contains(tx, ty)) {
                    audio.playSfx("Menu_Click24.wav");
                    audio.playMusic("menu.mp3", true);
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
        } else {
            renderGameplay(canvas, w, h);
            if (gameState == STATE_PAUSED) {
                renderPauseOverlay(canvas, w, h);
            } else if (gameState == STATE_LEVEL_COMPLETE) {
                renderLevelCompleteOverlay(canvas, w, h);
            }
        }
    }

    private void renderMainMenu(Canvas canvas, int w, int h) {
        // Clean sky gradient background
        paint.setShader(new LinearGradient(0, 0, 0, h,
                Color.rgb(4, 105, 185), Color.rgb(90, 185, 250), Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        // Soft drifting clouds in the sky (only pure cloud sprites 0..2, no trees/cannons/logos)
        for (int i = 0; i < 6; i++) {
            Bitmap cloud = sprites.bgClouds[i % 3];
            if (cloud != null) {
                float cx = ((i * 360f + animClock * (22f + i * 6f)) % (w + 420f)) - 210f;
                float cy = h * (0.14f + (i % 3) * 0.26f);
                drawBitmapCentered(canvas, cloud, cx, cy, 0.72f, 0.72f, 0f, 185);
            }
        }

        // Stylized title: ORION'S JOURNEY
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(h * 0.115f);
        // Drop shadow
        textPaint.setColor(Color.argb(160, 0, 30, 70));
        canvas.drawText("ORION'S JOURNEY", w * 0.5f + 4f, h * 0.27f + 5f, textPaint);
        // Main gold/white title
        textPaint.setColor(Color.WHITE);
        canvas.drawText("ORION'S JOURNEY", w * 0.5f, h * 0.27f, textPaint);

        textPaint.setTextSize(h * 0.036f);
        textPaint.setColor(Color.rgb(255, 235, 130));
        canvas.drawText("THE JOURNEY OF ORION", w * 0.5f, h * 0.34f, textPaint);

        // Play button
        float btnW = w * 0.28f;
        float btnH = h * 0.135f;
        btnPlay.set(w * 0.5f - btnW * 0.5f, h * 0.46f, w * 0.5f + btnW * 0.5f, h * 0.46f + btnH);
        if (sprites.playBtn != null) {
            canvas.drawBitmap(sprites.playBtn, null, btnPlay, paint);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(h * 0.055f);
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("PLAY", btnPlay.centerX(), btnPlay.centerY() + h * 0.02f, textPaint);
        } else {
            drawStylizedButton(canvas, btnPlay, Color.rgb(46, 204, 113), "PLAY", h * 0.055f);
        }

        // Bottom row buttons: Suit Change, Control Mode, Sound
        float subW = w * 0.27f;
        float subH = h * 0.12f;
        float bottomY = h * 0.69f;

        btnSuitSelect.set(w * 0.07f, bottomY, w * 0.07f + subW, bottomY + subH);
        drawStylizedButton(canvas, btnSuitSelect, Color.rgb(230, 126, 34),
                "SUIT: " + SUIT_NAMES[selectedSuitIndex], h * 0.033f);
        Bitmap suitBmp = sprites.orionSuits[selectedSuitIndex];
        if (suitBmp != null) {
            float iconScale = (subH * 0.72f) / suitBmp.getHeight();
            drawBitmapCentered(canvas, suitBmp, btnSuitSelect.left + subH * 0.45f, btnSuitSelect.centerY(), iconScale, iconScale, 0f, 255);
        }

        btnControlMode.set(w * 0.5f - subW * 0.5f, bottomY, w * 0.5f + subW * 0.5f, bottomY + subH);
        drawStylizedButton(canvas, btnControlMode, Color.rgb(52, 152, 219),
                "CONTROL: " + (tiltControls ? "TILT" : "TOUCH"), h * 0.033f);

        btnSound.set(w * 0.93f - subW, bottomY, w * 0.93f, bottomY + subH);
        drawStylizedButton(canvas, btnSound, audio.isMuted() ? Color.rgb(192, 57, 43) : Color.rgb(155, 89, 182),
                audio.isMuted() ? "SOUND: OFF" : "SOUND: ON", h * 0.033f);

        // Director & studio credit footer
        textPaint.setColor(Color.argb(235, 255, 255, 255));
        textPaint.setTextSize(h * 0.029f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Directed by Cristian Camilo Tabares  •  Cristian Camilo Tabares Development", w * 0.5f, h * 0.93f, textPaint);
    }

    private void renderWorldSelect(Canvas canvas, int w, int h) {
        canvas.drawColor(Color.rgb(14, 75, 135));

        btnBack.set(w * 0.03f, h * 0.05f, w * 0.16f, h * 0.15f);
        drawStylizedButton(canvas, btnBack, Color.rgb(231, 76, 60), "← MENU", h * 0.035f);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(h * 0.062f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("SELECT A WORLD", w * 0.5f, h * 0.13f, textPaint);

        String[] titles = {"1. BLUE SKY", "2. SUNSET HORIZON", "3. BOX DIMENSION"};
        String[] subtitles = {"Levels 1 – 12 • Boss Stage I", "Levels 13 – 24 • Boss Stage II", "Levels 25 – 36 • Bonus Stages"};
        int[] colors = {
            Color.rgb(4, 115, 192),
            Color.rgb(247, 138, 73),
            Color.rgb(111, 23, 157)
        };

        float cardW = w * 0.28f;
        float cardH = h * 0.66f;
        float gap = w * 0.035f;
        float startX = (w - (3 * cardW + 2 * gap)) * 0.5f;

        for (int i = 0; i < 3; i++) {
            float x0 = startX + i * (cardW + gap);
            float y0 = h * 0.22f;
            worldCards[i].set(x0, y0, x0 + cardW, y0 + cardH);

            paint.setColor(colors[i]);
            canvas.drawRoundRect(worldCards[i], 28f, 28f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(6f);
            paint.setColor(Color.WHITE);
            canvas.drawRoundRect(worldCards[i], 28f, 28f, paint);
            paint.setStyle(Paint.Style.FILL);

            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(h * 0.040f);
            canvas.drawText(titles[i], worldCards[i].centerX(), y0 + cardH * 0.16f, textPaint);

            textPaint.setTextSize(h * 0.025f);
            canvas.drawText(subtitles[i], worldCards[i].centerX(), y0 + cardH * 0.26f, textPaint);

            Bitmap scene = sprites.worldScenes[i];
            if (scene != null) {
                RectF imgRect = new RectF(x0 + 14f, y0 + cardH * 0.30f, x0 + cardW - 14f, y0 + cardH * 0.72f);
                canvas.drawBitmap(scene, null, imgRect, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3f);
                paint.setColor(Color.WHITE);
                canvas.drawRect(imgRect, paint);
                paint.setStyle(Paint.Style.FILL);
            }

            int worldRings = 0;
            int worldCleared = 0;
            for (int l = i * 12 + 1; l <= (i + 1) * 12; l++) {
                worldRings += bestRings[l];
                if (levelCleared[l]) worldCleared++;
            }
            textPaint.setColor(Color.rgb(255, 235, 110));
            textPaint.setTextSize(h * 0.032f);
            canvas.drawText("Cleared: " + worldCleared + " / 12", worldCards[i].centerX(), y0 + cardH * 0.82f, textPaint);
            canvas.drawText("Rings: " + worldRings, worldCards[i].centerX(), y0 + cardH * 0.92f, textPaint);
        }
    }

    private void renderLevelSelect(Canvas canvas, int w, int h) {
        int[] worldBg = {
            Color.rgb(4, 115, 192),
            Color.rgb(247, 138, 73),
            Color.rgb(111, 23, 157)
        };
        canvas.drawColor(worldBg[(selectedWorld - 1) % 3]);

        btnBack.set(w * 0.03f, h * 0.05f, w * 0.16f, h * 0.15f);
        drawStylizedButton(canvas, btnBack, Color.rgb(231, 76, 60), "← WORLDS", h * 0.033f);

        btnTurboToggle.set(w * 0.74f, h * 0.05f, w * 0.97f, h * 0.15f);
        drawStylizedButton(canvas, btnTurboToggle,
                turboMode ? Color.rgb(243, 156, 18) : Color.rgb(52, 73, 94),
                turboMode ? "TURBO MODE: ON 🔥" : "TURBO MODE: OFF", h * 0.031f);

        String[] worldNames = {"WORLD 1: BLUE SKY", "WORLD 2: SUNSET HORIZON", "WORLD 3: BOX DIMENSION"};
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(h * 0.052f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(worldNames[(selectedWorld - 1) % 3], w * 0.45f, h * 0.12f, textPaint);

        int cols = 4;
        int rows = 3;
        float btnH = h * 0.225f;
        float btnW = btnH * 1.05f;
        float gapX = w * 0.055f;
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
            Bitmap lvlBtnBmp = sprites.levelSelectBtns[(selectedWorld - 1) % 3];
            if (lvlBtnBmp != null) {
                float sqSize = btnH * 0.86f;
                RectF sqRect = new RectF(
                        levelButtons[i].centerX() - sqSize * 0.5f,
                        y0,
                        levelButtons[i].centerX() + sqSize * 0.5f,
                        y0 + sqSize);
                canvas.drawBitmap(lvlBtnBmp, null, sqRect, paint);

                String lbl = isBossLvl ? (lvl + " ★") : String.valueOf(lvl);
                textPaint.setTextSize(h * 0.050f);
                textPaint.setTextAlign(Paint.Align.CENTER);
                textPaint.setColor(Color.argb(170, 0, 20, 50));
                canvas.drawText(lbl, sqRect.centerX() + 3f, sqRect.centerY() + h * 0.018f + 3f, textPaint);
                textPaint.setColor(Color.WHITE);
                canvas.drawText(lbl, sqRect.centerX(), sqRect.centerY() + h * 0.018f, textPaint);
            } else {
                int cardColor = levelCleared[lvl] ? Color.rgb(39, 174, 96)
                        : isBossLvl ? Color.rgb(192, 57, 43) : Color.rgb(41, 128, 185);
                drawStylizedButton(canvas, levelButtons[i], cardColor,
                        isBossLvl ? ("LEVEL " + lvl + " (BOSS)") : ("LEVEL " + lvl), h * 0.036f);
            }

            // Clean bottom score pill below each square button
            RectF pillRect = new RectF(
                    levelButtons[i].centerX() - btnW * 0.44f,
                    y0 + btnH * 0.72f,
                    levelButtons[i].centerX() + btnW * 0.44f,
                    y0 + btnH * 0.98f);
            paint.setColor(Color.argb(165, 10, 25, 45));
            canvas.drawRoundRect(pillRect, 14f, 14f, paint);

            Bitmap ring = levelCleared[lvl] ? sprites.ringGold : sprites.ringOrange;
            if (ring != null) {
                float sc = (pillRect.height() * 0.78f) / ring.getHeight();
                drawBitmapCentered(canvas, ring, pillRect.left + pillRect.width() * 0.24f, pillRect.centerY(), sc, sc, 0f, 255);
            }
            textPaint.setColor(levelCleared[lvl] ? Color.rgb(120, 255, 150) : Color.rgb(255, 240, 150));
            textPaint.setTextSize(h * 0.027f);
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(bestRings[lvl] + (levelCleared[lvl] ? " ✓" : ""), pillRect.left + pillRect.width() * 0.62f, pillRect.centerY() + h * 0.010f, textPaint);
        }
    }

    private void renderGameplay(Canvas canvas, int w, int h) {
        if (level == null) return;

        int bgColor;
        if (level.worldNum == 1) {
            bgColor = Color.rgb(4, 115, 192);
        } else if (level.worldNum == 2) {
            bgColor = Color.rgb(247, 138, 73);
        } else {
            bgColor = Color.rgb(111, 23, 157);
        }
        canvas.drawColor(bgColor);

        camZoom = (h / 680f) * userZoomMultiplier;

        canvas.save();
        canvas.translate(w * 0.5f, h * 0.5f);
        canvas.scale(camZoom, camZoom);
        canvas.translate(-camX, -camY);

        // Background Clouds
        for (LevelData.BgElement bg : level.backgrounds) {
            Bitmap bmp = sprites.bgClouds[Math.abs(bg.type - 1) % sprites.bgClouds.length];
            if (bmp != null) {
                drawBitmapCentered(canvas, bmp, bg.x, bg.y, bg.scaleX * 0.85f, bg.scaleY * 0.85f, 0f, 195);
            }
        }

        // Ground / Ceiling FallZone
        paint.setColor(level.worldNum == 2 ? Color.argb(190, 180, 60, 20)
                : level.worldNum >= 3 ? Color.argb(190, 55, 10, 90)
                : Color.argb(190, 0, 65, 130));
        canvas.drawRect(camX - 4000f, level.groundY, camX + 4000f, level.groundY + 1200f, paint);
        paint.setColor(Color.argb(210, 255, 255, 255));
        paint.setStrokeWidth(6f);
        canvas.drawLine(camX - 4000f, level.groundY, camX + 4000f, level.groundY, paint);

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

        // Giant Finish Ring
        if (level.finishActive && sprites.giantFinishRing != null) {
            float pulse = 1.0f + 0.08f * (float) Math.sin(animClock * 5f);
            drawBitmapCentered(canvas, sprites.giantFinishRing, level.finishX, level.finishY, pulse * 1.1f, pulse * 1.1f, animClock * 25f, 255);
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

        // Render Orion
        Bitmap activeOrionBmp = sprites.orionSuits[selectedSuitIndex];
        if (turboMode && sprites.orionFireFrames[0] != null) {
            int fIdx = ((int) (animClock * 16f)) % sprites.orionFireFrames.length;
            if (sprites.orionFireFrames[fIdx] != null) {
                activeOrionBmp = sprites.orionFireFrames[fIdx];
            }
        }
        float orionScale = 1.0f;
        if (activeOrionBmp != null && activeOrionBmp.getHeight() > 0) {
            float targetWorldSize = turboMode ? 72f : 48f;
            orionScale = targetWorldSize / activeOrionBmp.getHeight();
        }
        if (orionState == ORION_IN_CANNON && currentCannon != null && activeOrionBmp != null) {
            double rad = Math.toRadians(currentCannon.currentAngleDeg);
            float peekX = currentCannon.x + (float) Math.cos(rad) * 26f;
            float peekY = currentCannon.y + (float) Math.sin(rad) * 26f;
            drawBitmapCentered(canvas, activeOrionBmp, peekX, peekY, orionScale * 0.88f, orionScale * 0.88f, currentCannon.currentAngleDeg + 90f, 255);
        } else if (orionState != ORION_IN_CANNON && activeOrionBmp != null) {
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
            drawBitmapCentered(canvas, cbmp, c.x, c.y, 0.92f * recoilScale, 0.92f * recoilScale, c.currentAngleDeg, 255);

            if (c.isCheckpoint && c.checkpointTriggered && sprites.checkpointFlag != null) {
                drawBitmapCentered(canvas, sprites.checkpointFlag, c.x, c.y - 54f, 0.85f, 0.85f, 0f, 255);
            }
        }

        canvas.restore();

        renderHud(canvas, w, h);
    }

    private void renderHud(Canvas canvas, int w, int h) {
        RectF badge = new RectF(w * 0.025f, h * 0.035f, w * 0.28f, h * 0.145f);
        paint.setColor(Color.argb(165, 10, 25, 50));
        canvas.drawRoundRect(badge, 22f, 22f, paint);
        if (sprites.ringOrange != null) {
            float sc = (badge.height() * 0.72f) / sprites.ringOrange.getHeight();
            drawBitmapCentered(canvas, sprites.ringOrange, badge.left + badge.height() * 0.55f, badge.centerY(), sc, sc, 0f, 255);
        }
        textPaint.setColor(Color.rgb(255, 230, 90));
        textPaint.setTextSize(h * 0.045f);
        textPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(collectedRings + " / " + totalLevelRings, badge.left + badge.height() * 1.05f, badge.centerY() + h * 0.016f, textPaint);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(h * 0.036f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        int mins = ((int) levelTimerSec) / 60;
        int secs = ((int) levelTimerSec) % 60;
        canvas.drawText(String.format("LEVEL %d  •  %02d:%02d", currentLevelNum, mins, secs), w * 0.5f, h * 0.09f, textPaint);

        if (orionState == ORION_IN_CANNON && currentCannon != null && !currentCannon.autoLaunch) {
            textPaint.setColor(Color.rgb(255, 245, 140));
            textPaint.setTextSize(h * 0.034f);
            String msg = currentCannon.playerRotatable
                    ? "DRAG TO AIM PURPLE CANNON • TAP TO LAUNCH ORION"
                    : "TAP SCREEN TO LAUNCH ORION";
            canvas.drawText(msg, w * 0.5f, h * 0.93f, textPaint);
        } else if (orionState == ORION_IN_SLINGSHOT) {
            textPaint.setColor(Color.rgb(255, 245, 140));
            textPaint.setTextSize(h * 0.034f);
            canvas.drawText("PULL DOWN THE SLINGSHOT AND RELEASE TO LAUNCH ORION", w * 0.5f, h * 0.93f, textPaint);
        }

        btnHudPause.set(w * 0.89f, h * 0.035f, w * 0.975f, h * 0.145f);
        drawStylizedButton(canvas, btnHudPause, Color.rgb(52, 152, 219), "II", h * 0.042f);

        btnHudSlowMo.set(w * 0.74f, h * 0.035f, w * 0.875f, h * 0.145f);
        drawStylizedButton(canvas, btnHudSlowMo,
                slowMoActive ? Color.rgb(241, 196, 15) : Color.rgb(44, 62, 80),
                slowMoActive ? "SLOW-MO ON" : "SLOW-MO", h * 0.025f);
        Bitmap slowIcon = slowMoActive ? sprites.slowMoBtnActive : sprites.slowMoBtnInactive;
        if (slowIcon != null) {
            float sc = (btnHudSlowMo.height() * 0.68f) / slowIcon.getHeight();
            drawBitmapCentered(canvas, slowIcon, btnHudSlowMo.left + btnHudSlowMo.height() * 0.42f, btnHudSlowMo.centerY(), sc, sc, 0f, 255);
        }

        btnHudZoomReset.set(w * 0.62f, h * 0.035f, w * 0.725f, h * 0.145f);
        drawStylizedButton(canvas, btnHudZoomReset, Color.rgb(44, 62, 80), "ZOOM", h * 0.028f);
    }

    private void renderPauseOverlay(Canvas canvas, int w, int h) {
        canvas.drawColor(Color.argb(185, 0, 15, 35));

        RectF panel = new RectF(w * 0.25f, h * 0.15f, w * 0.75f, h * 0.85f);
        paint.setColor(Color.rgb(18, 85, 150));
        canvas.drawRoundRect(panel, 32f, 32f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f);
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(panel, 32f, 32f, paint);
        paint.setStyle(Paint.Style.FILL);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(h * 0.06f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("GAME PAUSED", w * 0.5f, h * 0.28f, textPaint);

        float bw = w * 0.34f;
        float bh = h * 0.12f;
        btnPauseResume.set(w * 0.5f - bw * 0.5f, h * 0.36f, w * 0.5f + bw * 0.5f, h * 0.36f + bh);
        drawStylizedButton(canvas, btnPauseResume, Color.rgb(46, 204, 113), "RESUME", h * 0.04f);

        btnPauseRestart.set(w * 0.5f - bw * 0.5f, h * 0.52f, w * 0.5f + bw * 0.5f, h * 0.52f + bh);
        drawStylizedButton(canvas, btnPauseRestart, Color.rgb(243, 156, 18), "RESTART LEVEL", h * 0.04f);

        btnPauseLevels.set(w * 0.5f - bw * 0.5f, h * 0.68f, w * 0.5f + bw * 0.5f, h * 0.68f + bh);
        drawStylizedButton(canvas, btnPauseLevels, Color.rgb(231, 76, 60), "EXIT TO LEVELS", h * 0.04f);
    }

    private void renderLevelCompleteOverlay(Canvas canvas, int w, int h) {
        canvas.drawColor(Color.argb(190, 0, 20, 45));

        RectF panel = new RectF(w * 0.18f, h * 0.08f, w * 0.82f, h * 0.92f);
        if (sprites.endLevelPopup != null) {
            canvas.drawBitmap(sprites.endLevelPopup, null, panel, paint);
        } else {
            paint.setColor(Color.rgb(20, 95, 165));
            canvas.drawRoundRect(panel, 34f, 34f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(7f);
            paint.setColor(Color.rgb(255, 215, 60));
            canvas.drawRoundRect(panel, 34f, 34f, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        textPaint.setColor(Color.rgb(255, 230, 80));
        textPaint.setTextSize(h * 0.056f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("LEVEL " + currentLevelNum + " COMPLETED!", w * 0.5f, h * 0.27f, textPaint);

        if (sprites.giantFinishRing != null) {
            float sc = (h * 0.24f) / sprites.giantFinishRing.getHeight();
            drawBitmapCentered(canvas, sprites.giantFinishRing, w * 0.36f, h * 0.48f, sc, sc, animClock * 20f, 255);
        }

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(h * 0.046f);
        canvas.drawText("Rings: " + collectedRings + " / " + totalLevelRings, w * 0.58f, h * 0.45f, textPaint);
        int mins = ((int) levelTimerSec) / 60;
        int secs = ((int) levelTimerSec) % 60;
        canvas.drawText(String.format("Time: %02d:%02d", mins, secs), w * 0.58f, h * 0.54f, textPaint);

        float bw = w * 0.17f;
        float bh = h * 0.13f;
        float y0 = h * 0.68f;

        btnWinLevels.set(w * 0.23f, y0, w * 0.23f + bw, y0 + bh);
        if (sprites.endLevelMenuBtn != null) {
            canvas.drawBitmap(sprites.endLevelMenuBtn, null, btnWinLevels, paint);
        } else {
            drawStylizedButton(canvas, btnWinLevels, Color.rgb(52, 152, 219), "LEVELS", h * 0.035f);
        }

        btnWinReplay.set(w * 0.5f - bw * 0.5f, y0, w * 0.5f + bw * 0.5f, y0 + bh);
        if (sprites.endLevelReplayBtn != null) {
            canvas.drawBitmap(sprites.endLevelReplayBtn, null, btnWinReplay, paint);
        } else {
            drawStylizedButton(canvas, btnWinReplay, Color.rgb(243, 156, 18), "REPLAY", h * 0.035f);
        }

        btnWinNext.set(w * 0.77f - bw, y0, w * 0.77f, y0 + bh);
        if (sprites.endLevelNextBtn != null) {
            canvas.drawBitmap(sprites.endLevelNextBtn, null, btnWinNext, paint);
        } else {
            drawStylizedButton(canvas, btnWinNext, Color.rgb(46, 204, 113), "NEXT →", h * 0.035f);
        }
    }

    private void drawStylizedButton(Canvas canvas, RectF r, int bgColor, String label, float textSize) {
        paint.setColor(bgColor);
        canvas.drawRoundRect(r, 22f, 22f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4.5f);
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(r, 22f, 22f, paint);
        paint.setStyle(Paint.Style.FILL);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(textSize);
        textPaint.setTextAlign(Paint.Align.CENTER);
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
