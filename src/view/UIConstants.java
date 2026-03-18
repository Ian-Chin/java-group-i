package view;

import java.awt.*;

public final class UIConstants {

    private UIConstants() {} // prevent instantiation

    // ─── Brand colors ────────────────────────────────────────────
    public static final Color PRIMARY       = new Color(80, 110, 230);
    public static final Color PRIMARY_HOVER = PRIMARY.brighter();

    // ─── Background colors ───────────────────────────────────────
    public static final Color BG_PAGE       = new Color(240, 240, 245);
    public static final Color BG_CONTENT    = new Color(245, 246, 250);
    public static final Color BG_CARD       = Color.WHITE;
    public static final Color BG_HEADER     = Color.WHITE;

    // ─── Sidebar colors ─────────────────────────────────────────
    public static final Color SIDEBAR_BG     = new Color(30, 36, 50);
    public static final Color SIDEBAR_HOVER  = new Color(45, 52, 70);
    public static final Color SIDEBAR_ACTIVE = PRIMARY;
    public static final Color SIDEBAR_DIVIDER = new Color(60, 68, 85);

    // ─── Text colors ────────────────────────────────────────────
    public static final Color TEXT_PRIMARY   = new Color(30, 30, 40);
    public static final Color TEXT_SECONDARY = new Color(120, 120, 130);
    public static final Color TEXT_MUTED     = new Color(150, 150, 165);
    public static final Color TEXT_DARK      = new Color(50, 50, 60);
    public static final Color TEXT_SIDEBAR   = new Color(180, 185, 200);
    public static final Color TEXT_NAV_LABEL = new Color(120, 130, 150);
    public static final Color TEXT_DANGER    = new Color(220, 80, 80);

    // ─── Border / field colors ──────────────────────────────────
    public static final Color BORDER_DEFAULT = new Color(210, 210, 220);
    public static final Color BORDER_OUTLINE = new Color(200, 200, 210);
    public static final Color BORDER_HEADER  = new Color(230, 230, 235);

    // ─── Fonts ──────────────────────────────────────────────────
    public static final Font FONT_HEADING_1  = new Font("SansSerif", Font.BOLD, 32);
    public static final Font FONT_HEADING_2  = new Font("SansSerif", Font.BOLD, 28);
    public static final Font FONT_HEADING_3  = new Font("SansSerif", Font.BOLD, 26);
    public static final Font FONT_HEADING_4  = new Font("SansSerif", Font.BOLD, 24);
    public static final Font FONT_BODY       = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font FONT_BODY_BOLD  = new Font("SansSerif", Font.BOLD, 14);
    public static final Font FONT_SMALL      = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_SMALL_BOLD = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_LABEL      = new Font("SansSerif", Font.BOLD, 11);
    public static final Font FONT_SIDEBAR    = new Font("SansSerif", Font.BOLD, 16);

    // ─── Dimensions ─────────────────────────────────────────────
    public static final Dimension FIELD_SIZE  = new Dimension(380, 44);
    public static final Dimension BUTTON_SIZE = new Dimension(380, 48);
    public static final int SIDEBAR_WIDTH     = 240;
    public static final int HEADER_HEIGHT     = 64;
}
