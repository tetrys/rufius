package lab.drys.rufius.unit;

import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import lab.drys.rufius.MainActivity;
import lab.drys.rufius.R;
import lab.drys.rufius.Rufius;
import lab.drys.rufius.services.SShService;
import lab.drys.rufius.utilities.DialogPermission;
import lab.drys.rufius.utilities.DialogUnitDeletion;

/**
 * Created by lykanthrop on 6/8/15.
 */
public class UnitsListFragment extends ListFragment implements AbsListView.MultiChoiceModeListener, SwipeRefreshLayout.OnRefreshListener
{
	@Override
	public void onCreate(Bundle savedInstance)
	{
		super.onCreate(savedInstance);
		unitsList = new ArrayList<>();
		unitsBundles = new ArrayList<>();
		selectedItems = new ArrayList<>();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.fragment_units_list, container, false);
		swipeRefreshLayout = (SwipeRefreshLayout)v.findViewById(R.id.swiper);
		textEmpty = (TextView)v.findViewById(R.id.text_empty);

		swipeRefreshLayout.setOnRefreshListener(this);

		unitsArrayAdapter = new UnitsArrayAdapter(this.getActivity(),unitsList);
		this.setListAdapter(unitsArrayAdapter);

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstance)
	{
		super.onActivityCreated(savedInstance);

		this.getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		this.getListView().setMultiChoiceModeListener(this);
		this.getListView().setSelector(R.drawable.unitlist_selector);
		this.getListView().setItemsCanFocus(true);
	}

	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked)
	{
		int selectedNumber = selectedItems.size();

		String str = ((SharedPreferences)this.getListAdapter().getItem(position)).getString(Rufius.unit_code, "");
		if(checked)
		{
			selectedItems.add(str);
		}
		else
		{
			selectedItems.remove(str);
		}

		if(((selectedNumber==1) && (selectedItems.size()>1)) || ((selectedNumber>1) && (selectedItems.size()==1)))
		{
			onPrepareActionMode(mode,menu);
		}
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu mn)
	{
		menu = mn;
		mode.getMenuInflater().inflate(R.menu.units_list_action_menu, menu);

		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu)
	{
		if(menu!=null)
		{
			if(selectedItems.size()<2)
			{
				menu.findItem(R.id.unit_settings).setVisible(true);
                menu.findItem(R.id.unit_info).setVisible(true);
			}
			else
			{
                menu.findItem(R.id.unit_settings).setVisible(true);
				menu.findItem(R.id.unit_info).setVisible(false);
			}
		}

		return true;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item)
	{
		actMode = mode;

		if(!selectedItems.isEmpty())
		{
			switch(item.getItemId())
			{
				case R.id.unit_info :
					SharedPreferences shpr = this.getActivity().getSharedPreferences(selectedItems.get(0), Context.MODE_PRIVATE);
					((MainActivity) this.getActivity()).showToastInfo(Rufius.generateInfo(shpr));
					break;

				case R.id.unit_delete :

					DialogUnitDeletion dialog = new DialogUnitDeletion();
					Bundle bundle = new Bundle();
					bundle.putSerializable(Rufius.unit,selectedItems);
					dialog.setArguments(bundle);
					dialog.show(this.getFragmentManager(),"");
					break;

				case R.id.unit_settings :

					Intent intent = new Intent(this.getActivity(), UnitPreferencesActivity.class);
					intent.putExtra(Rufius.ready,true);
					intent.putExtra(Rufius.unit_code, selectedItems.get(0));
					startActivityForResult(intent, Rufius.REQUEST_CODE_EDIT_UNIT);
					this.endActionMode();
					break;

				case R.id.unit_status_update:
					this.spotCheck();
					this.endActionMode();
					break;
			}
		}

		return false;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode)
	{
		selectedItems.clear();
		actMode = null;
	}

	@Override
	public void onListItemClick(ListView list, View view, int position, long id)
	{
		Bundle bndl = unitsBundles.get(position);
		if(bndl!=null)
		{
			DialogPermission dlh = new DialogPermission();
			dlh.setArguments(bndl);
			dlh.show(this.getActivity().getFragmentManager(), "");

			unitsBundles.set(position,null);
		}
		else
		{
			Intent intent = new Intent(this.getActivity(),UnitActivity.class);
			intent.putExtra(Rufius.unit_code,unitsList.get(position).getString(Rufius.unit_code,null));
			this.getActivity().startActivity(intent);
		}
	}

	@Override
	public void onRefresh()
	{
		swipeRefreshLayout.setRefreshing(false);
		this.spotCheck();
	}

	public void spotCheck()
	{
		String netId = Rufius.getSSIDBSSID(this.getActivity());
		String ssidbssid;

		SharedPreferences commonPreferences = this.getActivity().getSharedPreferences(Rufius.statik,Context.MODE_PRIVATE);
		if(commonPreferences!=null)
		{
			if(netId!=null)
			{
				commonPreferences.edit().putString(Rufius.unit_code, netId).apply();
			}
			else
			{
				commonPreferences.edit().putString(Rufius.unit_code,null).apply();
			}
		}

		if(netId!=null)
		{
			for(SharedPreferences sp : unitsList)
			{
				ssidbssid = sp.getString(Rufius.unit_code, null);

				if((ssidbssid!=null) && (!ssidbssid.isEmpty()))
				{
					if(ssidbssid.equals(netId))
					{
						Intent intent0 = new Intent(this.getActivity(), SShService.class);
						intent0.putExtra(Rufius.unit_code,ssidbssid);
						intent0.putExtra(Rufius.command, Rufius.COM_INWIFI);
						intent0.putExtra(Rufius.auto,false);
						this.getActivity().startService(intent0);

						commonPreferences.edit().putString(Rufius.unit_code,ssidbssid);
					}
					else
					{
						Intent intent0 = new Intent(this.getActivity(), SShService.class);
						intent0.putExtra(Rufius.unit_code,ssidbssid);
						intent0.putExtra(Rufius.command, Rufius.COM_OUTWIFI);
						intent0.putExtra(Rufius.auto,false);
						this.getActivity().startService(intent0);
					}
				}
			}
		}
	}

	public void endActionMode()
	{
		if(actMode!=null)
		{
			actMode.finish();
		}
	}

	public void add(SharedPreferences unit)
	{
		unitsList.add(unit);
		unitsBundles.add(null);

		this.updateList();
	}

	public void set(String unit, Bundle bundle)
	{
		SharedPreferences shpr = this.getActivity().getSharedPreferences(unit,Context.MODE_PRIVATE);

		int index = unitsList.indexOf(shpr);
		if(index>-1)
		{
			unitsBundles.set(index, bundle);
		}
	}

	public void createList()
	{
		File unitsDir = new File(this.getActivity().getApplicationInfo().dataDir,"shared_prefs");

		unitsList.clear();
		unitsBundles.clear();
		if(unitsDir.exists())
		{

			String[] fileNames = unitsDir.list();
			SharedPreferences sp;
			boolean spok = false;

			for(String f : fileNames)
			{
				f = f.substring(0,f.length()-4);
				if(!f.equals(Rufius.statik))
				{
					sp = this.getActivity().getSharedPreferences(f, Context.MODE_PRIVATE);

					spok = sp.getBoolean(Rufius.ready,false);

					if(spok)
					{
						unitsList.add(sp);
						unitsBundles.add(null);

					}
					else
					{
						File fl = new File(this.getActivity().getApplicationInfo().dataDir+"/shared_prefs/"+f+".xml");
						if(fl.exists())
						{
							fl.delete();
						}
					}
				}
			}
		}

		this.updateList();
	}

	public void updateList()
	{
		if(unitsList.size()>0)
		{
			textEmpty.setVisibility(View.INVISIBLE);
		}
		else
		{
			textEmpty.setVisibility(View.VISIBLE);
		}

		unitsArrayAdapter.notifyDataSetChanged();
	}

	//Variables
	private static Menu menu;
	private static ActionMode actMode;
	private static UnitsArrayAdapter unitsArrayAdapter;
	private static ArrayList<SharedPreferences> unitsList;
	private static ArrayList<String> selectedItems;
	private static ArrayList<Bundle> unitsBundles;
	private TextView textEmpty;

	private static SwipeRefreshLayout swipeRefreshLayout;
}
