package de.cadentem.cave_dweller.entities;

import de.cadentem.cave_dweller.config.ServerConfig;
import de.cadentem.cave_dweller.entities.goals.*;
import de.cadentem.cave_dweller.network.CaveSound;
import de.cadentem.cave_dweller.network.NetworkHandler;
import de.cadentem.cave_dweller.registry.ModSounds;
import de.cadentem.cave_dweller.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation.LoopType;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Random;

public class CaveDwellerEntity extends Monster implements GeoEntity {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   private final RawAnimation CHASE = RawAnimation.begin().then("animation.cave_dweller.new_run", LoopType.LOOP);
   private final RawAnimation CHASE_IDLE = RawAnimation.begin().then("animation.cave_dweller.run_idle", LoopType.LOOP);
   private final RawAnimation CROUCH_RUN = RawAnimation.begin().then("animation.cave_dweller.crouch_run_new", LoopType.LOOP);
   private final RawAnimation CROUCH_IDLE = RawAnimation.begin().then("animation.cave_dweller.crouch_idle", LoopType.LOOP);
   private final RawAnimation CALM_RUN = RawAnimation.begin().then("animation.cave_dweller.calm_move", LoopType.LOOP);
   private final RawAnimation CALM_STILL = RawAnimation.begin().then("animation.cave_dweller.calm_idle", LoopType.LOOP);
   private final RawAnimation IS_SPOTTED = RawAnimation.begin().then("animation.cave_dweller.spotted", LoopType.HOLD_ON_LAST_FRAME);
   private final RawAnimation CRAWL = RawAnimation.begin().then("animation.cave_dweller.crawl", LoopType.LOOP);
   private final RawAnimation FLEE = RawAnimation.begin().then("animation.cave_dweller.flee", LoopType.LOOP);

