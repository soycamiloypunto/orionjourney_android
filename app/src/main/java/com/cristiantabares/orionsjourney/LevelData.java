package com.cristiantabares.orionsjourney;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LevelData {
    public int levelNumber;
    public int worldNum; // 1=Blue Sky, 2=Soda Sunset, 3/4=Box Dimension
    public float groundY;
    public float ceilingY = -999999f;
    public float spawnX;
    public float spawnY;
    public float finishX;
    public float finishY;
    public boolean finishActive = true;

    public final List<BgElement> backgrounds = new ArrayList<>();
    public final List<CannonEntity> cannons = new ArrayList<>();
    public final List<ObstacleEntity> obstacles = new ArrayList<>();
    public final List<RingItem> rings = new ArrayList<>();

    public static class BgElement {
        public int type;
        public float x, y, scaleX, scaleY;
    }

    public static class CannonEntity {
        public int type; // 1..22
        public float baseX, baseY;
        public float x, y;
        public float baseAngleDeg;
        public float currentAngleDeg;
        public float scaleX, scaleY;

        public boolean autoLaunch;
        public boolean playerRotatable;
        public boolean isCheckpoint;
        public boolean checkpointTriggered;

        public boolean hasRotator;
        public float rotStepCsvDeg;
        public int numSteps;
        public boolean pingPong;
        public boolean pauseBetweenSteps;
        public int currentStepIndex = 0;
        public int stepDirection = 1;
        public float targetAngleDeg;
        public float pauseTimer = 0f;

        public boolean hasMover;
        public float moveOffsetX;
        public float moveOffsetY;
        public float moveSpeed;
        public boolean alwaysActiveMover;
        public boolean moverActive;
        public float moverProgress = 0f;
        public int moverDir = 1;

        public float recoilAnim = 0f;
    }

    public static class ObstacleEntity {
        public int type; // 1..39
        public float baseX, baseY;
        public float x, y;
        public float angleDeg;
        public float scaleX, scaleY;
        public boolean active = true;
        public float hitAnim = 0f;
        public float cloudFadeTimer = 0f;

        public boolean hasMover;
        public float moveOffsetX;
        public float moveOffsetY;
        public float moveSpeed;
        public float moverProgress = 0f;
        public int moverDir = 1;

        public boolean isBoss;
        public int bossHp = 8;
        public int bossMaxHp = 8;
        public float bossTargetX;
        public float bossTargetY;
        public float bossMoveTimer = 0f;
        public float bossHitCooldown = 0f;
        public float bossSwingPhase = 0f;
    }

    public static class RingItem {
        public int type; // 1=normal, 2=silver, 3=gold, 4=fire
        public float x, y;
        public float scaleX, scaleY;
        public boolean collected = false;
        public float collectAnim = 0f;
    }

    public static LevelData load(Context context, int levelNum) {
        LevelData data = new LevelData();
        data.levelNumber = levelNum;
        data.worldNum = ((levelNum - 1) / 12) + 1;
        data.groundY = 850f;
        data.spawnX = 70f;
        data.spawnY = 320f;
        data.finishX = 100f;
        data.finishY = -810f;
        float minY = 999999f;

        try (InputStream is = context.getAssets().open("levels/Level" + levelNum + ".txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            String headerLine = reader.readLine();
            if (headerLine != null) {
                String[] h = headerLine.trim().split(",");
                if (h.length >= 6) {
                    data.worldNum = parseInt(h[0], 1);
                    data.groundY = parseFloat(h[1], 850f);
                    data.spawnX = parseFloat(h[2], 70f);
                    data.spawnY = parseFloat(h[3], 320f);
                    data.finishX = parseFloat(h[4], 100f);
                    data.finishY = parseFloat(h[5], -810f);
                    minY = Math.min(minY, Math.min(data.spawnY, data.finishY));
                }
            }

            int section = 0; // 1=Background, 2=Cannons, 3=Obstacles, 4=Rings
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.equalsIgnoreCase("[Background]")) {
                    section = 1;
                    continue;
                } else if (line.equalsIgnoreCase("[Cannons]")) {
                    section = 2;
                    continue;
                } else if (line.equalsIgnoreCase("[Obstacles]")) {
                    section = 3;
                    continue;
                } else if (line.equalsIgnoreCase("[PuffleOs]")) {
                    section = 4;
                    continue;
                }

                String[] p = line.split(",");
                if (section == 1 && p.length >= 5) {
                    BgElement bg = new BgElement();
                    bg.type = parseInt(p[0], 1);
                    bg.x = parseFloat(p[1], 0f);
                    bg.y = parseFloat(p[2], 0f);
                    bg.scaleX = parseFloat(p[3], 1f);
                    bg.scaleY = parseFloat(p[4], 1f);
                    data.backgrounds.add(bg);
                } else if (section == 2 && p.length >= 6) {
                    CannonEntity c = createCannon(
                            parseInt(p[0], 1),
                            parseFloat(p[1], 0f),
                            parseFloat(p[2], 0f),
                            parseFloat(p[3], 0f),
                            parseFloat(p[4], 1f),
                            parseFloat(p[5], 1f)
                    );
                    minY = Math.min(minY, c.baseY);
                    data.cannons.add(c);
                } else if (section == 3 && p.length >= 6) {
                    ObstacleEntity o = createObstacle(
                            parseInt(p[0], 1),
                            parseFloat(p[1], 0f),
                            parseFloat(p[2], 0f),
                            parseFloat(p[3], 0f),
                            parseFloat(p[4], 1f),
                            parseFloat(p[5], 1f)
                    );
                    minY = Math.min(minY, o.baseY);
                    if (o.isBoss) {
                        data.finishActive = false;
                    }
                    data.obstacles.add(o);
                } else if (section == 4 && p.length >= 5) {
                    RingItem item = new RingItem();
                    item.type = parseInt(p[0], 1);
                    item.x = parseFloat(p[1], 0f);
                    item.y = parseFloat(p[2], 0f);
                    item.scaleX = parseFloat(p[3], 1f);
                    item.scaleY = parseFloat(p[4], 1f);
                    minY = Math.min(minY, item.y);
                    data.rings.add(item);
                }
            }
        } catch (Exception ignored) {
        }

        if (data.worldNum >= 3) {
            // Margin widened from 650 to 1100 so the balloon bounce (1.4x) never hits the ceiling
            data.ceilingY = minY - 1100f;
        } else {
            data.ceilingY = minY - 2500f;
        }
        return data;
    }

    private static CannonEntity createCannon(int type, float x, float y, float angleDeg, float sx, float sy) {
        CannonEntity c = new CannonEntity();
        c.type = type;
        c.baseX = x;
        c.baseY = y;
        c.x = x;
        c.y = y;
        c.baseAngleDeg = angleDeg;
        c.currentAngleDeg = angleDeg;
        c.targetAngleDeg = angleDeg;
        c.scaleX = sx;
        c.scaleY = sy;

        switch (type) {
            case 1:
                c.autoLaunch = true;
                break;
            case 2:
            case 5:
                c.autoLaunch = false;
                c.hasRotator = true;
                c.rotStepCsvDeg = -90f;
                c.numSteps = 1;
                c.pingPong = true;
                c.pauseBetweenSteps = true;
                break;
            case 11:
                // Manual fixed-angle cannon (waits for player tap without rotating)
                c.autoLaunch = false;
                c.hasRotator = false;
                c.hasMover = false;
                break;
            case 3:
            case 4:
                c.autoLaunch = false;
                c.hasRotator = true;
                c.rotStepCsvDeg = +90f;
                c.numSteps = 1;
                c.pingPong = true;
                c.pauseBetweenSteps = true;
                break;
            case 6:
                c.autoLaunch = false;
                c.hasRotator = true;
                c.rotStepCsvDeg = +90f;
                c.numSteps = 4;
                c.pingPong = false;
                c.pauseBetweenSteps = true;
                break;
            case 7:
                c.autoLaunch = false;
                c.hasRotator = true;
                c.rotStepCsvDeg = -90f;
                c.numSteps = 2;
                c.pingPong = true;
                c.pauseBetweenSteps = true;
                break;
            case 8:
            case 9:
            case 10:
                c.autoLaunch = false;
                c.hasRotator = true;
                c.rotStepCsvDeg = +90f;
                c.numSteps = 2;
                c.pingPong = true;
                c.pauseBetweenSteps = true;
                break;
            case 12:
                c.autoLaunch = false;
                c.hasRotator = false;
                c.hasMover = true;
                c.moveOffsetX = 240f;
                c.moveOffsetY = 0f;
                c.moveSpeed = 1.6f;
                c.alwaysActiveMover = false;
                break;
            case 13:
                c.autoLaunch = false;
                c.hasRotator = false;
                c.hasMover = true;
                c.moveOffsetX = 0f;
                c.moveOffsetY = 240f;
                c.moveSpeed = 1.6f;
                c.alwaysActiveMover = false;
                break;
            case 14:
                c.autoLaunch = false;
                c.hasRotator = true;
                c.rotStepCsvDeg = +72f;
                c.numSteps = 5;
                c.pingPong = false;
                c.pauseBetweenSteps = false;
                break;
            case 15:
                c.autoLaunch = false;
                c.playerRotatable = true;
                break;
            case 16:
                c.autoLaunch = true;
                c.isCheckpoint = true;
                break;
            case 17:
                c.autoLaunch = false;
                c.hasRotator = false;
                c.hasMover = true;
                c.moveOffsetX = -240f;
                c.moveOffsetY = 0f;
                c.moveSpeed = 1.6f;
                c.alwaysActiveMover = false;
                break;
            case 18:
                c.autoLaunch = false;
                c.hasRotator = false;
                c.hasMover = true;
                c.moveOffsetX = 0f;
                c.moveOffsetY = -240f;
                c.moveSpeed = 1.6f;
                c.alwaysActiveMover = false;
                break;
            case 19:
                c.autoLaunch = false;
                c.hasRotator = false;
                c.hasMover = true;
                c.moveOffsetX = -240f;
                c.moveOffsetY = 0f;
                c.moveSpeed = 1.6f;
                c.alwaysActiveMover = true;
                c.moverActive = true;
                break;
            case 20:
                c.autoLaunch = false;
                c.hasRotator = false;
                c.hasMover = true;
                c.moveOffsetX = 0f;
                c.moveOffsetY = -240f;
                c.moveSpeed = 1.6f;
                c.alwaysActiveMover = true;
                c.moverActive = true;
                break;
            case 21:
                c.autoLaunch = false;
                c.hasRotator = false;
                c.hasMover = true;
                c.moveOffsetX = 240f;
                c.moveOffsetY = 0f;
                c.moveSpeed = 1.6f;
                c.alwaysActiveMover = true;
                c.moverActive = true;
                break;
            case 22:
                c.autoLaunch = false;
                c.hasRotator = false;
                c.hasMover = true;
                c.moveOffsetX = 0f;
                c.moveOffsetY = 240f;
                c.moveSpeed = 1.6f;
                c.alwaysActiveMover = true;
                c.moverActive = true;
                break;
            default:
                c.autoLaunch = true;
                break;
        }
        return c;
    }

    private static ObstacleEntity createObstacle(int type, float x, float y, float angleDeg, float sx, float sy) {
        ObstacleEntity o = new ObstacleEntity();
        o.type = type;
        o.baseX = x;
        o.baseY = y;
        o.x = x;
        o.y = y;
        o.angleDeg = angleDeg;
        o.scaleX = sx;
        o.scaleY = sy;

        switch (type) {
            case 4:
            case 20:
                o.hasMover = true;
                o.moveOffsetX = 0f;
                o.moveOffsetY = 240f;
                o.moveSpeed = 1.2f;
                break;
            case 5:
            case 21:
                o.hasMover = true;
                o.moveOffsetX = 240f;
                o.moveOffsetY = 0f;
                o.moveSpeed = 1.2f;
                break;
            case 6:
            case 22:
                o.hasMover = true;
                o.moveOffsetX = 0f;
                o.moveOffsetY = -240f;
                o.moveSpeed = 1.2f;
                break;
            case 13:
                o.hasMover = true;
                o.moveOffsetX = 0f;
                o.moveOffsetY = 300f;
                o.moveSpeed = 1.1f;
                break;
            case 14:
                o.hasMover = true;
                o.moveOffsetX = 0f;
                o.moveOffsetY = -300f;
                o.moveSpeed = 1.1f;
                break;
            case 15:
                o.hasMover = true;
                o.moveOffsetX = -300f;
                o.moveOffsetY = 0f;
                o.moveSpeed = 1.1f;
                break;
            case 16:
                o.hasMover = true;
                o.moveOffsetX = 300f;
                o.moveOffsetY = 0f;
                o.moveSpeed = 1.1f;
                break;
            case 25:
            case 27:
                o.isBoss = true;
                o.bossHp = 8;
                o.bossMaxHp = 8;
                o.bossTargetX = x;
                o.bossTargetY = y;
                break;
            default:
                break;
        }
        return o;
    }

    public static int getLevelRingCount(Context context, int levelNum) {
        int count = 0;
        try (InputStream is = context.getAssets().open("levels/Level" + levelNum + ".txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            boolean inRings = false;
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.equalsIgnoreCase("[PuffleOs]")) {
                    inRings = true;
                    continue;
                } else if (line.startsWith("[")) {
                    inRings = false;
                    continue;
                }
                if (inRings && line.split(",").length >= 5) {
                    count++;
                }
            }
        } catch (Exception ignored) {
        }
        return count;
    }

    private static int parseInt(String s, int def) {
        try {
            return (int) Float.parseFloat(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static float parseFloat(String s, float def) {
        try {
            return Float.parseFloat(s.trim());
        } catch (Exception e) {
            return def;
        }
    }
}
