package my.app.dustpang;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

/**
 * Created by ASUS on 2018-05-15.
 */

public class GameActivity extends AppCompatActivity {

    //게임 상태
    private static final int BEFORE_THE_GAME_START = 0;
    private static final int GAME_PAUSED = 1;
    private static final int GAME_TERMINATED = 2;
    private static final int GAME_PLAYING = 3;

    private SharedPreferences pref;
    private boolean pressBackBtn;

    //볼륨
    private float effectSoundVolume;
    private float backgroundMusicVolume;
    private boolean effectSound;
    private boolean backgroundSound;

    //게임 시간 관련
    private int timer; //순수 게임 플레이 타임
    private int stackedNumber; //게임을 종료하기위해 쌓이는 값
    private int targetNumber; //stackedNumber가 도달해야 하는 값 (stackedNumber와 targetNumber의 값이 같아지면 게임 종료)

    private int highScore;
    private int userScore;
    private int plateSize;
    private int division9; // plate를 9로 나눈 값
    private int gameStatus;
    private boolean touchStatus; //true면 터치가 가능한 상태임
    private boolean timerThreadContoller;
    private DustImageView[][] dustArray;
    private DustPosition[][] dustPositions;
    private ArrayList<Record> records;

    //먼지 스왑시 필요한 두 먼지의 좌표
    private int e1X;
    private int e1Y;
    private int e2X;
    private int e2Y;

    /**
     * 용어 설명
     * plate : 게임을 진행하는 9x9의 판을 지칭함
     */

    //위젯 참조 변수
    private TextView plate;
    private TextView userScoreView;
    private TextView highScoreView;
    private TextView hideDustBar;
    private TextView gameStartMessage;
    private TextView screenCover;
    private ProgressBar timerBar;
    private ImageView pauseAndContinueBtn;
    private ImageView effectSoundOnOffBtn;
    private ImageView backgroundSoundOnOffBtn;
    private ImageView gobackBtn;
    private ImageView timerImg;
    private ConstraintLayout layout;

    //view의 변화감지 리스너
    private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener;

    //제스처 감지
    private GestureDetector detector;

    //핸들러
    private Handler handler;

    //먼지 스왑 완료 감지 콜백
    private SwapCompletedListener swapCompletedListener;

    //먼지 채우기 감지 콜백
    private FillCompletedListener fillCompletedListener;

    //사운드 풀
    private SoundPool soundPool;

    //사운드 id
    private int swapSound;
    private int dustRemoveSound;
    private int gameStartSound;
    private int gameEndSound1;
    private int gameEndSound2;
    private int btnClick1;
    private int btnClick2;

    //gameStartSound play 반환값
    private int gameStartSoundReturnNumber;

    //미디어 플레이어
    private MediaPlayer mediaPlayer;

    //시작 애니메이션
    private Animation startAnim;

