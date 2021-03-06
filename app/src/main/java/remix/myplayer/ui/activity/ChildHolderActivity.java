package remix.myplayer.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.adapter.ChildHolderAdapter;
import remix.myplayer.fragment.BottomActionBarFragment;
import remix.myplayer.helper.UpdateHelper;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.interfaces.SortChangeCallback;
import remix.myplayer.model.mp3.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.customview.fastcroll_recyclerview.FastScrollRecyclerView;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.PlayListUtil;

/**
 * Created by Remix on 2015/12/4.
 */

/**
 * 专辑、艺术家、文件夹、播放列表详情
 */
public class ChildHolderActivity extends MultiChoiceActivity implements UpdateHelper.Callback{
    public final static String TAG = ChildHolderActivity.class.getSimpleName();
    public final static String TAG_PLAYLIST_SONG = ChildHolderActivity.class.getSimpleName() + "Song";
    private boolean mIsRunning = false;
    //获得歌曲信息列表的参数
    public static int mId;
    private int mType;
    private String mArg;
    private ArrayList<MP3Item> mInfoList;

    //歌曲数目与标题
    @BindView(R.id.childholder_item_num)
    TextView mNum;
    @BindView(R.id.child_holder_recyclerView)
    FastScrollRecyclerView mRecyclerView;
    @BindView(R.id.toolbar)
    Toolbar mToolBar;

    private String Title;
    private BottomActionBarFragment mBottombar;

    private ChildHolderAdapter mAdapter;
    private static ChildHolderActivity mInstance = null;
    private MaterialDialog mMDDialog;

    //更新
    private static final int START = 0;
    private static final int END = 1;
    private Handler mRefreshHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case Constants.CLEAR_MULTI:
                    mMultiChoice.clearSelectedViews();
                    break;
                case Constants.UPDATE_ADAPTER:
                    if(mInfoList == null)
                        return;
                    mAdapter.setList(mInfoList);
                    mNum.setText(getString(R.string.song_count,mInfoList.size()));
                    break;
                case START:
                    if(mMDDialog != null && !mMDDialog.isShowing()){
                        mMDDialog.show();
                    }
                    break;
                case END:
                    if(mMDDialog != null && mMDDialog.isShowing()){
                        mMDDialog.dismiss();
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_holder);
        ButterKnife.bind(this);

        mInstance = this;

        //参数id，类型，标题
        mId = getIntent().getIntExtra("Id", -1);
        mType = getIntent().getIntExtra("Type", -1);
        mArg = getIntent().getStringExtra("Title");

