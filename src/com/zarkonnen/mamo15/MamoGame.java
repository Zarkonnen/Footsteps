/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.zarkonnen.mamo15;

import com.zarkonnen.catengine.Draw;
import com.zarkonnen.catengine.Fount;
import com.zarkonnen.catengine.Frame;
import com.zarkonnen.catengine.Game;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.ScreenMode;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * @author David Stark
 */
public class MamoGame implements Game {
	public static final Fount OTTO = Fount.fromResource("otto", "/com/zarkonnen/mamo15/otto.txt");
	
	public static enum TileType {
		axe(100, 0, 0, 2),
		fire(255, 0, 0, 2, 15),
		fireplace(0, 0, 0, 1.2),
		house(0, 0, 255, 1.5),
		bigtree(0, 100, 0, 2),
		tree(0, 255, 0, 1.8),
		wood(100, 200, 0, 1.2),
		ground(255, 255, 255, 1);
		
		public final int numFrames;
		public final int r, g, b;
		public final Img[] imgs;
		public final double monsterCost;
		
		private TileType(int r, int g, int b, double monsterCost) {
			this.r = r; this.g = g; this.b = b;
			numFrames = 1;
			imgs = new Img[numFrames];
			for (int i = 0; i < numFrames; i++) {
				imgs[i] = new Img(name() + i);
			}
			this.monsterCost = monsterCost;
		}
		
		private TileType(int r, int g, int b, int numFrames, double monsterCost) {
			this.r = r; this.g = g; this.b = b;
			this.numFrames = numFrames;
			imgs = new Img[numFrames];
			for (int i = 0; i < numFrames; i++) {
				imgs[i] = new Img(name() + i);
			}
			this.monsterCost = monsterCost;
		}
	}
	
	private static final Img upI = new Img("up");
	private static final Img downI = new Img("down");
	private static final Img leftI = new Img("left");
	private static final Img rightI = new Img("right");
	private static final Img playerI = new Img("player");
	private static final Img dither0 = new Img("dither0");
	private static final Img dither1 = new Img("dither1");
	public static final int STARTX = 36, STARTY = 32;
	int px = STARTX, py = STARTY;
	boolean hasAxe = false, hasWood = false;
	Tile[][] map;
	boolean sized = false;
	int sleepTime = 0;
	int time = 0;
	int relight = 0;
	int dayTime = 0;
	int repeat = 0;
	String msg = "You wake up every\nmorning and see\nthe footsteps.";
	boolean gotWood = false;
	boolean touchedTree = false;
	boolean litFire = false;
	boolean touchedFireplace = false;
	boolean foundWood = false;
	boolean foundCabin = false;
	boolean touchedBigTree = false;
	boolean depositedWood = false;
	boolean hasMoved = false;
	int msgDecay = 4500;
	double monsterStrength = 200;
	boolean death = false;
	int deathAmt = 0;
	int intro = 5500;
	int mx, my;
	int monsterLine = 0;
	
	static class Tile {
		TileType type;
		int x, y;
		double cost;
		double value;
		boolean lit;

		public Tile(TileType type, int x, int y) {
			this.type = type;
			this.x = x;
			this.y = y;
		}
		
		boolean footsteps = false;
	}
	