    //게임 세팅
    private Setting setting;
    private Gson gson;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.game);
        gson = new Gson();

        //저장소 초기화
        pref = getSharedPreferences("store", MODE_PRIVATE);
        String recordJson = pref.getString("records", null);
        if(recordJson == null) {
            records = new ArrayList<>();
        } else {
            records = gson.fromJson(recordJson, new TypeToken<ArrayList<Record>>(){}.getType());
        }

        //위젯 초기화
        plate = findViewById(R.id.game_plate);
        layout = findViewById(R.id.constraint);
        userScoreView = findViewById(R.id.game_score);
        highScoreView = findViewById(R.id.game_high_score);
        hideDustBar = findViewById(R.id.game_hide_dust_bar);
        timerBar = findViewById(R.id.game_timer_bar);
        pauseAndContinueBtn = findViewById(R.id.game_pause_and_continue);
        gameStartMessage = findViewById(R.id.game_start_message);
        screenCover = findViewById(R.id.game_screen_cover);
        effectSoundOnOffBtn = findViewById(R.id.game_effect_sound_on_off);
        backgroundSoundOnOffBtn = findViewById(R.id.game_background_sound_on_off);
        gobackBtn = findViewById(R.id.game_goback);
        timerImg = findViewById(R.id.game_timer_img);

        //변수 초기화
        effectSoundVolume = 0.5f;
        backgroundMusicVolume = 0.1f; // 효과음 : 배경음 -> 5 : 1 비율이 적당함
        targetNumber = timerBar.getMax();
        highScore = records.size() >= 1 ? records.get(0).getScore() : 0; //기록 중 가장 높은 점수로 highScore 셋팅

        handler = new Handler();
        dustArray = new DustImageView[9][9];
        dustPositions = new DustPosition[9][9];

        //사운드 풀 초기화
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(7)
                    .build();
        } else {
            soundPool = new SoundPool(7, AudioManager.STREAM_MUSIC, 1);
        }

        //사운드 셋팅
        swapSound = soundPool.load(this, R.raw.swap_sound, 1);
        dustRemoveSound = soundPool.load(this, R.raw.pop_sound, 1);
        gameStartSound = soundPool.load(this, R.raw.game_start_sound, 1);
        gameEndSound1 = soundPool.load(this, R.raw.game_end1, 1);
        gameEndSound2 = soundPool.load(this, R.raw.game_end2, 1);
        btnClick1 = soundPool.load(this, R.raw.btn_click1, 1);
        btnClick2 = soundPool.load(this, R.raw.btn_click2, 1);


        //셋팅 값 적용
        String json = pref.getString("setting", null);
        if(json != null) {
            setting = gson.fromJson(json, Setting.class);
        } else {
            setting = new Setting("User", true, true);
        }

        effectSound = setting.isEffectSound();
        backgroundSound = setting.isBackgroundSound();
        effectSoundOnOffBtn.setImageDrawable(getDrawable(effectSound ? R.drawable.effect_sound_btn : R.drawable.effect_sound_btn_x));
        backgroundSoundOnOffBtn.setImageDrawable(getDrawable(backgroundSound ? R.drawable.background_sound_btn : R.drawable.background_sound_btn_x));

        //plate가 그려진 후 넓이와 높이를 구하기 위한 리스너
        mGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                //plate에 아이템을 9x9로 배치하기 위해 정확히 9로 나눠지는 수치를 계산
                plateSize = (plate.getWidth() / 9) * 9;

                //plate 넓이, 높이 설정
                ViewGroup.LayoutParams plateLayoutParams = plate.getLayoutParams();
                plateLayoutParams.width = plateSize;
                plateLayoutParams.height = plateSize;
                plate.setLayoutParams(plateLayoutParams);

                //hideBar넓이, 높이 설정
                ViewGroup.LayoutParams hideBarLayoutParams = hideDustBar.getLayoutParams();
                hideBarLayoutParams.width = plateSize;
                hideBarLayoutParams.height = plateSize / 9;
                hideDustBar.setLayoutParams(hideBarLayoutParams);

                //판의 크기를 설정한 후
                plate.post(new Runnable() {
                    @Override
                    public void run() {
                        do {
                            //겹치는게 없을 때까지 판을 셋팅
                            setDustArray();
                        } while (checkDustArray());
                        basicSetting();
                        showStartDialog(); //시작 다이어로그 띄우기
                    }
                });

                //리스너 지우기
                removeOnGlobalLayoutListener(plate.getViewTreeObserver(), mGlobalLayoutListener);
            }
        };

        //plate 넓이 구하기 위한 리스너 등록
        plate.getViewTreeObserver().addOnGlobalLayoutListener(mGlobalLayoutListener);

        //터치 및 스와이프 인식 리스너
        detector = new GestureDetector(this, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent motionEvent) {
                return false;
            }
            @Override
            public void onShowPress(MotionEvent motionEvent) {
            }
            @Override
            public boolean onSingleTapUp(MotionEvent motionEvent) {
                return false;
            }
            @Override
            public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
                return false;
            }
            @Override
            public void onLongPress(MotionEvent motionEvent) {
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float v1, float v2) {
                //먼지를 이동시키기 위해 스와이프를 한 경우 onFling이벤트가 발생
                //터치가 불가능한 상태일 경우 return false
                if(!touchStatus) {
                    Log.i("touchStatus false", "");
                    return false;
                } else {
                    Log.i("touchStatus true", "");
                    touchStatus = false;
                    int[] plateLocation = new int[2];
                    plate.getLocationOnScreen(plateLocation); //plate의 절대 좌표 가져오기

                    int plateX = plateLocation[0];
                    int plateY = plateLocation[1];

                    if(e1 == null || e2 == null) return false; //nullpointer exception 방지

                    //e1 및 e2 이벤트 위치가 plate 바깥쪽일 경우 return false
                    if(e1.getX() >= plateX
                    && e1.getX() <= plateX + plateSize
                    && e1.getY() >= plateY
                    && e1.getY() <= plateY + plateSize
                    && e2.getX() >= plateX
                    && e2.getX() <= plateX + plateSize
                    && e2.getY() >= plateY
                    && e2.getY() <= plateY + plateSize) {

                        //처음 터치한 먼지의 좌표
                        e1X = ((int)e1.getX() - plateX) / division9;
                        e1Y = ((int)e1.getY() - plateY) / division9;

                        //터치를 뗀 위치의 좌표
                        e2X = ((int)e2.getX() - plateX) / division9;
                        e2Y = ((int)e2.getY() - plateY) / division9;

                        //스왑할 먼지의 좌표 조정
                        //두칸 이상의 범위를 드래그한 경우 좌표값을 한칸으로 조정해줌
                        e2X = Math.abs(e1X - e2X) >= 2 ?
                                e1X > e2X ?
                                    e1X - 1 :
                                    e1X + 1
                                : e2X;

                        e2Y = Math.abs(e1Y - e2Y) >= 2 ?
                                e1Y > e2Y ?
                                        e1Y - 1 :
                                        e1Y + 1
                                : e2Y;


                        if((e1X != e2X && e1Y != e2Y) || (e1X == e2X && e1Y == e2Y)) {
                            Log.i("잘못된 드래그 ->", e1Y+" -> "+e2Y+" || "+e1X+" -> "+e2X);
                            //잘못된 드래그 방지
                            //1. 대각선 드래그
                            //2. 제자리 드래그
                            touchStatus = true;
                            return false;
                        }

                        //스왑 효과음 재생
                        if(effectSound)
                            soundPool.play(swapSound, effectSoundVolume, effectSoundVolume,  1,  0,  1);

                        //스왑 완료시 호출할 콜백 등록
                        swapCompletedListener = new SwapCompletedListener();
                        SwapCompletedListener.SwapCallback swapCallback = new SwapCompletedListener.SwapCallback() {
                            @Override
                            public void onSwapComplete(boolean restore) {
                                if(restore) {
                                    //되돌리기의 경우
                                    touchStatus = true;
                                    return;
                                }

                                if(!checkDustArray()) {
                                    //터트릴 먼지가 하나도 없을 경우 다시 스왑함으로써 원상태로 되돌림
                                    swapDust(e1X, e2X, e1Y, e2Y, true);
                                } else {
                                    fillCompletedListener = new FillCompletedListener();
                                    FillCompletedListener.FillCallback fillCallback = new FillCompletedListener.FillCallback() {
                                        @Override
                                        public void onFillComplete() {
                                            if(gameStatus != GAME_PLAYING) return;

                                            if(!checkDustArray()) {
                                                touchStatus = true;
                                            } else {
                                                fillBlank();
                                            }
                                        }
                                    };
                                    fillCompletedListener.setFillCallback(fillCallback); //콜백 등록
                                    fillBlank();
                                }
                            }
                        };
                        swapCompletedListener.setSwapCallback(swapCallback);

                        //두 먼지 스왑
                        swapDust(e1X, e2X, e1Y, e2Y, false);
                        return true;
                    } else {
                        touchStatus = true;
                        return false;
                    }
                }
            }
        });

        //게임 중지 및 계속하기 버튼
        pauseAndContinueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(gameStatus == GAME_PLAYING && touchStatus) {
                    pauseGame();
                } else if(gameStatus == GAME_PAUSED && !touchStatus) {
                    continueGame();
                } else {
                    return;
                }

                if(effectSound)
                    soundPool.play(btnClick2, effectSoundVolume, effectSoundVolume,  1,  0,  1);
            }
        });

        //게임 중지 상태일 때 스크린 커버를 터치하면 게임을 계속하도록 함
        screenCover.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(gameStatus == GAME_PAUSED && motionEvent.getY() >= 200) {
                    //화면 상단의 터치를 막아놓은 이유는
                    //화면 상단의 버튼을 클릭하다 실수로 커버를 클릭할 수도 있기 때문
                    continueGame();
                    return true;
                }
                return false;
            }
        });

        //효과음 on/off 버튼
        effectSoundOnOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //게임 중지상태일 때만 발동
                if(gameStatus == GAME_PAUSED) {
                    effectSoundOnOffBtn.setImageDrawable(getDrawable(effectSound ? R.drawable.effect_sound_btn_x : R.drawable.effect_sound_btn));
                    effectSound = !effectSound;
                    setting.setEffectSound(effectSound);

                    if(effectSound)
                        soundPool.play(btnClick2, effectSoundVolume, effectSoundVolume,  1,  0,  1);

                    Toast.makeText(GameActivity.this, "효과음 "+(effectSound ? "On" : "Off"), Toast.LENGTH_SHORT).show();
                }
            }
        });

        //배경음 on/off 버튼
        backgroundSoundOnOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //게임 중지상태일 때만 발동
                if(gameStatus == GAME_PAUSED) {
                    backgroundSoundOnOffBtn.setImageDrawable(getDrawable(backgroundSound ? R.drawable.background_sound_btn_x : R.drawable.background_sound_btn));


                    if(backgroundSound) {
                        mediaPlayer.pause();
                    } else {
                        mediaPlayer.start();
                    }
                    backgroundSound = !backgroundSound;
                    setting.setBackgroundSound(backgroundSound);

                    if(effectSound)
                        soundPool.play(btnClick2, effectSoundVolume, effectSoundVolume,  1,  0,  1);

                    Toast.makeText(GameActivity.this, "배경음 "+(backgroundSound ? "On" : "Off"), Toast.LENGTH_SHORT).show();
                }
            }
        });

        //게임 다시하기, 그만하기를 선택할 수 있는 다이어로그를 띄움
        gobackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(gameStatus == GAME_PAUSED) {
                    if(effectSound)
                        soundPool.play(btnClick2, effectSoundVolume, effectSoundVolume,  1,  0,  1);

                    LayoutInflater inflater = LayoutInflater.from(GameActivity.this);
                    final View dialogLayout = inflater.inflate(R.layout.go_back_dialog, null);
                    final Dialog dialog = new Dialog(GameActivity.this);

                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); //다이얼로그 제목 삭제
                    dialog.setContentView(dialogLayout);
                    dialog.setCancelable(false);
                    dialog.show();

                    ImageView replayBtn = dialogLayout.findViewById(R.id.go_back_replay);
                    ImageView stopGameBtn = dialogLayout.findViewById(R.id.go_back_stop_game);
                    ImageView cancel = dialogLayout.findViewById(R.id.go_back_cancel);

                    //게임 다시하기
                    replayBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(effectSound)
                                soundPool.play(btnClick1, effectSoundVolume, effectSoundVolume,  1,  0,  1);

                            dialog.dismiss();

                            //현재 bgm 종료
                            mediaPlayer.stop();
                            mediaPlayer.release();

                            //게임 중지화면 스크린 커버 걷어내기
                            pauseAndContinueBtn.setImageDrawable(getDrawable(R.drawable.pause_btn));
                            screenCover.setVisibility(View.INVISIBLE);
                            effectSoundOnOffBtn.setVisibility(View.INVISIBLE);
                            backgroundSoundOnOffBtn.setVisibility(View.INVISIBLE);
                            gobackBtn.setVisibility(View.INVISIBLE);

                            gameReplay();
                        }
                    });

                    //게임 종료
                    stopGameBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(effectSound)
                                soundPool.play(btnClick1, effectSoundVolume, effectSoundVolume,  1,  0,  1);

                            dialog.dismiss();
                            finish();
                        }
                    });

                    //취소
                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(effectSound)
                                soundPool.play(btnClick1, effectSoundVolume, effectSoundVolume,  1,  0,  1);

                            dialog.dismiss();
                        }
                    });
                }
            }
        });

        //시연을 위해 인위적으로 타이머를 빼는 코드
        timerImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stackedNumber = targetNumber - 30;
            }
        });

    }

    /**
     * 터치 이벤트 등록
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return detector.onTouchEvent(event);
    }

    /**
     * 게임 방법을 설명해줄 다이어로그를 띄우기위한 메소드
     */
    private void showStartDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        final View dialogLayout = inflater.inflate(R.layout.game_start_dialog, null);
        final Dialog dialog = new Dialog(GameActivity.this);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); //다이얼로그 제목 삭제
        dialog.setContentView(dialogLayout);
        dialog.setCancelable(false);
        dialog.show();

        ImageView gameStartBtn = dialogLayout.findViewById(R.id.start_dialog_game_start);
        gameStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(effectSound)
                    soundPool.play(btnClick1, effectSoundVolume, effectSoundVolume,  1,  0,  1);

                dialog.dismiss();
                startGame();
            }
        });

        //애니메이션
        final ImageView finger = dialogLayout.findViewById(R.id.start_dialog_finger);
        final ImageView dust1 = dialogLayout.findViewById(R.id.start_dialog_dust1);
        final ImageView dust2 = dialogLayout.findViewById(R.id.start_dialog_dust2);
        final ImageView dust3 = dialogLayout.findViewById(R.id.start_dialog_dust3);
        final ImageView dust4 = dialogLayout.findViewById(R.id.start_dialog_dust4);

        //시작 애니메이션 반복 쓰레드
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(gameStatus == BEFORE_THE_GAME_START) {
                    try {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                startAnim = AnimationUtils.loadAnimation(GameActivity.this, R.anim.finger_anim);
                                startAnim.setFillAfter(true);
                                finger.startAnimation(startAnim);

                                TranslateAnimation tranAnimation2 = new TranslateAnimation(
                                        Animation.RELATIVE_TO_SELF , 0
                                        ,Animation.RELATIVE_TO_SELF , 0
                                        ,Animation.RELATIVE_TO_SELF , 0
                                        ,Animation.RELATIVE_TO_SELF , 1);
                                tranAnimation2.setDuration(1000);
                                tranAnimation2.setStartOffset(1000);
                                tranAnimation2.setFillAfter(true);
                                dust4.startAnimation(tranAnimation2);

                                startAnim = AnimationUtils.loadAnimation(GameActivity.this, R.anim.start_dialog_dust_anim);
                                startAnim.setFillAfter(true);
                                dust3.startAnimation(startAnim);

                                startAnim = AnimationUtils.loadAnimation(GameActivity.this, R.anim.remove_dust);
                                startAnim.setFillAfter(true);
                                startAnim.setStartOffset(2000);
                                startAnim.setDuration(500);
                                dust1.startAnimation(startAnim);
                                dust2.startAnimation(startAnim);

                            }
                        });
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * plate에 먼지를 채워넣음
     */
    private void setDustArray() {
        division9 = plateSize/9;

        for(int q = 0 ; q < dustArray.length ; q++) {
            for(int w = 0 ; w < dustArray[q].length ; w++) {
                if(dustArray[q][w] == null) {
                    //먼지 포지션만을 저장하는 배열
                    dustPositions[q][w] = new DustPosition((int)plate.getX() + (division9 * w), (int)plate.getY() + (division9 * q));

                    //실제 먼지가 저장되는 배열
                    dustArray[q][w] = new DustImageView(GameActivity.this
                            , (int)plate.getX() + (division9 * w)
                            , (int)plate.getY() + (division9 * q)
                            , division9
                            , division9
                            , (int)(Math.random() * 5) + 1);

                    layout.addView(dustArray[q][w]);
                    Log.i("##### x", ""+dustArray[q][w].getX());
                    Log.i("##### ax", ""+dustArray[q][w].getAbsoluteX());
                }
            }
        }
    }

    /**
     * 먼지 삭제 후 공백 채우는 메소드
     */

    private void fillBlank() {
        int totalNullCount = 0;

/*        for(int q = 0 ; q < dustArray.length ; q++) {
            String row = "";
            for(int w = 0 ; w < dustArray[q].length ; w++) {
                row += dustArray[q][w] == null ? "x" : "o";
                row += " ";
            }
            Log.i("dustArray "+q, row);
        }*/

        for(int q = 0 ; q < dustArray.length ; q++) {
            final ArrayList<DustImageView> newDustList = new ArrayList<>();
            final ArrayList<DustImageView> dustList = new ArrayList<>();
            for(int w = 0 ; w < dustArray.length ; w++) {
                dustList.add(dustArray[w][q]);
            }
            int nullCount = 0;

            for(int w = dustArray.length - 1 ; w >= 0 ; w--) {
                if(dustArray[w][q] == null) {
                    nullCount++;
                    totalNullCount++;
                    DustImageView dustImageView = new DustImageView(GameActivity.this
                            , (int)hideDustBar.getX() + (division9 * q)
                            , (int)hideDustBar.getY()
                            , division9
                            , division9
                            , (int)(Math.random() * 5) + 1);
                    layout.addView(dustImageView);
                    hideDustBar.bringToFront();
                    newDustList.add(dustImageView);

                    if(w == 0) {
                        //첫번 째 칸이 0일 경우 plate를 채움
                        fillPlate(newDustList, q);
                    }

                } else {
                    if(nullCount >= 1) {
                        final int x = q;
                        final int y = w;
                        final int newY = w + nullCount;

                        dustArray[newY][x] = dustList.get(y);
                        TranslateAnimation tranAnimation = new TranslateAnimation(
                                Animation.RELATIVE_TO_SELF , 0
                                ,Animation.RELATIVE_TO_SELF , 0
                                ,Animation.RELATIVE_TO_SELF , 0
                                ,Animation.RELATIVE_TO_SELF , nullCount);
                        tranAnimation.setInterpolator(new AccelerateInterpolator());
                        tranAnimation.setDuration(200 + (nullCount * 100));
                        tranAnimation.setFillEnabled(true);
                        tranAnimation.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationEnd(Animation animation) {
                                //실행 순서가 보장 되지 않음
                                dustArray[newY][x].setX(dustPositions[newY][x].getX());
                                dustArray[newY][x].setY(dustPositions[newY][x].getY());

                                if(y == 0) {
                                    //먼지들을 아래로 옮기고 난 후 공백을 채워넣음
                                    fillPlate(newDustList, x);
                                }
                            }

                            @Override
                            public void onAnimationStart(Animation animation) {

                            }
                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });
                        dustArray[y][x].startAnimation(tranAnimation);

                    }
                }

            }
        }

        fillCompletedListener.setTotalNullCount(totalNullCount);
    }

    /**
     * newDustList에 있는 먼지를 plate에 채워 넣음
     * @param newDustList
     * @param x
     */
    private void fillPlate(final ArrayList<DustImageView> newDustList, final int x) {
        for(int q = 0 ; q < newDustList.size() ; q++) {
            final int index = q;

            //생성되있는 먼지들을 아래로 이동시키며 채워넣음
            dustArray[newDustList.size() - index - 1][x] = newDustList.get(index);

            TranslateAnimation tranAnimation = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF , 0
                    ,Animation.RELATIVE_TO_SELF , 0
                    ,Animation.RELATIVE_TO_SELF , 0
                    ,Animation.RELATIVE_TO_SELF , newDustList.size() - q);
            tranAnimation.setDuration(200 + ((newDustList.size() - q) * 100));
            tranAnimation.setFillEnabled(true);
            tranAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    dustArray[newDustList.size() - index - 1][x].setX(dustPositions[newDustList.size() - index - 1][x].getX());
                    dustArray[newDustList.size() - index - 1][x].setY(dustPositions[newDustList.size() - index - 1][x].getY());

                    fillCompletedListener.fillComplete();
                }

                @Override
                public void onAnimationStart(Animation animation) {

                }
                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            newDustList.get(q).startAnimation(tranAnimation);
        }
    }

    /**
     * 현재 plate에 동일한 모양의 먼지가 일렬로 3개 이상 나열된 곳이 있는지 체크 후
     * 있다면, 나열된 먼지 삭제 후 점수 획득 및 true 리턴
     * 없다면, false 리턴
     * @return
     */
    private boolean checkDustArray() {
        final ArrayList<String> removeList = new ArrayList<>();
        boolean flag = false; //리턴할 변수

        for(int q = 0 ; q < dustArray.length ; q++) {
            for(int w = 0 ; w < dustArray[q].length ; w++) {
                int verticalMin = q - 2 < 0 ? 0 : q - 2;
                int verticalMax = q + 2 >= dustArray.length ? dustArray.length - 1 : q + 2;
                int horizontalMin = w - 2 < 0 ? 0 : w - 2;
                int horizontalMax = w + 2 >= dustArray.length ? dustArray.length - 1 : w + 2;

                /*Log.i("현재 좌표", q+", "+w);
                Log.i("verticalMin", ""+verticalMin);
                Log.i("verticalMax", ""+verticalMax);
                Log.i("horizontalMin", ""+horizontalMin);
                Log.i("horizontalMax", ""+horizontalMax);*/

                int count = 0;
                for(int e = verticalMin + 1 ; e <= verticalMax ; e++) {
                    //세로 탐색
                    if(dustArray[e - 1][w].getDustType() == dustArray[e][w].getDustType()) {
                        count++;
                    } else {
                        count = 0;
                    }

                    if(count >= 2) {
                        /**
                         * 카운팅이 2 이상 된 경우 연속된 3개의 먼지가 있다는 의미 이므로
                         * 현재 검사한 먼지의 좌표(q, w)를 기준으로 인접한 같은 모양의 먼지를 모두 삭제함
                         * 범위는 기준점(q, w)에서 부터 최대 2칸
                         */
                        flag = true;

                        removeList.add(q+","+w);
                        for(int r = q + 1 ; r <= verticalMax ; r++) {
                            if(dustArray[q][w].getDustType() == dustArray[r][w].getDustType()) {
                                if(layout.getViewWidget(dustArray[r][w]) != null) {
                                    //리무브 애니메이션
                                    Animation anim = AnimationUtils.loadAnimation(this, R.anim.remove_dust);
                                    dustArray[r][w].startAnimation(anim);
                                }

                                layout.removeView(dustArray[r][w]);
                                removeList.add(r+","+w);

                            } else {
                                break;
                            }
                        }

                        for(int r = q - 1 ; r >= verticalMin ; r--) {
                            if(dustArray[q][w].getDustType() == dustArray[r][w].getDustType()) {
                                if(layout.getViewWidget(dustArray[r][w]) != null) {
                                    //리무브 애니메이션
                                    Animation anim = AnimationUtils.loadAnimation(this, R.anim.remove_dust);
                                    dustArray[r][w].startAnimation(anim);
                                }

                                layout.removeView(dustArray[r][w]);
                                removeList.add(r+","+w);
                            } else {
                                break;
                            }
                        }
                        break;
                    }
                }

                count = 0;
                for(int e = horizontalMin + 1 ; e <= horizontalMax ; e++) {
                    //가로 탐색
                    if(dustArray[q][e - 1].getDustType() == dustArray[q][e].getDustType()) {
                        count++;
                    } else {
                        count = 0;
                    }

                    if(count >= 2) {
                        flag = true;

                        removeList.add(q+","+w);
                        for(int r = w + 1 ; r <= horizontalMax ; r++) {
                            if(dustArray[q][w].getDustType() == dustArray[q][r].getDustType()) {
                                if(layout.getViewWidget(dustArray[q][r]) != null) {
                                    //리무브 애니메이션
                                    Animation anim = AnimationUtils.loadAnimation(this, R.anim.remove_dust);
                                    dustArray[q][r].startAnimation(anim);
                                }

                                layout.removeView(dustArray[q][r]);
                                removeList.add(q+","+r);
                            } else {
                                break;
                            }
                        }

                        for(int r = w - 1 ; r >= horizontalMin ; r--) {
                            if(dustArray[q][w].getDustType() == dustArray[q][r].getDustType()) {
                                if(layout.getViewWidget(dustArray[q][r]) != null) {
                                    //리무브 애니메이션
                                    Animation anim = AnimationUtils.loadAnimation(this, R.anim.remove_dust);
                                    dustArray[q][r].startAnimation(anim);
                                }

                                layout.removeView(dustArray[q][r]);
                                removeList.add(q+","+r);
                            } else {
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        }

        for(int q = 0 ; q < removeList.size() ; q++) {
            //실제 dustArray에서 먼지 삭제
            int i = Integer.parseInt(removeList.get(q).split(",")[0]);
            int j = Integer.parseInt(removeList.get(q).split(",")[1]);
            dustArray[i][j] = null;
        }

        if(flag && gameStatus == GAME_PLAYING) {
            //시간초 추가
            stackedNumber -= (float)removeList.size() / 2;

            //점수 갱신
            userScore += removeList.size() * 10;
            highScore = highScore < userScore ? userScore : highScore;

            handler.post(new Runnable() {
                @Override
                public void run() {
                    userScoreView.setText(""+String.format("%,d", userScore));
                    highScoreView.setText(""+String.format("%,d", highScore));
                }
            });

            //먼지 터지는 효과음
            if(effectSound)
                soundPool.play(dustRemoveSound, effectSoundVolume, effectSoundVolume,  1,  0,  1);
        }

        return flag;
    }

    //먼지 스왑 애니메이션
    private void swapDust(final int x1, final int x2, final int y1, final int y2, final boolean restore) {
        //restore가 true면 되돌리기 작업임
        //먼지1 : 사용자가 처음 터치한 먼지
        //먼지2 : 사용자가 교환하려고 드래그한 자리에 있는 먼지

        //먼지1 좌표 얻어오기
        final int dust1X = (int)dustPositions[y1][x1].getX();
        final int dust1Y = (int)dustPositions[y1][x1].getY();

        //먼지2 좌표 얻어오기
        final int dust2X = (int)dustPositions[y2][x2].getX();
        final int dust2Y = (int)dustPositions[y2][x2].getY();

        //먼지1을 먼지2 쪽으로 이동
        TranslateAnimation translateAnimation1 = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF , 0
                ,Animation.RELATIVE_TO_SELF , x2 - x1
                ,Animation.RELATIVE_TO_SELF , 0
                ,Animation.RELATIVE_TO_SELF , y2 - y1);
        translateAnimation1.setDuration(300);
        translateAnimation1.setFillEnabled(true);
        translateAnimation1.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                dustArray[y2][x2].setX(dust2X);
                dustArray[y2][x2].setY(dust2Y);

                //애니메이션이 끝난것을 리스너에게 알려주기위한 메소드
                swapCompletedListener.swapAnimationEnd(restore);
            }

            @Override
            public void onAnimationStart(Animation animation) {

            }
            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        dustArray[y1][x1].startAnimation(translateAnimation1);

        //먼지2를 먼지1 쪽으로 이동
        TranslateAnimation translateAnimation2 = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF , 0
                ,Animation.RELATIVE_TO_SELF , x1 - x2
                ,Animation.RELATIVE_TO_SELF , 0
                ,Animation.RELATIVE_TO_SELF , y1 - y2);
        translateAnimation2.setDuration(300);
        translateAnimation2.setFillEnabled(true);
        translateAnimation2.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                dustArray[y1][x1].setX(dust1X);
                dustArray[y1][x1].setY(dust1Y);

                //애니메이션이 끝난것을 리스너에게 알려주기위한 메소드
                swapCompletedListener.swapAnimationEnd(restore);
            }

            @Override
            public void onAnimationStart(Animation animation) {

            }
            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        dustArray[y2][x2].startAnimation(translateAnimation2);

        //먼지 스왑
        DustImageView tmpDust = dustArray[y2][x2];
        dustArray[y2][x2] = dustArray[y1][x1];
        dustArray[y1][x1] = tmpDust;
    }

    //게임 중지
    private void pauseGame() {
        if(gameStatus == GAME_TERMINATED) return;

        gameStatus = GAME_PAUSED;
        touchStatus = false;
        timerThreadContoller = false;

        pauseAndContinueBtn.setImageDrawable(getDrawable(R.drawable.continue_btn));
        screenCover.setVisibility(View.VISIBLE);
        effectSoundOnOffBtn.setVisibility(View.VISIBLE);
        backgroundSoundOnOffBtn.setVisibility(View.VISIBLE);
        gobackBtn.setVisibility(View.VISIBLE);

        screenCover.bringToFront();
        pauseAndContinueBtn.bringToFront();
        effectSoundOnOffBtn.bringToFront();
        backgroundSoundOnOffBtn.bringToFront();
        gobackBtn.bringToFront();

        //애니메이션
        TranslateAnimation translateAnimation1 = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF , 1
                ,Animation.RELATIVE_TO_SELF , 0
                ,Animation.RELATIVE_TO_SELF , 0
                ,Animation.RELATIVE_TO_SELF , 0);
        translateAnimation1.setDuration(250);
        translateAnimation1.setInterpolator(new OvershootInterpolator());
        effectSoundOnOffBtn.startAnimation(translateAnimation1);

        TranslateAnimation translateAnimation2 = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF , 2
                ,Animation.RELATIVE_TO_SELF , 0
                ,Animation.RELATIVE_TO_SELF , 0
                ,Animation.RELATIVE_TO_SELF , 0);
        translateAnimation2.setDuration(500);
        translateAnimation2.setInterpolator(new OvershootInterpolator());
        backgroundSoundOnOffBtn.startAnimation(translateAnimation2);

        TranslateAnimation translateAnimation3 = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF , 2
                ,Animation.RELATIVE_TO_SELF , 0
                ,Animation.RELATIVE_TO_SELF , 0
                ,Animation.RELATIVE_TO_SELF , 0);
        translateAnimation3.setDuration(750);
        translateAnimation3.setInterpolator(new OvershootInterpolator());
        gobackBtn.startAnimation(translateAnimation3);
    }

    //게임 이어하기
    private void continueGame() {
        if(gameStatus == GAME_TERMINATED) return;

        gameStatus = GAME_PLAYING;
        touchStatus = true;
        timerThreadContoller = true;
        startGameTimer();

        if(backgroundSound) mediaPlayer.start();
        pauseAndContinueBtn.setImageDrawable(getDrawable(R.drawable.pause_btn));
        screenCover.setVisibility(View.INVISIBLE);

        //애니메이션
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.fold_menu_anim1);
        effectSoundOnOffBtn.startAnimation(anim);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                effectSoundOnOffBtn.setVisibility(View.INVISIBLE);
            }
            @Override
            public void onAnimationStart(Animation animation) {

            }
            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        anim = AnimationUtils.loadAnimation(this, R.anim.fold_menu_anim2);
        backgroundSoundOnOffBtn.startAnimation(anim);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                backgroundSoundOnOffBtn.setVisibility(View.INVISIBLE);
            }
            @Override
            public void onAnimationStart(Animation animation) {

            }
            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        anim = AnimationUtils.loadAnimation(this, R.anim.fold_menu_anim3);
        gobackBtn.startAnimation(anim);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                gobackBtn.setVisibility(View.INVISIBLE);
            }
            @Override
            public void onAnimationStart(Animation animation) {

            }
            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    /**
     * 메소드 흐름
     *
     * startGame()
     *     ▽
     * startGameTimer()
     *     ▽
     * endGame()
     *     ▽
     * replay() endGame()에서 유저 선택에 따라 replay()로 올 수 있음
     *     ▽
     * startGame()
     *     ＇
     *     ＇
     *     ＇
     *     ＇
     */

    //게임 시작
    private void startGame() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //사운드 재생
        if(backgroundSound)
            mediaPlayer.start();

        //효과음 재생
        if(effectSound)
            gameStartSoundReturnNumber = soundPool.play(gameStartSound, effectSoundVolume, effectSoundVolume,  1,  0,  1.0f);

        //타이머바 셋팅
        timerBar.setProgress(targetNumber);

        //관련 변수 초기화
        timerThreadContoller = true; //타이머 쓰레드 컨트롤 변수
        gameStatus = GAME_PLAYING;

        //타이머 바와 Game Start 메시지 보이게 함
        timerBar.setVisibility(View.VISIBLE);
        gameStartMessage.setVisibility(View.VISIBLE);
        gameStartMessage.bringToFront();

        //글자 날아오는 애니메이션
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.ready_go_anim);
        gameStartMessage.startAnimation(animation);

        //'Ready Go!!' 메시지 1.5초 뒤 invisible처리
        //1.5초 뒤 게임 타이머 시작
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //글자 사라지는 애니메이션
                Animation animation = AnimationUtils.loadAnimation(GameActivity.this, R.anim.ready_go_anim2);
                gameStartMessage.startAnimation(animation);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        gameStartMessage.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onAnimationStart(Animation animation) {

                    }
                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

                startGameTimer();
                touchStatus = true; //터치 가능
            }
        }, 1500);
    }

    //게임 타이머 시작
    private void startGameTimer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //타이머 루프
                while(timerThreadContoller) {
                    try {
                        Thread.sleep(100);
                        timer++; //순수 게임시간
                        stackedNumber++;

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                //0.1초 마다 타이머 바 갱신
                                timerBar.setProgress(targetNumber - stackedNumber);
                            }
                        });

                        if(stackedNumber >= targetNumber) {
                            //게임 종료 조건
                            timerThreadContoller = false;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                //게임 상태가 GAME_PLAYING 일때만 게임 종료로 넘어감
                //이유는 onDestory에서도 위의 타이머 루프를 빠져나오기 때문. 그 땐 endGame()이 실행되면 안됨
                if(gameStatus != GAME_PLAYING)  {
                    return;
                } else {
                    endGame();
                }
            }
        }).start();
    }

    //게임 종료
    private void endGame() {
        //기록 저장
        saveRecord();

        mediaPlayer.stop();
        touchStatus = false;
        gameStatus = GAME_TERMINATED;

        handler.post(new Runnable() {
            @Override
            public void run() {
                //게임 종료 다이얼로그 생성
                LayoutInflater inflater = LayoutInflater.from(GameActivity.this);
                final View dialogLayout = inflater.inflate(R.layout.game_result, null);
                final Dialog dialog = new Dialog(GameActivity.this);

                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); //다이얼로그 제목 삭제
                dialog.setContentView(dialogLayout);
                dialog.setCancelable(false);
                dialog.show();

                TextView score = dialogLayout.findViewById(R.id.result_score);
                TextView playTime = dialogLayout.findViewById(R.id.result_play_time);
                TextView player = dialogLayout.findViewById(R.id.result_player);
                ImageView dustCryingImage = dialogLayout.findViewById(R.id.result_crying_dust);
                ImageView backBtn = dialogLayout.findViewById(R.id.result_back_btn);
                ImageView replayBtn = dialogLayout.findViewById(R.id.result_replay_btn);
                ImageView newScore = dialogLayout.findViewById(R.id.result_new_high_score);

                score.setText(String.format("%,d", userScore));
                playTime.setText(String.format("%.1fs", (float)timer/10));
                player.setText(setting.getPlayerName());
                newScore.setVisibility(userScore == highScore ? View.VISIBLE : View.INVISIBLE);

                //종료 창 먼지 애니메이션
                //점수가 신기록이면 하트 애니메이션 신기록이 아니면 우는 애니메이션
                dustCryingImage.setBackgroundResource(userScore == highScore ? R.drawable.dust_heart_anim : R.drawable.dust_crying_anim);
                final AnimationDrawable anim = (AnimationDrawable)dustCryingImage.getBackground();
                anim.start();

                //종료 효과음 출력
                //신기록 여부에 따라 다른 효과음 출력
                if(effectSound)
                    soundPool.play(userScore == highScore ? gameEndSound2 : gameEndSound1, effectSoundVolume, effectSoundVolume,  1,  0,  1.0f);

                backBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(effectSound)
                            soundPool.play(btnClick1, effectSoundVolume, effectSoundVolume,  1,  0,  1);

                        anim.stop();
                        dialog.dismiss();
                        finish();
                    }
                });

                replayBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(effectSound)
                            soundPool.play(btnClick1, effectSoundVolume, effectSoundVolume,  1,  0,  1);

                        anim.stop();
                        dialog.dismiss();
                        gameReplay();
                    }
                });
            }
        });
    }

    //게임 다시하기
    private void gameReplay() {
        //기존 plate 내의 먼지 전부 제거
        for(int q = 0 ; q < dustArray.length ; q++) {
            for(int w = 0 ; w < dustArray[q].length ; w++) {
                layout.removeView(dustArray[q][w]);
                dustArray[q][w] = null;
            }
        }

        do {
            //겹치는게 없을 때까지 plate 셋팅
            setDustArray();
        } while (checkDustArray());

        basicSetting();
        startGame();
    }

    //plate가 다 셋팅되고나서 게임 시작을 위한 초기 셋팅
    private void basicSetting() {
        //점수 셋팅
        userScore = 0;
        userScoreView.setText(""+String.format("%,d", userScore));
        highScoreView.setText(""+String.format("%,d", highScore));

        //미디어 플레이어 셋팅
        setMediaPlayer();

        timer = 0;
        stackedNumber = 0;
    }

    //기록 저장
    private void saveRecord() {
        for(int q = 0 ; q < records.size() ; q++) {
            //유저 기록이 순위에 맞도록 추가됨
            if(records.get(q).getScore() < userScore) {
                records.add(q, new Record(userScore, (float)timer/10, setting.getPlayerName()));
                return;
            }
        }

        records.add(new Record(userScore, (float)timer/10, setting.getPlayerName()));
    }

    private void setMediaPlayer() {
        Log.i("###########", "미디어 플레이어 셋팅");
        mediaPlayer = MediaPlayer.create(this, R.raw.bgm);
        mediaPlayer.setLooping(true);
        mediaPlayer.setVolume(backgroundMusicVolume, backgroundMusicVolume);
    }

    //리스너 삭제 메소드
    private void removeOnGlobalLayoutListener(ViewTreeObserver observer, ViewTreeObserver.OnGlobalLayoutListener listener) {
        if(observer == null) return ;
        observer.removeOnGlobalLayoutListener(listener);
    }


    /////////////////////////생명 주기
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        pressBackBtn = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(backgroundSound) {

            if(mediaPlayer != null)
                mediaPlayer.start();
        } else {
            setMediaPlayer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //게임 일시 중지
        if(gameStatus == GAME_PLAYING && !pressBackBtn) {
            pauseGame();
        }

        //게임 셋팅값(효과음, 배경음) 저장
        String json = gson.toJson(setting);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("setting", json);
        editor.commit();

        if(mediaPlayer != null)
            mediaPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //기록 저장
        SharedPreferences.Editor editor = pref.edit();
        Gson gson = new Gson();

        String recordJson = gson.toJson(records);
        editor.putString("records", recordJson);
        editor.commit();

        //gameStartSound stop
        soundPool.stop(gameStartSoundReturnNumber);

        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;

        //쓰레드 중지
        gameStatus = GAME_TERMINATED;
        timerThreadContoller = false;
    }
}
