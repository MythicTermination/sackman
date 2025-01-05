package de.cadentem.cave_dweller.entities;

import de.cadentem.cave_dweller.config.ServerConfig;
import de.cadentem.cave_dweller.entities.goals.Roll;
import de.cadentem.cave_dweller.network.CaveSound;
import de.cadentem.cave_dweller.network.NetworkHandler;
import de.cadentem.cave_dweller.registry.ModSounds;
import de.cadentem.cave_dweller.util.Utils;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.BlockPos.MutableBlockPos;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
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
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.RawAnimation;
import software.bernie.geckolib3.core.builder.ILoopType.EDefaultLoopTypes;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

public class CaveDwellerEntity extends Monster implements IAnimatable {
   private final AnimationFactory factory = GeckoLibUtil.createFactory(this);
   private final RawAnimation CHASE;
   private final RawAnimation CHASE_IDLE;
   private final RawAnimation CROUCH_RUN;
   private final RawAnimation CROUCH_IDLE;
   private final RawAnimation CALM_RUN;
   private final RawAnimation CALM_STILL;
   private final RawAnimation IS_SPOTTED;
   private final RawAnimation CRAWL;
   private final RawAnimation FLEE;
   public static final EntityDataAccessor<Boolean> FLEEING_ACCESSOR;
   public static final EntityDataAccessor<Boolean> CROUCHING_ACCESSOR;
   public static final EntityDataAccessor<Boolean> CRAWLING_ACCESSOR;
   public static final EntityDataAccessor<Boolean> SPOTTED_ACCESSOR;
   public static final EntityDataAccessor<Boolean> CLIMBING_ACCESSOR;
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
      this.CHASE = new RawAnimation("animation.cave_dweller.new_run", EDefaultLoopTypes.LOOP);
      this.CHASE_IDLE = new RawAnimation("animation.cave_dweller.run_idle", EDefaultLoopTypes.LOOP);
      this.CROUCH_RUN = new RawAnimation("animation.cave_dweller.crouch_run_new", EDefaultLoopTypes.LOOP);
      this.CROUCH_IDLE = new RawAnimation("animation.cave_dweller.crouch_idle", EDefaultLoopTypes.LOOP);
      this.CALM_RUN = new RawAnimation("animation.cave_dweller.calm_move", EDefaultLoopTypes.LOOP);
      this.CALM_STILL = new RawAnimation("animation.cave_dweller.calm_idle", EDefaultLoopTypes.LOOP);
      this.IS_SPOTTED = new RawAnimation("animation.cave_dweller.spotted", EDefaultLoopTypes.HOLD_ON_LAST_FRAME);
      this.CRAWL = new RawAnimation("animation.cave_dweller.crawl", EDefaultLoopTypes.LOOP);
      this.FLEE = new RawAnimation("animation.cave_dweller.flee", EDefaultLoopTypes.LOOP);
      this.currentRoll = Roll.STROLL;
      this.refreshDimensions();
      this.ticksTillRemove = Utils.secondsToTicks((Integer)ServerConfig.TIME_UNTIL_LEAVE.get());
      this.setPathfindingMalus(BlockPathTypes.UNPASSABLE_RAIL, 0.0F);
   }

   @Nullable
   public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty, @NotNull MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag tagData) {
      this.setAttribute(this.getAttribute(Attributes.MAX_HEALTH), (Double)ServerConfig.MAX_HEALTH.get());
      this.setAttribute(this.getAttribute(Attributes.ATTACK_DAMAGE), (Double)ServerConfig.ATTACK_DAMAGE.get());
      this.setAttribute(this.getAttribute(Attributes.ATTACK_SPEED), (Double)ServerConfig.ATTACK_SPEED.get());
      this.setAttribute(this.getAttribute(Attributes.MOVEMENT_SPEED), (Double)ServerConfig.MOVEMENT_SPEED.get());
      this.setAttribute(this.getAttribute((Attribute)ForgeMod.STEP_HEIGHT_ADDITION.get()), 0.4D);
      return super.finalizeSpawn(level, difficulty, reason, spawnData, tagData);
   }

   private void setAttribute(AttributeInstance attribute, double value) {
      if (attribute != null) {
         attribute.setBaseValue(value);
         if (attribute.getAttribute() == Attributes.MAX_HEALTH) {
            this.setHealth((float)value);
         } else if (attribute.getAttribute() == Attributes.MOVEMENT_SPEED) {
            this.setSpeed((float)value);
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
      if ((Boolean)ServerConfig.CAN_BREAK_DOOR.get()) {
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
      return (Boolean)ServerConfig.ALLOW_RIDING.get() ? super.canRide(vehicle) : false;
   }

   public boolean startRiding(@NotNull Entity vehicle, boolean force) {
      return (Boolean)ServerConfig.ALLOW_RIDING.get() ? super.startRiding(vehicle, force) : false;
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

      if (this.level instanceof ServerLevel) {
         boolean isAboveSolid = this.level.getBlockState(this.blockPosition().above()).getMaterial().isSolid();
         boolean isTwoAboveSolid = this.level.getBlockState(this.blockPosition().above(2)).getMaterial().isSolid();
         boolean isThreeAboveSolid = this.level.getBlockState(this.blockPosition().above(3)).getMaterial().isSolid();
         Vec3i offset = this.getDirectionVector();
         boolean isFacingSolid = this.level.getBlockState(this.blockPosition().relative(this.getDirection())).getMaterial().isSolid();
         if (isFacingSolid) {
            offset = offset.offset(0, 1, 0);
         }

         boolean isOffsetFacingSolid = this.level.getBlockState(this.blockPosition().offset(offset)).getMaterial().isSolid();
         boolean isOffsetFacingAboveSolid = this.level.getBlockState(this.blockPosition().offset(offset).above()).getMaterial().isSolid();
         boolean isOffsetFacingTwoAboveSolid = this.level.getBlockState(this.blockPosition().offset(offset).above(2)).getMaterial().isSolid();
         boolean shouldCrouch = isTwoAboveSolid || !isOffsetFacingSolid && !isOffsetFacingAboveSolid && (isOffsetFacingTwoAboveSolid || isFacingSolid && isThreeAboveSolid);
         boolean shouldCrawl = isAboveSolid || !isOffsetFacingSolid && isOffsetFacingAboveSolid || isFacingSolid && isTwoAboveSolid;
         if (this.isAggressive() || this.isFleeing) {
            this.entityData.set(SPOTTED_ACCESSOR, false);
         }

         this.setClimbing(this.horizontalCollision);
         this.entityData.set(CROUCHING_ACCESSOR, shouldCrouch);
         this.setCrawling(shouldCrawl);
      }

      if ((Boolean)this.entityData.get(SPOTTED_ACCESSOR)) {
         this.playSpottedSound();
      }

      this.refreshDimensions();
      this.getNavigation().setSpeedModifier(this.getSpeedModifier());
      super.tick();
   }

   public double getSpeedModifier() {
      return this.isCrawling() ? 0.35D : (this.isCrouching() ? 0.6D : 0.85D);
   }

   @NotNull
   public EntityDimensions getDimensions(@NotNull Pose pose) {
      if ((Boolean)this.entityData.get(CRAWLING_ACCESSOR)) {
         return new EntityDimensions(0.5F, 0.5F, true);
      } else {
         return (Boolean)this.entityData.get(CROUCHING_ACCESSOR) ? new EntityDimensions(0.5F, 1.7F, true) : super.getDimensions(pose);
      }
   }

   private boolean isMoving() {
      Vec3 velocity = this.getDeltaMovement();
      float avgVelocity = (float)(Math.abs(velocity.x) + Math.abs(velocity.z)) / 2.0F;
      return avgVelocity > 0.03F;
   }

   public void reRoll() {
      this.currentRoll = Roll.fromValue((new Random()).nextInt(3));
   }

   public void pickRoll(@NotNull List<Roll> rolls) {
      this.currentRoll = (Roll)rolls.get((new Random()).nextInt(rolls.size()));
   }

   public boolean onClimbable() {
      return this.isClimbing();
   }

   public boolean isClimbing() {
      if (!(Boolean)ServerConfig.CAN_CLIMB.get()) {
         return false;
      } else if (this.getTarget() == null) {
         return false;
      } else {
         return !this.isCrawling() && !this.isCrouching() && (Boolean)this.entityData.get(CLIMBING_ACCESSOR);
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

   private PlayState predicate(AnimationEvent<CaveDwellerEntity> event) {
      AnimationBuilder builder = new AnimationBuilder();
      AnimationController<CaveDwellerEntity> controller = event.getController();
      boolean isCurrentAboveSolid = this.level.getBlockState(this.blockPosition().above()).getMaterial().isSolid();
      boolean unsure = this.isCrawling() && this.level.getBlockState(this.blockPosition()).getMaterial().isSolid();
      boolean isCurrentTwoAboveSolid = this.level.getBlockState(this.blockPosition().above(2)).getMaterial().isSolid();
      if (!isCurrentAboveSolid && !unsure) {
         if (isCurrentTwoAboveSolid) {
            if (event.isMoving()) {
               builder.addAnimation(this.CROUCH_RUN.animationName, this.CROUCH_RUN.loopType);
            } else {
               builder.addAnimation(this.CROUCH_IDLE.animationName, this.CROUCH_IDLE.loopType);
            }
         } else if (this.isAggressive()) {
            if (event.isMoving()) {
               builder.addAnimation(this.CHASE.animationName, this.CHASE.loopType);
            } else {
               builder.addAnimation(this.CHASE_IDLE.animationName, this.CHASE_IDLE.loopType);
            }
         } else if ((Boolean)this.entityData.get(FLEEING_ACCESSOR)) {
            if (event.isMoving()) {
               builder.addAnimation(this.FLEE.animationName, this.FLEE.loopType);
            } else {
               builder.addAnimation(this.CHASE_IDLE.animationName, this.CHASE_IDLE.loopType);
            }
         } else if (!this.pleaseStopMoving && (!(Boolean)this.entityData.get(SPOTTED_ACCESSOR) || event.isMoving())) {
            if (event.isMoving()) {
               builder.addAnimation(this.CALM_RUN.animationName, this.CALM_RUN.loopType);
            } else {
               builder.addAnimation(this.CALM_STILL.animationName, this.CALM_STILL.loopType);
            }
         } else {
            builder.addAnimation(this.IS_SPOTTED.animationName, this.IS_SPOTTED.loopType);
         }
      } else {
         builder.addAnimation(this.CRAWL.animationName, this.CRAWL.loopType);
      }

      controller.setAnimation(builder);
      return PlayState.CONTINUE;
   }

   public void registerControllers(AnimationData data) {
      data.addAnimationController(new AnimationController(this, "controller", 3.0F, this::predicate));
   }

   public AnimationFactory getFactory() {
      return this.factory;
   }

   protected void playStepSound(@NotNull BlockPos pPos, @NotNull BlockState pState) {
      super.playStepSound(pPos, pState);
      this.playEntitySound(this.chooseStep());
   }

   private void playEntitySound(SoundEvent soundEvent) {
      this.playEntitySound(soundEvent, 1.0F, 1.0F);
   }

   private void playEntitySound(SoundEvent soundEvent, float volume, float pitch) {
      this.level.playSound((Player)null, this, soundEvent, SoundSource.HOSTILE, volume, pitch);
   }

   private void playBlockPosSound(ResourceLocation soundResource, float volume, float pitch) {
      Level var5 = this.level;
      if (var5 instanceof ServerLevel) {
         ServerLevel serverLevel = (ServerLevel)var5;
         int radius = 60;
         serverLevel.getPlayers((player) -> {
            return player.distanceToSqr(this) <= (double)(radius * radius);
         }).forEach((player) -> {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> {
               return player;
            }), new CaveSound(soundResource, player.blockPosition(), volume, pitch));
         });
      }

   }

   public void playChaseSound() {
      if (this.startedPlayingChaseSound || this.isMoving()) {
         if (this.chaseSoundClock <= 0) {
            Random rand = new Random();
            switch(rand.nextInt(4)) {
            case 0:
               this.playEntitySound((SoundEvent)ModSounds.CHASE_1.get(), 3.0F, 1.0F);
               break;
            case 1:
               this.playEntitySound((SoundEvent)ModSounds.CHASE_2.get(), 3.0F, 1.0F);
               break;
            case 2:
               this.playEntitySound((SoundEvent)ModSounds.CHASE_3.get(), 3.0F, 1.0F);
               break;
            case 3:
               this.playEntitySound((SoundEvent)ModSounds.CHASE_4.get(), 3.0F, 1.0F);
            }

            this.startedPlayingChaseSound = true;
            this.resetChaseSoundClock();
         }

         --this.chaseSoundClock;
      }

   }

   public void playDisappearSound() {
      this.playBlockPosSound(((SoundEvent)ModSounds.DISAPPEAR.get()).getLocation(), 3.0F, 1.0F);
   }

   public void playFleeSound() {
      if (!this.alreadyPlayedFleeSound) {
         Random rand = new Random();
         switch(rand.nextInt(2)) {
         case 0:
            this.playEntitySound((SoundEvent)ModSounds.FLEE_1.get(), 3.0F, 1.0F);
            break;
         case 1:
            this.playEntitySound((SoundEvent)ModSounds.FLEE_2.get(), 3.0F, 1.0F);
         }

         this.alreadyPlayedFleeSound = true;
      }

   }

   private void playSpottedSound() {
      if (!this.alreadyPlayedSpottedSound) {
         this.playEntitySound((SoundEvent)ModSounds.SPOTTED.get(), 3.0F, 1.0F);
         this.alreadyPlayedSpottedSound = true;
      }

   }

   private void resetChaseSoundClock() {
      this.chaseSoundClock = Utils.secondsToTicks(5);
   }

   private SoundEvent chooseStep() {
      Random rand = new Random();
      SoundEvent var10000;
      switch(rand.nextInt(4)) {
      case 1:
         var10000 = (SoundEvent)ModSounds.CHASE_STEP_2.get();
         break;
      case 2:
         var10000 = (SoundEvent)ModSounds.CHASE_STEP_3.get();
         break;
      case 3:
         var10000 = (SoundEvent)ModSounds.CHASE_STEP_4.get();
         break;
      default:
         var10000 = (SoundEvent)ModSounds.CHASE_STEP_1.get();
      }

      return var10000;
   }

   private SoundEvent chooseHurtSound() {
      Random rand = new Random();
      SoundEvent var10000;
      switch(rand.nextInt(4)) {
      case 1:
         var10000 = (SoundEvent)ModSounds.DWELLER_HURT_2.get();
         break;
      case 2:
         var10000 = (SoundEvent)ModSounds.DWELLER_HURT_3.get();
         break;
      case 3:
         var10000 = (SoundEvent)ModSounds.DWELLER_HURT_4.get();
         break;
      default:
         var10000 = (SoundEvent)ModSounds.DWELLER_HURT_1.get();
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
      return (Boolean)this.entityData.get(CRAWLING_ACCESSOR);
   }

   protected void tickDeath() {
      super.tickDeath();
      if (!this.alreadyPlayedDeathSound) {
         this.playBlockPosSound(((SoundEvent)ModSounds.DWELLER_DEATH.get()).getLocation(), 2.0F, 1.0F);
         this.alreadyPlayedDeathSound = true;
      }

   }

   public boolean isLookingAtMe(Entity target, boolean directlyLooking) {
      if (!Utils.isValidPlayer(target)) {
         return false;
      } else if (target.getEyePosition(1.0F).distanceTo(this.getPosition(1.0F)) > (double)(Integer)ServerConfig.SPOTTING_RANGE.get()) {
         return false;
      } else {
         Vec3 viewVector = target.getViewVector(1.0F).normalize();
         Vec3 difference = new Vec3(this.getX() - target.getX(), this.getEyeY() - target.getEyeY(), this.getZ() - target.getZ());
         difference = difference.normalize();
         double dot = viewVector.dot(difference);
         if (directlyLooking && target instanceof Player) {
            Player player = (Player)target;
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
         double d2 = this.getY() + ((double)this.getRandom().nextInt((int)radius) - radius / 2.0D) - targetPosition.y * radius;
         double d3 = this.getZ() + (this.getRandom().nextDouble() - 0.5D) * (radius / 2.0D) - targetPosition.z * radius;
         MutableBlockPos validPosition = new MutableBlockPos(d1, d2, d3);

         while(validPosition.getY() > this.level.getMinBuildHeight() && !this.level.getBlockState(validPosition).getMaterial().blocksMotion()) {
            validPosition.move(Direction.DOWN);
         }

         this.teleportTo((double)validPosition.getX(), (double)validPosition.getY(), (double)validPosition.getZ());
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
      return (SoundEvent)ModSounds.DWELLER_DEATH.get();
   }

   protected float getSoundVolume() {
      return 0.4F;
   }

   static {
      FLEEING_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.BOOLEAN);
      CROUCHING_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.BOOLEAN);
      CRAWLING_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.BOOLEAN);
      SPOTTED_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.BOOLEAN);
      CLIMBING_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.BOOLEAN);
   }
}
