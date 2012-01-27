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

import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import de.ub0r.android.changingcolors.ui.ChangingColorsActivity;

/**
 * A {@link Color} is a group of {@link Sprite}s representing one filed in game.
 * 
 * @author Felix Bechstein <f@ub0r.de>
 */
public final class Color extends ArrayList<Sprite> {
	/** Tag for logging. */
	// private static final String TAG = Color.class.getSimpleName();
	/** Serial version UID. */
	private static final long serialVersionUID = 3942153681138129850L;

	/** Direction: up. */
	public static final int DIRECTION_UP = 0;
	/** Direction: left. */
	public static final int DIRECTION_LEFT = 1;
	/** Direction: right. */
	public static final int DIRECTION_RIGHT = 2;
	/** Direction: down. */
	public static final int DIRECTION_DOWN = 3;

	/** Move {@link Sprite}s out of view. */
	private static final float MOVE_OUT_OF_VIEW = ChangingColorsActivity.CAMERA_HEIGHT
			* -2;

	/** Is there a neighbor present in any direction? */
	private final float[] mHasNeighbor = new float[] { 0f, 0f, 0f, 0f };

	/** {@link TextureRegion}. */
	private final TextureRegion[] mTextureRegion;
	/** coordinates. */
	private final int mX, mY;
	/** coordinates. */
	private final float mfX, mfY;
	/** {@link Color}'s color. */
	private int mColor;

	/**
	 * Default constructor.
	 * 
	 * @param pTextureRegion
	 *            {@link TextureRegion}
	 * @param pColor
	 *            color
	 * @param pX
	 *            x
	 * @param pY
	 *            y
	 */

	public Color(final TextureRegion[] pTextureRegion, final int pColor,
			final int pX, final int pY) {
		super(8);
		this.mTextureRegion = pTextureRegion;
		this.mX = pX;
		this.mY = pY;
		this.mfX = Block.translateXtoScene(pX);
		this.mfY = Block.translateYtoScene(pY);

		for (int i = 0; i < 8; i++) {
			Sprite s = new Sprite(this.mfX, this.mfY, this.mTextureRegion[i]);
			if (i % 2 == 1) {
				s.setPosition(this.mfX, this.mfY + MOVE_OUT_OF_VIEW);
			}
			this.add(s);
		}
		this.setColor(this.mColor);
	}

	/**
	 * @return x coordinate
	 */
	public int getX() {
		return this.mX;
	}

	/**
	 * @return y coordinate
	 */
	public int getY() {
		return this.mY;
	}

	/**
	 * Attach to scene.
	 * 
	 * @param pScene
	 *            {@link Scene}
	 */
	public void attach(final Scene pScene) {
		for (int i = 0; i < this.size(); i++) {
			pScene.attachChild(this.get(i));
		}
	}

	/**
	 * Detach from {@link Scene}.
	 * 
	 * @param pScene
	 *            {@link Scene}
	 */
	public void detach(final Scene pScene) {
		for (int i = 0; i < this.size(); i++) {
			pScene.detachChild(this.get(i));
		}
	}

	/**
	 * Change {@link Color}'s color.
	 * 
	 * @param pColor
	 *            {@link Color}'s color
	 */
	public void setColor(final int pColor) {
		if (pColor < 0 || pColor >= Block.COLORS.length) {
			this.mColor = Block.RAND.nextInt(Block.COLORS.length);
		} else {
			this.mColor = pColor;
		}
		float[] color = Block.COLORS[this.mColor];
		float r = color[Block.INDEX_RED];
		float g = color[Block.INDEX_GREEN];
		float b = color[Block.INDEX_BLUE];
		// float a = color[Block.INDEX_ALPHA];
		int l = this.size();
		for (int i = 0; i < l; i++) {
			Sprite s = this.get(i);
			s.setColor(r, g, b);
		}
	}

	/**
	 * Set whether a neighbor does exist.
	 * 
	 * @param pDirection
	 *            direction
	 * @param pHasNeighbor
	 *            true, if a neighbor does exist in given direction
	 */
	public void setNeighbor(final int pDirection, final boolean pHasNeighbor) {
		float f;
		if (pHasNeighbor) {
			f = MOVE_OUT_OF_VIEW;
		} else {
			f = 0f;
		}
		this.mHasNeighbor[pDirection] = f;

		Sprite s = this.get(pDirection * 2);
		s.setPosition(this.mfX, this.mfY + f);
		s = this.get(pDirection * 2 + 1);
		s.setPosition(this.mfX, this.mfY + MOVE_OUT_OF_VIEW - f);
	}

	/**
	 * Set whether a neighbor does exist.
	 * 
	 * @param pHasNeighbor
	 *            array, for each direction: true, if a neighbor does exist in
	 *            given direction
	 */
	public void setNeighbors(final boolean[] pHasNeighbor) {
		for (int i = 0; i < 4; i++) {
			this.setNeighbor(i, pHasNeighbor[i]);
		}
	}

	/**
	 * Check if mark has same coordinates.
	 * 
	 * @param pX
	 *            X
	 * @param pY
	 *            Y
	 * @return true if mark has same coordinates
	 */
	public boolean equals(final int pX, final int pY) {
		return this.mX == pX && this.mY == pY;
	}

	/**
	 * Check if mark has same coordinates.
	 * 
	 * @param pX
	 *            X
	 * @param pY
	 *            Y
	 * @return true if mark has same coordinates
	 */
	public boolean equals(final float pX, final float pY) {
		return this.mX == Block.translateXfromScene(pX)
				&& this.mY == Block.translateYfromScene(pY);
	}

	@Override
	public boolean equals(final Object object) {
		if (object == null) {
			return false;
		} else if (object == this) {
			return true;
		} else if (!(object instanceof Color)) {
			return false;
		} else {
			Color c = (Color) object;
			return this.mX == c.getX() && this.mY == c.getY();
		}
	}

	@Override
	public String toString() {
		return "Color: x=" + this.mX + " y=" + this.mY;
	}
}