        mAdapter = new ChildHolderAdapter(this,mType,mArg,mMultiChoice);
        mAdapter.setCallback(new SortChangeCallback() {
            @Override
            public void SortChange() {
                new GetSongList(false).start();
            }
        });
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if(position < 0 || mInfoList == null || position >= mInfoList.size())
                    return;
                int songid = mInfoList.get(position).getId();
                if( !mMultiChoice.itemAddorRemoveWithClick(view,position,songid,mType == Constants.PLAYLISTSONG ? TAG_PLAYLIST_SONG : TAG)){
                    if (mInfoList != null && mInfoList.size() == 0)
                        return;
                    ArrayList<Integer> idList = new ArrayList<>();
                    for (MP3Item info : mInfoList) {
                        if(info != null && info.getId() > 0)
                            idList.add(info.getId());
                    }
                    //设置正在播放列表
                    Intent intent = new Intent(Constants.CTL_ACTION);
                    Bundle arg = new Bundle();
                    arg.putInt("Control", Constants.PLAYSELECTEDSONG);
                    arg.putInt("Position", position);
                    intent.putExtras(arg);
                    Global.setPlayQueue(idList,ChildHolderActivity.this,intent);
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                int songid = mInfoList.get(position).getId();
                mMultiChoice.itemAddorRemoveWithLongClick(view,position,songid, TAG,mType == Constants.PLAYLIST ? Constants.PLAYLISTSONG : Constants.SONG);
            }
        });

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setBubbleTextColor(ThemeStore.isLightTheme()
                ? ColorUtil.getColor(R.color.white)
                : ThemeStore.getTextColorPrimary());

        //歌曲数目与标题
        if(mType != Constants.FOLDER) {
            if(mArg.contains("unknown")){
                if(mType == Constants.ARTIST)
                    Title = getString(R.string.unknow_artist);
                else if(mType == Constants.ALBUM){
                    Title = getString(R.string.unknow_album);
                }
            } else {
                Title = mArg;
            }
        } else
            Title = mArg.substring(mArg.lastIndexOf("/") + 1,mArg.length());
        //初始化toolbar
        setUpToolbar(mToolBar,Title);

        //加载歌曲列表
        mMDDialog = new MaterialDialog.Builder(this)
                .title("加载中")
                .titleColorAttr(R.attr.text_color_primary)
                .content("请稍等")
                .contentColorAttr(R.attr.text_color_primary)
                .progress(true, 0)
                .backgroundColorAttr(R.attr.background_color_3)
                .progressIndeterminateStyle(false).build();

        //读取歌曲列表
        new GetSongList().start();
        //初始化底部状态栏
        mBottombar = (BottomActionBarFragment) getSupportFragmentManager().findFragmentById(R.id.bottom_actionbar_new);
        if(Global.PlayQueue == null || Global.PlayQueue.size() == 0)
            return;
        mBottombar.UpdateBottomStatus(MusicService.getCurrentMP3(), MusicService.isPlay());

    }

    @Override
    public void onBackPressed() {
        if(mMultiChoice.isShow()) {
            onBackPress();
        } else {
            finish();
        }
    }

    class GetSongList extends Thread{
        //是否需要重新查询歌曲列表
        private boolean mNeedReset = true;
        GetSongList(boolean needReset) {
            this.mNeedReset = needReset;
        }
        GetSongList(){}

        @Override
        public void run() {
            mRefreshHandler.sendEmptyMessage(START);
            if(mNeedReset)
                mInfoList = getMP3List();
            sortList();
            mRefreshHandler.sendEmptyMessage(END);
            mRefreshHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER);
        }
    }

    /**
     * 根据条件排序
     */
    private void sortList(){
        Collections.sort(mInfoList, new Comparator<MP3Item>() {
            @Override
            public int compare(MP3Item o1, MP3Item o2) {
                if(o1 == null || o2 == null)
                    return 0;
                //当前是按名字排序
                if(ChildHolderAdapter.SORT == ChildHolderAdapter.NAME){
                    if(TextUtils.isEmpty(o1.getTitleKey()) || TextUtils.isEmpty(o2.getTitleKey()))
                        return 0;
                    if(ChildHolderAdapter.ASC_DESC == ChildHolderAdapter.ASC){
                        return o1.getTitleKey().compareTo(o2.getTitleKey());
                    } else {
                        return o2.getTitleKey().compareTo(o1.getTitleKey());
                    }
                } else if(ChildHolderAdapter.SORT == ChildHolderAdapter.ADDTIME){
                    //当前是按添加时间排序
                    if(o1.getAddTime() == o2.getAddTime()){
                        return 0;
                    }
                    if(ChildHolderAdapter.ASC_DESC == ChildHolderAdapter.ASC){
                        return Long.valueOf(o1.getAddTime()).compareTo(o2.getAddTime());
                    } else {
                        return Long.valueOf(o2.getAddTime()).compareTo(o1.getAddTime());
                    }
                } else {
                    return 0;
                }
            }
        });
    }

    /**
     * 根据参数(专辑id 歌手id 文件夹名 播放列表名)获得对应的歌曲信息列表
     * @return 对应歌曲信息列表
     */
    private ArrayList<MP3Item> getMP3List(){
        if(mId < 0)
            return  null;
        mInfoList = new ArrayList<>();
        switch (mType) {
            //专辑id
            case Constants.ALBUM:
                mInfoList = MediaStoreUtil.getMP3InfoByArg(mId, Constants.ALBUM);
                break;
            //歌手id
            case Constants.ARTIST:
                mInfoList = MediaStoreUtil.getMP3InfoByArg(mId, Constants.ARTIST);
                break;
            //文件夹名
            case Constants.FOLDER:
//                mInfoList = MediaStoreUtil.getMP3ListByIds(Global.FolderMap.get(mArg));
                mInfoList = MediaStoreUtil.getMP3ListByFolderName(mArg);
                break;
            //播放列表名
            case Constants.PLAYLIST:
                /* 播放列表歌曲id列表 */
                ArrayList<Integer> playListSongIDList = PlayListUtil.getIDList(mId);
                if(playListSongIDList == null)
                    return mInfoList;
                mInfoList = PlayListUtil.getMP3ListByIds(playListSongIDList);
                break;
        }
        return mInfoList;
    }

    //更新界面
    @Override
    public void UpdateUI(MP3Item MP3Item, boolean isplay) {
        //底部状态兰
        mBottombar.UpdateBottomStatus(MP3Item, isplay);
        //更新高亮歌曲
//        if(SPUtil.getValue(mContext,"Setting","ShowHighLight",false))
//        if(mAdapter != null)
//            mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        MobclickAgent.onPageEnd(ChildHolderActivity.class.getSimpleName());
        super.onPause();
        if(mMultiChoice.isShow()){
            mRefreshHandler.sendEmptyMessageDelayed(Constants.CLEAR_MULTI,500);
        }
    }

    @Override
    protected void onResume() {
        MobclickAgent.onPageStart(ChildHolderActivity.class.getSimpleName());
        super.onResume();
        mIsRunning = true;

    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsRunning = false;
    }

    public void updateList() {
        if(mIsRunning)
            new GetSongList().start();
    }

    public static ChildHolderActivity getInstance(){
        return mInstance;
    }

}