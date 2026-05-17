package com.nextgenlab.game.crafting;

public enum ItemType {
    PISTOL       ("Pistol",       true,  "items/icon_pistol.png"),
    STUN_GUN     ("Stun Gun",     true,  "items/icon_stun_gun.png"),
    ACID_GRENADE ("Acid Grenade", true,  "items/icon_acid_grenade.png"),
    TASER        ("Taser",        true,  "items/icon_taser.png"),
    RAIL_GUN     ("Rail Gun",     true,  "items/icon_rail_gun.png"),
    HEAL_KIT     ("Heal Kit",     false, "items/icon_heal_kit.png"),
    TRAP         ("Trap",         false, "items/icon_trap.png"),
    DECOY        ("Decoy",        false, "items/icon_decoy.png"),
    AMMO_PACK    ("Ammo Pack",   false, "items/icon_ammo_pack.png");

    public final String displayName;
    public final boolean isWeapon;
    public final String iconPath;

    ItemType(String displayName, boolean isWeapon, String iconPath) {
        this.displayName = displayName;
        this.isWeapon    = isWeapon;
        this.iconPath    = iconPath;
    }
}
