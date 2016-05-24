//  =====GPL=============================================================
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; version 2 dated June, 1991.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program;  if not, write to the Free Software
//  Foundation, Inc., 675 Mass Ave., Cambridge, MA 02139, USA.
//  =====================================================================
//
//
// Copyright © 2011-2015 Michael Sheppard (crackedEgg)
//
package com.parachute.common;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.*;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class EntityParachute extends Entity {
	
	private static final DataParameter<Integer> CHUTE_COLOR = EntityDataManager.<Integer>createKey(EntityParachute.class, DataSerializers.VARINT);
    private double velocityX;
    private double velocityY;
    private double velocityZ;
    private double motionFactor;
    private double maxAltitude;
    private boolean allowThermals;
    private boolean lavaThermals;
    private double lavaDistance;
    private double maxThermalRise;
    private double curLavaDistance;
    private boolean allowTurbulence;
    private boolean showContrails;
    private boolean autoDismount;
    private boolean dismountInWater;

    final static double drift = 0.004; // value applied to motionY to descend or drift downward
    final static double ascend = drift * -10.0; // -0.04 - value applied to motionY to ascend

    private static boolean ascendMode;

    public EntityParachute(World world) {
        super(world);
        allowTurbulence = ConfigHandler.getAllowturbulence();
        showContrails = ConfigHandler.getShowContrails();
        lavaDistance = ConfigHandler.getMinLavaDistance();
        allowThermals = ConfigHandler.getAllowThermals();
        maxAltitude = ConfigHandler.getMaxAltitude();
        lavaThermals = ConfigHandler.getAllowLavaThermals();
        autoDismount = ConfigHandler.isAutoDismount();
        dismountInWater = ConfigHandler.getDismountInWater();
        maxThermalRise = ConfigHandler.getMaxLavaDistance();

        curLavaDistance = lavaDistance;
        worldObj = world;
        preventEntitySpawning = true;
        setSize(1.5f, 0.0625f);
        motionFactor = 0.07;
        ascendMode = false;
        setSilent(false);
    }

    public EntityParachute(World world, double x, double y, double z) {
        this(world);
        setPosition(x, y, z);

        motionX = 0.0D;
        motionY = 0.0D;
        motionZ = 0.0D;

        prevPosX = x;
        prevPosY = y;
        prevPosZ = z;
    }

    static public void setAscendMode(boolean mode) {
        ascendMode = mode;
    }

    public void dismountParachute() {
        Entity skyDiver = getControllingPassenger();
        if (!worldObj.isRemote && skyDiver != null) {
            dismountRidingEntity();
            killParachute();
        }
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    protected void entityInit() {
		dataManager.register(CHUTE_COLOR, EntityParachute.Color.RANDOM.ordinal());
    }

    @Override
    public AxisAlignedBB getCollisionBox(Entity entity) {
        if (entity != getControllingPassenger() && entity.getRidingEntity() != this) {
            return entity.getEntityBoundingBox();
        }
        return null;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox() {
        return getEntityBoundingBox();
    }

    // skydiver should 'hang' when on the parachute and then
    // 'pick up legs' when landing.
    @Override
    public boolean shouldRiderSit() {
        Entity skyDiver = getControllingPassenger();
        boolean sitting = false;
        if (skyDiver != null) {
            BlockPos bp = new BlockPos(skyDiver.posX, skyDiver.getEntityBoundingBox().minY - 3.0, skyDiver.posZ);
            sitting = (worldObj.getBlockState(bp).getBlock() != Blocks.AIR);
        }
        return sitting;
    }

    @Override
    public EnumFacing getAdjustedHorizontalFacing() {
        return getHorizontalFacing().rotateY();
    }

    @Override
    public boolean canPassengerSteer() {
        return true;
    }

    @Override
    public Entity getControllingPassenger() {
        List<Entity> list = getPassengers();
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public boolean shouldDismountInWater(Entity pilot) {
        return dismountInWater;
    }

    @Override
    public double getMountedYOffset() {
        return -(ParachuteCommonProxy.getOffsetY());
    }

    @Override
    public boolean canBeCollidedWith() {
        return !isDead;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int inc, boolean teleport) {
        double deltaX = x - posX;
        double deltaY = y - posY;
        double deltaZ = z - posZ;
        double magnitude = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;

        if (magnitude <= 1.0D) {
            return;
        }

        // forward & vertical motion
        motionX = velocityX;
        motionY = velocityY;
        motionZ = velocityZ;
    }

    @Override
    public void setVelocity(double x, double y, double z) {
        velocityX = motionX = x;
        velocityY = motionY = y;
        velocityZ = motionZ = z;
    }

    @Override
    public void onUpdate() {
        Entity skyDiver = getControllingPassenger();
        super.onUpdate();

        // the player has pressed LSHIFT or been killed,
        // this is necessary for LSHIFT to kill the parachute
        if (skyDiver == null && !worldObj.isRemote) { // server side
            killParachute();
            return;
        }

        // initial forward velocity for this update
        double initialVelocity = Math.sqrt(motionX * motionX + motionZ * motionZ);

        if (showContrails && initialVelocity > 0.2) {
            generateContrails(initialVelocity);
        }

        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;

        // drop the chute when close to ground if enabled
        if (autoDismount && skyDiver != null) {
            double pilotFeetPos = skyDiver.getEntityBoundingBox().minY;
            BlockPos bp = new BlockPos(skyDiver.posX, pilotFeetPos - 1.0, skyDiver.posZ);
            if (checkForGroundProximity(bp)) {
                dismountParachute();
                return;
            }
        }

        // update forward velocity for 'W' key press
        // moveForward is > 0.0 when the 'W' key is pressed. Value is either 0.0 | ~0.98
        if (skyDiver != null && skyDiver instanceof EntityLivingBase) {
            EntityLivingBase pilot = (EntityLivingBase) skyDiver;
            double yaw = pilot.rotationYaw + -pilot.moveStrafing * 90.0;
            motionX += -Math.sin(Math.toRadians(yaw)) * motionFactor * 0.05 * (pilot.moveForward * 1.05);
            motionZ += Math.cos(Math.toRadians(yaw)) * motionFactor * 0.05 * (pilot.moveForward * 1.05);
        }

        // forward velocity after forward movement is applied
        double adjustedVelocity = Math.sqrt(motionX * motionX + motionZ * motionZ);
        // clamp the adjustedVelocity and modify motionX/Z
        if (adjustedVelocity > 0.35D) {
            double motionAdj = 0.35D / adjustedVelocity;
            motionX *= motionAdj;
            motionZ *= motionAdj;
            adjustedVelocity = 0.35D;
        }
        // clamp the motionFactor between 0.07 and 0.35
        if (adjustedVelocity > initialVelocity && motionFactor < 0.35D) {
            motionFactor += (0.35D - motionFactor) / 35.0D;
            if (motionFactor > 0.35D) {
                motionFactor = 0.35D;
            }
        } else {
            motionFactor -= (motionFactor - 0.07D) / 35.0D;
            if (motionFactor < 0.07D) {
                motionFactor = 0.07D;
            }
        }

        // calculate the descent rate
        motionY -= currentDescentRate();

        // move the parachute with the motion equations applied
        moveEntity(motionX, motionY, motionZ);

        // apply momentum
        motionX *= 0.99D;
        motionY *= 0.95D;
        motionZ *= 0.99D;

        // update pitch and yaw. Pitch is always 0.0
        rotationPitch = 0.0f;
        double yaw = rotationYaw;
        double delta_X = prevPosX - posX;
        double delta_Z = prevPosZ - posZ;

        // update direction (yaw)
        if (delta_X * delta_X + delta_Z * delta_Z > 0.001D) {
            yaw = Math.toDegrees(Math.atan2(delta_Z, delta_X));
        }

        // update and clamp yaw between -180 and 180
        double adjustedYaw = MathHelper.wrapDegrees(yaw - rotationYaw);
        // further clamp yaw between -20 and 20 per update, slower turn radius
        if (adjustedYaw > 20.0D) {
            adjustedYaw = 20.0D;
        }
        if (adjustedYaw < -20.0D) {
            adjustedYaw = -20.0D;
        }
        // update final yaw and apply to parachute
        rotationYaw += adjustedYaw;
        setRotation(rotationYaw, rotationPitch);

        // finally apply turbulence if flags allow
        if (((ConfigHandler.getWeatherAffectsDrift() && isBadWeather()) || allowTurbulence) && rand.nextBoolean()) {
            applyTurbulence(worldObj.isThundering());
        }

        // something bad happened, somehow the skydiver was killed.
        if (!worldObj.isRemote && skyDiver != null && skyDiver.isDead) { // server side
            killParachute();
        }

        // update distance by parachute statistics
        if (skyDiver != null) {
            double dX = posX - prevPosX;
            double dZ = posZ - prevPosZ;
            int distance = Math.round(MathHelper.sqrt_double(dX * dX + dZ * dZ) * 100.0F);
            if (skyDiver instanceof EntityPlayer) {
                ((EntityPlayer) skyDiver).addStat(Parachute.parachuteDistance, distance);
            }
        }
    }

    public void killParachute() {
        ParachuteCommonProxy.setDeployed(false);
        setDead();
    }

    // check for bad weather, if the biome can rain or snow check to see
    // if it is raining (or snowing) or thundering.
    public boolean isBadWeather() {
        BlockPos bp = new BlockPos(posX, posY, posZ);
        Chunk chunk = worldObj.getChunkFromBlockCoords(bp);
        boolean canSnow = chunk.getBiome(bp, worldObj.provider.getBiomeProvider()).getEnableSnow();
        boolean canRain = chunk.getBiome(bp, worldObj.provider.getBiomeProvider()).getRainfall() > 0;
        return (canRain || canSnow) && (worldObj.isRaining() || worldObj.isThundering());
    }

    // determines the descent rate based on whether or not
    // the space bar has been pressed. weather and lava affect
    // the final result.
    private double currentDescentRate() {
        double descentRate = drift; // defaults to drift

        if (ConfigHandler.getWeatherAffectsDrift()) {
            if (worldObj.isRaining()) { // rain makes you fall faster
                descentRate += 0.002;
            }

            if (worldObj.isThundering()) { // more rain really makes you fall faster
                descentRate += 0.004;
            }
        }

        if (!allowThermals && !lavaThermals) {
            return descentRate;
        }

        if (lavaThermals) {
            descentRate = doHeatSourceThermals();
            if (!allowThermals) {
                return descentRate;
            }
        }

        if (ascendMode) {
            descentRate = ascend;
        }

        if (maxAltitude > 0.0D) { // altitude limiting
            if (posY >= maxAltitude) {
                descentRate = drift;
            }
        }

        return descentRate;
    }

    // the following three methods detect lava below the player
    // at up to 'maxThermalRise' distance.
    private boolean isHeatSource(BlockPos bp) {
        Block block = worldObj.getBlockState(bp).getBlock();
        return worldObj.isFlammableWithin(new AxisAlignedBB(bp).expand(0,1,0));
    }

    private boolean isHeatSourceInRange(BlockPos bp) {
       Vec3d v1 = new Vec3d(posX, posY, posZ);
       Vec3d v2 = new Vec3d(bp.getX(), bp.getY(), bp.getZ());
       RayTraceResult mop = worldObj.rayTraceBlocks(v1, v2, true);
       if (mop != null && mop.typeOfHit == RayTraceResult.Type.BLOCK) {
           BlockPos blockpos = mop.getBlockPos();
			if (isHeatSource(blockpos)) {
				return true;
			}
		}
        return false;
    }

    private double doHeatSourceThermals() {
        double thermals = drift;
        final double inc = 0.5;

        BlockPos blockPos = new BlockPos(posX, posY - ParachuteCommonProxy.getOffsetY() - maxThermalRise, posZ);

        if (isHeatSourceInRange(blockPos)) {
            curLavaDistance += inc;
            thermals = ascend;
            if (curLavaDistance >= maxThermalRise) {
                curLavaDistance = lavaDistance;
                thermals = drift;
            }
        } else {
            curLavaDistance = lavaDistance;
        }
        return thermals;
    }

    // BlockPos bp is the pilot's position. The pilot's feet posY - 1.0
    // to be exact. We check for air blocks, flowers, leaves, and grass at
    // that position Y. The check for leaves means the parachute can get
    // hung up in the trees. Also means that the pilot must manually
    // dismount to land on trees. Dismounting over water is handled by the
    // shouldDismountInWater method and config option.
    public boolean checkForGroundProximity(BlockPos bp) {
        Block block = worldObj.getBlockState(bp).getBlock();
        boolean isAir = (block == Blocks.AIR);
        boolean isVegetation = (block instanceof BlockFlower) || (block instanceof BlockGrass) || (block instanceof BlockLeaves);
        return (!isAir && !isVegetation);
    }

    // apply 'turbulence' in the form of a collision force
    public void applyTurbulence(boolean roughWeather) {
        double rmin = 0.1;
        double deltaPos = rmin + 0.9 * rand.nextDouble();

        if (deltaPos >= 0.20) {
            double rmax = roughWeather ? 0.8 : 0.5;
            rmax = (rand.nextInt(5) == 0) ? 1.0 : rmax; // gusting
            double deltaX = rmin + (rmax - rmin) * rand.nextDouble();
            double deltaY = rmin + 0.2 * rand.nextDouble();
            double deltaZ = rmin + (rmax - rmin) * rand.nextDouble();

            deltaPos = MathHelper.sqrt_double(deltaPos);
            double deltaInv = 1.0 / deltaPos;

            deltaX /= deltaPos;
            deltaY /= deltaPos;
            deltaZ /= deltaPos;

            if (deltaInv > 1.0) {
                deltaInv = 1.0;
            }

            deltaX *= deltaInv;
            deltaY *= deltaInv;
            deltaZ *= deltaInv;

            deltaX *= 0.05;
            deltaY *= 0.05;
            deltaZ *= 0.05;

            if (rand.nextBoolean()) {
                addVelocity(-deltaX, -deltaY, -deltaZ);
            } else {
                addVelocity(deltaX, deltaY, deltaZ);
            }
        }
    }

    // generate condensation trails at the trailing edge
    // of the parachute. Yes I know that most parachutes
    // don't generate contrails (no engines), but most worlds
    // aren't made of blocks with cubic cows either. If you
    // like, you can think of the trails as chemtrails.
    public void generateContrails(double velocity) {
        double cosYaw = 2.0 * Math.cos(Math.toRadians(rotationYaw));
        double sinYaw = 2.0 * Math.sin(Math.toRadians(rotationYaw));

        for (int k = 0; (double) k < 1.0 + velocity; k++) {
            double sign = (double) (rand.nextInt(2) * 2 - 1) * 0.7;
            double x = prevPosX - cosYaw * -0.35 + sinYaw * sign;
            double y = posY - 0.25;
            double z = prevPosZ - sinYaw * -0.35 - cosYaw * sign;

            worldObj.spawnParticle(EnumParticleTypes.CLOUD, x, y, z, motionX, motionY, motionZ);
        }
    }

    @Override
    public void updatePassenger(Entity skydiver) {
        if (skydiver != null) {
            double x = posX + (Math.cos(Math.toRadians(rotationYaw)) * 0.04);
            double y = posY + getMountedYOffset() + skydiver.getYOffset();
            double z = posZ + (Math.sin(Math.toRadians(rotationYaw)) * 0.04);
            skydiver.setPosition(x, y, z);
            skydiver.setRenderYawOffset(rotationYaw + 90.0f);
            skydiver.setRotationYawHead(rotationYaw + 90);
        }
    }
    
    public EntityParachute.Color getChuteColor() {
		return EntityParachute.Color.byId(dataManager.get(CHUTE_COLOR));
	}
	
	public void setChuteColor(EntityParachute.Color chuteColor)
    {
        dataManager.set(CHUTE_COLOR, chuteColor.ordinal());
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
		nbt.setString("Color", getChuteColor().getColor());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
		if (nbt.hasKey("Color", 10)) {
            setChuteColor(EntityParachute.Color.getTypeFromString(nbt.getString("Color")));
        }
    }
    
    public enum Color {
        RANDOM("random"),
		BLACK("black"),
		BLUE("blue"),
		BROWN("brown"),
		CYAN("cyan"),
		GRAY("gray"),
		GREEN("green"),
		LIGHT_BLUE("light_blue"),
		LIME("lime"),
		MAGENTA("magenta"),
		ORANGE("orange"),
		PINK("pink"),
		PURPLE("purple"),
		RED("red"),
		SILVER("silver"),
		WHITE("white"),
		YELLOW("yellow");
		
		private final String color;
		
		private Color(String colorParam) {
			color = colorParam;
		}
		
		public String getColor() {
			return color;
		}
		
		public static EntityParachute.Color byId(int id) {
            if (id < 0 || id >= values().length) {
                id = 0;
            }

            return values()[id];
        }
        
        public static EntityParachute.Color getTypeFromString(String colorParam) {
            for (int k = 0; k < values().length; k++) {
                if (values()[k].getColor().equals(colorParam)) {
                    return values()[k];
                }
            }

            return values()[0];
        }
	}

    @Override
    public String toString() {
        return String.format("%s: {x=%.1f, y=%.1f, z=%.1f}, {yaw=%.1f, pitch=%.1f}", getClass().getSimpleName(), posX, posY, posZ, rotationYaw, rotationPitch);
    }

}
