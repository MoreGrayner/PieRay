신호기 기반 파일런 시스템 팀장은 OP 플레이어가 /master add nickname으로 배정 가능. 팀장은 신호기 설치가 가능함. 신호기에는 다음과 같은 기능이 있음. * 산호기 업그레이드 정도에 따른(마인크래트프트 신호기는 밑에 깔린 광물 층 수에 따라 레벨이 결정됨) 팀원 전체 강화 버프 1초 10틱마다, 범위 내의 적군에는 나약함 (신호기레벨)과 구속 (신호기레벨)의 디버프를 주며 기존 신호기의 버프 효과는 제거됨. * 신호기를 팀원 또는 팀장이 우클릭 시 ui가 생성됨: 9칸의 1줄짜리 인벤토리이며 여기에는 각각 1, 3, 5, 7번째 칸에 스페어바디(구매 시 팀원이 사망하는것 또는 스왑 명령어를 사용해 위치를 스왑할 수 있는 스페어 바디 지급, 이 스페어바디는 캘 수 있으며, 들고 돌아다니다가 일반적인 블록처럼 설치 가능(아머스탠드로 구현, 외형은 생돌 블록으로 고정)하며 사망으로 인해 부활하면 가장 먼저 생성된 스페어바디로 텔레포트되며 파괴됨. 스페어바디는 명령어로 이름 지정 가능(스왑시도시 인자로 받음), 공동창고(팀원끼리 공유하는 공동창고 엑세스), 경험치 상점(각종 방어구나 장비, 소모품(물약, 음식, 랜덤 레벨 인첸트북 등등) 구입 가능), 정찰(클릭시 5분 뒤에 사라지는 겉날개와 폭죽 32개 지급(사라지기 30초와 10초, 5초전에 알림줌). 매우 많은 경험치를 요구함(쿨타임은 없음), 경험치 상점 및 모든 요소는 경험치를 사용해 구매하며 싼건 100XP 정도의 수치를 깎지만 비싼건 30레벨에서 50레벨정도를 고정적으로 깎음. 경험치 팜은 가능하나 엔더드래곤과 광물을 제외한 모든 경험치 수급량이 절반으로 감소함. 그리고 경험치 상점은 오늘의 핫딜!이라는 항목과 고정 판매창이 있음. 핫딜은 설정파일로 직접 설정할 수도 있고 설정되어있지 않으면 플러그인에서 자동으로 랜덤으로 골라서 무조건 최고레벨로 고정한, 소모품의 경우 품목당 16개정도로 제한을 둔 형태로 원본 가격보다 30퍼센트 -  70퍼센트 가격을 깎아 판매함.

커스텀 인첸트도 추가됨. 이는 인첸트북 형태로 상점에서 최고레벨(1-3)을 확정적으로 판매할 수도 있음(핫딜에서만 판매), 그리고 간혹가다 인첸트 테이블에서 뜰 수도 있음,
목록은 대충 깃털걸음(기본 이동속도 attribute와 점프력 약간 상승, 넉백받는정도 1.3배), 흡혈(크리티컬 타격 시 입힌 피해량의 10%만큼 체력회복), 자동복구(2초마다 플레이어의 xp 수치를 70 깎아 내구도 회복), 표식(활, 무기 등등: 타격시 표식을 남김, 표식이 남겨진 적에게 추가 공격을 가하면 원래 대미지의 1.5배의 대미지가 들어가며 표식은 사라짐. 표식이 부여된 상대에게는 발광이 상시 적용되며(제거될시 같이 제거됨), 표식 인첸트가 있는 무기로 지속 타격시 표식은 계속 유지됨), 고통 분담(10초마다 손실된 체력의 10배 내구도를 희생해 체력복구) - 이 인첸트들은 3단계까지 있으며 패널티는 레벨에 따른 변동없음, 대신 어드벤티지는 레벨에 따라 변동됨, 저건 최대수치기준



package com.beaconpylon.managers;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.*;

public class ShopManager {

    private final BeaconPylonSystem plugin;
    private final Map<String, ShopItem> regularItems;
    private final List<ShopItem> hotDeals;
    private final Random random;
    
    public ShopManager(BeaconPylonSystem plugin) {
        this.plugin = plugin;
        this.regularItems = new HashMap<>();
        this.hotDeals = new ArrayList<>();
        this.random = new Random();
        
        initializeShopItems();
        refreshHotDeals();
    }
    
    private void initializeShopItems() {
        // 방어구
        addItem("diamond_helmet", Material.DIAMOND_HELMET, 5000, "다이아몬드 투구");
        addItem("diamond_chestplate", Material.DIAMOND_CHESTPLATE, 8000, "다이아몬드 흉갑");
        addItem("diamond_leggings", Material.DIAMOND_LEGGINGS, 7000, "다이아몬드 레깅스");
        addItem("diamond_boots", Material.DIAMOND_BOOTS, 4000, "다이아몬드 부츠");
        
        // 무기
        addItem("diamond_sword", Material.DIAMOND_SWORD, 6000, "다이아몬드 검");
        addItem("diamond_axe", Material.DIAMOND_AXE, 5500, "다이아몬드 도끼");
        addItem("bow", Material.BOW, 3000, "활");
        addItem("crossbow", Material.CROSSBOW, 4000, "석궁");
        
        // 도구
        addItem("diamond_pickaxe", Material.DIAMOND_PICKAXE, 5000, "다이아몬드 곡괭이");
        addItem("diamond_shovel", Material.DIAMOND_SHOVEL, 3000, "다이아몬드 삽");
        
        // 소모품 - 물약
        addItem("healing_potion", createPotion(PotionEffectType.INSTANT_HEALTH, 2), 500, "즉시 회복 물약 II");
        addItem("strength_potion", createPotion(PotionEffectType.STRENGTH, 2), 800, "힘 물약 II");
        addItem("speed_potion", createPotion(PotionEffectType.SPEED, 2), 600, "신속 물약 II");
        addItem("regen_potion", createPotion(PotionEffectType.REGENERATION, 2), 700, "재생 물약 II");
        addItem("fire_res_potion", createPotion(PotionEffectType.FIRE_RESISTANCE, 1), 500, "화염 저항 물약");
        
        // 소모품 - 음식
        addItem("golden_apple", Material.GOLDEN_APPLE, 300, "황금 사과", 8);
        addItem("enchanted_golden_apple", Material.ENCHANTED_GOLDEN_APPLE, 2000, "마법이 부여된 황금 사과", 2);
        addItem("cooked_beef", Material.COOKED_BEEF, 100, "구운 소고기", 16);
        
        // 기타
        addItem("ender_pearl", Material.ENDER_PEARL, 200, "엔더 진주", 16);
        addItem("arrow", Material.ARROW, 50, "화살", 64);
        addItem("totem", Material.TOTEM_OF_UNDYING, 15000, "불사의 토템");
    }
    
    private void addItem(String id, Material material, int xpCost, String displayName) {
        addItem(id, new ItemStack(material), xpCost, displayName, 1);
    }
    
    private void addItem(String id, Material material, int xpCost, String displayName, int amount) {
        ItemStack item = new ItemStack(material, amount);
        addItem(id, item, xpCost, displayName, amount);
    }
    
    private void addItem(String id, ItemStack item, int xpCost, String displayName, int amount) {
        ShopItem shopItem = new ShopItem(id, item, xpCost, displayName, amount);
        regularItems.put(id, shopItem);
    }
    
    private ItemStack createPotion(PotionEffectType type, int amplifier) {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.addCustomEffect(new PotionEffect(type, 3600, amplifier - 1), true);
        potion.setItemMeta(meta);
        return potion;
    }
    
    public void refreshHotDeals() {
        hotDeals.clear();
        
        // 설정 파일에서 핫딜 읽기
        List<String> configHotDeals = plugin.getConfig().getStringList("hot_deals");
        
        if (!configHotDeals.isEmpty()) {
            for (String itemId : configHotDeals) {
                ShopItem item = regularItems.get(itemId);
                if (item != null) {
                    int discount = random.nextInt(41) + 30; // 30-70% 할인
                    hotDeals.add(item.createHotDeal(discount));
                }
            }
        } else {
            // 랜덤으로 4개 선택
            List<ShopItem> allItems = new ArrayList<>(regularItems.values());
            Collections.shuffle(allItems);
            
            for (int i = 0; i < Math.min(4, allItems.size()); i++) {
                ShopItem item = allItems.get(i);
                int discount = random.nextInt(41) + 30; // 30-70% 할인
                hotDeals.add(item.createHotDeal(discount));
            }
        }
        
        Bukkit.broadcastMessage(ChatColor.GOLD + "★ 오늘의 핫딜이 갱신되었습니다! ★");
    }
    
    public List<ShopItem> getHotDeals() {
        return new ArrayList<>(hotDeals);
    }
    
    public Map<String, ShopItem> getRegularItems() {
        return new HashMap<>(regularItems);
    }
    
    public boolean purchaseItem(Player player, ShopItem item) {
        int playerXP = getTotalExperience(player);
        
        if (playerXP < item.getXpCost()) {
            player.sendMessage(ChatColor.RED + "경험치가 부족합니다! 필요: " + item.getXpCost() + " XP, 보유: " + playerXP + " XP");
            return false;
        }
        
        // 경험치 차감
        setTotalExperience(player, playerXP - item.getXpCost());
        
        // 아이템 지급
        ItemStack purchasedItem = item.getItem().clone();
        player.getInventory().addItem(purchasedItem);
        
        player.sendMessage(ChatColor.GREEN + item.getDisplayName() + "을(를) 구매했습니다! (-" + item.getXpCost() + " XP)");
        return true;
    }
    
    public int getTotalExperience(Player player) {
        int exp = Math.round(player.getExpToLevel() * player.getExp());
        int currentLevel = player.getLevel();
        
        while (currentLevel > 0) {
            currentLevel--;
            exp += getExpToLevel(currentLevel);
        }
        
        return exp;
    }
    
    public void setTotalExperience(Player player, int exp) {
        player.setLevel(0);
        player.setExp(0);
        player.giveExp(exp);
    }
    
    private int getExpToLevel(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        } else if (level <= 30) {
            return 5 * level - 38;
        } else {
            return 9 * level - 158;
        }
    }
    
    public static class ShopItem {
        private final String id;
        private final ItemStack item;
        private final int xpCost;
        private final String displayName;
        private final int amount;
        private final boolean isHotDeal;
        private final int discount;
        
        public ShopItem(String id, ItemStack item, int xpCost, String displayName, int amount) {
            this(id, item, xpCost, displayName, amount, false, 0);
        }
        
        private ShopItem(String id, ItemStack item, int xpCost, String displayName, int amount, boolean isHotDeal, int discount) {
            this.id = id;
            this.item = item;
            this.xpCost = xpCost;
            this.displayName = displayName;
            this.amount = amount;
            this.isHotDeal = isHotDeal;
            this.discount = discount;
        }
        
        public ShopItem createHotDeal(int discount) {
            int discountedPrice = (int) (xpCost * (100 - discount) / 100.0);
            return new ShopItem(id, item, discountedPrice, displayName, amount, true, discount);
        }
        
        public String getId() {
            return id;
        }
        
        public ItemStack getItem() {
            return item.clone();
        }
        
        public int getXpCost() {
            return xpCost;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public int getAmount() {
            return amount;
        }
        
        public boolean isHotDeal() {
            return isHotDeal;
        }
        
        public int getDiscount() {
            return discount;
        }
        
        public ItemStack getDisplayItem() {
            ItemStack display = item.clone();
            ItemMeta meta = display.getItemMeta();
            
            meta.setDisplayName(ChatColor.YELLOW + displayName);
            List<String> lore = new ArrayList<>();
            
            if (isHotDeal) {
                lore.add(ChatColor.GOLD + "★ 오늘의 핫딜! " + discount + "% 할인 ★");
            }
            
            lore.add(ChatColor.GRAY + "가격: " + ChatColor.GREEN + xpCost + " XP");
            
            if (amount > 1) {
                lore.add(ChatColor.GRAY + "수량: " + ChatColor.WHITE + amount + "개");
            }
            
            lore.add("");
            lore.add(ChatColor.YELLOW + "클릭하여 구매");
            
            meta.setLore(lore);
            display.setItemMeta(meta);
            
            return display;
        }
    }
}

package com.beaconpylon.managers;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;

public class EnchantmentManager {

    private final BeaconPylonSystem plugin;
    private final NamespacedKey featherStepKey;
    private final NamespacedKey vampirismKey;
    private final NamespacedKey autoRepairKey;
    private final NamespacedKey markKey;
    private final NamespacedKey painShareKey;
    private final Map<UUID, Long> markedPlayers; // 표식이 남겨진 플레이어
    
    public EnchantmentManager(BeaconPylonSystem plugin) {
        this.plugin = plugin;
        this.featherStepKey = new NamespacedKey(plugin, "feather_step");
        this.vampirismKey = new NamespacedKey(plugin, "vampirism");
        this.autoRepairKey = new NamespacedKey(plugin, "auto_repair");
        this.markKey = new NamespacedKey(plugin, "mark");
        this.painShareKey = new NamespacedKey(plugin, "pain_share");
        this.markedPlayers = new HashMap<>();
    }
    
    public void registerCustomEnchantments() {
        // 커스텀 인챈트는 PersistentDataContainer를 통해 구현
        Bukkit.getLogger().info("커스텀 인챈트 시스템 등록 완료");
    }
    
    public ItemStack createEnchantedBook(CustomEnchantType type, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + type.getDisplayName() + " " + getRomanNumeral(level));
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + type.getDescription(level));
        lore.add("");
        lore.add(ChatColor.YELLOW + "적용 가능: " + ChatColor.WHITE + type.getApplicableItems());
        lore.add(ChatColor.RED + "패널티: " + ChatColor.WHITE + type.getPenalty());
        
        meta.setLore(lore);
        
        // 커스텀 인챈트 데이터 저장
        meta.getPersistentDataContainer().set(
            getKeyForType(type),
            PersistentDataType.INTEGER,
            level
        );
        
        book.setItemMeta(meta);
        return book;
    }
    
    public boolean hasCustomEnchant(ItemStack item, CustomEnchantType type) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
            .has(getKeyForType(type), PersistentDataType.INTEGER);
    }
    
    public int getCustomEnchantLevel(ItemStack item, CustomEnchantType type) {
        if (!hasCustomEnchant(item, type)) return 0;
        return item.getItemMeta().getPersistentDataContainer()
            .get(getKeyForType(type), PersistentDataType.INTEGER);
    }
    
    public void applyCustomEnchant(ItemStack item, CustomEnchantType type, int level) {
        if (item == null || !item.hasItemMeta()) return;
        
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(
            getKeyForType(type),
            PersistentDataType.INTEGER,
            level
        );
        
        // Lore에 인챈트 정보 추가
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        lore.add(ChatColor.LIGHT_PURPLE + type.getDisplayName() + " " + getRomanNumeral(level));
        meta.setLore(lore);
        
        item.setItemMeta(meta);
    }
    
    private NamespacedKey getKeyForType(CustomEnchantType type) {
        switch (type) {
            case FEATHER_STEP: return featherStepKey;
            case VAMPIRISM: return vampirismKey;
            case AUTO_REPAIR: return autoRepairKey;
            case MARK: return markKey;
            case PAIN_SHARE: return painShareKey;
            default: return featherStepKey;
        }
    }
    
    private String getRomanNumeral(int level) {
        switch (level) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            default: return String.valueOf(level);
        }
    }
    
    public void markPlayer(UUID playerUUID) {
        markedPlayers.put(playerUUID, System.currentTimeMillis());
    }
    
    public boolean isMarked(UUID playerUUID) {
        return markedPlayers.containsKey(playerUUID);
    }
    
    public void removeMark(UUID playerUUID) {
        markedPlayers.remove(playerUUID);
    }
    
    public Map<UUID, Long> getMarkedPlayers() {
        return markedPlayers;
    }
    
    public enum CustomEnchantType {
        FEATHER_STEP("깃털걸음", "boots"),
        VAMPIRISM("흡혈", "sword, axe"),
        AUTO_REPAIR("자동복구", "all"),
        MARK("표식", "sword, axe, bow"),
        PAIN_SHARE("고통 분담", "armor");
        
        private final String displayName;
        private final String applicableItems;
        
        CustomEnchantType(String displayName, String applicableItems) {
            this.displayName = displayName;
            this.applicableItems = applicableItems;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getApplicableItems() {
            return applicableItems;
        }
        
        public String getDescription(int level) {
            switch (this) {
                case FEATHER_STEP:
                    double speedBonus = 0.05 * level;
                    return "이동속도 +" + (speedBonus * 100) + "%, 점프력 증가, 넉백 1.3배";
                case VAMPIRISM:
                    int healPercent = 10 * level;
                    return "크리티컬 시 피해량의 " + healPercent + "% 회복";
                case AUTO_REPAIR:
                    return "2초마다 70 XP를 소모하여 내구도 회복";
                case MARK:
                    double damageMultiplier = 1.0 + (0.5 * level);
                    return "타격 시 표식 부여, 표식된 대상에게 " + damageMultiplier + "배 피해";
                case PAIN_SHARE:
                    int healPercent2 = 10 * level;
                    return "10초마다 손실 체력의 " + healPercent2 + "% 회복 (내구도 10배 소모)";
                default:
                    return "";
            }
        }
        
        public String getPenalty() {
            switch (this) {
                case FEATHER_STEP:
                    return "넉백 저항 감소";
                case VAMPIRISM:
                    return "크리티컬 타격 시에만 발동";
                case AUTO_REPAIR:
                    return "지속적인 경험치 소모";
                case MARK:
                    return "표식은 한 번 발동 후 사라짐";
                case PAIN_SHARE:
                    return "내구도 소모가 매우 큼";
                default:
                    return "없음";
            }
        }
    }
}

