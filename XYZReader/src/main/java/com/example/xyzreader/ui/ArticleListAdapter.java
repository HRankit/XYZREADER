package com.example.xyzreader.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import static com.example.xyzreader.ui.ArticleListActivity.EXTRA_STARTING_ARTICLE_POSITION;


public class ArticleListAdapter extends RecyclerView.Adapter<ArticleListAdapter.ViewHolder> {
    private Cursor mCursor;
    private Context mContext;
    private int mArticlePosition;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss", Locale.getDefault());
    // Use default locale format
    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);


    class ViewHolder extends RecyclerView.ViewHolder {
        private DynamicHeightNetworkImageView thumbnailView;
        private TextView titleTextView;
        private TextView subTextView;
        private TextView authorTextView;

        ViewHolder(View view) {
            super(view);

            this.thumbnailView = view.findViewById(R.id.thumbnail);
            this.titleTextView = view.findViewById(R.id.artTitle);
            this.subTextView = view.findViewById(R.id.artSubtitle);
            this.authorTextView = view.findViewById(R.id.artAuthor);


        }
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            return new Date();
        }
    }

    ArticleListAdapter(Context context, Cursor cursor) {
        mContext = context;
        mCursor = cursor;
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(ArticleLoader.Query._ID);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.list_item_article, parent, false);
        final ViewHolder vh = new ViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mArticlePosition = vh.getAdapterPosition();

                ActivityOptionsCompat bundle = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            (Activity) mContext,
                            vh.thumbnailView,
                            vh.thumbnailView.getTransitionName()
                    );
                }
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition())));
                intent.putExtra(EXTRA_STARTING_ARTICLE_POSITION, mArticlePosition);
                if (bundle != null) {
                    mContext.startActivity(intent, bundle.toBundle());
                } else {
                    mContext.startActivity(intent);

                }

            }
        });
        return vh;
    }


    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        mCursor.moveToPosition(position);
        mArticlePosition = position;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.thumbnailView.setTransitionName(mContext.getString(R.string.transitionImage) + position);
        }
        holder.thumbnailView.setTag(mContext.getString(R.string.transitionImage) + position);

        String title = mCursor.getString(ArticleLoader.Query.TITLE);


        Date publishedDate = parsePublishedDate();

        if (!publishedDate.before(START_OF_EPOCH.getTime())) {
            holder.subTextView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            publishedDate.getTime(),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()));

        } else {
            // If date is before 1902, just show the string
            holder.subTextView.setText(Html.fromHtml(
                    outputFormat.format(publishedDate) + "<br> by <font color='#ffffff'>"
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)
                            + "</font>"));
        }

        String author = mCursor.getString(ArticleLoader.Query.AUTHOR);
        String image = mCursor.getString(ArticleLoader.Query.THUMB_URL);
        holder.titleTextView.setText(title);
        holder.authorTextView.setText(author);
        holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));

        ImageLoader loader = ImageLoaderHelper.getInstance(mContext).getImageLoader();
        holder.thumbnailView.setImageUrl(image, loader);
        loader.get(image, new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                Bitmap bitmap = imageContainer.getBitmap();
                if (bitmap != null) {
                    Palette p = Palette.from(bitmap).generate();
                    int mMutedColor = p.getDarkMutedColor(0xFF424242);
                    holder.itemView.setBackgroundColor(mMutedColor);
                }
            }

            @Override
            public void onErrorResponse(VolleyError volleyError) {

            }
        });


    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }
}

