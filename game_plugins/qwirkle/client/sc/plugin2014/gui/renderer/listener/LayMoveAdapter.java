package sc.plugin2014.gui.renderer.listener;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import sc.plugin2014.gui.renderer.components.GUIStone;
import sc.plugin2014.gui.renderer.display.GameRenderer;

public class LayMoveAdapter extends MouseAdapter {

    private final GameRenderer parent;

    public LayMoveAdapter(GameRenderer parent) {
        this.parent = parent;

    }

    @Override
    public void mousePressed(MouseEvent e) {

        // requestFocusInWindow();
        if (e.getButton() == MouseEvent.BUTTON1) {
            int x = e.getX();
            int y = e.getY();

            for (GUIStone stone : parent.sensetiveStones) {
                if (checkIfInner(x, y, stone)) {
                    parent.removeStone(stone);
                    parent.updateView();
                    break;
                }
            }

            for (GUIStone stone : parent.toLayStones) {
                if (checkIfInner(x, y, stone)) {
                    parent.removeStone(stone);
                    parent.updateView();
                    break;
                }
            }
        }
    }

    private boolean checkIfInner(int x, int y, GUIStone stone) {
        if (stone.inner(x, y)) {
            parent.selectedStone = stone;
            parent.ox = stone.getX();
            parent.oy = stone.getY();
            parent.dx = x - parent.ox;
            parent.dy = y - parent.oy;

            return true;
        }
        return false;
    }

    @Override
    public void mouseDragged(MouseEvent e) {

        if (parent.selectedStone != null) {
            parent.selectedStone.moveTo(e.getX() - parent.dx, e.getY()
                    - parent.dy);
            parent.updateView();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {

        if (e.getButton() == MouseEvent.BUTTON1) {
            if (parent.selectedStone != null) {
                parent.selectedStone.moveTo(e.getX(), e.getY());

                parent.layStone(parent.selectedStone);

                parent.selectedStone = null;
            }
            parent.updateView();

        }
    }
}