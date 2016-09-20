package remix.myplayer.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.listener.OnItemClickListener;
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.customview.ColumnView;
import remix.myplayer.ui.dialog.OptionDialog;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;

/**
 * Created by taeja on 16-6-24.
 */
public class ChildHolderAdapter extends RecyclerView.Adapter<ChildHolderAdapter.ViewHoler> {
    private ArrayList<MP3Item> mInfoList;
    private Context mContext;
    private int mType;
    private String mArg;
    private OnItemClickListener mOnItemClickLitener;
    public ChildHolderAdapter(Context context, int type, String arg){
        this.mContext = context;
        this.mType = type;
        this.mArg = arg;
    }

    public void setList(ArrayList<MP3Item> list){
        mInfoList = list;
        notifyDataSetChanged();
    }

    public void setOnItemClickLitener(OnItemClickListener l)
    {
        this.mOnItemClickLitener = l;
    }


    @Override
    public ViewHoler onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHoler(LayoutInflater.from(mContext).inflate(R.layout.child_holder_item,null,false));
    }

    @Override
    public void onBindViewHolder(final ViewHoler holder, int position) {
        final MP3Item temp = mInfoList.get(position);
        if(temp == null)
            return;

        //获得正在播放的歌曲
        final MP3Item currentMP3 = MusicService.getCurrentMP3();
        //判断该歌曲是否是正在播放的歌曲
        //如果是,高亮该歌曲，并显示动画
        if(currentMP3 != null){
            boolean highlight = temp.getId() == currentMP3.getId();
            holder.mTitle.setTextColor(highlight ?
                    ColorUtil.getColor(ThemeStore.isDay() ? ThemeStore.MATERIAL_COLOR_PRIMARY : R.color.purple_782899) :
                    ColorUtil.getColor(ThemeStore.isDay() ? R.color.day_textcolor_primary : R.color.night_textcolor_primary));
            holder.mColumnView.setVisibility(highlight ? View.VISIBLE : View.GONE);

            //根据当前播放状态以及动画是否在播放，开启或者暂停的高亮动画
            if(MusicService.getIsplay() && !holder.mColumnView.getStatus() && highlight){
                holder.mColumnView.startAnim();
            }
            else if(!MusicService.getIsplay() && holder.mColumnView.getStatus()){
                holder.mColumnView.stopAnim();
            }
        }

        //设置标题
        holder.mTitle.setText(CommonUtil.processInfo(temp.getTitle(),CommonUtil.SONGTYPE));

        if(holder.mButton != null) {
            //设置按钮着色
            int tintColor = ThemeStore.THEME_MODE == ThemeStore.DAY ? ColorUtil.getColor(R.color.gray_6c6a6c) : Color.WHITE;
            Theme.TintDrawable(holder.mButton,R.drawable.list_icn_more,tintColor);
            holder.mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext, OptionDialog.class);
                    intent.putExtra("MP3Item", temp);
                    if (mType == Constants.PLAYLIST_HOLDER) {
                        intent.putExtra("IsDeletePlayList", true);
                        intent.putExtra("PlayListName", mArg);
                    }
                    mContext.startActivity(intent);
                }
            });
        }

        if(holder.mContainer != null && mOnItemClickLitener != null){
            holder.mContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemClickLitener.onItemClick(v,holder.getAdapterPosition());
                }
            });
            holder.mContainer.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mOnItemClickLitener.onItemLongClick(v,holder.getAdapterPosition());
                    return true;
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mInfoList == null ? 0 : mInfoList.size();
    }

    public static class ViewHoler extends BaseViewHolder {
        @BindView(R.id.album_holder_item_title)
        public TextView mTitle;
        @BindView(R.id.song_item_button)
        public ImageButton mButton;
        @BindView(R.id.song_columnview)
        public ColumnView mColumnView;
        public View mContainer;
        public ViewHoler(View itemView) {
            super(itemView);
            mContainer = itemView;
        }
    }
}
