package com.kaetter.motorcyclemaintenancelog;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import butterknife.Bind;
import butterknife.ButterKnife;
import dbcontrollers.MainLogSource;
import de.greenrobot.event.EventBus;
import events.DatePickedEvent;
import utils.Summarize;

public class ConfigFragment extends Fragment implements
		LoaderManager.LoaderCallbacks<Cursor> {

	@Bind(R.id.bikenametext) EditText bikeName;
	@Bind(R.id.dateofpurchaset) EditText bikeDate;
	@Bind(R.id.initialodometert) EditText bikeOdo;
	@Bind(R.id.otherdetails) EditText bikeOtherDetails;
	@Bind(R.id.from) Button from;
	@Bind(R.id.to) Button to;
	@Bind(R.id.maintelemspinner) Spinner maintelemspinner;
	@Bind(R.id.cashperelementvalue) TextView cashperelementvalue;

	MainLogSource mainLogSource;
	Cursor currentCursor;
	Summarize sum;

	View root;

	public static ConfigFragment newInstance() {
		Bundle args = new Bundle();
		ConfigFragment fragment = new ConfigFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		root = inflater.inflate(R.layout.fragment_config, container, false);

		ButterKnife.bind(this, root);

		// get general bike data
		getGeneralBikeData();

		// set FROM date
		SharedPreferences generalPref = getActivity().getSharedPreferences(
				getString(R.string.general_preference_file_key),
				Context.MODE_PRIVATE);
		if (TextUtils.isEmpty(generalPref.getString("dateofpurchaset", ""))) {
			from.setText(getString(R.string.select_date));
		} else {
			from.setText(generalPref.getString("dateofpurchaset", ""));
		}

		from.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				DialogFragment newFragment = new DatePickerFragment();
				Bundle args = new Bundle();
				args.putInt("type", 1);
				newFragment.setArguments(args);
				newFragment.show(getChildFragmentManager(), "");
			}
		});

		// set TO date
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		to.setText(sdf.format(Calendar.getInstance().getTime()));

		to.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				DialogFragment newFragment = new DatePickerFragment();
				Bundle args = new Bundle();
				args.putInt("type", 2);
				newFragment.setArguments(args);
				newFragment.show(getChildFragmentManager(), "");
			}
		});

		// populate spinner
		ArrayList<String> list = new ArrayList<>();
		list.add("...");

		ArrayAdapter<String> maintElemAdapter = new ArrayAdapter<>(
				getActivity(), android.R.layout.simple_list_item_1, list);

		maintelemspinner.setAdapter(maintElemAdapter);
		maintelemspinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos , long id) {
				if (pos != 0) {
					sum = new Summarize();
					sum.execute(2,currentCursor,from.getText().toString(), root,
							bikeDate.getText().toString());
				} else {
					cashperelementvalue.setText("0");
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		Bundle b = new Bundle();
		b.putString("from", "1800-01-01");
		b.putString("to", "2200-01-01");

		getLoaderManager().initLoader(2, b, this);

		return root;
	}

	@Override
	public void onResume() {
		super.onResume();
		EventBus.getDefault().register(this);
	}

	@Override
	public void onPause() {
		EventBus.getDefault().unregister(this);
		super.onPause();
	}

	public void onEvent(DatePickedEvent event) {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		Calendar cal = Calendar.getInstance();
		cal.set(event.year, event.month, event.day);

		if (event.type == 0) {
			bikeDate.setText(sdf.format(cal.getTime()));

			SharedPreferences generalPref = getActivity().getSharedPreferences(
					getString(R.string.general_preference_file_key),
					Context.MODE_PRIVATE);
			final SharedPreferences.Editor generalEditor = generalPref.edit();
			generalEditor.putString("dateofpurchaset", sdf.format(cal.getTime()));
			generalEditor.commit();
		} else if (event.type == 1) {
			from.setText(sdf.format(cal.getTime()));
		} else if (event.type == 2) {
			to.setText(sdf.format(cal.getTime()));
		}
	}

	public void getGeneralBikeData() {

		SharedPreferences generalPref = getActivity().getSharedPreferences(
				getString(R.string.general_preference_file_key),
				Context.MODE_PRIVATE);

		final SharedPreferences.Editor generalEditor = generalPref.edit();

		// bike name
		bikeName.setText(generalPref.getString("bikenametext", ""));
		bikeName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					generalEditor.putString("bikenametext", bikeName
							.getText().toString().trim());
					generalEditor.commit();
				}
			}
		});

		// bike date
		bikeDate.setText(generalPref.getString("dateofpurchaset", ""));
		bikeDate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				DialogFragment newFragment = new DatePickerFragment();
				Bundle args = new Bundle();
				args.putInt("type", 0);
				newFragment.setArguments(args);
				newFragment.show(getChildFragmentManager(), "");
			}
		});

		// bike odometer
		bikeOdo.setText(generalPref.getString("initialodometert", ""));
		bikeOdo.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					generalEditor.putString("initialodometert", bikeOdo
							.getText().toString().trim());
					generalEditor.commit();
				}
			}
		});

		bikeOtherDetails.setText(generalPref.getString("otherdetails", ""));
		bikeOtherDetails.setOnFocusChangeListener(new View.OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {

				if (!hasFocus) {

					generalEditor.putString("otherdetails", bikeOtherDetails
							.getText().toString().trim());
					generalEditor.commit();
				}
			}
		});
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, final Bundle args) {
		AsyncTaskLoader<Cursor> loader = null;
		if (id == 2) {
			loader = new AsyncTaskLoader<Cursor>(getActivity()) {
				@Override
				public Cursor loadInBackground() {
					if (mainLogSource == null) {
						mainLogSource = new MainLogSource(getContext());
					}
					return mainLogSource.getConfCursor(
							args.getString("from"), args.getString("to"));
				}
			};
			loader.forceLoad();
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if(loader.getId()==2) {
			currentCursor = data;
			if(!data.isAfterLast()) {
				sum = new Summarize();
				sum.execute(1, data, from.getText().toString(), root,
						bikeDate.getText().toString());
			} else {
				sum = new Summarize();
				sum.execute(0, data, from.getText().toString(), root,
						bikeDate.getText().toString());
			}

		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {}

	/**
	 * Pass argument "type" according to view clicked<br>
	 * 0 - Date of Purchase<br>
	 * 1 - FROM date<br>
	 * 2 - TO date<br>
	 * <br>
	 * Pass arguments "year", "month", "day"<br>
	 * if date other than TODAY is desired<br>
	 */
	public static class DatePickerFragment extends DialogFragment
			implements DatePickerDialog.OnDateSetListener {

		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Use the current date as the default date in the picker
			final Calendar c = Calendar.getInstance();
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH);
			int day = c.get(Calendar.DAY_OF_MONTH);
			// Create a new instance of DatePickerDialog and return it
			return new DatePickerDialog(getActivity(), this, year, month, day);
		}

		public void onDateSet(DatePicker view, int year, int month, int day) {
			EventBus.getDefault().post(
					new DatePickedEvent(getArguments().getInt("type"), year, month, day));
		}
	}
}
