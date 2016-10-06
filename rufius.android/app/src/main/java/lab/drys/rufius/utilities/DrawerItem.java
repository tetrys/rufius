package lab.drys.rufius.utilities;

/**
 * Created by lykanthrop on 6/19/15.
 */
public class DrawerItem
{
	public DrawerItem(String str, int rsc)
	{
		name = str;
		resourceIcon = rsc;
	}

	public void setText(String str)
	{
		name = str;
	}

	public void setResourceIcon(int rsc)
	{
		resourceIcon = rsc;
	}

	public void set(String str, int rsc)
	{
		name = str;
		resourceIcon = rsc;
	}

	public String getText()
	{
		return name;
	}

	public int getResourceIcon()
	{
		return resourceIcon;
	}

	//Variables
	private String name;
	private int resourceIcon;
}
