package lab.drys.rufius.unit;

import android.support.v4.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import lab.drys.rufius.R;
import lab.drys.rufius.Rufius;

/**
 * Created by lykanthrop on 1507/16/.
 */
public class UnitImageListFragment extends ListFragment implements AdapterView.OnItemClickListener
{
	@Override
	public void onCreate(Bundle savedInstance)
	{
		super.onCreate(savedInstance);

		type = this.getArguments().getString(Rufius.desc);
		images = new ArrayList<>();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.fragment_unit_image_list, container, false);

		progressBar = (ProgressBar)v.findViewById(R.id.progress_spin);
		progressBar.setVisibility(View.INVISIBLE);
		textEmpty = (TextView)v.findViewById(R.id.text_empty);
		title = (TextView)v.findViewById(R.id.image_list_text);
		title.setText(type);

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstance)
	{
		super.onActivityCreated(savedInstance);

		imageAdapter = new UnitImageArrayAdapter(this.getActivity(), images);
		this.setListAdapter(imageAdapter);

		this.getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		this.getListView().setOnItemClickListener(this);
	}

	@Override
	public void onResume()
	{
		super.onResume();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		if(type.equals("Snapshots"))
		{
			((UnitActivity)this.getActivity()).downloadSnapshot(images.get(position)+".jpg");
		}
		else if(type.equals("Triggers"))
		{
			((UnitActivity)this.getActivity()).downloadTrigger(images.get(position)+".jpg");
		}
	}

	public void populateList(String file)
	{
		progressBar.setVisibility(View.VISIBLE);

		FileInputStream is;

		try
		{
			is = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));

			String line;

			try
			{
				while((line = br.readLine()) !=null)
				{
					images.add(line);
				}

				br.close();
			}
			catch(IOException xcpt0)
			{
				Rufius.logDebug(xcpt0.getMessage());
			}

		}
		catch (FileNotFoundException xcpt)
		{
			Rufius.logDebug(xcpt.getMessage());
		}
		this.updateList();
		progressBar.setVisibility(View.GONE);
	}

	public void updateList()
	{
		if(images.size()>0)
		{
			textEmpty.setVisibility(View.INVISIBLE);
		}
		else
		{
			textEmpty.setVisibility(View.VISIBLE);
		}

		Rufius.logDebug("List Size: "+images.size());
		imageAdapter.notifyDataSetChanged();
	}

	//Variables
	private String type;
	private ArrayList<String> images;
	private UnitImageArrayAdapter imageAdapter;
	private TextView textEmpty;
	private TextView title;
	private ProgressBar progressBar;
}