package com.beaconpylon.listeners;

import com.beaconpylon.BeaconPylonSystem;
import com.beaconpylon.managers.*;
import org.bukkit.*;
import org.bukkit.block.Beacon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public class BeaconListener implements Listener {

    private final BeaconPylonSystem plugin;
    private final Map<UUID, Long> scoutCooldowns;
    
    public BeaconListener(BeaconPylonSystem plugin) {
        this.plugin = plugin;
        this.scoutCooldowns = new HashMap<>();
    }
    
    @EventHandler
    public void onBeaconPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.BEACON) return;
        
        Player player = event.getPlayer();
        TeamManager.Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        
        if (team == null || !team.getMaster().equals(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "팀장만 신호기를 설치할 수 있습니다!");
            return;
        }
        
        plugin.getBeaconManager().registerBeacon(event.getBlock().getLocation(), team.getName());
        player.sendMessage(ChatColor.GREEN + "신호기가 설치되었습니다!");
    }
    
    @EventHandler
    public void onBeaconInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.BEACON) return;
        
        Player player = event.getPlayer();
        TeamManager.Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        
        if (team == null) {
            player.sendMessage(ChatColor.RED + "팀에 소속되어 있지 않습니다!");
            return;
        }
        
        BeaconManager.BeaconData beaconData = plugin.getBeaconManager()
            .getBeacon(event.getClickedBlock().getLocation());
        
        if (beaconData == null || !beaconData.getTeamName().equals(team.getName())) {
            player.sendMessage(ChatColor.RED + "이 신호기는 당신의 팀 소유가 아닙니다!");
            return;
        }
        
        event.setCancelled(true);
        openBeaconUI(player);
    }
    
    private void openBeaconUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.DARK_PURPLE + "신호기 메뉴");
        
        // 슬롯 1: 스페어 바디
        ItemStack spareBodyIcon = new ItemStack(Material.BEDROCK);
        ItemMeta spareMeta = spareBodyIcon.getItemMeta();
        spareMeta.setDisplayName(ChatColor.GOLD + "스페어 바디 구매");
        spareMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "부활 지점으로 사용할 수 있는",
            ChatColor.GRAY + "스페어 바디를 구매합니다.",
            "",
            ChatColor.YELLOW + "가격: " + ChatColor.GREEN + "5000 XP"
        ));
        spareBodyIcon.setItemMeta(spareMeta);
        inv.setItem(1, spareBodyIcon);
        
        // 슬롯 3: 공동 창고
        ItemStack storageIcon = new ItemStack(Material.CHEST);
        ItemMeta storageMeta = storageIcon.getItemMeta();
        storageMeta.setDisplayName(ChatColor.AQUA + "공동 창고");
        storageMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "팀원들과 공유하는 창고입니다.",
            "",
            ChatColor.YELLOW + "클릭하여 열기"
        ));
        storageIcon.setItemMeta(storageMeta);
        inv.setItem(3, storageIcon);
        
        // 슬롯 5: 경험치 상점
        ItemStack shopIcon = new ItemStack(Material.EMERALD);
        ItemMeta shopMeta = shopIcon.getItemMeta();
        shopMeta.setDisplayName(ChatColor.GREEN + "경험치 상점");
        shopMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "장비와 소모품을 구매할 수 있습니다.",
            "",
            ChatColor.YELLOW + "클릭하여 열기"
        ));
        shopIcon.setItemMeta(shopMeta);
        inv.setItem(5, shopIcon);
        
        // 슬롯 7: 정찰
        ItemStack scoutIcon = new ItemStack(Material.ELYTRA);
        ItemMeta scoutMeta = scoutIcon.getItemMeta();
        scoutMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "정찰");
        scoutMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "겉날개와 폭죽 32개를 지급받습니다.",
            ChatColor.GRAY + "5분 후 자동으로 사라집니다.",
            "",
            ChatColor.YELLOW + "가격: " + ChatColor.GREEN + "50000 XP"
        ));
        scoutIcon.setItemMeta(scoutMeta);
        inv.setItem(7, scoutIcon);
        
        player.openInventory(inv);
    }
    
    @EventHandler
    public void onBeaconUIClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals(ChatColor.DARK_PURPLE + "신호기 메뉴")) return;
        
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        
        if (event.getCurrentItem() == null) return;
        
        int slot = event.getSlot();
        
        switch (slot) {
            case 1: // 스페어 바디
                purchaseSpareBody(player);
                break;
            case 3: // 공동 창고
                openSharedStorage(player);
                break;
            case 5: // 경험치 상점
                openShop(player);
                break;
            case 7: // 정찰
                activateScout(player);
                break;
        }
    }
    
    private void purchaseSpareBody(Player player) {
        int cost = 5000;
        ShopManager shopManager = plugin.getShopManager();
        int playerXP = shopManager.getTotalExperience(player);
        
        if (playerXP < cost) {
            player.sendMessage(ChatColor.RED + "경험치가 부족합니다! (필요: " + cost + " XP)");
            return;
        }
        
        shopManager.setTotalExperience(player, playerXP - cost);
        ItemStack spareBody = plugin.getSpareBodyManager().createSpareBodyItem(player);
        player.getInventory().addItem(spareBody);
        player.sendMessage(ChatColor.GREEN + "스페어 바디를 구매했습니다! (-" + cost + " XP)");
        player.closeInventory();
    }
    
    private void openSharedStorage(Player player) {
        TeamManager.Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) return;
        
        // 공동 창고 구현 (54칸 더블 체스트)
        Inventory storage = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN + team.getName() + " 공동 창고");
        player.openInventory(storage);
    }
    
    private void openShop(Player player) {
        Inventory shop = Bukkit.createInventory(null, 54, ChatColor.GREEN + "경험치 상점");
        
        // 핫딜 표시 (첫 줄)
        List<ShopManager.ShopItem> hotDeals = plugin.getShopManager().getHotDeals();
        for (int i = 0; i < Math.min(hotDeals.size(), 9); i++) {
            shop.setItem(i, hotDeals.get(i).getDisplayItem());
        }
        
        // 구분선
        ItemStack separator = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta sepMeta = separator.getItemMeta();
        sepMeta.setDisplayName(ChatColor.GOLD + "★ 오늘의 핫딜! ★");
        separator.setItemMeta(sepMeta);
        for (int i = 9; i < 18; i++) {
            shop.setItem(i, separator);
        }
        
        // 일반 상품 표시
        Map<String, ShopManager.ShopItem> regularItems = plugin.getShopManager().getRegularItems();
        int slot = 18;
        for (ShopManager.ShopItem item : regularItems.values()) {
            if (slot >= 54) break;
            shop.setItem(slot++, item.getDisplayItem());
        }
        
        player.openInventory(shop);
    }
    
    private void activateScout(Player player) {
        int cost = 50000;
        ShopManager shopManager = plugin.getShopManager();
        int playerXP = shopManager.getTotalExperience(player);
        
        if (playerXP < cost) {
            player.sendMessage(ChatColor.RED + "경험치가 부족합니다! (필요: " + cost + " XP)");
            return;
        }
        
        shopManager.setTotalExperience(player, playerXP - cost);
        
        // 겉날개와 폭죽 지급
        ItemStack elytra = new ItemStack(Material.ELYTRA);
        ItemStack fireworks = new ItemStack(Material.FIREWORK_ROCKET, 32);
        
        player.getInventory().addItem(elytra, fireworks);
        player.sendMessage(ChatColor.GREEN + "정찰 모드 활성화! 5분 후 아이템이 사라집니다.");
        player.closeInventory();
        
        // 5분 후 아이템 제거 스케줄
        scheduleScoutRemoval(player, elytra, fireworks);
    }
    
    private void scheduleScoutRemoval(Player player, ItemStack elytra, ItemStack fireworks) {
        // 30초 전 알림
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.sendMessage(ChatColor.YELLOW + "정찰 아이템이 30초 후 사라집니다!");
                }
            }
        }.runTaskLater(plugin, 20L * 270); // 4분 30초
        
        // 10초 전 알림
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.sendMessage(ChatColor.GOLD + "정찰 아이템이 10초 후 사라집니다!");
                }
            }
        }.runTaskLater(plugin, 20L * 290); // 4분 50초
        
        // 5초 전 알림
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.sendMessage(ChatColor.RED + "정찰 아이템이 5초 후 사라집니다!");
                }
            }
        }.runTaskLater(plugin, 20L * 295); // 4분 55초
        
        // 5분 후 제거
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.getInventory().remove(elytra);
                    player.getInventory().remove(Material.FIREWORK_ROCKET);
                    player.sendMessage(ChatColor.GRAY + "정찰 아이템이 사라졌습니다.");
                }
            }
        }.runTaskLater(plugin, 20L * 300); // 5분
    }
}

