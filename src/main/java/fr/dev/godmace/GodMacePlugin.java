package fr.dev.godmace;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public final class GodMacePlugin extends JavaPlugin implements Listener {

    private NamespacedKey maceKey;
    private NamespacedKey killsKey;

    // Prefabs de textes Adventure pré-compilés en mémoire (Optimisation GC)
    private static final Component TITLE_ORDINAIRE = Component.text("Masse Ordinaire", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    private static final Component TITLE_GOD = Component.text("⚡ MASSE DES DIEUX ⚡", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
    private static final Component LORE_GOD_POWER = Component.text("Pouvoir : Banni quiconque est tué !", NamedTextColor.RED, TextDecoration.ITALIC);

    @Override
    public void onEnable() {
        this.maceKey = new NamespacedKey(this, "is_god_mace");
        this.killsKey = new NamespacedKey(this, "mace_kills");

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("GodMacePlugin 1.1.0 prêt et optimisé !");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!hasMace(player)) {
            player.getInventory().addItem(createBaseMace());
            player.sendMessage(Component.text("⚡ Tu as reçu ta Masse Ordinaire !", NamedTextColor.YELLOW));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // 1. SUPPRESSION DE LA MASSE PERDUE LORS DE LA MORT (Nettoyage des drops)
        event.getDrops().removeIf(this::isMaceItem);

        // 2. GESTION DU KILL PAR UN AUTRE JOUEUR
        if (killer != null && killer != victim) {
            ItemStack itemInHand = killer.getInventory().getItemInMainHand();

            if (isMaceItem(itemInHand)) {
                ItemMeta meta = itemInHand.getItemMeta();
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                int currentKills = pdc.getOrDefault(killsKey, PersistentDataType.INTEGER, 0);

                if (currentKills >= 10) {
                    // La Masse des Dieux banni la victime
                    executeBan(victim, killer);
                } else {
                    // Évolution de la masse
                    int newKills = currentKills + 1;
                    applyMaceEvolution(killer, itemInHand, meta, newKills);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        // Le joueur réapparaît : on lui donne une masse réinitialisée (0 kills)
        Bukkit.getScheduler().runTask(this, () -> {
            if (!hasMace(player)) {
                player.getInventory().addItem(createBaseMace());
                player.sendMessage(Component.text("💀 Tu es mort ! Ta masse a été réinitialisée.", NamedTextColor.RED));
            }
        });
    }

    private ItemStack createBaseMace() {
        ItemStack mace = new ItemStack(Material.MACE);
        ItemMeta meta = mace.getItemMeta();
        if (meta != null) {
            meta.displayName(TITLE_ORDINAIRE);
            meta.setCustomModelData(1001); // ID Texture de base

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(maceKey, PersistentDataType.BOOLEAN, true);
            pdc.set(killsKey, PersistentDataType.INTEGER, 0);

            meta.lore(List.of(
                Component.text("Kills : ", NamedTextColor.DARK_GRAY)
                    .append(Component.text("0/10", NamedTextColor.GREEN))
            ));

            mace.setItemMeta(meta);
        }
        return mace;
    }

    private void applyMaceEvolution(Player killer, ItemStack mace, ItemMeta meta, int kills) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(killsKey, PersistentDataType.INTEGER, kills);

        // Model Data évolutif (1001 à 1010)
        meta.setCustomModelData(1000 + kills);

        switch (kills) {
            case 1 -> {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                killer.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 20 * 120, 0));
            }
            case 2 -> {
                meta.addEnchant(Enchantment.UNBREAKING, 2, true);
                killer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 120, 0));
            }
            case 3 -> {
                meta.addEnchant(Enchantment.DENSITY, 1, true);
                killer.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20 * 120, 0));
            }
            case 4 -> {
                meta.addEnchant(Enchantment.BREACH, 1, true);
                killer.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 180, 0));
            }
            case 5 -> {
                meta.addEnchant(Enchantment.DENSITY, 2, true);
                meta.addEnchant(Enchantment.BREACH, 2, true);
                killer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 100, 0));
            }
            case 6 -> {
                meta.addEnchant(Enchantment.WIND_BURST, 1, true);
                killer.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 180, 0));
            }
            case 7 -> {
                meta.addEnchant(Enchantment.DENSITY, 3, true);
                meta.addEnchant(Enchantment.BREACH, 3, true);
                killer.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 120, 0));
            }
            case 8 -> {
                meta.addEnchant(Enchantment.DENSITY, 4, true);
                meta.addEnchant(Enchantment.BREACH, 4, true);
                killer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 180, 1));
            }
            case 9 -> {
                meta.addEnchant(Enchantment.DENSITY, 5, true);
                meta.addEnchant(Enchantment.BREACH, 4, true);
                killer.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 120, 1));
            }
            case 10 -> {
                meta.displayName(TITLE_GOD);
                meta.addEnchant(Enchantment.BREACH, 5, true);
                meta.addEnchant(Enchantment.DENSITY, 6, true);
                meta.addEnchant(Enchantment.WIND_BURST, 3, true);
                meta.addEnchant(Enchantment.UNBREAKING, 5, true);

                killer.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 600, 1)); // Résistance II
                killer.getWorld().playSound(killer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                
                Bukkit.broadcast(Component.text("⚡ " + killer.getName() + " A FORGÉ LA MASSE DES DIEUX !", NamedTextColor.GOLD, TextDecoration.BOLD));
            }
        }

        // Mise à jour du Lore
        if (kills >= 10) {
            meta.lore(List.of(
                Component.text("Étape : ", NamedTextColor.GRAY).append(Component.text("Divinité Obtenue", NamedTextColor.GOLD)),
                LORE_GOD_POWER
            ));
        } else {
            meta.lore(List.of(
                Component.text("Kills : ", NamedTextColor.DARK_GRAY).append(Component.text(kills + "/10", NamedTextColor.GREEN))
            ));
        }

        mace.setItemMeta(meta);
    }

    private void executeBan(Player victim, Player killer) {
        String banReason = "Tu as été pulvérisé par la Masse des Dieux de " + killer.getName() + " !";
        Bukkit.getBanList(BanList.Type.NAME).addBan(
                victim.getName(),
                banReason,
                null,
                "Masse des Dieux"
        );
        victim.kick(Component.text(banReason, NamedTextColor.RED));
        Bukkit.broadcast(Component.text("⚡ " + victim.getName() + " a été banni par la Masse des Dieux de " + killer.getName() + " !", NamedTextColor.DARK_RED));
    }

    private boolean isMaceItem(ItemStack item) {
        if (item == null || item.getType() != Material.MACE) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(maceKey, PersistentDataType.BOOLEAN);
    }

    private boolean hasMace(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isMaceItem(item)) return true;
        }
        return false;
    }
}
