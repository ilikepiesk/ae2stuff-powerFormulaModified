/*
 * Copyright (c) bdew, 2014 - 2015
 * https://github.com/bdew/ae2stuff
 *
 * This mod is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://bdew.net/minecraft-mod-public-license/
 */

package net.bdew.ae2stuff.items

import java.util

import appeng.api.config.SecurityPermissions
import appeng.api.exceptions.FailedConnection
import net.bdew.ae2stuff.grid.Security
import net.bdew.ae2stuff.machines.wireless.{BlockWireless, TileWireless}
import net.bdew.ae2stuff.misc.ItemLocationStore
import net.bdew.lib.Misc
import net.bdew.lib.PimpVanilla._
import net.bdew.lib.items.BaseItem
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.{ActionResult, EnumActionResult, EnumFacing, EnumHand}
import net.minecraft.world.World

object ItemWirelessKit extends BaseItem("wireless_kit") with ItemLocationStore {
  setMaxStackSize(1)

  def checkSecurity(t1: TileWireless, t2: TileWireless, p: EntityPlayer) = {
    val pid = Security.getPlayerId(p)
    Security.playerHasPermission(t1.getNode.getGrid, pid, SecurityPermissions.BUILD) &&
      Security.playerHasPermission(t2.getNode.getGrid, pid, SecurityPermissions.BUILD)
  }

  override def onItemRightClick(world: World, player: EntityPlayer, hand: EnumHand): ActionResult[ItemStack] = {
    val stack = player.getHeldItem(hand)
    if (player.isSneaking && !world.isRemote) {
      val copy = stack.copy()
      clearLocation(copy)
      ActionResult.newResult(EnumActionResult.SUCCESS, copy)
    } else ActionResult.newResult(EnumActionResult.PASS, stack)
  }

  override def onItemUse(player: EntityPlayer, world: World, pos: BlockPos, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): EnumActionResult = {
    import net.bdew.lib.helpers.ChatHelper._
    val stack = player.getHeldItem(hand)
    if (world.getBlockState(pos).getBlock != BlockWireless) return EnumActionResult.PASS
    if (!world.isRemote) {
      world.getTileSafe[TileWireless](pos) foreach { tile =>
        val pid = Security.getPlayerId(player)
        // Check that the player can modify the network
        if (!Security.playerHasPermission(tile.getNode.getGrid, pid, SecurityPermissions.BUILD)) {
          player.sendStatusMessage(L("ae2stuff.wireless.tool.security.player").setColor(Color.RED), true)
        } else {
          getLocation(stack) match {
            case Some(otherLoc) =>
              // Have other location - start connecting
              if (otherLoc.dim != world.provider.getDimension) {
                // Different dimensions - error out
                player.sendStatusMessage(L("ae2stuff.wireless.tool.dimension").setColor(Color.RED), true)
                clearLocation(stack)
              } else if (pos == otherLoc.pos) {
                // Same block - clear the location
                clearLocation(stack)
              } else {
                world.getTileSafe[TileWireless](otherLoc.pos) match {
                  // Check that the other tile is still around
                  case Some(other: TileWireless) =>
                    // And check that the player can modify it too
                    if (!Security.playerHasPermission(other.getNode.getGrid, pid, SecurityPermissions.BUILD)) {
                      player.sendStatusMessage(L("ae2stuff.wireless.tool.security.player").setColor(Color.RED), true)
                    } else {
                      // Player can modify both sides - unlink current connections if any
                      tile.doUnlink()
                      other.doUnlink()

                      // Make player the owner of both blocks
                      tile.getNode.setPlayerID(pid)
                      other.getNode.setPlayerID(pid)
                      try {
                        if (tile.doLink(other)) {
                          player.sendStatusMessage(L("ae2stuff.wireless.tool.connected", pos.getX.toString, pos.getY.toString, pos.getZ.toString).setColor(Color.GREEN), true)
                        } else {
                          player.sendStatusMessage(L("ae2stuff.wireless.tool.failed").setColor(Color.RED), true)
                        }
                      } catch {
                        case e: FailedConnection =>
                          player.sendMessage((L("ae2stuff.wireless.tool.failed") & ": " & e.getMessage).setColor(Color.RED))
                          tile.doUnlink()
                      }
                    }
                    clearLocation(stack)
                  case _ =>
                    // The other block is gone - error out
                    player.sendStatusMessage(L("ae2stuff.wireless.tool.noexist").setColor(Color.RED), true)
                    clearLocation(stack)
                }
              }
            case None =>
              // Have no location stored - store current location
              player.sendStatusMessage(L("ae2stuff.wireless.tool.bound1", pos.getX.toString, pos.getY.toString, pos.getZ.toString).setColor(Color.GREEN), true)
              setLocation(stack, pos, world.provider.getDimension)
          }
        }
      }
    }
    EnumActionResult.SUCCESS
  }

  override def addInformation(stack: ItemStack, worldIn: World, tooltip: util.List[String], flagIn: ITooltipFlag) = {
    super.addInformation(stack, worldIn, tooltip, flagIn)
    getLocation(stack) match {
      case Some(loc) =>
        tooltip.add(Misc.toLocalF("ae2stuff.wireless.tool.bound1", loc.pos.getX, loc.pos.getY, loc.pos.getZ))
        tooltip.add(Misc.toLocal("ae2stuff.wireless.tool.bound2"))
      case None =>
        tooltip.add(Misc.toLocal("ae2stuff.wireless.tool.empty"))
    }
  }
}
