package io.github.moregrayner.pieRay

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.Material
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Sound
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

class PieRay : JavaPlugin() {

    private lateinit var configManager: ConfigManager
    private lateinit var dataManager: DataManager
    private lateinit var vectorAnalyzer: VectorAnalyzer
    private lateinit var suspicionManager: SuspicionManager
    private lateinit var versionHandler: VersionHandler

    override fun onEnable() {
        logger.info("[PieRay] 시작중...")

        initializeComponents()
        setupScheduler()
        registerEvents()

        logger.info("[PieRay] 작동에 성공했습니다. 데이터 로딩이 완료되었습니다.")
    }

    private fun registerEvents() {
        server.pluginManager.registerEvents(PieRayListener(this), this)
    }

    override fun onDisable() {
        dataManager.saveData()
        logger.info("PieRay 플러그인이 비활성화되었습니다.")
    }

    private fun initializeComponents() {
        configManager = ConfigManager(this)
        dataManager = DataManager(this)
        vectorAnalyzer = VectorAnalyzer(configManager)
        suspicionManager = SuspicionManager(this, configManager)
        versionHandler = VersionHandler()

        dataManager.loadData()
    }

    private fun setupScheduler() {
        object : BukkitRunnable() {
            override fun run() {
                checkPlayersNearTargetBlocks()
            }
        }.runTaskTimer(this, 0L, 100L) // 5초마다 실행
    }

    private fun checkPlayersNearTargetBlocks() {
        val targetBlocks = dataManager.getCachedBlocks()
        val onlinePlayers = Bukkit.getOnlinePlayers()

        for (player in onlinePlayers) {
            val nearestBlock = findNearestTargetBlock(player, targetBlocks)
            nearestBlock?.let { blockData ->
                suspicionManager.startTracking(player, blockData)
            }
        }
    }

    private fun findNearestTargetBlock(player: Player, blocks: Map<Location, BlockData>): BlockData? {
        var nearest: BlockData? = null
        var minDistance = Double.MAX_VALUE

        for ((location, blockData) in blocks) {
            if (location.world == player.world) {
                val distance = location.distance(player.location)
                if (distance < minDistance && distance <= configManager.getDetectionRange()) {
                    minDistance = distance
                    nearest = blockData
                }
            }
        }

        return nearest
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (!sender.hasPermission("pieray.admin")) {
            sender.sendMessage("${ChatColor.GOLD}[${ChatColor.WHITE}PieRay${ChatColor.GOLD}]${ChatColor.WHITE}권한이 없습니다.")
            return true
        }

        when (args.getOrNull(0)?.lowercase()) {
            "scan" -> handleScanCommand(sender, args)
            "list" -> handleListCommand(sender)
            "reload" -> {
                configManager.reloadConfig()
                sender.sendMessage("${ChatColor.GOLD}[${ChatColor.WHITE}PieRay${ChatColor.GOLD}]${ChatColor.WHITE} 설정이 다시 로드되었습니다.")
            }
            else -> {
                sender.sendMessage("${ChatColor.GOLD}[${ChatColor.WHITE}PieRay${ChatColor.GOLD}]${ChatColor.WHITE} 사용법: /pieray <scan|list|reload>")
            }
        }
        return true
    }

    private fun handleScanCommand(sender: CommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage("${ChatColor.GOLD}[${ChatColor.WHITE}PieRay${ChatColor.GOLD}]${ChatColor.WHITE} 사용법: /pieray scan <범위>")
            return
        }

        val range = args[1].toIntOrNull()
        if (range == null || range <= 0) {
            sender.sendMessage("${ChatColor.GOLD}[${ChatColor.WHITE}PieRay${ChatColor.GOLD}]${ChatColor.WHITE} 올바른 범위를 입력하세요.")
            return
        }

        if (sender !is Player) {
            sender.sendMessage("${ChatColor.GOLD}[${ChatColor.WHITE}PieRay${ChatColor.GOLD}]${ChatColor.WHITE} 플레이어만 이 명령어를 사용할 수 있습니다.")
            return
        }

