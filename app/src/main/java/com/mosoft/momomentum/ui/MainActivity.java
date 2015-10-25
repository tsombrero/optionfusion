package com.mosoft.momomentum.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.SharedElementCallback;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.transition.TransitionInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.mosoft.momomentum.R;
import com.mosoft.momomentum.model.Spread;
import com.mosoft.momomentum.model.provider.amtd.OptionChain;
import com.mosoft.momomentum.ui.results.ResultsFragment;
import com.mosoft.momomentum.ui.search.SearchFragment;
import com.mosoft.momomentum.ui.tradedetails.TradeDetailsFragment;

import java.util.List;
import java.util.Map;

public class MainActivity extends Activity implements SearchFragment.Host, ResultsFragment.Host {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Fragment frag = SearchFragment.newInstance();
        getFragmentManager().beginTransaction()
                .add(R.id.fragment_container, frag, "tag_search")
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void openResultsFragment(OptionChain optionChain) {
        Fragment fragment = ResultsFragment.newInstance(optionChain.getSymbol());
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, optionChain.getSymbol())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void showDetails(Spread spread, Fragment requestingFragment, View headerLayout, View detailsLayout, View stockInfoLayout) {

        requestingFragment.setSharedElementReturnTransition(TransitionInflater.from(this).inflateTransition(R.transition.change_transform));
        requestingFragment.setExitTransition(TransitionInflater.from(this).inflateTransition(android.R.transition.fade));
        requestingFragment.setEnterTransition(TransitionInflater.from(this).inflateTransition(android.R.transition.fade));

        Fragment fragment = TradeDetailsFragment.newInstance(spread);
        fragment.setSharedElementEnterTransition(TransitionInflater.from(this).inflateTransition(R.transition.change_transform));
        fragment.setEnterTransition(TransitionInflater.from(this).inflateTransition(android.R.transition.fade));
        fragment.setExitTransition(TransitionInflater.from(this).inflateTransition(android.R.transition.fade));

        fragment.setEnterSharedElementCallback(new MySharedElementCallback());

        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, spread.toString())
                .addSharedElement(detailsLayout, SharedViewHolders.BriefTradeDetailsHolder.getTransitionName(spread))
                .addSharedElement(headerLayout, SharedViewHolders.TradeDetailsHeaderHolder.getTransitionName(spread))
                .addSharedElement(stockInfoLayout, SharedViewHolders.StockInfoHolder.getTransitionName(spread.getUnderlyingSymbol()))
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 1) {
            getFragmentManager().popBackStack();
        } else {
            finish();
        }
    }

    public class MySharedElementCallback extends SharedElementCallback {
        @Override
        public void onSharedElementStart(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {
            super.onSharedElementStart(sharedElementNames, sharedElements, sharedElementSnapshots);
        }

        @Override
        public void onSharedElementEnd(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {
            super.onSharedElementEnd(sharedElementNames, sharedElements, sharedElementSnapshots);
        }

        @Override
        public void onRejectSharedElements(List<View> rejectedSharedElements) {
            super.onRejectSharedElements(rejectedSharedElements);
        }

        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            super.onMapSharedElements(names, sharedElements);
        }

        @Override
        public Parcelable onCaptureSharedElementSnapshot(View sharedElement, Matrix viewToGlobalMatrix, RectF screenBounds) {
            return super.onCaptureSharedElementSnapshot(sharedElement, viewToGlobalMatrix, screenBounds);
        }

        @Override
        public View onCreateSnapshotView(Context context, Parcelable snapshot) {
            return super.onCreateSnapshotView(context, snapshot);
        }

        @Override
        public void onSharedElementsArrived(List<String> sharedElementNames, List<View> sharedElements, OnSharedElementsReadyListener listener) {
            super.onSharedElementsArrived(sharedElementNames, sharedElements, listener);
        }
    }
}
