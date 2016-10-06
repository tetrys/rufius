package lab.drys.rufius.unit;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import lab.drys.rufius.R;
import lab.drys.rufius.Rufius;

/**
 * Created by lykanthrop on 6/2/15.
 */
public class UnitsArrayAdapter extends ArrayAdapter<SharedPreferences>
{
	public UnitsArrayAdapter(Context context, ArrayList<SharedPreferences> unitsList)
	{
		super(context, R.layout.unit_list_item,unitsList);
		this.unitsList = unitsList;
		this.currentNetwork = context.getSharedPreferences(Rufius.statik,Context.MODE_PRIVATE).getString(Rufius.unit_code,"");
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		if(convertView==null)
		{
			convertView = ((LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.unit_list_item,parent,false);
		}

		TextView name = (TextView)convertView.findViewById(R.id.unit_name);
		TextView description = (TextView)convertView.findViewById(R.id.unit_desription);
		ImageView statusIcon = (ImageView)convertView.findViewById(R.id.status_icon);
		ImageView infoIcon = (ImageView)convertView.findViewById(R.id.info_icon);
		ImageView snapIcon = (ImageView)convertView.findViewById(R.id.snap_icon);
		ImageView netIcon = (ImageView)convertView.findViewById(R.id.edge_icon);

		SharedPreferences unitPreferences = unitsList.get(position);

		String str = unitPreferences.getString(Rufius.unit, "");
		if(str.isEmpty())
		{
			str = "Unnamed Unit";
		}
		name.setText(str);
		str = unitPreferences.getString(Rufius.desc, "Description");
		if(str.isEmpty())
		{
			str = "No Description";
		}
		description.setText(str);

		int status = unitPreferences.getInt(Rufius.status, 0xfff0000);

		statusIcon.setImageResource(Rufius.getStatusResourceId(status));

		if((status&0xfff0000)==0)
		{
			infoIcon.setVisibility(View.VISIBLE);
			if((status&0x400)!=0)
			{
				infoIcon.setImageResource(R.drawable.ic_notifications_black_48dp);
			}
			else
			{
				infoIcon.setImageResource(R.drawable.ic_notifications_off_black_48dp);
			}
			snapIcon.setVisibility(View.VISIBLE);
			if((status&0x4)!=0)
			{
				snapIcon.setImageResource(R.drawable.ic_party_mode_black_48dp);
			}
			else
			{
				snapIcon.setImageResource(R.drawable.ic_party_mode_off_black_48dp);
			}
		}
		else
		{
			infoIcon.setVisibility(View.INVISIBLE);
			snapIcon.setVisibility(View.INVISIBLE);
		}

		if(currentNetwork.equals(unitPreferences.getString(Rufius.unit_code,"")))
		{
			netIcon.setImageResource(R.drawable.ic_home_in_black_48dp);
		}
		else
		{
			netIcon.setImageResource(R.drawable.ic_nature_people_black_48dp);
		}

		return convertView;
	}

	//Variables
	private String currentNetwork;
	private ArrayList<SharedPreferences> unitsList;
}
