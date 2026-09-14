package utils;

import java.util.ArrayList;
import java.util.Collection;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

public class InvStore {

    public Player player;
    public double health;
    public int fire;
    public int food;
    public int level;
    public float exp;
    public Collection<PotionEffect> activePotionEffects;

    public InvStore(Player player) {

        this.player = player;
        this.exp = 0.0F;
        this.level = 0;
        this.health = 0.0D;
        this.food = 0;
        this.fire = 0;
        this.activePotionEffects = new ArrayList<>();

    }

    public void load() {

        for (PotionEffect effect : this.player.getActivePotionEffects()) {

            this.player.removePotionEffect(effect.getType());

        }

        this.player.addPotionEffects(this.activePotionEffects);

        this.player.setExp(this.exp);
        this.player.setLevel(this.level);
        this.player.setFoodLevel(this.food);
        this.player.setFireTicks(this.fire);

        final Attribute maxHealthAttribute = Attribute.GENERIC_MAX_HEALTH;
        final double maxHealth = this.player.getAttribute(maxHealthAttribute) != null
                ? this.player.getAttribute(maxHealthAttribute).getValue()
                : 20.0D;
        final double restoredHealth = this.health > 0.0D ? this.health : maxHealth;
        this.player.setHealth(Math.min(restoredHealth, maxHealth));

        // No gamemode restore here: load() runs after the player is back in the
        // overworld, where MyWorlds already re-applied the saved gamemode, and a
        // second change there is exactly the flip GameModeInventories-OG answers
        // by swapping the real inventory out. No scoreboard reset either:
        // LobbyScoreboard.detach() has already handed the board back.
        this.player.updateInventory();

    }

    public void save() {

        this.exp = this.player.getExp();
        this.level = this.player.getLevel();
        this.food = this.player.getFoodLevel();
        this.fire = this.player.getFireTicks();
        this.health = this.player.getHealth();
        this.activePotionEffects = new ArrayList<>(this.player.getActivePotionEffects());
        this.player.updateInventory();

    }

    public void reset() {

        this.exp = 0.0F;
        this.level = 0;
        this.health = 0.0D;
        this.food = 0;
        this.fire = 0;
        this.activePotionEffects = new ArrayList<>();

    }

}
