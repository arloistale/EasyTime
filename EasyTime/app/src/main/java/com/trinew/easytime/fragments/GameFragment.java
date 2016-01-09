package com.trinew.easytime.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.trinew.easytime.R;
import com.trinew.easytime.models.ParseStamp;
import com.trinew.easytime.modules.ProfileBuilder;
import com.trinew.easytime.modules.time.EasyTimer;
import com.trinew.easytime.utils.PrettyTime;
import com.trinew.easytime.utils.StampHelper;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class GameFragment extends Fragment {

    // listener
    private OnGameInteractionListener onGameInteractionListener;

    // views
    private TextView dateText;
    private TextView timeText;

    private TextView timerHoursText;
    private TextView timerMinutesText;

    private ViewAnimator feedAnimator;
    private TextView feedText1;
    private TextView feedText2;

    private RelativeLayout errorContainer;
    private RelativeLayout progressContainer;

    private TextView errorText;

    private Button checkInButton;
    private Button checkOutButton;

    // time
    private EasyTimer easyTimer;

    // state
    private int checkState = -1;
    private int checkIndex = 0;

    private ParseStamp lastStamp;

    public static GameFragment newInstance() {
        GameFragment fragment = new GameFragment();
        return fragment;
    }

    public GameFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_game, container, false);

        // init timer
        easyTimer = new EasyTimer();
        easyTimer.setEasyTimerListener(new EasyTimer.EasyTimerListener() {
            @Override
            public void onUpdate(long elapsedTime) {
                syncDateDisplay();

                if(lastStamp != null && lastStamp.getFlag() == ParseStamp.FLAG_CHECK_IN) {

                    // check if its a new day
                    if(StampHelper.isStampNewDay(lastStamp)) {
                        // if its a new day we generate a checkout stamp for the end previous day
                        // we also generate a check in stamp for the beginning of today so that the
                        // timer can keep running
                        lastStamp = generateOverFlowStamps(lastStamp);
                    }
                }

                syncDurationDisplay();
            }
        });

        // init views
        dateText = (TextView) view.findViewById(R.id.dateText);
        timeText = (TextView) view.findViewById(R.id.timeText);

        timerHoursText = (TextView) view.findViewById(R.id.timerHoursText);
        timerMinutesText = (TextView) view.findViewById(R.id.timerMinutesText);

        feedAnimator = (ViewAnimator) view.findViewById(R.id.feedAnimator);
        feedAnimator.setInAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.anim_slide_in_top));
        feedAnimator.setOutAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.anim_slide_out_top));

        feedText1 = (TextView) view.findViewById(R.id.feedText1);
        feedText2 = (TextView) view.findViewById(R.id.feedText2);

        errorContainer = (RelativeLayout) view.findViewById(R.id.errorContainer);
        progressContainer = (RelativeLayout) view.findViewById(R.id.progressContainer);

        errorText = (TextView) view.findViewById(R.id.errorText);

        checkInButton = (Button) view.findViewById(R.id.checkInButton);
        checkOutButton = (Button) view.findViewById(R.id.checkOutButton);

        checkInButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                checkIn();
            }
        });

        checkOutButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                checkOut();
            }
        });

        // fill views
        Date currDate = Calendar.getInstance().getTime();

        dateText.setText(PrettyTime.getPrettyFullDate(currDate));
        timeText.setText(PrettyTime.getPrettyTime(currDate));

        // init from cloud
        progressContainer.setVisibility(View.VISIBLE);

        ParseUser currentUser = ParseUser.getCurrentUser();
        List<ParseStamp> stamps = currentUser.getList(ProfileBuilder.PROFILE_KEY_STAMPS);
        if(stamps == null) {
            errorContainer.setVisibility(View.VISIBLE);
            // corrupt data error
            errorText.setText(R.string.corrupt_stamps_error_description);
        } else if(stamps.size() > 0) {
            ParseStamp.fetchAllIfNeededInBackground(stamps, new FindCallback<ParseStamp>() {
                @Override
                public void done(List<ParseStamp> list, ParseException e) {
                    progressContainer.setVisibility(View.GONE);

                    if (e != null) {
                        errorContainer.setVisibility(View.VISIBLE);
                        errorText.setText(R.string.loading_error_description);

                        return;
                    }

                    ParseStamp potentialLastStamp = list.get(list.size() - 1);
                    Date stampDate = potentialLastStamp.getLogDate();
                    Calendar nowCalendar = Calendar.getInstance();
                    Calendar stampCalendar = Calendar.getInstance();
                    stampCalendar.setTime(stampDate);

                    if (stampCalendar.get(Calendar.YEAR) == nowCalendar.get(Calendar.YEAR) &&
                            stampCalendar.get(Calendar.DAY_OF_YEAR) == nowCalendar.get(Calendar.DAY_OF_YEAR)) {

                        switch (potentialLastStamp.getFlag()) {
                            case ParseStamp.FLAG_CHECK_IN:
                                changeState(ParseStamp.FLAG_CHECK_OUT);

                                feedText1.setText("Last checked in at " + PrettyTime.getPrettyTime(potentialLastStamp.getLogDate()));
                                break;
                            case ParseStamp.FLAG_CHECK_OUT:
                                feedText1.setText("Last checked out at " + PrettyTime.getPrettyTime(potentialLastStamp.getLogDate()));
                                changeState(ParseStamp.FLAG_CHECK_IN);
                                break;
                        }

                        lastStamp = potentialLastStamp;

                    } else if(potentialLastStamp.getFlag() == ParseStamp.FLAG_CHECK_IN) {
                        // its a new day and the user didnt check out in time
                        // saves a parse stamp at 11:59 PM of the previous day
                        lastStamp = generateOverFlowStamps(potentialLastStamp);

                        changeState(ParseStamp.FLAG_CHECK_OUT);
                    }

                    // start the timer once everything is loaded
                    easyTimer.startTimer();

                    syncDurationDisplay();
                }
            });
        } else {
            progressContainer.setVisibility(View.GONE);
            syncDurationDisplay();

            // start the timer since we don't have to load anything
            easyTimer.startTimer();
        }

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            onGameInteractionListener = (OnGameInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnGameInteractionListener");
        }
    }

    @Override
    public void onPause() {
        easyTimer.pauseTimer();

        super.onPause();
    }

    @Override
    public void onResume() {
        easyTimer.resumeTimer();

        super.onResume();
    }

    // generates two stamps for when a user is checked in at midnight of the previous day
    // returns lastStamp to the generated check in stamp (assumes there are in stamps on the current day)
    private ParseStamp generateOverFlowStamps(ParseStamp overFlowStamp) {
        Calendar stampCalendar = Calendar.getInstance();
        stampCalendar.setTime(overFlowStamp.getLogDate());
        stampCalendar.set(Calendar.HOUR_OF_DAY, 23);
        stampCalendar.set(Calendar.MINUTE, 59);

        Calendar nowCalendar = Calendar.getInstance();
        nowCalendar.set(Calendar.HOUR_OF_DAY, 0);
        nowCalendar.set(Calendar.MINUTE, 0);

        final ParseStamp generatedOutStamp = new ParseStamp();
        generatedOutStamp.setLogDate(stampCalendar.getTime());
        generatedOutStamp.setFlag(ParseStamp.FLAG_CHECK_OUT);
        generatedOutStamp.setComment("Automatically checked out");
        generatedOutStamp.saveEventually(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    // save the stamps
                    // TODO: There may be a bug here if the user doesn't save in time for when the check out button is pressed
                    // so the list might become out of order but probably not
                    ParseUser currentUser = ParseUser.getCurrentUser();
                    List<ParseStamp> currentStamps = currentUser.getList(ProfileBuilder.PROFILE_KEY_STAMPS);
                    currentStamps.add(generatedOutStamp);
                    currentUser.put(ProfileBuilder.PROFILE_KEY_STAMPS, currentStamps);
                    currentUser.saveEventually();
                }
            }
        });

        // set the last stamp to our generated in stamp
        final ParseStamp generatedLastStamp = new ParseStamp();
        generatedLastStamp.setLogDate(nowCalendar.getTime());
        generatedLastStamp.setFlag(ParseStamp.FLAG_CHECK_IN);
        generatedLastStamp.setComment("Automatically checked in");
        generatedLastStamp.saveEventually(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    // save the stamps
                    // TODO: There may be a bug here if the user doesn't save in time for when the check out button is pressed
                    // so the list might become out of order but probably not
                    ParseUser currentUser = ParseUser.getCurrentUser();
                    List<ParseStamp> currentStamps = currentUser.getList(ProfileBuilder.PROFILE_KEY_STAMPS);
                    currentStamps.add(generatedLastStamp);
                    currentUser.put(ProfileBuilder.PROFILE_KEY_STAMPS, currentStamps);
                    currentUser.saveEventually();
                }
            }
        });

        return generatedLastStamp;
    }

    // pushes a new feed entry onto the check view
    private void pushFeedEntry(ParseStamp stamp) {
        checkIndex = (checkIndex + 1) % 2;

        Date stampDate = stamp.getLogDate();
        int stampFlag = stamp.getFlag();

        String flagStr = "";
        switch(stampFlag) {
            case ParseStamp.FLAG_CHECK_IN:
                flagStr = "in";
                break;
            case ParseStamp.FLAG_CHECK_OUT:
                flagStr = "out";
                break;
        }

        String stampStr = "Last checked " + flagStr + " at " + PrettyTime.getPrettyTime(stampDate);

        switch(checkIndex) {
            case 0:
                feedText1.setText(stampStr);
                break;
            case 1:
                feedText2.setText(stampStr);
                break;
        }

        feedAnimator.showNext();
    }

    private void syncDateDisplay() {
        Date currDate = Calendar.getInstance().getTime();
        dateText.setText(PrettyTime.getPrettyFullDate(currDate));
        timeText.setText(PrettyTime.getPrettyTime(currDate));
    }

    // updates the duration textviews with total time since last stamp
    private void syncDurationDisplay() {
        long durationTime = 0;

        if(lastStamp != null && lastStamp.getFlag() == ParseStamp.FLAG_CHECK_IN) {
            durationTime = StampHelper.getStampDurationNow(lastStamp);
        }

        long hours = TimeUnit.MILLISECONDS.toHours(durationTime);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationTime) - TimeUnit.HOURS.toMinutes(hours);
        NumberFormat hoursFormat = new DecimalFormat("0");
        NumberFormat minutesFormat = new DecimalFormat("00");
        String hoursStr = hoursFormat.format(hours);
        String minutesStr = minutesFormat.format(minutes);
        timerHoursText.setText(hoursStr);
        timerMinutesText.setText(minutesStr);
    }

    // changes the state of the page
    private void changeState(int newState) {
        switch (newState) {
            case ParseStamp.FLAG_CHECK_IN:
                checkInButton.setVisibility(View.VISIBLE);
                checkOutButton.setVisibility(View.GONE);
                break;
            case ParseStamp.FLAG_CHECK_OUT:
                checkInButton.setVisibility(View.GONE);
                checkOutButton.setVisibility(View.VISIBLE);
                break;
        }
    }

    // interaction
    private void checkOut() {
        checkOutButton.setEnabled(false);

        if (easyTimer.isRunning()) {
            easyTimer.pauseTimer();
        }

        final ParseUser user = ParseUser.getCurrentUser();

        // clone the list of stamps from the user
        final List<ParseStamp> parseStamps = user.getList(ProfileBuilder.PROFILE_KEY_STAMPS);

        // init the stamp
        lastStamp = new ParseStamp();
        lastStamp.setLogDate(new Date());
        lastStamp.setFlag(ParseStamp.FLAG_CHECK_OUT);
        lastStamp.setComment("");

        lastStamp.saveEventually(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Toast.makeText(getActivity(), "There was a problem checking out, please try again", Toast.LENGTH_LONG).show();
                    return;
                }

                parseStamps.add(lastStamp);
                user.put(ProfileBuilder.PROFILE_KEY_STAMPS, parseStamps);

                user.saveEventually(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e != null) {
                            Toast.makeText(getActivity(), "There was a problem checking out, please try again", Toast.LENGTH_LONG).show();
                            return;
                        }

                        checkState = ParseStamp.FLAG_CHECK_OUT;
                    }
                });
            }
        });

        // begin animations
        final Animation a = new RotateAnimation(0f, 360f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        a.setRepeatCount(-1);
        a.setDuration(1000);
        a.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                if (checkState == ParseStamp.FLAG_CHECK_OUT) {
                    pushFeedEntry(lastStamp);

                    checkOutButton.clearAnimation();
                    checkOutButton.setEnabled(true);

                    checkOutButton.setVisibility(View.GONE);
                    checkInButton.setVisibility(View.VISIBLE);

                    syncDurationDisplay();
                }
            }
        });

        checkOutButton.startAnimation(a);
    }

    private void checkIn() {
        checkInButton.setEnabled(false);

        final ParseUser user = ParseUser.getCurrentUser();

        final List<ParseStamp> parseStamps = user.getList(ProfileBuilder.PROFILE_KEY_STAMPS);

        lastStamp = new ParseStamp();
        lastStamp.setLogDate(new Date());
        lastStamp.setFlag(ParseStamp.FLAG_CHECK_IN);
        lastStamp.setComment("");

        lastStamp.saveEventually(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Toast.makeText(getActivity(), "There was a problem checking in, please try again.", Toast.LENGTH_LONG).show();
                    return;
                }

                parseStamps.add(lastStamp);
                user.put(ProfileBuilder.PROFILE_KEY_STAMPS, parseStamps);

                user.saveEventually(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e != null) {
                            Toast.makeText(getActivity(), "There was a problem checking in, please try again.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        checkState = ParseStamp.FLAG_CHECK_IN;
                    }
                });
            }
        });

        final Animation a = new RotateAnimation(0.0f, 360.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        a.setRepeatCount(-1);
        a.setDuration(1000);
        a.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                if (checkState == ParseStamp.FLAG_CHECK_IN) {

                    // TODO: The start time of the timer may not be the same as the log date of the flag
                    pushFeedEntry(lastStamp);

                    checkInButton.clearAnimation();
                    checkInButton.setVisibility(View.GONE);
                    checkInButton.setEnabled(true);

                    checkOutButton.setVisibility(View.VISIBLE);

                    syncDurationDisplay();
                }
            }
        });

        checkInButton.startAnimation(a);
    }

    private interface StampListener {
        void done(Exception error);
    }

    public interface OnGameInteractionListener {
    }
}
