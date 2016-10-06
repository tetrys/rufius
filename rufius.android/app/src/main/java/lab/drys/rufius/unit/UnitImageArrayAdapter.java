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
 * Created by lykanthrop on 1507/17/.
 */
public class UnitImageArrayAdapter extends ArrayAdapter<String>
{
	public UnitImageArrayAdapter(Context context, ArrayList<String> imageList)
	{
		super(context, R.layout.unit_image_list_item,imageList);
		this.imageList = imageList;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		if(convertView==null)
		{
			convertView = ((LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.unit_image_list_item,parent,false);
		}

		TextView name = (TextView)convertView.findViewById(R.id.image_name);

		name.setText(imageList.get(position));

		return convertView;
	}

	//Variables
	private ArrayList<String> imageList;
}
