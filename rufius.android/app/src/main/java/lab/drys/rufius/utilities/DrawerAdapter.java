package lab.drys.rufius.utilities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import lab.drys.rufius.R;

/**
 * Created by lykanthrop on 6/19/15.
 */
public class DrawerAdapter extends ArrayAdapter<DrawerItem>
{
	public DrawerAdapter(Context context, ArrayList<DrawerItem> itemsList)
	{
		super(context, R.layout.drawer_list_item,itemsList);
		this.itemsList = itemsList;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		if(convertView==null)
		{
			convertView = ((LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.drawer_list_item,parent,false);
		}

		TextView name = (TextView)convertView.findViewById(R.id.drawer_text);
		ImageView icon = (ImageView)convertView.findViewById(R.id.drawer_icon);

		DrawerItem item = itemsList.get(position);
		name.setText(item.getText());
		icon.setImageResource(item.getResourceIcon());

		return convertView;
	}

	//Variables
	private ArrayList<DrawerItem> itemsList;
}
