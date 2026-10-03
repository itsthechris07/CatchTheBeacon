package com.christian34.catchthebeacon.game;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

/**
 * What players with ctb.vip get - nothing that helps in the fight (servers often sell VIP ranks):
 * <ul>
 *     <li>/ctb start and the start item in the lobby</li>
 *     <li>into a full lobby: the last player without VIP makes room ({@link Game#join(com.christian34.catchthebeacon.user.GamePlayer)})</li>
 *     <li>their vote for the variant counts twice ({@link Game#getVotes(Variant)})</li>
 *     <li>a highlighted join message, particles on kills and a golden firework on a win</li>
 * </ul>
 *
 * @author Christian34
 */
public final class VipPerks {
    public static final String PERMISSION = "ctb.vip";
    public static final int VOTE_WEIGHT = 2;

    private VipPerks() {
    }

    public static boolean isVip(Player player) {
        return player.hasPermission(PERMISSION);
    }

    /**
     * the killer is a VIP: a burst of particles in the color of his team where the victim died
     */
    public static void playKillEffect(Player killer, Location location, Team team) {
        if (!isVip(killer) || location.getWorld() == null) return;
        Location center = location.clone().add(0, 1, 0);
        Color color = team == null ? Color.WHITE : team.getLeatherColor();
        center.getWorld().spawnParticle(Particle.DUST, center, 40, 0.4, 0.6, 0.4, new Particle.DustOptions(color, 1.5f));
        center.getWorld().spawnParticle(Particle.END_ROD, center, 15, 0.2, 0.4, 0.2, 0.08);
    }

    /**
     * a VIP of the winning team: an extra golden firework above him
     */
    public static void launchWinFirework(Player player, Color teamColor) {
        if (!isVip(player)) return;
        Location location = player.getLocation().add(0, 2, 0);
        location.getWorld().spawn(location, Firework.class, firework -> {
            FireworkMeta meta = firework.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder()
                    .with(FireworkEffect.Type.BURST)
                    .withColor(Color.fromRGB(0xFFD700), teamColor)
                    .withFade(Color.WHITE)
                    .flicker(true)
                    .trail(true)
                    .build());
            meta.setPower(1);
            firework.setFireworkMeta(meta);
        });
    }

}