        scanArea(sender, range)
    }

    private fun handleListCommand(sender: CommandSender) {
        val suspiciousPlayers = suspicionManager.getSuspiciousPlayers()

        if (suspiciousPlayers.isEmpty()) {
            sender.sendMessage("${ChatColor.GOLD}[${ChatColor.WHITE}PieRay${ChatColor.GOLD}]${ChatColor.WHITE} 의심 대상이 없습니다.")
            return
        }

        sender.sendMessage("${ChatColor.GOLD}===${ChatColor.WHITE} 의심 대상 목록 ${ChatColor.GOLD}===")
        for ((player, score) in suspiciousPlayers) {
            sender.sendMessage("${ChatColor.WHITE}${player.name}: ${ChatColor.YELLOW}${String.format("%.2f", score)}점")
        }
    }

    private fun scanArea(player: Player, range: Int) {
        val center = player.location
        val targetMaterials = configManager.getTargetBlocks()
        var foundBlocks = 0
        val filteredBlocks: Int

        player.sendMessage("${ChatColor.GOLD}[${ChatColor.WHITE}PieRay${ChatColor.GOLD}]${ChatColor.WHITE} 스캔을 시작합니다. (범위: ${range})")

        val candidateBlocks = mutableSetOf<Location>()

        for (x in -range..range) {
            for (y in -range..range) {
                for (z in -range..range) {
                    val location = center.clone().add(x.toDouble(), y.toDouble(), z.toDouble())
                    val block = location.block

                    if (targetMaterials.contains(block.type)) {
                        candidateBlocks.add(location.clone())
                        foundBlocks++
                    }
                }
            }
        }

        val validBlocks = filterExposedBlocks(candidateBlocks, targetMaterials)
        filteredBlocks = foundBlocks - validBlocks.size

        for (location in validBlocks) {
            val block = location.block
            val blockData = BlockData(location.clone(), block.type)
            dataManager.addBlock(blockData)
        }

        dataManager.saveData()
        player.sendMessage("${ChatColor.GOLD}[${ChatColor.WHITE}PieRay${ChatColor.GOLD}]${ChatColor.WHITE} 스캔 완료!")
        player.sendMessage("${ChatColor.GOLD}[${ChatColor.WHITE}PieRay${ChatColor.GOLD}]${ChatColor.WHITE} 발견: ${foundBlocks}개, 등록: ${validBlocks.size}개, 제외: ${filteredBlocks}개")
        if (filteredBlocks > 0) {
            player.sendMessage("${ChatColor.GOLD}[${ChatColor.WHITE}PieRay${ChatColor.GOLD}]${ChatColor.GRAY} (노출된 블록 ${filteredBlocks}개가 제외되었습니다)")
        }
    }

    private fun filterExposedBlocks(candidateBlocks: Set<Location>, targetMaterials: Set<Material>): Set<Location> {
        val validBlocks = candidateBlocks.toMutableSet()
        val toRemove = mutableSetOf<Location>()

        var removedInThisIteration: Boolean
        do {
            removedInThisIteration = false
            toRemove.clear()

            for (location in validBlocks) {
                if (isBlockExposed(location, validBlocks, targetMaterials)) {
                    toRemove.add(location)
                }
            }

            if (toRemove.isNotEmpty()) {
                validBlocks.removeAll(toRemove)
                removedInThisIteration = true
            }
        } while (removedInThisIteration && validBlocks.isNotEmpty())

        return validBlocks
    }

    private fun isBlockExposed(location: Location, validBlocks: Set<Location>, targetMaterials: Set<Material>): Boolean {
        val directions = arrayOf(
            Vector(1, 0, 0),   // 동쪽
            Vector(-1, 0, 0),  // 서쪽
            Vector(0, 1, 0),   // 위쪽
            Vector(0, -1, 0),  // 아래쪽
            Vector(0, 0, 1),   // 남쪽
            Vector(0, 0, -1)   // 북쪽
        )

        var exposedSides = 0

        for (direction in directions) {
            val adjacentLocation = location.clone().add(direction)
            val adjacentBlock = adjacentLocation.block

            if (isExposingMaterial(adjacentBlock.type) ||
                (!validBlocks.contains(adjacentLocation) && !targetMaterials.contains(adjacentBlock.type))) {
                exposedSides++
            }
        }

        val maxExposedSides = configManager.getMaxExposedSides()
        return exposedSides >= maxExposedSides
    }

    private fun isExposingMaterial(material: Material): Boolean {
        return when (material) {
            Material.AIR -> true
            Material.WATER -> true
            Material.LAVA -> true
            else -> {
                val materialName = material.name
                materialName.contains("WATER") ||
                        materialName.contains("LAVA") ||
                        materialName == "CAVE_AIR" ||
                        materialName == "VOID_AIR"
            }
        }
    }
}

class ConfigManager(private val plugin: PieRay) {
    private val config = plugin.config

    init {
        plugin.saveDefaultConfig()
        setupDefaultConfig()
    }

    private fun setupDefaultConfig() {
        if (!config.contains("areart")) {
            config.set("areart", true)
        }
        if (!config.contains("areartMessage")) {
            config.set("areartMessage", true)
        }
        if (!config.contains("blocks")) {
            config.set("blocks", listOf("DIAMOND_ORE", "GOLD_ORE", "IRON_ORE", "EMERALD_ORE", "ANCIENT_DEBRIS"))
        }
        if (!config.contains("detection.range")) {
            config.set("detection.range", 10.0)
        }
        if (!config.contains("detection.passDistance")) {
            config.set("detection.passDistance", 5.0)
        }
        if (!config.contains("suspicion.threshold")) {
            config.set("suspicion.threshold", 100.0)
        }
        if (!config.contains("suspicion.decreaseRate")) {
            config.set("suspicion.decreaseRate", 0.5)
        }
        if (!config.contains("vector.sight.weight")) {
            config.set("vector.sight.weight", 30.0)
        }
        if (!config.contains("vector.movement.weight")) {
            config.set("vector.movement.weight", 25.0)
        }
        if (!config.contains("vector.cone.weight")) {
            config.set("vector.cone.weight", 20.0)
        }
        if (!config.contains("vector.cone.angle")) {
            config.set("vector.cone.angle", 45.0)
        }
        if (!config.contains("vector.sight.threshold")) {
            config.set("vector.sight.threshold", 60.0)
        }
        if (!config.contains("vector.movement.threshold")) {
            config.set("vector.movement.threshold", 70.0)
        }
        if (!config.contains("vector.cone.threshold")) {
            config.set("vector.cone.threshold", 50.0)
        }
        if (!config.contains("vector.sight.gridMode")) {
            config.set("vector.sight.gridMode", "dynamic")
        }
        if (!config.contains("vector.sight.gridSize")) {
            config.set("vector.sight.gridSize", 0.6)
        }
        if (!config.contains("vector.sight.centerWeight")) {
            config.set("vector.sight.centerWeight", 1.5)
        }
        if (!config.contains("filtering.maxExposedSides")) {
            config.set("filtering.maxExposedSides", 1)
        }
        if (!config.contains("filtering.enableWaterCheck")) {
            config.set("filtering.enableWaterCheck", true)
        }
        if (!config.contains("filtering.enableLavaCheck")) {
            config.set("filtering.enableLavaCheck", true)
        }
        if (!config.contains("advanced.enableWiggleDetection")) {
            config.set("advanced.enableWiggleDetection", true)
        }
        if (!config.contains("advanced.wiggleAngleThreshold")) {
            config.set("advanced.wiggleAngleThreshold", 45.0)
        }
        if (!config.contains("advanced.wiggleDistanceThreshold")) {
            config.set("advanced.wiggleDistanceThreshold", 3.0)
        }
        if (!config.contains("advanced.wiggleTimeThreshold")) {
            config.set("advanced.wiggleTimeThreshold", 1000)
        }
        if (!config.contains("advanced.wiggleCountThreshold")) {
            config.set("advanced.wiggleCountThreshold", 3)
        }
        if (!config.contains("advanced.wiggleSuspicionBonus")) {
            config.set("advanced.wiggleSuspicionBonus", 5.0)
        }
        if (!config.contains("advanced.enableLineOfSight")) {
            config.set("advanced.enableLineOfSight", true)
        }
        if (!config.contains("advanced.lineOfSightMaxDistance")) {
            config.set("advanced.lineOfSightMaxDistance", 50)
        }
        if (!config.contains("advanced.enableBlockFaceAnalysis")) {
            config.set("advanced.enableBlockFaceAnalysis", false)
        }
        if (!config.contains("advanced.enableAdjacentRemoval")) {
            config.set("advanced.enableAdjacentRemoval", true)
        }
        if (!config.contains("advanced.enableCascadeExposure")) {
            config.set("advanced.enableCascadeExposure", true)
        }

        plugin.saveConfig()
    }