   public static final EntityDataAccessor<Boolean> FLEEING_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.BOOLEAN);
   public static final EntityDataAccessor<Boolean> CROUCHING_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.BOOLEAN);
   public static final EntityDataAccessor<Boolean> CRAWLING_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.BOOLEAN);
   public static final EntityDataAccessor<Boolean> SPOTTED_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.BOOLEAN);
   public static final EntityDataAccessor<Boolean> CLIMBING_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.BOOLEAN);

   public Roll currentRoll;
   public boolean isFleeing;
   public boolean hasSpawned;
   public boolean pleaseStopMoving;
   public boolean targetIsFacingMe;
   private int ticksTillRemove;
   private int chaseSoundClock;
   private boolean alreadyPlayedFleeSound;
   private boolean alreadyPlayedSpottedSound;
   private boolean startedPlayingChaseSound;
   private boolean alreadyPlayedDeathSound;

   public static float updateRotation(float angle, float targetAngle, float maxIncrease) {
      float f = Mth.wrapDegrees(targetAngle - angle);
      if (f > maxIncrease) {
         f = maxIncrease;
      }

      if (f < -maxIncrease) {
         f = -maxIncrease;
      }

      return angle + f;
   }

   public boolean canRiderInteract() {
      return false;
   }

   public boolean shouldRiderSit() {
      return true;
   }

   public CaveDwellerEntity(EntityType<? extends CaveDwellerEntity> entityType, Level level) {
      super(entityType, level);
      this.currentRoll = Roll.STROLL;
      this.refreshDimensions();
      this.ticksTillRemove = Utils.secondsToTicks((Integer) ServerConfig.TIME_UNTIL_LEAVE.get());
      this.setPathfindingMalus(BlockPathTypes.UNPASSABLE_RAIL, 0.0F);
   }

   @Nullable
   public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty, @NotNull MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag tagData) {
      this.setAttribute(this.getAttribute(Attributes.MAX_HEALTH), (Double) ServerConfig.MAX_HEALTH.get());
      this.setAttribute(this.getAttribute(Attributes.ATTACK_DAMAGE), (Double) ServerConfig.ATTACK_DAMAGE.get());
      this.setAttribute(this.getAttribute(Attributes.ATTACK_SPEED), (Double) ServerConfig.ATTACK_SPEED.get());
      this.setAttribute(this.getAttribute(Attributes.MOVEMENT_SPEED), (Double) ServerConfig.MOVEMENT_SPEED.get());
      this.setAttribute(this.getAttribute((Attribute) ForgeMod.STEP_HEIGHT_ADDITION.get()), 0.4D);
      return super.finalizeSpawn(level, difficulty, reason, spawnData, tagData);
   }

   private void setAttribute(AttributeInstance attribute, double value) {
      if (attribute != null) {
         attribute.setBaseValue(value);
         if (attribute.getAttribute() == Attributes.MAX_HEALTH) {
            this.setHealth((float) value);
         } else if (attribute.getAttribute() == Attributes.MOVEMENT_SPEED) {
            this.setSpeed((float) value);
         }
      }

   }

   public static AttributeSupplier getAttributeBuilder() {
      double maxHealth = 60.0D;
      double attackDamage = 6.0D;
      double attackSpeed = 0.35D;
      double movementSpeed = 0.3D;
      double followRange = 100.0D;
      return createMobAttributes().add(Attributes.MAX_HEALTH, maxHealth).add(Attributes.ATTACK_DAMAGE, attackDamage).add(Attributes.ATTACK_SPEED, attackSpeed).add(Attributes.MOVEMENT_SPEED, movementSpeed).add(Attributes.FOLLOW_RANGE, followRange).build();
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(FLEEING_ACCESSOR, false);
      this.entityData.define(CROUCHING_ACCESSOR, false);
      this.entityData.define(CRAWLING_ACCESSOR, false);
      this.entityData.define(SPOTTED_ACCESSOR, false);
      this.entityData.define(CLIMBING_ACCESSOR, false);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(1, new de.cadentem.cave_dweller.entities.goals.CaveDwellerChaseGoal(this, true));
      this.goalSelector.addGoal(1, new de.cadentem.cave_dweller.entities.goals.CaveDwellerFleeGoal(this, 20.0F, 1.0D));
      this.goalSelector.addGoal(2, new de.cadentem.cave_dweller.entities.goals.CaveDwellerBreakInvisGoal(this));
      this.goalSelector.addGoal(2, new de.cadentem.cave_dweller.entities.goals.CaveDwellerStareGoal(this));
      if ((Boolean) ServerConfig.CAN_BREAK_DOOR.get()) {
         this.goalSelector.addGoal(2, new de.cadentem.cave_dweller.entities.goals.CaveDwellerBreakDoorGoal(this, (difficulty) -> {
            return true;
         }));
      }

      this.goalSelector.addGoal(3, new de.cadentem.cave_dweller.entities.goals.CaveDwellerStrollGoal(this, 0.35D));
      this.targetSelector.addGoal(1, new de.cadentem.cave_dweller.entities.goals.CaveDwellerTargetTooCloseGoal(this, 12.0F));
      this.targetSelector.addGoal(2, new de.cadentem.cave_dweller.entities.goals.CaveDwellerTargetSeesMeGoal(this));
   }

   public void disappear() {
      this.playDisappearSound();
      this.discard();
   }

   public boolean hasSpawned() {
      return this.hasSpawned;
   }

   protected boolean canRide(@NotNull Entity vehicle) {
      return (Boolean) ServerConfig.ALLOW_RIDING.get() ? super.canRide(vehicle) : false;
   }

   public boolean startRiding(@NotNull Entity vehicle, boolean force) {
      return (Boolean) ServerConfig.ALLOW_RIDING.get() ? super.startRiding(vehicle, force) : false;
   }

   public void tick() {
      --this.ticksTillRemove;
      if (this.ticksTillRemove <= 0) {
         this.disappear();
      }

      if (this.goalSelector.getAvailableGoals().isEmpty() || this.targetSelector.getAvailableGoals().isEmpty()) {
         this.registerGoals();
         this.goalSelector.tick();
         this.targetSelector.tick();
      }

      if (this.getTarget() != null) {
         this.targetIsFacingMe = this.isLookingAtMe(this.getTarget(), false);
      }

      if (level instanceof ServerLevel) {
         boolean isAboveSolid = level.getBlockState(blockPosition().above()).getMaterial().isSolid();
         boolean isTwoAboveSolid = level.getBlockState(blockPosition().above(2)).getMaterial().isSolid();
         boolean isThreeAboveSolid = level.getBlockState(blockPosition().above(3)).getMaterial().isSolid();

         Vec3i offset = getDirectionVector();
         boolean isFacingSolid = level.getBlockState(blockPosition().relative(getDirection())).getMaterial().isSolid();

            /* Offset is set to the block above the block position (which is at feet level) (since direction is used it's the block in front for both cases)
                -----o                  -----o
                     o                       o <- offset
                -----o <- current       -----o
            */
         if (isFacingSolid) { // TODO :: Clean up, the offset with the check is kinda useless at this point since both positions are needed for correct checks
            offset = offset.offset(0, 1, 0);
         }

         boolean isOffsetFacingSolid = level.getBlockState(blockPosition().offset(offset)).getMaterial().isSolid();
         boolean isOffsetFacingAboveSolid = level.getBlockState(blockPosition().offset(offset).above()).getMaterial().isSolid();
         boolean isOffsetFacingTwoAboveSolid = level.getBlockState(blockPosition().offset(offset).above(2)).getMaterial().isSolid();

            /* [- : blocks | o : cave dweller | + : cave dweller in solid block]
                To handle these variants among other things:
               ----+        ----o       -----
                   o            o           o
                   o            o           o
               -----        -----       ----o
            */
         boolean shouldCrouch = isTwoAboveSolid || (!isOffsetFacingSolid && !isOffsetFacingAboveSolid && (isOffsetFacingTwoAboveSolid || isFacingSolid && isThreeAboveSolid));

            /* [- : blocks | o : cave dweller | + : cave dweller in solid block]
                To handle these variants among other things:
                    o           o
                ----+       ----o       ----+
                    o           o           o
                -----       -----       ----o
            */
         boolean shouldCrawl = isAboveSolid || !isOffsetFacingSolid && isOffsetFacingAboveSolid || isFacingSolid && isTwoAboveSolid;

         if (isAggressive() || isFleeing) {
            entityData.set(SPOTTED_ACCESSOR, false);
         }

         setClimbing(horizontalCollision);
         entityData.set(CROUCHING_ACCESSOR, shouldCrouch);
         setCrawling(shouldCrawl);
      }

      if (entityData.get(SPOTTED_ACCESSOR)) {
         playSpottedSound();
      }

      refreshDimensions(); // TODO :: Currently needed to make client stay in sync
      getNavigation().setSpeedModifier(getSpeedModifier());

      super.tick();
   }

   public double getSpeedModifier() {
      return this.isCrawling() ? 0.35D : (this.isCrouching() ? 0.6D : 0.85D);
   }

   @NotNull
   public EntityDimensions getDimensions(@NotNull Pose pose) {
      if ((Boolean) this.entityData.get(CRAWLING_ACCESSOR)) {
         return new EntityDimensions(0.5F, 0.5F, true);
      } else {
         return (Boolean) this.entityData.get(CROUCHING_ACCESSOR) ? new EntityDimensions(0.5F, 1.7F, true) : super.getDimensions(pose);
      }
   }

   private boolean isMoving() {
      Vec3 velocity = this.getDeltaMovement();
      float avgVelocity = (float) (Math.abs(velocity.x) + Math.abs(velocity.z)) / 2.0F;
      return avgVelocity > 0.03F;
   }

   public void reRoll() {
      this.currentRoll = Roll.fromValue(new Random().nextInt(3));
   }

   public void pickRoll(@NotNull List<Roll> rolls) {
      this.currentRoll = (Roll) rolls.get((new Random()).nextInt(rolls.size()));
   }

   public boolean onClimbable() {
      return this.isClimbing();
   }

   public boolean isClimbing() {
      if (!(Boolean) ServerConfig.CAN_CLIMB.get()) {
         return false;
      } else if (this.getTarget() == null) {
         return false;
      } else {
         return !this.isCrawling() && !this.isCrouching() && (Boolean) this.entityData.get(CLIMBING_ACCESSOR);
      }
   }

   public void setClimbing(boolean isClimbing) {
      this.entityData.set(CLIMBING_ACCESSOR, isClimbing);
   }

   @NotNull
   protected PathNavigation createNavigation(@NotNull Level level) {
      WallClimberNavigation navigation = new WallClimberNavigation(this, level);
      navigation.setMaxVisitedNodesMultiplier(4.0F);
      return navigation;
   }

   private PlayState predicate(final AnimationState<CaveDwellerEntity> state) {
      boolean isCurrentAboveSolid = level.getBlockState(blockPosition().above()).getMaterial().isSolid();
      boolean unsure = isCrawling() && level.getBlockState(blockPosition()).getMaterial().isSolid();
//        boolean isFacingAboveSolid = isCrawling() && level.getBlockState(blockPosition().offset(getDirectionVector()).above()).getMaterial().isSolid();
      boolean isCurrentTwoAboveSolid = level.getBlockState(blockPosition().above(2)).getMaterial().isSolid();
//        boolean isFacingTwoAboveSolid = isCrouching() && level.getBlockState(blockPosition().offset(getDirectionVector()).above(2)).getMaterial().isSolid();;

      // TODO :: Climbing animation
      if (isCurrentAboveSolid || unsure/* || isFacingAboveSolid*/) {
         // Crawling
         return state.setAndContinue(CRAWL);
      } else if (isCurrentTwoAboveSolid /*|| isFacingTwoAboveSolid*/) {
         // Crouching
         if (state.isMoving()) {
            return state.setAndContinue(CROUCH_RUN);
         } else {
            return state.setAndContinue(CROUCH_IDLE);
         }
      } else if (isAggressive()) {
         // Chase
         if (state.isMoving()) {
            return state.setAndContinue(CHASE);
         } else {
            return state.setAndContinue(CHASE_IDLE);
         }
      } else if (entityData.get(FLEEING_ACCESSOR)) {
         // Fleeing
         if (state.isMoving()) {
            return state.setAndContinue(FLEE);
         } else {
            return state.setAndContinue(CHASE_IDLE);
         }
      } else if (pleaseStopMoving || entityData.get(SPOTTED_ACCESSOR) && !state.isMoving()) {
         // Spotted
         return state.setAndContinue(IS_SPOTTED);
      } else {
         // Normal
         if (state.isMoving()) {
            return state.setAndContinue(CALM_RUN);
         } else {
            return state.setAndContinue(CALM_STILL);
         }
      }
   }

   @Override
   public void registerControllers(final AnimatableManager.ControllerRegistrar registrar) {
      registrar.add(new AnimationController<>(this, "controller", 3, this::predicate));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return cache;
   }

   protected void playStepSound(@NotNull BlockPos pPos, @NotNull BlockState pState) {
      super.playStepSound(pPos, pState);
      this.playEntitySound(this.chooseStep());
   }

   private void playEntitySound(SoundEvent soundEvent) {
      this.playEntitySound(soundEvent, 1.0F, 1.0F);
   }

   private void playEntitySound(SoundEvent soundEvent, float volume, float pitch) {
      this.level.playSound((Player) null, this, soundEvent, SoundSource.HOSTILE, volume, pitch);
   }

   private void playBlockPosSound(final ResourceLocation soundResource, float volume, float pitch) {
      if (level instanceof ServerLevel serverLevel) {
         int radius = 32; // blocks
         serverLevel.getPlayers(player -> player.distanceToSqr(this) <= radius * radius).forEach(player -> NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CaveSound(soundResource, blockPosition(), volume, pitch)));
      }
   }

   public void playChaseSound() {
      if (this.startedPlayingChaseSound || this.isMoving()) {
         if (this.chaseSoundClock <= 0) {
            Random rand = new Random();
            switch (rand.nextInt(4)) {
               case 0:
                  this.playEntitySound((SoundEvent) ModSounds.CHASE_1.get(), 3.0F, 1.0F);
                  break;
               case 1:
                  this.playEntitySound((SoundEvent) ModSounds.CHASE_2.get(), 3.0F, 1.0F);
                  break;
               case 2:
                  this.playEntitySound((SoundEvent) ModSounds.CHASE_3.get(), 3.0F, 1.0F);
                  break;
               case 3:
                  this.playEntitySound((SoundEvent) ModSounds.CHASE_4.get(), 3.0F, 1.0F);
            }

            this.startedPlayingChaseSound = true;
            this.resetChaseSoundClock();
         }

         --this.chaseSoundClock;
      }

   }

   public void playDisappearSound() {
      this.playBlockPosSound(((SoundEvent) ModSounds.DISAPPEAR.get()).getLocation(), 3.0F, 1.0F);
   }

   public void playFleeSound() {
      if (!this.alreadyPlayedFleeSound) {
         Random rand = new Random();
         switch (rand.nextInt(2)) {
            case 0:
               this.playEntitySound((SoundEvent) ModSounds.FLEE_1.get(), 3.0F, 1.0F);
               break;
            case 1:
               this.playEntitySound((SoundEvent) ModSounds.FLEE_2.get(), 3.0F, 1.0F);
         }

         this.alreadyPlayedFleeSound = true;
      }

   }

   private void playSpottedSound() {
      if (!this.alreadyPlayedSpottedSound) {
         this.playEntitySound((SoundEvent) ModSounds.SPOTTED.get(), 3.0F, 1.0F);
         this.alreadyPlayedSpottedSound = true;
      }

   }

   private void resetChaseSoundClock() {
      this.chaseSoundClock = Utils.secondsToTicks(5);
   }

   private SoundEvent chooseStep() {
      Random rand = new Random();
      SoundEvent var10000;
      switch (rand.nextInt(4)) {
         case 1:
            var10000 = (SoundEvent) ModSounds.CHASE_STEP_2.get();
            break;
         case 2:
            var10000 = (SoundEvent) ModSounds.CHASE_STEP_3.get();
            break;
         case 3:
            var10000 = (SoundEvent) ModSounds.CHASE_STEP_4.get();
            break;
         default:
            var10000 = (SoundEvent) ModSounds.CHASE_STEP_1.get();
      }

      return var10000;
   }

   private SoundEvent chooseHurtSound() {
      Random rand = new Random();
      SoundEvent var10000;
      switch (rand.nextInt(4)) {
         case 1:
            var10000 = (SoundEvent) ModSounds.DWELLER_HURT_2.get();
            break;
         case 2:
            var10000 = (SoundEvent) ModSounds.DWELLER_HURT_3.get();
            break;
         case 3:
            var10000 = (SoundEvent) ModSounds.DWELLER_HURT_4.get();
            break;
         default:
            var10000 = (SoundEvent) ModSounds.DWELLER_HURT_1.get();
      }

      return var10000;
   }

   protected void playHurtSound(@NotNull DamageSource pSource) {
      SoundEvent soundevent = this.chooseHurtSound();
      this.playEntitySound(soundevent, 2.0F, 1.0F);
   }

   public void setCrawling(boolean shouldCrawl) {
      if (shouldCrawl) {
         this.getEntityData().set(CROUCHING_ACCESSOR, false);
      }

      this.getEntityData().set(CRAWLING_ACCESSOR, shouldCrawl);
      this.refreshDimensions();
   }

   public boolean isCrawling() {
      return (Boolean) this.entityData.get(CRAWLING_ACCESSOR);
   }

   protected void tickDeath() {
      super.tickDeath();
      if (!this.alreadyPlayedDeathSound) {
         this.playBlockPosSound(((SoundEvent) ModSounds.DWELLER_DEATH.get()).getLocation(), 2.0F, 1.0F);
         this.alreadyPlayedDeathSound = true;
      }

   }

   public boolean isLookingAtMe(Entity target, boolean directlyLooking) {
      if (!Utils.isValidPlayer(target)) {
         return false;
      } else if (target.getEyePosition(1.0F).distanceTo(this.getPosition(1.0F)) > (double) (Integer) ServerConfig.SPOTTING_RANGE.get()) {
         return false;
      } else {
         Vec3 viewVector = target.getViewVector(1.0F).normalize();
         Vec3 difference = new Vec3(this.getX() - target.getX(), this.getEyeY() - target.getEyeY(), this.getZ() - target.getZ());
         difference = difference.normalize();
         double dot = viewVector.dot(difference);
         if (directlyLooking && target instanceof Player) {
            Player player = (Player) target;
            return dot > 0.99D && player.hasLineOfSight(this);
         } else {
            return dot > 0.3D;
         }
      }
   }

   public boolean teleportToTarget() {
      LivingEntity target = this.getTarget();
      if (target == null) {
         return false;
      } else {
         Vec3 targetPosition = new Vec3(this.getX() - target.getX(), this.getY(0.5D) - target.getEyeY(), this.getZ() - target.getZ());
         targetPosition = targetPosition.normalize();
         double radius = 16.0D;
         double d1 = this.getX() + (this.getRandom().nextDouble() - 0.5D) * (radius / 2.0D) - targetPosition.x * radius;
         double d2 = this.getY() + ((double) this.getRandom().nextInt((int) radius) - radius / 2.0D) - targetPosition.y * radius;
         double d3 = this.getZ() + (this.getRandom().nextDouble() - 0.5D) * (radius / 2.0D) - targetPosition.z * radius;
         BlockPos.MutableBlockPos validPosition = new BlockPos.MutableBlockPos(d1, d2, d3);

         while (validPosition.getY() > this.level.getMinBuildHeight() && !this.level.getBlockState(validPosition).getMaterial().blocksMotion()) {
            validPosition.move(Direction.DOWN);
         }

         this.teleportTo((double) validPosition.getX(), (double) validPosition.getY(), (double) validPosition.getZ());
         return true;
      }
   }

   private Vec3i getDirectionVector() {
      return new Vec3i(this.getDirection().getStepX(), this.getDirection().getStepY(), this.getDirection().getStepZ());
   }

   protected SoundEvent getHurtSound(@NotNull DamageSource damageSourceIn) {
      return this.chooseHurtSound();
   }

   protected SoundEvent getDeathSound() {
      return (SoundEvent) ModSounds.DWELLER_DEATH.get();
   }

   protected float getSoundVolume() {
      return 0.4F;
   }
}