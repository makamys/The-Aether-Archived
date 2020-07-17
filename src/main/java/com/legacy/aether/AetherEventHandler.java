package com.legacy.aether;

import com.legacy.aether.advancements.AetherAdvancements;
import com.legacy.aether.blocks.BlocksAether;
import com.legacy.aether.blocks.portal.BlockAetherPortal;
import com.legacy.aether.entities.AetherEntities;
import com.legacy.aether.entities.bosses.EntityValkyrie;
import com.legacy.aether.entities.passive.mountable.EntityAerbunny;
import com.legacy.aether.entities.passive.mountable.EntityFlyingCow;
import com.legacy.aether.entities.projectile.darts.EntityDartBase;
import com.legacy.aether.items.ItemsAether;
import com.legacy.aether.items.accessories.ItemAccessoryDyable;
import com.legacy.aether.items.dungeon.ItemDungeonKey;
import com.legacy.aether.items.tools.ItemSkyrootBucket;
import com.legacy.aether.items.util.EnumSkyrootBucketType;
import com.legacy.aether.items.weapons.ItemSkyrootSword;

import com.legacy.aether.world.AetherWorld;
import com.legacy.aether.world.AetherWorldProvider;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatList;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;

import java.util.Random;

import static net.minecraft.block.BlockCauldron.LEVEL;

public class AetherEventHandler 
{

