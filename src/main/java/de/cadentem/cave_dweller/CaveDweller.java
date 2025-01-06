package de.cadentem.cave_dweller;

import com.mojang.logging.LogUtils;
import de.cadentem.cave_dweller.config.ServerConfig;
import de.cadentem.cave_dweller.client.CaveDwellerRenderer;
import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller.network.CaveSound;
import de.cadentem.cave_dweller.network.NetworkHandler;
import de.cadentem.cave_dweller.registry.ModEntityTypes;
import de.cadentem.cave_dweller.registry.ModItems;
import de.cadentem.cave_dweller.registry.ModSounds;
import de.cadentem.cave_dweller.util.Timer;
import de.cadentem.cave_dweller.util.Utils;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.SpawnUtil.Strategy;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;
import software.bernie.geckolib.GeckoLib;

@Mod("cave_dweller")
public class CaveDweller {
   public static final String MODID = "cave_dweller";
   public static final Logger LOG = LogUtils.getLogger();
   public static final Random RANDOM = new Random();
   private static final HashMap<String, Timer> TIMERS = new HashMap();
   public static boolean RELOAD_ALL = false;
   public static boolean RELOAD_MISSING = false;

   public CaveDweller() {
      GeckoLib.initialize();
      IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
      modEventBus.addListener(this::clientSetup);
      modEventBus.addListener(this::commonSetup);
      modEventBus.addListener(ModItems::addCreative);

      ModItems.register(modEventBus);
      ModSounds.register(modEventBus);
      ModEntityTypes.register(modEventBus);
      MinecraftForge.EVENT_BUS.register(this);
      ModLoadingContext.get().registerConfig(Type.SERVER, ServerConfig.SPEC);
   }

   private void clientSetup(final FMLClientSetupEvent event) {
      EntityRenderers.register(ModEntityTypes.CAVE_DWELLER.get(), CaveDwellerRenderer::new);
   }

   private void commonSetup(final FMLCommonSetupEvent event) {
      NetworkHandler.register();
   }

   @SubscribeEvent
   public void serverStartup(ServerStartedEvent event) {
      RELOAD_ALL = true;
   }

   @SubscribeEvent
   public void serverTick(ServerTickEvent event) {
      if (event.phase != Phase.END) {
         if (RELOAD_ALL) {
            TIMERS.clear();
            RELOAD_ALL = false;
         }

         Iterable<ServerLevel> levels = event.getServer().getAllLevels();
         Iterator var3;
         ServerLevel level;
         String key;
         boolean isRelevant;
         if (TIMERS.isEmpty()) {
            var3 = levels.iterator();

            while(var3.hasNext()) {
               level = (ServerLevel)var3.next();
               key = level.dimension().location().toString();
               isRelevant = ((List)ServerConfig.DIMENSION_WHITELIST.get()).contains(key);
               if (isRelevant) {
                  TIMERS.put(key, new Timer());
               }
            }

            if (TIMERS.isEmpty() && event.getServer().getTickCount() % 6000 == 0) {
               LOG.debug("There are currently no timers present - are the dimensions properly configured?");
            }

            RELOAD_ALL = false;
            RELOAD_MISSING = false;
         } else if (RELOAD_MISSING) {
            var3 = levels.iterator();

            while(var3.hasNext()) {
               level = (ServerLevel)var3.next();
               key = level.dimension().location().toString();
               isRelevant = TIMERS.get(key) == null && ((List)ServerConfig.DIMENSION_WHITELIST.get()).contains(key);
               if (isRelevant) {
                  TIMERS.put(key, new Timer());
               }
            }

            RELOAD_MISSING = false;
         }

         var3 = levels.iterator();

         while(var3.hasNext()) {
            level = (ServerLevel)var3.next();
            key = level.dimension().location().toString();
            if (TIMERS.get(key) != null) {
               this.handleLogic(level);
            }
         }

      }
   }

