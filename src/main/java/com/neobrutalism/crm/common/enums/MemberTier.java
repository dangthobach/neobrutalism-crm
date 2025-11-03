package com.neobrutalism.crm.common.enums;

/**
 * Member tier enumeration for membership levels
 */
public enum MemberTier {
    /**
     * Free tier - basic access
     */
    FREE("Free", "Basic access", 0, "#94A3B8"),

    /**
     * Silver tier - intermediate access
     */
    SILVER("Silver", "Intermediate access", 1, "#C0C0C0"),

    /**
     * Gold tier - advanced access
     */
    GOLD("Gold", "Advanced access", 2, "#FFD700"),

    /**
     * VIP tier - premium access
     */
    VIP("VIP", "Premium access", 3, "#9333EA");

    private final String displayName;
    private final String description;
    private final int level;
    private final String color;

    MemberTier(String displayName, String description, int level, String color) {
        this.displayName = displayName;
        this.description = description;
        this.level = level;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getLevel() {
        return level;
    }

    public String getColor() {
        return color;
    }

    /**
     * Check if this tier is higher than another tier
     */
    public boolean isHigherThan(MemberTier other) {
        return this.level > other.level;
    }

    /**
     * Check if this tier is higher or equal to another tier
     */
    public boolean isHigherOrEqualTo(MemberTier other) {
        return this.level >= other.level;
    }

    /**
     * Check if user with this tier can access content requiring specified tier
     */
    public boolean canAccess(MemberTier requiredTier) {
        return this.level >= requiredTier.level;
    }

    /**
     * Get all tiers accessible to this tier level
     * For example: GOLD can access FREE, SILVER, and GOLD tiers
     */
    public static java.util.List<MemberTier> getAccessibleTiers(MemberTier userTier) {
        java.util.List<MemberTier> accessible = new java.util.ArrayList<>();
        for (MemberTier tier : values()) {
            if (userTier.canAccess(tier)) {
                accessible.add(tier);
            }
        }
        return accessible;
    }
}
