package org.winlogon.infohub.storage

import net.kyori.adventure.util.TriState
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.winlogon.infohub.ChoiceStorage
import java.util.UUID

class CombinedChoiceStorage(
    private val pdcStorage: ChoiceStorage,
    private val dbStorage: ChoiceStorage
) : ChoiceStorage {

    override fun getChoice(playerUuid: UUID): TriState {
        val pdcChoice = pdcStorage.getChoice(playerUuid)
        return if (pdcChoice != TriState.NOT_SET) {
            pdcChoice
        } else {
            dbStorage.getChoice(playerUuid)
        }
    }

    override fun setChoice(playerUuid: UUID, choice: Boolean) {
        pdcStorage.setChoice(playerUuid, choice)
        dbStorage.setChoice(playerUuid, choice)
    }
}