   private void handleLogic(ServerLevel level) {
      if (level != null) {
         List<ServerPlayer> players = level.getPlayers(this::isRelevantPlayer);
         if (!players.isEmpty()) {
            String key = level.dimension().location().toString();
            Timer timer = (Timer)TIMERS.get(key);
            if (timer.currentVictim == null || players.stream().filter((element) -> {
               return element.getStringUUID().equals(timer.currentVictim.getStringUUID());
            }).toList().isEmpty()) {
               timer.currentVictim = (Entity)players.get(RANDOM.nextInt(players.size()));
            }

            Iterable<Entity> entities = level.getAllEntities();
            AtomicInteger caveDwellerCount = new AtomicInteger();
            entities.forEach((entity) -> {
               if (entity instanceof CaveDwellerEntity) {
                  caveDwellerCount.getAndAdd(1);
               }

            });
            ++timer.currentSpawn;
            ++timer.currentNoise;
            if (timer.isNoiseTimerReached() && (caveDwellerCount.get() > 0 || timer.currentSpawn >= Utils.secondsToTicks((Integer)ServerConfig.CAN_SPAWN_MAX.get()) / 2)) {
               this.playCaveSoundToSpelunkers(players, timer);
            }

            if (timer.isSpawnTimerReached() && caveDwellerCount.get() < (Integer)ServerConfig.MAXIMUM_AMOUNT.get() && RANDOM.nextDouble() <= (Double)ServerConfig.SPAWN_CHANCE_PER_TICK.get() && timer.currentVictim != null) {
               Optional<CaveDwellerEntity> optionalEntity = Utils.trySpawnMob(timer.currentVictim, (EntityType)ModEntityTypes.CAVE_DWELLER.get(), MobSpawnType.TRIGGERED, level, timer.currentVictim.blockPosition(), 40, 35, 6, Strategy.ON_TOP_OF_COLLIDER);
               if (optionalEntity.isPresent()) {
                  this.playCaveSoundToSpelunkers(players, timer);
                  CaveDwellerEntity caveDweller = (CaveDwellerEntity)optionalEntity.get();
                  caveDweller.setInvisible(true);
                  caveDweller.hasSpawned = true;
                  timer.resetSpawnTimer();
               } else {
                  timer.currentVictim = null;
               }
            }

         }
      }
   }

   private void playCaveSoundToSpelunkers(List<ServerPlayer> players, Timer timer) {
      players.forEach((player) -> {
         ResourceLocation var10000;
         switch(RANDOM.nextInt(4)) {
         case 1:
            var10000 = ((SoundEvent)ModSounds.CAVENOISE_2.get()).getLocation();
            break;
         case 2:
            var10000 = ((SoundEvent)ModSounds.CAVENOISE_3.get()).getLocation();
            break;
         case 3:
            var10000 = ((SoundEvent)ModSounds.CAVENOISE_4.get()).getLocation();
            break;
         default:
            var10000 = ((SoundEvent)ModSounds.CAVENOISE_1.get()).getLocation();
         }

         ResourceLocation soundLocation = var10000;
         if (!(Boolean)ServerConfig.ONLY_PLAY_NOISE_TO_TARGET.get() || timer.currentVictim != null && player.is(timer.currentVictim)) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> {
               return player;
            }), new CaveSound(soundLocation, player.blockPosition(), 2.0F, 1.0F));
         }

      });
      timer.resetNoiseTimer();
   }

   private boolean isRelevantPlayer(ServerPlayer player) {
      if (!Utils.isValidPlayer(player)) {
         return false;
      } else if (player.position().y > (double)(Integer)ServerConfig.SPAWN_HEIGHT.get()) {
         return false;
      } else {
         Level serverLevel = player.level;
         int actualSkyLightLevel = serverLevel.getBrightness(LightLayer.SKY, player.blockPosition()) - serverLevel.getSkyDarken();
         float sunAngle = serverLevel.getSunAngle(1.0F);
         if (actualSkyLightLevel > 0) {
            float f1 = sunAngle < 3.1415927F ? 0.0F : 6.2831855F;
            sunAngle += (f1 - sunAngle) * 0.2F;
            actualSkyLightLevel = Math.round((float)actualSkyLightLevel * Mth.cos(sunAngle));
         }

         actualSkyLightLevel = Mth.clamp(actualSkyLightLevel, 0, 15);
         if (actualSkyLightLevel > (Integer)ServerConfig.SKY_LIGHT_LEVEL.get()) {
            return false;
         } else {
            LayerLightEventListener blockLighting = serverLevel.getLightEngine().getLayerListener(LightLayer.BLOCK);
            if (blockLighting.getLightValue(player.blockPosition()) > (Integer)ServerConfig.BLOCK_LIGHT_LEVEL.get()) {
               return false;
            } else {
               boolean isOnSurface = Utils.isOnSurface(player);
               if (isOnSurface) {
                  return !(Boolean)ServerConfig.ALLOW_SURFACE_SPAWN.get() ? false : ServerConfig.isInValidBiome(player);
               } else {
                  return true;
               }
            }
         }
      }
   }

   public static void speedUpTimers(String key, int spawnDelta, int noiseDelta) {
      Timer timer = (Timer)TIMERS.get(key);
      LOG.debug("Speeding up timers for the dimension [{}], timer: [{}]", key, timer);
      if (timer != null) {
         timer.currentSpawn += spawnDelta;
         timer.currentNoise += noiseDelta;
      }

   }
}
