package com.kaetter.motorcyclemaintenancelog;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import beans.MaintenanceItem;
import beans.ReminderItem;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dbcontrollers.MainLogSource;
import dbcontrollers.MotoLogHelper;
import dbcontrollers.RemLogSource;
import dialogs.DatePickerFragment;
import dialogs.NewElementDialog.OnNewElementListener;
import dialogs.NewRemDialog;
import events.DatePickedEvent;

public class NewLogActivity extends AppCompatActivity implements OnNewElementListener {

    public static final String TAG = "NewLogActivity";

	@BindView(R.id.toolbar) Toolbar toolbar;
	@BindView(R.id.spinnerElement) Spinner spinnerElement;
	@BindView(R.id.spinnerType) Spinner spinnerType;
	@BindView(R.id.checkBoxReminder) CheckBox checkBoxReminder;
	@BindView(R.id.editTextReminderInterval) EditText editTextReminderInterval;
	@BindView(R.id.textReminderIntervalUnit) TextView textReminderIntervalUnit;
	@BindView(R.id.checkBoxDate) CheckBox checkBoxDate;
	@BindView(R.id.spinnerReminderIntervalDateType) Spinner spinnerReminderIntervalDateType;
	@BindView(R.id.spinnerReminderIntervalDateNumber) Spinner spinnerReminderIntervalDateNumber;
	@BindView(R.id.editTextFuel) EditText editTextFuel;
	@BindView(R.id.editTextMemo) EditText editTextMemo;
	@BindView(R.id.editTextOdometer) EditText editTextOdometer;
	@BindView(R.id.editTextPrice) EditText editTextPrice;
	@BindView(R.id.buttonCreate) Button buttonCreate;
	@BindView(R.id.buttonCancel) Button buttonCancel;
	@BindView(R.id.buttonDate) Button buttonDate;
	@BindView(R.id.textPriceUnit) TextView textPriceUnit;
	@BindView(R.id.layoutReminder) View layoutReminder;

	private final int NEWELEMDIALOG = 10;
	private final int DELETEELEM = 1;

	private ArrayAdapter<String> adapterSpinnerElement;
	private ArrayAdapter<String> adapterSpinnerType;
	private ArrayAdapter<String> intervalDateAdapter;
	private ArrayAdapter<String> intervalDateAdapterP;
	private ArrayList<String> maintElemArrayList;
	private ArrayList<String> maintTypeArrayList;

	private MaintenanceItem item;
	private MainLogSource mainlogSource;
	private ReminderItem remItemSent;

	private boolean isModification;

	private SharedPreferences intervalPref;
	private SharedPreferences elemTypePref;
	private SharedPreferences elemPref;
	private SharedPreferences sharedPrefs;

	private int elemTypeCount;

	public static final String INTERVALCOUNTSTRING = "intervalCount";
	public static final String ELEMCOUNTSTRING = "elemCountString";
	public static final String ELEMTYPECOUNTSTRING = "elemTypeCount";

