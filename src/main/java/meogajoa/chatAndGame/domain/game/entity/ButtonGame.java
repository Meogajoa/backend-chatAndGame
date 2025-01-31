package meogajoa.chatAndGame.domain.game.entity;

import meogajoa.chatAndGame.domain.game.publisher.RedisPubSubGameMessagePublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ButtonGame implements MiniGame {
    private String id;
    private List<Long> twentyButtons;
    private List<Long> fiftyButtons;
    private List<Long> hundredButtons;
    private final RedisPubSubGameMessagePublisher redisPubSubGameMessagePublisher;
    private boolean isBlind = false;

    public ButtonGame(RedisPubSubGameMessagePublisher redisPubSubGameMessagePublisher, String id) {
        this.id = id;
        this.twentyButtons = new ArrayList<>();
        this.fiftyButtons = new ArrayList<>();
        this.hundredButtons = new ArrayList<>();
        this.redisPubSubGameMessagePublisher = redisPubSubGameMessagePublisher;
    }

    @Override
    public void publishCurrentStatus(){
        if(!isBlind) redisPubSubGameMessagePublisher.publishButtonGameStatus(twentyButtons, fiftyButtons, hundredButtons, id);
        else redisPubSubGameMessagePublisher.publishButtonGameStatus(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), id);
    }

    @Override
    public void clickButton(Long number, String button){
        for(int i = 0; i < twentyButtons.size(); i++){
            if(twentyButtons.get(i).equals(number)){
                twentyButtons.remove(number);
                break;
            }
        }

        for(int i = 0; i < fiftyButtons.size(); i++){
            if(fiftyButtons.get(i).equals(number)){
                fiftyButtons.remove(i);
                break;
            }
        }

        for(int i = 0; i < hundredButtons.size(); i++){
            if(hundredButtons.get(i).equals(number)){
                hundredButtons.remove(i);
                break;
            }
        }

        if(button.equals("twenty")){
            twentyButtons.add(number);
        } else if(button.equals("fifty")){
            fiftyButtons.add(number);
        } else if(button.equals("hundred")){
            hundredButtons.add(number);
        } else {
            System.out.println("잘못된 버튼 클릭입니다.");
        }

        publishCurrentStatus();
    }

    @Override
    public void setBlind(boolean isBlind) {
        this.isBlind = isBlind;
    }

    @Override
    public void startGame() {

    }

    @Override
    public void endGame() {

    }

}
