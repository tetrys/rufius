package lab.drys.rufius.unit;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import lab.drys.rufius.R;
import lab.drys.rufius.Rufius;
import lab.drys.rufius.RufiusActivityBasic;
import lab.drys.rufius.services.SShService;

/**
 * Created by lykanthrop on 7/15/15.
 */
public class UnitActivity extends RufiusActivityBasic
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.activity_unit);

		imageView = (ImageView)this.findViewById(R.id.unit_imageview);
		progressSpin = (ProgressBar)this.findViewById(R.id.unit_progress_spin);
		imageProgress = (ProgressBar) this.findViewById(R.id.image_progress_spin);
		toolbar = (Toolbar)this.findViewById(R.id.unit_toolbar);

		unitFile = this.getIntent().getStringExtra(Rufius.unit_code);

		SharedPreferences unitPreferences = null;
		if((unitFile!=null) && (!unitFile.isEmpty()))
		{
			unitPreferences = this.getSharedPreferences(unitFile,MODE_PRIVATE);
		}

		if(unitPreferences!=null)
		{
			String title = unitPreferences.getString(Rufius.unit,"Rufius");
			toolbar.setTitle(title);
		}

		toolbar.setNavigationIcon(R.drawable.ic_launcher);

		this.setSupportActionBar(toolbar);

		filter = new IntentFilter(UnitActivityReceiver.SET_IMAGE);
		filter.addAction(UnitActivityReceiver.LIST_FAILURE);
		filter.addAction(UnitActivityReceiver.SET_IMAGE_LISTS);
		filter.addAction(UnitActivityReceiver.UPDATE_IMAGE_LISTS);

		receiver = new UnitActivityReceiver(this);

		Bundle bundle = new Bundle();
		bundle.putString(Rufius.desc, "Snapshots");
		snapshotsListFragment = new UnitImageListFragment();
		snapshotsListFragment.setArguments(bundle);
		bundle = new Bundle();
		bundle.putString(Rufius.desc, "Triggers");
		triggersListFragment = new UnitImageListFragment();
		triggersListFragment.setArguments(bundle);

		UnitImagePagerAdapter pagerAdapter = new UnitImagePagerAdapter(this.getSupportFragmentManager());
		pagerAdapter.setFragment(0,snapshotsListFragment);
		pagerAdapter.setFragment(1, triggersListFragment);
		ViewPager pager = (ViewPager) this.findViewById(R.id.pager);
		pager.setAdapter(pagerAdapter);

		TabLayout slidingTabs = (TabLayout)this.findViewById(R.id.sliding_tabs);
		slidingTabs.setupWithViewPager(pager);
	}

	@Override
	public void onResume()
	{
		super.onResume();

		Rufius.unitActivityResumed();

		this.downloadImageList();

		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
	}

	@Override
	public void onPause()
	{
		Rufius.unitActivityPaused();

		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);

		super.onPause();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int caseId = item.getItemId();

		if(caseId == android.R.id.home)
		{
			this.finish();
		}

		return true;
	}

	public void downloadSnapshot(String image)
	{
		imageProgress.setVisibility(View.VISIBLE);
		Intent intent = new Intent(this,SShService.class);
		intent.putExtra(Rufius.unit_code, unitFile);
		intent.putExtra(Rufius.snappath, image);
		this.startService(intent);
	}

	public void downloadTrigger(String image)
	{
		imageProgress.setVisibility(View.VISIBLE);
		Intent intent = new Intent(this,SShService.class);
		intent.putExtra(Rufius.unit_code, unitFile);
		intent.putExtra(Rufius.trigpath, image);
		this.startService(intent);
	}

	public void downloadImageList()
	{
		progressSpin.setVisibility(View.VISIBLE);
		Intent intent = new Intent(this,SShService.class);
		intent.putExtra(Rufius.unit_code, unitFile);
		intent.putExtra(Rufius.desc,true);
		this.startService(intent);
	}

	public void parseLists()
	{
		snapshotsListFragment.populateList(this.getFilesDir().getAbsolutePath() + "/snapshots/" + unitFile + ".snapshots");

		triggersListFragment.populateList(this.getFilesDir().getAbsolutePath() + "/triggers/" + unitFile + ".triggers");

		this.downloadSnapshot("lastsnap.jpg");
	}

	public void getImage()
	{
		if(unitFile!=null)
		{
			File image = new File(this.getFilesDir().getAbsolutePath()+"/snapshots/"+unitFile+".jpg");
			if(image.exists())
			{
				new UnitImageLoader(this).execute(image.getAbsolutePath());
			}
		}
	}

	public void setImage(Bitmap bitmap)
	{
		imageProgress.setVisibility(View.GONE);
		imageView.setImageBitmap(bitmap);
	}

	public void updateLists()
	{
		snapshotsListFragment.updateList();
		triggersListFragment.updateList();
	}

	public void setBusy(boolean b)
	{
		if(b)
		{
			progressSpin.setVisibility(View.VISIBLE);
		}
		else
		{
			progressSpin.setVisibility(View.GONE);
		}
	}

	public void setImageBusy(boolean b)
	{
		if(b)
		{
			imageProgress.setVisibility(View.VISIBLE);
		}
		else
		{
			imageProgress.setVisibility(View.GONE);
		}
	}

	//Variables
	private static String unitFile;
	private static ImageView imageView;
	private static ProgressBar progressSpin;
	private static ProgressBar imageProgress;
	private static Toolbar toolbar;
	private static IntentFilter filter;
	private static UnitActivityReceiver receiver;
	private static UnitImageListFragment snapshotsListFragment;
	private static UnitImageListFragment triggersListFragment;
}
