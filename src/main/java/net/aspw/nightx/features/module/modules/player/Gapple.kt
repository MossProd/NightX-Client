package net.aspw.nightx.features.module.modules.player

import net.aspw.nightx.NightX
import net.aspw.nightx.event.EventTarget
import net.aspw.nightx.event.UpdateEvent
import net.aspw.nightx.features.module.Module
import net.aspw.nightx.features.module.ModuleCategory
import net.aspw.nightx.features.module.ModuleInfo
import net.aspw.nightx.utils.InventoryUtils
import net.aspw.nightx.utils.timer.MSTimer
import net.aspw.nightx.value.BoolValue
import net.aspw.nightx.value.FloatValue
import net.aspw.nightx.value.IntegerValue
import net.aspw.nightx.value.ListValue
import net.aspw.nightx.visual.hud.element.elements.Notification
import net.minecraft.init.Items
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import java.util.*

@ModuleInfo(name = "Gapple", category = ModuleCategory.PLAYER)
class Gapple : Module() {
    val modeValue = ListValue("Mode", arrayOf("Auto", "Once", "Head"), "Once")

    // Auto Mode
    private val healthValue = FloatValue("Health", 10F, 1F, 20F)
    private val delayValue = IntegerValue("Delay", 150, 0, 1000, "ms")
    private val noAbsorption = BoolValue("NoAbsorption", true)
    private val timer = MSTimer()

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {
        when (modeValue.get().lowercase(Locale.getDefault())) {
            "once" -> {
                doEat(true)
                state = false
            }

            "auto" -> {
                if (!timer.hasTimePassed(delayValue.get().toLong()))
                    return
                if (mc.thePlayer.health <= healthValue.get()) {
                    doEat(false)
                    timer.reset()
                }
            }

            "head" -> {
                if (!timer.hasTimePassed(delayValue.get().toLong()))
                    return
                if (mc.thePlayer.health <= healthValue.get()) {
                    val headInHotbar = InventoryUtils.findItem(36, 45, Items.skull)
                    if (headInHotbar != -1) {
                        mc.netHandler.addToSendQueue(C09PacketHeldItemChange(headInHotbar - 36))
                        mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
                        mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                        timer.reset()
                    }
                }
            }
        }
    }

    private fun doEat(warn: Boolean) {
        if (noAbsorption.get() && !warn) {
            val abAmount = mc.thePlayer.absorptionAmount
            if (abAmount > 0)
                return
        }

        val gappleInHotbar = InventoryUtils.findItem(36, 45, Items.golden_apple)
        if (gappleInHotbar != -1) {
            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(gappleInHotbar - 36))
            mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
            repeat(35) {
                mc.netHandler.addToSendQueue(C03PacketPlayer(mc.thePlayer.onGround))
            }
            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
        } else if (warn)
            NightX.hud.addNotification(Notification("No Gapple were found in hotbar.", Notification.Type.ERROR))
    }

    override val tag: String
        get() = modeValue.get()
}