	private void newDay() {
		dayTime = 0;
		for (int y = 0; y < map.length; y++) { for (int x = 0; x < map[0].length; x++) {
			Tile t = map[y][x];
			t.footsteps = false;
			t.lit = false;
			t.cost = r.nextDouble() * 20;
		}}
		for (int y = 0; y < map.length; y++) { for (int x = 0; x < map[0].length; x++) {
			for (int dy = -8; dy < 8; dy++) { for (int dx = -8; dx < 8; dx++) {
				int x2 = x + dx;
				int y2 = y + dy;
				if (x2 < 0 || x2 >= map[0].length || y2 < 0 || y2 >= map.length) {
					continue;
				}
				/*int dsq = dx * dx + dy * dy + 3;
				map[y2][x2].cost += map[y][x].type.monsterCost / dsq;*/
				int dsq = dx * dx + dy * dy;
				if (dsq < 8*8) {
					map[y2][x2].cost = Math.max(map[y2][x2].cost, map[y][x].type.monsterCost);
				}
			}}
		}}
		
		floodFill();
		
		mx = 0;
		my = 0;
		switch (r.nextInt(4)) {
			case 0:
				mx = 12;
				break;
			case 1:
				mx = 79;
				break;
			case 2:
				my = 59;
				break;
			case 3:
				mx = 79;
				my = 59;
				break;
		}
		
		double str = monsterStrength;
		while (str > 0) {
			int bestDx = 0;
			int bestDy = 0;
			double bestValue = map[my][mx].value;
			for (int dy = -1; dy < 2; dy++) { for (int dx = -1; dx < 2; dx++) {
				if (dx != 0 && dy != 0) { continue; }
				if (dx == 0 && dy == 0) { continue; }
				int x2 = mx + dx; int y2 = my + dy;
				if (x2 < 0 || x2 >= map[0].length || y2 < 0 || y2 >= map.length) {
					continue;
				}
				double v = map[y2][x2].value;
				if (v < bestValue) {
					bestDx = dx;
					bestDy = dy;
					bestValue = v;
				}
			}}
			if (bestDx == 0 && bestDy == 0) {
				if (map[my][mx].type == TileType.house) {
					death = true;
					msg("You wake up to find\nthe door is open.\nThere is a noise -");
				}
				break;
			}
			mx += bestDx;
			my += bestDy;
			str -= map[my][mx].cost;
			map[my][mx].footsteps = true;
			//System.out.println(mx + " / " + my);
		}
		
		monsterStrength += 80;
		
		for (int y = 0; y < map.length; y++) { for (int x = 0; x < map[0].length; x++) {
			Tile t = map[y][x];
			if (t.type == TileType.fire) {
				t.type = TileType.fireplace;
			}
		}}
	}
	
	private void floodFill() {
		HashSet<Tile> qs = new HashSet<>();
		LinkedList<Tile> q = new LinkedList<>();
		for (int y = 0; y < map.length; y++) { for (int x = 0; x < map[0].length; x++) {
			Tile t = map[y][x];
			t.value = t.type == TileType.house ? 0 : 1000000;
			if (t.type == TileType.house) {
				q.add(t);
				qs.add(t);
			}
		}}
		
		while (!q.isEmpty()) {
			Tile t = q.pollLast();
			qs.remove(t);
			for (int dy = -1; dy < 2; dy++) { for (int dx = -1; dx < 2; dx++) {
				if (dx != 0 && dy != 0) { continue; }
				if (dx == 0 && dy == 0) { continue; }
				int x2 = t.x + dx; int y2 = t.y + dy;
				if (x2 < 0 || x2 >= map[0].length || y2 < 0 || y2 >= map.length) {
					continue;
				}
				Tile t2 = map[y2][x2];
				double newValue = t.value + t2.cost;
				if (newValue < t2.value) {
					t2.value = newValue;
					if (!qs.contains(t2)) {
						q.push(t2);
						qs.add(t2);
					}
				}
			}}
		}
	}

