/**
 * 
 */
package sc.plugin2012.gui.renderer;

import static sc.plugin2012.gui.renderer.RenderConfiguration.BACKGROUND;
import static sc.plugin2012.gui.renderer.RenderConfiguration.ANTIALIASING;
import static sc.plugin2012.gui.renderer.RenderConfiguration.OPTIONS;
import static sc.plugin2012.gui.renderer.RenderConfiguration.TRANSPARANCY;

import static sc.plugin2012.util.Constants.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import sc.plugin2012.gui.renderer.RenderConfiguration;
import sc.plugin2012.BuildMove;
import sc.plugin2012.Card;
import sc.plugin2012.GameState;
import sc.plugin2012.Move;
import sc.plugin2012.MoveType;
import sc.plugin2012.Player;
import sc.plugin2012.PlayerColor;
import sc.plugin2012.Segment;
import sc.plugin2012.SelectMove;
import sc.plugin2012.Tower;
import sc.plugin2012.util.Constants;

/**
 * @author tkra, ffi
 */
@SuppressWarnings("serial")
public class FrameRenderer extends JPanel {

	// konstanten
	private final static int BORDER_SIZE = 6;
	private static final int PROGRESS_ICON_SIZE = 60;
	private static final int PROGRESS_BAR_HEIGTH = 36;

	private static final int TOWER_LEFT_WIDTH = 25;
	private static final int TOWER_LEFT_HEIGTH = 10;
	private static final int TOWER_RIGHT_WIDTH = 15;
	private static final int TOWER_RIGHT_HEIGTH = 15;
	private static final int TOWER_TOTAL_WIDTH = TOWER_LEFT_WIDTH + TOWER_RIGHT_WIDTH;
	private static final int TOWER_STORIE_HEIGTH = 14;

	private static final int STUFF_GAP = 8;
	private static final int GAP_SIZE = 10;
	private static final int GAP_TOWER_TOP = TOWER_RIGHT_HEIGTH;
	private static final int GAP_TOWER_BOTTOM = TOWER_LEFT_HEIGTH;
	private static final int DIST_TOWER_SELECT = 15;

	private static final int DIAGONAL_GAP = 10;
	private static final int CITY_GAP = 42;

	private static final int CARD_DOT = 8;
	private static final int CARD_WIDTH = 8 + SLOTS * 6 - 6 + CARD_DOT;
	private static final int CARD_HEGTH = 8 + SLOTS * (CARD_DOT + 4) - 4;

	private static final int MAX_SEGMENT_HEIGTH = MAX_SEGMENT_SIZE * TOWER_STORIE_HEIGTH + TOWER_RIGHT_HEIGTH
			+ TOWER_LEFT_HEIGTH;

	// schrift
	// private static final Font h0 = new Font("Helvetica", Font.BOLD, 73);
	private static final Font h1 = new Font("Helvetica", Font.BOLD, 27);
	private static final Font h2 = new Font("Helvetica", Font.BOLD, 23);
	private static final Font h3 = new Font("Helvetica", Font.BOLD, 14);
	private static final Font h4 = new Font("Helvetica", Font.PLAIN, 10);

	private static final JPanel fmPanel = new JPanel();
	// private static final FontMetrics fmH0 = fmPanel.getFontMetrics(h0);
	private static final FontMetrics fmH1 = fmPanel.getFontMetrics(h1);
	private static final FontMetrics fmH2 = fmPanel.getFontMetrics(h2);
	private static final FontMetrics fmH3 = fmPanel.getFontMetrics(h3);
	private static final FontMetrics fmH4 = fmPanel.getFontMetrics(h4);

	private static final String SELECT_STRING = "Bauelemente auswaehlen";
	private static final int MIN_DIALOG_SIZE = fmH1.stringWidth(SELECT_STRING) + 2 * GAP_SIZE;
	private static final int STATUS_HEIGTH = PROGRESS_BAR_HEIGTH + 2 * STUFF_GAP
			+ Math.max(CARD_HEGTH, MAX_SEGMENT_HEIGTH) + fmH2.getHeight();

	// current (game) state
	private PlayerColor currentPlayer;
	private GameState gameState;
	private MoveType currentMoveType;

	// image components
	private BufferedImage buffer;
	private boolean updateBuffer;
	private final Image bgImage;
	private Image scaledBgImage;
	private final Image progressIcon;

