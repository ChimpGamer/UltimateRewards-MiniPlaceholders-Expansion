package nl.chimpgamer.ultimaterewards.miniplaceholders.expansion

import eu.athelion.ultimaterewards.api.UltimateReward
import eu.athelion.ultimaterewards.backend.DataHandler
import eu.athelion.ultimaterewards.backend.user.UserHandler
import eu.athelion.ultimaterewards.backend.user.setting.SettingType
import eu.athelion.ultimaterewards.configuration.file.ConfigGlobal
import eu.athelion.ultimaterewards.configuration.file.Lang
import eu.athelion.ultimaterewards.reward.ProgressiveReward
import eu.athelion.ultimaterewards.reward.RewardType
import eu.athelion.ultimaterewards.reward.RewardWithTimer
import eu.athelion.ultimaterewards.reward.type.StreakFixedReward
import eu.athelion.ultimaterewards.reward.type.StreakReward
import eu.athelion.ultimaterewards.reward.type.WorldAfkReward
import eu.athelion.ultimaterewards.util.PlayerUtil
import eu.athelion.ultimaterewards.util.TextUtil
import io.github.miniplaceholders.api.Expansion
import net.kyori.adventure.text.minimessage.tag.Tag.preProcessParsed
import net.kyori.adventure.text.minimessage.tag.Tag.selfClosingInserting
import nl.chimpgamer.ultimaterewards.miniplaceholders.expansion.extensions.parsePossibleLegacy
import nl.chimpgamer.ultimaterewards.miniplaceholders.expansion.extensions.toComponent
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import kotlin.jvm.optionals.getOrNull
import kotlin.math.abs

class UltimateRewardsMiniPlaceholdersExpansion : JavaPlugin() {

    lateinit var expansion: Expansion