	public MamoGame() {
		try {
			BufferedImage img = ImageIO.read(MamoGame.class.getResourceAsStream("/com/zarkonnen/mamo15/images/map.png"));
			
			map = new Tile[60][80];
			for (int y = 0; y < map.length; y++) { for (int x = 0; x < map[0].length; x++) {
				Color c = new Color(img.getRGB(x, y));
				TileType type = TileType.ground;
				for (TileType tt : TileType.values()) {
					if (tt.r == c.getRed() && tt.g == c.getGreen() && tt.b == c.getBlue()) {
						type = tt;
					}
				}
				map[y][x] = new Tile(type, x, y);
			}}
			newDay();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	void light(int x, int y) {
		for (int dy = -8; dy < 8; dy++) { for (int dx = -8; dx < 8; dx++) {
			int x2 = x + dx; int y2 = y + dy;
			if (x2 < 0 || x2 >= map[0].length || y2 < 0 || y2 >= map.length) {
				continue;
			}
			int dsq = dx * dx + dy * dy;
			if (dsq < 6 * 6 || (dsq < 7*7 + 1 && r.nextBoolean())) {
				map[y2][x2].lit = true;
			}
		}}
	}

	@Override
	public void input(Input in) {
		in.setCursorVisible(false);

		if (in.keyPressed("ESCAPE")) {
			in.quit();
		}
		
		if (!sized) {
			if (in.keyPressed("1")) {
				ScreenMode sm = new ScreenMode(800, 600, false);
				for (ScreenMode m : in.modes()) {
					if ((m.width > sm.width || m.height > sm.height) && m.width >= 800 && m.height >= 600 && m.fullscreen) {
						sm = m;
					}
				}
				in.setMode(sm);
				sized = true;
			}
			if (in.keyPressed("2")) {
				in.setMode(new ScreenMode(800, 600, true));
				sized = true;
			}
			if (in.keyPressed("3")) {
				in.setMode(new ScreenMode(800, 600, false));
				sized = true;
			}
			
			return;
		}
		
		time += in.msDelta();
		repeat -= in.msDelta();
		msgDecay -= in.msDelta();
		intro -= in.msDelta();
		relight += in.msDelta();
		
		if (intro > 0) {
			return;
		}
		
		if (death) {
			deathAmt += in.msDelta();
			if (deathAmt > 2000 && in.clicked() != null) {
				in.quit();
			} else if (deathAmt > 10000) {
				in.quit();
			}
			return;
		}
		
		if (dayTime > 190) {
			death = true;
			return;
		}
		
		if (sleepTime > 0) {
			sleepTime -= in.msDelta();
			if (sleepTime <= 0) {
				newDay();
			}
			return;
		}
		
		if (relight >= 333) {
			relight %= 333;
			for (int y = 0; y < map.length; y++) { for (int x = 0; x < map[0].length; x++) {
				map[y][x].lit = false;
			}}
			
			for (int y = 0; y < map.length; y++) { for (int x = 0; x < map[0].length; x++) {
				if (map[y][x].type == TileType.fire) {
					light(x, y);
				}
			}}
		}
		
		if (repeat <= 0) {
			if (in.keyDown("LEFT")) {
				step(in, -1, 0);
			}
			if (in.keyDown("RIGHT")) {
				step(in, 1, 0);
			}
			if (in.keyDown("UP")) {
				step(in, 0, -1);
			}
			if (in.keyDown("DOWN")) {
				step(in, 0, 1);
			}
		}
		map[py][px].footsteps = true;
	}
	
	Random r = new Random();
	
	private void step(Input in, int dx, int dy) {
		hasMoved = true;
		Tile t2 = map[py + dy][px + dx];
		repeat = 150;
		switch (t2.type) {
			case ground:
				px += dx;
				py += dy;
				dayTime++;
				if (px == mx && py == my) {
					mx = 0;
					my = 0;
					if (monsterLine == 0) {
						msg("How long did it sit there,\nwatching the house?");
					} else if (monsterLine == 1) {
						msg("It's scared\nof the fire.\nFor now.");
					}
					monsterLine++;
				}
				break;
			case axe:
				msg("An axe.\nIt makes you feel\na little safer.");
				hasAxe = true;
				t2.type = TileType.ground;
				dayTime++;
				break;
			case bigtree:
				if (!touchedBigTree) {
					msg("This tree has endured\nfor ages.");
					touchedBigTree = true;
				}
				break;
			case tree:
				if (hasAxe && !hasWood) {
					hasWood = true;
					dayTime += 8;
					if (!gotWood) {
						msg("You collect\na pile of wood.");
						gotWood = true;
					}
					t2.type = TileType.ground;
				} else if (!touchedTree) {
					msg("The trees crowd\nclose together.");
					touchedTree = true;
				}
				break;
			case fireplace:
				if (hasWood) {
					dayTime += 2;
					hasWood = false;
					t2.type = TileType.fire;
					light(px + dx, py + dy);
					if (!litFire) {
						msg("You light a fire\nto push back\nthe dark.");
						litFire = true;
					}
				} else if (!touchedFireplace) {
					msg("A burnt-out\nfireplace.");
					touchedFireplace = true;
				}
				break;
			case wood:
				if (!hasWood) {
					hasWood = true;
					t2.type = TileType.ground;
					dayTime++;
					if (!foundWood) {
						msg("You pick up a\npile of wood.");
						foundWood = true;
					}
				} else if (!foundWood) {
					msg("You find a\npile of wood.");
					foundWood = true;
				}
				break;
			case fire:
				 if (hasWood && map[py - dy][px - dx].type == TileType.ground) {
					map[py][px].type = TileType.wood;
					px -= dx;
					py -= dy;
					hasWood = false;
					if (!depositedWood) {
						msg("You deposit a pile\nof firewood.");
						depositedWood = true;
					}
					dayTime++;
				 }
				 break;
			case house:
				if (dayTime > 120) {
					sleepTime = 6000;
					dayTime = 0;
					msg("You lock the door\nbehind you.");
				} else if (!foundCabin) {
					msg("Your cabin.\nSturdy enough.\nYou think.");
					foundCabin = true;
				}
				break;
		}
	}
	
	private void msg(String msg) {
		this.msg = msg;
		msgDecay = 4000;
	}
	
	public static final Clr BEIGE = Clr.fromHex("e1df7e");
	public static final Clr RED = Clr.fromHex("ff4003");
	public static final Clr BLACK = new Clr(66, 14, 1);

	@Override
	public void render(Frame f) {
		Draw d = new Draw(f);
		ScreenMode sm = f.mode();
		d.rect(RED, 0, 0, sm.width, sm.height);
		
		if (!sized) {
			d.text("[e1df7e]1: Max screen resolution\n2: 800x600 fullscreen\n3: 800x600 windowed\nEscape: Exit", OTTO, 10, 10);
			return;
		}
		
		d.shift(sm.width / 2 - 400, sm.height / 2 - 300);
		if (death) {
			if (deathAmt > 500) {
				d.rect(BLACK, 0, 0, 800, 600);
				d.text("[ff4003]" + msg, OTTO, 10, 10);
			} else if (deathAmt > 250) {
				for (int y = 0; y < map.length; y++) { for (int x = 0; x < map[0].length; x++) {
					d.blit(dither1, BLACK, x * 10, y * 10);
				}}
			} else if (deathAmt > 0) {
				for (int y = 0; y < map.length; y++) { for (int x = 0; x < map[0].length; x++) {
					d.blit(dither0, BLACK, x * 10, y * 10);
				}}
			}
			return;
		}
		
		if (sleepTime > 0) {
			d.text("[e1df7e]" + msg, OTTO, 10, 10);
			return;
		}
		
		d.rect(BEIGE, 0, 0, 800, 600);
		for (int y = 0; y < map.length; y++) { for (int x = 0; x < map[0].length; x++) {
			Tile t = map[y][x];
			d.blit(t.type.imgs[((time + x + y) / 333) % t.type.imgs.length], RED, x * 10, y * 10);
			if (!t.lit) {
				if (dayTime > 150) {
					d.blit(dither1, RED, x * 10, y * 10);
				} else if (dayTime > 120) {
					d.blit(dither0, RED, x * 10, y * 10);
				}
			}
		}}
		Img pi = playerI;
		if (hasAxe) {
			pi = TileType.axe.imgs[0];
		}
		if (hasWood) {
			pi = TileType.wood.imgs[0];
		}
		d.blit(pi, RED, px * 10, py * 10);
		if (!hasMoved && time > 8000 && (time / 666) % 2 == 0) {
			d.blit(upI, RED, px * 10, py * 10 - 10);
			d.blit(downI, RED, px * 10, py * 10 + 10);
			d.blit(leftI, RED, px * 10 - 10, py * 10);
			d.blit(rightI, RED, px * 10 + 10, py * 10);
		}
		
		if (intro > 500) {
			d.rect(BLACK, 0, 0, 800, 600);
		} else if (intro > 250) {
			for (int y = 0; y < map.length; y++) { for (int x = 0; x < map[0].length; x++) {
				d.blit(dither1, BLACK, x * 10, y * 10);
			}}
		} else if (intro > 0) {
			for (int y = 0; y < map.length; y++) { for (int x = 0; x < map[0].length; x++) {
				d.blit(dither0, BLACK, x * 10, y * 10);
			}}
		}
		
		for (int y = 0; y < map.length; y++) { for (int x = 0; x < map[0].length; x++) {
			Tile t = map[y][x];
			if (t.footsteps && t.type == TileType.ground) {
				d.rect(RED, x * 10 + 4, y * 10 + 4, 2, 2);
			}
		}}
		
		if (msgDecay > 0) {
			d.text("[ff4003]" + msg, OTTO, 10, 10);
		} else if (dayTime > 150) {
			d.text("[ff4003]The sun is setting...", OTTO, 10, 10);
		} else if (dayTime > 120) {
			d.text("[ff4003]It's getting dark.\nTime to return home.", OTTO, 10, 10);
		}
	}
	
}
