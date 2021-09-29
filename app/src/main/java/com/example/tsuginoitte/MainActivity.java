package com.example.tsuginoitte;

        import androidx.appcompat.app.AppCompatActivity;

        import android.content.Context;
        import android.content.Intent;
        import android.graphics.Insets;
        import android.graphics.Point;
        import android.os.Bundle;
        import android.view.Display;
        import android.view.MotionEvent;
        import android.view.View;
        import android.view.WindowInsets;
        import android.view.WindowManager;
        import android.view.WindowMetrics;
        import android.widget.Button;
        import android.widget.EditText;
        import android.widget.SeekBar;
        import android.widget.TextView;

        import java.io.BufferedReader;
        import java.io.File;
        import java.io.FileReader;
        import java.io.FileWriter;
        import java.io.IOException;
        import java.io.LineNumberInputStream;
        import java.lang.reflect.Array;
        import java.util.ArrayList;
        import java.util.Currency;
        import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private enum StateEnum {
        Raw,
        Ready,
        finish
    }

    private StateEnum MainState;

    private int[][] MainBox = new int[9][9];
    private int[][] DispBox = new int[9][9];
    private int[][] StartBox = new int [9][9];      // 現在のMatrix
    private int[][] EditBox = new int[9][9];        // 編集されたMatrix

    private TextView textView1;
    private TextView textView2;
    private EditText editText;

    //static final String BR = System.getProperty("line.separator");

    Context context;
    private File file;

    private int CursorPosition = 0;
    private boolean ResultPhase = false;
    private int DeepThinkCount;
    private final int MaxDTC=100;
    private int [] DTtable = new int[MaxDTC];


    private MyView CustomView;

    private Button inbutton;

    //
    // 解析結果保存用　構造体ArrayListを生成
    //
    ArrayList<KeepInfoStructure> arrayInfo = new ArrayList<KeepInfoStructure>();


    public static final String MATRIX_STR = "com.example.tsuginoitte.MATRIX_STR";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // 画面を明るい状態に保持する

        textView1 = findViewById(R.id.text1);
        textView2 = findViewById(R.id.text2);
        CustomView = this.findViewById(R.id.customlayout);

        textView1.setText("解析実行後、枠をTouchすると確定１個前にJumpします");

        // 画面のサイズを得る
        WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        CustomView.informValues(size.x, size.y);    // 画面サイズを描画Taskに通知する

        /***
        WindowMetrics windowMetrics = this.getWindowManager().getCurrentWindowMetrics();
        Insets insets = windowMetrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
        int ScreenWidth = windowMetrics.getBounds().width();
        int ScreenHeight = windowMetrics.getBounds().height();
        int StatusBar = insets.top;
        textView1.setText(ScreenWidth + " x " + ScreenHeight);
        CustomView.informValues(ScreenWidth, ScreenHeight);
        ***/


        context = getApplicationContext();      // contextをここで設定


        // 数値初期化
        //String IniVal="000003061005090807000806500000087006040000050800130000002709000503020100980300000";
        String IniVal="000000007097500000021400000078006300000000000003900450000009160000008290300000000";
        SetNumToBox(IniVal, StartBox);          // 文字列をBOXに設定
        CustomView.updateValue(StartBox);       // 初期値で表示させる

        MainState = StateEnum.Raw;  // State Machineの状態を初期に設定

        //
        // 構造体ArrayListを生成
        //
        //ArrayList<KeepInfoStructure> arrayInfo = new ArrayList<KeepInfoStructure>();






        // 一気に実行して履歴を残す
        Button button3 = findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MainState = StateEnum.Raw;      // いかなる状態でもクリックされたら、解析を毎回実行するように変更した
                CopyBox(MainBox,StartBox);      // Matrix設定
                int[] Qlevel=new int[10];       // 問題のレベル
                for(int n=0;n<10;n++)
                    Qlevel[n]=0;                // 初期化

                for(;;){
                    if (MainState == StateEnum.Raw) {
                        // 入力された数字にダブりが無いかをCheckする
                        if (CheckDoubleNum()) {
                            textView1.setText("設定値は正常です");
                            arrayInfo.clear();  // リストから履歴を削除
                            CursorPosition = 0;
                            ResultPhase = false;
                            arrayInfo.add(setInfo(0,textView1.getText().toString(),MainBox,MainBox));
                            // 1bitのみON以外の場合は全bitONさせる
                            OnNotfixNum(MainBox);
                            MainState = StateEnum.Ready;        // State更新
                            DeepThinkCount=0;
                        }else{
                            break;
                        }
                        //CustomView.updateValue(MainBox);
                    }else if(MainState == StateEnum.Ready) {

                        if (FinishCheck()) {
                            StringBuilder sb = new StringBuilder();     // 順次文字追加できる文字列
                            sb.append("熟考箇所:" + DeepThinkCount +"  ");
                            int dv = (Qlevel[5]>9?9:Qlevel[5])*1000 +
                                    (Qlevel[4]>9?9:Qlevel[4])*100 +
                                    (Qlevel[3]>9?9:Qlevel[3]*10 +
                                    (Qlevel[2]>9?9:Qlevel[2]) );
                            sb.append("難しさ指数:" + dv + "  ");
                            if(dv<5)   sb.append("初級レベル。");
                            else if(dv<10)  sb.append("中級レベル。");
                            else if(dv<100) sb.append("上級レベル。");
                            else sb.append("名人級レベル。");
                            textView1.setText(sb);
                            arrayInfo.add(setInfo(0,"完成",MainBox,MainBox));
                            MainState = StateEnum.finish;        // State更新
                            CustomView.updateValue(StartBox);      // 表示初期化
                            break;
                        } else if (ExecuteSameValueErase()) {
                            //CustomView.updateValue(DispBox);      // 表示更新
                            arrayInfo.add(setInfo(1,textView1.getText().toString(),MainBox,DispBox));
                            Qlevel[1]++;
                        } else if (FindUniqueNumber()) {
                            //CustomView.updateValue(DispBox);      // 表示更新
                            arrayInfo.add(setInfo(2,textView1.getText().toString(),MainBox,DispBox));
                            if(DeepThinkCount<MaxDTC)
                                DTtable[DeepThinkCount++]=arrayInfo.size()-1;
                            Qlevel[2]++;
                        } else if (TwoCountryTop()) {
                            //CustomView.updateValue(DispBox);      // 表示更新
                            arrayInfo.add(setInfo(3,textView1.getText().toString(),MainBox,DispBox));
                            if(DeepThinkCount<MaxDTC)
                                DTtable[DeepThinkCount++]=arrayInfo.size()-1;
                            Qlevel[3]++;
                        } else if (FindExistOnlyOneSide()) {
                            //CustomView.updateValue(DispBox);      // 表示更新
                            arrayInfo.add(setInfo(4,textView1.getText().toString(),MainBox,DispBox));
                            if(DeepThinkCount<MaxDTC)
                                DTtable[DeepThinkCount++]=arrayInfo.size()-1;
                            Qlevel[4]++;
                        } else if(SameNumberSameCount()){
                            //CustomView.updateValue(DispBox);      // 表示更新
                            arrayInfo.add(setInfo(5,textView1.getText().toString(),MainBox,DispBox));
                            if(DeepThinkCount<MaxDTC)
                                DTtable[DeepThinkCount++]=arrayInfo.size()-1;
                            Qlevel[5]++;
                        } else {
                            StringBuilder sb = new StringBuilder();     // 順次文字追加できる文字列
                            sb.append("解けませんでした。Giveupです。  ");
                            sb.append("熟考箇所:" + DeepThinkCount);
                            textView1.setText(sb);
                            arrayInfo.add(setInfo(6,sb.toString(),MainBox,DispBox));

                            CustomView.updateValue(DispBox);        // 表示更新
                            MainState = StateEnum.finish;               // State更新
                            break;
                        }
                    }else if(MainState == StateEnum.finish){
                        break;
                    }
                }
                textView2.setText(  CursorPosition*2 + ((!ResultPhase)?0:1) +1 + "/" + arrayInfo.size()*2);
            }
        });





        // Matrixの値を編集する

        Button sendButton = findViewById(R.id.edit_button);