    override fun onEnable() {
        // Plugin startup logic
        val builder = Expansion.builder("ultimaterewards")
            .filter(Player::class.java)
            .audiencePlaceholder("available") { audience, argumentQueue, _ ->
                audience as Player
                val user = UserHandler.getFrom(audience)
                    ?: return@audiencePlaceholder selfClosingInserting(Lang.LOADING.asColoredString().parsePossibleLegacy())

                val availableRewards = if (argumentQueue.hasNext()) {
                    val rewardTypeStr = argumentQueue.pop().value()
                    val rewardType = RewardType.findByName(rewardTypeStr)
                        ?: return@audiencePlaceholder preProcessParsed("Reward type $rewardTypeStr does not exist!")

                    user.getAvailableRewards(rewardType).size
                } else {
                    user.availableRewards.size
                }

                selfClosingInserting(
                    if (availableRewards == 0) Lang.REWARDS_AVAILABLE_PLACEHOLDER_ZERO.asColoredString()
                        .parsePossibleLegacy() else availableRewards.toComponent()
                )
            }
            .audiencePlaceholder("collected") { audience, argumentQueue, _ ->
                audience as Player
                val rewardIdentifier = argumentQueue.popOr("reward identifier argument required!").value()
                val reward = UltimateReward.getRewardByName(rewardIdentifier).getOrNull()
                    ?: return@audiencePlaceholder preProcessParsed("Reward $rewardIdentifier doesn't exist!")
                val user = UserHandler.getFrom(audience)
                    ?: return@audiencePlaceholder selfClosingInserting(Lang.LOADING.asColoredString().parsePossibleLegacy())

                selfClosingInserting(user.getInteger(reward.rewardIdentifier + "Collected").toComponent())
            }
            .audiencePlaceholder("current_streak") { audience, argumentQueue, _ ->
                audience as Player
                val rewardIdentifier = argumentQueue.popOr("reward identifier argument required!").value()
                val reward = UltimateReward.getRewardByName(rewardIdentifier).getOrNull()
                    ?: return@audiencePlaceholder preProcessParsed("Reward $rewardIdentifier doesn't exist!")
                if (reward !is StreakReward) return@audiencePlaceholder preProcessParsed("Reward $rewardIdentifier is not a streak reward!")
                if (!audience.hasPermission(reward.permission)) return@audiencePlaceholder selfClosingInserting(Lang.NO_PERMISSION.asColoredString().parsePossibleLegacy())

                val user = UserHandler.getFrom(audience)
                    ?: return@audiencePlaceholder selfClosingInserting(Lang.LOADING.asColoredString().parsePossibleLegacy())

                selfClosingInserting(user.getInteger(reward.getRewardIdentifier() + "CurrentStreak").toComponent())
            }
            .audiencePlaceholder("afk_session_time") { audience, _, _ ->
                audience as Player
                val user = UserHandler.getFrom(audience)
                    ?: return@audiencePlaceholder selfClosingInserting(Lang.LOADING.asColoredString().parsePossibleLegacy())
                selfClosingInserting(
                    TextUtil.getFormat(
                        ConfigGlobal.AFK_TIME_PLACEHOLDER_FORMAT.asString(),
                        user.getData("afkTime").toString().toLong()
                    ).parsePossibleLegacy()
                )
            }
            .audiencePlaceholder("playtime_global") { audience, _, _ ->
                if (DataHandler.isUsingSQLite()) return@audiencePlaceholder preProcessParsed("For this placeholder MySQL is required")
                audience as Player
                selfClosingInserting(
                    TextUtil.getTimeFormat(
                        PlayerUtil.getPlayersNetworkPlayTimeInMinutes(audience),
                        ConfigGlobal.PLAY_TIME_PLACEHOLDER_FORMAT.asString()
                    ).parsePossibleLegacy()
                )
            }
            .audiencePlaceholder("playtime_local_in_hours") { audience, _, _ ->
                audience as Player

                selfClosingInserting(
                    PlayerUtil.getPlayersPlayTimeInHours(audience).toString().parsePossibleLegacy()
                )
            }
            .audiencePlaceholder("playtime_local_in_minutes") { audience, _, _ ->
                audience as Player

                selfClosingInserting(
                    PlayerUtil.getPlayersPlayTimeInMinutes(audience).toString().parsePossibleLegacy()
                )
            }
            .audiencePlaceholder("playtime_local") { audience, argumentQueue, _ ->
                audience as Player
                val format = if (argumentQueue.hasNext()) {
                    argumentQueue.pop().value()
                } else {
                    ConfigGlobal.PLAY_TIME_PLACEHOLDER_FORMAT.asString()
                }

                selfClosingInserting(
                    TextUtil.getTimeFormat(
                        PlayerUtil.getPlayersPlayTimeInMinutes(audience).toLong(),
                        format
                    ).parsePossibleLegacy()
                )
            }
            .audiencePlaceholder("setting_enabled") { audience, argumentQueue, _ ->
                audience as Player
                val settingStr = argumentQueue.popOr("setting argument is required!").value()
                val settingType = runCatching { SettingType.valueOf(settingStr.uppercase()) }.getOrNull()
                    ?: return@audiencePlaceholder selfClosingInserting(Lang.INVALID_SETTING_TYPE.asColoredString().parsePossibleLegacy())
                val user = UserHandler.getFrom(audience)
                    ?: return@audiencePlaceholder selfClosingInserting(Lang.LOADING.asColoredString().parsePossibleLegacy())

                selfClosingInserting(user.hasToggledSetting(settingType).toComponent())
            }
            .audiencePlaceholder("playtime_session") { audience, _, _ ->
                audience as Player
                val user = UserHandler.getFrom(audience)
                    ?: return@audiencePlaceholder selfClosingInserting(Lang.LOADING.asColoredString().parsePossibleLegacy())
                selfClosingInserting(
                    TextUtil.getTimeFormat(
                        (PlayerUtil.getPlayersPlayTimeInMinutes(audience).toLong() - user.getData("session").toString()
                            .toLong()),
                        ConfigGlobal.PLAY_TIME_PLACEHOLDER_FORMAT.asString()
                    ).toComponent()
                )
            }
            .audiencePlaceholder("referrals") { audience, _, _ ->
                audience as Player
                val user = UserHandler.getFrom(audience)
                    ?: return@audiencePlaceholder selfClosingInserting(Lang.LOADING.asColoredString().parsePossibleLegacy())

                val result = when (val uses = user.getLong("uses")) {
                    0L -> Lang.REFERRALS_USES_PLACEHOLDER_ZERO.asColoredString()
                    -1L -> Lang.REFERRALS_USES_PLACEHOLDER_NOT_CREATED.asColoredString()
                    else -> uses.toString()
                }

                selfClosingInserting(result.parsePossibleLegacy())
            }
            .audiencePlaceholder("cooldown") { audience, argumentQueue, _ ->
                audience as Player
                val rewardIdentifier = argumentQueue.popOr("reward identifier argument required!").value()
                val reward = UltimateReward.getRewardByName(rewardIdentifier).getOrNull()
                    ?: return@audiencePlaceholder preProcessParsed("Reward $rewardIdentifier doesn't exist!")
                val user = UserHandler.getFrom(audience)
                    ?: return@audiencePlaceholder selfClosingInserting(Lang.LOADING.asColoredString().parsePossibleLegacy())

                if (reward is StreakFixedReward) {
                    if (reward.getAvailability(user, null).isClaimable) {
                        return@audiencePlaceholder selfClosingInserting(
                            Lang.CLAIMABLE.asColoredString().parsePossibleLegacy()
                        )
                    }
                    selfClosingInserting(
                        TextUtil.getFormat(
                            reward.cooldownFormat,
                            reward.duration.get().toMillis()
                        ).toComponent())
                } else if (reward is RewardWithTimer) {
                    if (!audience.hasPermission(reward.permission)) {
                        return@audiencePlaceholder selfClosingInserting(Lang.NO_PERMISSION.asColoredString().parsePossibleLegacy())
                    }
                    val timeLeft = reward.getCooldown(user)?.timeLeftInMillis ?: 0
                    val result = if (timeLeft <= 0) {
                        Lang.CLAIMABLE.asColoredString()
                    } else {
                        TextUtil.getFormat(reward.cooldownFormat, timeLeft)
                    }
                    selfClosingInserting(result.parsePossibleLegacy())
                } else if (reward is WorldAfkReward) {
                    val afkTime = user.getData("afkTime").toString().toLong()
                    val requiredTime = (reward.requiredTimeInMinutes.toLong() * 60L * 1000L)
                    val multiplier = (afkTime / requiredTime).toInt() + 1
                    selfClosingInserting(
                        TextUtil.getFormat(
                            ConfigGlobal.AFK_TIME_PLACEHOLDER_FORMAT.asString(),
                            abs((afkTime - (requiredTime * multiplier)))
                        ).toComponent()
                    )
                } else {
                    preProcessParsed("Reward doesn't have a timer!")
                }
            }
            .audiencePlaceholder("progress") { audience, argumentQueue, _ ->
                audience as Player
                val rewardIdentifier = argumentQueue.popOr("reward identifier argument required!").value()
                val reward = UltimateReward.getRewardByName(rewardIdentifier).getOrNull()
                    ?: return@audiencePlaceholder preProcessParsed("Reward $rewardIdentifier doesn't exist!")
                val user = UserHandler.getFrom(audience)
                    ?: return@audiencePlaceholder selfClosingInserting(Lang.LOADING.asColoredString().parsePossibleLegacy())

                if (reward is ProgressiveReward) {
                    if (reward.getAvailability(user, null).isClaimable) {
                        return@audiencePlaceholder selfClosingInserting(
                            Lang.CLAIMABLE.asColoredString().parsePossibleLegacy()
                        )
                    }
                    selfClosingInserting(TextUtil.getProgressBar(reward.getProgressPercentage(user)).parsePossibleLegacy())
                } else {
                    preProcessParsed("Reward doesn't have a timer!")
                }
            }
            .audiencePlaceholder("state") { audience, argumentQueue, _ ->
                audience as Player
                val rewardIdentifier = argumentQueue.popOr("reward identifier argument required!").value()
                val reward = UltimateReward.getRewardByName(rewardIdentifier).getOrNull()
                    ?: return@audiencePlaceholder preProcessParsed("Reward $rewardIdentifier doesn't exist!")
                val user = UserHandler.getFrom(audience)
                    ?: return@audiencePlaceholder selfClosingInserting(Lang.LOADING.asColoredString().parsePossibleLegacy())

                selfClosingInserting(reward.getAvailability(user).name.toComponent())
            }
            .audiencePlaceholder("total_collected") { audience, _, _ ->
                audience as Player
                selfClosingInserting(
                    UserHandler.getFrom(audience)?.collectedRewards?.toComponent() ?: Lang.LOADING.asColoredString()
                        .parsePossibleLegacy()
                )
            }
            .audiencePlaceholder("total_votes") { audience, _, _ ->
                audience as Player
                selfClosingInserting(
                    UserHandler.getFrom(audience)?.votes?.toComponent() ?: Lang.LOADING.asColoredString().parsePossibleLegacy()
                )
            }

        expansion = builder.build()
            .also { it.register() }
    }

    override fun onDisable() {
        // Plugin shutdown logic

        if (this::expansion.isInitialized && expansion.registered()) {
            expansion.unregister()
        }
    }
}
