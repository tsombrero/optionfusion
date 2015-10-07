package com.mosoft.momomentum.ui;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.mosoft.momomentum.R;
import com.mosoft.momomentum.model.amtd.OptionChain;
import com.mosoft.momomentum.ui.results.ResultsFragment;
import com.mosoft.momomentum.ui.search.SearchFragment;

public class MainActivity extends AppCompatActivity implements SearchFragment.FragmentHost {

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
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null)
                .commit();
    }
}