    fun reloadConfig() {
        plugin.reloadConfig()
    }

    fun isAreartEnabled() = config.getBoolean("areart", true)
    fun isAreartMessageEnabled() = config.getBoolean("areartMessage", true)

    fun getTargetBlocks(): Set<Material> {
        return config.getStringList("blocks").mapNotNull { materialName ->
            try {
                Material.valueOf(materialName.uppercase())
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("올바르지 않은 Material: $materialName")
                null
            }
        }.toSet()
    }

    fun getDetectionRange() = config.getDouble("detection.range", 10.0)
    fun getPassDistance() = config.getDouble("detection.passDistance", 5.0)
    fun getSuspicionThreshold() = config.getDouble("suspicion.threshold", 100.0)
    fun getDecreaseRate() = config.getDouble("suspicion.decreaseRate", 0.5)

    fun getSightVectorWeight() = config.getDouble("vector.sight.weight", 30.0)
    fun getMovementVectorWeight() = config.getDouble("vector.movement.weight", 25.0)
    fun getConeVectorWeight() = config.getDouble("vector.cone.weight", 20.0)
    fun getConeAngle() = config.getDouble("vector.cone.angle", 45.0)

    fun getSightVectorThreshold() = config.getDouble("vector.sight.threshold", 60.0)
    fun getMovementVectorThreshold() = config.getDouble("vector.movement.threshold", 70.0)
    fun getConeVectorThreshold() = config.getDouble("vector.cone.threshold", 50.0)

    fun getSightGridMode() = config.getString("vector.sight.gridMode", "dynamic")
    fun getSightGridSize() = config.getDouble("vector.sight.gridSize", 0.6)
    fun getSightCenterWeight() = config.getDouble("vector.sight.centerWeight", 1.5)

    fun getMaxExposedSides() = config.getInt("filtering.maxExposedSides", 1)
    fun isWaterCheckEnabled() = config.getBoolean("filtering.enableWaterCheck", true)
    fun isLavaCheckEnabled() = config.getBoolean("filtering.enableLavaCheck", true)

    fun isWiggleDetectionEnabled() = config.getBoolean("advanced.enableWiggleDetection", true)
    fun getWiggleAngleThreshold() = config.getDouble("advanced.wiggleAngleThreshold", 45.0)
    fun getWiggleDistanceThreshold() = config.getDouble("advanced.wiggleDistanceThreshold", 3.0)
    fun getWiggleTimeThreshold() = config.getLong("advanced.wiggleTimeThreshold", 1000)
    fun getWiggleCountThreshold() = config.getInt("advanced.wiggleCountThreshold", 3)
    fun getWiggleSuspicionBonus() = config.getDouble("advanced.wiggleSuspicionBonus", 5.0)

    fun isLineOfSightEnabled() = config.getBoolean("advanced.enableLineOfSight", true)
    fun getLineOfSightMaxDistance() = config.getInt("advanced.lineOfSightMaxDistance", 50)

    fun isBlockFaceAnalysisEnabled() = config.getBoolean("advanced.enableBlockFaceAnalysis", false)
    fun isAdjacentRemovalEnabled() = config.getBoolean("advanced.enableAdjacentRemoval", true)
    fun isCascadeExposureEnabled() = config.getBoolean("advanced.enableCascadeExposure", true)
}

class DataManager(private val plugin: PieRay) {
    private val dataFile = File(plugin.dataFolder, "data.yml")
    private val cachedBlocks = ConcurrentHashMap<Location, BlockData>()

    fun loadData() {
        if (!dataFile.exists()) {
            plugin.dataFolder.mkdirs()
            dataFile.createNewFile()
            return
        }

        val yaml = YamlConfiguration.loadConfiguration(dataFile)
        val blocksSection = yaml.getConfigurationSection("blocks") ?: return

        for (key in blocksSection.getKeys(false)) {
            val blockSection = blocksSection.getConfigurationSection(key) ?: continue

            try {
                val world = plugin.server.getWorld(blockSection.getString("world") ?: continue) ?: continue
                val x = blockSection.getDouble("x")
                val y = blockSection.getDouble("y")
                val z = blockSection.getDouble("z")
                val material = Material.valueOf(blockSection.getString("material") ?: continue)

                val location = Location(world, x, y, z)
                val blockData = BlockData(location, material)
                cachedBlocks[location] = blockData
            } catch (e: Exception) {
                plugin.logger.warning("블록 데이터 로드 실패: ${e.message}")
            }
        }

        plugin.logger.info("${cachedBlocks.size}개의 블록 데이터를 로드했습니다.")
    }

