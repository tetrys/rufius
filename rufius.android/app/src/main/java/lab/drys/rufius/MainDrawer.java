package lab.drys.rufius;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

import lab.drys.rufius.utilities.DialogPermission;
import lab.drys.rufius.utilities.DrawerAdapter;
import lab.drys.rufius.utilities.DrawerItem;

/**
 * Created by lykanthrop on 6/3/15.
 */
public class MainDrawer implements ListView.OnItemClickListener, DrawerLayout.DrawerListener
{
	public MainDrawer(MainActivity mainActivity)
	{
		this.mainActivity = mainActivity;

		drawerItems = new ArrayList<>();

		signIcon = new DrawerItem("Sign Out",R.drawable.ic_sign_out_black);

		commonPreferences = mainActivity.getSharedPreferences(Rufius.statik, Context.MODE_PRIVATE);

		boolean autorefresh = commonPreferences.getBoolean(Rufius.sync,false);

		String usergmail = commonPreferences.getString(Rufius.user_gmail,"");

		signedIn = true;

		if(usergmail==null || usergmail.isEmpty())
		{
			signedIn = false;
			signIcon.setText("Sign In");
			signIcon.setResourceIcon(R.drawable.ic_sign_in_black);
		}

		drawerItems.add(signIcon);
		//drawerItems.add(new DrawerItem("Presence",R.drawable.ic_home_black_48dp));
		drawerItems.add(new DrawerItem("Auto-Refresh Off",R.drawable.ic_sync_disabled_black_48dp));
		drawerItems.add(new DrawerItem("Reset",R.drawable.ic_replay_black_48dp));
		drawerItems.add(new DrawerItem("About",R.drawable.ic_help_black_48dp));

		this.updateSyncItem(autorefresh);

		drawerAdapter = new DrawerAdapter(mainActivity, drawerItems);

		this.drawerLayout = (DrawerLayout)mainActivity.findViewById(R.id.drawer_layout);
		this.drawerList = (ListView)mainActivity.findViewById(R.id.left_drawer_list);

		this.drawerList.setAdapter(drawerAdapter);
		this.drawerList.setOnItemClickListener(this);
		this.drawerLayout.setDrawerListener(this);
	}

	@Override
	public void onDrawerSlide(View drawerView, float slideOffset)
	{

	}

	@Override
	public void onDrawerOpened(View drawerView)
	{
	}

	@Override
	public void onDrawerClosed(View drawerView)
	{
	}

	public boolean isDrawerOpen()
	{
		return drawerLayout.isDrawerOpen(Gravity.LEFT);
	}

	@Override
	public void onDrawerStateChanged(int newState)
	{

	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
	{
		switch(i)
		{
			case 0:
				drawerLayout.closeDrawer(Gravity.LEFT);
				if(!signedIn)
				{
					mainActivity.pickGmailAccount();
				}
				else
				{
					commonPreferences.edit().putString(Rufius.user_gmail,"").apply();
					mainActivity.setGoogleInfo("");
					this.setSignedIn(false);
				}
				break;

			case 1 :
				this.toggleSync();
				break;
			case 2 :
				//mainActivity.declarePresence();
				DialogPermission dialog = new DialogPermission();
				Bundle bundle = new Bundle();
				bundle.putBoolean(Rufius.statik, true);
				dialog.setArguments(bundle);
				drawerLayout.closeDrawer(Gravity.LEFT);
				dialog.show(mainActivity.getFragmentManager(),"");
				break;
		}
	}

	public void openDrawer()
	{
		drawerLayout.openDrawer(Gravity.LEFT);
	}

	public void closeDrawer()
	{
		drawerLayout.closeDrawer(Gravity.LEFT);
	}

	public void setSignedIn(boolean bl)
	{
		signedIn = bl;
		this.updateList();
	}

	public void updateList()
	{
		if(signedIn)
		{
			signIcon.setText("Sign Out");
			signIcon.setResourceIcon(R.drawable.ic_sign_out_black);
		}
		else
		{
			signIcon.setText("Sign In");
			signIcon.setResourceIcon(R.drawable.ic_sign_in_black);
		}

		drawerAdapter.notifyDataSetChanged();
	}

	private void toggleSync()
	{
		boolean auto = commonPreferences.getBoolean(Rufius.sync,false);

		if(auto)
		{
			commonPreferences.edit().putBoolean(Rufius.sync, false).apply();
		}
		else
		{
			commonPreferences.edit().putBoolean(Rufius.sync,true).apply();
		}

		this.updateSyncItem(!auto);
	}

	private void updateSyncItem(boolean bl)
	{
		if(bl)
		{
			drawerItems.get(1).setText("Auto-Refresh On");
			drawerItems.get(1).setResourceIcon(R.drawable.ic_sync_black_48dp);
		}
		else
		{
			drawerItems.get(1).setText("Auto-Refresh Off");
			drawerItems.get(1).setResourceIcon(R.drawable.ic_sync_disabled_black_48dp);
		}

		if(drawerAdapter!=null)
		{
			drawerAdapter.notifyDataSetChanged();
		}
	}

	//Variables
	private MainActivity mainActivity;
	private static SharedPreferences commonPreferences;
	private static DrawerItem signIcon;
	private static ArrayList<DrawerItem> drawerItems;
	private static DrawerAdapter drawerAdapter;
	private static boolean signedIn;
	private DrawerLayout drawerLayout;
	private ListView drawerList;
}
