package com.trinew.easytime.fragments.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.trinew.easytime.R;
import com.trinew.easytime.models.ParseStamp;
import com.trinew.easytime.utils.PrettyTime;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by jonathanlu on 9/14/15.
 */
public class EditStampDialogFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener,
        TimePickerDialog.OnTimeSetListener {

    // arg key constants
    public final static String KEY_DIALOG_INTENT = "dialogIntent";
    public final static String KEY_EDIT_FLAG = "editFlag";
    public final static String KEY_EDIT_DATE = "editDate";
    public final static String KEY_EDIT_COMMENT = "editComment";

    // intent constants
    public final static int DIALOG_INTENT_ADD = 0;
    public final static int DIALOG_INTENT_EDIT = 1;

    // views
    private TextView editTitleText;

    private ImageButton deleteButton;

    private EditText dateEditText;
    private EditText timeEditText;

    private EditText commentEditText;

    private Button flagButtonIn;
    private Button flagButtonOut;

    private Button cancelButton;
    private Button submitButton;

    // data
    private int dialogIntent;

    private Dialog editDialog;

    private int editFlag;
    private Calendar editCalendar;

    public EditStampDialogFragment() { }

    public static EditStampDialogFragment newInstance() {
        return newInstance(DIALOG_INTENT_ADD, ParseStamp.FLAG_CHECK_IN, Calendar.getInstance().getTime(), "");
    }

    public static EditStampDialogFragment newInstance(int dialogIntent, int editFlag, Date editDate, String editComment) {
        EditStampDialogFragment fragment = new EditStampDialogFragment();

        // arguments
        Bundle arguments = new Bundle();
        arguments.putInt(KEY_DIALOG_INTENT, dialogIntent);
        arguments.putInt(KEY_EDIT_FLAG, editFlag);
        arguments.putLong(KEY_EDIT_DATE, editDate.getTime());
        arguments.putString(KEY_EDIT_COMMENT, editComment);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_fragment_edit_stamp, container);

        editDialog = getDialog();
        editCalendar = Calendar.getInstance();

        // init views
        editTitleText = (TextView) view.findViewById(R.id.editStampTitleText);

        deleteButton = (ImageButton) view.findViewById(R.id.deleteButton);

        dateEditText = (EditText) view.findViewById(R.id.dateEditText);
        timeEditText = (EditText) view.findViewById(R.id.timeEditText);
        commentEditText = (EditText) view.findViewById(R.id.commentEditText);

        flagButtonIn = (Button) view.findViewById(R.id.flagButtonIn);
        flagButtonOut = (Button) view.findViewById(R.id.flagButtonOut);

        cancelButton = (Button) view.findViewById(R.id.cancelButton);
        submitButton = (Button) view.findViewById(R.id.submitButton);

        // add listeners to edit texts
        dateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(
                        EditStampDialogFragment.this,
                        editCalendar.get(Calendar.YEAR),
                        editCalendar.get(Calendar.MONTH),
                        editCalendar.get(Calendar.DAY_OF_MONTH)
                );

                datePickerDialog.setAccentColor(ContextCompat.getColor(getActivity(), R.color.accent));

                datePickerDialog.setMaxDate(Calendar.getInstance());

                datePickerDialog.show(getFragmentManager(), "Date Picker Dialog");
            }
        });

        timeEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(
                        EditStampDialogFragment.this,
                        editCalendar.get(Calendar.HOUR_OF_DAY),
                        editCalendar.get(Calendar.MINUTE),
                        false
                );

                timePickerDialog.setAccentColor(ContextCompat.getColor(getActivity(), R.color.accent));

                timePickerDialog.show(getFragmentManager(), "Time Picker Dialog");
            }
        });

        // add listeners to buttons
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editDialog.dismiss();

                onDelete();
            }
        });

        flagButtonIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editFlag = ParseStamp.FLAG_CHECK_OUT;
                flagButtonIn.setVisibility(View.GONE);
                flagButtonOut.setVisibility(View.VISIBLE);
            }
        });

        flagButtonOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editFlag = ParseStamp.FLAG_CHECK_IN;
                flagButtonOut.setVisibility(View.GONE);
                flagButtonIn.setVisibility(View.VISIBLE);
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editDialog.dismiss();
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editDialog.dismiss();

                onFinish();
            }
        });

        commentEditText.requestFocus();

        // fill views
        Bundle args = getArguments();
        dialogIntent = args.getInt(KEY_DIALOG_INTENT, DIALOG_INTENT_ADD);

        switch(dialogIntent) {
            case DIALOG_INTENT_ADD:
                deleteButton.setVisibility(View.GONE);
                editTitleText.setText(R.string.title_add_stamp);
                break;
            case DIALOG_INTENT_EDIT:
                editTitleText.setText(R.string.title_edit_stamp);
                break;
        }
        commentEditText.setText(args.getString(KEY_EDIT_COMMENT, ""));

        editFlag = args.getInt(KEY_EDIT_FLAG, ParseStamp.FLAG_CHECK_IN);
        switch (editFlag) {
            case ParseStamp.FLAG_CHECK_OUT:
                flagButtonIn.setVisibility(View.GONE);
                flagButtonOut.setVisibility(View.VISIBLE);
                break;
        }

        editCalendar.setTimeInMillis(args.getLong(KEY_EDIT_DATE, System.currentTimeMillis()));

        Date editDate = editCalendar.getTime();
        dateEditText.setText(PrettyTime.getPrettyShortDate(editDate));
        timeEditText.setText(PrettyTime.getPrettyTime(editDate));

        return view;
    }

    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
        editCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        editCalendar.set(Calendar.MINUTE, minute);

        timeEditText.setText(PrettyTime.getPrettyTime(editCalendar.getTime()));
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        editCalendar.set(Calendar.YEAR, year);
        editCalendar.set(Calendar.MONTH, monthOfYear);
        editCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        Calendar nowCalendar = Calendar.getInstance();

        String dateStr = "";
        if(nowCalendar.get(Calendar.YEAR) != year) {
            dateStr = new SimpleDateFormat("MMM d, yyyy").format(editCalendar.getTime());
        } else {
            dateStr = PrettyTime.getPrettyShortDate(editCalendar.getTime());
        }

        dateEditText.setText(dateStr);
    }

    private void onDelete() {
        OnEditStampInteractionListener onEditStampInteractionListener = (OnEditStampInteractionListener) getActivity();
        onEditStampInteractionListener.onEditStampDelete();
    }

    private void onFinish() {
        String commentStr = commentEditText.getText().toString();

        OnEditStampInteractionListener onEditStampInteractionListener = (OnEditStampInteractionListener) getActivity();
        onEditStampInteractionListener.onEditStampFinish(dialogIntent, editFlag, editCalendar.getTime(), commentStr);
    }

    public interface OnEditStampInteractionListener {
        void onEditStampDelete();
        void onEditStampFinish(int dialogIntent, int editFlag, Date editDate, String editComment);
    }
}
