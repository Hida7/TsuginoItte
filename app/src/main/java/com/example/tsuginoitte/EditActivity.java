package com.example.tsuginoitte;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class EditActivity extends AppCompatActivity {

    private int Current_Position=0;

    private Context context;
    private TextView textViewFC;
    private EditText edit_fname;
    private EditText edit_text;

    private Button buttonNext;
    private Button buttonBack;
    private Button buttonDelete;
    private Button buttonLoad;
    private Button buttonStore;
    private Button buttonSample;
    private Button buttonAbout;

    private File file;
    private String[] files;

    private static final int QNUM = 5;
    private String [][] SAMPLES = {
            {"サンプル 初級", "009000500\n300070006\n000386000\n010509030\n023000480\n050208010\n000751000\n100060009\n005000200"},
            {"サンプル 中級", "080000060\n400050007\n000102000\n803070409\n009305100\n107040802\n000807000\n600020004\n030000010"},
            {"サンプル 上級", "021000000\n030060050\n540007800\n000410073\n000000000\n670089000\n008300021\n060090030\n000000540"},
            {"サンプル 名人級","000000000\n006308900\n030504020\n002070600\n001040500\n050203090\n008701200\n400000003\n000000000"}};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.edit_layout);

        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.MATRIX_STR);
        edit_text = findViewById(R.id.edit_text_sub);
        edit_text.setText(message);

        edit_fname = findViewById(R.id.edit_text_fname);
        textViewFC = findViewById(R.id.textFileCnt);
        context = getApplicationContext();      // contextをここで設定
        updateFileCnt();
        //files = context.fileList();
        //textViewFC.setText( "保存されているファイル数は" +files.length+"個です");

        /**
        if(files.length!=0){
            Current_Position=0;
            edit_fname.setText( files[0] );
        }
         **/


        // ファイル名を次のファイルに遷移させる
        buttonNext = findViewById(R.id.button_next );
        buttonNext.setOnClickListener(v -> {
            if(files.length==0)
                return;

            Current_Position++;
            if(Current_Position>=files.length)
                Current_Position=0;

            edit_fname.setText( files[Current_Position] );
            updateFileCnt();
        });
        // ファイル名を元のファイルに遷移させる
        buttonBack = findViewById(R.id.button_back );
        buttonBack.setOnClickListener(v -> {
            if(files.length==0)
                return;

            Current_Position--;
            if(Current_Position<0)
                Current_Position=files.length-1;

            edit_fname.setText( files[Current_Position] );
            updateFileCnt();
        });


        // ファイルをロードする
        buttonLoad = findViewById(R.id.button_load);
        buttonLoad.setOnClickListener(v -> {

            file = new File(context.getFilesDir(), edit_fname.getText().toString());

            try
            {
                BufferedReader br = new BufferedReader(new FileReader(file));
                StringBuilder sb = new StringBuilder();
                String str;
                int n=0;
                while ((str = br.readLine()) != null) {
                    sb.append(str);
                    if((n++)<8)
                        sb.append('\n');
                    else
                        break;
                }
                edit_text.setText( sb );
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            formatData();
        });

        // ファイルをライトする
        buttonStore = findViewById(R.id.button_store);
        buttonStore.setOnClickListener(v -> {

            formatData();

            file = new File(context.getFilesDir(), edit_fname.getText().toString());

            // try-with-resources
            try
            {
                FileWriter writer = new FileWriter(file, false);
                writer.write(edit_text.getText().toString());
                writer.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            int len=files.length;
            files = context.fileList();     //更新
            if(len!=files.length)
                Current_Position = files.length - 1;    // 追加ファイルは最終番号になる
            updateFileCnt();
        });

        // ファイルを削除する
        buttonDelete = findViewById(R.id.button_delete);
        buttonDelete.setOnClickListener(v -> {

            file = new File(context.getFilesDir(), edit_fname.getText().toString());
            file.delete();

            files = context.fileList();     //更新
            // Current_Positionが最後を指していたら1個減らす
            if(Current_Position==files.length)
                Current_Position--;
            updateFileCnt();
        });


        // Sample問題を設定する
        buttonSample = findViewById(R.id.button_sample);
        buttonSample.setOnClickListener(v->{

            int n;
            for(n=0;n<SAMPLES.length;n++) {
                file = new File(context.getFilesDir(), SAMPLES[n][0]);
                try
                {
                    FileWriter writer = new FileWriter(file, false);
                    writer.write(SAMPLES[n][1]);
                    writer.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            updateFileCnt();
        });

        // 説明文を表示する
        buttonAbout = findViewById(R.id.button_about);
        buttonAbout.setOnClickListener(v->{

            SimpleDialogFragment dialog = new SimpleDialogFragment();
            FragmentManager manager = getSupportFragmentManager();
            dialog.show(manager,"SimpleDialogFragment");

        });








        // 元画面に復帰する
        Button returnButton = findViewById(R.id.return_button);
        // lambda式
        returnButton.setOnClickListener(v -> {

            Intent intentSub = new Intent();
            formatData();
            intentSub.putExtra(MainActivity.MATRIX_STR, edit_text.getText().toString());
            setResult(RESULT_OK, intentSub);
            finish();
        });
    }

    private void updateFileCnt()
    {
        files = context.fileList();

        if(files.length==0)
            Current_Position = 0;
        else if(Current_Position>=files.length)
            Current_Position--;

        if(files.length!=0){
            edit_fname.setText( files[Current_Position] );
            textViewFC.setText( "ファイル:" + (Current_Position+1)+"/"+files.length);
        }else{
            edit_fname.setText("");
            textViewFC.setText("ファイル: 0/0");
        }
    }

    // ユーザが入力したデータを、9文字９桁の数字列に整列させる
    private void formatData()
    {
        char [][] workbox = new char [9][9];
        int x,y,p;

        for(y=0;y<9;y++) {
            for (x = 0; x < 9; x++)
                workbox[y][x] = '0';
        }
        for(x=y=p=0;p<edit_text.getText().toString().length();p++){
            char c = edit_text.getText().toString().charAt(p);
            if(c != '\n') {
                if(x<9)
                    workbox[y][x++] = c;
            }else{
                x=0;
                y++;
                if(y==9)
                    break;
            }
        }
        StringBuilder sb = new StringBuilder();
        for(y=0;y<9;y++) {
            for (x = 0; x < 9; x++)
                sb.append(workbox[y][x]);
            if(y!=8)
                sb.append('\n');
        }
        edit_text.setText(sb.toString());
    }



}
