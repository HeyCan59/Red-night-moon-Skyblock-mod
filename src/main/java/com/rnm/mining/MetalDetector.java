package com.rnm.mining;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rnm.RnmText;
import com.rnm.gui.GuiConfig;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.GizmoStyle;

public final class MetalDetector {

    private static final Minecraft CLIENT = Minecraft.getInstance();

    // 偵測設定
    private static final int READ_DELAY_TICKS = 20;
    private static final int REQUIRED_STATIONARY_TICKS = 5;

    private static final double DISTANCE_TOLERANCE = 1.25;
    private static final double MINES_RADIUS_SQUARED = 10000.0;
    private static final double MOVEMENT_THRESHOLD_SQUARED = 0.0001;

    // 光束設定
    private static final double BEAM_HEIGHT = 256.0;
    private static final double OUTER_BEAM_RADIUS = 0.35;
    private static final double INNER_BEAM_RADIUS = 0.10;

    private static final int POSSIBLE_COLOR = 0xFF22DDFF;
    private static final int TREASURE_COLOR = 0xFFFFFF00;

    private static final Pattern TREASURE_PATTERN =
        Pattern.compile(
            "TREASURE:\\s*([\\d,]+(?:\\.\\d+)?)\\s*m",
            Pattern.CASE_INSENSITIVE
        );

    private static final Pattern KEEPER_PATTERN =
        Pattern.compile("Keeper of (Diamond|Lapis|Emerald|Gold)");

    private static final Map<String, Vec3i> KEEPER_OFFSETS = Map.of(
        "Diamond", new Vec3i(33, 0, 3),
        "Lapis", new Vec3i(-33, 0, -3),
        "Emerald", new Vec3i(-3, 0, 33),
        "Gold", new Vec3i(3, 0, -33)
    );

    // Mines of Divan 中已知的寶箱相對座標
    private static final Set<Vec3i> KNOWN_CHEST_OFFSETS = Set.of(
        new Vec3i(-38, -22, 26),
        new Vec3i(38, -22, -26),
        new Vec3i(-40, -22, 18),
        new Vec3i(-41, -20, 22),
        new Vec3i(-5, -21, 16),
        new Vec3i(40, -22, -30),
        new Vec3i(-42, -20, -28),
        new Vec3i(-43, -22, -40),
        new Vec3i(42, -19, -41),
        new Vec3i(43, -21, -16),
        new Vec3i(-1, -22, -20),
        new Vec3i(6, -21, 28),
        new Vec3i(7, -21, 11),
        new Vec3i(7, -21, 22),
        new Vec3i(-12, -21, -44),
        new Vec3i(12, -22, 31),
        new Vec3i(12, -22, -22),
        new Vec3i(12, -21, 7),
        new Vec3i(12, -21, -43),
        new Vec3i(-14, -21, 43),
        new Vec3i(-14, -21, 22),
        new Vec3i(-17, -21, 20),
        new Vec3i(-20, -22, 0),
        new Vec3i(1, -21, 20),
        new Vec3i(19, -22, 29),
        new Vec3i(20, -22, 0),
        new Vec3i(20, -21, -26),
        new Vec3i(-23, -22, 40),
        new Vec3i(22, -21, -14),
        new Vec3i(-24, -22, 12),
        new Vec3i(23, -22, 26),
        new Vec3i(23, -22, -39),
        new Vec3i(24, -22, 27),
        new Vec3i(25, -22, 17),
        new Vec3i(29, -21, -44),
        new Vec3i(-31, -21, -12),
        new Vec3i(-31, -21, -40),
        new Vec3i(30, -21, -25),
        new Vec3i(-32, -21, -40),
        new Vec3i(-36, -20, 42),
        new Vec3i(-37, -21, -14),
        new Vec3i(-37, -21, -22)
    );

    private static Vec3i minesCenter;

    // 所有已知位置，以及目前符合距離的候選位置
    private static final List<Vec3i> allCandidateBlocks = new ArrayList<>();
    private static final List<Vec3i> possibleBlocks = new ArrayList<>();

    private static Vec3 lastTickPlayerPos;
    private static Vec3 previousPlayerPos;

    private static Vec3i lockedTreasure;
    private static Vec3i lastReportedTreasure;

    private static double previousDistance = -1.0;
    private static double distanceBeforeTreasure = -1.0;

    private static int stationaryTicks;
    private static int readDelayTicks;
    private static int uniqueConfirmations;