    fun saveData() {
        val yaml = YamlConfiguration()

        cachedBlocks.values.forEachIndexed { index, blockData ->
            val section = yaml.createSection("blocks.$index")
            section.set("world", blockData.location.world?.name)
            section.set("x", blockData.location.x)
            section.set("y", blockData.location.y)
            section.set("z", blockData.location.z)
            section.set("material", blockData.material.name)
        }

        try {
            yaml.save(dataFile)
        } catch (e: Exception) {
            plugin.logger.severe("데이터 저장 실패: ${e.message}")
        }
    }

    fun addBlock(blockData: BlockData) {
        cachedBlocks[blockData.location] = blockData
    }

    fun removeBlock(location: Location) {
        cachedBlocks.remove(location)
    }

    fun getCachedBlocks(): Map<Location, BlockData> = cachedBlocks.toMap()
}

data class BlockData(
    val location: Location,
    val material: Material
)

class VectorAnalyzer(private val configManager: ConfigManager) {

    fun calculateSuspicionScore(player: Player, blockData: BlockData, playerHistory: PlayerHistory): Double {
        val sightScore = calculateSightVectorScore(player, blockData, playerHistory)
        val movementScore = calculateMovementVectorScore(player, blockData, playerHistory)
        val coneScore = calculateConeVectorScore(player, blockData, playerHistory)

        return sightScore + movementScore + coneScore
    }

    private fun calculateSightVectorScore(player: Player, blockData: BlockData, history: PlayerHistory): Double {
        val playerDirection = player.location.direction
        val toBlock = blockData.location.toVector().subtract(player.location.toVector()).normalize()

        val dotProduct = playerDirection.dot(toBlock)
        val angle = Math.toDegrees(acos(dotProduct.coerceIn(-1.0, 1.0)))

        val suspicionMultiplier = when {
            angle <= 15.0 -> 1.0
            angle <= 30.0 -> 0.7
            angle <= 45.0 -> 0.4
            angle <= 60.0 -> 0.2
            else -> 0.0
        }

        return configManager.getSightVectorWeight() * suspicionMultiplier
    }

    private fun calculateMovementVectorScore(player: Player, blockData: BlockData, history: PlayerHistory): Double {
        val previousLocation = history.getLastLocation() ?: return 0.0
        val movementDirection = player.location.toVector().subtract(previousLocation.toVector()).normalize()
        val toBlock = blockData.location.toVector().subtract(player.location.toVector()).normalize()

        val dotProduct = movementDirection.dot(toBlock)
        val alignment = (dotProduct + 1.0) / 2.0 // -1~1을 0~1로 변환

        return configManager.getMovementVectorWeight() * alignment
    }

    private fun calculateConeVectorScore(player: Player, blockData: BlockData, history: PlayerHistory): Double {
        val entryPoint = history.getEntryPoint() ?: return 0.0
        val currentPos = player.location.toVector()
        val blockPos = blockData.location.toVector()
        val entryPos = entryPoint.toVector()

        val entryToBlock = blockPos.subtract(entryPos).normalize()
        val currentToBlock = blockPos.subtract(currentPos).normalize()

        val dotProduct = entryToBlock.dot(currentToBlock)
        val angle = Math.toDegrees(acos(dotProduct.coerceIn(-1.0, 1.0)))

        val coneAngle = configManager.getConeAngle()
        val suspicionMultiplier = if (angle <= coneAngle) {
            1.0 - (angle / coneAngle)
        } else {
            0.0
        }

        return configManager.getConeVectorWeight() * suspicionMultiplier
    }
}

class PlayerHistory {
    private val locationHistory = mutableListOf<Location>()
    private var entryPoint: Location? = null
    private var trackingStartTime = System.currentTimeMillis()

    private var lastMovementTimestamp = System.currentTimeMillis()
    private var wiggleCount = 0
    private var totalAngleMatchTime = 0.0
    private var totalLookMatchTime = 0.0
    private var totalConeMatchTime = 0.0

    fun addLocation(location: Location) {
        locationHistory.add(location.clone())
        if (locationHistory.size > 20) {
            locationHistory.removeAt(0)
        }
    }

    fun getLastLocation(): Location? = locationHistory.getOrNull(locationHistory.size - 2)

    fun setEntryPoint(location: Location) {
        if (entryPoint == null) {
            entryPoint = location.clone()
        }
    }

    fun getEntryPoint(): Location? = entryPoint

    fun getTrackingDuration(): Long = System.currentTimeMillis() - trackingStartTime

    fun incrementWiggleCount() { wiggleCount++ }
    fun getWiggleCount(): Int = wiggleCount
    fun resetWiggleCount() { wiggleCount = 0 }

    fun addAngleMatchTime(time: Double) { totalAngleMatchTime += time }
    fun addLookMatchTime(time: Double) { totalLookMatchTime += time }
    fun addConeMatchTime(time: Double) { totalConeMatchTime += time }

    fun getTotalAngleMatchTime(): Double = totalAngleMatchTime
    fun getTotalLookMatchTime(): Double = totalLookMatchTime
    fun getTotalConeMatchTime(): Double = totalConeMatchTime

    fun getLastMovementTimestamp(): Long = lastMovementTimestamp

    fun reset() {
        locationHistory.clear()
        entryPoint = null
        trackingStartTime = System.currentTimeMillis()
    }
}

