package com.example.tsuginoitte;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

public class SimpleDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("数独：次の一手");

        StringBuilder sb = new StringBuilder();
        sb.append("解析実行を押すと問題を解いて複数枚の紙芝居を生成します。" +
                "＜と＞は1枚づつめくります。＜＜と＞＞は考察が必要な箇所にJumpします。" +
                "9x9の枠にタッチすると、その枠の数字が確定する１個前にJumpします。" +
                "このアプリには解けない問題があるため、上級者向けではありません。" +
                "またこのアプリのソースコードは公開されています。\n");
        sb.append("問題編集により値を変更することができます。" +
                "空白は1-9と改行以外の文字を入力しますが、0(zero)が分かり易いと思います。" +
                "９文字未満途中の改行以降は空白とみなされます。"+
                "戻るを押して元画面に戻ってください。システムの戻るボタンでは値が更新されません。\n");
        sb.append("このアプリ専用のフォルダにファイルを保存することができます。アプリ削除時にファイルは消えます。"+
                "ファイル名を入力してライト実行を押すと、表の値をファイルに保存します。"+
                "ファイルを選択してリード実行を押すと、ファイルの値を読み込みます。"+
                "サンプルセットを押すと、あらかじめ準備された問題がファイルに保存されます（初回のみ）。");

        builder.setMessage(sb);

        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // このボタンを押した時の処理を書きます。
            }
        });
        AlertDialog dialog = builder.create();
        return dialog;
    }
}