    private static boolean playerStationary;
    private static boolean waitingForFreshDistance;
    private static boolean startedLooking;
    private static boolean wasInMinesOfDivan;
    private static boolean initialized;
    private static boolean distanceDebugShown;

    private MetalDetector() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;

        // 每 Tick 更新偵測狀態
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            // GUI 關閉模組時，停止偵測並清除暫存狀態
            if (!GuiConfig.MetalDetector) {
                lastTickPlayerPos = null;
                previousPlayerPos = null;

                stationaryTicks = 0;
                playerStationary = false;

                readDelayTicks = 0;
                waitingForFreshDistance = false;
                distanceBeforeTreasure = -1.0;

                wasInMinesOfDivan = false;
                return;
            }

            if (client.player == null || client.level == null) {
                lastTickPlayerPos = null;
                stationaryTicks = 0;
                playerStationary = false;
                wasInMinesOfDivan = false;
                return;
            }

            updateStationaryState(client);

            if (readDelayTicks > 0) {
                readDelayTicks--;

                if (readDelayTicks == 0) {
                    waitingForFreshDistance = true;
                }
            }

            boolean inMines = isInMinesOfDivan();

            // 進入 Mines of Divan 時重新開始偵測
            if (inMines && !wasInMinesOfDivan) {
                resetTreasureDetection();

                readDelayTicks = READ_DELAY_TICKS;
                waitingForFreshDistance = true;
                distanceBeforeTreasure = -1.0;

                client.player.sendSystemMessage(
                    Component.literal(
                        RnmText.PREFIX
                            + "已進入 Mines of Divan，開始偵測。"
                    )
                );
            }

