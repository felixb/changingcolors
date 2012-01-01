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
package de.ub0r.android.changingcolors.ui;

import java.util.ArrayList;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.handler.timer.ITimerCallback;
import org.anddev.andengine.engine.handler.timer.TimerHandler;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.ui.activity.LayoutGameActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import de.ub0r.android.changingcolors.R;
import de.ub0r.android.changingcolors.objects.Block;
import de.ub0r.android.changingcolors.objects.Mark;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;

/**
 * {@link LayoutGameActivity}.
 * 
 * @author Felix Bechstein <f@ub0r.de>
 */
public class ChangingColorsActivity extends LayoutGameActivity implements
		OnClickListener {
	/** Tag for logging. */
	private static final String TAG = "main";

	/** Preference's name: difficulty. */
	private static final String PREFS_DEFFICULTY = "difficulty";

	/** Dialog: options. */
	private static final int DIALOG_OPTIONS = 0;
	/** Dialog: about. */
	private static final int DIALOG_ABOUT = 1;

	/** Width of the camera. */
	public static final int CAMERA_WIDTH = 320;
	/** Height of the camera. */
	public static final int CAMERA_HEIGHT = 320;

	/** Time between ticks. */
	public static final float[] DELAYS_TICK = new float[] { 3f, 2f, 1f, 0.5f };

	/** State: waiting for game to start. */
	public static final int STATE_NEW_GAME = 0;
	/** State: in game. */
	public static final int STATE_IN_GAME = 1;
	/** State: game has finished. */
	public static final int STATE_PAUSED = 2;
	/** State: game has finished. */
	public static final int STATE_GAME_FINISHED = 3;

	/** State of game. */
	private int mGameState = STATE_NEW_GAME;
	/** Time the game ran already. */
	private int mTime = 0;
	/** Difficulty. */
	private int mDifficulty = 1;
	/** {@link TimerHandler} changing colors. */
	private TimerHandler mChangeColorTimerHandler;

	/** {@link TextView} showing time. */
	private TextView mTvTime;

	/** {@link ArrayList} of marked colors. */
	private ArrayList<Mark> mMarked = new ArrayList<Mark>();
	/** Texture atlas. */
	private BitmapTextureAtlas mBitmapTextureAtlas;
	/** Texture region for {@link Color}s. */
	private TextureRegion[] mTextureRegionColors;

	/** The {@link Camera}. */
	private Camera mCamera;
	/** {@link Block}s. */
	private ArrayList<Block> mBlocks;

	/** Handle UI updates. */
	private Handler mHandler = new Handler() {
		@Override
		public void dispatchMessage(final Message msg) {
			ChangingColorsActivity.this.mTvTime
					.setText(ChangingColorsActivity.this.mTime + "s");
			super.dispatchMessage(msg);
		}
	};

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		Log.init("ChangingColors");

		if (Utils.isApi(Build.VERSION_CODES.HONEYCOMB)) {
			this.setTheme(android.R.style.Theme_Holo);
		} else {
			this.setTheme(android.R.style.Theme);
		}

		super.onCreate(savedInstanceState);

		SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		this.mDifficulty = p.getInt(PREFS_DEFFICULTY, 1);

		this.mTvTime = (TextView) this.findViewById(R.id.time);
		this.findViewById(R.id.new_game).setOnClickListener(this);
		this.findViewById(R.id.pause).setOnClickListener(this);
		this.findViewById(R.id.openfeint).setOnClickListener(this);
		((TextView) this.findViewById(R.id.difficulty))
				.setText(this.getResources().getStringArray(
						R.array.diffuculty_values)[this.mDifficulty]);
	}

	@Override
	protected int getLayoutID() {
		return R.layout.main;
	}

	@Override
	protected int getRenderSurfaceViewID() {
		return R.id.rendersurfaceview;
	}

	@Override
	public Engine onLoadEngine() {
		this.mCamera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);

		return new Engine(new EngineOptions(false, ScreenOrientation.PORTRAIT,
				new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT),
				this.mCamera));
	}

	@Override
	public void onLoadResources() {
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");
		this.mBitmapTextureAtlas = new BitmapTextureAtlas(256, 32,
				TextureOptions.DEFAULT);
		int l = 8;
		this.mTextureRegionColors = new TextureRegion[l];
		for (int i = 0; i < l; i++) {
			Log.d(TAG, "BTATRF.cFA(colors.png," + i + ")");
			this.mTextureRegionColors[i] = BitmapTextureAtlasTextureRegionFactory
					.createFromAsset(this.mBitmapTextureAtlas, this, "colors"
							+ i + ".png", i * (int) Block.COLOR_WIDTH, 0);
		}
		this.mEngine.getTextureManager().loadTexture(this.mBitmapTextureAtlas);
	}

	/**
	 * Initialize {@link Block}s.
	 * 
	 * @param pScene
	 *            {@link Scene} the blocks need to be attached to.
	 */
	private void initBlocks(final Scene pScene) {
		if (this.mBlocks != null) {
			int l = this.mBlocks.size();
			for (int i = 0; i < l; i++) {
				this.mBlocks.get(i).detach();
			}
			this.mBlocks.clear();
			this.mBlocks = null;
		}
		this.mBlocks = new ArrayList<Block>(Block.BLOCK_COUNT_WIDTH
				* Block.BLOCK_COUNT_HEIGHT);
		for (int x = 0; x < Block.BLOCK_COUNT_WIDTH; x++) {
			for (int y = 0; y < Block.BLOCK_COUNT_HEIGHT; y++) {
				this.mBlocks.add(new Block(this.mTextureRegionColors,
						Block.COLOR_RANDOM, x, y));
			}
		}

		int l = this.mBlocks.size();
		for (int i = 0; i < l; i++) {
			this.mBlocks.get(i).attach(this.mEngine, pScene);
		}
		this.changeColor();
		this.mTime = 0;
	}

	@Override
	public Scene onLoadScene() {
		final Scene scene = new Scene();
		scene.setColor(1, 1, 1);

		this.initBlocks(scene);

		scene.setOnSceneTouchListener(new IOnSceneTouchListener() {
			@Override
			public boolean onSceneTouchEvent(final Scene pScene,
					final TouchEvent pSceneTouchEvent) {
				if (ChangingColorsActivity.this.mGameState != STATE_IN_GAME) {
					return false;
				}

				final int action = pSceneTouchEvent.getAction();
				final float x = pSceneTouchEvent.getX();
				final float y = pSceneTouchEvent.getY();
				Log.d(TAG, "touch: action=" + action);
				Log.d(TAG, "touch: x=" + x + " y=" + y);

				if (Block.valid(x, y)) {
					// TouchEvent on game field
					Log.d(TAG, "valid");
					ArrayList<Mark> marks = ChangingColorsActivity.this.mMarked;

					if (action == TouchEvent.ACTION_DOWN) {
						marks.clear();
					}

					int l = marks.size();
					boolean marked = false;
					for (int i = l - 1; i >= 0; i--) {
						if (marks.get(i).equals(x, y)) {
							marked = true;
							break;
						}
					}
					if (!marked) {
						marks.add(new Mark(x, y));
					}
					if (action == TouchEvent.ACTION_UP) {
						l = marks.size();
						for (int i = 0; i < l; i++) {
							Log.d(TAG, i + ": " + marks.get(i));
						}

						int color = -1;
						ArrayList<Block> markedBlocks = new ArrayList<Block>(
								marks.size());
						for (int i = 0; i < l; i++) {
							Mark m = marks.get(i);
							Log.d(TAG, i + ": " + m);
							Block b = ChangingColorsActivity.this.getBlock(
									m.getX(), m.getY());
							if (b == null) {
								continue;
							} else {
								int bcolor = b.getColor();
								Log.d(TAG, "block: " + b);
								if (color < 0) {
									color = bcolor;
									markedBlocks.add(b);
								} else if (markedBlocks.contains(b)) {
									Log.d(TAG, "block already marked: " + b);
									continue;
								} else if (color != bcolor) {
									marks.clear();
									Log.d(TAG, "brk: " + color + "!=" + bcolor);
									break;
								} else {
									markedBlocks.add(b);
								}
							}
						}
						if (marks.isEmpty()) {
							Log.d(TAG, "marks is empty");
						} else {
							int ll = markedBlocks.size();
							Log.d(TAG, "merge blocks: " + ll);
							if (ll > 0) {
								Block b = markedBlocks.get(0);
								Log.d(TAG, "base block: " + b);
								for (int i = 1; i < ll; i++) {
									Block bb = markedBlocks.get(i);
									b.merge(bb);
									ChangingColorsActivity.this.removeBlock(bb);
								}
							}
							marks.clear();
						}
					}
				}

				return true;
			}
		});
		scene.registerUpdateHandler(new TimerHandler(1f, true,
				new ITimerCallback() {
					@Override
					public void onTimePassed(final TimerHandler pTimerHandler) {
						if (ChangingColorsActivity.this.mGameState != STATE_IN_GAME) {
							return;
						}
						++ChangingColorsActivity.this.mTime;
						ChangingColorsActivity.this.mHandler
								.sendEmptyMessage(0);
					}
				}));

		this.mChangeColorTimerHandler = new TimerHandler(
				DELAYS_TICK[this.mDifficulty], true, new ITimerCallback() {
					@Override
					public void onTimePassed(final TimerHandler pTimerHandler) {
						if (ChangingColorsActivity.this.mGameState != STATE_IN_GAME) {
							return;
						}
						ChangingColorsActivity.this.changeColor();
					}
				});
		scene.registerUpdateHandler(this.mChangeColorTimerHandler);

		return scene;
	}

	@Override
	public void onLoadComplete() {
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle(R.string.new_game);
		b.setMessage(R.string.new_game_text);
		b.setCancelable(false);
		b.setPositiveButton(R.string.start,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						ChangingColorsActivity.this.changeState(STATE_IN_GAME);
					}
				});
		b.show();
	}

	/**
	 * Change color of each block.
	 */
	private void changeColor() {
		ArrayList<Block> blocks = this.mBlocks;
		int l = blocks.size();
		for (int i = 0; i < l; i++) {
			blocks.get(i).setColor(Block.COLOR_RANDOM);
		}
	}

	/**
	 * Get {@link Block} by coordinates.
	 * 
	 * @param pX
	 *            column
	 * @param pY
	 *            row
	 * @return {@link Block} or null
	 */
	public Block getBlock(final int pX, final int pY) {
		int l = this.mBlocks.size();
		for (int i = 0; i < l; i++) {
			Block b = this.mBlocks.get(i);
			if (b.equals(pX, pY)) {
				return b;
			}
		}
		return null;
	}

	/**
	 * Remove a dead {@link Block}.
	 * 
	 * @param pBlock
	 *            {@link Block}
	 */
	public void removeBlock(final Block pBlock) {
		this.mBlocks.remove(pBlock);
		if (this.mBlocks.size() == 1) {
			// game won!
			this.changeState(STATE_GAME_FINISHED);
		}
	}

	/**
	 * Change game's state.
	 * 
	 * @param pNewState
	 *            new state
	 */
	private void changeState(final int pNewState) {
		switch (pNewState) {
		case STATE_GAME_FINISHED:
			this.findViewById(R.id.pause).setEnabled(false);
			Toast.makeText(this, "you won!!!!1", Toast.LENGTH_LONG).show();
			this.mGameState = pNewState;
			break;
		case STATE_IN_GAME:
			this.findViewById(R.id.pause).setEnabled(true);
			((Button) this.findViewById(R.id.pause)).setText(R.string.pause);
			// TODO
			this.mGameState = pNewState;
			break;
		case STATE_NEW_GAME:
			((TextView) this.findViewById(R.id.difficulty))
					.setText(this.getResources().getStringArray(
							R.array.diffuculty_values)[this.mDifficulty]);
			if (this.mChangeColorTimerHandler != null) {
				this.mChangeColorTimerHandler
						.setTimerSeconds(DELAYS_TICK[this.mDifficulty]);
			}
			this.findViewById(R.id.pause).setEnabled(false);
			((Button) this.findViewById(R.id.pause)).setText(R.string.pause);
			this.initBlocks(this.getEngine().getScene());
			if (this.mGameState != STATE_NEW_GAME) {
				this.changeState(STATE_IN_GAME);
			}
			break;
		case STATE_PAUSED:
			((Button) this.findViewById(R.id.pause)).setText(R.string.cont);
			this.mGameState = pNewState;
			break;
		default:
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		this.getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.options:
			this.showDialog(DIALOG_OPTIONS);
			return true;
		case R.id.about:
			this.showDialog(DIALOG_ABOUT);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected Dialog onCreateDialog(final int id) {
		switch (id) {
		case DIALOG_OPTIONS:
			final Dialog od = new Dialog(this);
			od.setContentView(R.layout.options);
			((Spinner) od.findViewById(R.id.difficulty))
					.setSelection(this.mDifficulty);
			od.setTitle(R.string.options);
			od.setCancelable(true);
			od.findViewById(R.id.ok).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					ChangingColorsActivity.this.mDifficulty = ((Spinner) od
							.findViewById(R.id.difficulty))
							.getSelectedItemPosition();
					PreferenceManager
							.getDefaultSharedPreferences(
									ChangingColorsActivity.this)
							.edit()
							.putInt(PREFS_DEFFICULTY,
									ChangingColorsActivity.this.mDifficulty)
							.commit();
					ChangingColorsActivity.this.changeState(STATE_NEW_GAME);
					od.dismiss();
				}
			});
			return od;
		case DIALOG_ABOUT:
			final Dialog ad = new Dialog(this);
			ad.setContentView(R.layout.about);
			ad.setTitle(R.string.about);
			ad.setCancelable(true);
			return ad;
		default:
			return null;
		}
	}

	@Override
	public void onClick(final View v) {
		switch (v.getId()) {
		case R.id.new_game:
			this.changeState(STATE_NEW_GAME);
			break;
		case R.id.pause:
			if (this.mGameState == STATE_IN_GAME) {
				this.changeState(STATE_PAUSED);
			} else {
				this.changeState(STATE_IN_GAME);
			}
			break;
		case R.id.openfeint:
			// TODO
			break;
		default:
			break;
		}
	}
}