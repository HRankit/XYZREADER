package com.example.xyzreader.ui;

import android.app.LoaderManager;
import android.app.SharedElementCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.transition.TransitionManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Toast;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.UpdaterService;


import java.util.List;
import java.util.Map;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, SwipeRefreshLayout.OnRefreshListener {

    private static String LOG_TAG = ArticleListActivity.class.getSimpleName();


    Toolbar mToolbar;
    SwipeRefreshLayout mSwipeRefreshLayout;
    RecyclerView mRecyclerView;

    static final String EXTRA_STARTING_ARTICLE_POSITION = "extra_starting_item_position";
    static final String EXTRA_CURRENT_ARTICLE_POSITION = "extra_current_item_position";

    private Bundle mTmpReenterState;
    public boolean mIsRefreshing;
    public static boolean mIsDetailsActivityStarted;


    private SharedElementCallback mCallback;

    private boolean isItListLayout = false;
    private ArticleListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);


        mToolbar = findViewById(R.id.toolbar);
        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        mRecyclerView = findViewById(R.id.recycler_view);

        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mCallback = new SharedElementCallback() {
                @Override
                public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                    if (mTmpReenterState != null) {
                        int startingPosition = mTmpReenterState.getInt(EXTRA_STARTING_ARTICLE_POSITION);
                        int currentPosition = mTmpReenterState.getInt(EXTRA_CURRENT_ARTICLE_POSITION);
                        if (startingPosition != currentPosition) {
                            // If startingPosition != currentPosition the user must have swiped to a
                            // different page in the DetailsActivity. We must update the shared element
                            // so that the correct one falls into place.
                            String newTransitionName = getString(R.string.transitionImage) + currentPosition;
                            View newSharedElement = mRecyclerView.findViewWithTag(newTransitionName);
                            if (newSharedElement != null) {
                                names.clear();
                                names.add(newTransitionName);
                                sharedElements.clear();
                                sharedElements.put(newTransitionName, newSharedElement);
                            }
                        }

                        mTmpReenterState = null;
                    } else {
                        // If mTmpReenterState is null, then the activity is exiting.
                        View navigationBar = findViewById(android.R.id.navigationBarBackground);
                        View statusBar = findViewById(android.R.id.statusBarBackground);
                        if (navigationBar != null) {
                            names.add(navigationBar.getTransitionName());
                            sharedElements.put(navigationBar.getTransitionName(), navigationBar);
                        }
                        if (statusBar != null) {
                            names.add(statusBar.getTransitionName());
                            sharedElements.put(statusBar.getTransitionName(), statusBar);
                        }
                    }
                }
            };
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setExitSharedElementCallback(mCallback);
        }

        mSwipeRefreshLayout.setOnRefreshListener(this);

        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            startService(new Intent(this, UpdaterService.class));
        }

        IntentFilter filter = new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE);
        filter.addAction(UpdaterService.BROADCAST_ACTION_NO_CONNECTIVITY);
        LocalBroadcastManager.getInstance(this).registerReceiver(mRefreshingReceiver,
                filter);


    }


    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsDetailsActivityStarted = false;
    }

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
            } else if (UpdaterService.BROADCAST_ACTION_NO_CONNECTIVITY.equals(intent.getAction())) {
                mSwipeRefreshLayout.setRefreshing(false);
                Toast.makeText(ArticleListActivity.this, getString(R.string.noInternetAvailable), Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        adapter = new ArticleListAdapter(this, cursor);
        adapter.setHasStableIds(true);

        changeLayout();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    @Override
    public void onRefresh() {
        Log.i(LOG_TAG, "onRefresh called from SwipeRefreshLayout");

        startService(new Intent(this, UpdaterService.class));
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        return !isLandscape();
    }

    private boolean isLandscape() {
        return getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.refresh:
                changeLayout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void changeLayout(MenuItem item) {
        isItListLayout = !isItListLayout;

        Drawable drawbable;
        if (isItListLayout) {
            drawbable = getResources().getDrawable(R.drawable.ic_apps_black_24dp);
        } else {
            drawbable = getResources().getDrawable(R.drawable.ic_view_list_black_24dp);
        }


        item.setIcon(drawbable);

        changeLayout();
    }

    public static int calculateNoOfColumns(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        int scalingFactor = 200; // You can vary the value held by the scalingFactor
        // variable. The smaller it is the more no. of columns you can display, and the
        // larger the value the less no. of columns will be calculated. It is the scaling
        // factor to tweak to your needs.
        int columnCount = (int) (dpWidth / scalingFactor);
        return (columnCount >= 2 ? columnCount : 2); // if column no. is less than 2, we still display 2 columns
    }

    private void changeLayout() {
        if (!isItListLayout) {
            StaggeredGridLayoutManager _sGridLayoutManager = new StaggeredGridLayoutManager(calculateNoOfColumns(getApplication()),
                    StaggeredGridLayoutManager.VERTICAL);
            mRecyclerView.setLayoutManager(_sGridLayoutManager);
            TransitionManager.beginDelayedTransition(mRecyclerView);

        } else {
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            TransitionManager.beginDelayedTransition(mRecyclerView);
        }

        initRVAnimations();

        mRecyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

    }

    private void initRVAnimations() {
        int resId = R.anim.layout_animation_fall_down;
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(getApplication(), resId);
        mRecyclerView.setLayoutAnimation(animation);
    }

    @Override
    public void onActivityReenter(int requestCode, Intent data) {
        super.onActivityReenter(requestCode, data);
        mTmpReenterState = new Bundle(data.getExtras());
        int startingPosition = mTmpReenterState.getInt(EXTRA_STARTING_ARTICLE_POSITION);
        int currentPosition = mTmpReenterState.getInt(EXTRA_CURRENT_ARTICLE_POSITION);
        if (startingPosition != currentPosition) {
            mRecyclerView.scrollToPosition(currentPosition);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            postponeEnterTransition();
            mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                    mRecyclerView.requestLayout();
                    startPostponedEnterTransition();
                    return true;
                }
            });
        }
    }

    public static boolean getmIsDetailsActivityStarted() {
        return mIsDetailsActivityStarted;
    }
}
