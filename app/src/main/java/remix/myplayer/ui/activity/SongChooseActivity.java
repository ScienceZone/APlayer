package remix.myplayer.ui.activity;

import android.content.CursorLoader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.adapter.SongChooseAdaper;
import remix.myplayer.interfaces.OnSongChooseListener;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.ToastUtil;

/**
 * @ClassName SongChooseActivity
 * @Description 新建列表后添加歌曲
 * @Author Xiaoborui
 * @Date 2016/10/21 09:34
 */

public class SongChooseActivity extends BaseActivity implements android.app.LoaderManager.LoaderCallbacks<Cursor> {
    public static final String TAG = SongChooseActivity.class.getSimpleName();

    private int mPlayListID;
    private String mPlayListName;
    Cursor mCursor = null;
    //索引
    public static int mArtistIdIndex = -1;
    public static int mArtistIndex = -1;
    public static int mAlbumIdIndex = -1;
    public static int mTitleIndex = -1;
    public static int mSongIdIndex = -1;
    public static int mDisPlayNameIndex = -1;
    private static int LOADER_ID = 1;
    @BindView(R.id.confirm)
    TextView mConfirm;
    @BindView(R.id.recyclerview)
    RecyclerView mRecyclerView;
    private SongChooseAdaper mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_choose);
        ButterKnife.bind(this);

        mPlayListID = getIntent().getIntExtra("PlayListID",-1);
        if(mPlayListID <= 0){
            ToastUtil.show(this, R.string.add_error, Toast.LENGTH_SHORT);
            return;
        }
        mPlayListName = getIntent().getStringExtra("PlayListName");

        TextView cancel = findView(R.id.cancel);
        cancel.setTextColor(ColorUtil.getColor(ThemeStore.isLightTheme() ? R.color.day_textcolor_primary : R.color.night_textcolor_primary));
        mConfirm.setTextColor(ColorUtil.getColor(ThemeStore.isLightTheme() ? R.color.day_textcolor_primary : R.color.night_textcolor_primary));
        mAdapter = new SongChooseAdaper(this, new OnSongChooseListener() {
            @Override
            public void OnSongChoose(boolean isValid) {
                mConfirm.setAlpha(isValid ? 1.0f : 0.6f);
                mConfirm.setClickable(isValid);
            }
        });
        getLoaderManager().initLoader(++LOADER_ID, null, this);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mConfirm.setAlpha(0.6f);
    }

    @OnClick({R.id.confirm,R.id.cancel})
    public void onClick(View v){
        switch (v.getId()){
            case R.id.cancel:
                finish();
                break;
            case R.id.confirm:
                if(mAdapter.getCheckedSong() == null || mAdapter.getCheckedSong().size() == 0){
                    ToastUtil.show(this,R.string.choose_no_song);
                    return;
                }
                final int num = PlayListUtil.addMultiSongs(mAdapter.getCheckedSong(),mPlayListName,mPlayListID);
                ToastUtil.show(this,getString(R.string.add_song_playlist_success, num,mPlayListName));
                finish();
        }
    }

    @Override
    public android.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        //查询所有歌曲
        return  new CursorLoader(this,
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Media._ID,MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.ARTIST_ID, MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.DISPLAY_NAME,MediaStore.Audio.Media.ALBUM_ID},
                MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE + MediaStoreUtil.getBaseSelection(),null,MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
    }

    @Override
    public void onLoadFinished(android.content.Loader<Cursor> loader, Cursor data) {
        if(data == null)
            return;
        //保存查询结果，并设置查询索引
        mCursor = data;
        mTitleIndex = data.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
        mArtistIndex = data.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
        mArtistIdIndex = data.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID);
        mAlbumIdIndex = data.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
        mSongIdIndex = data.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
        mDisPlayNameIndex = data.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
        mAdapter.setCursor(mCursor);
    }

    @Override
    public void onLoaderReset(android.content.Loader<Cursor> loader) {
        if(mAdapter != null)
            mAdapter.setCursor(null);
    }
}
