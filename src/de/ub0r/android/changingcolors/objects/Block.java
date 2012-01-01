/*
 * Copyright (C) 2011-2012 Felix Bechstein
 * 
 * This file is part of ChangingColors.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package de.ub0r.android.changingcolors.objects;

import java.util.ArrayList;
import java.util.Random;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import android.util.Log;

/**
 * A {@link Block} is a group of colors.
 * 
 * @author Felix Bechstein <f@ub0r.de>
 */
public class Block extends ArrayList<Color> {
	/** Tag for logging. */
	private static final String TAG = Block.class.getSimpleName();
	/** Serial version UID. */
	private static final long serialVersionUID = -4240587306180861954L;

	/** {@link Random} number generator. */
	static final Random RAND = new Random();

	/** Number of columns of {@link Block}s. */
	public static final int BLOCK_COUNT_WIDTH = 10;
	/** Number of rows of {@link Block}s. */
	public static final int BLOCK_COUNT_HEIGHT = 10;

	/** Width of a tile. */
	public static final float COLOR_WIDTH = 32f;
	/** Height of a tile. */
	public static final float COLOR_HEIGHT = 32f;

	/** Index in color array: red. */
	public static final int INDEX_RED = 0;
	/** Index in color array: green. */
	public static final int INDEX_GREEN = 1;
	/** Index in color array: blue. */
	public static final int INDEX_BLUE = 2;
	/** Index in color array: alpha. */
	public static final int INDEX_ALPHA = 3;

	/** Random color. */
	public static final int COLOR_RANDOM = -1;

	/** An array of available colors. */
	public static final float[][] COLORS = new float[][] { { 1f, 0f, 0f, 1f },
			{ 0f, 1f, 0f, 1f }, { 0f, 0f, 1f, 1f }, { 0.5f, 0.5f, 0.5f, 1f } };

	/** {@link TextureRegion} for {@link Color}s. */
	private final TextureRegion[] mTextureRegion;

	/** {@link Block}'s color. */
	private int mColor;

	/** Used {@link Engine}. */
	private Engine mEngine;
	/** {@link Scene} this {@link Block} is attached to. */
	private Scene mScene;

	/**
	 * Translate column to x coordinate.
	 * 
	 * @param pX
	 *            column
	 * @return X
	 */
	public static float translateXtoScene(final int pX) {
		return pX * COLOR_WIDTH;
	}

	/**
	 * Translate row to y coordinate.
	 * 
	 * @param pY
	 *            row
	 * @return Y
	 */
	public static float translateYtoScene(final int pY) {
		return pY * COLOR_HEIGHT;
	}

	/**
	 * Translate x coordinate into column.
	 * 
	 * @param pX
	 *            X
	 * @return column
	 */
	public static int translateXfromScene(final float pX) {
		return (int) (pX / COLOR_WIDTH);
	}

	/**
	 * Translate y coordinate into row.
	 * 
	 * @param pY
	 *            Y
	 * @return row
	 */
	public static int translateYfromScene(final float pY) {
		return (int) (pY / COLOR_WIDTH);
	}

	public static boolean valid(final float pX, final float pY) {
		Log.d(TAG, "valid(" + pX + "," + pY + ")");
		int x = translateXfromScene(pX);
		int y = translateYfromScene(pY);
		Log.d(TAG, "x=" + x + ", y=" + y);
		return x >= 0 && y >= 0 && x < BLOCK_COUNT_WIDTH
				&& y < BLOCK_COUNT_HEIGHT;
	}

	/**
	 * Create a {@link Block} with one color.
	 * 
	 * @param pTexture
	 *            {@link TextureRegion} holding {@link Sprite}s
	 * @param pColor
	 *            {@link Block}'s color
	 * @param pX
	 *            x position in array
	 * @param pY
	 *            y position in array
	 */
	public Block(final TextureRegion[] pTextureRegion, final int pColor,
			final int pX, final int pY) {
		super();
		this.mTextureRegion = pTextureRegion;

		if (pColor < 0 || pColor >= COLORS.length) {
			this.mColor = RAND.nextInt(COLORS.length);
		} else {
			this.mColor = pColor;
		}
		this.add(pX, pY);
	}

