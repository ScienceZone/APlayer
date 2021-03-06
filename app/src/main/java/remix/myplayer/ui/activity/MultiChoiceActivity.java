package remix.myplayer.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.umeng.analytics.MobclickAgent;

import butterknife.BindView;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.application.APlayerApplication;
import remix.myplayer.interfaces.OnUpdateOptionMenuListener;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.customview.TipPopupwindow;
import remix.myplayer.ui.dialog.TimerDialog;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.SPUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/9/29 10:37
 */
public class MultiChoiceActivity extends ToolbarActivity {
    @BindView(R.id.toolbar)
    Toolbar mToolBar;
    @BindView(R.id.toolbar_multi)
    ViewGroup mMultiToolBar;
    protected MultiChoice mMultiChoice = null;
    private TipPopupwindow mTipPopupWindow;
    public MultiChoice getMultiChoice(){
        return mMultiChoice;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMultiChoice = new MultiChoice(this);
        mMultiChoice.setOnUpdateOptionMenuListener(new OnUpdateOptionMenuListener() {
            @Override
            public void onUpdate(boolean multiShow) {
                mMultiChoice.setShowing(multiShow);
                mToolBar.setVisibility(multiShow ? View.GONE : View.VISIBLE);
                mMultiToolBar.setVisibility(multiShow ? View.VISIBLE : View.GONE);
                //清空
                if(!mMultiChoice.isShow()){
                    mMultiChoice.clear();
                }
                //只有主界面显示分割线
                mMultiToolBar.findViewById(R.id.multi_divider).setVisibility(MultiChoiceActivity.this instanceof MainActivity ? View.VISIBLE : View.GONE);
                //第一次长按操作显示提示框
                if(SPUtil.getValue(APlayerApplication.getContext(),"Setting","IsFirstMulti",true)){
                    SPUtil.putValue(APlayerApplication.getContext(),"Setting","IsFirstMulti",false);
                    if(mTipPopupWindow == null){
                        mTipPopupWindow = new TipPopupwindow(MultiChoiceActivity.this);
                        mTipPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                            @Override
                            public void onDismiss() {
                                mTipPopupWindow = null;
                            }
                        });
                    }
                    if(!mTipPopupWindow.isShowing() && multiShow){
                        mTipPopupWindow.show(new View(MultiChoiceActivity.this));
                    }
                }
            }
        });
    }

    @Override
    protected void setUpToolbar(Toolbar toolbar, String title) {
        super.setUpToolbar(toolbar,title);

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.toolbar_search:
                        startActivity(new Intent(MultiChoiceActivity.this, SearchActivity.class));
                        break;
                    case R.id.toolbar_timer:
                        startActivity(new Intent(MultiChoiceActivity.this, TimerDialog.class));
                        break;
                }
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        getMenuInflater().inflate(R.menu.toolbar_menu,menu);
        //主题颜色
        int themeColor = ColorUtil.getColor(ThemeStore.isLightTheme() ? R.color.black : R.color.white);
        for(int i = 0 ; i < menu.size();i++){
            MenuItem menuItem = menu.getItem(i);
            menuItem.setIcon(Theme.TintDrawable(menuItem.getIcon(),themeColor));
        }
        return true;
    }

    public void onBackPress(){
        mMultiChoice.UpdateOptionMenu(false);
        if(mTipPopupWindow != null && mTipPopupWindow.isShowing()){
            mTipPopupWindow.dismiss();
            mTipPopupWindow = null;
        }
    }


    @OnClick({R.id.multi_delete,R.id.multi_playlist,R.id.multi_playqueue})
    public void onMutltiClick(View v){
        switch (v.getId()){
            case R.id.multi_delete:
                String title = MultiChoice.TYPE == Constants.PLAYLIST ? getString(R.string.confirm_delete_playlist) : MultiChoice.TYPE == Constants.PLAYLISTSONG ?
                        getString(R.string.confirm_delete_from_playlist) : getString(R.string.confirm_delete_from_library);
                new MaterialDialog.Builder(this)
                        .content(title)
                        .buttonRippleColor(ThemeStore.getRippleColor())
                        .positiveText(R.string.confirm)
                        .negativeText(R.string.cancel)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                MobclickAgent.onEvent(MultiChoiceActivity.this,"Delete");
                                if(mMultiChoice != null)
                                    mMultiChoice.OnDelete();
                            }
                        })
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            }
                        })
                        .backgroundColorAttr(R.attr.background_color_3)
                        .positiveColorAttr(R.attr.text_color_primary)
                        .negativeColorAttr(R.attr.text_color_primary)
                        .contentColorAttr(R.attr.text_color_primary)
                        .show();
                break;
            case R.id.multi_playqueue:
                MobclickAgent.onEvent(MultiChoiceActivity.this,"AddtoPlayingList");
                if(mMultiChoice != null)
                    mMultiChoice.OnAddToPlayQueue();
                break;
            case R.id.multi_playlist:
                MobclickAgent.onEvent(MultiChoiceActivity.this,"AddtoPlayList");
                if(mMultiChoice != null)
                    mMultiChoice.OnAddToPlayList();
                break;
        }
    }
}
