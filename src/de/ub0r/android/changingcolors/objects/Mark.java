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

/**
 * Mark colors.
 * 
 * @author Felix Bechstein <f@ub0r.de>
 */
public final class Mark {
	/** Coordinates. */
	private final int mX, mY;

	/**
	 * Create a {@link Mark}.
	 * 
	 * @param pX
	 *            column
	 * @param pY
	 *            row
	 */
	public Mark(final int pX, final int pY) {
		this.mX = pX;
		this.mY = pY;
	}

	/**
	 * Create a {@link Mark}.
	 * 
	 * @param pX
	 *            X
	 * @param pY
	 *            Y
	 */
	public Mark(final float pX, final float pY) {
		this(Block.translateXfromScene(pX), Block.translateYfromScene(pY));
	}

	/**
	 * @return X
	 */
	public int getX() {
		return this.mX;
	}

	/**
	 * @return Y
	 */
	public int getY() {
		return this.mY;
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
		} else if (!(object instanceof Mark)) {
			return false;
		} else {
			Mark m = (Mark) object;
			return this.mX == m.getX() && this.mY == m.getY();
		}
	}

	@Override
	public String toString() {
		return "Mark: x=" + this.mX + " y=" + this.mY;
	}
}
