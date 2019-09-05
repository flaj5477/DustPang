package my.app.dustpang;

/**
 * Created by ASUS on 2018-05-22.
 */

public class Setting {

    private String playerName;
    private boolean effectSound;
    private boolean backgroundSound;

    public Setting(String playerName, boolean effectSound, boolean backgroundSound) {
        this.playerName = playerName;
        this.effectSound = effectSound;
        this.backgroundSound = backgroundSound;
    }

    public void setBackgroundSound(boolean backgroundSound) {
        this.backgroundSound = backgroundSound;
    }

    public void setEffectSound(boolean effectSound) {
        this.effectSound = effectSound;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public boolean isBackgroundSound() {
        return backgroundSound;
    }

    public boolean isEffectSound() {
        return effectSound;
    }

    public String getPlayerName() {
        return playerName;
    }
}
