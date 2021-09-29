package com.example.tsuginoitte;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowMetrics;

public class MyView extends View {

    Paint paint;

    private int screenWidth,screenHeight;    //画面のサイズ
    private float offset_x,offset_y;    // 表の左上の座標
    private float wx,wy;                // 数字枠のサイズ
    private float tf,th;                // 太線と細線のサイズ

    private float csize;
    private int[][] MainBox = new int[9][9];


    public MyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
    }


    public void informValues(int m_screenWidth, int m_screenHeigh){
        screenWidth = m_screenWidth;
        screenHeight = m_screenHeigh;

        //　画面のサイズに従って表のサイズを決定する
        offset_x = screenWidth*0.1f;
        offset_y = screenHeight*0.05f;
        wx = screenWidth*0.8f/9.0f;
        wy = wx;
        tf = screenWidth/200f;
        th = tf/3;

        //invalidate();
    }

    public void updateValue( int[][] Box ){
        MainBox = Box;
        invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {

        // 枠線を引く
        paint.setColor(Color.BLACK);
        for(int m=0; m<=9 ;m++){
            float xp=offset_x + (wx+tf) * m;
            float yp=offset_y + (wy+tf) * 9;
            if(m%3==0)
                paint.setStrokeWidth(tf);
            else
                paint.setStrokeWidth(th);
            canvas.drawLine( xp, offset_y, xp, yp, paint );
        }
        for(int m=0; m<=9 ;m++){
            float xp=offset_x + (wx+tf) * 9;
            float yp=offset_y + (wy+tf) * m;
            if(m%3==0)
                paint.setStrokeWidth(tf);
            else
                paint.setStrokeWidth(th);
            canvas.drawLine( offset_x, yp, xp, yp, paint );
        }

        paint.setColor(Color.BLACK);
        paint.setTextSize(wx/3);
        // 横にABCの番号を付ける
        for(int m=0; m<9 ;m++){
            float px=offset_x + (wx+tf) * (m + 0.4f);
            float py=offset_y - (wy/5);
            char c = 'A';
            c += m;
            canvas.drawText( c+"", px, py, paint);
        }
        // 縦に123の行番号を付ける
        for(int m=0; m<9 ;m++){
            float px=offset_x - (wx*0.4f);
            float py=offset_y + (wy+tf) * (m + 0.5f) +wy*0.2f;
            int c = m + 1;
            canvas.drawText( c+"", px, py, paint);
        }





        paint.setAntiAlias(true);
        paint.setTextSize(wx);
        paint.setColor(Color.BLACK);

        csize = paint.measureText("5",0,1); // 1文字の大きさを得る

        paint.setTextSize(wx/2);
        //canvas.drawText(csize+" "+screenWidth+" "+screenHeight,
        //        wx, wy, paint);

        /***
         for(int m=0; m<9 ; m++) {
         float x = offset_x + (wx + tf) * m + wx/2 - csize/2;
         for(int n=0; n<9 ;n++) {
         float y = offset_y + (wy + tf) * n + wy*0.9f;
         canvas.drawText((MainBox[m][n]%9) +1 + "", x, y, paint);
         }
         }
         ***/

        for(int y=0;y<9;y++){
            for(int x=0;x<9;x++){
                if(CountOne(MainBox[y][x]) == 1)
                    DrawChar(canvas, MainBox[y][x], x, y);
                else
                    DrawSmallChar(canvas, MainBox[y][x], x, y );
            }
        }



    }

    // bit8-0に"1"の数が何個あるかを数える
    public int CountOne( int val ){
        int cnt,n;
        for(cnt=n=0;n<9;n++){
            if( ((val >> n ) & 1) == 1)
                cnt++;
        }
        return cnt;
    }


    // bit8-0の中で、最初に"1"がONしているbit番号を大きな文字で表示する
    public void DrawChar(Canvas canvas, int val, int x, int y){
        int n;
        for(n=0;n<9;n++){
            if( ((val>>n)&1)==1 )
                break;
        }

        paint.setAntiAlias(true);
        paint.setTextSize(wx);
        // もし16bitの上位の同じbit位置がONしていたら赤色にする
        if( ((1<<(n+16)) & val ) != 0)
            paint.setColor(Color.RED);
        else
            paint.setColor(Color.BLACK);

        float px = offset_x + (wx + tf) * x + wx/2 - csize/2;
        float py = offset_y + (wy + tf) * y + wy*0.9f;
        int v = n+1;
        canvas.drawText( v + "", px, py, paint);
    }

    // bit8-0の中で、"1"がONしているbit番号を小さな文字で表示する
    public void DrawSmallChar(Canvas canvas, int val, int x, int y ){

        float px = offset_x + (wx + tf) * x;
        float py = offset_y + (wy + tf) * y;

        paint.setStyle(Paint.Style.FILL);
        // 1bitでも上位bitがONしていたら、BOXを色塗りする
        //if( ((val>>16) & val & 0x1FF) != 0)
        //    paint.setColor(Color.parseColor("#00FF40"));    // BOXを色塗り
        //else
        //    paint.setColor(Color.WHITE);

        //canvas.drawRect(px+tf, py+tf,px+wx, py+wy, paint);

        float fx1,fx2,fy1,fy2;
        if((x%3)==0){
            fx1=px+tf/2;  fx2=fx1+wx;
        }else if((x%3)==1){
            fx1=px+th/2;  fx2=fx1+wx+tf-th;
        }else{
            fx1=px+th/2;  fx2=fx1+wx;
        }
        if((y%3)==0){
            fy1=py+tf/2;    fy2=fy1+wy;
        }else if((y%3)==1){
            fy1=py+th/2;    fy2=fy1+wy+tf-th;
        }else{
            fy1=py+th/2;    fy2=fy1+wy;
        }
        if( ((val>>16) & val & 0x1FF) != 0) {
            paint.setColor(Color.parseColor("#00FF40"));    // BOXを色塗り
            canvas.drawRect(fx1, fy1, fx2, fy2, paint);
        }


        // 小さい文字で3行3列で表示
        paint.setTextSize(wx/3);
        int n=0;
        for(int y1=0; y1<3; y1++) {
            for (int x1 = 0; x1 < 3; x1++) {
                float px1 = px + x1 * wx / 3 + (wx / 3 - csize / 3) / 2;
                float py1 = py + (y1 + 1) * wy / 3;
                if(((val>>n)&1)==1) {
                    int v = n+1;
                    // もし16bitの上位の同じbit位置がONしていたら赤色にする
                    if( ((1<<(n+16)) & val ) != 0)
                        paint.setColor(Color.RED);
                    else
                        paint.setColor(Color.BLACK);
                    canvas.drawText(v + "", px1, py1, paint);
                }
                n++;
            }
        }
    }

    // touchされた座標が、9x9の表のどこにあたるか、
    // 表の外にあるかを計算して、場所情報を返す
    // 1桁目：X座標(0-8), 2桁目：Y座標(0-8),座標外：99

    public int GetTouchPoint(float px, float py)
    {
        float fx,fy;
        int ix,iy;

        if(px<offset_x || px>offset_x+(wx+tf)*9)
            return 99;
        if(py<offset_y || py>offset_y+(wy+tf)*9)
            return 99;
        fx=(px-offset_x)/(wx+tf);
        fy=(py-offset_y)/(wy+tf);

        return ((int)fx) + ((int)fy)*10;
    }


}
