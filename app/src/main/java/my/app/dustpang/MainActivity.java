package my.app.dustpang;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences pref;
    private RecordAdapter adapter;
    private ListView listView;
    private MediaPlayer mediaPlayer;
    private SoundPool soundPool;
    private Gson gson;

    private ArrayList<Record> records;
    private Setting setting;

    //사운드 관련
    private boolean effectSound;
    private boolean backgroundSound;

    private float effectSoundVolume;
    private float backgroundMusicVolume;

    private int btnClick1;
    private int btnClick2;

    //다이얼로그
    private Dialog recordDialog;
    private Dialog settingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pref = getSharedPreferences("store", MODE_PRIVATE);
        gson = new Gson();

        effectSoundVolume = 0.5f;
        backgroundMusicVolume = 0.5f;

        //미디어 플레이어 셋팅
        mediaPlayer = MediaPlayer.create(this, R.raw.intro_bgm);
        mediaPlayer.setLooping(true);
        mediaPlayer.setVolume(backgroundMusicVolume, backgroundMusicVolume);

        //사운드 풀 초기화
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(2)
                    .build();
        } else {
            soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 1);
        }

        //사운드 셋팅
        btnClick1 = soundPool.load(this, R.raw.btn_click1, 1);
        btnClick2 = soundPool.load(this, R.raw.btn_click2, 1);

        final TextView gameStartBtn = findViewById(R.id.intro_game_start);
        final TextView recordBtn = findViewById(R.id.intro_record);
        final TextView settingBtn = findViewById(R.id.intro_game_setting);
        final TextView exitBtn = findViewById(R.id.intro_game_exit);

        gameStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.click_anim);
                gameStartBtn.startAnimation(animation);

                if(effectSound)
                    soundPool.play(btnClick1, effectSoundVolume, effectSoundVolume,  1,  0,  1);

                Intent intent = new Intent(MainActivity.this, GameActivity.class);
                startActivity(intent);
            }
        });

        //기록 보기 버튼
        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(recordDialog != null && recordDialog.isShowing()) return;

                Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.click_anim);
                recordBtn.startAnimation(animation);

                if(effectSound)
                    soundPool.play(btnClick1, effectSoundVolume, effectSoundVolume,  1,  0,  1);

                //다이얼로그 생성 시작
                LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                final View dialogLayout = inflater.inflate(R.layout.record_dialog, null);
                recordDialog = new Dialog(MainActivity.this);

                recordDialog.requestWindowFeature(Window.FEATURE_NO_TITLE); //다이얼로그 제목 삭제
                recordDialog.getWindow().getAttributes().windowAnimations = R.style.CustomDialogAnimation;
                recordDialog.setContentView(dialogLayout);
                recordDialog.show();

                Display display = getWindowManager().getDefaultDisplay();
                Point point = new Point();
                display.getSize(point);

                Window window = recordDialog.getWindow();

                int x = (int)(point.x * 0.95f);
                int y = (int)(point.y * 0.8f);

                window.setLayout(x, y);
                //다이얼로그 생성 끝

                //shared에서 기록 가져오기
                String json = pref.getString("records", null);
                if(json != null) {
                    records = gson.fromJson(json, new TypeToken<ArrayList<Record>>(){}.getType());
                } else {
                    records = new ArrayList<>();
                }

                adapter = new RecordAdapter(records);
                listView = recordDialog.findViewById(R.id.record_listview);
                listView.setAdapter(adapter);

                recordDialog.findViewById(R.id.reset_cheat_key).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //데이터 초기화 치트키
                        adapter.setAdapter(new ArrayList<Record>());
                        adapter.notifyDataSetChanged();

                        SharedPreferences.Editor editor = pref.edit();
                        String json = gson.toJson(new ArrayList<Record>());
                        editor.putString("records", json);
                        editor.commit();
                    }
                });
            }
        });

        //환경 설정 버튼
        settingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(settingDialog != null && settingDialog.isShowing()) return;

                Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.click_anim);
                settingBtn.startAnimation(animation);

                if(effectSound)
                    soundPool.play(btnClick1, effectSoundVolume, effectSoundVolume,  1,  0,  1);

                //다이얼로그 생성 시작
                LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                final View dialogLayout = inflater.inflate(R.layout.game_setting_dialog, null);
                settingDialog = new Dialog(MainActivity.this);

                settingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE); //다이얼로그 제목 삭제
                settingDialog.getWindow().getAttributes().windowAnimations = R.style.CustomDialogAnimation;
                settingDialog.setContentView(dialogLayout);
                settingDialog.show();
                //다이얼로그 생성 끝

                ImageView applyBtn = settingDialog.findViewById(R.id.setting_apply_btn);
                final EditText playerName = settingDialog.findViewById(R.id.setting_player_name);
                final Switch effectSoundSwitch = settingDialog.findViewById(R.id.setting_effect_sound);
                final Switch backgroundSoundSwitch = settingDialog.findViewById(R.id.setting_background_sound);

                playerName.setText(setting.getPlayerName());
                effectSoundSwitch.setChecked(effectSound);
                backgroundSoundSwitch.setChecked(backgroundSound);

                effectSoundSwitch.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        effectSound = !effectSound;

                        if(effectSound)
                            soundPool.play(btnClick2, effectSoundVolume, effectSoundVolume,  1,  0,  1);
                    }
                });

                backgroundSoundSwitch.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(backgroundSound) {
                            mediaPlayer.pause();
                        } else {
                            mediaPlayer.start();
                        }
                        backgroundSound = !backgroundSound;

                        if(effectSound)
                            soundPool.play(btnClick2, effectSoundVolume, effectSoundVolume,  1,  0,  1);
                    }
                });

                applyBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(effectSound)
                            soundPool.play(btnClick2, effectSoundVolume, effectSoundVolume,  1,  0,  1);

                        setting.setPlayerName(playerName.getText().toString());
                        setting.setEffectSound(effectSound);
                        setting.setBackgroundSound(backgroundSound);

                        //셋팅 정보 shared에 저장
                        String json = gson.toJson(setting);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("setting", json);
                        editor.commit();

                        settingDialog.dismiss();
                    }
                });

            }
        });

        //게임 종료
        exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.click_anim);
                exitBtn.startAnimation(animation);

                if(effectSound)
                    soundPool.play(btnClick1, effectSoundVolume, effectSoundVolume,  1,  0,  1);

                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        //shared에서 셋팅 정보 가져오기
        String json = pref.getString("setting", null);
        if(json != null) {
            setting = gson.fromJson(json, Setting.class);
        } else {
            setting = new Setting("User", true, true);
        }

        //사운드 플래그 셋팅
        effectSound = setting.isEffectSound();
        backgroundSound = setting.isBackgroundSound();

        if(backgroundSound) {
            //배경음악 재생
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(500);
                        if(mediaPlayer != null)
                            mediaPlayer.start();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(backgroundSound)
            mediaPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
