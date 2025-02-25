package nl.chimpgamer.ultimaterewards.miniplaceholders.expansion.extensions

import io.github.miniplaceholders.api.utils.LegacyUtils
import net.kyori.adventure.text.Component

fun String.toComponent() = Component.text(this)
fun Int.toComponent() = Component.text(this)
fun Boolean.toComponent() = Component.text(this)

fun String.parsePossibleLegacy() = LegacyUtils.parsePossibleLegacy(this)