//        sendButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(getApplication(), SubActivity.class);
//                startActivity(intent);
//            }
//        });

        // lambda式
        sendButton.setOnClickListener(v -> {
            // MaixBox[9][9]の値を文字列に変換する
            StringBuilder matrixstr= new StringBuilder();     // 順次文字追加できる文字列
            for(int y=0;y<9;y++){
                for(int x=0;x<9;x++){
                    if(IsOnlyOneBit(StartBox[y][x]))
                        matrixstr.append(GetBitNumber(StartBox[y][x])+1);
                    else
                        matrixstr.append("0");
                }
                if(y<8)
                    matrixstr.append("\n");
            }
            Intent intent = new Intent(getApplication(), EditActivity.class);
            intent.putExtra( MATRIX_STR, matrixstr.toString() );
            int requestCode = 1000;
            startActivityForResult(intent, requestCode );
        });






        // SeekBar
        SeekBar seekBar = findViewById(R.id.seekbar);
        // 初期値
        seekBar.setProgress(0);
        // 最大値
        seekBar.setMax(100);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //ツマミがドラッグされると呼ばれる
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(arrayInfo.size()==0)
                    return;
                CursorPosition=(arrayInfo.size()-1)*progress/100;
                ResultPhase=false;  // Cursorでは常にHint画面を出す

                int p=CursorPosition;

                CustomView.updateValue(arrayInfo.get(p).KDispBox);      // 表示更新
                CopyBox(MainBox,arrayInfo.get(p).KMainBox);     // 結果画面をMainにコピーする
                textView1.setText( arrayInfo.get(p).msg );
                textView2.setText(  CursorPosition*2 + ((!ResultPhase)?0:1) +1 + "/" + arrayInfo.size()*2);
            }

            //ツマミがタッチされた時に呼ばれる
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            //ツマミがリリースされた時に呼ばれる
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

        });

        // 前方向　Single表示　
        Button buttonf1 = findViewById(R.id.buttonf1);
        buttonf1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int p;
                if(CursorPosition*2 + ((!ResultPhase)?0:1) +1 >=arrayInfo.size()*2)
                    return;
                if(ResultPhase) {
                    p = CursorPosition + 1;
                    CustomView.updateValue(arrayInfo.get(p).KDispBox);      // 表示更新
                    textView1.setText( arrayInfo.get(p).msg );
                    ResultPhase = false;
                    CursorPosition = p; //更新
                }else{
                    p = CursorPosition;
                    CustomView.updateValue(arrayInfo.get(p).KMainBox);      // 表示更新
                    textView1.setText("");                  // 空白文
                    CopyBox(MainBox,arrayInfo.get(p).KMainBox);
                    ResultPhase = true;
                }
                //seekBar.setProgress(CursorPosition*100/arrayInfo.size());   // seekbar 位置更新
                //textView1.setText(CursorPosition+" " +p+" "+arrayInfo.get(p).id +" " + arrayInfo.get(p).msg);
                //textView1.setText( arrayInfo.get(p).msg +"  ("+ ((p+1)*100/arrayInfo.size())+"%)");

                textView2.setText(  CursorPosition*2 + ((!ResultPhase)?0:1) +1 + "/" + arrayInfo.size()*2);

            }

        });

        // 前方向　SKIP表示　
        Button buttonf2 = findViewById(R.id.buttonf2);
        buttonf2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int p;
                //if(CursorPosition*2 + ((!ResultPhase)?0:1) +1 >=arrayInfo.size()*2)
                //    return;
                if(CursorPosition >= arrayInfo.size()-1)
                    return;


                // 連続してこのボタンが押された場合などは、ポジションを進めて次の候補を探せるようにする
                if(arrayInfo.get(CursorPosition+1).id>=2 && ResultPhase && CursorPosition<arrayInfo.size()-2)
                    CursorPosition++;

                // 熟考箇所があるかを探す
                for(p=CursorPosition+1;p<arrayInfo.size()-1;p++){
                    if(arrayInfo.get(p).id >= 2) {
                        break;
                    }
                }

                if(p==arrayInfo.size()-1) {   // もし最終画面まで見つからなかったら
                    CustomView.updateValue(arrayInfo.get(p).KDispBox);      // 表示更新
                    textView1.setText( arrayInfo.get(p).msg );
                    CopyBox(MainBox,arrayInfo.get(p).KMainBox);
                    CursorPosition = p; //更新
                    ResultPhase=true;
                }else{
                    CustomView.updateValue(arrayInfo.get(p-1).KMainBox);      // Hintが無い前の画面
                    textView1.setText("次の手は??　[" + FindDTP(p) +"/" + DeepThinkCount + "]");
                    CursorPosition = p-1;   // 前の画面
                    ResultPhase=true;
                }

                textView2.setText(  CursorPosition*2 + ((!ResultPhase)?0:1) +1 + "/" + arrayInfo.size()*2);
            }

        });

        // 後方向　Single表示　
        Button buttonb1 = findViewById(R.id.buttonb1);
        buttonb1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int p;

                if(CursorPosition*2 + ((!ResultPhase)?0:1)  <= 0)
                    return;

                if(ResultPhase){
                    p=CursorPosition;
                    CustomView.updateValue(arrayInfo.get(p).KDispBox);      // 表示更新
                    textView1.setText(arrayInfo.get(p).msg);
                    ResultPhase=false;
                }else {
                    p = CursorPosition - 1;
                    CustomView.updateValue(arrayInfo.get(p).KMainBox);      // 表示更新
                    textView1.setText("");
                    CursorPosition = p; //更新
                    ResultPhase=true;
                }

                //seekBar.setProgress(CursorPosition*100/arrayInfo.size());   // seekbar 位置更新
                textView2.setText(  CursorPosition*2 + ((!ResultPhase)?0:1) +1 + "/" + arrayInfo.size()*2);
            }

        });

        // 後方向　SKIP表示　
        Button buttonb2 = findViewById(R.id.buttonb2);
        buttonb2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int p;
                if(CursorPosition==0)
                    return;
                if(arrayInfo.get(CursorPosition).id >= 2) {
                    p=CursorPosition;
                }else {
                    for (p = CursorPosition - 1; p > 0; p--) {
                        if (arrayInfo.get(p).id >= 2) {
                            break;
                        }
                    }
                }

                if(p==0){
                    CustomView.updateValue(arrayInfo.get(p).KDispBox);      // 表示更新
                    textView1.setText( arrayInfo.get(p).msg  );
                    CursorPosition = p; //更新
                    ResultPhase=false;
                }else{
                    CustomView.updateValue(arrayInfo.get(p-1).KMainBox);      // 前の結果画面
                    textView1.setText("次の手は??　[" + FindDTP(p) +"/" + DeepThinkCount + "]");
                    CursorPosition = p-1;
                    ResultPhase=true;
                }
                //seekBar.setProgress(CursorPosition*100/arrayInfo.size());   // seekbar 位置更新

                //CustomView.updateValue(arrayInfo.get(p).KDispBox);      // 表示更新
                //textView1.setText(CursorPosition+" " +p+" "+arrayInfo.get(p).id +" " + arrayInfo.get(p).msg);
                //textView1.setText( arrayInfo.get(p).msg +"  ("+ ((p+1)*100/arrayInfo.size())+"%)");
                //CursorPosition = p; //更新

                textView2.setText(  CursorPosition*2 + ((!ResultPhase)?0:1) +1 + "/" + arrayInfo.size()*2);
            }

        });


    }


    // 別画面でMatrixを編集したあとに起動されるルーティン
    // EditActivity からの返しの結果を受け取る
    protected void onActivityResult( int requestCode, int resultCode, Intent intent) {

        super.onActivityResult(requestCode, resultCode, intent);

        if(resultCode == RESULT_OK && //requestCode == RESULT_SUBACTIVITY &&
                null != intent) {
            String resp = intent.getStringExtra(MainActivity.MATRIX_STR);

            // EditされたMatrixを解読して値を設定する
            int p=0,m,x,y;
            for(y=0;y<9;y++) {
                for (x = 0; x < 9; x++) {
                    EditBox[y][x] = 0;  // 空白(0)で初期化
                }
            }
            for(y=0;y<9 && p<resp.length();y++){
                for(x=0;x<9 && p<resp.length();x++){
                    if (Character.isDigit(resp.charAt(p))) {
                        m = resp.charAt(p) - '0';
                    }else if(resp.charAt(p)=='\n') {
                        break;      // 改行だったら以降は0
                    }else{
                        m = 0;      //数字以外は0と見なす
                    }
                    if (m != 0) {
                        int val = 1;
                        val = (val << (m - 1));
                        EditBox[y][x] = val;    // 1bitだけONしている確定数字を設定
                    }else{
                        EditBox[y][x] = 0;      // 空白であることを示す0を設定
                    }
                    p++;
                }
                p++;    // 改行コードをSkipする
            }
        }

        //inbutton.performClick();
        CopyBox(StartBox, EditBox);             // Matrixの値更新
        CustomView.updateValue(StartBox);       // 表示更新
        MainState = StateEnum.Raw;  // State Machineの状態を初期に設定
        arrayInfo.clear();          // リストから履歴を削除
        CursorPosition = 0;
        ResultPhase = false;
        textView1.setText("値が更新されました");
        textView2.setText("Position");
    }



    // Matrixをタッチすると、その枠の数字が確定する直前にJumpする。
    // 最初から確定していたら、最初の画面に遷移する
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {

        int touchpoint,p,statusBarHeight;
        int [][] wbox = new int [9][9];

        if( motionEvent.getAction() == MotionEvent.ACTION_DOWN){

            // Status barの求め方がよくわからないので、暫定処理として計算で求める方法を採用している
            // API30以降は簡単に求められるようだ
            statusBarHeight = (int) Math.ceil(25 * context.getResources().getDisplayMetrics().density);

            // Y=10の桁(0-8),X=1の桁(0-8)、範囲外のときは99
            touchpoint = CustomView.GetTouchPoint(motionEvent.getX(), motionEvent.getY()-statusBarHeight);

            if(touchpoint!=99){
                for(p=0;p<arrayInfo.size();p++){
                    CopyBox(wbox,arrayInfo.get(p).KMainBox);
                    if(IsOnlyOneBit(wbox[touchpoint/10][touchpoint%10])){   // 指定枠の数字が確定していたら終了
                        break;
                    }
                }
                if(p!=arrayInfo.size()){
                    CursorPosition = p;     // 見つかった場所に遷移
                    ResultPhase=false;      // Hint画面を表示
                    CustomView.updateValue(arrayInfo.get(p).KDispBox);  // 表示更新
                    CopyBox(MainBox,arrayInfo.get(p).KMainBox);         // 結果画面をMainにコピーする
                    textView1.setText(arrayInfo.get(p).msg );
                    textView2.setText(CursorPosition*2 + ((!ResultPhase)?0:1) +1 + "/" + arrayInfo.size()*2);
                }
            }
        }
        return false;
    }








    /**
     *  構造体に値をセット
     */
    public KeepInfoStructure setInfo(int id, String msg, int MB[][], int DB[][]){
        KeepInfoStructure kis = new KeepInfoStructure();
        int [][] SMB = new int[9][9];
        int [][] SDB = new int[9][9];
        CopyBox(SMB,MB);        // 領域を確保し、値をコピーしている
        CopyBox(SDB,DB);

        kis.id = id;
        kis.msg = msg;
        kis.KMainBox = SMB;
        kis.KDispBox = SDB;
        return kis;
    }

    /**
     * 構造体用の内部クラス
     */
    class KeepInfoStructure{
        int id;
        String msg;
        int KMainBox[][];
        int KDispBox[][];
    }


    // DTtableから、指定箇所が前から何番目のDeepThink場所かを検索する
    private int FindDTP(int position){
        int n;
        for(n=0;n<DeepThinkCount;n++){
            if(DTtable[n] == position)
                return n+1;
        }
        return DeepThinkCount;      // 見つからなかった場合
    }
















    // ファイルを保存
    public void saveFile(String str) {

        // エディットテキストのテキストを取得
        String keyintext = editText.getText().toString();
        // File名生成　Keyinの1文字を追加する
        String fileName;
        if(keyintext.length()==1){
            fileName = "TestFile" + keyintext.charAt(0) +".txt";
        }else{
            fileName = "TestFile.txt";
        }
        //String fileName = "TestFile.txt";
        file = new File(context.getFilesDir(), fileName);

        // try-with-resources
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ファイルを読み出し
    public String readFile() {
        String text = null;

        // エディットテキストのテキストを取得
        String keyintext = editText.getText().toString();
        // File名生成　Keyinの1文字を追加する
        String fileName;
        if(keyintext.length()==1){
            fileName = "TestFile" + keyintext.charAt(0) +".txt";
        }else{
            fileName = "TestFile.txt";
        }
        //String fileName = "TestFile.txt";
        file = new File(context.getFilesDir(), fileName);



        String[] files = context.fileList();
        textView1.setText( files.length+"");



        // try-with-resources
        try (
                BufferedReader br = new BufferedReader(new FileReader(file))
        ) {
            text = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return text;
    }

    // ダブり数値のチェック
    // 縦、横、3x3の3種類のチェックを実施
    public boolean CheckDoubleNum() {
        int x, y, s, val, n, ef, x3, y3;
        // 設定された数字にダブりが無いかをCheckする
        // 横方向のダブりチェック
        for (ef = y = 0; y < 9 && ef == 0; y++) {
            for (s = x = 0; x < 9 && ef == 0; x++) {
                val = MainBox[y][x];
                if (IsOnlyOneBit(val)) {
                    n = GetBitNumber(val);
                    if (((1 << n) & s) != 0) {     // 同じ数字が現れた場合はNG
                        String msg = (char) ('A' + x) + "" + (char) ('1' + y) + "の値'" + (n + 1) +
                                "'が他の欄の値とダブっています";
                        textView1.setText(msg);
                        ef++;
                    } else {
                        s |= (1 << n);
                    }
                }
            }
        }
        if (ef != 0) return false;

        // 縦方向のダブりチェック
        for (ef = x = 0; x < 9 && ef == 0; x++) {
            for (s = y = 0; y < 9 && ef == 0; y++) {
                val = MainBox[y][x];
                if (IsOnlyOneBit(val)) {
                    n = GetBitNumber(val);
                    if (((1 << n) & s) != 0) {     // 同じ数字が現れた場合はNG
                        String msg = (char) ('A' + x) + "" + (char) ('1' + y) + "の値'" + (n + 1) +
                                "'が他の欄の値とダブっています";
                        textView1.setText(msg);
                        ef++;
                    } else {
                        s |= (1 << n);
                    }
                }
            }
        }
        if (ef != 0) return false;

        // 3x3のダブりチェック
        for (x3 = 0; x3 < 9; x3 += 3) {
            for (y3 = 0; y3 < 9; y3 += 3) {
                for (ef = s = x = 0; x < 3 && ef == 0; x++) {
                    for (y = 0; y < 3 && ef == 0; y++) {
                        val = MainBox[y3 + y][x3 + x];
                        if (IsOnlyOneBit(val)) {
                            n = GetBitNumber(val);
                            if (((1 << n) & s) != 0) {     // 同じ数字が現れた場合はNG
                                String msg = (char) ('A' + x3 + x) + "" + (char) ('1' + y3 + y) + "の値'" + (n + 1) +
                                        "'が他の欄の値とダブっています";
                                textView1.setText(msg);
                                ef++;
                            } else {
                                s |= (1 << n);
                            }
                        }
                    }
                    if (ef != 0) return false;
                }
                if (ef != 0) return false;
            }
        }
        return true;
    }

    // 1bitのみON以外の場合は全bitONさせる
    private void OnNotfixNum(int [][] Box) {
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                if (!IsOnlyOneBit(Box[y][x]))
                    Box[y][x] = 0x1FF;
            }
        }
    }


    // 確定した数字が縦、横、3x3の中の不確定欄に存在すれば、その数値を消す作業を行う
    // 全領域をチェックして、消した、あるいは消す候補があればtrueを返す
    // 初回は色だけ変える、2回目は実際に消す、などの処理をおこう
    // もはや消せるものが無ければfalseを返す
    private boolean ExecuteSameValueErase(){
        int ff=0;
        int [] sp = new int[2];
        CopyBox(DispBox,MainBox);   // copy
        for(int y=0;y<9 && ff==0;y++){
            for(int x=0;x<9 && ff==0;x++){
                if(IsOnlyOneBit(MainBox[y][x])) {
                    if (FindSameValueRan(x, y, sp)) {
                        int cp = GetBitNumber(MainBox[y][x]);
                        String msg = "[確定数字消去] " +
                                (char) ('A' + sp[0]) + "" + (char) ('1' + sp[1]) + "から'"
                                + (cp + 1) + "'を消します";
                        textView1.setText(msg);

                        DispBox[y][x] |= (1<< (cp+16));             // 色付け
                        DispBox[sp[1]][sp[0]] |= (1<< (cp+16));     // 色付け
                        MainBox[sp[1]][sp[0]] &= (~MainBox[y][x]);  // 数字クリア
                        ff++;
                    }
                }
            }
        }
        if(ff!=0)
            return true;
        else
            return false;
    }




    // 指定された欄の数字が、その欄の縦、横、3x3の中にあるかをチェックする
    // もしあればtrueを返す
    // sp[0] = x座標
    // sp[1] = y座標
    private boolean FindSameValueRan(int px, int py, int sp[]){
        int val,x,y,x3,y3;
        val = GetBitNumber(MainBox[py][px]);
        // 横方向のチェック
        for(x=0;x<9;x++){
            if(x!=px){
                if( ((MainBox[py][x]>>val)&1) == 1){ //　もし同じbitがONしていたら終了
                    sp[1] = py;
                    sp[0] = x;
                    return true;
                }
            }
        }
        // 縦方向のチェック
        for(y=0;y<9;y++){
            if(y!=py){
                if( ((MainBox[y][px]>>val)&1) == 1){ //　もし同じbitがONしていたら終了
                    sp[1]=y;
                    sp[0]=px;
                    return true;
                }
            }
        }
        // 3x3のチェック
        x3=(px/3)*3;
        y3=(py/3)*3;
        for(y=0;y<3;y++){
            for(x=0;x<3;x++){
                if(x3+x!=px && y3+y!=py){
                    if( ((MainBox[y3+y][x3+x]>>val)&1) == 1){ //　もし同じbitがONしていたら終了
                        sp[1] = y3+y;
                        sp[0] = x3+x;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // 1block中の9個中の不確定の数字の中で、1回しか現れないものは、
    //　その数字が確定数字となる
    //　縦、横、3x3の9個を全部Scanして、もし見つかればTrue、見つからなければFalseを返す
    //　戻り値：　rs[3]=0/1/2 縦方向/横方向/3x3
    //  　　　　　rs[2]=Y座標(0-8), rs[1]=X座標(0-8), rs[0]=Unique数字(0-8)

    private boolean FindUniqueNumber(){
        int [] r=new int[4];

        if(FindUniqueNumberSub(r)) {

            int y = r[2];
            int x = r[1];
            int val = r[0];
            String id;
            if (r[3] == 0) id = "縦";
            else if (r[3] == 1) id = "横";
            else id = "3x3";

            String msg = "[★][Unique数字検出:" + id + "] " + (char) ('A' + x) +
                    "" + (char) ('1' + y) + "の値'" + (val + 1) +
                    "'がユニークです";
            textView1.setText(msg);

            CopyBox(DispBox, MainBox);
            DispBox[y][x] |= (1 << (16 + val));         // 確定数字に色を付ける
            MainBox[y][x] = (1 << val);               // 表の値更新
            return true;
        }else{
            //String msg = "Unique数字は見つかりませんでした";
            //textView1.setText(msg);
            return false;
        }

    }


    private boolean FindUniqueNumberSub( int re[]){

        int x,y,n,b;
        int cnt[] = new int[9];
        int p[] = new int[9];

        // 縦方向チェック
        for(x=0;x<9;x++) {
            for (n = 0; n < 9; n++)
                cnt[n] = 0;  // 回数クリア
            for (y = 0; y < 9; y++) {
                b = MainBox[y][x];
                for (n = 0; n < 9; n++) {
                    if (((b >> n) & 1) == 1) {
                        cnt[n]++;       // ONしているbit番号の値を加算する
                        p[n] = y;         // 1回しか現れなければ、これが有効ポジションになる
                    }
                }
            }
            for (n = 0; n < 9; n++) {
                if (cnt[n] == 1 && !IsOnlyOneBit(MainBox[p[n]][x])) {
                    // 確定数字ではなく、1回しか現れていない数字ならば、その数字はUniqueとなる
                    re[3]=0;        // 縦方向のCheckで発見
                    re[2]=p[n];     // Y座標
                    re[1]=x;        // X座標
                    re[0]=n;        // Uniqueな数字（複数候補がある中で）
                    return true;
                }
            }
        }

        // 横方向チェック
        for(y=0;y<9;y++) {
            for (n = 0; n < 9; n++)
                cnt[n] = 0;  // 回数クリア
            for (x = 0; x < 9; x++) {
                b = MainBox[y][x];
                for (n = 0; n < 9; n++) {
                    if (((b >> n) & 1) == 1) {
                        cnt[n]++;       // ONしているbit番号の値を加算する
                        p[n] = x;         // 1回しか現れなければ、これが有効ポジションになる
                    }
                }
            }
            for (n = 0; n < 9; n++) {
                if (cnt[n] == 1 && !IsOnlyOneBit(MainBox[y][p[n]])) {
                    // 確定数字ではなく、1回しか現れていない数字ならば、その数字はUniqueとなる
                    re[3]=1;        // 横方向のCheckで発見
                    re[2]=y;        // Y座標
                    re[1]=p[n];     // X座標
                    re[0]=n;        // Uniqueな数字（複数候補がある中で）
                    return true;
                }
            }
        }

        // 3x3単位でチェック
        for(int y3=0;y3<9;y3+=3) {
            for (int x3 = 0; x3 < 9; x3 += 3) {
                for (n = 0; n < 9; n++)
                    cnt[n] = 0;  // 回数クリア
                for (y = 0; y < 3; y++) {
                    for (x = 0; x < 3; x++) {
                        b = MainBox[y3 + y][x3 + x];
                        for (n = 0; n < 9; n++) {
                            if (((b >> n) & 1) == 1) {
                                cnt[n]++;       // ONしているbit番号の値を加算する
                                p[n] = y * 3 + x;   // 1回しか現れなければ、これが有効ポジションになる
                            }
                        }
                    }
                }
                for (n = 0; n < 9; n++) {
                    if (cnt[n] == 1 && !IsOnlyOneBit(MainBox[y3 + p[n] / 3][x3 + p[n] % 3])) {
                        // 確定数字ではなく、1回しか現れていない数字ならば、その数字はUniqueとなる
                        re[3] = 2;          // 3x3のCheckで発見
                        re[2] = y3 + p[n] / 3;  // Y座標
                        re[1] = x3 + p[n] % 3;  // X座標
                        re[0] = n;          // Uniqueな数字（複数候補がある中で）
                        return true;
                    }
                }
            }
        }

        return false;
    }


    //　2国同盟
    // 同じ2bitのみがonしているものが、9cell中に2cellある場合
    // その2つの候補数字はその2cellに含まれることが確定する
    //　その結果、残りの7cellからその２つの候補数字が消せる。

    private boolean TwoCountryTop(){
        int r[] = new int[8];

        if(TwoCountry(r)){
            int x,y,x1,x2,y1,y2,v1,v2,ptn;
            v1 = r[5];      // 2個の数字
            v2 = r[4];      // 2個の数字
            y2 = r[3];      // Y2
            x2 = r[2];      // X2
            y1 = r[1];      // Y1
            x1 = r[0];      // X1

            ptn = (1<<v1) | (1<<v2);

            String id;
            if(r[6]==0)        id="縦";
            else if(r[6]==1)   id="横";
            else    id="3x3";

            String msg = "[★★][2Pair検出:" +id + "] " +
                    (char) ('A' + x1) + "" + (char) ('1' + y1) + "," +
                    (char) ('A' + x2) + "" + (char) ('1' + y2) + "の数値" +
                    (v2 + 1) + "," +(v1 + 1) +"が2Pairです";
            textView1.setText(msg);

            CopyBox(DispBox, MainBox);
            if(r[6]==0){
                for(y=0;y<9;y++){
                    DispBox[y][x1] |= ptn <<16;     // 表示用
                    if(y!=y1 && y!=y2){
                        MainBox[y][x1] &= ~ptn;     // 2つの数字を消す
                    }
                }
            }else if(r[6]==1){
                for(x=0;x<9;x++) {
                    DispBox[y1][x] |= ptn << 16;    // 表示用
                    if (x != x1 && x != x2) {
                        MainBox[y1][x] &= ~ptn;     // 2つの数字を消す
                    }
                }
            }else{
                for(y=0;y<3;y++){
                    for(x=0;x<3;x++){
                        DispBox[(y1/3)*3+y][(x1/3)*3+x] |= ptn<<16; // 表示用
                        if( !(((y1/3)*3+y)==y1 && ((x1/3)*3+x)==x1) &&
                                !(((y1/3)*3+y)==y2 && ((x1/3)*3+x)==x2))  {
                            MainBox[(y1/3)*3+y][(x1/3)*3+x] &= ~ptn;     // 2つの数字を消す
                        }

                    }
                }
            }
            return true;

        }else{
            //String msg = "2Pairが見つかりませんでした";
            //textView1.setText(msg);
            return false;
        }
    }



    private boolean TwoCountry(int r[]){
        int nine[] = new int[9];
        int re[] = new int[4];
        int x,y,x3,y3;

        for(x=0;x<9;x++){
            for(y=0;y<9;y++)
                nine[y] = MainBox[y][x];
            if(FindTwoCountryIn9(nine, re)){
                r[6] = 0;          // 縦方向
                r[5] = re[3];      // 2個の数字
                r[4] = re[2];      // 2個の数字
                r[3] = re[1];      // Y2
                r[2] = x;          // X2
                r[1] = re[0];      // Y1
                r[0] = x;          // X1
                return true;
            }
        }

        for(y=0;y<9;y++){
            for(x=0;x<9;x++)
                nine[x] = MainBox[y][x];
            if(FindTwoCountryIn9(nine, re)){
                r[6] = 1;          // 縦方向
                r[5] = re[3];      // 2個の数字
                r[4] = re[2];      // 2個の数字
                r[3] = y;          // Y2
                r[2] = re[1];      // X2
                r[1] = y;          // Y1
                r[0] = re[0];      // X1
                return true;
            }
        }


        for(y3=0;y3<9;y3+=3){
            for(x3=0;x3<9;x3+=3){
                for(y=0;y<3;y++){
                    for(x=0;x<3;x++){
                        nine[y*3+x] = MainBox[y3+y][x3+x];
                    }
                }
                if(FindTwoCountryIn9(nine, re)) {
                    r[6] = 2;          // 3x3
                    r[5] = re[3];      // 2個の数字
                    r[4] = re[2];      // 2個の数字
                    r[3] = y3 + re[1] / 3;   // Y2
                    r[2] = x3 + re[1] % 3;   // X2
                    r[1] = y3 + re[0] / 3;   // Y1
                    r[0] = x3 + re[0] % 3;   // X1
                    return true;
                }
            }
        }

        return false;
    }




    // 2国同盟
    // onしているbit数が2個のものが、9cell中に2cellあり、かつ
    // その2個の数字が、他Cellの不確定数値列の中に含まれていた場合Trueを返す
    // [3] = 2個の数字 2
    // [2] = 2個の数字 1
    // [1] = 2番目の順番
    // [0] = 1番目の順番

    private boolean FindTwoCountryIn9(int nine[], int re[]){
        int n1,n2=0,ff,val1=0,m;
        ff=0;
        for(n1=0;n1<8;n1++){
            if(CountBitNumber(nine[n1])==2){
                val1=nine[n1];
                for(n2=n1+1;n2<9;n2++) {
                    if(CountBitNumber(nine[n2])==2 && nine[n2]==val1){
                        ff++;
                        break;
                    }
                }
            }
            if(ff!=0)
                break;
        }
        if(ff==0)
            return false;
        for(ff=0,m=0;m<9;m++){
            if(m!=n1 && m!=n2){
                if( (nine[m] & val1 & 0x1FF) != 0 )
                    ff++;
            }
        }
        if(ff==0)
            return false;
        else{
            int b,b1=0,b2=0,bf=0;
            for(b=0;b<9;b++){
                if(((val1>>b)&1)==1){
                    if(bf==0) {
                        b1 = b;
                        bf++;
                    }else
                        b2 = b;
                }
            }
            re[3]=b2;
            re[2]=b1;
            re[1]=n2;
            re[0]=n1;
            return true;
        }
    }

















    // 3cellを共有blockとすると、3x3にはほかに2block、縦方向（横方向）には2block、全部で5blockになる。
    // 共有blockに含まれる候補数字1個が他の2clockに含まれていないとすると、その候補数字１個は
    // その共有blockのみに含まれる。となると他の2blockからその候補数字１個が消せることになる
    // 候補数字１個は、共有Blockに何回現れても良い。


    private boolean FindExistOnlyOneSide(){

        int x,y,x3,y3,n,m;
        int [][] fb = new int[5][3];
        int [][] fbp = new int[5][3];
        int [] re = new int[6];
        StringBuilder sb = new StringBuilder();     // 順次文字追加できる文字列

        for(y3=0;y3<9;y3+=3){
            for(x3=0;x3<9;x3+=3){
                for(y=0;y<3;y++) {
                    for (n = 0; n < 3; n++) {
                        fbp[0][n] = (y3+y)*10+(x3+n);
                        fbp[1][n] = (y3 + (y + 1) % 3)*10 + (x3 + n);
                        fbp[2][n] = (y3 + (y + 2) % 3)*10 + (x3 + n);
                        fbp[3][n] = (y3 + y)*10 + ((x3 + 3) % 9  + n);
                        fbp[4][n] = (y3 + y)*10 + ((x3 + 6) % 9  + n);
                        fb[0][n] = MainBox[y3 + y][x3 + n];
                        fb[1][n] = MainBox[y3 + (y + 1) % 3][x3 + n];
                        fb[2][n] = MainBox[y3 + (y + 2) % 3][x3 + n];
                        fb[3][n] = MainBox[y3 + y][(x3 + 3) % 9  + n];
                        fb[4][n] = MainBox[y3 + y][(x3 + 6) % 9  + n];
                    }

                    if(CheckExistEatherSide(fb,re)){

                        String s0 = (char) ('A' + (fbp[0][0]%10+re[3])) + "" + (char) ('1' + (fbp[0][0]/10)) + "";
                        String s1 = (char) ('A' + (fbp[0][0]%10+re[4])) + "" + (char) ('1' + (fbp[0][0]/10)) + "";
                        String s2 = (char) ('A' + (fbp[0][0]%10+re[5])) + "" + (char) ('1' + (fbp[0][0]/10)) + "";

                        sb.append("[★★★][排他的確定:横]");
                        if(re[2]==1){
                            sb.append(s0);
                        }else if(re[2]==2){
                            sb.append(s0+","+s1);
                        }else
                            sb.append(s0+","+s1+","+s2);

                        sb.append( "の中の'" + (re[0] + 1) + "'は確定数字です");
                        textView1.setText(sb.toString());

                        CopyBox(DispBox,MainBox);       // コピー

                        for(m=0;m<5;m++){
                            for(n=0;n<3;n++) {
                                DispBox[fbp[m][n]/10][fbp[m][n]%10] |= (1<<(16+re[0])); // 確定数字に色をつける（値は変えない）

                                if(m!=0 && (MainBox[fbp[m][n]/10][fbp[m][n]%10] & (1<<re[0])) !=0 ){
                                    MainBox[fbp[m][n]/10][fbp[m][n]%10] &= ~(1<<re[0]);    // 確定数字を消す
                                }
                            }
                        }

                        return true;
                    }
                }
            }
        }
        for(y3=0;y3<9;y3+=3){
            for(x3=0;x3<9;x3+=3){
                for(x=0;x<3;x++) {
                    for (n = 0; n < 3; n++) {
                        fbp[0][n] = (y3+n)*10+(x3+x);
                        fbp[1][n] = (y3 + n)*10 + (x3 + (x+1)%3);
                        fbp[2][n] = (y3 + n)*10 + (x3 + (x+2)%3);
                        fbp[3][n] = ((y3 + 3)%9 + n)*10 + (x3 + x);
                        fbp[4][n] = ((y3 + 6)%9 + n)*10 + (x3 + x);
                        fb[0][n] = MainBox[y3 + n][x3 + x];
                        fb[1][n] = MainBox[y3 + n][x3 + (x+1)%3];
                        fb[2][n] = MainBox[y3 + n][x3 + (x+2)%3];
                        fb[3][n] = MainBox[(y3 + 3)%9 + n][x3 + x];
                        fb[4][n] = MainBox[(y3 + 6)%9 + n][x3 + x];
                    }
                    if(CheckExistEatherSide(fb,re)){

                        String s0 = (char) ('A' + (fbp[0][0]%10)) + "" + (char) ('1' + (fbp[0][0]/10+re[3])) + "";
                        String s1 = (char) ('A' + (fbp[0][0]%10)) + "" + (char) ('1' + (fbp[0][0]/10+re[4])) + "";
                        String s2 = (char) ('A' + (fbp[0][0]%10)) + "" + (char) ('1' + (fbp[0][0]/10+re[5])) + "";

                        sb.append("[★★★][排他的確定:縦]");
                        if(re[2]==1){
                            sb.append(s0);
                        }else if(re[2]==2){
                            sb.append(s0+","+s1);
                        }else
                            sb.append(s0+","+s1+","+s2);


                        sb.append( "の中の'" + (re[0] + 1) + "'は確定数字です");
                        textView1.setText(sb.toString());

                        CopyBox(DispBox,MainBox);       // コピー

                        for(m=0;m<5;m++){
                            for(n=0;n<3;n++) {
                                DispBox[fbp[m][n]/10][fbp[m][n]%10] |= (1<<(16+re[0])); // 確定数字に色をつける（値は変えない）

                                if(m!=0 && (MainBox[fbp[m][n]/10][fbp[m][n]%10] & (1<<re[0])) !=0 ){
                                    MainBox[fbp[m][n]/10][fbp[m][n]%10] &= ~(1<<re[0]);    // 確定数字を消す
                                }
                            }
                        }
                        return true;
                    }
                }
            }
        }


        return false;
    }




    // 5block(3cell/block)=共有block、2block,2block
    // 共有blockの中の候補数字が2blockになく、他の2blockに含まれる場合Trueを返す
    // re[3,4,5] = 確定数字の3cellの場所（0,1,2)
    // re[2] = 確定数字の個数(1,2,3)
    // re[1] = 確定数字があるcellの場所　0/1=first 2block/second 2block
    // re[0] = 確定数字(0-8)

    private boolean CheckExistEatherSide(int FiveBlocks[][], int re[]){
        int p,q,m1,m2,m3,cnt,n;
        int [] s=new int[5];


        for(q=0;q<5;q++){
            for(s[q]=0,p=0;p<3;p++){
                if (q!=0 || !IsOnlyOneBit(FiveBlocks[0][p]))    // 確定数字は候補から除外
                    s[q] |= (FiveBlocks[q][p])&0x1FF; // 候補数字だけがONしている数値
            }
        }

        if(s[0]==0)  return false;
        m1 = s[0] & ~(s[3]|s[4]);   // 3cellの中に確実に納まる数字がONしている
        //for(p=0;p<3;p++) {
        //    if (IsOnlyOneBit(FiveBlocks[0][p])) {
        //        m1 &= ~FiveBlocks[0][p];        // 確定している数字は候補から消す
        //    }
        //}
        m2 = s[0] & (s[1]|s[2]);        // 3cellx3の中に含まれる候補数字がONしている
        m3 = m1 & m2;
        if( m1 != 0 && m3 != 0 ){  // もし候補数字が存在し、他の3cellx2に含まれていたらそれは消せる
            for(cnt=n=0; n<3;n++){
                if( (FiveBlocks[0][n] & m3)==m3 ){
                    re[3+cnt] = n;      // 確定数字が含まれる場所(0,1,2)
                    cnt++;
                }
            }

            re[2] = cnt;                // 確定数字の個数
            re[1] = 0;                  // 最初の2blockに存在する
            re[0] = GetBitNumber(m3);   // 消す候補値
            return true;
        }

        m1 = s[0] & ~(s[1]|s[2]);
        //for(p=0;p<3;p++) {
        //    if (IsOnlyOneBit(FiveBlocks[0][p])) {
        //        m1 &= ~FiveBlocks[0][p];        // 確定している数字は候補から消す
        //    }
        //}
        m2 = s[0] & (s[3]|s[4]);
        m3 = m1 & m2;
        if( m1 != 0 && m3 != 0 ){
            for(cnt=n=0; n<3;n++){
                if( (FiveBlocks[0][n] & m3)==m3 ){
                    re[3+cnt] = n;      // 確定数字が含まれる場所(0,1,2)
                    cnt++;
                }
            }
            re[2] = cnt;                // 確定数字の個数
            re[1] = 1;                                      // 最後の2blockに存在する
            re[0] = GetBitNumber(m3);   // 消す候補値
            return true;
        }
        return false;
    }







    private boolean SameNumberSameCount(){

        int y3,x3,y,x,n;
        int [] NineBox = new int[9];
        int [][] Rbox = new int[9][2];
        int [] Re = new int[10];

        CopyBox(DispBox, MainBox);

        // 横方向
        for(y=0;y<9;y++){
            for(x=0;x<9;x++){
                NineBox[x] = MainBox[y][x];
            }
            CountNumberApear(NineBox, Rbox);
            if(FindSameNum(NineBox, Rbox, Re)){
                if (Re[0] == 2) {   //数字が２種類の場合
                    String msg = "[★★★★][2種2欄]" + (char) ('A' + Re[3]) + "" + (char) ('1' + y) + "," +
                            (char) ('A' + Re[4]) + "" + (char) ('1' + y) + "の中に" +
                            (Re[1] + 1) + "," + (Re[2] + 1) + "が必ず含まれます";
                    textView1.setText(msg);
                    DispBox[y][Re[3]] |= (1 << (Re[1] + 16)) + (1 << (Re[2] + 16));
                    DispBox[y][Re[4]] |= (1 << (Re[1] + 16)) + (1 << (Re[2] + 16));
                    MainBox[y][Re[3]] = ((1 << Re[1]) + (1 << Re[2]));
                    MainBox[y][Re[4]] = ((1 << Re[1]) + (1 << Re[2]));
                    return true;
                }else if(Re[0]==3){     //数字が３種類の場合
                    String msg = "[★★★★][3種3欄]" + (char) ('A' + Re[3]) + "" + (char) ('1' + y) + "," +
                            (char) ('A' + Re[4]) + "" + (char) ('1' + y) + "の中に" +
                            (Re[1] + 1) + "," + (Re[2] + 1) + "が必ず含まれます";
                    textView1.setText(msg);
                    DispBox[y][Re[4]] |= (1 << (Re[1] + 16)) + (1 << (Re[2] + 16)) + (1 << (Re[3] + 16));
                    DispBox[y][Re[5]] |= (1 << (Re[1] + 16)) + (1 << (Re[2] + 16)) + (1 << (Re[3] + 16));
                    DispBox[y][Re[6]] |= (1 << (Re[1] + 16)) + (1 << (Re[2] + 16)) + (1 << (Re[3] + 16));
                    MainBox[y][Re[4]] = ((1 << Re[1]) + (1 << Re[2])+ (1 << Re[3]));
                    MainBox[y][Re[5]] = ((1 << Re[1]) + (1 << Re[2])+ (1 << Re[3]));
                    MainBox[y][Re[6]] = ((1 << Re[1]) + (1 << Re[2])+ (1 << Re[3]));
                    return true;
                }else{
                    String msg = "[★★★★][4種以上の可能性あり。要アプリ改良]";
                    textView1.setText(msg);
                }
            }
        }
        // 縦方向
        for(x=0;x<9;x++){
            for(y=0;y<9;y++){
                NineBox[y] = MainBox[y][x];
            }
            CountNumberApear(NineBox, Rbox);
            if(FindSameNum(NineBox, Rbox, Re)){
                if (Re[0] == 2) {   //数字が２種類の場合
                    String msg = "[★★★★][2種2欄]" + (char) ('A' + x) + "" + (char) ('1' + Re[3]) + "," +
                            (char) ('A' + x) + "" + (char) ('1' + Re[4]) + "の中に" +
                            (Re[1] + 1) + "," + (Re[2] + 1) + "が必ず含まれます";
                    textView1.setText(msg);
                    DispBox[Re[3]][x] |= (1 << (Re[1] + 16)) + (1 << (Re[2] + 16));
                    DispBox[Re[4]][x] |= (1 << (Re[1] + 16)) + (1 << (Re[2] + 16));
                    MainBox[Re[3]][x] = ((1 << Re[1]) + (1 << Re[2]));
                    MainBox[Re[4]][x] = ((1 << Re[1]) + (1 << Re[2]));
                    return true;
                }else if(Re[0]==3) {     //数字が３種類の場合
                    String msg = "[★★★★][3種3欄]" + (char) ('A' + x) + "" + (char) ('1' + Re[4]) + "," +
                            (char) ('A' + x) + "" + (char) ('1' + Re[5]) + "," +
                            (char) ('A' + x) + "" + (char) ('1' + Re[6]) + "," + "の中に" +
                            (Re[1] + 1) + "," + (Re[2] + 1) + "," + (Re[3] + 1) + "が必ず含まれます";
                    textView1.setText(msg);
                    DispBox[Re[4]][x] |= (1 << (Re[1] + 16)) + (1 << (Re[2] + 16)) + (1 << (Re[3] + 16));
                    DispBox[Re[5]][x] |= (1 << (Re[1] + 16)) + (1 << (Re[2] + 16)) + (1 << (Re[3] + 16));
                    DispBox[Re[6]][x] |= (1 << (Re[1] + 16)) + (1 << (Re[2] + 16)) + (1 << (Re[3] + 16));
                    MainBox[Re[4]][x] = ((1 << Re[1]) + (1 << Re[2]) + (1 << Re[3]));
                    MainBox[Re[5]][x] = ((1 << Re[1]) + (1 << Re[2]) + (1 << Re[3]));
                    MainBox[Re[6]][x] = ((1 << Re[1]) + (1 << Re[2]) + (1 << Re[3]));
                    return true;
                }else{
                    String msg = "[★★★★][4種以上の可能性あり。要アプリ改良]";
                    textView1.setText(msg);
                }
            }
        }
        for(y3=0;y3<9;y3+=3){
            for(x3=0;x3<9;x3+=3){
                for(y=0;y<3;y++){
                    for(x=0;x<3;x++){
                        NineBox[y*3+x] = MainBox[y3+y][x3+x];
                    }
                }
                CountNumberApear(NineBox, Rbox);
                if(FindSameNum(NineBox, Rbox, Re)){
                    if(Re[0]==2){
                        String msg = "[★★★★][2種2欄]" + (char) ('A' + x3 + Re[3] % 3) + "" + (char) ('1' + y3 + Re[3] / 3) + "," +
                                (char) ('A' + x3 + Re[4] % 3) + "" + (char) ('1' + y3 + Re[4] / 3) + "の中に" +
                                (Re[1] + 1) + "," + (Re[2] + 1) + "が必ず含まれます";
                        textView1.setText(msg);
                        DispBox[y3+Re[3]/3][x3+Re[3]%3] |= (1<<(Re[1]+16)) + (1<<(Re[2]+16));
                        DispBox[y3+Re[4]/3][x3+Re[4]%3] |= (1<<(Re[1]+16)) + (1<<(Re[2]+16));
                        MainBox[y3+Re[3]/3][x3+Re[3]%3] = ((1<<Re[1]) + (1<<Re[2]));
                        MainBox[y3+Re[4]/3][x3+Re[4]%3] = ((1<<Re[1]) + (1<<Re[2]));
                        return true;
                    }else if (Re[0] == 3) {
                        String msg = "[★★★★][3種3欄]" + (char) ('A' + x3 + Re[4] % 3) + "" + (char) ('1' + y3 + Re[4] / 3) + "," +
                                (char) ('A' + x3 + Re[5] % 3) + "" + (char) ('1' + y3 + Re[5] / 3) + "," +
                                (char) ('A' + x3 + Re[6] % 3) + "" + (char) ('1' + y3 + Re[6] / 3) + "," +
                                "の中に" +
                                (Re[1] + 1) + "," + (Re[2] + 1) + "," + (Re[3] + 1) + "が必ず含まれます";
                        textView1.setText(msg);
                        DispBox[y3+Re[4]/3][x3+Re[4]%3] |= (1<<(Re[1]+16)) + (1<<(Re[2]+16)) + (1<<(Re[3]+16));
                        DispBox[y3+Re[5]/3][x3+Re[5]%3] |= (1<<(Re[1]+16)) + (1<<(Re[2]+16)) + (1<<(Re[3]+16));
                        DispBox[y3+Re[6]/3][x3+Re[6]%3] |= (1<<(Re[1]+16)) + (1<<(Re[2]+16)) + (1<<(Re[3]+16));
                        MainBox[y3+Re[4]/3][x3+Re[4]%3] = ((1<<Re[1]) + (1<<Re[2]) + (1<<Re[3]));
                        MainBox[y3+Re[5]/3][x3+Re[5]%3] = ((1<<Re[1]) + (1<<Re[2]) + (1<<Re[3]));
                        MainBox[y3+Re[6]/3][x3+Re[6]%3] = ((1<<Re[1]) + (1<<Re[2]) + (1<<Re[3]));
                        return true;
                    }else{
                        String msg = "[★★★★][4種以上の可能性あり。要アプリ改良]";
                        textView1.setText(msg);
                    }
                }
            }
        }

        return false;
    }

    // ２個の数字が２回出現し、かつそれが同じ２つのCellだったら、
    // その２個の数字はその２つのcellに確実に納まる
    // つまり、その2つのcellの２個以外の数字は消せる
    // 同じように３個、４個と調べる
    private boolean FindSameNum(int NineBox[], int Rbox[][],int Re[]){
        int p1,p2,p3;

        for(p1=0;p1<8;p1++){
            if(Rbox[p1][1]==2){
                for(p2=p1+1;p2<9;p2++){
                    if(Rbox[p2][1]==2 && Rbox[p1][0]==Rbox[p2][0]
                            && (CountBitNumber(NineBox[GetOnbitAtN(Rbox[p1][0],1)])!=2
                            || CountBitNumber(NineBox[GetOnbitAtN(Rbox[p1][0],2)])!=2) ){
                        Re[0]=2;
                        Re[1]=p1;
                        Re[2]=p2;
                        Re[3]=GetOnbitAtN(Rbox[p1][0],1);
                        Re[4]=GetOnbitAtN(Rbox[p1][0],2);
                        return true;
                    }
                }
            }
        }
        for(p1=0;p1<7;p1++){
            if(Rbox[p1][1]==3){
                for(p2=p1+1;p2<8;p2++){
                    if(Rbox[p2][1]==3){
                        for(p3=p2+1;p3<9;p3++){

                            if(Rbox[p3][1]==3
                                    && Rbox[p1][0]==Rbox[p2][0] && Rbox[p1][0]==Rbox[p3][0]
                                    && ( CountBitNumber(NineBox[GetOnbitAtN(Rbox[p1][0],1)])!=3
                                    || CountBitNumber(NineBox[GetOnbitAtN(Rbox[p1][0],2)])!=3
                                    || CountBitNumber(NineBox[GetOnbitAtN(Rbox[p1][0],3)])!=3)){
                                Re[0]=3;
                                Re[1]=p1;
                                Re[2]=p2;
                                Re[3]=p3;
                                Re[4]=GetOnbitAtN(Rbox[p1][0],1);
                                Re[5]=GetOnbitAtN(Rbox[p1][0],2);
                                Re[6]=GetOnbitAtN(Rbox[p1][0],3);
                                return true;
                            }
                        }
                    }
                }
            }
        }



        return false;
    }









    // 9cell中の候補数字から、cell順ではなく、候補数字順に出現cell番号とcell数を作成する
    // Rbox[n][0]  本数字のbit0はcellの0番目であり、nは候補数字。候補数字があればbitがONしている
    // Rbox[n][1]　上記数字のONしているbit数。数字の出現回数。
    private void CountNumberApear(int NineBox[], int Rbox[][] ){
        int p,q;
        for(p=0;p<9;p++){
            Rbox[p][0]=Rbox[p][1]=0;
        }
        for(q=0;q<9;q++) {
            for (p = 0; p < 9; p++) {
                if( ((NineBox[q]>>p)&1)==1 ){
                    Rbox[p][0] |= (1<<q);       // bit0はcell番号0
                }
            }
        }
        for(p=0;p<9;p++){
            Rbox[p][1] = CountBitNumber(Rbox[p][0]);
        }
    }











    // bit31:16の上位16bitをクリアする
    private void CleanUpperArea(){
        for(int y=0;y<9;y++){
            for(int x=0;x<9;x++){
                MainBox[y][x] &= 0x0000FFFF;
            }
        }
    }





    // 終了チェック　全部確定したことをチェックする
    private boolean FinishCheck(){
        int ff=0,v;
        for(int y=0;y<9;y++){
            v = 0;
            for(int x=0;x<9;x++){
                if( ! IsOnlyOneBit(MainBox[y][x])) {
                    ff++;
                    break;
                }
                if( (v & MainBox[y][x]) != 0){
                    ff++;
                    break;
                }else{
                    v |= MainBox[y][x];
                }
            }
        }
        if(ff!=0)
            return false;

        for(int x=0;x<9;x++){
            v = 0;
            for(int y=0;y<9;y++){
                if( (v & MainBox[y][x]) != 0){
                    ff++;
                    break;
                }else{
                    v |= MainBox[y][x];
                }
            }
        }
        if(ff!=0)
            return false;

        return true;
    }


    // bit8-0のONの個数が1個か否かをチェックする
    public boolean IsOnlyOneBit(int val) {
        int cnt = 0;
        for (int n = 0; n < 9; n++) {
            if (((val >> n) & 1) == 1)
                cnt++;
        }
        if (cnt == 1)
            return true;
        else
            return false;
    }

    // bit8-0のONしているbit番号を返す(0-8)
    public int GetBitNumber(int val) {
        for (int n = 0; n < 9; n++) {
            if (((val >> n) & 1) == 1)
                return n;
        }
        return 9;   // ここに来たらError
    }

    // bit8-0のONしているbit数をカウント
    public int CountBitNumber(int val){
        int cnt=0;
        for(int n=0;n<9;n++){
            if(((val>>n)&1)==1){
                cnt++;
            }
        }
        return cnt;
    }

    // 0bit目から数えて、p番目にONしているbit番号を返す
    // もしONしているbitがなければ10を返す
    public int GetOnbitAtN(int val,int p){
        int m,n;
        for(n=m=0;m<9;m++){
            if(((val>>m)&1)==1){
                n++;
                if(n==p){
                    return m;
                }
            }
        }
        return 10;
    }




    // 9x9 cellのBOXのコピー処理
    public void CopyBox(int dest[][], int source[][]){
        for(int y=0;y<9;y++){
            for(int x=0;x<9;x++){
                dest[y][x]=source[y][x];
            }
        }
    }

    // 81個の数字を配列に収める
    private void SetNumToBox(String NumStr, int Box[][]){
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                int v = NumStr.charAt(y*9+x) - '0';
                if(v==0)
                    Box[y][x] = 0;
                else
                    Box[y][x]=(1<<(v-1));
            }
        }
    }






}