class SuspicionManager(
    private val plugin: PieRay,
    private val configManager: ConfigManager
) {
    private val playerSuspicion = ConcurrentHashMap<UUID, Double>()
    private val playerHistory = ConcurrentHashMap<UUID, PlayerHistory>()
    private val currentlyTracking = ConcurrentHashMap<UUID, BlockData>()
    private val vectorAnalyzer = VectorAnalyzer(configManager)

    fun startTracking(player: Player, blockData: BlockData) {
        val playerId = player.uniqueId
        val currentTarget = currentlyTracking[playerId]

        if (currentTarget == null ||
            player.location.distance(blockData.location) < player.location.distance(currentTarget.location)) {

            currentlyTracking[playerId] = blockData

            val history = playerHistory.computeIfAbsent(playerId) { PlayerHistory() }
            history.setEntryPoint(player.location)
            history.addLocation(player.location)

            val suspicionScore = vectorAnalyzer.calculateSuspicionScore(player, blockData, history)
            addSuspicion(player, suspicionScore)
        }

        val passDistance = configManager.getPassDistance()
        currentTarget?.let { target ->
            if (player.location.distance(target.location) > passDistance) {
                val currentSuspicion = playerSuspicion[playerId] ?: 0.0
                val decreaseAmount = currentSuspicion * configManager.getDecreaseRate()
                playerSuspicion[playerId] = (currentSuspicion - decreaseAmount).coerceAtLeast(0.0)

                currentlyTracking.remove(playerId)
                playerHistory[playerId]?.reset()
            }
        }

        if (configManager.isWiggleDetectionEnabled()) {
            val history = playerHistory[player.uniqueId]
            if (history != null) {
                detectWiggling(player, history)
            }
        }
    }

    private fun detectWiggling(player: Player, history: PlayerHistory) {
        val lastLocation = history.getLastLocation() ?: return
        val currentLocation = player.location
        val currentTime = System.currentTimeMillis()

        if (currentLocation.distance(lastLocation) >= configManager.getWiggleDistanceThreshold()) {
            val previousMoveVector = currentLocation.toVector().subtract(lastLocation.toVector()).normalize()

            val locationHistory = history.getLastLocation()
            locationHistory?.let { prevLoc ->
                val currentMoveVector = currentLocation.toVector().subtract(prevLoc.toVector()).normalize()
                val angleBetweenMoves = Math.toDegrees(previousMoveVector.angle(currentMoveVector).toDouble())

                val wiggleThreshold = 180 - configManager.getWiggleAngleThreshold()

                if (angleBetweenMoves >= wiggleThreshold &&
                    angleBetweenMoves <= (180 + configManager.getWiggleAngleThreshold()) &&
                    (currentTime - history.getLastMovementTimestamp()) <= configManager.getWiggleTimeThreshold()) {

                    history.incrementWiggleCount()

                    if (history.getWiggleCount() >= configManager.getWiggleCountThreshold()) {
                        plugin.logger.info("[PieRay-Wiggle] ${player.name}이 의심스러운 와리가리 움직임을 보입니다! 와리가리 횟수: ${history.getWiggleCount()}")

                        val wiggleBonus = configManager.getWiggleSuspicionBonus()
                        addSuspicion(player, wiggleBonus)
                    }
                } else {
                    history.resetWiggleCount()
                }
            }
        }
    }

    private fun addSuspicion(player: Player, score: Double) {
        val playerId = player.uniqueId
        val currentScore = playerSuspicion[playerId] ?: 0.0
        val newScore = currentScore + score

        playerSuspicion[playerId] = newScore

        if (newScore >= configManager.getSuspicionThreshold()) {
            applyPenalty(player)

            if (configManager.isAreartMessageEnabled()) {
                sendWarningToOps(player, currentlyTracking[playerId])
            }
        }

        if (configManager.isAreartEnabled()) {
            sendSuspicionListToOps()
        }
    }

    private fun applyPenalty(player: Player) {
        try {
            player.addPotionEffect(PotionEffect(PotionEffectType.MINING_FATIGUE, 1200, 2))
        } catch (e: Exception) {
            @Suppress("1.16.5")
            player.addPotionEffect(PotionEffect(PotionEffectType.MINING_FATIGUE, 1200, 2, false, true))
        }

        try {
            player.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f)
        } catch (e: Exception) {
            try {
                @Suppress("1.16.5")
                player.playSound(player.location, "entity.ender_dragon.growl", 1.0f, 1.0f)
            } catch (e2: Exception) {
                // 사운드 재생 실패시 무시
            }
        }
    }

    private fun sendWarningToOps(player: Player, blockData: BlockData?) {
        val material = blockData?.material?.toString() ?: "UNKNOWN"
        val message = "${ChatColor.RED}[PieRay] ${player.name}님이 XRay 의심 대상으로 간주됩니다. 블록: $material"

        plugin.server.onlinePlayers
            .filter { it.isOp }
            .forEach { it.sendMessage(message) }
    }

    private fun sendSuspicionListToOps() {
        val suspiciousCount = playerSuspicion.values.count { it >= configManager.getSuspicionThreshold() * 0.7 }

        if (suspiciousCount > 0) {
            val message = "${ChatColor.YELLOW}[PieRay] ${suspiciousCount}명의 의심대상이 있습니다. ${ChatColor.AQUA}[보기]"

            plugin.server.onlinePlayers
                .filter { it.isOp }
                .forEach {
                    it.sendMessage(message)
                }
        }
    }

    fun onBlockBreak(player: Player, location: Location, material: Material) {
        val playerId = player.uniqueId
        val targetBlocks = configManager.getTargetBlocks()

        if (targetBlocks.contains(material)) {
            val blockData = BlockData(location, material)
            val history = playerHistory[playerId] ?: PlayerHistory()

            val sightScore = if (configManager.isBlockFaceAnalysisEnabled()) {
                calculateBlockFaceSightVector(player, blockData)
            } else {
                calculateSightVectorOnMining(player, blockData)
            }
            val movementScore = calculateMovementVectorOnMining(player, blockData, history)
            val coneScore = calculateConeVectorOnMining(player, blockData, history)
            val lineOfSightBonus = if (configManager.isLineOfSightEnabled()) {
                checkLineOfSightBonus(player, blockData)
            } else {
                0.0
            }

            val totalScore = sightScore + movementScore + coneScore + lineOfSightBonus

            if (totalScore > 0) {
                addSuspicion(player, totalScore)

                plugin.logger.info("[PieRay] ${player.name} 채굴 분석: 시야=${String.format("%.1f", sightScore)}, 동선=${String.format("%.1f", movementScore)}, 원뿔=${String.format("%.1f", coneScore)}")
            }

            val dataManager = plugin.javaClass.getDeclaredField("dataManager").let { field ->
                field.isAccessible = true
                field.get(plugin) as DataManager
            }
            dataManager.removeBlock(location)

            if (configManager.isCascadeExposureEnabled()) {
                cascadeExposeBlocks(location)
            }

            if (configManager.isAdjacentRemovalEnabled()) {
                removeAdjacentBlocks(location, targetBlocks)
            }

            if (configManager.isAdjacentRemovalEnabled()) {
                removeAdjacentBlocks(location, targetBlocks)
            }
        }
    }

    private fun calculateBlockFaceSightVector(player: Player, blockData: BlockData): Double {
        val playerEyeLocation = player.eyeLocation
        val playerDirection = playerEyeLocation.direction.normalize()
        val blockCenter = blockData.location.clone().add(0.5, 0.5, 0.5)

        val faceCenters = listOf(
            blockCenter.clone().add(0.5, 0.0, 0.0),   // 동쪽
            blockCenter.clone().add(-0.5, 0.0, 0.0),  // 서쪽
            blockCenter.clone().add(0.0, 0.5, 0.0),   // 위쪽
            blockCenter.clone().add(0.0, -0.5, 0.0),  // 아래쪽
            blockCenter.clone().add(0.0, 0.0, 0.5),   // 남쪽
            blockCenter.clone().add(0.0, 0.0, -0.5)   // 북쪽
        )

        var minAngle = Double.MAX_VALUE

        for (faceCenter in faceCenters) {
            val toFaceVector = faceCenter.toVector().subtract(playerEyeLocation.toVector()).normalize()
            val angle = Math.toDegrees(playerDirection.angle(toFaceVector).toDouble())
            if (angle < minAngle) {
                minAngle = angle
            }
        }

        val suspicionPercentage = when {
            minAngle <= 10.0 -> 100.0
            minAngle <= 20.0 -> 80.0
            minAngle <= 30.0 -> 60.0
            minAngle <= 45.0 -> 40.0
            minAngle <= 60.0 -> 20.0
            else -> 0.0
        }

        return if (suspicionPercentage >= configManager.getSightVectorThreshold()) {
            configManager.getSightVectorWeight() * (suspicionPercentage / 100.0)
        } else {
            0.0
        }
    }

    private fun checkLineOfSightBonus(player: Player, blockData: BlockData): Double {
        val playerEye = player.eyeLocation
        val blockCenter = blockData.location.clone().add(0.5, 0.5, 0.5)

        if (isLineOfSightClear(playerEye, blockCenter)) {
            return 5.0
        }

        return 0.0
    }

    private fun isLineOfSightClear(startLoc: Location, targetLoc: Location): Boolean {
        if (startLoc.world != targetLoc.world) return false

        val startVector = startLoc.toVector()
        val targetVector = targetLoc.toVector()
        val direction = targetVector.clone().subtract(startVector).normalize()
        val distance = startVector.distance(targetVector)
        val maxDistance = configManager.getLineOfSightMaxDistance()

        if (distance > maxDistance) return false

        val step = 0.2
        val currentVector = startVector.clone()
        var currentDistance = 0.0

        while (currentDistance < distance) {
            currentVector.add(direction.clone().multiply(step))
            currentDistance += step

            val blockAtCurrentPos = startLoc.world?.let { currentVector.toLocation(it).block }

            if (blockAtCurrentPos?.type?.isSolid == true &&
                blockAtCurrentPos.type != Material.WATER &&
                blockAtCurrentPos.type != Material.LAVA) {

                val currentBlockLoc = blockAtCurrentPos.location
                if (currentBlockLoc.blockX != targetLoc.blockX ||
                    currentBlockLoc.blockY != targetLoc.blockY ||
                    currentBlockLoc.blockZ != targetLoc.blockZ) {
                    return false
                }
            }
        }

        return true
    }

    private fun removeAdjacentBlocks(centerLocation: Location, targetBlocks: Set<Material>) {
        val directions = arrayOf(
            Vector(1, 0, 0), Vector(-1, 0, 0),
            Vector(0, 1, 0), Vector(0, -1, 0),
            Vector(0, 0, 1), Vector(0, 0, -1)
        )

        val dataManager = plugin.javaClass.getDeclaredField("dataManager").let { field ->
            field.isAccessible = true
            field.get(plugin) as DataManager
        }

        for (direction in directions) {
            val adjacentLocation = centerLocation.clone().add(direction)
            val adjacentBlock = adjacentLocation.block

            if (targetBlocks.contains(adjacentBlock.type)) {
                dataManager.removeBlock(adjacentLocation)
                plugin.logger.info("[PieRay-Adjacent] 인접한 ${adjacentBlock.type}을 ${adjacentLocation}에서 제거했습니다.")
            }
        }
    }

    private fun calculateSightVectorOnMining(player: Player, blockData: BlockData): Double {
        val playerEyeLocation = player.eyeLocation
        val blockCenter = blockData.location.clone().add(0.5, 0.5, 0.5)

        val targetPoints = when (configManager.getSightGridMode()) {
            "dynamic" -> generateDynamic3x3Points(blockCenter, playerEyeLocation)
            else -> generateStatic3x3Points(blockCenter)
        }

        val playerDirection = playerEyeLocation.direction.normalize()
        var totalWeight = 0.0
        var weightedSuspicion = 0.0

        val weights = arrayOf(
            0.8, 1.0, 0.8,  // 상단 행
            1.0, configManager.getSightCenterWeight(), 1.0,
            0.8, 1.0, 0.8   // 하단 행
        )

        targetPoints.forEachIndexed { index, point ->
            val toPoint = point.toVector().subtract(playerEyeLocation.toVector()).normalize()
            val dotProduct = playerDirection.dot(toPoint)
            val angleRadians = acos(dotProduct.coerceIn(-1.0, 1.0))
            val angleDegrees = Math.toDegrees(angleRadians)

            val pointSuspicion = calculatePointSuspicion(angleDegrees)
            val weight = weights[index]

            weightedSuspicion += pointSuspicion * weight
            totalWeight += weight
        }

        val averageSuspicion = if (totalWeight > 0) {
            (weightedSuspicion / totalWeight) * 100.0
        } else {
            0.0
        }

        return if (averageSuspicion >= configManager.getSightVectorThreshold()) {
            configManager.getSightVectorWeight() * (averageSuspicion / 100.0)
        } else {
            0.0
        }
    }

    private fun generateStatic3x3Points(blockCenter: Location): List<Location> {
        val points = mutableListOf<Location>()
        val world = blockCenter.world
        val baseX = blockCenter.x
        val baseY = blockCenter.y
        val baseZ = blockCenter.z
        val step = configManager.getSightGridSize() / 2.0

        for (i in -1..1) {
            for (j in -1..1) {
                val x = baseX + (i * step)
                val z = baseZ + (j * step)
                points.add(Location(world, x, baseY, z))
            }
        }

        return points
    }

    private fun generateDynamic3x3Points(blockCenter: Location, playerEyeLocation: Location): List<Location> {
        val points = mutableListOf<Location>()
        //val world = blockCenter.world

        val toBlockVector = blockCenter.toVector().subtract(playerEyeLocation.toVector()).normalize()

        val up = Vector(0, 1, 0)
        val right = toBlockVector.getCrossProduct(up).normalize()
        val actualUp = right.getCrossProduct(toBlockVector).normalize()

        val gridSize = configManager.getSightGridSize()
        val step = gridSize / 2.0

        for (i in -1..1) {
            for (j in -1..1) {
                val offset = right.clone().multiply(i * step)
                    .add(actualUp.clone().multiply(j * step))

                val point = blockCenter.clone().add(offset)
                points.add(point)
            }
        }

        return points
    }

    private fun calculatePointSuspicion(angleDegrees: Double): Double {
        return when {
            angleDegrees <= 5.0 -> 1.0    // 100%: 시선 정확도
            angleDegrees <= 10.0 -> 0.9   // 90%
            angleDegrees <= 15.0 -> 0.8   // 80%
            angleDegrees <= 20.0 -> 0.7   // 70%
            angleDegrees <= 30.0 -> 0.5   // 50%
            angleDegrees <= 45.0 -> 0.3   // 30%
            angleDegrees <= 60.0 -> 0.1   // 10%
            else -> 0.0                   // 0%
        }
    }

    private fun calculateMovementVectorOnMining(player: Player, blockData: BlockData, history: PlayerHistory): Double {
        val previousLocation = history.getLastLocation() ?: return 0.0

        val movementDirection = player.location.toVector()
            .subtract(previousLocation.toVector()).normalize()
        val toBlock = blockData.location.clone().add(0.5, 0.5, 0.5)
            .toVector().subtract(previousLocation.toVector()).normalize()

        val dotProduct = movementDirection.dot(toBlock)
        val angleRadians = acos(dotProduct.coerceIn(-1.0, 1.0))
        val angleDegrees = Math.toDegrees(angleRadians)

        val alignmentPercentage = when {
            angleDegrees <= 15.0 -> 100.0  // 100%: 이동 정확도
            angleDegrees <= 30.0 -> 85.0   // 85%
            angleDegrees <= 45.0 -> 70.0   // 70%
            angleDegrees <= 60.0 -> 50.0   // 50%
            angleDegrees <= 90.0 -> 30.0   // 30%
            else -> 0.0                    // 0%
        }

        return if (alignmentPercentage >= configManager.getMovementVectorThreshold()) {
            configManager.getMovementVectorWeight() * (alignmentPercentage / 100.0)
        } else {
            0.0
        }
    }

    private fun calculateConeVectorOnMining(player: Player, blockData: BlockData, history: PlayerHistory): Double {
        val entryPoint = history.getEntryPoint() ?: return 0.0

        val currentPos = player.location.toVector()
        val blockPos = blockData.location.clone().add(0.5, 0.5, 0.5).toVector()
        val entryPos = entryPoint.toVector()

        val idealVector = blockPos.subtract(entryPos).normalize()
        val actualVector = currentPos.subtract(entryPos).normalize()

        val dotProduct = idealVector.dot(actualVector)
        val angleRadians = acos(dotProduct.coerceIn(-1.0, 1.0))
        val angleDegrees = Math.toDegrees(angleRadians)

        val coneAngle = configManager.getConeAngle()

        val conePercentage = if (angleDegrees <= coneAngle) {
            ((coneAngle - angleDegrees) / coneAngle) * 100.0
        } else {
            0.0
        }

        return if (conePercentage >= configManager.getConeVectorThreshold()) {
            configManager.getConeVectorWeight() * (conePercentage / 100.0)
        } else {
            0.0
        }
    }

    fun getSuspiciousPlayers(): Map<Player, Double> {
        return playerSuspicion.mapNotNull { (playerId, score) ->
            plugin.server.getPlayer(playerId)?.let { it to score }
        }.toMap()
    }

    private fun cascadeExposeBlocks(centerLocation: Location) {
        val processedBlocks = mutableSetOf<Location>()
        val toProcess = mutableListOf<Location>()
        toProcess.add(centerLocation)

        val dataManager = plugin.javaClass.getDeclaredField("dataManager").let { field ->
            field.isAccessible = true
            field.get(plugin) as DataManager
        }

        val cachedBlocks = dataManager.getCachedBlocks().toMutableMap()
        var removedCount = 0

        while (toProcess.isNotEmpty()) {
            val currentLocation = toProcess.removeAt(0)

            if (processedBlocks.contains(currentLocation)) continue
            processedBlocks.add(currentLocation)

            // 주변 26방향 블록 체크 (3x3x3에서 중심 제외)
            for (dx in -1..1) {
                for (dy in -1..1) {
                    for (dz in -1..1) {
                        if (dx == 0 && dy == 0 && dz == 0) continue

                        val neighborLocation = currentLocation.clone().add(dx.toDouble(), dy.toDouble(), dz.toDouble())

                        // 이미 처리했거나 캐시에 없는 블록은 건너뛰기
                        if (processedBlocks.contains(neighborLocation) ||
                            !cachedBlocks.containsKey(neighborLocation)) {
                            continue
                        }

                        // 블록이 이제 노출되었는지 확인
                        if (isBlockNowExposed(neighborLocation, cachedBlocks.keys)) {
                            // 캐시에서 제거
                            cachedBlocks.remove(neighborLocation)
                            dataManager.removeBlock(neighborLocation)
                            removedCount++

                            // 연쇄 처리를 위해 큐에 추가
                            toProcess.add(neighborLocation)

                            plugin.logger.info("[PieRay-Cascade] ${neighborLocation}의 블록이 노출되어 제거되었습니다.")
                        }
                    }
                }
            }
        }

        if (removedCount > 0) {
            plugin.logger.info("[PieRay-Cascade] 총 ${removedCount}개의 블록이 연쇄적으로 노출되어 제거되었습니다.")
        }
    }

    private fun isExposingMaterial(material: Material): Boolean {
        return when (material) {
            Material.AIR -> true
            Material.WATER -> true
            Material.LAVA -> true
            else -> {
                val materialName = material.name
                materialName.contains("WATER") ||
                        materialName.contains("LAVA") ||
                        materialName == "CAVE_AIR" ||
                        materialName == "VOID_AIR"
            }
        }
    }

    private fun isBlockNowExposed(location: Location, remainingBlocks: Set<Location>): Boolean {
        val directions = arrayOf(
            Vector(1, 0, 0), Vector(-1, 0, 0),
            Vector(0, 1, 0), Vector(0, -1, 0),
            Vector(0, 0, 1), Vector(0, 0, -1)
        )

        var exposedSides = 0
        val targetMaterials = configManager.getTargetBlocks()

        for (direction in directions) {
            val adjacentLocation = location.clone().add(direction)
            val adjacentBlock = adjacentLocation.block

            // 인접한 위치가 공기, 물, 용암이거나 등록된 블록이 아닌 경우 노출된 것으로 간주
            if (isExposingMaterial(adjacentBlock.type) ||
                (!remainingBlocks.contains(adjacentLocation) && !targetMaterials.contains(adjacentBlock.type))) {
                exposedSides++
            }
        }

        // 설정 가능한 노출 기준
        val maxExposedSides = configManager.getMaxExposedSides()
        return exposedSides >= maxExposedSides
    }
}

