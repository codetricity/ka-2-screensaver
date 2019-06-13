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

package guide.theta360.screensaver;

import android.os.Bundle;
import android.view.KeyEvent;

import com.theta360.pluginapplication.R;

import guide.theta360.screensaver.task.TakePictureTask;

import com.theta360.pluginlibrary.activity.PluginActivity;
import com.theta360.pluginlibrary.callback.KeyCallback;
import com.theta360.pluginlibrary.receiver.KeyReceiver;
import com.theta360.pluginlibrary.values.LedColor;
import com.theta360.pluginlibrary.values.LedTarget;

import guide.theta360.screensaver.oled.Oled;


public class MainActivity extends PluginActivity {
    private TakePictureTask.Callback mTakePictureTaskCallback = new TakePictureTask.Callback() {
        @Override
        public void onTakePicture(String fileUrl) {

        }
    };

    //Z1固有Fnボタンのキーコード定義
    private static final int KEYCODE_FUNCTION = 119;

    //onKeyUp()での長押し操作認識用
    boolean longPressWLAN = false;
    boolean longPressFUNC = false;

    Oled oledDisplay = null;        //OLED描画クラス
    boolean oledInvert = false;   //OLED白黒反転状態

    //OLED表示の操作モード関連
    private static final int OLED_MODE_MOVE_H = 0;
    private static final int OLED_MODE_MOVE_V = 1;
    private static final int OLED_MODE_CANGE_THRESH = 2;
    int oledMode = OLED_MODE_MOVE_H;
    int oledModeTmp = oledMode;

    int dispX = 0;              //写真表示領域 開始位置 x
    int dispY = 0;              //写真表示領域 開始位置 y
    int dispWidth = 92;        //写真表示領域 幅
    int dispHeight = 24;        //写真表示領域 高さ

    String srcFileName = "kuma7_192.jpg";
    int bitmapThresh = 92;     //表示画像の輝度閾値
    int srcX = 48;              //表示画像の開始点 x
    int srcY = 36;              //表示画像の開始点 y

    int textStartX = dispWidth + 1;  //文字領域 横方向 開始位置 x
    int textLine1 = 0;              //文字領域 1行目 高さ
    int textLine2 = 8;              //文字領域 2行目 高さ
    int textLine3 = 16;             //文字領域 3行目 高さ

