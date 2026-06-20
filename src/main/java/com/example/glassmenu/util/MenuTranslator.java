package com.example.glassmenu.util;

import com.example.glassmenu.GlassMenuClient;
import com.example.glassmenu.GlassMenuConfigModel.MenuLanguage;

import java.util.HashMap;
import java.util.Map;

public class MenuTranslator {
    private static final Map<String, String> ru = new HashMap<>();
    static {
        ru.put("Left Hand Item", "Левая рука");
        ru.put("Player Card", "Карточка игрока");
        ru.put("Red", "Красный");
        ru.put("Player Radar", "Радар Игроков");
        ru.put("Player Radar Settings", "Настройки Радара Игроков");
        ru.put("RGB Arrows", "RGB Стрелки");
        ru.put("Arrow Color", "Цвет Стрелки");
        ru.put("Radius: ", "Радиус: ");
        ru.put("Radius", "Радиус");
        ru.put("Max Distance", "Макс. дистанция");
        ru.put("Circle Size", "Размер круга");
        ru.put("Icon Size", "Размер указателя");
        ru.put("Radius", "Радиус");

        ru.put("Green", "Зеленый");
        ru.put("Blue", "Синий");
        ru.put("Armor HUD", "Броня (HUD)");
        ru.put("User HUD Color", "Цвет User HUD");
        ru.put("Effects HUD Color", "Цвет эффектов");
        ru.put("Enable", "Включить");
        ru.put("WORLD & RENDER", "МИР И РЕНДЕР");
        ru.put("BedWars ESP", "BedWars ESP");
        ru.put("Position Editor", "Редактор Позиций");
        ru.put("Stretch", "Растяжение Экрана");
        ru.put("Save & Back", "Сохранить и Назад");
        ru.put("Back", "Назад");
        ru.put("Target ESP", "Target ESP (Цель)");
        ru.put("Stars", "Звезды");
        ru.put("Custom Nametags Settings", "Настройки Кастомных Ников");
        ru.put("Potion Effects", "Эффекты Зелий");
        ru.put("Jump Pulse Rings", "Кольца при Прыжке");
        ru.put("Custom Crosshair", "Кастомный Прицел");
        ru.put("Item Effects", "Эффекты Предмета");
        ru.put("HUD & OVERLAYS", "ИНТЕРФЕЙС И ОВЕРЛЕИ");
        ru.put("Attack Range ESP", "Радиус Атаки (ESP)");
        ru.put("Enable ESP (Hitbox/Alert)", "Включить ESP (Хитбокс)");
        ru.put("WasterVisual", "WasterVisual");
        ru.put("RGB", "RGB Переливание");
        ru.put("PREVIEW", "ПРЕДПРОСМОТР");
        ru.put("Mode", "Режим");
        ru.put("Enable Dynamic Island", "Включить Dynamic Island");
        ru.put("Enable Attack Range", "Включить Радиус Атаки");
        ru.put("BedWars ESP Settings", "Настройки BedWars ESP");
        ru.put("Better Inv HUD", "Улучшенный Инвентарь");
        ru.put("Gear / Clock Effect", "Эффект Шестеренки / Часов");
        ru.put("Hit Star Settings", "Настройки Звезд при Ударе");
        ru.put("Enable Custom Hit Stars", "Включить Кастомные Звезды");
        ru.put("Drop Predictor", "Предсказатель Падения");
        ru.put("Color Grading", "Цветокоррекция");
        ru.put("Visuals Settings", "Визуальные Настройки");
        ru.put("Bridge Box", "Bridge Box");
        ru.put("User HUD", "User HUD (Игрок)");
        ru.put("Hex", "Hex Код");
        ru.put("Enable Armor HUD", "Включить Armor HUD");
        ru.put("RGB Value", "Значение RGB");
        ru.put("Enable Card", "Включить Карточку");
        ru.put("Enable Indicator", "Включить Индикатор");
        ru.put("Show Teammate Hearts", "Показывать Сердечки Тиммейтов");
        ru.put("Afterimage Trail", "След (Afterimage)");
        ru.put("Jump Effects", "Эффекты Прыжка");
        ru.put("Bridge", "Мосты (Bridge)");
        ru.put("Inventory HUD", "Инвентарь (HUD)");
        ru.put("Hex:", "Hex:");
        ru.put("Circle Hotbar", "Круговой Хотбар");
        ru.put("RGB / Rainbow Effect", "Эффект RGB / Радуги");
        ru.put("Attack Range ESP Settings", "Настройки Радиуса Атаки");
        ru.put("User Indicator", "Индикатор Игрока");
        ru.put("Enable Effects HUD", "Включить Effects HUD");
        ru.put("HEX Color", "HEX Цвет");
        ru.put("Overlay Color", "Цвет Оверлея");
        ru.put("Enable Nametags through walls", "Ники Сквозь Стены");
        ru.put("Hit Effects", "Эффекты Удара");
        ru.put("Afterimage", "След-Призрак");
        ru.put("Enable User HUD", "Включить User HUD");
        ru.put("DRAG ELEMENTS TO POSITION / EDGES TO RESIZE", "ПЕРЕТАСКИВАЙТЕ ЭЛЕМЕНТЫ / ТЯНИТЕ ЗА КРАЯ");
        ru.put("Custom Nametags", "Кастомные Ники");
        ru.put("Hand Customization", "Кастомизация Руки");
        ru.put("Color", "Цвет");
        ru.put("Dynamic Island", "Dynamic Island");
        ru.put("Reset", "Сброс");
        ru.put("Rainbow", "Радужный");
        ru.put("Language / Язык", "Language / Язык");
        ru.put("Language", "Язык");
        
        // Tab names
        ru.put("GENERAL", "ОБЩЕЕ");
        ru.put("POSITIONS", "ПОЗИЦИИ");
        ru.put("VISUALS", "ВИЗУАЛ");
        ru.put("COMBAT", "БОЙ");

        // Enums
        ru.put("CIRCLE", "КРУГ");
        ru.put("BLOCK_OUTLINE", "КОНТУР БЛОКА");
        ru.put("HITBOX", "ХИТБОКС");
        ru.put("ALERT", "ВНИМАНИЕ");
        ru.put("SOLID_OUTLINE", "СПЛОШНОЙ КОНТУР");
        ru.put("GLOW_OUTLINE", "СВЕЧЕНИЕ");
        ru.put("FILLED", "ЗАЛИВКА");
        ru.put("NONE", "НЕТ");
        ru.put("PARTICLES", "ЧАСТИЦЫ");
        ru.put("RGB_PARTICLES", "RGB ЧАСТИЦЫ");
        ru.put("DEFAULT", "ПО УМОЛЧАНИЮ");
        ru.put("SWING_DOWN", "УДАР ВНИЗ");
        ru.put("SWING_UP", "УДАР ВВЕРХ");
        ru.put("SWING_CENTER", "УДАР В ЦЕНТР");
        
        // Single letters
        ru.put("R", "R");
        ru.put("G", "G");
        ru.put("B", "B");
        ru.put("X", "X");
        ru.put("Y", "Y");
        ru.put("Z", "Z");
    }

    public static String tr(String key) {
        if (GlassMenuClient.CONFIG.menuLanguage() == MenuLanguage.RUSSIAN) {
            return ru.getOrDefault(key, key);
        }
        if (GlassMenuClient.CONFIG.menuLanguage() == MenuLanguage.AUTO) {
            String lang = net.minecraft.client.MinecraftClient.getInstance().options.language;
            if (lang != null && lang.toLowerCase().contains("ru")) {
                return ru.getOrDefault(key, key);
            }
        }
        return key; // English by default
    }
}
