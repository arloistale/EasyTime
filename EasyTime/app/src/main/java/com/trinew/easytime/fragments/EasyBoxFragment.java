package com.trinew.easytime.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.trinew.easytime.R;
import com.trinew.easytime.adapters.StampCollectionAdapter;
import com.trinew.easytime.models.ParseStamp;
import com.trinew.easytime.modules.ProfileBuilder;
import com.trinew.easytime.modules.stamps.StampCollection;
import com.trinew.easytime.modules.stamps.StampCollectionBox;
import com.trinew.easytime.views.AnimatedExpandableListView;

import java.util.List;


public class EasyBoxFragment extends DataFragment {

    private RelativeLayout progressContainer;
    private RelativeLayout errorContainer;
    private TextView errorText;

    private FloatingActionButton floatingActionButton;

    private AnimatedExpandableListView listView;

    private StampCollectionAdapter stampCollectionAdapter;

    // listener
    private OnBoxInteractionListener onBoxInteractionListener;

    // data

    // remember the group index we've selected
    private int selectedGroupIndex = -1;

    public static EasyBoxFragment newInstance() {
        EasyBoxFragment fragment = new EasyBoxFragment();
        return fragment;
    }

    public EasyBoxFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_stamp_box, container, false);

        // init views
        progressContainer = (RelativeLayout) view.findViewById(R.id.progressContainer);
        errorContainer = (RelativeLayout) view.findViewById(R.id.errorContainer);
        errorText = (TextView) view.findViewById(R.id.errorText);

        floatingActionButton = (FloatingActionButton) view.findViewById(R.id.addButton);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBoxInteractionListener.onRequestAddStamp();
            }
        });

        listView = (AnimatedExpandableListView) view.findViewById(R.id.stampBox);

        // In order to show animations, we need to use a custom click handler
        // for our ExpandableListView.
        listView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {

            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                // We call collapseGroupWithAnimation(int) and
                // expandGroupWithAnimation(int) to animate group
                // expansion/collapse.
                if (listView.isGroupExpanded(groupPosition)) {
                    listView.collapseGroupWithAnimation(groupPosition);
                } else {
                    selectedGroupIndex = groupPosition;
                    listView.expandGroupWithAnimation(groupPosition);
                }
                return true;
            }

        });

        listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int groupPosition, int childPosition, long l) {
                ParseStamp stamp = (ParseStamp) stampCollectionAdapter.getChild(groupPosition, childPosition);
                onBoxInteractionListener.onRequestEditStamp(stamp);
                return true;
            }
        });

        stampCollectionAdapter = new StampCollectionAdapter(getActivity());
        listView.setAdapter(stampCollectionAdapter);

        // fill list up
        // we set selected group index to 0 in the beginning to force the latest collection to expand
        // this is kind of a hack since in the beginning no collections are expanded and
        selectedGroupIndex = 0;
        refreshData();

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            onBoxInteractionListener = (OnBoxInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnBoxInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void refreshData() {

        ParseUser currentUser = ParseUser.getCurrentUser();
        List<ParseStamp> stamps = currentUser.getList(ProfileBuilder.PROFILE_KEY_STAMPS);

        // check for valid data
        if(stamps == null) {
            errorContainer.setVisibility(View.VISIBLE);
            errorText.setText(R.string.corrupt_stamps_error_description);
            return;
        }

        // check if there are any stamps
        if(stamps.size() == 0) {
            errorContainer.setVisibility(View.VISIBLE);
            errorText.setText(R.string.no_stamps_error_description);

            return;
        }

        progressContainer.setVisibility(View.VISIBLE);

        // fetch the stamps and fill our charts
        ParseStamp.fetchAllIfNeededInBackground(stamps, new FindCallback<ParseStamp>() {
            @Override
            public void done(List<ParseStamp> stampList, ParseException e) {
                progressContainer.setVisibility(View.GONE);

                if (e != null || stampList == null) {
                    errorContainer.setVisibility(View.VISIBLE);
                    errorText.setText(R.string.loading_error_description);

                    return;
                }

                StampCollectionBox stampCollectionBox = new StampCollectionBox(stampList);
                List<StampCollection> stampCollections = stampCollectionBox.getStampCollections();

                stampCollectionAdapter.setCollections(stampCollections);

                if(selectedGroupIndex > -1 && selectedGroupIndex < stampCollectionAdapter.getGroupCount())
                    listView.expandGroup(selectedGroupIndex);
            }
        });
    }

    public interface OnBoxInteractionListener {
        void onRequestAddStamp();
        void onRequestEditStamp(ParseStamp stamp);
    }
}
