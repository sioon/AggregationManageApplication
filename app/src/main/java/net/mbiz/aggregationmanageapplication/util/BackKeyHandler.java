package net.mbiz.aggregationmanageapplication.util;

import android.app.Activity;
import android.widget.Toast;

public class BackKeyHandler {

        private long backKeyPressedTime = 0;
        private Activity activity;
        private Toast toast;

    public BackKeyHandler(Activity activity) {
        this.activity = activity;
    }

    public void onBackPressed() {
        if(System.currentTimeMillis() > backKeyPressedTime + 2000) {
            backKeyPressedTime = System.currentTimeMillis();
            showGuid();
            return;
        }

        if(System.currentTimeMillis() <= backKeyPressedTime + 2000) {
            activity.finish();
            toast.cancel();
        }
    }
    // 뒤로가기 버튼을 한 번 누를 시 뛰울 메세지를 위한 메서드
    private void showGuid() {
        toast = Toast.makeText(activity,"\'뒤로\' 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT);
        toast.show();
    }
}
