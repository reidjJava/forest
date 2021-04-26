package me.func.forest.user.listener.prepare

import clepto.cristalix.Cristalix
import me.func.forest.user.User
import org.bukkit.Bukkit
import java.util.*

class SetupScoreBoard : PrepareUser{
    override fun execute(user: User) {
        val address = UUID.randomUUID().toString()
        val objective = Cristalix.scoreboardService().getPlayerObjective(user.uuid, address)

        objective.displayName = "Выжить в тайге"

        objective.startGroup("Игрок")
            .record("Уровень") { "§b${user.stat?.level} §fур."}
            .record("Жизней") {
                "§c${"❤".repeat(user.stat!!.heart)}§7${"❤".repeat(user.stat!!.maxHeart - user.stat!!.heart)}"}
            .record("Дней") { "§a${(user.stat!!.timeAlive / 1000 / 3600 / 24).toInt()}"}

        objective.startGroup("Тайга")
            .record("Погода") { "§eЯсная 25°С"}
            .record("Население") { "§b${Bukkit.getOnlinePlayers().size}"}

        Cristalix.scoreboardService().setCurrentObjective(user.uuid, address)
    }
}