	// selektionsmenue
	private final List<TowerData> selectTowers;
	private int differentSegments;
	private int differentSegmentsAccN;
	private int mostSegments;
	private int selectHeight;
	private int selectWidth;
	private int selectX;
	private int selectY;
	private int[] selectButton;
	private boolean selectionOkay;
	private boolean selectionPressed;

	// selektion
	private final int[] selections;
	private int selectionSize;

	// spielersegmente
	private final List<TowerData> redSegments;
	private final List<TowerData> blueSegments;
	private List<TowerData> sensetiveSegments;
	private final List<TowerData> sensetiveTowers;
	private TowerData selectedSegment;
	private int dx, dy;
	private int ox, oy;

	// sonstiges
	private final TowerData[][] cityTowers;
	private TowerData selectedTower;
	private TowerData droppedSegment;
	private int turnToAnswer;
	private boolean gameEnded;

	private final MouseAdapter selectMouseAdapter = new MouseAdapter() {

		@Override
		public void mouseClicked(MouseEvent e) {
			int x = e.getX();
			int y = e.getY();

			for (TowerData tower : selectTowers) {
				if (inner(x, y, tower.xs, tower.ys)) {
					if (tower.owner == null) {
						tower.owner = currentPlayer;
						selections[tower.size - 1]++;
						selectionSize++;
					} else {
						tower.owner = null;
						selections[tower.size - 1]--;
						selectionSize--;
					}
					selectionOkay = selectionSize == Constants.SELECTION_SIZE;
					repaint();
					break;
				}
			}

		}

		@Override
		public void mousePressed(MouseEvent e) {

			if (selectionOkay) {

				int x = e.getX();
				int y = e.getY();

				if (x > selectButton[0] && x < selectButton[0] + selectButton[2]) {
					if (y > selectButton[1] && y < selectButton[1] + selectButton[3]) {
						selectionPressed = true;
						repaint();
					}

				}
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {

			if (selectionPressed) {

				int x = e.getX();
				int y = e.getY();
				selectionPressed = false;

				if (x > selectButton[0] && x < selectButton[0] + selectButton[2]) {
					if (y > selectButton[1] && y < selectButton[1] + selectButton[3]) {

						FrameRenderer.this.removeMouseListener(this);
						sendMove(new SelectMove(selections));

					}

				}
				repaint();
			}

		}

	};

	private final MouseAdapter buildMouseAdapter = new MouseAdapter() {

		@Override
		public void mousePressed(MouseEvent e) {
			int x = e.getX();
			int y = e.getY();

			for (TowerData tower : sensetiveSegments) {
				if (inner(x, y, tower.xs, tower.ys)) {
					selectedSegment = tower;
					ox = tower.x;
					oy = tower.y;
					dx = x - ox;
					dy = y - oy;

					for (BuildMove move : gameState.getPossibleMoves()) {
						if (move.size == tower.size) {
							TowerData data = cityTowers[move.city][move.slot];
							sensetiveTowers.add(data);
							data.highlited = true;
						}
					}

					updateBuffer = true;
					repaint();
					break;
				}

			}

		}

		@Override
		public void mouseDragged(MouseEvent e) {

			if (selectedSegment != null) {
				selectedSegment.moveTo(e.getX() - dx, e.getY() - dy);

				TowerData oldSelectedTower = selectedTower;
				selectedTower = null;
				for (TowerData tower : sensetiveTowers) {
					if (inner(selectedSegment.xs[1], selectedSegment.ys[1], tower.xs, tower.ys)) {
						if (tower.slot > 0) {
							TowerData clipper = cityTowers[tower.city][tower.slot - 1];
							if (!inner(selectedSegment.xs[1], selectedSegment.ys[1], clipper.xs, clipper.ys)) {
								selectedTower = tower;
								break;
							}
						} else {
							selectedTower = tower;
							break;
						}
					}
				}

				if (selectedTower != oldSelectedTower) {
					updateBuffer = true;
				}

				repaint();
			}

		}

		@Override
		public void mouseReleased(MouseEvent e) {

			if (selectedSegment != null) {
				sensetiveTowers.clear();
				for (int i = 0; i < CITIES; i++) {
					for (int j = 0; j < SLOTS; j++) {
						cityTowers[i][j].highlited = false;
					}
				}
				if (selectedTower != null) {

					TowerData data = selectedTower;
					droppedSegment = selectedSegment;
					selectedTower = null;
					sendMove(new BuildMove(data.city, data.slot, selectedSegment.size));

				} else {
					selectedSegment.moveTo(ox, oy);
				}
				selectedSegment = null;
				updateBuffer = true;
				repaint();
			}

		}

	};

	private ComponentListener componentListener = new ComponentAdapter() {

		@Override
		public void componentResized(ComponentEvent e) {
			resizeBoard();
			repaint();
		}

	};

	private class Point {
		int x, y;

		public Point(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	class TowerData {

		int[] xs;
		int[] ys;
		int innerX;
		int innerY;
		protected boolean highlited;
		PlayerColor owner = null;
		int size;
		int x, y;
		int slot;
		int city;
		int diff;

		public TowerData(int size) {
			highlited = false;
			this.size = size;
		}

		public TowerData(int size, int city, int slot) {
			highlited = false;
			this.size = size;
			this.city = city;
			this.slot = slot;
		}

		public void moveTo(int x, int y) {
			rebuild(x, y, size);
		}

		public void resize(int size) {
			rebuild(x, y, size);
		}

		public void rebuild(int x, int y, int size) {
			int heigth = size * TOWER_STORIE_HEIGTH + 1;
			xs = new int[6];
			ys = new int[6];
			this.size = size;
			this.x = x;
			this.y = y;

			xs[0] = x;
			xs[1] = x + TOWER_LEFT_WIDTH;
			xs[2] = x + TOWER_TOTAL_WIDTH;
			xs[3] = x + TOWER_TOTAL_WIDTH;
			xs[4] = x + TOWER_RIGHT_WIDTH;
			xs[5] = x;

			ys[0] = y - TOWER_LEFT_HEIGTH;
			ys[1] = y;
			ys[2] = y - TOWER_RIGHT_HEIGTH;
			ys[3] = y - TOWER_RIGHT_HEIGTH - heigth;
			ys[4] = y - TOWER_LEFT_HEIGTH - TOWER_RIGHT_HEIGTH - heigth;
			ys[5] = y - TOWER_LEFT_HEIGTH - heigth;

			innerX = x + TOWER_LEFT_WIDTH;
			innerY = y - heigth;

		}

	}

	public FrameRenderer() {

		updateBuffer = true;
		bgImage = loadImage("resource/game/boden_wiese3.png");
		progressIcon = loadImage("resource/game/kelle.png");
		selections = new int[Constants.MAX_SEGMENT_SIZE];
		selectTowers = new LinkedList<TowerData>();
		redSegments = new LinkedList<TowerData>();
		blueSegments = new LinkedList<TowerData>();
		sensetiveTowers = new LinkedList<TowerData>();
		cityTowers = new TowerData[CITIES][SLOTS];
		for (int i = 0; i < CITIES; i++) {
			for (int j = 0; j < SLOTS; j++) {
				cityTowers[i][j] = new TowerData(0, i, j);
			}
		}

		setMinimumSize(new Dimension(2 * CARDS_PER_PLAYER * (CARD_WIDTH + STUFF_GAP) + 2 * SELECTION_SIZE
				* (TOWER_TOTAL_WIDTH + STUFF_GAP), 600));

		setDoubleBuffered(true);
		addComponentListener(componentListener);
		setFocusable(true);
		requestFocusInWindow();

		RenderConfiguration.loadSettings();

		resizeBoard();
		repaint();

	}

	public void updateGameState(GameState gameState) {

		if (this.gameState != null && currentMoveType == MoveType.BUILD) {
			int turnDiff = gameState.getTurn() - this.gameState.getTurn();

			Move move = gameState.getLastMove();
			if (move != null && turnDiff == 1) {
				moveSegment(gameState);
			}
		}

		// aktuellen spielstand sichern
		this.gameState = gameState;
		this.currentMoveType = gameState.getCurrentMoveType();
		currentPlayer = gameState.getCurrentPlayer().getPlayerColor();
		updateBuffer = true;

		selectedSegment = null;
		droppedSegment = null;
		removeMouseListener(buildMouseAdapter);
		removeMouseListener(selectMouseAdapter);
		removeMouseMotionListener(buildMouseAdapter);

		if (currentMoveType == MoveType.SELECT) {
			// daten vorbereiten
			createSelectDialog();
			selectionOkay = false;
			selectionPressed = false;
			selectionSize = 0;
			for (int i = 0; i < Constants.MAX_SEGMENT_SIZE; i++) {
				selections[i] = 0;
			}
			addMouseListener(selectMouseAdapter);
		} else {
			if (currentPlayer == PlayerColor.RED) {
				sensetiveSegments = redSegments;
			} else {
				sensetiveSegments = blueSegments;
			}
			addMouseListener(buildMouseAdapter);
			addMouseMotionListener(buildMouseAdapter);
		}

		createPlayerSegments();
		updateCityTowers();
		repaint();

	}

	private synchronized void moveSegment(final GameState gameState) {

		setEnabled(false);
		BuildMove move = (BuildMove) gameState.getLastMove();
		TowerData targetTower = cityTowers[move.city][move.slot];

		if (droppedSegment == null) {
			updateBuffer = true;
			for (int i = sensetiveSegments.size() - 1; i >= 0; i--) {
				if (sensetiveSegments.get(i).size == move.size) {
					selectedSegment = sensetiveSegments.get(i);
					break;
				}
			}
		} else {
			selectedSegment = droppedSegment;
		}

		Point p = new Point(selectedSegment.x, selectedSegment.y);
		Point q = new Point(targetTower.innerX - TOWER_LEFT_WIDTH, targetTower.innerY);

		double pixelPerFrame = ((double) getHeight()) / 40;
		double dist = Math.sqrt(Math.pow(p.x - q.x, 2) + Math.pow(p.y - q.y, 2));

		final int frames = (int) Math.ceil(dist / pixelPerFrame);
		final Point o = new Point(p.x, p.y);
		final Point dP = new Point(q.x - p.x, q.y - p.y);

		for (int frame = 0; frame < frames; frame++) {

			p.x = o.x + (int) ((double) (frame * dP.x) / (double) frames);
			p.y = o.y + (int) ((double) (frame * dP.y) / (double) frames);
			selectedSegment.moveTo(p.x, p.y);

			invalidate();
			getParent().repaint();

			try {
				Thread.sleep(1000 / 40);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
		setEnabled(true);

	}

	public void requestMove(int turn) {
		turnToAnswer = turn;
	}

	private boolean myTurn() {
		return turnToAnswer == gameState.getTurn();
	}

	private synchronized void sendMove(Move move) {
		if (myTurn() && !gameEnded) {
			RenderFacade.getInstance().sendMove(move);
			turnToAnswer = -1;
		}
	}

	private void updateCityTowers() {

		for (Tower tower : gameState.getTowers()) {
			TowerData data = cityTowers[tower.city][tower.slot];
			data.resize(tower.getHeight());
			data.owner = tower.getOwner();
			data.diff = Math.abs(tower.getRedParts() - tower.getBlueParts());
		}

	}

	private void recreateCityTowers() {

		int y = getHeight() - BORDER_SIZE - STATUS_HEIGTH - 25;
		int cityWidth = TOWER_LEFT_WIDTH + SLOTS * (TOWER_RIGHT_WIDTH + DIAGONAL_GAP) - DIAGONAL_GAP;
		int citiesWidth = CITIES * (cityWidth + CITY_GAP) - CITY_GAP;
		int x = (getWidth() - citiesWidth) / 2;

		for (int i = 0; i < CITIES; i++) {
			int y2 = y;
			for (int j = 0; j < SLOTS; j++) {
				cityTowers[i][j].moveTo(x, y2);
				x += TOWER_RIGHT_WIDTH + DIAGONAL_GAP;
				y2 -= TOWER_RIGHT_HEIGTH + DIAGONAL_GAP;
			}
			x += TOWER_LEFT_WIDTH + CITY_GAP - DIAGONAL_GAP;
		}

	}

	private void resizeBoard() {

		int width = getWidth() - 2 * BORDER_SIZE;
		int heigth = getHeight() - 2 * BORDER_SIZE - PROGRESS_BAR_HEIGTH;

		if (width > 0 && heigth > 0) {
			MediaTracker tracker = new MediaTracker(this);

			scaledBgImage = new BufferedImage(width, heigth, BufferedImage.TYPE_3BYTE_BGR);
			scaledBgImage.getGraphics().drawImage(bgImage, 0, 0, width, heigth, this);
			tracker.addImage(scaledBgImage, 0);
			try {
				tracker.waitForID(0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		recreateCityTowers();
		recreatePlayerSegments();
		if (currentMoveType == MoveType.SELECT) {
			recreateSelectDialog();
		}

		System.gc();
		updateBuffer = true;
		repaint();
	}

	private void createSelectDialog() {

		synchronized (selectTowers) {

			selectTowers.clear();
			differentSegments = 0;
			differentSegmentsAccN = 0;
			mostSegments = 0;
			List<Segment> segments = gameState.getCurrentPlayer().getSegments();
			for (Segment segment : segments) {
				int retained = segment.getRetained();

				// hilfsdaten fuer menue berechnen
				if (retained > 0) {
					differentSegments++;
					differentSegmentsAccN += segment.size;
					if (retained > mostSegments) {
						mostSegments = retained;
					}
				}

				// tuerme fuer selektionsmenue einmalig erstellen
				for (int i = 0; i < retained; i++) {
					selectTowers.add(new TowerData(segment.size));
				}
			}

			recreateSelectDialog();
		}
	}

	private void recreateSelectDialog() {

		synchronized (selectTowers) {

			// dialoggroesse bestimmen
			selectHeight = 2 * GAP_SIZE + 2 * fmH1.getHeight();
			selectHeight += differentSegments * (GAP_TOWER_TOP + GAP_TOWER_BOTTOM + 2 * GAP_SIZE);
			selectHeight += differentSegmentsAccN * TOWER_STORIE_HEIGTH;
			selectWidth = Math.max(MIN_DIALOG_SIZE, mostSegments * (DIST_TOWER_SELECT + TOWER_TOTAL_WIDTH));

			selectX = (getWidth() - selectWidth) / 2;
			selectY = (getHeight() - STATUS_HEIGTH - selectHeight) / 2;

			// basispunkte bestimmen
			int x = 0;
			int y = selectY + fmH1.getHeight();
			List<Segment> segments = gameState.getCurrentPlayer().getSegments();
			int listOffset = 0;
			for (Segment segment : segments) {
				int retained = segment.getRetained();
				if (retained > 0) {
					y += 2 * GAP_SIZE + GAP_TOWER_TOP + GAP_TOWER_BOTTOM + segment.size * TOWER_STORIE_HEIGTH;
					x = (getWidth() - retained * (TOWER_TOTAL_WIDTH + DIST_TOWER_SELECT) + DIST_TOWER_SELECT) / 2;
					for (int i = listOffset; i < listOffset + retained; i++) {
						selectTowers.get(i).moveTo(x, y);
						x += TOWER_TOTAL_WIDTH + DIST_TOWER_SELECT;
					}
					listOffset += retained;
				}
			}

			selectButton = new int[] { (getWidth() - 200) / 2,
					selectY + selectHeight - GAP_SIZE - fmH1.getHeight(), 200, fmH1.getHeight() };

		}

	}

	private void createPlayerSegments() {

		// spieler rot
		Player player = gameState.getRedPlayer();
		synchronized (redSegments) {
			redSegments.clear();
			for (int i = MAX_SEGMENT_SIZE; i > 0; i--) {
				int usable = player.getSegment(i).getUsable();
				for (int j = 0; j < usable; j++) {
					TowerData tower = new TowerData(i);
					tower.owner = PlayerColor.RED;
					redSegments.add(tower);
				}
			}
		}

		// spieler blau
		player = gameState.getBluePlayer();
		synchronized (blueSegments) {
			blueSegments.clear();
			for (int i = MAX_SEGMENT_SIZE; i > 0; i--) {
				int usable = player.getSegment(i).getUsable();
				for (int j = 0; j < usable; j++) {
					TowerData tower = new TowerData(i);
					tower.owner = PlayerColor.BLUE;
					blueSegments.add(tower);
				}
			}
		}

		recreatePlayerSegments();

	}

	private void recreatePlayerSegments() {

		// spieler rot
		int x = BORDER_SIZE + STUFF_GAP + CARDS_PER_PLAYER * (CARD_WIDTH + STUFF_GAP);
		int y = getHeight() - BORDER_SIZE - PROGRESS_BAR_HEIGTH - STUFF_GAP;

		synchronized (redSegments) {
			for (TowerData tower : redSegments) {
				if (tower != selectedSegment) {
					tower.moveTo(x, y);
				}
				x += TOWER_TOTAL_WIDTH + STUFF_GAP;
			}
		}

		// spieler blau
		x = getWidth() - BORDER_SIZE - STUFF_GAP - CARDS_PER_PLAYER * (CARD_WIDTH + STUFF_GAP)
				- TOWER_TOTAL_WIDTH;
		y = getHeight() - BORDER_SIZE - PROGRESS_BAR_HEIGTH - STUFF_GAP;

		synchronized (blueSegments) {
			for (TowerData tower : blueSegments) {
				if (tower != selectedSegment) {
					tower.moveTo(x, y);
				}
				x -= TOWER_TOTAL_WIDTH + STUFF_GAP;
			}
		}

	}

	@Override
	public void paint(Graphics g) {

		super.paint(g);
		Graphics2D g2 = (Graphics2D) g;

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				OPTIONS[ANTIALIASING] ? RenderingHints.VALUE_ANTIALIAS_ON
						: RenderingHints.VALUE_ANTIALIAS_OFF);

		if (updateBuffer) {
			fillBuffer();
		}

		g2.drawImage(buffer, 0, 0, getWidth(), getHeight(), this);

		if (gameState != null) {
			paintDynamicComponents(g2);
		}

		if (myTurn() && currentMoveType == MoveType.SELECT) {
			paintSelectDialog(g2);
		}

		// TODO
		// if (config) {
		// drawConfigMessage(g2);
		// } else if (gameEnded) {
		// drawEndMessage(g2);
		// }

		// bmFrames++;
		// // repainted = true;
		// synchronized (LOCK) {
		// LOCK.notifyAll();
		// }

	}

	private void fillBuffer() {

		int imageType = OPTIONS[TRANSPARANCY] ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_BGR;
		buffer = new BufferedImage(getWidth(), getHeight(), imageType);
		Graphics2D g2 = (Graphics2D) buffer.getGraphics();

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				OPTIONS[ANTIALIASING] ? RenderingHints.VALUE_ANTIALIAS_ON
						: RenderingHints.VALUE_ANTIALIAS_OFF);

		paintStaticComponents(g2);
		if (gameState != null) {
			// printGameStatus(g2);
			paintSemiStaticComponents(g2);
		}

		updateBuffer = false;
	}

	private void paintStaticComponents(Graphics2D g2) {

		// hintergrundbild oder farbe
		if (OPTIONS[BACKGROUND] && scaledBgImage != null) {
			g2.drawImage(scaledBgImage, BORDER_SIZE, BORDER_SIZE, getWidth() - 2 * BORDER_SIZE, getHeight()
					- 2 * BORDER_SIZE, this);
		} else {
			g2.setColor(new Color(76, 119, 43));
			g2
					.fillRect(BORDER_SIZE, BORDER_SIZE, getWidth() - 2 * BORDER_SIZE, getHeight() - 2
							* BORDER_SIZE);
		}

		// seitenleiste
		g2.setColor(getTransparentColor(new Color(200, 240, 200), 160));
		// g2.fillRect(getWidth() - BORDER_SIZE - STATS_WIDTH, BORDER_SIZE,
		// STATS_WIDTH, getHeight() - 2 * BORDER_SIZE);

		// fortschrittsleite, spielerinfo hintergrund
		int heigth = PROGRESS_BAR_HEIGTH + 2 * STUFF_GAP + CARD_HEGTH + fmH2.getHeight();
		g2.fillRect(BORDER_SIZE, getHeight() - BORDER_SIZE - heigth, getWidth() - 2 * BORDER_SIZE, heigth);
	}

	private void paintSemiStaticComponents(Graphics2D g2) {

		// fortschrittsbalken
		g2.setColor(Color.BLACK);
		g2.setFont(h3);
		int left = fmH3.stringWidth("Spielfortschritt:") + BORDER_SIZE + 30;
		int right = getWidth() - left - 30;
		int fontY = getHeight() - BORDER_SIZE - PROGRESS_BAR_HEIGTH / 2 + fmH3.getHeight() / 2 - 4;
		g2.drawString("Spielfortschritt:", BORDER_SIZE + 10, fontY);

		int round = gameState.getRound() + 1;
		String roundString = Integer.toString(gameState.getRound() + 1);
		if (round > Constants.ROUND_LIMIT) {
			roundString = Integer.toString(Constants.ROUND_LIMIT);
		}

		g2.drawString("Runde " + roundString + " von " + Constants.ROUND_LIMIT, right + 30, fontY);

		int progress = (gameState.getTurn() * (right - left)) / (2 * Constants.ROUND_LIMIT);

		g2.setColor(Color.GRAY);
		g2.fillRoundRect(left, getHeight() - BORDER_SIZE - PROGRESS_BAR_HEIGTH + 8, right - left,
				PROGRESS_BAR_HEIGTH - 16, 10, 10);

		g2.setColor(Color.GREEN);
		g2.fillRoundRect(left, getHeight() - BORDER_SIZE - PROGRESS_BAR_HEIGTH + 8, progress,
				PROGRESS_BAR_HEIGTH - 16, 10, 10);

		g2.setColor(Color.DARK_GRAY);
		g2.drawRoundRect(left, getHeight() - BORDER_SIZE - PROGRESS_BAR_HEIGTH + 8, right - left,
				PROGRESS_BAR_HEIGTH - 16, 10, 10);

		// rahmen
		g2.setColor(getPlayerColor(currentPlayer));
		g2.fillRect(0, 0, getWidth(), BORDER_SIZE);
		g2.fillRect(0, getHeight() - BORDER_SIZE, getWidth(), BORDER_SIZE);
		g2.fillRect(0, 0, BORDER_SIZE, getHeight());
		g2.fillRect(getWidth() - BORDER_SIZE, 0, BORDER_SIZE, getHeight());

		// spielerinfo rot
		Player player = gameState.getRedPlayer();
		int x = BORDER_SIZE + STUFF_GAP;
		int y = getHeight() - BORDER_SIZE - PROGRESS_BAR_HEIGTH - STUFF_GAP - CARD_HEGTH;

		for (Card card : player.getCards()) {
			paintCard(g2, x, y, card.slot);
			x += CARD_WIDTH + STUFF_GAP;
		}

		synchronized (redSegments) {
			for (TowerData tower : redSegments) {
				if (tower != selectedSegment) {
					paintTower(g2, tower);
				}
			}
		}

		g2.setFont(h2);
		y -= (STUFF_GAP + 5);
		g2.setColor(getPlayerColor(PlayerColor.RED));
		g2.drawString(player.getDisplayName(), 2 * BORDER_SIZE, y);

		// spielerinfo blau
		player = gameState.getBluePlayer();
		x = getWidth() - BORDER_SIZE - STUFF_GAP - CARD_WIDTH;
		y = getHeight() - BORDER_SIZE - PROGRESS_BAR_HEIGTH - STUFF_GAP - CARD_HEGTH;

		for (Card card : player.getCards()) {
			paintCard(g2, x, y, card.slot);
			x -= CARD_WIDTH + STUFF_GAP;
		}

		synchronized (blueSegments) {
			for (TowerData tower : blueSegments) {
				if (tower != selectedSegment) {
					paintTower(g2, tower);
				}
			}
		}

		g2.setFont(h2);
		y -= (STUFF_GAP + 5);
		g2.setColor(getPlayerColor(PlayerColor.BLUE));
		int nameWidth = fmH2.stringWidth(player.getDisplayName());
		g2.drawString(player.getDisplayName(), getWidth() - 2 * BORDER_SIZE - nameWidth, y);

		// staedte
		g2.setFont(h4);
		for (int i = CITIES - 1; i >= 0; i--) {
			for (int j = SLOTS - 1; j >= 0; j--) {
				TowerData data = cityTowers[i][j];
				paintTower(g2, data, true, data == selectedTower);

				if (data.size > 0) {
					g2.setColor(Color.DARK_GRAY.darker());
					g2.fillRoundRect(data.xs[1], data.ys[1] - fmH4.getHeight() - 6, TOWER_RIGHT_WIDTH, fmH4
							.getHeight(), 8, 8);
					g2.setColor(data.diff > MAX_SEGMENT_SIZE ? Color.YELLOW : Color.WHITE);
					String s = Integer.toString(data.diff);
					g2.drawString(s, data.xs[1] + (TOWER_RIGHT_WIDTH - fmH4.stringWidth(s)) / 2,
							data.ys[1] - 8);
				}
			}
		}

		// fortschirttsbalken, icon
		g2.drawImage(progressIcon, left + progress - PROGRESS_ICON_SIZE / 2 + 3, getHeight()
				- PROGRESS_ICON_SIZE - 3, PROGRESS_ICON_SIZE, PROGRESS_ICON_SIZE, this);

	}

	private void paintDynamicComponents(Graphics2D g2) {

		if (selectedSegment != null) {
			paintTower(g2, selectedSegment);
		}

	}

	private void paintSelectDialog(Graphics2D g2) {

		g2.setColor(getTransparentColor(new Color(200, 240, 200), 160));
		g2.fillRoundRect(selectX, selectY, selectWidth, selectHeight, 25, 25);

		String msg = SELECT_STRING;
		int msgW = fmH1.stringWidth(msg);
		int msgH = fmH1.getHeight();

		// menueueberschrift
		g2.setFont(h1);
		g2.setColor(getPlayerColor(currentPlayer));
		g2.drawString(msg, (getWidth() - msgW) / 2, selectY + GAP_SIZE + msgH - 5);

		// tuerme zeichnen
		synchronized (selectTowers) {
			for (TowerData tower : selectTowers) {
				paintTower(g2, tower);
			}
		}

		// button
		String okay = "Abschicken";
		int okayW = fmH3.stringWidth(okay);

		if (selectionOkay) {

			g2.setColor(selectionPressed ? Color.LIGHT_GRAY : Color.GRAY);
			g2.fillRoundRect(selectButton[0], selectButton[1], selectButton[2], selectButton[3], 15, 15);

			g2.setFont(h3);
			g2.setColor(Color.DARK_GRAY);
			g2.drawString(okay, (getWidth() - okayW) / 2, selectY + selectHeight - GAP_SIZE - 10);

			g2.setStroke(new BasicStroke(1f));
			g2.setColor(Color.DARK_GRAY);
			g2.drawRoundRect(selectButton[0], selectButton[1], selectButton[2], selectButton[3], 15, 15);

		}

	}

	private void paintTower(Graphics2D g2, TowerData tower) {

		paintTower(g2, tower, false, false);

	}

	private void paintTower(Graphics2D g2, TowerData tower, boolean forced, boolean glow) {

		g2.setColor(glow ? getPlayerColor(currentPlayer) : getBrightPlayerColor(tower.owner, forced));
		g2.fillPolygon(tower.xs, tower.ys, 6);

		g2.setStroke(new BasicStroke(2f));
		g2.setColor(tower.highlited ? getPlayerColor(currentPlayer) : Color.DARK_GRAY);
		g2.drawPolygon(tower.xs, tower.ys, 6);
		g2.drawLine(tower.innerX, tower.innerY, tower.xs[1], tower.ys[1]);
		g2.drawLine(tower.innerX, tower.innerY, tower.xs[3], tower.ys[3]);
		g2.drawLine(tower.innerX, tower.innerY, tower.xs[5], tower.ys[5]);

	}

	private void paintCard(Graphics2D g2, int x, int y, int slot) {

		g2.setStroke(new BasicStroke(1.5f));
		g2.setColor(Color.WHITE);
		g2.fillRoundRect(x, y, CARD_WIDTH, CARD_HEGTH, 10, 10);

		g2.setColor(Color.DARK_GRAY);
		g2.drawRoundRect(x, y, CARD_WIDTH, CARD_HEGTH, 10, 10);

		g2.fillRect(x + 4 + slot * 6, y + CARD_HEGTH - 12 - slot * 12, 8, 8);
		for (int i = 0; i < SLOTS; i++) {
			g2.drawRect(x + 4 + i * 6, y + CARD_HEGTH - 12 - i * 12, 8, 8);
		}

		g2.setFont(h3);
		String s = Integer.toString(slot + 1);
		int sW = fmH3.stringWidth(s);
		g2.drawString(s, x + CARD_WIDTH - 6 - sW, y + CARD_HEGTH - 4);

	}

	public boolean inner(int x, int y, int[] xs, int[] ys) {

		boolean inner = true;
		double scalar;
		double ref = 0;
		int n = xs.length;

		for (int i = 0; i < n; i++) {
			int j = (i + 1) % n;
			scalar = (ys[j] - ys[i]) * (x - xs[i]) + (xs[i] - xs[j]) * (y - ys[i]);

			if (i == 0) {
				ref = Math.signum(scalar);
			}

			if (Math.signum(scalar) != ref) {
				inner = false;
				break;
			}
		}

		return inner;

	}

	private Color getTransparentColor(Color c, int alpha) {
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), OPTIONS[TRANSPARANCY] ? alpha : 255);
	}

	private Color getPlayerColor(PlayerColor player) {
		return getPlayerColor(player, false);
	}

	private Color getPlayerColor(PlayerColor player, boolean forced) {
		Color color;

		if (player == null || (player != currentPlayer && !forced)) {
			return Color.DARK_GRAY;
		}

		switch (player) {
		case RED:
			color = Color.RED;
			break;
		case BLUE:
			color = Color.BLUE;
			break;

		default:
			color = Color.DARK_GRAY;
		}

		return color;
	}

	private Color getBrightPlayerColor(PlayerColor player, boolean forced) {
		Color color;

		if (player == null || (player != currentPlayer && !forced)) {
			return Color.GRAY;
		}

		switch (player) {
		case RED:
			color = new Color(255, 60, 60);
			break;
		case BLUE:
			color = new Color(80, 80, 255);
			break;

		default:
			color = Color.GRAY;
		}

		return color;
	}

	public Image getImage() {
		BufferedImage img;
		img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		paint(img.getGraphics());
		return img;
	}

	private static Image loadImage(String filename) {
		URL url = FrameRenderer.class.getClassLoader().getResource(filename);

		if (url == null) {
			return null;
		}
		return (new ImageIcon(url)).getImage();
	}

}