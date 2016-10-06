package lab.drys.rufius.unit;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by lykanthrop on 1507/17/.
 */
public class UnitImagePagerAdapter extends FragmentPagerAdapter
{
	public UnitImagePagerAdapter(FragmentManager fm)
	{
		super(fm);

		fragments = new Fragment[2];
	}

	@Override
	public int getCount()
	{
		return 2;
	}

	@Override
	public CharSequence getPageTitle(int position)
	{
		String out = "Tab";

		switch (position)
		{
			case 0 :
				out = "Snapshots";
				break;
			case 1 :
				out = "Triggers";
				break;
		}

		return out;
	}

	@Override
	public Fragment getItem(int position)
	{
		return fragments[position];
	}

	public void setFragment(int pos, Fragment frg)
	{
		fragments[pos] = frg;
	}

	//Variables
	private Fragment[] fragments;
}
