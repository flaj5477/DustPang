package my.app.dustpang;

import android.content.Context;
import android.support.v7.widget.AppCompatImageView;
import android.view.ViewGroup;

/**
 * Created by ASUS on 2018-05-15.
 */

public class DustImageView extends AppCompatImageView {

    private int dustType;
    private int[] location;

    public DustImageView(Context context, int x, int y, int width, int height, int dustType) {
        super(context);

        setX(x);
        setY(y);
        setLayoutParams(new ViewGroup.LayoutParams(width, height));
        this.dustType = dustType;

        switch (dustType) {
            case 1:
                setImageDrawable(getResources().getDrawable(R.drawable.dust1));
                break;
            case 2:
                setImageDrawable(getResources().getDrawable(R.drawable.dust2));
                break;
            case 3:
                setImageDrawable(getResources().getDrawable(R.drawable.dust3));
                break;
            case 4:
                setImageDrawable(getResources().getDrawable(R.drawable.dust4));
                break;
            case 5:
                setImageDrawable(getResources().getDrawable(R.drawable.dust5));
                break;
        }
    }

    public int getDustType() {
        return dustType;
    }

    public int getAbsoluteX() {
        location = new int[2];
        this.getLocationOnScreen(location);
        return location[0];
    }

    public int getAbsoluteY() {
        location = new int[2];
        this.getLocationOnScreen(location);
        return location[1];
    }

}
