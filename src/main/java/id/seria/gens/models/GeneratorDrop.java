package id.seria.gens.models;

import org.bukkit.inventory.ItemStack;

public class GeneratorDrop {
    
    private final String id;
    private final int chance;
    private final double value;
    private final ItemStack item;
    
    public GeneratorDrop(String id, int chance, double value, ItemStack item) {
        this.id = id;
        this.chance = chance;
        this.value = value;
        this.item = item;
    }
    
    public String getId() { return id; }
    public int getChance() { return chance; }
    public double getValue() { return value; }
    public ItemStack getItem() { return item.clone(); }
    
    @Override
    public String toString() {
        return "GeneratorDrop{" +
                "id='" + id + '\'' +
                ", chance=" + chance +
                ", value=" + value +
                ", item=" + item.getType() +
                '}';
    }
}