	@SubscribeEvent
	public void checkBlockBannedEvent(RightClickBlock event)
	{
		EntityPlayer player = event.getEntityPlayer();
		ItemStack currentStack = event.getItemStack();

		if (player.dimension == AetherConfig.dimension.aether_dimension_id)
		{
			if (currentStack.getItem() == Items.FLINT_AND_STEEL || currentStack.getItem() == Item.getItemFromBlock(Blocks.TORCH) || currentStack.getItem() == Items.FIRE_CHARGE)
			{
				for (int i = 0; i < 10; ++i)
				{
					event.getWorld().spawnParticle(EnumParticleTypes.SMOKE_LARGE, event.getHitVec().x, event.getHitVec().y, event.getHitVec().z, 0.0D, 0.0D, 0.0D, new int [] {});
				}

				event.setCanceled(true);

			}
			else if (event.getWorld().getBlockState(event.getPos()).getBlock() == Blocks.BED)
			{
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public void onMilkedCow(EntityInteractSpecific event)
	{
		if (event.getTarget() instanceof EntityCow || event.getTarget() instanceof EntityFlyingCow)
		{
			EntityPlayer player = event.getEntityPlayer();
			ItemStack heldItem = player.getHeldItem(event.getHand());

			if (heldItem.getItem() == ItemsAether.skyroot_bucket && EnumSkyrootBucketType.getType(heldItem.getMetadata()) == EnumSkyrootBucketType.Empty)
			{
				heldItem.shrink(1);

	            if (heldItem.isEmpty())
	            {
	            	event.getEntityPlayer().playSound(SoundEvents.ENTITY_COW_MILK, 1.0F, 1.0F);
	            	
	                player.setHeldItem(event.getHand(), new ItemStack(ItemsAether.skyroot_bucket, 1, EnumSkyrootBucketType.Milk.meta));
	            }
	            else if (!player.inventory.addItemStackToInventory(new ItemStack(ItemsAether.skyroot_bucket, 1, EnumSkyrootBucketType.Milk.meta)))
	            {
	                player.dropItem(new ItemStack(ItemsAether.skyroot_bucket, 1, EnumSkyrootBucketType.Milk.meta), false);
	            }
			}
		}
	}

	@SubscribeEvent
	public void onFillBucket(FillBucketEvent event)
	{
		World worldObj = event.getWorld();
		RayTraceResult target = event.getTarget();
		ItemStack stack = event.getEmptyBucket();
		EntityPlayer player = event.getEntityPlayer();

		boolean isWater = (!AetherConfig.gameplay_changes.skyroot_bucket_only && stack.getItem() == Items.WATER_BUCKET) || stack.getItem() == ItemsAether.skyroot_bucket && stack.getMetadata() == 1;
		boolean isLava = stack.getItem() == Items.LAVA_BUCKET;

		boolean validDimension = (player.dimension == 0 || player.dimension == AetherConfig.dimension.aether_dimension_id);

		if (target != null && target.typeOfHit == Type.BLOCK && validDimension)
		{
			BlockPos hitPos = target.getBlockPos().offset(target.sideHit);

			if (isWater)
			{
				if (((BlockAetherPortal) BlocksAether.aether_portal).trySpawnPortal(worldObj, hitPos))
				{
					event.getEntityPlayer().playSound(SoundEvents.ENTITY_GENERIC_SWIM, 1.0F, 1.0F);
					
					if (!player.capabilities.isCreativeMode)
					{
						if (stack.getItem() == ItemsAether.skyroot_bucket || stack.getItemDamage() == 1)
						{
							event.setFilledBucket(new ItemStack(ItemsAether.skyroot_bucket));
						}

						if (stack.getItem() == Items.WATER_BUCKET)
						{
							event.setFilledBucket(new ItemStack(Items.BUCKET));
						}
					}

					event.setResult(Result.ALLOW);
				}
			}

			if (isLava && player.dimension == AetherConfig.dimension.aether_dimension_id)
			{
				if (player.capabilities.isCreativeMode && player.isSneaking())
				{
					return;
				}

				if (worldObj.isAirBlock(hitPos))
				{
					worldObj.spawnParticle(EnumParticleTypes.SMOKE_LARGE, hitPos.getX() + 0.5, hitPos.getY() + 1, hitPos.getZ() + 0.5, 0, 0, 0);
					event.getEntityPlayer().playSound(SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, 1.0F, 1.0F);
					
					worldObj.setBlockState(hitPos, BlocksAether.aerogel.getDefaultState());

					if (!player.capabilities.isCreativeMode)
					{
						event.setFilledBucket(new ItemStack(Items.BUCKET));
					}
				}

				event.setResult(Result.ALLOW);
			}
		}
	}

	@SubscribeEvent
	public void onCrafting(ItemCraftedEvent event)
	{
		if (event.player instanceof EntityPlayerMP)
		{
			if (this.isGravititeTool(event.crafting.getItem()))
			{
				AetherAdvancements.GRAV_TOOLSET_TRIGGER.trigger((EntityPlayerMP) event.player);
			}

			AetherAdvancements.CRAFT_ITEM_TRIGGER.trigger((EntityPlayerMP) event.player, event.crafting);
		}
	}

	@SubscribeEvent
	public void onEntityDropLoot(LivingDropsEvent event)
	{
		if (event.getSource() instanceof EntityDamageSource)
		{
			EntityLivingBase entity = event.getEntityLiving();
			EntityDamageSource source = (EntityDamageSource) event.getSource();

			if (source.getImmediateSource() instanceof EntityPlayer)
			{
				EntityPlayer player = (EntityPlayer) source.getImmediateSource();
				ItemStack currentItem = player.inventory.getCurrentItem();

				if (currentItem.getItem() instanceof ItemSkyrootSword && !(entity instanceof EntityPlayer) && !(entity instanceof EntityWither) && !(entity instanceof EntityValkyrie))
				{
					for (EntityItem items : event.getDrops())
					{
						ItemStack stack = items.getItem();

						if (!(stack.getItem() instanceof ItemDungeonKey) && stack.getItem() != ItemsAether.victory_medal && stack.getItem() != Items.SKULL)
						{
							EntityItem item = new EntityItem(entity.world, entity.posX, entity.posY, entity.posZ, items.getItem());

							entity.world.spawnEntity(item);
						}
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void onEntityStruckByLightning(EntityStruckByLightningEvent event)
	{
		if (event.getEntity() instanceof EntityPlayer)
		{
			EntityPlayer player = (EntityPlayer) event.getEntity();

			if (player.inventory.getCurrentItem().getItem() == ItemsAether.lightning_sword || player.inventory.getCurrentItem().getItem() == ItemsAether.lightning_knife)
			{
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public void onEntityDamage(LivingAttackEvent event)
	{
		if (event.getEntityLiving() instanceof EntityAerbunny)
		{
			EntityAerbunny aerbunny = (EntityAerbunny) event.getEntityLiving();

			if (aerbunny.isRiding() && aerbunny.getRidingEntity() instanceof EntityPlayer)
			{
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public void onEntityAttack(AttackEntityEvent event)
	{
		if (event.getEntityPlayer().getHeldItemMainhand().getItem() == ItemsAether.flaming_sword)
		{
			if (event.getTarget().canBeAttackedWithItem())
			{
				if (!event.getTarget().hitByEntity(event.getEntityPlayer()))
				{
					if (event.getTarget() instanceof EntityLivingBase)
					{
						int defaultTime = 30;

						int fireAspectModifier = EnchantmentHelper.getFireAspectModifier(event.getEntityPlayer());

						if (fireAspectModifier > 0)
						{
							defaultTime += (fireAspectModifier * 4);
						}

						event.getTarget().setFire(defaultTime);
					}
				}
			}
		}
		else if (event.getEntityPlayer().getHeldItemMainhand().getItem() == ItemsAether.pig_slayer)
		{
			String s = EntityList.getEntityString(event.getTarget());

			if(s != null && (s.toLowerCase().contains("pig") || s.toLowerCase().contains("phyg") || s.toLowerCase().contains("taegore") || event.getTarget().getUniqueID().toString().equals("1d680bb6-2a9a-4f25-bf2f-a1af74361d69")))
			{
				if (event.getTarget().world.isRemote)
				{
					Random rand = new Random();

					for(int j = 0; j < 20; j++)
					{
						double d = rand.nextGaussian() * 0.02D;
						double d1 = rand.nextGaussian() * 0.02D;
						double d2 = rand.nextGaussian() * 0.02D;
						double d3 = 5D;
						event.getTarget().world.spawnParticle(EnumParticleTypes.FLAME, (event.getTarget().posX + (double)(rand.nextFloat() * event.getTarget().width * 2.0F)) - (double)event.getTarget().width - d * d3, (event.getTarget().posY + (double)(rand.nextFloat() * event.getTarget().height)) - d1 * d3, (event.getTarget().posZ + (double)(rand.nextFloat() * event.getTarget().width * 2.0F)) - (double)event.getTarget().width - d2 * d3, d, d1, d2);
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void onProjectileImpact(ProjectileImpactEvent event)
	{
		if (event.getEntity() instanceof EntityArrow)
		{
			EntityArrow arrow = (EntityArrow) event.getEntity();

			if (!arrow.world.isRemote)
			{
				if (!arrow.shootingEntity.getPassengers().isEmpty() && event.getRayTraceResult().entityHit == arrow.shootingEntity.getPassengers().get(0))
				{
					event.setCanceled(true);
				}
				else
				{
					if (event.getEntity() instanceof EntityDartBase)
					{
						EntityDartBase entityDartBase = (EntityDartBase) event.getEntity();

						if (event.getRayTraceResult().entityHit != entityDartBase.shootingEntity && event.getRayTraceResult().typeOfHit == Type.ENTITY)
						{
							entityDartBase.setDead();
						}
						else
						{
							entityDartBase.setNoGravity(true);
						}
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event)
	{
		if (event.getHand() == EnumHand.MAIN_HAND)
		{
			IBlockState block = event.getWorld().getBlockState(event.getPos());

			if (block.getBlock() == Blocks.CAULDRON)
			{
				BlockCauldron cauldron = (BlockCauldron) block.getBlock();

				int waterLevel = block.getValue(LEVEL);

				if (event.getItemStack().getItem() == ItemsAether.leather_gloves)
				{
					ItemAccessoryDyable gloves = (ItemAccessoryDyable) event.getItemStack().getItem();

					if (waterLevel > 0)
					{
						if (gloves.hasColor(event.getItemStack()) && !event.getWorld().isRemote)
						{
							gloves.removeColor(event.getItemStack());
							cauldron.setWaterLevel(event.getWorld(), event.getPos(), block, waterLevel - 1);
							event.getEntityPlayer().addStat(StatList.ARMOR_CLEANED);
						}
					}
				}

				if (event.getItemStack().getItem() == ItemsAether.skyroot_bucket)
				{
					ItemSkyrootBucket bucket = (ItemSkyrootBucket) event.getItemStack().getItem();

					if (EnumSkyrootBucketType.getType(bucket.getMetadata(event.getItemStack())) == EnumSkyrootBucketType.Water)
					{
						if (waterLevel < 3 && !event.getWorld().isRemote)
						{
							if (!event.getEntityPlayer().capabilities.isCreativeMode)
							{
								event.getEntityPlayer().setHeldItem(event.getHand(), new ItemStack(ItemsAether.skyroot_bucket, 1, EnumSkyrootBucketType.Empty.meta));
							}

							event.getEntityPlayer().addStat(StatList.CAULDRON_FILLED);
							cauldron.setWaterLevel(event.getWorld(), event.getPos(), block, 3);
							event.getEntityPlayer().world.playSound(null, event.getPos(), SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
						}
					}
					else if (EnumSkyrootBucketType.getType(bucket.getMetadata(event.getItemStack())) == EnumSkyrootBucketType.Empty)
					{
						if (waterLevel == 3 && !event.getWorld().isRemote)
						{
							if (!event.getEntityPlayer().capabilities.isCreativeMode)
							{
								event.getItemStack().shrink(1);

								if (event.getItemStack().isEmpty())
								{
									event.getEntityPlayer().setHeldItem(event.getHand(), new ItemStack(ItemsAether.skyroot_bucket, 1, EnumSkyrootBucketType.Water.meta));
								}
								else if (!event.getEntityPlayer().inventory.addItemStackToInventory(new ItemStack(ItemsAether.skyroot_bucket, 1, EnumSkyrootBucketType.Water.meta)))
								{
									event.getEntityPlayer().dropItem(new ItemStack(ItemsAether.skyroot_bucket, 1, EnumSkyrootBucketType.Water.meta), false);
								}
							}

							event.getEntityPlayer().addStat(StatList.CAULDRON_USED);
							cauldron.setWaterLevel(event.getWorld(), event.getPos(), block, 0);
							event.getWorld().playSound(null, event.getPos(), SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
						}
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void onPlayerSleepInBed(PlayerWakeUpEvent event)
	{
		final World world = event.getEntityPlayer().world;

		if (!world.isRemote && event.getEntityPlayer().dimension == AetherConfig.dimension.aether_dimension_id)
		{
			final MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

			final WorldServer worldServer = server.getWorld(0);

			if (worldServer.playerEntities.size() > 0)
			{
				if (worldServer.areAllPlayersAsleep())
				{
					performTimeSet(event, world, worldServer);
				}
			}
			else
			{
				performTimeSet(event, world, worldServer);
			}
		}
	}

	private void performTimeSet(PlayerWakeUpEvent event, World world, WorldServer worldServer)
	{
		if (world.getGameRules().getBoolean("doDaylightCycle") && event.getEntityPlayer().isPlayerFullyAsleep())
		{
			final long i = worldServer.getWorldInfo().getWorldTime() + 24000L;

			worldServer.getWorldInfo().setWorldTime(i - i % 24000L);
		}
	}

	public boolean isGravititeTool(Item stackID)
	{
		return stackID == ItemsAether.gravitite_shovel || stackID == ItemsAether.gravitite_axe || stackID == ItemsAether.gravitite_pickaxe;
	}

}