            wasInMinesOfDivan = inMines;
        });

        // 接收遊戲訊息
        ClientReceiveMessageEvents.ALLOW_GAME.register(
            MetalDetector::onGameMessage
        );

        // 加入伺服器時重設
        ClientPlayConnectionEvents.JOIN.register(
            (handler, sender, client) -> reset()
        );

        // 渲染標記
        LevelRenderEvents.END_EXTRACTION.register(
            context -> extractRendering()
        );
    }

    // 判斷玩家是否連續靜止 5 Tick
    private static void updateStationaryState(Minecraft client) {
        Vec3 currentPos = client.player.position();

        if (lastTickPlayerPos == null) {
            stationaryTicks = 0;
            playerStationary = false;
        } else if (currentPos.distanceToSqr(lastTickPlayerPos)
                > MOVEMENT_THRESHOLD_SQUARED) {
            stationaryTicks = 0;
            playerStationary = false;
        } else {
            stationaryTicks++;

            playerStationary =
                stationaryTicks >= REQUIRED_STATIONARY_TICKS;
        }

        lastTickPlayerPos = currentPos;
    }

    private static boolean onGameMessage(
        Component message,
        boolean overlay
    ) {
        // 關閉模組時不處理探測訊息
        if (!GuiConfig.MetalDetector) {
            return true;
        }

        Minecraft client = Minecraft.getInstance();

        if (client.player == null || client.level == null) {
            return true;
        }

        String text = ChatFormatting.stripFormatting(
            message.getString()
        );

        if (text == null) {
            return true;
        }

        text = text.trim();

        // 挖到寶藏後，重新等待距離更新
        if (text.contains("You found")) {
            distanceBeforeTreasure = previousDistance;

            resetTreasureDetection();

            readDelayTicks = READ_DELAY_TICKS;
            waitingForFreshDistance = false;

            client.player.sendSystemMessage(
                Component.literal(
                    RnmText.PREFIX
                        + "已挖到寶藏，靜止不動進行讀取。"
                )
            );

            return true;
        }

        if (!overlay
            || readDelayTicks > 0
            || !isInMinesOfDivan()
            || !playerStationary) {
            return true;
        }

        Matcher matcher = TREASURE_PATTERN.matcher(text);

        if (!matcher.find()) {
            return true;
        }

        final double distance;

        try {
            distance = Double.parseDouble(
                matcher.group(1).replace(",", "")
            );
        } catch (NumberFormatException exception) {
            return true;
        }

        // 等待挖寶後的第一筆新距離
        if (waitingForFreshDistance) {
            if (distanceBeforeTreasure >= 0
                    && Math.abs(distance - distanceBeforeTreasure) < 0.1) {
                return true;
            }

            waitingForFreshDistance = false;
            distanceBeforeTreasure = -1.0;

            resetTreasureDetection();

            return true;
        }

        Vec3 playerPos = client.player.position();

        if (!startedLooking) {
            startedLooking = true;

            client.player.sendSystemMessage(
                Component.literal(
                    RnmText.PREFIX
                        + "Metal Detector 輔助已啟用。"
                )
            );
        }

        if (!distanceDebugShown) {
            distanceDebugShown = true;

            client.player.sendSystemMessage(
                Component.literal(
                    RnmText.PREFIX
                        + "已讀取探測距離：" + distance + "m"
                )
            );
        }

        boolean movedEnough =
            previousPlayerPos == null
                || playerPos.distanceTo(previousPlayerPos) >= 0.5;

        boolean distanceChanged =
            previousDistance < 0
                || Math.abs(distance - previousDistance) >= 0.1;

        if (!movedEnough && !distanceChanged) {
            return true;
        }

        previousDistance = distance;
        previousPlayerPos = playerPos;

        updatePossibleBlocks(distance, playerPos);

        if (possibleBlocks.size() == 1) {
            lockedTreasure = possibleBlocks.getFirst();
            reportTreasure(lockedTreasure);
        } else {
            lockedTreasure = null;
        }

        return true;
    }

    private static boolean isInMinesOfDivan() {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null || client.level == null) {
            return false;
        }

        if (minesCenter == null) {
            findCenterOfMines();
        }

        return minesCenter != null
            && client.player.blockPosition()
                .distSqr(minesCenter) <= MINES_RADIUS_SQUARED;
    }

    private static void findCenterOfMines() {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null || client.level == null) {
            return;
        }

        AABB searchBox = client.player.getBoundingBox().inflate(500);

        List<ArmorStand> armorStands =
            client.level.getEntitiesOfClass(
                ArmorStand.class,
                searchBox,
                ArmorStand::hasCustomName
            );

        for (ArmorStand armorStand : armorStands) {
            String name = ChatFormatting.stripFormatting(
                armorStand.getName().getString()
            );

            if (name == null) {
                continue;
            }

            Matcher matcher = KEEPER_PATTERN.matcher(name);

            if (!matcher.find()) {
                continue;
            }

            Vec3i offset = KEEPER_OFFSETS.get(matcher.group(1));

            if (offset != null) {
                minesCenter = armorStand.blockPosition().offset(offset);
                return;
            }
        }
    }

    private static void updatePossibleBlocks(
        double distance,
        Vec3 playerPos
    ) {
        if (minesCenter == null) {
            return;
        }

        // 第一次偵測時建立所有候選位置
        if (allCandidateBlocks.isEmpty()) {
            for (Vec3i offset : KNOWN_CHEST_OFFSETS) {
                allCandidateBlocks.add(
                    minesCenter.offset(offset).offset(0, 1, 0)
                );
            }

            possibleBlocks.clear();
            possibleBlocks.addAll(allCandidateBlocks);
        }

        List<Vec3i> filtered = new ArrayList<>();

        // 先依目前候選位置縮小範圍
        filterByDistance(
            possibleBlocks,
            filtered,
            distance,
            playerPos
        );

        // 如果沒有符合位置，重新比對所有已知位置
        if (filtered.isEmpty()) {
            filterByDistance(
                allCandidateBlocks,
                filtered,
                distance,
                playerPos
            );
        }

        possibleBlocks.clear();
        possibleBlocks.addAll(filtered);

        if (CLIENT.player == null) {
            return;
        }

        if (possibleBlocks.isEmpty()) {
            CLIENT.player.sendSystemMessage(
                Component.literal(
                    RnmText.PREFIX
                        + "沒有符合的候選座標，請停止移動後再次探測。"
                )
            );
        } else if (possibleBlocks.size() == 1) {
            lockedTreasure = possibleBlocks.getFirst();
            reportTreasure(lockedTreasure);
        } else {
            CLIENT.player.sendSystemMessage(
                Component.literal(
                    RnmText.PREFIX
                        + "剩餘 "
                        + possibleBlocks.size()
                        + " 個可能位置。"
                )
            );
        }
    }

    private static void filterByDistance(
        List<Vec3i> source,
        List<Vec3i> result,
        double distance,
        Vec3 playerPos
    ) {
        for (Vec3i pos : source) {
            double actualDistance = playerPos.distanceTo(
                Vec3.atLowerCornerOf(pos)
            );

            if (Math.abs(actualDistance - distance)
                    <= DISTANCE_TOLERANCE) {
                result.add(pos);
            }
        }
    }

    private static void reportTreasure(Vec3i candidate) {
        Vec3i target = candidate.offset(0, -1, 0);

        if (target.equals(lastReportedTreasure)) {
            return;
        }

        lastReportedTreasure = target;

        if (CLIENT.player != null) {
            CLIENT.player.sendSystemMessage(
                Component.literal(
                    RnmText.PREFIX
                        + "已鎖定寶藏座標："
                        + target.getX() + " "
                        + target.getY() + " "
                        + target.getZ()
                )
            );
        }
    }

    private static void extractRendering() {
        // 關閉模組時不渲染任何標記
        if (!GuiConfig.MetalDetector
            || CLIENT.player == null
            || CLIENT.level == null
            || !isInMinesOfDivan()) {
            return;
        }

        // 已鎖定寶藏：顯示黃色光束、方框與文字
        if (lockedTreasure != null) {
            BlockPos targetBlock = new BlockPos(
                lockedTreasure.getX(),
                lockedTreasure.getY() - 1,
                lockedTreasure.getZ()
            );

            drawBeaconBeam(targetBlock, TREASURE_COLOR, true);

            Gizmos.billboardTextOverBlock(
                "寶藏",
                targetBlock,
                0,
                TREASURE_COLOR,
                1.5F
            ).persistForMillis(50);

            return;
        }

        // 可能位置：只顯示文字，不顯示烽火台光束
        for (Vec3i pos : possibleBlocks) {
            BlockPos targetBlock = new BlockPos(
                pos.getX(),
                pos.getY() - 1,
                pos.getZ()
            );

            Gizmos.billboardTextOverBlock(
                "可能",
                targetBlock,
                0,
                POSSIBLE_COLOR,
                1.2F
            ).persistForMillis(50);
        }
    }

    private static void drawBeaconBeam(
        BlockPos targetBlock,
        int color,
        boolean showBox
    ) {
        double centerX = targetBlock.getX() + 0.5;
        double centerZ = targetBlock.getZ() + 0.5;
        double startY = targetBlock.getY() + 0.5;
        double endY = startY + BEAM_HEIGHT;

        // 外層光束
        AABB outerBeam = new AABB(
            centerX - OUTER_BEAM_RADIUS,
            startY,
            centerZ - OUTER_BEAM_RADIUS,
            centerX + OUTER_BEAM_RADIUS,
            endY,
            centerZ + OUTER_BEAM_RADIUS
        );

        Gizmos.cuboid(
            outerBeam,
            GizmoStyle.strokeAndFill(
                color,
                0.5F,
                0x30FFFF00
            )
        ).persistForMillis(50);

        // 內層光束
        AABB innerBeam = new AABB(
            centerX - INNER_BEAM_RADIUS,
            startY,
            centerZ - INNER_BEAM_RADIUS,
            centerX + INNER_BEAM_RADIUS,
            endY,
            centerZ + INNER_BEAM_RADIUS
        );

        Gizmos.cuboid(
            innerBeam,
            GizmoStyle.strokeAndFill(
                color,
                0.5F,
                0x50FFFF00
            )
        ).persistForMillis(50);

        // 最終目標的黃色方框
        if (showBox) {
            Gizmos.cuboid(
                targetBlock,
                GizmoStyle.strokeAndFill(
                    TREASURE_COLOR,
                    2.0F,
                    0x30FFFF00
                )
            ).persistForMillis(50);
        }
    }

    private static void resetTreasureDetection() {
        possibleBlocks.clear();
        possibleBlocks.addAll(allCandidateBlocks);

        previousDistance = -1.0;
        previousPlayerPos = null;

        lockedTreasure = null;
        lastReportedTreasure = null;

        uniqueConfirmations = 0;
        distanceDebugShown = false;
    }

    private static void reset() {
        minesCenter = null;

        allCandidateBlocks.clear();
        possibleBlocks.clear();

        resetTreasureDetection();

        readDelayTicks = 0;
        waitingForFreshDistance = false;
        distanceBeforeTreasure = -1.0;

        startedLooking = false;
        wasInMinesOfDivan = false;

        lastTickPlayerPos = null;
        previousPlayerPos = null;

        stationaryTicks = 0;
        playerStationary = false;
    }
}