class PieRayListener(private val plugin: PieRay) : org.bukkit.event.Listener {
    private val suspicionManager: SuspicionManager = plugin.javaClass.getDeclaredField("suspicionManager").let { field ->
        field.isAccessible = true
        field.get(plugin) as SuspicionManager
    }

    @org.bukkit.event.EventHandler
    fun onBlockBreak(event: org.bukkit.event.block.BlockBreakEvent) {
        suspicionManager.onBlockBreak(event.player, event.block.location, event.block.type)
    }
}

class VersionHandler {
    private val version = getServerVersion()

    private fun getServerVersion(): String {
        return Bukkit.getVersion()
    }

    fun isVersionSupported(): Boolean {
        return version.contains("1.16") || version.contains("1.17") || version.contains("1.18") ||
                version.contains("1.19") || version.contains("1.20") || version.contains("1.21")
    }

    fun loadVersionSpecificClasses() {
        when {
            version.contains("1.21") -> loadV121classes()
            version.contains("1.20") -> loadV120classes()
            version.contains("1.19") -> loadV119classes()
            version.contains("1.18") -> loadV118Classes()
            version.contains("1.17") -> loadV117Classes()
            version.contains("1.16") -> loadV116Classes()
            else -> throw UnsupportedOperationException("지원하지 않는 버전입니다: $version")
        }
    }

    private fun loadV121classes() {
    }

    private fun loadV120classes() {
    }

    private fun loadV119classes() {
    }

    private fun loadV118Classes() {
    }

    private fun loadV117Classes() {
    }

    private fun loadV116Classes() {
    }
}