	public static final String INTERVALVAL = "intervalVal_";
	public static final String ELEMVAL = "elemVal_";
	public static final String ELEMTYPEVAL = "elemtypeVal_";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_new_log);
		ButterKnife.bind(this);

		setSupportActionBar(toolbar);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle(getString(R.string.toolbar_title_new_log_entry));
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

		// load preferences
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		intervalPref = getSharedPreferences(
				getString(R.string.interval_preference_file_key), MODE_PRIVATE);
		elemTypePref = getSharedPreferences(
				getString(R.string.elemtype_preference_file_key), MODE_PRIVATE);
		elemPref = getSharedPreferences(
				getString(R.string.elem_preference_file_key), MODE_PRIVATE);
		setPreferences();

		// these arguments are sent from update in LogFragment
		item = (MaintenanceItem) getIntent().getSerializableExtra("Maintenanceitem");
		isModification = getIntent().getBooleanExtra("isModification", false);
		remItemSent = (ReminderItem) getIntent().getSerializableExtra("ReminderItem");

		// set units according to user specified mileageType
		if (sharedPrefs.getString("pref_CashType", "0").equals("0")) {
			if (sharedPrefs.getString("pref_MileageType", "0").equals("1")) {
				textPriceUnit.setText("/g");
			} else {
				textPriceUnit.setText("/l");
			}
		}

		populateSpinnerElement(-1);
		populateSpinnerType(-1);

		initialiseDatePicker(null);
		initialiseCheckBoxes();

		if (item != null) {
			setValues();
		}
	}

	public void setPreferences() {

		Editor intervalEditor = intervalPref.edit();
		Editor elemTypeEditor = elemTypePref.edit();
		Editor elemEditor = elemPref.edit();

		// set intervals defined by user from preferences
		// these are used to populate spinners
		int intervalCount = intervalPref.getInt(INTERVALCOUNTSTRING, 0);
		String[] intervalNumberList = getResources().getStringArray(R.array.intervalNumberArray);
		ArrayList<String> intervalNumberArrayList =
				new ArrayList<>(Arrays.asList(intervalNumberList));

		if (intervalCount == 0) { // populate
			intervalCount = intervalNumberList.length - 1;

			intervalEditor.putInt(INTERVALCOUNTSTRING, intervalCount);
			intervalEditor.apply();

			for (int i = 0; i <= intervalCount; i++) {
				intervalEditor.putString(INTERVALVAL + i, intervalNumberList[i]);
				intervalEditor.apply();
			}
		}

		if (intervalCount > intervalNumberList.length - 1) {
			for (int i = intervalNumberList.length; i <= intervalCount; i++) {
				intervalNumberArrayList.add(intervalPref.getString(INTERVALVAL
						+ i, " "));
			}
		}

		// Set maintElemList from preferences
		// populate maintSpinner
		int elemCount = elemPref.getInt(ELEMCOUNTSTRING, 0);
		String[] maintElemList = getResources().getStringArray(R.array.maintElemArray);
		maintElemArrayList = new ArrayList<>(Arrays.asList(maintElemList));

		if (elemCount == 0) { // populate preferences
			elemCount = maintElemList.length;
			elemEditor.putInt(ELEMCOUNTSTRING, elemCount);
			elemEditor.apply();

			for (int i = 0; i < elemCount; i++) {
				elemEditor.putString(ELEMVAL + i, maintElemList[i]);
				elemEditor.apply();
			}
		}

		// populate from preferences array that fills elemTypespinner
		elemTypeCount = elemTypePref.getInt(ELEMTYPECOUNTSTRING, 0);
		String[] maintTypeList = getResources().getStringArray(R.array.maintTypeArray);
		maintTypeArrayList = new ArrayList<>(Arrays.asList(maintTypeList));

		if (elemTypeCount == 0) {
			elemTypeCount = maintTypeList.length ;
			elemTypeEditor.putInt(ELEMTYPECOUNTSTRING, elemTypeCount);
			elemTypeEditor.apply();

			for (int i = 0; i < elemTypeCount; i++) {
				elemTypeEditor.putString(ELEMTYPEVAL + i, maintTypeList[i]);
				elemTypeEditor.apply();
			}
		}

		if(elemPref.getBoolean("firstrun", true)) {
			// let's recount elemcount and set it to the new value
			int elemcount = 0;
			Editor ed = elemPref.edit();
			Map<String, ?> m =elemPref.getAll();
			for (Object o : m.entrySet()) {
				Entry<String, ?> me = (Entry<String, ?>) o;
				if (me.getKey().startsWith(ELEMVAL)) {
					elemcount++;
				}
			}
			elemCount=elemcount;
			ed.putInt(ELEMCOUNTSTRING,elemcount).apply();

			int elemtypecount = 0;
			Editor edType = elemTypePref.edit();
			Map<String, ?> mt =elemTypePref.getAll();

			for (Object o : mt.entrySet()) {
				Entry<String, ?> mte = (Entry<String, ?>) o;
				if (mte.getKey().startsWith(ELEMTYPEVAL)) {
					elemtypecount++;
				}
			}
			elemTypeCount = elemtypecount;
			edType.putInt(ELEMTYPECOUNTSTRING, elemtypecount).apply();
			elemPref.edit().putBoolean("firstrun", false).apply();
		}

		if (elemCount > maintElemList.length ) {
			for (int i = maintElemList.length; i < elemCount; i++) {
				maintElemArrayList.add(elemPref.getString(ELEMVAL + i, " "));
			}
		}

		if (elemTypeCount > maintTypeList.length ) {
			for (int i = maintTypeList.length; i < elemTypeCount; i++) {
				maintTypeArrayList.add(elemTypePref.getString(ELEMTYPEVAL + i,
						" "));
			}
		}
	}

	public void setElemType() {
		elemTypeCount = elemTypePref.getInt(ELEMTYPECOUNTSTRING, 0);

		maintTypeArrayList.clear();
		for (int i = 0; i < elemTypeCount; i++) {
			maintTypeArrayList
					.add(elemTypePref.getString(ELEMTYPEVAL + i, " "));
		}
	}

	public void initialiseCheckBoxes() {

		// enable checkBoxDate and related fields if checked
		checkBoxReminder.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					layoutReminder.setVisibility(View.VISIBLE);
				} else {
					layoutReminder.setVisibility(View.GONE);
				}
			}
		});

		// enabled related fields if checked TODO: Will have layout problems
		checkBoxDate.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					spinnerReminderIntervalDateNumber.setVisibility(View.VISIBLE);
					spinnerReminderIntervalDateType.setVisibility(View.VISIBLE);
					editTextReminderInterval.setVisibility(View.GONE);
					textReminderIntervalUnit.setVisibility(View.GONE);
				} else {
					if (checkBoxReminder.isChecked()) {
						spinnerReminderIntervalDateNumber.setVisibility(View.GONE);
						spinnerReminderIntervalDateType.setVisibility(View.GONE);
						editTextReminderInterval.setVisibility(View.VISIBLE);
						textReminderIntervalUnit.setVisibility(View.VISIBLE);
					}
				}
			}
		});

		// populate spinnerReminderIntervalDateNumber and spinnerReminderIntervalDateType
		int intervalCount = intervalPref.getInt(INTERVALCOUNTSTRING, 0);

		String[] intervalNumberList = getResources().getStringArray(R.array.intervalNumberArray);

		ArrayList<String> intervalNumberArrayList =
				new ArrayList<>(Arrays.asList(intervalNumberList));

		if (intervalCount == 0) { // populate
			intervalCount = intervalNumberList.length - 1;
			intervalPref.edit().putInt(INTERVALCOUNTSTRING, intervalCount).apply();

			for (int i = 0; i <= intervalCount; i++) {
				intervalPref.edit().putString(INTERVALVAL + i, intervalNumberList[i]).apply();
			}
		}

		if (intervalCount > intervalNumberList.length - 1) {
			for (int i = intervalNumberList.length; i <= intervalCount; i++) {
				intervalNumberArrayList.add(intervalPref.getString(INTERVALVAL
						+ i, " "));
			}
		}

		ArrayAdapter<String> intervalNumberAdapter = new ArrayAdapter<>(
						this, android.R.layout.simple_spinner_item, intervalNumberArrayList);
		intervalNumberAdapter.setDropDownViewResource(
				android.R.layout.simple_spinner_dropdown_item);

		spinnerReminderIntervalDateNumber.setAdapter(intervalNumberAdapter);

		// load adapter for type list singular
		String[] intervalTypeList = getResources().getStringArray(R.array.intervalTypeArray);

		ArrayList<String> intervalTypeArrayList = new ArrayList<>(Arrays.asList(intervalTypeList));
		intervalDateAdapter = new ArrayAdapter<>(
				this, android.R.layout.simple_spinner_item, intervalTypeArrayList);

		// load adapter for type list plural
		String[] intervalTypeListP = getResources().getStringArray(R.array.intervalTypeArrayP);

		ArrayList<String> intervalTypeArrayListP =
				new ArrayList<>(Arrays.asList(intervalTypeListP));

		intervalDateAdapterP = new ArrayAdapter<>(
				this, android.R.layout.simple_spinner_item, intervalTypeArrayListP);

		// set singular first then change in listener to plural if necessary
		intervalDateAdapterP.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerReminderIntervalDateType.setAdapter(intervalDateAdapter);

		spinnerReminderIntervalDateNumber.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int position, long id) {

				if (!spinnerReminderIntervalDateNumber.getItemAtPosition(position)
						.equals("1")) {
					int i = spinnerReminderIntervalDateType.getSelectedItemPosition();
					spinnerReminderIntervalDateType.setAdapter(intervalDateAdapterP);
					spinnerReminderIntervalDateType.setSelection(i);

				} else {
					int i = spinnerReminderIntervalDateType.getSelectedItemPosition();
					spinnerReminderIntervalDateType.setAdapter(intervalDateAdapter);
					spinnerReminderIntervalDateType.setSelection(i);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});

	}

	public void populateSpinnerElement(int pos) {
		adapterSpinnerElement =
				new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, maintElemArrayList);
		adapterSpinnerElement.setDropDownViewResource(
				android.R.layout.simple_spinner_dropdown_item);
		spinnerElement.setAdapter(adapterSpinnerElement);

		spinnerElement.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				final String select = spinnerElement.getItemAtPosition(position).toString();

				//TODO: Deal with hardcoded text
				// if user selected "Other", show dialog for entering custom element
				if (select.equalsIgnoreCase("Other")) {
					showAddNewElementDialog();
				} else {
					if (item == null) {
						populateSpinnerType(-1);
					}

					if (!select.equalsIgnoreCase("Fuel")) {
						editTextFuel.setVisibility(View.GONE);
						textPriceUnit.setVisibility(View.GONE);
					} else {
						editTextFuel.setVisibility(View.VISIBLE);
						textPriceUnit.setVisibility(View.VISIBLE);
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});

		if (pos > -1 ) {
			spinnerElement.setSelection(pos-1);
		}
	}

	public void populateSpinnerType(final int pos) {
		String selectedElement = "";

		if (spinnerElement != null) {
			selectedElement = spinnerElement.getSelectedItem().toString();
		}

		if (item == null) {
			if (selectedElement.equalsIgnoreCase("Fuel")) {
				maintTypeArrayList.clear();
				maintTypeArrayList.add("Refuel");
				maintTypeArrayList.add("Other");
				editTextFuel.setEnabled(true);
			} else {
				editTextFuel.setEnabled(false);
			}

			if (selectedElement.equals("Oil")) {
				maintTypeArrayList.clear();
				maintTypeArrayList.add("Replace");
				maintTypeArrayList.add("Other");
			} else {
				if (!selectedElement.equals("Fuel")) {
					setElemType();
					maintTypeArrayList.remove("Refuel");
				}
			}
		}

		adapterSpinnerType = new ArrayAdapter<>(
				this, android.R.layout.simple_spinner_item, maintTypeArrayList);
		adapterSpinnerType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerType.setAdapter(adapterSpinnerType);

		spinnerType.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position != -1) {
					String select = spinnerType.getItemAtPosition(position).toString();
					if (select.equals(getString(R.string.text_other)) &&
							!spinnerElement.getSelectedItem().toString().equals(
									getString(R.string.text_fuel)) &&
							!spinnerElement.getSelectedItem().toString().equals(
									getString(R.string.text_oil))) {
						showAddNewElementTypeDialog();
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});

		spinnerType.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int pos, long arg3) {
				if (pos<4 ) {
				String value = (String) spinnerType.getItemAtPosition(pos);
					deleteSharedPreference(NewLogActivity.ELEMTYPEVAL, value);
				}
				else {
					Toast.makeText(getApplicationContext(),
							"Can't delete main types! ", Toast.LENGTH_LONG).show();
				}
				return false;
			}
		});
		
		if (pos >-1 ) {
			spinnerType.setSelection(pos-2);
		}
	}

	private void showAddNewElementDialog() {
		new MaterialDialog.Builder(this)
				.title(R.string.dialog_title_add_new_element)
				.inputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
				.inputRangeRes(1, 20, android.R.color.holo_red_light)
				.input("", "", new MaterialDialog.InputCallback() {
					@Override
					public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
						String newElement = input.toString();
						if (input.toString().equalsIgnoreCase("Other")) {
							newElement = "Other";
						}

						addNewSharedPreference(ELEMVAL, newElement.trim());
					}
				})
				.positiveText(R.string.button_add)
				.neutralText(R.string.button_use_other)
				.show();
	}

	private void showAddNewElementTypeDialog() {
		new MaterialDialog.Builder(this)
				.title(R.string.dialog_title_add_new_element_type)
				.inputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
				.inputRangeRes(1, 20, android.R.color.holo_red_light)
				.input("", "", new MaterialDialog.InputCallback() {
					@Override
					public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
						String newElementType = input.toString();
						if (input.toString().equalsIgnoreCase("Other")) {
							newElementType = "Other";
						}

						addNewSharedPreference(ELEMTYPEVAL, newElementType.trim());
					}
				})
				.positiveText(R.string.button_add)
				.neutralText(R.string.button_use_other)
				.show();
	}

	private void deleteSharedPreference(String type, String value) {
		if (type.equals(NewLogActivity.ELEMVAL)) {

			int elemCount = elemPref.getInt(NewLogActivity.ELEMCOUNTSTRING, 0);

			if (elemCount == 0) {
				Toast.makeText(this,
						"Something is wrong with SharedPreferences", Toast.LENGTH_LONG)
						.show();
			} else {
				String[] values = new String[elemCount-1];
				
				for (int i=0 ;i< elemCount;i++) {
					if (i < 5 ||
							(i >= 5 &&
									!elemPref.getString(NewLogActivity.ELEMVAL + elemCount, "")
											.equalsIgnoreCase(value))) {
						values[i] = elemPref.getString(NewLogActivity.ELEMVAL + i, "");
					}
				}
				elemPref.edit().clear();
				elemCount--;
				
				for (int j=0; j<elemCount; j++) {
					elemPref.edit().putString(NewLogActivity.ELEMVAL + j, values[j]);
				}
				
				elemPref.edit().putInt(NewLogActivity.ELEMCOUNTSTRING, elemCount);
				elemPref.edit().apply();
				
				setPreferences();
				populateSpinnerElement(-1);
				populateSpinnerType(-1);
			}
		} else if (type.equals(NewLogActivity.ELEMTYPEVAL)) {

			int elemTypeCount = elemTypePref.getInt(NewLogActivity.ELEMTYPECOUNTSTRING, 0);
			if (elemTypeCount == 0) {
				Toast.makeText(this,
						"Something wrong with SharedPreferences on "
								+ NewLogActivity.ELEMTYPECOUNTSTRING, Toast.LENGTH_LONG)
						.show();
			} else {
				String [] values = new String[elemTypeCount-1];

				for (int i=0 ;i< elemTypeCount;i++) {
					if (i<5 || (i>=5 && !elemPref.getString(NewLogActivity.ELEMTYPEVAL+elemTypeCount,"").equalsIgnoreCase(value)))
						values[i]=elemPref.getString(NewLogActivity.ELEMTYPEVAL+i,"");
				}
				elemTypePref.edit().clear();
				elemTypeCount--;

				for (int i=0 ;i< elemTypeCount;i++) {
					elemTypePref.edit().putString(NewLogActivity.ELEMTYPEVAL+i, values[i]);
				}

				elemTypePref.edit().putInt(NewLogActivity.ELEMTYPECOUNTSTRING, elemTypeCount);
				elemTypePref.edit().apply();

				setPreferences();
				populateSpinnerElement(-1);
				populateSpinnerType(-1);
			}
		}
		setPreferences();
	}

	@Subscribe
	public void onEvent(DatePickedEvent event) {
		Calendar cal = Calendar.getInstance();
		cal.set(event.year, event.month, event.day);

		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
		buttonDate.setText(sdf.format(cal.getTime()));
	}

	@OnClick(R.id.buttonDate)
	public void showDatePickerDialog() {

		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
		SimpleDateFormat sdf2 = new SimpleDateFormat("dd MM yyyy", Locale.US);
		String currentlySelectedDate = buttonDate.getText().toString();

		try {
			String dateString = sdf2.format(sdf.parse(currentlySelectedDate));

			String[] split = dateString.split(" ");

			Bundle args = new Bundle();
			args.putInt("year", Integer.parseInt(split[2]));
			args.putInt("month", Integer.parseInt(split[1]) - 1);
			args.putInt("day", Integer.parseInt(split[0]));

			DialogFragment newFragment = new DatePickerFragment();
			newFragment.setArguments(args);
			newFragment.show(getSupportFragmentManager(), "datePicker");
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initialise buttonDate with optional preselected Date
	 */
	private void initialiseDatePicker(Date date) {
		if (date == null) {
			date = new Date();
		}
		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
		buttonDate.setText(sdf.format(date));
	}

	@OnClick(R.id.buttonCancel)
	public void cancelLog() {
		setResult(Activity.RESULT_CANCELED);
		this.onBackPressed();
	}

	/*
	Return false if input is invalid.
	 */
	private boolean validateInput() {
		String element = spinnerElement.getSelectedItem().toString();
		String type = spinnerType.getSelectedItem().toString();

		if (element.equalsIgnoreCase(getString(R.string.text_fuel))) {
			if (TextUtils.isEmpty(editTextFuel.getText().toString())) {
				new MaterialDialog.Builder(this)
						.title(R.string.dialog_error)
						.content(R.string.text_fuel_amount_required)
						.positiveText(R.string.button_ok)
						.show();

				return false;
			}
		}

		if (TextUtils.isEmpty(editTextPrice.getText().toString())) {
			new MaterialDialog.Builder(this)
					.title(R.string.dialog_error)
					.content(R.string.text_price_required)
					.positiveText(R.string.button_ok)
					.show();

			return false;
		}

		Double priceCheck = Double.parseDouble(editTextPrice.getText().toString());
		Double priceLimit = 99999.99;
		if (priceCheck > priceLimit) {
			DecimalFormat df = new DecimalFormat("#.00");

			new MaterialDialog.Builder(this)
					.title(R.string.dialog_error)
					.content(getString(R.string.text_price_invalid, df.format(priceLimit)))
					.positiveText(R.string.button_ok)
					.onPositive(new MaterialDialog.SingleButtonCallback() {
						@Override
						public void onClick(@NonNull MaterialDialog dialog,
						                    @NonNull DialogAction which) {
							editTextPrice.requestFocus();
						}
					})
					.show();

			return false;
		}

		if (TextUtils.isEmpty(editTextOdometer.getText().toString())) {
			new MaterialDialog.Builder(this)
					.title(R.string.dialog_error)
					.content(R.string.text_odo_required)
					.positiveText(R.string.button_ok)
					.show();

			return false;
		}

		return true;
	}

	@OnClick(R.id.buttonCreate)
	public void createLog() {
		String vehicle = "default";
		String element = spinnerElement.getSelectedItem().toString();
		String type = spinnerType.getSelectedItem().toString();
		int odometer = 0;
		double fuelAmount;
		double price;
		double consumption;
		String fuelAmtStr = editTextFuel.getText().toString();
		String notes = editTextMemo.getText().toString();
		String dateStr;

		// validate price input first

		if (!validateInput()) {
			return;
		}

		if (mainlogSource == null) {
			mainlogSource = new MainLogSource(this);
		}

		// get date of this log
		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
		try {
			dateStr = sdf2.format(sdf.parse(buttonDate.getText().toString()));
		} catch (ParseException e) {
			e.printStackTrace();
			Toast.makeText(this, getString(R.string.error_date), Toast.LENGTH_LONG);
			return;
		}

		// calculate fuel amount
		if (element.equals(getString(R.string.text_fuel)) && !TextUtils.isEmpty(fuelAmtStr)) {
			try {
				fuelAmount = Math.round(Double.parseDouble(fuelAmtStr) * 100.0) / 100.0;
			} catch (Exception e) { // should never happen due to inputType restriction
				e.printStackTrace();
				Toast.makeText(this, "Value " + fuelAmtStr + " not a number!", Toast.LENGTH_LONG);
				return;
			}
		} else {
			fuelAmount = 0;
		}

		// get price
		if (TextUtils.isEmpty(editTextPrice.getText().toString())) {
			price = 0;
		} else {
			try {
				price = Double.parseDouble(editTextPrice.getText().toString());
			} catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(this, getString(R.string.error_price), Toast.LENGTH_LONG).show();
				return;
			}
		}

		int cashType = Integer.parseInt(sharedPrefs.getString("pref_CashType", "0"));

		if (cashType == 0 && element.equals(getString(R.string.text_fuel))) {
			price = price * fuelAmount;
			price = Math.round(price * 100.0) / 100.0;
		}

		// get odometer reading
		if (!TextUtils.isEmpty(editTextOdometer.getText().toString())) {
			try {
				odometer = Integer.parseInt(editTextOdometer.getText().toString());
			} catch (Exception e) { // should never happen due to inputType restriction
				e.printStackTrace();
				Toast.makeText(this, getString(R.string.error_odometer), Toast.LENGTH_LONG).show();
			}
		}

		// calculate consumption
		if (fuelAmount == 0) {
			consumption = 0;
		} else {
			consumption = calculateConsumption(vehicle,
					spinnerElement.getItemAtPosition(0).toString(),
					fuelAmount,
					odometer);
			consumption = Math.round(consumption * 100.0) / 100.0;
		}

		// add/update database
		if (item != null && isModification) { // modification
			item = new MaintenanceItem(item.getKey(), vehicle, element, type, fuelAmount,
					consumption, dateStr, odometer, notes, MyListFragment.mileageType, price);

			mainlogSource.updateEntry(item);

			setResult(Activity.RESULT_OK);
			Toast.makeText(
					this, getString(R.string.text_log_entry_updated), Toast.LENGTH_SHORT).show();
		} else { // creation
			item = new MaintenanceItem(vehicle, element, type, fuelAmount, consumption, dateStr,
					odometer, notes, MyListFragment.mileageType, price);

			mainlogSource.addMaintenanceItem(item);

			setResult(Activity.RESULT_OK);
			Toast.makeText(
					this, getString(R.string.text_log_entry_added), Toast.LENGTH_SHORT).show();
		}

		// add reminder if required
		if (checkBoxReminder.isChecked()) {
			Date dateInserted = new Date();
			ReminderItem remItem;

			if (!checkBoxDate.isChecked()) {
				remItem = new ReminderItem(vehicle, element, type, 0,
						editTextReminderInterval.getText().toString(), "km",
						String.valueOf(odometer), notes, dateInserted.toString());
			} else {
				remItem = new ReminderItem(vehicle, element, type, 1,
						spinnerReminderIntervalDateNumber.getSelectedItem().toString(),
						spinnerReminderIntervalDateType.getSelectedItem().toString(),
						String.valueOf(odometer), notes, dateInserted.toString());
			}

			Intent intent = new Intent(this, NewRem.class);
			intent.putExtra("ReminderItem", remItem);
			intent.putExtra("isModification", false);
			startActivity(intent);

			// if checkbox is checked (new reminder) and isModification is false and
			// we have remItem <> null, delete other reminder
			if (!isModification && remItemSent != null) {
				RemLogSource rLS = new RemLogSource(this);
				rLS.deleteEntry(remItemSent);
			}
		} else {
			// if checkbox is not checked ( no new reminder ) and isModification is false and
			// we have remItem <> null, regen other reminder with new interval
			if (!isModification && remItemSent != null) {

				RemLogSource rLS = new RemLogSource(this);

				Bundle b = new Bundle();
				b.putStringArray("intervalSizeArray", getResources().getStringArray(
					R.array.intervalTypeArray));
				b.putStringArray("intervalSizeArrayP", getResources().getStringArray(
					R.array.intervalTypeArrayP));

				NewRemDialog newRemDialog = new NewRemDialog();
				newRemDialog.setArguments(b);
				if (remItemSent.getReminderType() == 0) {
					remItemSent.setLastInterval(String.valueOf(odometer));
				} else {
					remItemSent.setLastInterval(dateStr);
				}

				remItemSent = newRemDialog.setNextInterval(remItemSent);
				rLS.updateEntry(remItemSent);
				Toast.makeText(this, getString(R.string.text_reminder_regenerated),
						Toast.LENGTH_SHORT).show();
			}
		}
		finish();
	}

	public void setValues() {

		if (isModification) {
			buttonCreate.setText(getString(R.string.button_update));
			if (getSupportActionBar() != null) {
				getSupportActionBar().setTitle(getString(R.string.text_update_log_entry));
			}
		}

		spinnerElement.setSelection(adapterSpinnerElement.getPosition(item.getMaintElem()));

		spinnerType.setSelection(adapterSpinnerType.getPosition(item.getMaintType()));

		DecimalFormat df = new DecimalFormat("####.00");

		if (item.getMaintElem().equals(getString(R.string.element_type_fuel))) {
			editTextFuel.setText(df.format(item.getFuelAmount()));
		} else {
			editTextFuel.setVisibility(View.GONE);
		}

		editTextPrice.setText(df.format(item.getCash()));
		editTextPrice.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					if(editTextPrice.getText().toString().equals("0.0")){
						editTextPrice.setText("");
					} 
				}
			}
		});

		int cashType = Integer.parseInt(sharedPrefs.getString("pref_CashType", "0"));

		if (cashType == 0 && item.getMaintElem().equals("Fuel")) {
			double cash = item.getCash() / item.getFuelAmount();
			cash = Math.round(cash * 100.0) / 100.0;
			editTextPrice.setText(String.valueOf(cash));
		}

		editTextOdometer.setText(String.valueOf(item.getOdometer()));
		editTextOdometer.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					if(editTextOdometer.getText().toString().equals("0")) {
						editTextOdometer.setText("");
					} 
				}
			}
		});

		editTextMemo.setText(item.getDetails());

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
		try {
			initialiseDatePicker(sdf.parse(item.getDate()));
		} catch (ParseException e) {
			Log.e(TAG, "Unable to parse item date, wrong format?", e);
			Toast.makeText(this, R.string.error_corrupt_data, Toast.LENGTH_LONG).show();
			finish();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_newlog, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.menu_elem:
				Intent intent1 = new Intent(this, DeleteElem.class);
				intent1.putExtra("type", NewRem.ELEMVAL);
				startActivityForResult(intent1, DELETEELEM);
				break;
			case R.id.menu_add_maintenance_type:
				Intent intent2 = new Intent(this, DeleteElem.class);
				intent2.putExtra("type", NewRem.ELEMTYPEVAL);
				startActivityForResult(intent2,DELETEELEM);
				break;
			case R.id.menu_clear_preferences:
				intervalPref.edit().clear().apply();
				elemTypePref.edit().clear().apply();
				elemPref.edit().clear().apply();

				setPreferences();
				populateSpinnerElement(-1);
				populateSpinnerType(-1);
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	public double calculateConsumption(String vehicle, String element, double fuel, int odometer) {
		Cursor cursor;
		if (!isModification) {
			cursor = mainlogSource.getLastItem(vehicle, element);
		} else {
			cursor = mainlogSource.getItemAtKey(Integer.toString(item.getKey()), element);
			if (cursor.getCount() > 1) { // modification
				cursor.moveToNext();
			} else {
				return 0;
			}
		}

		if (cursor != null) {
			double consumption;
			if (MyListFragment.mileageType == 0) {
				consumption = 100 * fuel /
						(odometer - cursor.getDouble(cursor.getColumnIndex(MotoLogHelper.FIELD7)));
			} else {
				if (MyListFragment.mileageType == 2) {
					consumption = fuel /
							(odometer - cursor.getDouble(cursor.getColumnIndex(MotoLogHelper.FIELD7)));
				} else { // adica si pe 1 ( mpg ) si pe 3 (km/l)
					consumption = (odometer -
							cursor.getDouble(cursor.getColumnIndex(MotoLogHelper.FIELD7))) / fuel;
				}
			}

			if (consumption < 0) {
				return 0;
			} else {
				return consumption;
			}
		}
		return 0;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult");
		Log.d(TAG, "requestCode: " + requestCode + " resultCode: " + resultCode);

		if (requestCode == DELETEELEM && resultCode == 1) {
			setPreferences();
			populateSpinnerElement(-1);
			populateSpinnerType(-1);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		EventBus.getDefault().register(this);
	}

	@Override
	protected void onStop() {
		EventBus.getDefault().unregister(this);
		super.onStop();
	}

	@Override
	public void addNewSharedPreference(String preferenceType, String value) {

		switch (preferenceType) {
			case NewLogActivity.ELEMVAL:
				int elemCount = elemPref.getInt(NewLogActivity.ELEMCOUNTSTRING, 0);

				if (elemCount == 0) {
					Toast.makeText(this,
							"Something wrong with SharedPreferences", Toast.LENGTH_LONG)
							.show();
				} else {
					elemPref.edit().putString(NewLogActivity.ELEMVAL + elemCount, value).apply();
					elemCount++;
					elemPref.edit().putInt(NewLogActivity.ELEMCOUNTSTRING, elemCount).apply();

					setPreferences();
					populateSpinnerElement(elemCount);
					populateSpinnerType(-1);
				}
				break;
			case NewLogActivity.ELEMTYPEVAL:
				int elemTypeCount = elemTypePref.getInt(NewLogActivity.ELEMTYPECOUNTSTRING, 0);

				if (elemTypeCount == 0) {
					Toast.makeText(this,
							"Something wrong with SharedPreferences on "
									+ NewLogActivity.ELEMTYPECOUNTSTRING, Toast.LENGTH_LONG)
							.show();
				} else {
					elemTypePref.edit()
							.putString(NewLogActivity.ELEMTYPEVAL + elemTypeCount, value).apply();
					elemTypeCount++;
					elemTypePref.edit().putInt(NewLogActivity.ELEMTYPECOUNTSTRING, elemTypeCount)
							.apply();
					setPreferences();
					populateSpinnerType(elemTypeCount);
				}
				break;
			default:
				setPreferences();
				break;
		}
	}
}