	/**
	 * Add a color.
	 * 
	 * @param pX
	 *            x position in array
	 * @param pY
	 *            y position in array
	 */
	public void add(final int pX, final int pY) {
		Color c = new Color(this.mTextureRegion, this.mColor, pX, pY);
		this.add(c);
		if (this.mScene != null) {
			this.attachColor(this.size() - 1);
		}
	}

	/**
	 * Change {@link Block}'s color.
	 * 
	 * @param pColor
	 *            {@link Block}'s color
	 */
	public void setColor(final int pColor) {
		if (pColor < 0 || pColor >= COLORS.length) {
			this.mColor = RAND.nextInt(COLORS.length);
		} else {
			this.mColor = pColor;
		}
		int l = this.size();
		for (int i = 0; i < l; i++) {
			this.get(i).setColor(this.mColor);
		}
	}

	/**
	 * @return color of {@link Block}
	 */
	public int getColor() {
		return this.mColor;
	}

	/**
	 * Merge to {@link Block}s and detach the merged one from scene.
	 * 
	 * @param pTarget
	 *            target {@link Block}
	 */
	public void merge(final Block pTarget) {
		Log.d(TAG, "merge block " + this + " with " + pTarget);
		this.addAll(pTarget);
		this.setNeighbors();
		pTarget.clear();
		pTarget.detach();
	}

	/**
	 * Set neighbors of all colors.
	 */
	private void setNeighbors() {
		int l = this.size();
		for (int i = 0; i < l; i++) {
			Color c0 = this.get(i);
			int x0 = c0.getX();
			int y0 = c0.getY();
			boolean[] neighbors = new boolean[] { false, false, false, false };
			int count = 0;
			for (int j = 0; j < l; j++) {
				Color c1 = this.get(j);
				if (c0 == c1) {
					continue;
				} else {
					int x1 = c1.getX();
					int y1 = c1.getY();
					if (x0 == x1 && y0 + 1 == y1) {
						neighbors[Color.DIRECTION_DOWN] = true;
						++count;
					} else if (x0 == x1 && y0 - 1 == y1) {
						neighbors[Color.DIRECTION_UP] = true;
						++count;
					} else if (y0 == y1 && x0 + 1 == x1) {
						neighbors[Color.DIRECTION_RIGHT] = true;
						++count;
					} else if (y0 == y1 && x0 - 1 == x1) {
						neighbors[Color.DIRECTION_LEFT] = true;
						++count;
					}
					if (count == 4) {
						break;
					}
				}
			}
			c0.setNeighbors(neighbors);
		}
	}

	/**
	 * Attach to scene.
	 * 
	 * @param pEngine
	 *            {@link Engine}
	 * @param pScene
	 *            {@link Scene}
	 */
	public void attach(final Engine pEngine, final Scene pScene) {
		this.mEngine = pEngine;
		this.mScene = pScene;
		int l = this.size();
		for (int i = 0; i < l; i++) {
			this.attachColor(i);
		}
	}

	/**
	 * Attach color.
	 * 
	 * @param i
	 *            index of color.
	 */
	private void attachColor(final int i) {
		this.get(i).attach(this.mScene);
	}

	/**
	 * Detach from scene.
	 */
	public void detach() {
		this.mEngine.runOnUpdateThread(new Runnable() {
			@Override
			public void run() {
				int l = Block.this.size();
				for (int i = 0; i < l; i++) {
					Block.this.detachColor(i);
				}

				Block.this.mEngine = null;
				Block.this.mScene = null;
			}
		});
	}

	/**
	 * Detach color.
	 * 
	 * @param i
	 *            index of color.
	 */
	private void detachColor(final int i) {
		this.get(i).detach(this.mScene);
	}

	@Override
	public String toString() {
		return "Block: size=" + this.size() + " color=" + this.mColor
				+ " attached=" + (this.mScene != null);
	}

	/**
	 * Check if this {@link Block} is blocking some coordinates.
	 * 
	 * @param pX
	 *            X
	 * @param pY
	 *            Y
	 * @return true if {@link Block} has same coordinates
	 */
	public boolean equals(final int pX, final int pY) {
		int l = this.size();
		for (int i = 0; i < l; i++) {
			if (this.get(i).equals(pX, pY)) {
				return true;
			}
		}
		return false;
	}
}
