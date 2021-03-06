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
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.openfeint.api.OpenFeint;
import com.openfeint.api.OpenFeintDelegate;
import com.openfeint.api.OpenFeintSettings;
import com.openfeint.api.resource.Achievement;
import com.openfeint.api.resource.Leaderboard;
import com.openfeint.api.resource.Score;
import com.openfeint.api.ui.Dashboard;

import de.ub0r.android.changingcolors.R;
import de.ub0r.android.changingcolors.objects.Block;
import de.ub0r.android.lib.DonationHelper;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;

/**
 * {@link LayoutGameActivity}.
 * 
 * @author Felix Bechstein <f@ub0r.de>
 */
public final class ChangingColorsActivity extends LayoutGameActivity implements
		OnClickListener {
	/**
	 * Achievment's unlock callback.
	 * 
	 * @author flx
	 */
	private static class UnlockCB extends Achievement.UnlockCB {
		/** {@link Context}. */
		private final Context mContext;
		/** Name. */
		private final String mName;
		/** Text. */
		private final String mText;

		/**
		 * Default constructor.
		 * 
		 * @param pContext
		 *            {@link Context}
		 * @param pName
		 *            name of avhievement
		 * @param pText
		 *            text showed when unlocked; null for default text
		 */
		public UnlockCB(final Context pContext, final String pName,
				final String pText) {
			super();
			this.mContext = pContext;
			this.mName = pName;
			if (pText == null) {
				this.mText = pContext.getString(R.string.achievement_unlocked,
						pName);
			} else {
				this.mText = pText;
			}
		}

		// public UnlockCB(final Context pContext, final String pName,
		// final int pText) {
		// this(pContext, pName, pContext.getString(pText));
		// }

		@Override
		public void onSuccess(final boolean newUnlock) {
			if (newUnlock) {
				Log.i(TAG, "achievment unlocked: " + this.mName);
				Toast.makeText(this.mContext, this.mText, Toast.LENGTH_LONG)
						.show();
			} else {
				Log.i(TAG, "achievment allready unlocked " + this.mName);
			}
		}

		@Override
		public void onFailure(final String exceptionMessage) {
			Log.e(TAG, "achievment unlock failed: " + this.mName
					+ " error message: " + exceptionMessage);
		}

	}

	/** Tag for logging. */
	private static final String TAG = "main";
	/** Ad's unit id. */
	private static final String AD_UNITID = "a14f1f2dd79dfa2";

	/** OpenFeint: game name. */
	private static final String GAME_NAME = "Changing Colors";
	/** OpenFeint: game id. */
	private static final String GAME_ID = "423882";
	/** OpenFeint: game key. */
	private static final String GAME_KEY = "vvqtr05XtZ6Ak2YfFXmw";
	/** OpenFeint: game secret. */
	private static final String GAME_SECRET = "K6rY9qRbQEWRLla6xZ6amkocvUBdQPx0kHWlzALKE";

	/** OpenFeint: settings. */
	private OpenFeintSettings mOpenFeintSettings;

	/** Preference's name: difficulty. */
	private static final String PREFS_DEFFICULTY = "difficulty";

	/** Dialog: options. */
	private static final int DIALOG_DIFFICULTY = 0;
	/** Dialog: about. */
	private static final int DIALOG_ABOUT = 1;

	/** Width of the camera. */
	public static final int CAMERA_WIDTH = 640;
	/** Height of the camera. */
	public static final int CAMERA_HEIGHT = 640;

	/** Time between ticks. */
	public static final float[] DELAYS_TICK = new float[] { 3f, 2f, 1f, 0.5f };
	/** OpenFeint: leaderboards. */
	public static final String[] OPENFEINT_LEADERBOARDS = new String[] {
			"1038807", "1039517", "1039527", "1039537" };

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

	/** Texture atlas. */
	private BitmapTextureAtlas mBitmapTextureAtlas;
	/** Texture region for {@link Block}s. */
	private TextureRegion[] mTextureRegionColors;

	/** The {@link Camera}. */
	private Camera mCamera;
	/** {@link Block}s. */
	private Block[][] mBlocks;
	/** Number of existing blocks. */
	private int mBlockCount;

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

		View vRender = this.findViewById(R.id.rendersurfaceview);
		View vRenderLayout = this.findViewById(R.id.renderlayout);

		DisplayMetrics dm = new DisplayMetrics();
		this.getWindowManager().getDefaultDisplay().getMetrics(dm);
		final int margin = 15;
		int size = -1 * 2 * margin;
		if (dm.widthPixels < dm.heightPixels) {
			size += dm.widthPixels;
		} else {
			size += dm.heightPixels;
		}
		Log.d(TAG, "set layout to size: " + size);
		vRenderLayout.setLayoutParams(new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, size + 2 * margin));
		vRender.setLayoutParams(new FrameLayout.LayoutParams(size, size));

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

		this.mOpenFeintSettings = new OpenFeintSettings(GAME_NAME, GAME_KEY,
				GAME_SECRET, GAME_ID);
		OpenFeint.initialize(this, this.mOpenFeintSettings,
				new OpenFeintDelegate() {
				});

		if (this.getString(R.string.app_version).startsWith("0.")) {
			new Achievement("1455052").unlock(new UnlockCB(this, "0.", null));
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (this.mOpenFeintSettings != null) {
			OpenFeint.onExit();
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
		if (this.mOpenFeintSettings != null) {
			OpenFeint.onResume();
		}
		if (!DonationHelper.hideAds(this)) {
			Ads.loadAd(this, R.id.ad, AD_UNITID, null);
		} else {
			this.findViewById(R.id.ad).setVisibility(View.GONE);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (this.mOpenFeintSettings != null) {
			OpenFeint.onPause();
		}
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
		int l = 8;
		this.mBitmapTextureAtlas = new BitmapTextureAtlas(
				(int) Block.COLOR_WIDTH * l, (int) Block.COLOR_HEIGHT,
				TextureOptions.DEFAULT);
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
		this.mBlockCount = Block.BLOCK_COUNT_WIDTH * Block.BLOCK_COUNT_HEIGHT;
		if (this.mBlocks != null) {
			for (int x = 0; x < Block.BLOCK_COUNT_WIDTH; x++) {
				for (int y = 0; y < Block.BLOCK_COUNT_HEIGHT; y++) {
					this.mBlocks[x][y].detach();
					this.mBlocks[x][y] = null;
				}
			}
		} else {
			this.mBlocks = new Block[Block.BLOCK_COUNT_WIDTH][Block.BLOCK_COUNT_HEIGHT];
		}
		for (int x = 0; x < Block.BLOCK_COUNT_WIDTH; x++) {
			for (int y = 0; y < Block.BLOCK_COUNT_HEIGHT; y++) {
				this.mBlocks[x][y] = new Block(this.mTextureRegionColors,
						Block.COLOR_RANDOM, x, y);
			}
		}

		for (int x = 0; x < Block.BLOCK_COUNT_WIDTH; x++) {
			for (int y = 0; y < Block.BLOCK_COUNT_HEIGHT; y++) {
				this.mBlocks[x][y].attach(this.mEngine, pScene);
			}
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
			/** Marked {@link Block}s. */
			private ArrayList<Block> mMarkedBlocks = new ArrayList<Block>();

			@Override
			public boolean onSceneTouchEvent(final Scene pScene,
					final TouchEvent pSceneTouchEvent) {
				if (ChangingColorsActivity.this.mGameState != STATE_IN_GAME) {
					return false;
				}
				long c = SystemClock.elapsedRealtime();

				final int action = pSceneTouchEvent.getAction();
				final float x = pSceneTouchEvent.getX();
				final float y = pSceneTouchEvent.getY();
				final int cX = Block.translateXfromScene(x);
				final int cY = Block.translateYfromScene(y);
				Log.d(TAG, "touch: action=" + action);
				Log.d(TAG, "touch: x=" + x + " y=" + y);
				Log.d(TAG, "touch: x=" + cX + " y=" + cY, c);

				if (Block.valid(cX, cY)) {
					// TouchEvent on game field
					Log.d(TAG, "valid", c);
					ArrayList<Block> markedBlocks = this.mMarkedBlocks;

					if (action == TouchEvent.ACTION_DOWN) {
						markedBlocks.clear();
					}

					Block block = ChangingColorsActivity.this.getBlock(cX, cY);
					if (block != null && !markedBlocks.contains(block)) {
						markedBlocks.add(block);
					}

					if (action == TouchEvent.ACTION_UP) {
						int l = markedBlocks.size();
						int color = -1;
						if (l <= 1) {
							Log.d(TAG, "marked blocks: " + l, c);
						} else {
							for (int i = 0; i < l; i++) {
								Block b = markedBlocks.get(i);
								Log.d(TAG, i + ": " + b, c);
								if (b == null) {
									continue;
								} else {
									int bcolor = b.getColor();
									if (color < 0) {
										color = bcolor;
									} else if (color != bcolor) {
										Log.d(TAG, "brk:" + color + "!="
												+ bcolor);
										color = -1;
										break;
									}
								}
							}
						}
						if (color > -1) {
							Block b = markedBlocks.get(0);
							Log.d(TAG, "base block: " + b);
							for (int i = 1; i < l; i++) {
								Block bb = markedBlocks.get(i);
								b.merge(bb, ChangingColorsActivity.this);
								ChangingColorsActivity.this.removeBlock(bb);
							}
						}
						markedBlocks.clear();
					}
				}
				Log.d(TAG, "onSceneTouchEvent() finished", c);
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
		for (int x = 0; x < Block.BLOCK_COUNT_WIDTH; x++) {
			for (int y = 0; y < Block.BLOCK_COUNT_HEIGHT; y++) {
				this.mBlocks[x][y].setColor(Block.COLOR_RANDOM);
			}
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
		return this.mBlocks[pX][pY];
	}

	/**
	 * Set a new {@link Block}.
	 * 
	 * @param pX
	 *            column
	 * @param pY
	 *            row
	 * @param pBlock
	 *            {@link Block}
	 */
	public void setBlock(final int pX, final int pY, final Block pBlock) {
		this.mBlocks[pX][pY] = pBlock;
	}

	/**
	 * Remove a dead {@link Block}.
	 * 
	 * @param pBlock
	 *            {@link Block}
	 */
	public void removeBlock(final Block pBlock) {
		--this.mBlockCount;
		if (this.mBlockCount == 1) {
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
			if (this.mOpenFeintSettings != null) {
				Score s = new Score(this.mTime, null);
				Leaderboard l = new Leaderboard(
						OPENFEINT_LEADERBOARDS[this.mDifficulty]);
				s.submitTo(l, new Score.SubmitToCB() {
					@Override
					public void onSuccess(final boolean newHighScore) {
						Log.d(TAG, "score posted");
						// sweet, score was posted
						// MyClass.this.setResult(Activity.RESULT_OK);
						// MyClass.this.finish();
					}

					@Override
					public void onFailure(final String exceptionMessage) {
						Log.d(TAG, "score post failed");
						Toast.makeText(
								ChangingColorsActivity.this,
								"Error (" + exceptionMessage
										+ ") posting score.",
								Toast.LENGTH_SHORT).show();
						// MyClass.this.setResult(Activity.RESULT_CANCELED);
						// MyClass.this.finish();
					}
				});

			}
			this.mGameState = pNewState;
			break;
		case STATE_IN_GAME:
			this.findViewById(R.id.pause).setEnabled(true);
			((Button) this.findViewById(R.id.pause)).setText(R.string.pause);
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
		if (DonationHelper.hideAds(this)) {
			menu.removeItem(R.id.item_donate);
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.difficulty:
			this.showDialog(DIALOG_DIFFICULTY);
			return true;
		case R.id.about:
			this.showDialog(DIALOG_ABOUT);
			return true;
		case R.id.item_donate:
			DonationHelper.startDonationActivity(this, false);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected Dialog onCreateDialog(final int id) {
		switch (id) {
		case DIALOG_DIFFICULTY:
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
			if (this.mOpenFeintSettings == null) {
				Log.d(TAG, "init openfeint");
				this.mOpenFeintSettings = new OpenFeintSettings(GAME_NAME,
						GAME_KEY, GAME_SECRET, GAME_ID);
				OpenFeint.initialize(this, this.mOpenFeintSettings,
						new OpenFeintDelegate() {
						});
			}
			Log.d(TAG, "open dashboard");
			Dashboard.open();
			break;
		default:
			break;
		}
	}
}