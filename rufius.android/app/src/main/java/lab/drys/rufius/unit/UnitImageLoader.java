package lab.drys.rufius.unit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

/**
 * Created by lykanthrop on 1507/15/.
 */
public class UnitImageLoader extends AsyncTask<String,Void,Bitmap>
{
	public UnitImageLoader(UnitActivity act)
	{
		unitActivity = act;
	}


	protected Bitmap doInBackground(String... strings)
	{
		Bitmap unitImage = null;
		if(strings.length>0)
		{
			unitImage = BitmapFactory.decodeFile(strings[0]);
		}

		return unitImage;
	}

	protected void onPostExecute(Bitmap unitImage)
	{
		if(unitImage!=null)
		{
			unitActivity.setImage(unitImage);
		}
	}

	//Variables
	UnitActivity unitActivity;
}
