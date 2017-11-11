package com.bergerkiller.bukkit.tc.attachments.ui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapResourcePack;
import com.bergerkiller.bukkit.common.map.MapTexture;
import com.bergerkiller.bukkit.common.map.widgets.MapWidget;
import com.bergerkiller.bukkit.common.resources.CommonSounds;
import com.bergerkiller.bukkit.tc.attachments.config.CartAttachmentType;

/**
 * A single attachment node in the attachment node tree, containing
 * the configuration for the node.
 */
public class MapWidgetAttachmentNode extends MapWidget {
    private final ConfigurationNode config;
    private List<MapWidgetAttachmentNode> attachments = new ArrayList<MapWidgetAttachmentNode>();
    private MapWidgetAttachmentNode parentAttachment = null;
    private int col, row;

    public MapWidgetAttachmentNode() {
        this(new ConfigurationNode());
    }

    public MapWidgetAttachmentNode(ConfigurationNode config) {
        this.config = config;

        // Add child attachments
        if (config.contains("attachments")) {
            for (ConfigurationNode subAttachment : config.getNodeList("attachments")) {
                MapWidgetAttachmentNode sub = new MapWidgetAttachmentNode(subAttachment);
                sub.parentAttachment = this;
                this.attachments.add(sub);
            }
        }

        // Can be focused
        this.setFocusable(true);
    }

    public MapWidgetAttachmentNode getParentAttachment() {
        return this.parentAttachment;
    }

    public void openMenu(MenuItem item) {
        ((MapWidgetAttachmentTree) this.parent).onMenuOpen(this, item);
    }

    public List<MapWidgetAttachmentNode> getAttachments() {
        return this.attachments;
    }
    
    public ConfigurationNode getConfig() {
        return this.config;
    }

    public MapWidgetAttachmentNode addAttachment() {
        MapWidgetAttachmentNode attachment = new MapWidgetAttachmentNode();
        attachment.parentAttachment = this;
        this.attachments.add(attachment);
        return attachment;
    }

    /**
     * Sets the column and row this node is displayed at.
     * This control the indent level when drawing.
     * 
     * @param col
     * @param row
     */
    public void setCell(int col, int row) {
        this.col = col;
        this.row = row;
    }

    public CartAttachmentType getType() {
        return this.config.get("type", CartAttachmentType.ENTITY);
    }

    public void setType(CartAttachmentType type) {
        this.config.set("type", type);
    }

    @Override
    public void onAttached() {
        this.setSize(this.parent.getWidth(), 18);
    }

    @Override
    public void onActivate() {
        super.onActivate();

        // Play a neat sliding sound
        display.playSound(CommonSounds.PISTON_EXTEND);

        // After being activated, add a bunch of buttons that can be pressed
        // Each button will open its own context menu to edit things
        // The buttons shown here depend on the type of the node, somewhat
        int px = this.col * 17 + 1;
        this.addWidget(new MapWidgetBlinkyButton() {
            @Override
            public void onClick() {
                openMenu(MenuItem.APPEARANCE);
            }
        }.setIcon(getIcon()).setPosition(px, 1));

        this.addWidget(new MapWidgetBlinkyButton() {
            @Override
            public void onClick() {
                openMenu(MenuItem.POSITION);
            }
        }.setIcon("attachments/move.png").setPosition(px + 17, 1));
    }

    @Override
    public void onDeactivate() {
        this.clearWidgets();

        // Play a neat sliding sound
        display.playSound(CommonSounds.PISTON_CONTRACT);
    }

    @Override
    public void onFocus() {
        // Click navigation sounds
        //display.playSound(CommonSounds.CLICK_WOOD);
        this.activate();
    }

    @Override
    public void onDraw() {        
        int px = this.col * 17;

        if (this.isFocused()) {
            view.fillRectangle(px, 0, getWidth() - px, getHeight(), MapColorPalette.getColor(220, 220, 220));
        } else if (this.isActivated()) {
            view.fillRectangle(px, 0, getWidth() - px, getHeight(), MapColorPalette.getColor(220, 255, 220));
        }

        // Draw dots to the left to show our tree hierarchy
        // When we are root, we do something special (?)
        if (this.parentAttachment == null) {
            // Do something special
            // No node for now.
        } else {
            // There is an odd/even problem with the dot pattern
            // Find out our index as a child to correct this pattern problem
            int dotOffset = (((this.row - this.parentAttachment.row) & 0x1) == 0x1) ? 1 : 0;

            // Dot pattern vertical line to top
            byte dotColor = MapColorPalette.getColor(64, 64, 64);
            for (int n = 0; n < 5; n++) {
                this.view.drawPixel(px - 17 + 8, n * 2 + dotOffset, dotColor);
            }

            // Dot pattern horizontal
            for (int n = 1; n < 5; n++) {
                this.view.drawPixel(px - 17 + 8 + n * 2, 8 + dotOffset, dotColor);
            }

            // If not last child, continue dot pattern down
            int childIdx = this.parentAttachment.attachments.indexOf(this);
            if (childIdx != (this.parentAttachment.attachments.size() - 1)) {
                for (int n = 5; n < 9; n++) {
                    this.view.drawPixel(px - 17 + 8, n * 2 + dotOffset, dotColor);
                }
            }

            // For all further parent levels down, check if there is another child
            // If there is, draw a vertical dotted line to indicate so
            int tmpX = px - 26;
            MapWidgetAttachmentNode tmpNode = this.parentAttachment;
            while (tmpNode != null) {
                MapWidgetAttachmentNode tmpNodeParent = tmpNode.parentAttachment;
                if (tmpNodeParent != null && tmpNode != tmpNodeParent.attachments.get(tmpNodeParent.attachments.size() - 1)) {
                    // Node has a parent and is not the last child of that parent: we need to draw a line
                    // Use the known row property to calculate the dot index
                    dotOffset = (((this.row - tmpNodeParent.row) & 0x1) == 0x1) ? 1 : 0;
                    for (int n = 0; n < 9; n++) {
                        this.view.drawPixel(tmpX, n * 2 + dotOffset, dotColor);
                    }
                }
                tmpNode = tmpNodeParent;
                tmpX -= 17;
            }
        }

        // Draw icon and maybe labels or other stuff when not activated
        if (!this.isActivated()) {
            view.draw(getIcon(), px + 1, 1);
        }

        if (this.isFocused()) {
            view.drawRectangle(px, 0, getWidth() - px, getHeight(), MapColorPalette.COLOR_BLACK);
        } else if (this.isActivated()) {
            view.drawRectangle(px, 0, getWidth() - px, getHeight(), MapColorPalette.COLOR_GREEN);
        }
    }

    private MapTexture getIcon() {
        return MapResourcePack.SERVER.getItemTexture(new ItemStack(Material.MINECART), 16, 16);
    }

    public static enum MenuItem {
        APPEARANCE, POSITION
    }
}