package com.beaconpylon.listeners;

import com.beaconpylon.BeaconPylonSystem;
import com.beaconpylon.managers.SpareBodyManager;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerDeathListener implements Listener {

    private final BeaconPylonSystem plugin;
    
    public PlayerDeathListener(BeaconPylonSystem plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        SpareBodyManager.SpareBody spareBody = plugin.getSpareBodyManager()
            .getOldestSpareBody(event.getEntity().getUniqueId());
        
        if (spareBody != null) {
            event.getEntity().sendMessage(ChatColor.YELLOW + "스페어 바디로 부활합니다...");
        }
    }
    
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        SpareBodyManager.SpareBody spareBody = plugin.getSpareBodyManager()
            .getOldestSpareBody(event.getPlayer().getUniqueId());
        
        if (spareBody != null) {
            // 스페어 바디 위치로 텔레포트
            new BukkitRunnable() {
                @Override
                public void run() {
                    event.getPlayer().teleport(spareBody.getLocation());
                    event.getPlayer().sendMessage(ChatColor.GREEN + "스페어 바디로 부활했습니다!");
                    
                    // 스페어 바디 제거
                    plugin.getSpareBodyManager().removeSpareBody(spareBody.getArmorStandUUID());
                }
            }.runTaskLater(plugin, 1L);
        }
    }
}

이어서 작성