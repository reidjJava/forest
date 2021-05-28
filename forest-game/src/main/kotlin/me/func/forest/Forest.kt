package me.func.forest

import clepto.bukkit.B
import clepto.cristalix.WorldMeta
import dev.implario.bukkit.platform.Platforms
import dev.implario.platform.impl.darkpaper.PlatformDarkPaper
import me.func.forest.clock.GameTimer
import me.func.forest.craft.CraftManager
import me.func.forest.drop.ResourceManager
import me.func.forest.item.ItemList
import me.func.forest.item.ItemManager
import me.func.forest.user.Stat
import me.func.forest.user.User
import me.func.forest.user.listener.CancelEvents
import me.func.forest.user.listener.PlayerListener
import me.func.forest.weather.ZoneManager
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import ru.cristalix.core.CoreApi
import ru.cristalix.core.inventory.IInventoryService
import ru.cristalix.core.inventory.InventoryService
import ru.cristalix.core.math.V3
import ru.cristalix.core.realm.IRealmService
import ru.cristalix.core.realm.RealmStatus
import ru.cristalix.core.stats.IStatService
import ru.cristalix.core.stats.PlayerScope
import ru.cristalix.core.stats.UserManager
import ru.cristalix.core.stats.impl.StatService
import ru.cristalix.core.stats.impl.network.StatServiceConnectionData
import ru.cristalix.npcs.server.Npcs
import java.util.*


lateinit var app: Forest

class Forest : JavaPlugin() {

    private val statScope = PlayerScope("forest", Stat::class.java)

    lateinit var worldMeta: WorldMeta
    lateinit var userManager: UserManager<User>

    lateinit var spawn: Location
    lateinit var start: Location

    override fun onEnable() {
        B.plugin = this
        app = this
        Platforms.set(PlatformDarkPaper())
        Npcs.init(this)

        // Загрузка карты
        worldMeta = MapLoader().load("prod")!!
        spawn = worldMeta.getLabel("guide_end")
        start = worldMeta.getLabel("guide_pre")

        // Конфигурация реалма
        val info = IRealmService.get().currentRealmInfo
        info.status = RealmStatus.GAME_STARTED_CAN_JOIN
        info.readableName = "Лес"
        info.groupName = "Лес"

        // Регистрация сервисов
        val core = CoreApi.get()

        CoreApi.get().registerService(IInventoryService::class.java, InventoryService())
        val statService = StatService(core.platformServer, StatServiceConnectionData.fromEnvironment())
        core.registerService(IStatService::class.java, statService)

        statService.useScopes(statScope)

        userManager = statService.registerUserManager(
            {
                val user = User(it.uuid, it.name, it.getData(statScope))
                user.stat!!.lastEntry = Date().time
                user
            },
            { user: User, context ->
                user.ifTent {
                    user.stat!!.placeInventory?.clear()
                    val items = mutableMapOf<ItemList, Int>()

                    user.homeInventory.forEach {
                        val nms = CraftItemStack.asNMSCopy(it)
                        if (nms.tag!= null && nms.tag.hasKey("code")) {
                            val type = ItemList.valueOf(nms.tag.getString("code"))
                            if (items[type] != null)
                                items.replace(type, items[type]?.plus(it.amount) ?: 0)
                            else
                                items[type] = it.amount
                        }
                    }

                    user.stat!!.placeInventory = items.toList().toMutableList()
                    user.tent?.remove()
                }

                val dot = user.player.location
                user.stat!!.exit = V3(dot.x, dot.y, dot.z)

                user.stat!!.timeAlive += Date().time - user.stat!!.lastEntry
                context.store(statScope, user.stat)
            }
        )

        B.regCommand({ player, _ ->
            val mob = player.world.spawnEntity(player.location, EntityType.ZOMBIE)
            mob.customName = "1"
            mob.isCustomNameVisible = true
            null
        }, "f", "")

        B.regCommand({ player, args ->
            getUser(player)?.giveExperience(args[0].toInt())
            null
        }, "exp", "")

        // Регистрация меню крафтов
        CraftManager()

        // Регистрация палаток
        TentManipulator()

        // Регистрация обработчиков событий
        B.events(PlayerListener(), CancelEvents(), ItemManager(), ResourceManager())

        // Начало игрового времени и добавление временных собитий
        GameTimer(listOf(ZoneManager())).runTaskTimer(this, 0, 1)
    }

    fun getUser(player: Player): User? {
        return userManager.getUser(player)
    }

    fun getWorld(): World {
        return worldMeta.world
    }

    fun getNMSWorld(): net.minecraft.server.v1_12_R1.World {
        return worldMeta.world.handle
    }
}