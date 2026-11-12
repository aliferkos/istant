package br.com.fendas.gen;

import br.com.fendas.util.Weighted;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.List;

/** Paleta de blocos, mobs e nomes de cada tipo de andar. */
public final class Theme {

    public final String name;
    public final String color;          // tag MiniMessage, ex: "<aqua>"
    public final Weighted<Material> floor = new Weighted<>();
    public final Weighted<Material> wall = new Weighted<>();
    public final Weighted<Material> trim = new Weighted<>();
    public final Weighted<Material> cover = new Weighted<>();
    public final Weighted<EntityType> mobs = new Weighted<>();
    public Material ceiling, sub, light;
    public Material hazard;             // poças (lava, neve fofa...) - pode ser null
    public Material patch;              // manchas no piso - pode ser null
    public Material decor;              // enfeite (camada de neve, teia) - pode ser null
    public EntityType elite;
    public String eliteName;
    public List<String> lootSuffixes;

    private Theme(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public static final Theme GELO;
    public static final Theme RUINAS;
    public static final Theme ABISMO;
    public static final Theme INFERNO;
    public static final List<Theme> ALL;

    static {
        GELO = new Theme("Cavernas de Gelo", "<aqua>");
        GELO.floor.add(Material.SNOW_BLOCK, 60).add(Material.PACKED_ICE, 25).add(Material.POLISHED_DIORITE, 15);
        GELO.wall.add(Material.PACKED_ICE, 55).add(Material.BLUE_ICE, 20).add(Material.SNOW_BLOCK, 25);
        GELO.trim.add(Material.POLISHED_DIORITE, 50).add(Material.CALCITE, 50);
        GELO.cover.add(Material.PACKED_ICE, 50).add(Material.BLUE_ICE, 30).add(Material.SNOW_BLOCK, 20);
        GELO.ceiling = Material.PACKED_ICE;
        GELO.sub = Material.STONE;
        GELO.light = Material.SEA_LANTERN;
        GELO.hazard = Material.POWDER_SNOW;
        GELO.patch = Material.BLUE_ICE;
        GELO.decor = Material.SNOW;
        GELO.mobs.add(EntityType.STRAY, 35).add(EntityType.ZOMBIE, 25).add(EntityType.SKELETON, 15).add(EntityType.SPIDER, 15);
        GELO.elite = EntityType.ZOMBIE;
        GELO.eliteName = "<aqua><bold>Sentinela do Inverno";
        GELO.lootSuffixes = List.of("da Nevasca", "do Inverno Eterno", "do Gerador", "da Geada");

        RUINAS = new Theme("Ruínas Esquecidas", "<green>");
        RUINAS.floor.add(Material.STONE_BRICKS, 45).add(Material.CRACKED_STONE_BRICKS, 25)
                .add(Material.MOSSY_STONE_BRICKS, 20).add(Material.COBBLESTONE, 10);
        RUINAS.wall.add(Material.STONE_BRICKS, 40).add(Material.MOSSY_STONE_BRICKS, 35).add(Material.CRACKED_STONE_BRICKS, 25);
        RUINAS.trim.add(Material.CHISELED_STONE_BRICKS, 40).add(Material.POLISHED_ANDESITE, 60);
        RUINAS.cover.add(Material.MOSSY_COBBLESTONE, 50).add(Material.COBBLESTONE, 30).add(Material.STONE_BRICKS, 20);
        RUINAS.ceiling = Material.STONE_BRICKS;
        RUINAS.sub = Material.STONE;
        RUINAS.light = Material.GLOWSTONE;
        RUINAS.hazard = null;
        RUINAS.patch = Material.MOSS_BLOCK;
        RUINAS.decor = Material.COBWEB;
        RUINAS.mobs.add(EntityType.ZOMBIE, 30).add(EntityType.SKELETON, 30).add(EntityType.SPIDER, 20)
                .add(EntityType.BOGGED, 10).add(EntityType.VINDICATOR, 10);
        RUINAS.elite = EntityType.VINDICATOR;
        RUINAS.eliteName = "<green><bold>Carrasco das Ruínas";
        RUINAS.lootSuffixes = List.of("das Ruínas", "do Rei Esquecido", "da Última Cidade", "dos Antigos");

        ABISMO = new Theme("Abismo Profundo", "<dark_aqua>");
        ABISMO.floor.add(Material.DEEPSLATE_TILES, 40).add(Material.COBBLED_DEEPSLATE, 35).add(Material.POLISHED_DEEPSLATE, 25);
        ABISMO.wall.add(Material.DEEPSLATE_BRICKS, 50).add(Material.CRACKED_DEEPSLATE_BRICKS, 30).add(Material.DEEPSLATE_TILES, 20);
        ABISMO.trim.add(Material.POLISHED_BASALT, 50).add(Material.CHISELED_DEEPSLATE, 50);
        ABISMO.cover.add(Material.COBBLED_DEEPSLATE, 60).add(Material.DEEPSLATE_BRICKS, 40);
        ABISMO.ceiling = Material.DEEPSLATE_BRICKS;
        ABISMO.sub = Material.DEEPSLATE;
        ABISMO.light = Material.SEA_LANTERN;
        ABISMO.hazard = null;
        ABISMO.patch = Material.SCULK;
        ABISMO.decor = null;
        ABISMO.mobs.add(EntityType.ZOMBIE, 25).add(EntityType.SKELETON, 20).add(EntityType.CAVE_SPIDER, 20)
                .add(EntityType.BOGGED, 15).add(EntityType.WITCH, 10).add(EntityType.ZOMBIE_VILLAGER, 10);
        ABISMO.elite = EntityType.WITHER_SKELETON;
        ABISMO.eliteName = "<dark_aqua><bold>Devorador do Abismo";
        ABISMO.lootSuffixes = List.of("do Abismo", "das Profundezas", "do Eco", "do Vazio");

        INFERNO = new Theme("Forja Infernal", "<red>");
        INFERNO.floor.add(Material.BLACKSTONE, 40).add(Material.POLISHED_BLACKSTONE, 35).add(Material.NETHER_BRICKS, 25);
        INFERNO.wall.add(Material.POLISHED_BLACKSTONE_BRICKS, 55).add(Material.CRACKED_POLISHED_BLACKSTONE_BRICKS, 35)
                .add(Material.GILDED_BLACKSTONE, 10);
        INFERNO.trim.add(Material.BASALT, 50).add(Material.CHISELED_POLISHED_BLACKSTONE, 50);
        INFERNO.cover.add(Material.BLACKSTONE, 50).add(Material.NETHER_BRICKS, 50);
        INFERNO.ceiling = Material.BLACKSTONE;
        INFERNO.sub = Material.NETHERRACK;
        INFERNO.light = Material.SHROOMLIGHT;
        INFERNO.hazard = Material.LAVA;
        INFERNO.patch = Material.MAGMA_BLOCK;
        INFERNO.decor = null;
        INFERNO.mobs.add(EntityType.WITHER_SKELETON, 25).add(EntityType.HUSK, 25).add(EntityType.BLAZE, 20)
                .add(EntityType.MAGMA_CUBE, 15).add(EntityType.SKELETON, 15);
        INFERNO.elite = EntityType.PIGLIN_BRUTE;
        INFERNO.eliteName = "<red><bold>Guardião das Brasas";
        INFERNO.lootSuffixes = List.of("das Brasas", "do Inferno", "da Fornalha", "das Cinzas");

        ALL = List.of(GELO, RUINAS, ABISMO, INFERNO);
    }
}
