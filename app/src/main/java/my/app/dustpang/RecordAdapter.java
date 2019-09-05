package my.app.dustpang;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by ASUS on 2018-05-22.
 */

public class RecordAdapter extends BaseAdapter {
    private ArrayList<Record> records;

    public RecordAdapter(ArrayList<Record> records) {
        this.records = records;
    }

    @Override
    public int getCount() {
        return records.size();
    }

    @Override
    public Object getItem(int i) {
        return records.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int index, View convertView, ViewGroup viewGroup) {
        final Context context = viewGroup.getContext();

        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.record_item, null);
        }

        TextView rank = convertView.findViewById(R.id.record_rank);
        TextView player = convertView.findViewById(R.id.record_player);
        TextView score = convertView.findViewById(R.id.record_score);
        TextView playTime = convertView.findViewById(R.id.record_play_time);

        rank.setText(Html.fromHtml("<font color="+(index == 0 ? "#e7489b" : index == 1 ? "#53b8de" : index == 2 ? "#42ae2c" : "#828282")+">"+(index+1)+"</font>"));
        player.setText(Html.fromHtml("<font color="+(index == 0 ? "#e7489b" : index == 1 ? "#53b8de" : index == 2 ? "#42ae2c" : "#828282")+">"+records.get(index).getPlayer()+"</font>"));
        score.setText(Html.fromHtml("<font color="+(index == 0 ? "#e7489b" : index == 1 ? "#53b8de" : index == 2 ? "#42ae2c" : "#828282")+">"+records.get(index).getScore()+"점</font>"));
        playTime.setText(Html.fromHtml("<font color="+(index == 0 ? "#e7489b" : index == 1 ? "#53b8de" : index == 2 ? "#42ae2c" : "#828282")+">"+records.get(index).getTime()+"초</font>"));

        return convertView;
    }

    public void setAdapter(ArrayList<Record> records) {
        this.records = records;
    }

}
