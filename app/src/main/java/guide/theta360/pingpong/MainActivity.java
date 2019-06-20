/**
 * Copyright 2018 Ricoh Company, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package guide.theta360.pingpong;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import guide.theta360.pingpong.task.TakePictureTask;
import guide.theta360.pingpong.task.TakePictureTask.Callback;
import com.theta360.pluginlibrary.activity.PluginActivity;
import com.theta360.pluginlibrary.callback.KeyCallback;
import com.theta360.pluginlibrary.receiver.KeyReceiver;
import com.theta360.pluginlibrary.values.LedColor;
import com.theta360.pluginlibrary.values.LedTarget;

import guide.theta360.pingpong.oled.Oled;



public class MainActivity extends PluginActivity {
    private TakePictureTask.Callback mTakePictureTaskCallback = new Callback() {
        @Override
        public void onTakePicture(String fileUrl) {

        }
    };

    //Z1固有Fnボタンのキーコード定義
    private static final int KEYCODE_FUNCTION = 119;

    Oled oledDisplay = null;        //OLED描画クラス

    //OLED表示スレッド終了用
    private boolean mFinished;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set enable to close by pluginlibrary, If you set false, please call close() after finishing your end processing.
        setAutoClose(true);

        //OLED描画クラスをnewする
        oledDisplay = new Oled(getApplicationContext());

        oledDisplay.brightness(100);     //輝度設定
        oledDisplay.clear(oledDisplay.black); //表示領域クリア設定
        oledDisplay.draw();                     //表示領域クリア結果を反映


        // Set a callback when a button operation event is acquired.
        setKeyCallback(new KeyCallback() {
            @Override
            public void onKeyDown(int keyCode, KeyEvent event) {
                if (keyCode == KeyReceiver.KEYCODE_CAMERA) {
                    /*
                     * To take a static picture, use the takePicture method.
                     * You can receive a fileUrl of the static picture in the callback.
                     */
                    new TakePictureTask(mTakePictureTaskCallback).execute();
                }

                if (keyCode == KeyReceiver.KEYCODE_WLAN_ON_OFF) {
                    btnUpStat=true;
                }
                if (keyCode == KEYCODE_FUNCTION) {
                    btnDownStat=true;
                }
            }

            @Override
            public void onKeyUp(int keyCode, KeyEvent event) {
                /**
                 * You can control the LED of the camera.
                 * It is possible to change the way of lighting, the cycle of blinking, the color of light emission.
                 * Light emitting color can be changed only LED3.
                 */
                notificationLedBlink(LedTarget.LED3, LedColor.BLUE, 1000);

                if (keyCode == KeyReceiver.KEYCODE_WLAN_ON_OFF) {
                    btnUpStat=false;
                }
                if (keyCode == KEYCODE_FUNCTION) {
                    btnDownStat=false;
                }
            }

            @Override
            public void onKeyLongPress(int keyCode, KeyEvent event) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isApConnected()) {

        }

        //スレッド開始
        mFinished = false;
        drawOledThread();
    }

    @Override
    protected void onPause() {
        // Do end processing
        //close();

        //スレッドを終わらせる指示。終了待ちしていません。
        mFinished = true;

        super.onPause();
    }

    //OLE描画スレッド
    public void drawOledThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                //描画ループ
                while (mFinished == false) {

                    try {

                        //ピンポンゲーム
                        updatePaddlePositions();
                        moveBall();
                        drawGame();
                        if (checkWin()!=0)
                        {
                            drawWin(checkWin());
                            Thread.sleep(5000);
                            cleanUp();
                        }

                        //描画が高頻度になりすぎないよう5msスリープする
                        Thread.sleep(5);

                    } catch (InterruptedException e) {
                        // Deal with error.
                        e.printStackTrace();
                    } finally {
                    }
                }
            }
        }).start();
    }

    //============================================
    // ピンポンゲーム
    //============================================
    boolean btnUpStat = false;
    boolean btnDownStat = false;

    int scoreToWin = 5;
    int playerScore = 0;
    int player2Score = 0;

    double paddleWidth = 3;
    double paddleHeight = 8;
    double halfPaddleWidth = paddleWidth/2.0;
    double halfPaddleHeight = paddleHeight/2.0;

    double player1PosX = 1.0 + halfPaddleWidth;
    double player1PosY = 0.0;
    double player2PosX = (Oled.OLED_WIDTH) - 1.0 - halfPaddleWidth;
    double player2PosY = 0.0;
    double enemyVelY = 0.5;

    final double ballRadius = 2.0;
    final double ballSpeedX = 1.0;
    double ballPosX = (Oled.OLED_WIDTH)/2.0;
    double ballPosY = (Oled.OLED_HEIGHT)/2.0;
    double ballVelX = -1.0 * ballSpeedX;
    double ballVelY = 0;

    final int PLAYER_1_WIN = 1;
    final int PLAYER_2_WIN = 2;

    final int SINGLE_PLAYER = 0;
    final int MULTI_PLAYER = 1;
    int playMode = SINGLE_PLAYER;

    void updatePaddlePositions() {
        if (btnUpStat) {
            player1PosY--;
        }
        if (btnDownStat) {
            player1PosY++;
        }
        player1PosY = constrainPosition(player1PosY);

        if (player2PosY < ballPosY)
        {
            player2PosY += enemyVelY;
        }
        else if(player2PosY > ballPosY)
        {
            player2PosY -= enemyVelY;
        }
        player2PosY = constrainPosition(player2PosY);

    }

    double constrainPosition(double position) {
        double newPaddlePosY = position;

        if (position - halfPaddleHeight < 0)
        {
            newPaddlePosY = halfPaddleHeight;
        }
        else if (position + halfPaddleHeight > (Oled.OLED_HEIGHT) )
        {
            newPaddlePosY = (Oled.OLED_HEIGHT) - halfPaddleHeight;
        }

        return newPaddlePosY;
    }

    void moveBall(){
        ballPosY += ballVelY;
        ballPosX += ballVelX;

        if (ballPosY < ballRadius)
        {
            ballPosY = ballRadius;
            ballVelY *= -1.0;
        }
        else if (ballPosY > (Oled.OLED_HEIGHT) - ballRadius)
        {
            ballPosY = (Oled.OLED_HEIGHT) - ballRadius;
            ballVelY *= -1.0;
        }

        if (ballPosX < ballRadius)
        {
            ballPosX = ballRadius;
            ballVelX = ballSpeedX;
            player2Score++;
        }
        else if (ballPosX > (Oled.OLED_WIDTH) - ballRadius)
        {
            ballPosX = (Oled.OLED_WIDTH) - ballRadius;
            ballVelX *= -1.0 * ballSpeedX;
            playerScore++;
        }

        if (ballPosX < player1PosX + ballRadius + halfPaddleWidth)
        {
            if (ballPosY > player1PosY - halfPaddleHeight - ballRadius &&
                    ballPosY < player1PosY + halfPaddleHeight + ballRadius)
            {
                ballVelX = ballSpeedX;
                ballVelY = 2.0 * (ballPosY - player1PosY) / halfPaddleHeight;
            }
        }
        else if (ballPosX > player2PosX - ballRadius - halfPaddleWidth)
        {
            if (ballPosY > player2PosY - halfPaddleHeight - ballRadius &&
                    ballPosY < player2PosY + halfPaddleHeight + ballRadius)
            {
                ballVelX = -1.0 * ballSpeedX;
                ballVelY = 2.0 * (ballPosY - player2PosY) / halfPaddleHeight;
            }
        }
    }
    void drawGame(){
        oledDisplay.clear();
        drawScore(playerScore, player2Score);
        drawPaddle((int)player1PosX, (int)player1PosY);
        drawPaddle((int)player2PosX, (int)player2PosY);
        drawBall((int)ballPosX, (int)ballPosY);
        oledDisplay.draw();
    }
    void drawScore(int player1, int player2){
        int cursorX=58;
        int cursorY=2;
        oledDisplay.setString(cursorX, cursorY,Integer.toString(player1), oledDisplay.white, true);
        cursorX=70;
        cursorY=2;
        oledDisplay.setString(cursorX, cursorY,Integer.toString(player2), oledDisplay.white, true);
    }
    void drawPaddle(int x, int y){
        oledDisplay.rect(
                (int)(x - halfPaddleWidth),
                (int)(y - halfPaddleHeight),
                (int)paddleWidth,
                (int)paddleHeight);
    }
    void drawBall(int x, int y){
        oledDisplay.circle(x, y, 1);
    }

    int checkWin(){
        if (playerScore >= scoreToWin)
        {
            return PLAYER_1_WIN;
        }
        else if (player2Score >= scoreToWin)
        {
            return PLAYER_2_WIN;
        }

        return 0;
    }

    void drawWin(int player){
        int cursorX=44;
        int cursorY=2;

        oledDisplay.clear();
        if (player == PLAYER_1_WIN)
        {
            oledDisplay.setString(cursorX, cursorY,"Player 1");
        }
        else if (player == PLAYER_2_WIN)
        {
            oledDisplay.setString(cursorX, cursorY,"Player 2");
        }
        cursorX+=10;
        cursorY=12;
        oledDisplay.setString(cursorX, cursorY,"Wins!");
        oledDisplay.draw();
    }

    void cleanUp(){
        playerScore=0;
        player2Score=0;

        ballPosX = (Oled.OLED_WIDTH)/2.0;
        ballPosY = (Oled.OLED_HEIGHT)/2.0;
        ballVelX = -1.0 * ballSpeedX;
        ballVelY = 0;

        oledDisplay.clear();
        oledDisplay.draw();
    }

}