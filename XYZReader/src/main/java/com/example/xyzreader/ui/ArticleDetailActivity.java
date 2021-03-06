package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.app.SharedElementCallback;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

import java.util.List;
import java.util.Map;

import static com.example.xyzreader.ui.ArticleListActivity.EXTRA_CURRENT_ARTICLE_POSITION;
import static com.example.xyzreader.ui.ArticleListActivity.EXTRA_STARTING_ARTICLE_POSITION;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public final static String ARTICLE_ID = "ARTICLE_ID";

    private Cursor mCursor;
    private long mBeginID;


    ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;
    private ArticleDetailFragment mCurrentDetailsFragment;
    private boolean mIsReturning;
    private int mCurrentPosition;
    private int mStartingPosition;

    @SuppressWarnings("NewApi")
    private final SharedElementCallback mCallback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (mIsReturning) {

                ImageView sharedElement = mCurrentDetailsFragment.getAlbumImage();
                if (sharedElement == null) {
                    // If shared element is null, then it has been scrolled off screen and
                    // no longer visible. In this case we cancel the shared element transition by
                    // removing the shared element from the shared elements map.
                    names.clear();
                    sharedElements.clear();
                } else if (mStartingPosition != mCurrentPosition) {
                    // If the user has swiped to a different ViewPager page, then we need to
                    // remove the old shared element and replace it with the new shared element
                    // that should be transitioned instead.
                    names.clear();
                    names.add(sharedElement.getTransitionName());
                    sharedElements.clear();
                    sharedElements.put(sharedElement.getTransitionName(), sharedElement);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

            postponeEnterTransition();
            setEnterSharedElementCallback(mCallback);
        }

        mStartingPosition = getIntent().getIntExtra(EXTRA_STARTING_ARTICLE_POSITION, 0);
        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mBeginID = ItemsContract.Items.getItemId(getIntent().getData());
            }
            mCurrentPosition = mStartingPosition;
        } else {
            mBeginID = savedInstanceState.getLong(ARTICLE_ID);
            mCurrentPosition = savedInstanceState.getInt(EXTRA_CURRENT_ARTICLE_POSITION);
        }

        getLoaderManager().initLoader(0, null, this);

        mPager = findViewById(R.id.viewPager);


        mPagerAdapter = new MyPagerAdapter(getFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(mCurrentPosition);
        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                    mCurrentPosition = position;
                }
            }
        });
        mPager.setPageTransformer(false, new Transformer());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ARTICLE_ID, mBeginID);
        outState.putInt(EXTRA_CURRENT_ARTICLE_POSITION, mCurrentPosition);
    }

    @Override
    public void finishAfterTransition() {
        mIsReturning = true;
        Intent data = new Intent();
        data.putExtra(EXTRA_STARTING_ARTICLE_POSITION, mStartingPosition);
        data.putExtra(EXTRA_CURRENT_ARTICLE_POSITION, mCurrentPosition);
        setResult(RESULT_OK, data);
        super.finishAfterTransition();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        // Select the start ID
        if (mBeginID > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                if (cursor.getLong(ArticleLoader.Query._ID) == mBeginID) {
                    final int position = cursor.getPosition();
                    mCursor = cursor;
                    mPagerAdapter.notifyDataSetChanged();
                    mPager.setCurrentItem(position, false);
                    break;
                }
                cursor.moveToNext();
            }
        }
    }


    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            mCurrentDetailsFragment = (ArticleDetailFragment) object;
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID), position, mStartingPosition);
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }


    public class Transformer implements ViewPager.PageTransformer {
        @Override
        public void transformPage(@NonNull View page, float position) {
            if (position <= -1 || position >= 1) {
//                page.setAlpha(0.0F);
            } else if (position == 0.0f) {
//                page.setAlpha(1);
            } else {
                page.findViewById(R.id.thumbnail)
                        .setTranslationX(-position * page.getWidth() / 2);

                page.findViewById(R.id.artTitle)
                        .setTranslationX(-position * page.getWidth() / 2);
                page.findViewById(R.id.artTitle)
                        .setAlpha(1.0f - Math.abs(position));

                page.findViewById(R.id.artByline)
                        .setTranslationX(-position * page.getWidth() / 2);
                page.findViewById(R.id.artByline)
                        .setAlpha(1.0f - Math.abs(position));

                page.findViewById(R.id.share_fab)
                        .setRotation(-position * page.getWidth() / 2);

//                page.setAlpha(1.0f - Math.abs(position));
            }
        }
    }

}
