package lab.drys.rufius;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by lykanthrop on 6/8/15.
 */
public abstract class RufiusActivityBasic extends AppCompatActivity
{
	@Override
	public void onCreate(Bundle savedInstance)
	{
		super.onCreate(savedInstance);

		commonPreferences = this.getSharedPreferences(Rufius.statik,MODE_PRIVATE);

		toastView = getLayoutInflater().inflate(R.layout.toast_info,(ViewGroup)findViewById(R.id.toast_layout));
		toastText = (TextView)toastView.findViewById(R.id.toast_text_view);
	}

	@Override
	public void onResume()
	{
		super.onResume();

		if(commonPreferences==null)
		{
			commonPreferences = this.getSharedPreferences(Rufius.statik,MODE_PRIVATE);
		}
	}

	public void showToastInfo(String msg)
	{
		toastText.setText(msg);
		Toast toast = new Toast(this);
		toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
		toast.setDuration(Toast.LENGTH_LONG);
		toast.setView(toastView);
		toast.show();
	}

	public void showToastInfo(int resid)
	{
		toastText.setText(resid);
		Toast toast = new Toast(this);
		toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
		toast.setDuration(Toast.LENGTH_LONG);
		toast.setView(toastView);
		toast.show();
	}

	//Variables
	private View toastView;
	private TextView toastText;
	protected SharedPreferences commonPreferences;
}
