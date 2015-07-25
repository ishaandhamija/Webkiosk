package com.blackMonster.webkiosk.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.blackMonster.webkiosk.SharedPrefs.MainPrefs;
import com.blackMonster.webkiosk.SharedPrefs.RefreshServicePrefs;
import com.blackMonster.webkiosk.controller.UpdateAvgAtnd;
import com.blackMonster.webkiosk.databases.TimetableDbHelper;
import com.blackMonster.webkiosk.controller.RefreshDB;
import com.blackMonster.webkioskApp.R;

import java.util.Calendar;

public class TimetableActivity extends StartupActivity {
	public String TAG = "TimetableActivity";
	public static final int WORKING_DAYS_IN_WEEK = 6;

	ViewPager mViewPager = null;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		getSupportActionBar().setTitle(
				getResources().getString(R.string.action_title_timetable));

		getLayoutInflater().inflate(R.layout.swipe_view, activityContent);

		((PagerTabStrip) findViewById(R.id.pager_title_strip))
				.setTabIndicatorColor(getResources().getColor(R.color.theme));

		mViewPager = (ViewPager) findViewById(R.id.timetable_pager);

		mViewPager.setAdapter(new PageAdapter(getSupportFragmentManager(),
				mViewPager));

		mViewPager.setCurrentItem(54 + getDay() - 2);

