package com.trinew.easytime.activities;

import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.trinew.easytime.R;
import com.trinew.easytime.adapters.pagers.GraphPagerAdapter;
import com.trinew.easytime.fragments.EasyBoxFragment;
import com.trinew.easytime.fragments.dialogs.EditStampDialogFragment;
import com.trinew.easytime.models.ParseStamp;
import com.trinew.easytime.modules.ProfileBuilder;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


/**
 * The Activity MainActivity is the Main screen of the app. It simply shows a
 * dummy image with some filter options. You need to write actual code image
 * processing and filtering.
 *
 */
public class GraphActivity extends AppCompatActivity implements EditStampDialogFragment.OnEditStampInteractionListener,
        EasyBoxFragment.OnBoxInteractionListener {

    private GraphPagerAdapter mPagerAdapter;
    private ViewPager mViewPager;

    private ParseStamp editStamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        // Set up toolbar_
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_action_arrow_left);

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(R.string.page_title_graph);
        }

        // init pager
        mPagerAdapter = new GraphPagerAdapter(this, getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setOffscreenPageLimit(GraphPagerAdapter.NUM_PAGES);

        final PagerSlidingTabStrip tabStrip = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        tabStrip.setViewPager(mViewPager);

        tabStrip.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.anim_move_in_left, R.anim.anim_move_out_right);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                overridePendingTransition(R.anim.anim_move_in_left, R.anim.anim_move_out_right);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    // edit stamp dialog overrides
    @Override
    public void onEditStampDelete() {
        if(editStamp != null) {
            ParseUser currentUser = ParseUser.getCurrentUser();
            List<ParseStamp> stamps = currentUser.getList(ProfileBuilder.PROFILE_KEY_STAMPS);
            stamps.remove(editStamp);
            currentUser.put(ProfileBuilder.PROFILE_KEY_STAMPS, stamps);
            currentUser.saveEventually(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if(e != null) {
                        Toast.makeText(GraphActivity.this, "Deleting failed, please try again", Toast.LENGTH_LONG).show();
                        return;
                    }

                    mPagerAdapter.refreshAllData();
                }
            });

            editStamp.deleteEventually();
        } else {
            Toast.makeText(this, "Deleting failed, please try again", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onEditStampFinish(int dialogIntent, int editFlag, Date editDate, String editComment) {
        ParseUser currentUser = ParseUser.getCurrentUser();
        List<ParseStamp> stamps = currentUser.getList(ProfileBuilder.PROFILE_KEY_STAMPS);

        if(stamps == null)
            return;

        switch (dialogIntent) {
            case EditStampDialogFragment.DIALOG_INTENT_ADD:
                final ParseStamp newStamp = new ParseStamp();
                newStamp.setFlag(editFlag);
                newStamp.setLogDate(editDate);
                newStamp.setComment(editComment);

                newStamp.saveEventually();

                stamps.add(newStamp);

                // we resort the list after adding the stamp to maintain a constantly sorted list
                Collections.sort(stamps, new Comparator<ParseStamp>() {
                    @Override
                    public int compare(ParseStamp t0, ParseStamp t1) {
                        return (int) (t0.getLogDate().getTime() - t1.getLogDate().getTime());
                    }
                });

                currentUser.put(ProfileBuilder.PROFILE_KEY_STAMPS, stamps);
                // TODO: save the stamp ONLY then fake the stamp being added to the list (only show it)
                // this way even if the stamp pointers arent saved in time then it doesnt matter
                currentUser.saveEventually(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if(e != null) {
                            Toast.makeText(GraphActivity.this, "There was a problem saving your stamp!", Toast.LENGTH_LONG).show();
                            return;
                        }

                        mPagerAdapter.refreshAllData();
                    }
                });

                break;
            case EditStampDialogFragment.DIALOG_INTENT_EDIT:
                // stamp must be dirty to save
                if(editStamp != null) {
                    if (editStamp.getFlag() != editFlag || !editStamp.getLogDate().equals(editDate) || !editStamp.getComment().equals(editComment)) {

                        editStamp.setFlag(editFlag);
                        editStamp.setLogDate(editDate);
                        editStamp.setComment(editComment);

                        editStamp.saveEventually();

                        Collections.sort(stamps, new Comparator<ParseStamp>() {
                            @Override
                            public int compare(ParseStamp t0, ParseStamp t1) {
                                return (int) (t0.getLogDate().getTime() - t1.getLogDate().getTime());
                            }
                        });

                        currentUser.put(ProfileBuilder.PROFILE_KEY_STAMPS, stamps);
                        // TODO: save the stamp ONLY then fake the stamp being added to the list (only show it)
                        // this way even if the stamp pointers arent saved in time then it doesnt matter
                        currentUser.saveEventually(new SaveCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e != null) {
                                    Toast.makeText(GraphActivity.this, "There was a problem saving your stamp!", Toast.LENGTH_LONG).show();
                                    return;
                                }

                                mPagerAdapter.refreshAllData();
                            }
                        });
                    }
                } else {
                    Toast.makeText(this, "Editing failed, please try again", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    // box listener overrides
    @Override
    public void onRequestAddStamp() {
        FragmentManager fragmentManager = getFragmentManager();
        EditStampDialogFragment editStampDialog = EditStampDialogFragment.newInstance();
        editStampDialog.show(fragmentManager, "EditStampDialogFragment");
    }

    @Override
    public void onRequestEditStamp(ParseStamp stamp) {
        editStamp = stamp;

        FragmentManager fragmentManager = getFragmentManager();
        EditStampDialogFragment editStampDialog = EditStampDialogFragment.newInstance(EditStampDialogFragment.DIALOG_INTENT_EDIT, stamp.getFlag(), stamp.getLogDate(), stamp.getComment());
        editStampDialog.show(fragmentManager, "EditStampDialogFragment");
    }
}