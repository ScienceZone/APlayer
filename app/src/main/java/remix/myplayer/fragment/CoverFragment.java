package remix.myplayer.fragment;

import android.content.ContentUris;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringSystem;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.interfaces.OnInflateFinishListener;
import remix.myplayer.model.mp3.MP3Item;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.thumb.SearchCover;

/**
 * Created by Remix on 2015/12/2.
 */

/**
 * 专辑封面Fragment
 *
 */
public class CoverFragment extends BaseFragment {
    @BindView(R.id.cover_image)
    SimpleDraweeView mImage;
    @BindView(R.id.cover_shadow)
    ImageView mShadow;
    @BindView(R.id.cover_container)
    View mCoverContainer;
    private MP3Item mInfo;
    private int mWidth;
    private Uri mUri = Uri.EMPTY;
    private OnInflateFinishListener mInflateFinishListener;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageName = CoverFragment.class.getSimpleName();
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInfo = (MP3Item)getArguments().getSerializable("MP3Item");
        mWidth = getArguments().getInt("Width");
        View rootView = inflater.inflate(R.layout.fragment_cover,container,false);
        mUnBinder = ButterKnife.bind(this,rootView);
        mImage.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mImage.getViewTreeObserver().removeOnPreDrawListener(this);
                if(mInflateFinishListener != null)
                    mInflateFinishListener.onViewInflateFinish(mImage);
                return true;
            }
        });
        mImage.setAspectRatio(1);
        mImage.getHierarchy().setFailureImage(ThemeStore.isDay() ? R.drawable.album_empty_bg_day : R.drawable.album_empty_bg_night);

        return rootView;
    }

    public void setInflateFinishListener(OnInflateFinishListener l){
        mInflateFinishListener = l;
    }

    /**
     * 操作为上一首歌曲时，显示往左侧消失的动画
     *       下一首歌曲时，显示往右侧消失的动画
     * @param info 需要更新的歌曲
     * @param withAnim 是否需要动画
     */
    public void UpdateCover(MP3Item info,Uri uri, boolean withAnim){
        if(!isAdded())
            return;
        if (mImage == null || (mInfo = info) == null)
            return;

        mUri = uri;

        if(withAnim){
            int operation = Global.getOperation();

            int offsetX = (mWidth +  mImage.getWidth()) >> 1;
            final double startValue = 0;
            final double endValue = operation == Constants.PREV ? offsetX : -offsetX;

            //封面移动动画
            final Spring outAnim = SpringSystem.create().createSpring();
            outAnim.addListener(new SimpleSpringListener(){
                @Override
                public void onSpringUpdate(Spring spring) {
                    if(mCoverContainer == null || spring == null)
                        return;
                    mCoverContainer.setTranslationX((float) spring.getCurrentValue());
                }
                @Override
                public void onSpringAtRest(Spring spring) {
                    //显示封面的动画
                    if(mImage == null || spring == null)
                        return;
                    mCoverContainer.setTranslationX((float) startValue);
                    mImage.setImageURI(mUri);
                    final Spring inAnim = SpringSystem.create().createSpring();
                    inAnim.addListener(new SimpleSpringListener(){
                        @Override
                        public void onSpringUpdate(Spring spring) {
                            if(mImage == null || spring == null)
                                return;
                            mCoverContainer.setScaleX((float) spring.getCurrentValue());
                            mCoverContainer.setScaleY((float) spring.getCurrentValue());
                        }
                    });
                    inAnim.setCurrentValue(0.85);
                    inAnim.setEndValue(1);
                }
            });
            outAnim.setOvershootClampingEnabled(true);
            outAnim.setCurrentValue(startValue);
            outAnim.setEndValue(endValue);
        } else {
            mImage.setImageURI(mUri);
            mShadow.setVisibility(ThemeStore.isDay() ? View.VISIBLE : View.INVISIBLE);
        }
    }

    /**
     * activity退出时隐藏封面
     */
    public void hideImage(){
        if(mCoverContainer != null)
            mCoverContainer.setVisibility(View.INVISIBLE);
    }

    /**
     * activity入场动画播放完毕时显示封面
     */
    public void showImage(){
        if(mImage != null)
            mImage.setVisibility(View.VISIBLE);
        if(mCoverContainer != null)
            mCoverContainer.setVisibility(View.VISIBLE);
    }

    /**
     * 判断该专辑id在本地数据库是否有封面
     * 没有则查找缓存目录是否有下载
     * 没有则下载
     */
    private void setImage(){
        try {
            final Message msg = new Message();
            Uri uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), mInfo.getAlbumId());

            boolean exist = CommonUtil.isAlbumThumbExistInDB(uri);
            if(false){
                mImage.setImageURI(uri);
            } else {
                //查找缓存目录是否有，没有则去下载
                mImage.setImageURI(Uri.parse(new SearchCover(mInfo.getDisplayname(),mInfo.getArtist(),SearchCover.COVER).getImgUrl()));
//                String coverCahcePath = CommonUtil.getCoverInCache(mInfo.getAlbumId());
//                if(coverCahcePath != null && !coverCahcePath.equals("")){
////                    msg.obj = Uri.parse("file://" + coverCahcePath);
////                    mHandler.sendMessage(msg);
//                    mImage.setImageURI(Uri.parse("file://" + coverCahcePath));
//                    return;
//                }
//                //下载
//                new Thread(){
//                    @Override
//                    public void run() {
//                        String coverPath = CommonUtil.downAlbumCover(mInfo.getDisplayname(),mInfo.getArtist(),mInfo.getAlbumId());
//                        msg.obj = Uri.parse("file://" + coverPath);
//                        mHandler.sendMessage(msg);
//                    }
//                }.startListen();

            }
        } catch (Exception e){
            e.printStackTrace();
        }


    }

}
