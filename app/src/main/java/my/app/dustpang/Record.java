package my.app.dustpang;

/**
 * Created by ASUS on 2018-05-21.
 */

public class Record {

    private int score;
    private float time;
    private String player;

    public Record(int score, float time, String player) {
        this.score = score;
        this.time = time;
        this.player = player;
    }

    public int getScore() {
        return score;
    }

    public float getTime() {
        return time;
    }

    public String getPlayer() {
        return player;
    }
}