		showOverLayIfFirstTime();

	}

	private void showOverLayIfFirstTime() {

		if (MainPrefs.isFirstTime(this)
				&& StartupActivity.isStartupActivity(getClass(), this)) {
			getSupportActionBar().hide();
			final FrameLayout mFrame = (FrameLayout) findViewById(R.id.base_frame);
			final View tutView = LayoutInflater.from(getBaseContext()).inflate(
					R.layout.help_overlay, null);
			mFrame.addView(tutView);
			tutView.setOnTouchListener(new View.OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					MainPrefs.setFirstTimeOver(getBaseContext());
					mFrame.removeView(tutView);
					getSupportActionBar().show();
					return false;

				}

			});

		}

	}

	private int getDay() {
		int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
		if (day == Calendar.SUNDAY)
			day = Calendar.MONDAY;	//To show monday timetable on sunday.
		return day;
	}

	public class PageAdapter extends FragmentStatePagerAdapter implements
			OnPageChangeListener {
		ViewPager viewPager;
		public static final int PAGER_CHILD_COUNT = 100;	//Hack to create circular view pager effect.
		String[] daysOfWeek = getResources().getStringArray(
				R.array.days_of_week);

		public PageAdapter(FragmentManager fm, ViewPager viewPager) {
			super(fm);
			this.viewPager = viewPager;
		}

		@Override
		public Fragment getItem(int i) {

			i = i % WORKING_DAYS_IN_WEEK;
			Fragment fragment = new TimetableListFragment();
			Bundle args = new Bundle();
			args.putInt(TimetableListFragment.ARG_DAY, i + 2);
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public int getItemPosition(Object object) {
			TimetableListFragment fragment = (TimetableListFragment) object;

			if (fragment.CURRENT_DAY == viewPager.getCurrentItem()
					% WORKING_DAYS_IN_WEEK + Calendar.MONDAY) {
				try {
					fragment.updateThisFragment();
				} catch (Exception e) {
					e.printStackTrace();
					return POSITION_NONE;
				}
				return POSITION_UNCHANGED;
			}

			return POSITION_NONE;
		}

		@Override
		public int getCount() {

			return PAGER_CHILD_COUNT;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			position = position % WORKING_DAYS_IN_WEEK;
			switch (position) {
			case 0:
				return daysOfWeek[0];
			case 1:
				return daysOfWeek[1];
			case 2:
				return daysOfWeek[2];
			case 3:
				return daysOfWeek[3];
			case 4:
				return daysOfWeek[4];
			case 5:
				return daysOfWeek[5];
			default:
				return "unknown";
			}
		}

		@Override
		public void setPrimaryItem(ViewGroup container, int position,
				Object object) {
			super.setPrimaryItem(container, 3, object);
		}

		@Override
		public void onPageScrollStateChanged(int arg0) {

		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {

		}

		@Override
		public void onPageSelected(int position) {

			if (position == 0) {
				viewPager.setCurrentItem(PAGER_CHILD_COUNT - 2, false);
			} else if (position == PAGER_CHILD_COUNT - 1) {
				viewPager.setCurrentItem(1, false);
			}
		}
	}

	@Override
	public void registerReceivers() {
		// Log.d(TAG, "subclass register receiver");
		if (!isReceiverRegistered) {
			// Log.d(TAG, "registered tempatnd");

			LocalBroadcastManager.getInstance(this).registerReceiver(
					broadcastUpdateAttendanceResult,
					new IntentFilter(
							RefreshDB.BROADCAST_UPDATE_ATND_RESULT));
		}

		super.registerReceivers();
	}

	@Override
	public void unregisterIfRegistered() {
		// Log.d(TAG, "subclass unregister receiver");
		if (isReceiverRegistered) {
			// Log.d(TAG, "unregistered tempatnd");

			LocalBroadcastManager.getInstance(this).unregisterReceiver(
					broadcastUpdateAttendanceResult);
		}
		super.unregisterIfRegistered();
	}

	BroadcastReceiver broadcastUpdateAttendanceResult = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// Log.d(TAG, "received : BroadcastTempAtndResult");

			unanimateRefreshButton();

			int result = intent.getExtras().getInt(
					RefreshDB.BROADCAST_UPDATE_ATND_RESULT);

			if (result == UpdateAvgAtnd.ERROR) {
				AlertDialogHandler.checkDialog(TimetableActivity.this);
			} else {
				makeToast(result);
				updateUI();

			}

		}

		private void makeToast(int result) {
			Toast.makeText(TimetableActivity.this,
					result + " " + getString(R.string.temp_attendence_updated),
					Toast.LENGTH_LONG).show();
		}
	};

	private void updateUI() {
		mViewPager.getAdapter().notifyDataSetChanged();

	}

	@Override
	public void inflateOnCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		if (TimetableDbHelper.databaseExists(this))
			inflater.inflate(R.menu.optionsmenu_timetable, menu);
		else
			inflater.inflate(R.menu.mainmenu, menu);
		// Log.d(TAG, "oncreateoptinosmenu");
	}

	BroadcastAddClassDialog broadcastAddClassDialog;

	@Override
	boolean switchForOnOptionsItemSelected(int itemId) {
		switch (itemId) {

		case R.id.action_add_to_timetable:
			registerReceiver();
			DialogFragment dialogFragment = new AddClassDialog();

			Bundle args = new Bundle();

			dialogFragment.show(getSupportFragmentManager(), "timetable");
			return true;
		default:
			return super.switchForOnOptionsItemSelected(itemId);
		}
	}

	private void registerReceiver() {
		broadcastAddClassDialog = new BroadcastAddClassDialog();
		LocalBroadcastManager.getInstance(this).registerReceiver(
				broadcastAddClassDialog,
				new IntentFilter(AddClassDialog.BROADCAST_ADD_CLASS_DIALOG));
	}

	private class BroadcastAddClassDialog extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			try {
				mViewPager.getAdapter().notifyDataSetChanged();
				LocalBroadcastManager.getInstance(getBaseContext())
						.unregisterReceiver(broadcastAddClassDialog);
				broadcastAddClassDialog = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	protected void onPause() {
		super.onPause();

		unregisterIfRegistered();
		AlertDialogHandler.dismissIfPresent();
		unanimateRefreshButton();
	}

	@Override
	protected void onResume() {
		super.onResume();
		RefreshServicePrefs.resetIfrunningFromLongTime(this);
		updateUI();
		AlertDialogHandler.checkDialog(this);

		if (RefreshServicePrefs.isStatus(RefreshServicePrefs.LOGGING_IN, this)
				|| RefreshServicePrefs.isStatus(
						RefreshServicePrefs.REFRESHING_O, this)) {
			animateRefreshButton();
			registerReceivers();
		}

	}

}
