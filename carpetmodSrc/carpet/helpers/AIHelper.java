package carpet.helpers;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AIHelper {
    private static final WeakHashMap<EntityAITasks, EntityLiving> TASK_TO_ENTITY_MAP = new WeakHashMap<>();
    private static final WeakHashMap<EntityLiving, WeakHashMap<EntityAIBase, String>> DETAILED_INFO = new WeakHashMap<>();

    public static Stream<EntityAIBase> getCurrentTasks(EntityLiving e) {
        return e.tasks.executingTaskEntries.stream()
                .sorted((a, b) -> b.priority - a.priority)
                .map(entry -> entry.action);
    }

    public static Stream<String> getCurrentTaskNames(EntityLiving e, Map<EntityAIBase, String> details) {
        return getCurrentTasks(e).map(task -> getTaskName(task, details));
    }

    public static String getTaskName(EntityAIBase task) {
        return getTaskName(task, null);
    }

    public static String getTaskName(EntityAIBase task, Map<EntityAIBase, String> details) {
        String detailsInfo = details != null && details.containsKey(task) ? ": " + details.get(task) : "";
        String className = CarpetServer.DEOBFUSCATOR.hasClassNames()
                ? CarpetServer.DEOBFUSCATOR.deobfuscate(task.getClass()).replaceFirst("net/minecraft/entity/ai/EntityAI", "")
                : "UNKNOWN";
        return className + detailsInfo;
    }

    public static String getInfo(EntityAITasks tasks, EntityAIBase task) {
        return "Entity: " + getName(getOwner(tasks)) + ", Task: " + getTaskName(task);
    }

    public static String getName(@Nullable Entity entity) {
        if (entity == null) return "unknown";
        if (!entity.hasCustomName()) return entity.getName();
        String id = EntityList.getEntityString(entity);
        if (id == null) id = "generic";
        return I18n.translateToLocal("entity." + id + ".name");
    }

    public static Optional<String> formatCurrentTasks(EntityLiving e, Map<EntityAIBase, String> details) {
        if (!CarpetServer.DEOBFUSCATOR.hasClassNames()) return Optional.empty();
        return Optional.of(getCurrentTaskNames(e, details).collect(Collectors.joining(",")));
    }

    @Nullable
    public static EntityLiving getOwner(EntityAITasks tasks) {
        return TASK_TO_ENTITY_MAP.computeIfAbsent(tasks, t -> {
            for (WorldServer world : CarpetServer.minecraft_server.worlds) {
                for (EntityLiving e : world.getEntities(EntityLiving.class, x -> true)) {
                    if (e.tasks == tasks) return e;
                }
            }
            return null;
        });
    }

    public static void update(EntityAITasks tasks) {
        if (!CarpetSettings.displayMobAI) return;
        EntityLiving owner = getOwner(tasks);
        if (owner == null) return;
        Map<EntityAIBase, String> details = DETAILED_INFO.get(owner);
        Optional<String> formatted = formatCurrentTasks(owner, details);
        if (!formatted.isPresent()) return;
        owner.setCustomNameTag(formatted.get());
    }

    public static void setDetailedInfo(EntityLiving owner, EntityAIBase task, String info) {
        DETAILED_INFO.computeIfAbsent(owner, x -> new WeakHashMap<>()).put(task, info);
        TASK_TO_ENTITY_MAP.put(owner.tasks, owner);
        update(owner.tasks);
    }
}