    //OLED表示スレッド終了用
    private boolean mFinished;
    private boolean demoDisplay;


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
            }

            @Override
            public void onKeyUp(int keyCode, KeyEvent event) {
                /**
                 * You can control the LED of the camera.
                 * It is possible to change the way of lighting, the cycle of blinking, the color of light emission.
                 * Light emitting color can be changed only LED3.
                 */
                notificationLedBlink(LedTarget.LED3, LedColor.BLUE, 1000);

                if (keyCode == KeyReceiver.KEYCODE_CAMERA) {
                    //撮影するとデモモード
                    demoDisplay = true;
                } else if (keyCode != KEYCODE_FUNCTION) {
                    //Fnボタン操作以外でデモモード解除
                    demoDisplay = false;
                }

                if (keyCode == KEYCODE_FUNCTION) {
                    if (longPressFUNC) {
                        longPressFUNC = false;
                    } else {
                        if (oledInvert) {
                            oledInvert = false;
                        } else {
                            oledInvert = true;
                        }
                    }
                } else if (keyCode == KeyReceiver.KEYCODE_MEDIA_RECORD) {
                    if (oledMode == OLED_MODE_MOVE_V) {
                        srcY += 4;
                        if (srcY > (oledDisplay.imgHeight - dispHeight)) {
                            srcY = oledDisplay.imgHeight - dispHeight;
                        }
                    } else if (oledMode == OLED_MODE_MOVE_H) {
                        srcX += 4;
                        if (srcX > (oledDisplay.imgWidth - dispWidth)) {
                            srcX = oledDisplay.imgWidth - dispWidth;
                        }
                    } else if (oledMode == OLED_MODE_CANGE_THRESH) {
                        bitmapThresh += 8;
                        if (bitmapThresh > 256) {
                            bitmapThresh = 256;
                        }
                    }

                } else if (keyCode == KeyReceiver.KEYCODE_WLAN_ON_OFF) {
                    if (longPressWLAN) {
                        longPressWLAN = false;
                    } else {
                        if (oledMode == OLED_MODE_MOVE_V) {
                            srcY -= 4;
                            if (srcY < 0) {
                                srcY = 0;
                            }
                        } else if (oledMode == OLED_MODE_MOVE_H) {
                            srcX -= 4;
                            if (srcX < 0) {
                                srcX = 0;
                            }
                        } else if (oledMode == OLED_MODE_CANGE_THRESH) {
                            bitmapThresh -= 8;
                            if (bitmapThresh < 0) {
                                bitmapThresh = 0;
                            }
                        }
                    }
                }

            }

            @Override
            public void onKeyLongPress(int keyCode, KeyEvent event) {
                if (keyCode == KEYCODE_FUNCTION) {
                    longPressFUNC = true;

                    if (oledMode == OLED_MODE_MOVE_H) {
                        oledMode = OLED_MODE_MOVE_V;
                    } else {
                        oledMode = OLED_MODE_MOVE_H;
                    }
                } else if (keyCode == KeyReceiver.KEYCODE_WLAN_ON_OFF) {
                    longPressWLAN = true;

                    if (oledMode == OLED_MODE_CANGE_THRESH) {
                        oledMode = oledModeTmp;
                    } else {
                        oledModeTmp = oledMode;
                        oledMode = OLED_MODE_CANGE_THRESH;
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isApConnected()) {

        }

        //デモ表示開始設定
        demoDisplay = true;

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
                        if (demoDisplay) {
                            demoDraw();
                        } else {
                            drawImageControl();
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

    private void drawImageControl() {
        //写真表示領域 描画
        oledDisplay.setBitmap(dispX, dispY, dispWidth, dispHeight, srcX, srcY, bitmapThresh, srcFileName);

        //文字表示領域 クリア
        oledDisplay.rectFill(textStartX, textLine1, Oled.OLED_WIDTH - dispWidth, dispHeight, oledDisplay.black, false);

        //操作可能なパラメータの左に '*'を描画
        if (oledMode == OLED_MODE_MOVE_H) {
            oledDisplay.setString(textStartX, textLine1, "*");
        } else if (oledMode == OLED_MODE_MOVE_V) {
            oledDisplay.setString(textStartX, textLine2, "*");
        } else if (oledMode == OLED_MODE_CANGE_THRESH) {
            oledDisplay.setString(textStartX, textLine3, "*");
        }

        //各パラメータを描画
        int dispNumX = textStartX + Oled.FONT_WIDTH;
        oledDisplay.setString(dispNumX, textLine1, "X=" + Integer.toString(srcX));
        oledDisplay.setString(dispNumX, textLine2, "Y=" + Integer.toString(srcY));
        oledDisplay.setString(dispNumX, textLine3, "T=" + Integer.toString(bitmapThresh));

        //OLEDへ出力指示（oledInvert==false は　draw()と同じ）
        oledDisplay.invert(oledInvert);
    }

    //============================================
    // 以降、デモンストレーション描画 (盛りすぎです)
    //============================================
    boolean lineRollDir = true;
    int demoMode = 0;
    int demoModeBack = 0;
    int demoX0=0;
    int demoY0=0;
    int demoX1=Oled.OLED_WIDTH-1;
    int demoY1=0;
    int demoX2=Oled.OLED_WIDTH-1;
    int demoY2=Oled.OLED_HEIGHT-1;
    int demoX3=0;
    int demoY3=Oled.OLED_HEIGHT-1;

    int demoThresh = 0;
    int demoThreshStep = 2;
    int demoImgX = 0;
    int demoImgY = 0;
    int demoStepX = 1;
    int demoStepY = 1;

    String demoText = "Demo";
    int demoTextX = 52;
    int demoTextY = 8;
    int demoTextStepX = 1;
    int demoTextStepY = -1;

    private void demoDraw() {
        oledDisplay.clear();

        if (demoMode!=4) {
            oledDisplay.circleFill(demoX0,demoY0,11);
            oledDisplay.circleFill(demoX2,demoY2,11);
            oledDisplay.circle(demoX1,demoY1, 22);
            oledDisplay.circle(demoX3,demoY3, 22);

            oledDisplay.rect(demoX2-11,demoY0-11, 22,22, oledDisplay.white,true);
            oledDisplay.rect(demoX0-11,demoY2-11, 22,22, oledDisplay.white,true);
            oledDisplay.rectFill(demoX3-23,demoY1-23, 47,47, oledDisplay.white,true);
            oledDisplay.rectFill(demoX1-23,demoY3-23, 47,47, oledDisplay.white,true);

            if (lineRollDir) {
                oledDisplay.line(demoX0,demoY0,demoX1,demoY1, oledDisplay.white,true);
                oledDisplay.line(demoX2,demoY2,demoX3,demoY3, oledDisplay.white,true);
                oledDisplay.line(demoX1,demoY1,demoX2,demoY2, oledDisplay.white,true);
                oledDisplay.line(demoX3,demoY3,demoX0,demoY0, oledDisplay.white,true);
            } else {
                oledDisplay.line(demoX2,demoY0,demoX3,demoY1, oledDisplay.white,true);
                oledDisplay.line(demoX0,demoY2,demoX1,demoY3, oledDisplay.white,true);
                oledDisplay.line(demoX3,demoY1,demoX0,demoY2, oledDisplay.white,true);
                oledDisplay.line(demoX1,demoY3,demoX2,demoY0, oledDisplay.white,true);
            }
        } else {
            oledDisplay.setBitmap(dispX, dispY, Oled.OLED_WIDTH, Oled.OLED_HEIGHT, demoImgX, demoImgY, demoThresh, srcFileName);
        }

        if (demoMode==0) {
            if (demoY1<(Oled.OLED_HEIGHT-1)) {
                demoY1++;
                demoY3=(Oled.OLED_HEIGHT-1)-demoY1;
            } else {
                if (demoX1>0){
                    demoX1--;
                    demoX3=(Oled.OLED_WIDTH-1)-demoX1;
                } else {
                    demoModeBack=1;
                    demoMode=4;
                }
            }
        } else if (demoMode==1) {
            if (demoX0<(Oled.OLED_WIDTH-1)) {
                demoX0++;
                demoX2=(Oled.OLED_WIDTH-1)-demoX0;
            } else {
                if ( demoY0<(Oled.OLED_HEIGHT-1) ) {
                    demoY0++;
                    demoY2=(Oled.OLED_HEIGHT-1)-demoY0;
                } else {
                    demoModeBack=2;
                    demoImgX = oledDisplay.imgWidth-Oled.OLED_WIDTH;
                    demoStepX=-1;
                    demoMode=4;
                }
            }
        } else if (demoMode==2) {
            if (demoY1>0) {
                demoY1--;
                demoY3=(Oled.OLED_HEIGHT-1)-demoY1;
            } else {
                if (demoX1<(Oled.OLED_WIDTH-1)) {
                    demoX1++;
                    demoX3=(Oled.OLED_WIDTH-1)-demoX1;
                } else {
                    demoModeBack=3;
                    demoImgY = oledDisplay.imgHeight-Oled.OLED_HEIGHT;
                    demoStepY = -1;
                    demoMode=4;
                }
            }
        } else if (demoMode==3) {
            if ( demoX0>0 ) {
                demoX0--;
                demoX2=(Oled.OLED_WIDTH-1)-demoX0;
            } else {
                if ( demoY0>0 ) {
                    demoY0--;
                    demoY2=(Oled.OLED_HEIGHT-1)-demoY0;
                } else {
                    if (lineRollDir) {
                        lineRollDir = false;
                    } else {
                        lineRollDir = true;
                    }
                    demoModeBack=0;
                    demoImgX = oledDisplay.imgWidth-Oled.OLED_WIDTH;
                    demoStepX=-1;
                    demoImgY = oledDisplay.imgHeight-Oled.OLED_HEIGHT;
                    demoStepY = -1;
                    demoMode=4;
                }
            }
        } else if (demoMode==4) {

            demoThresh+=demoThreshStep;
            if (demoThresh>256) {
                demoThresh=256;
                demoThreshStep*=-1;
                demoImgX = 0;
                demoImgY = 0;
                demoMode = demoModeBack;
            } else if (demoThresh<0) {
                demoThresh=0;
                demoThreshStep*=-1;
                demoImgX = 0;
                demoImgY = 0;
                demoMode = demoModeBack;
            }
            demoImgX+=demoStepX;
            if (demoImgX>(oledDisplay.imgWidth-Oled.OLED_WIDTH)) {
                demoImgX = oledDisplay.imgWidth-Oled.OLED_WIDTH;
                demoStepX=-1;
            } else if ( demoImgX < 0 ) {
                demoImgX = 0;
                demoStepX = 1;
            }
            demoImgY+=demoStepY;
            if (demoImgY>(oledDisplay.imgHeight-Oled.OLED_HEIGHT)) {
                demoImgY = oledDisplay.imgHeight-Oled.OLED_HEIGHT;
                demoStepY = -1;
            } else if ( demoImgY < 0 ) {
                demoImgY = 0;
                demoStepY = 1;
            }

        }

        oledDisplay.setString(demoTextX, demoTextY, demoText, oledDisplay.white,true);
        demoTextX += demoTextStepX ;
        if ( demoTextX > (Oled.OLED_WIDTH - (demoText.length()*Oled.FONT_WIDTH) ) ) {
            demoTextX = Oled.OLED_WIDTH - (demoText.length()*Oled.FONT_WIDTH) ;
            demoTextStepX *= -1;
        } else if ( demoTextX < 0 ) {
            demoTextX = 0;
            demoTextStepX *= -1;
        }
        demoTextY += demoTextStepY ;
        if ( demoTextY > (Oled.OLED_HEIGHT - Oled.FONT_HEIGHT ) ) {
            demoTextY = Oled.OLED_HEIGHT - Oled.FONT_HEIGHT ;
            demoTextStepY *= -1;
        } else if ( demoTextY < 0 ) {
            demoTextY = 0;
            demoTextStepY *= -1;
        }

        //OLEDへ出力指示（oledInvert==false は　draw()と同じ）
        //負荷サンプルです。oledInvert==true のときに、表示が遅くなるのがわかると思います。
        oledDisplay.invert(oledInvert);
    }

}