package com.advantech.oemconfigtest.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.RestrictionEntry;
import android.content.RestrictionsManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.advantech.oemconfigtest.DeviceAdminReceiver;
import com.advantech.oemconfigtest.R;
import com.advantech.oemconfigtest.utils.Utils;
import com.advantech.oemconfigtest.adapter.EditDeleteArrayAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class MainFragment extends Fragment
        implements View.OnClickListener, EditDeleteArrayAdapter.OnEditButtonClickListener<RestrictionEntry>{
    public static final String FRAGMENT_TAG = "MainFragment";
    private static final String TAG = "oemconfigtest";

    private static final int[] SUPPORTED_TYPES = {
            KeyValuePairDialogFragment.DialogType.BOOL_TYPE,
            KeyValuePairDialogFragment.DialogType.INT_TYPE,
            KeyValuePairDialogFragment.DialogType.STRING_TYPE,
            KeyValuePairDialogFragment.DialogType.STRING_ARRAY_TYPE,
            KeyValuePairDialogFragment.DialogType.BUNDLE_TYPE,
            KeyValuePairDialogFragment.DialogType.BUNDLE_ARRAY_TYPE
    };
    private static final int[] SUPPORTED_TYPES_PRE_M = {
            KeyValuePairDialogFragment.DialogType.BOOL_TYPE,
            KeyValuePairDialogFragment.DialogType.INT_TYPE,
            KeyValuePairDialogFragment.DialogType.STRING_TYPE,
            KeyValuePairDialogFragment.DialogType.STRING_ARRAY_TYPE,
    };

    private String packageName = "com.advantech.oemconfig";

    private List<RestrictionEntry> mRestrictionEntries = new ArrayList<>();
    private List<RestrictionEntry> mLastRestrictionEntries;

    private DevicePolicyManager mDevicePolicyManager;
    private RestrictionsManager mRestrictionsManager;

    private EditDeleteArrayAdapter<RestrictionEntry> mAppRestrictionsArrayAdapter;
    private ListView listView;

    private RestrictionEntry mEditingRestrictionEntry;
    private ComponentName mAdminComponent;

    private static final int RESULT_CODE_EDIT_DIALOG = 1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDevicePolicyManager =
                (DevicePolicyManager) getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        mRestrictionsManager =
                (RestrictionsManager) getActivity().getSystemService(Context.RESTRICTIONS_SERVICE);
        if (Utils.hasDelegation(getActivity(), DevicePolicyManager.DELEGATION_APP_RESTRICTIONS)) {
            mAdminComponent = null;
        } else {
            mAdminComponent = DeviceAdminReceiver.getComponentName(getActivity());
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_main, container, false);
        listView = view.findViewById(R.id.listview);
        mAppRestrictionsArrayAdapter = new RestrictionEntryEditDeleteArrayAdapter(getActivity(), mRestrictionEntries, this, null);
        listView.setAdapter(mAppRestrictionsArrayAdapter);
        view.findViewById(R.id.save_app).setOnClickListener(this);
        view.findViewById(R.id.reset_app).setOnClickListener(this);
        view.findViewById(R.id.load_default_button).setOnClickListener(this);

        if (mDevicePolicyManager.isAdminActive(mAdminComponent)) {
            Bundle bundle = mDevicePolicyManager.getApplicationRestrictions(mAdminComponent, packageName);
                loadAppRestrictionsList(convertBundleToRestrictions(bundle));
            mLastRestrictionEntries = new ArrayList<>(mRestrictionEntries);
            loadDefault();
        } else {
            showToast("Please obtain device administrator privileges.");
            loadDefault();
        }

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        RestrictionEntry newRestrictionEntry;
        switch (requestCode) {
            case RESULT_CODE_EDIT_DIALOG:
                int type = result.getIntExtra(KeyValuePairDialogFragment.RESULT_TYPE, 0);
                String key = result.getStringExtra(KeyValuePairDialogFragment.RESULT_KEY);
                newRestrictionEntry = new RestrictionEntry(getRestrictionTypeFromDialogType(type), key);
                updateRestrictionEntryFromResultIntent(newRestrictionEntry, result);
                mAppRestrictionsArrayAdapter.remove(mEditingRestrictionEntry);
                mEditingRestrictionEntry = null;
                mAppRestrictionsArrayAdapter.add(newRestrictionEntry);
                break;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void updateRestrictionEntryFromResultIntent(
            RestrictionEntry restrictionEntry, Intent intent) {
        switch (restrictionEntry.getType()) {
            case RestrictionEntry.TYPE_BOOLEAN:
                restrictionEntry.setSelectedState(intent.getBooleanExtra(KeyValuePairDialogFragment.RESULT_VALUE, false));
                break;
            case RestrictionEntry.TYPE_INTEGER:
                restrictionEntry.setIntValue(intent.getIntExtra(KeyValuePairDialogFragment.RESULT_VALUE, 0));
                break;
            case RestrictionEntry.TYPE_STRING:
                restrictionEntry.setSelectedString(intent.getStringExtra(KeyValuePairDialogFragment.RESULT_VALUE));
                break;
            case RestrictionEntry.TYPE_MULTI_SELECT:
                restrictionEntry.setAllSelectedStrings(intent.getStringArrayExtra(KeyValuePairDialogFragment.RESULT_VALUE));
                break;
            case RestrictionEntry.TYPE_BUNDLE: {
                Bundle bundle = intent.getBundleExtra(KeyValuePairDialogFragment.RESULT_VALUE);
                restrictionEntry.setRestrictions(convertBundleToRestrictions(bundle));
                break;
            }
            case RestrictionEntry.TYPE_BUNDLE_ARRAY: {
                Parcelable[] bundleArray = intent.getParcelableArrayExtra(KeyValuePairDialogFragment.RESULT_VALUE);
                RestrictionEntry[] restrictionEntryArray = new RestrictionEntry[bundleArray.length];
                for (int i = 0; i < bundleArray.length; i++) {
                    restrictionEntryArray[i] =
                            RestrictionEntry.createBundleEntry(
                                    String.valueOf(i), convertBundleToRestrictions((Bundle) bundleArray[i]));
                }
                restrictionEntry.setRestrictions(restrictionEntryArray);
                break;
            }
        }
    }

    private int getRestrictionTypeFromDialogType(int typeIndex) {
        switch (typeIndex) {
            case KeyValuePairDialogFragment.DialogType.BOOL_TYPE:
                return RestrictionEntry.TYPE_BOOLEAN;
            case KeyValuePairDialogFragment.DialogType.INT_TYPE:
                return RestrictionEntry.TYPE_INTEGER;
            case KeyValuePairDialogFragment.DialogType.STRING_TYPE:
                return RestrictionEntry.TYPE_STRING;
            case KeyValuePairDialogFragment.DialogType.STRING_ARRAY_TYPE:
                return RestrictionEntry.TYPE_MULTI_SELECT;
            case KeyValuePairDialogFragment.DialogType.BUNDLE_TYPE:
                return RestrictionEntry.TYPE_BUNDLE;
            case KeyValuePairDialogFragment.DialogType.BUNDLE_ARRAY_TYPE:
                return RestrictionEntry.TYPE_BUNDLE_ARRAY;
            default:
                throw new AssertionError("Unknown type index");
        }
    }

    private RestrictionEntry[] convertBundleToRestrictions(Bundle restrictionBundle) {
        List<RestrictionEntry> restrictionEntries = new ArrayList<>();
        Set<String> keys = restrictionBundle.keySet();
        for (String key : keys) {
            Object value = restrictionBundle.get(key);
            if (value instanceof Boolean) {
                restrictionEntries.add(new RestrictionEntry(key, (boolean) value));
            } else if (value instanceof Integer) {
                restrictionEntries.add(new RestrictionEntry(key, (int) value));
            } else if (value instanceof String) {
                RestrictionEntry entry = new RestrictionEntry(RestrictionEntry.TYPE_STRING, key);
                entry.setSelectedString((String) value);
                restrictionEntries.add(entry);
            } else if (value instanceof String[]) {
                restrictionEntries.add(new RestrictionEntry(key, (String[]) value));
            } else if (value instanceof Bundle) {
                addBundleEntryToRestrictions(restrictionEntries, key, (Bundle) value);
            } else if (value instanceof Parcelable[]) {
                addBundleArrayToRestrictions(restrictionEntries, key, (Parcelable[]) value);
            }
        }
        return restrictionEntries.toArray(new RestrictionEntry[0]);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void addBundleEntryToRestrictions(
            List<RestrictionEntry> restrictionEntries, String key, Bundle value) {
        restrictionEntries.add(
                RestrictionEntry.createBundleEntry(key, convertBundleToRestrictions(value)));
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void addBundleArrayToRestrictions(
            List<RestrictionEntry> restrictionEntries, String key, Parcelable[] value) {
        int length = value.length;
        RestrictionEntry[] entriesArray = new RestrictionEntry[length];
        for (int i = 0; i < entriesArray.length; ++i) {
            entriesArray[i] =
                    RestrictionEntry.createBundleEntry(key, convertBundleToRestrictions((Bundle) value[i]));
        }
        restrictionEntries.add(RestrictionEntry.createBundleArrayEntry(key, entriesArray));
    }

    private void loadAppRestrictionsList(RestrictionEntry[] restrictionEntries) {
        if (restrictionEntries != null) {
            mAppRestrictionsArrayAdapter.clear();
            mAppRestrictionsArrayAdapter.addAll(Arrays.asList(restrictionEntries));
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.reset_app) {
            resetConfig();
        } else if (id == R.id.save_app) {
            saveConfig();
        } else if (id == R.id.load_default_button) {
            loadDefault();
        }
    }

    private void resetConfig() {
        mAppRestrictionsArrayAdapter.clear();
        mAppRestrictionsArrayAdapter.addAll(mLastRestrictionEntries);
    }

    private void saveConfig() {
        mDevicePolicyManager.setApplicationRestrictions(mAdminComponent, packageName,
                convertRestrictionsToBundle(mRestrictionEntries));
    }

    private Bundle convertRestrictionsToBundle(List<RestrictionEntry> entries) {
        final Bundle bundle = new Bundle();
        for (RestrictionEntry entry : entries) {
            addRestrictionToBundle(bundle, entry);
        }
        return bundle;
    }

    private Bundle addRestrictionToBundle(Bundle bundle, RestrictionEntry entry) {
        switch (entry.getType()) {
            case RestrictionEntry.TYPE_BOOLEAN:
                bundle.putBoolean(entry.getKey(), entry.getSelectedState());
                break;
            case RestrictionEntry.TYPE_CHOICE:
            case RestrictionEntry.TYPE_MULTI_SELECT:
                bundle.putStringArray(entry.getKey(), entry.getAllSelectedStrings());
                break;
            case RestrictionEntry.TYPE_INTEGER:
                bundle.putInt(entry.getKey(), entry.getIntValue());
                break;
            case RestrictionEntry.TYPE_STRING:
            case RestrictionEntry.TYPE_NULL:
                bundle.putString(entry.getKey(), entry.getSelectedString());
                break;
            case RestrictionEntry.TYPE_BUNDLE:
                addBundleRestrictionToBundle(bundle, entry);
                break;
            case RestrictionEntry.TYPE_BUNDLE_ARRAY:
                addBundleArrayRestrictionToBundle(bundle, entry);
                break;
            default:
                throw new IllegalArgumentException("Unsupported restrictionEntry type: " + entry.getType());
        }
        return bundle;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void addBundleRestrictionToBundle(Bundle bundle, RestrictionEntry entry) {
        if (Utils.SDK_INT >= Build.VERSION_CODES.M) {
            RestrictionEntry[] restrictions = entry.getRestrictions();
            Bundle childBundle = convertRestrictionsToBundle(Arrays.asList(restrictions));
            bundle.putBundle(entry.getKey(), childBundle);
        } else {
            Log.w(TAG, "addBundleRestrictionToBundle is called in pre-M");
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void addBundleArrayRestrictionToBundle(Bundle bundle, RestrictionEntry entry) {
        if (Utils.SDK_INT >= Build.VERSION_CODES.M) {
            RestrictionEntry[] bundleRestrictionArray = entry.getRestrictions();
            Bundle[] bundleArray = new Bundle[bundleRestrictionArray.length];
            for (int i = 0; i < bundleRestrictionArray.length; i++) {
                RestrictionEntry[] bundleRestrictions = bundleRestrictionArray[i].getRestrictions();
                if (bundleRestrictions == null) {
                    // Non-bundle entry found in bundle array.
                    Log.w(TAG, "addRestrictionToBundle: " + "Non-bundle entry found in bundle array");
                    bundleArray[i] = new Bundle();
                } else {
                    bundleArray[i] = convertRestrictionsToBundle(Arrays.asList(bundleRestrictions));
                }
            }
            bundle.putParcelableArray(entry.getKey(), bundleArray);
        } else {
            Log.w(TAG, "addBundleArrayRestrictionToBundle is called in pre-M");
        }
    }

    private void addNewRow() {

    }

    private void loadDefault() {
        loadManifestAppRestrictions(packageName);
    }

    private void loadManifestAppRestrictions(String pkgName) {
        if (!TextUtils.isEmpty(pkgName)) {
            List<RestrictionEntry> manifestRestrictions = null;
            try {
                manifestRestrictions = mRestrictionsManager.getManifestRestrictions(pkgName);
                convertTypeChoiceAndNullToString(manifestRestrictions);
            } catch (NullPointerException e) {
                // This means no default restrictions.
            }
            if (manifestRestrictions != null) {
                loadAppRestrictionsList(manifestRestrictions.toArray(new RestrictionEntry[0]));
            }
        }
    }

    private void convertTypeChoiceAndNullToString(List<RestrictionEntry> restrictionEntries) {
        for (RestrictionEntry entry : restrictionEntries) {
            if (entry.getType() == RestrictionEntry.TYPE_CHOICE
                    || entry.getType() == RestrictionEntry.TYPE_NULL) {
                entry.setType(RestrictionEntry.TYPE_STRING);
            }
        }
    }

    @Override
    public void onEditButtonClick(RestrictionEntry restrictionEntry) {
        showEditDialog(restrictionEntry);
    }

    private void showEditDialog(final RestrictionEntry restrictionEntry) {
        mEditingRestrictionEntry = restrictionEntry;
        int type = KeyValuePairDialogFragment.DialogType.BOOL_TYPE;
        Object value = null;
        String key = "";
        if (restrictionEntry != null) {
            key = restrictionEntry.getKey();
            type = getTypeIndexFromRestrictionType(restrictionEntry.getType());
            switch (restrictionEntry.getType()) {
                case RestrictionEntry.TYPE_BOOLEAN:
                    value = restrictionEntry.getSelectedState();
                    break;
                case RestrictionEntry.TYPE_INTEGER:
                    value = restrictionEntry.getIntValue();
                    break;
                case RestrictionEntry.TYPE_STRING:
                    value = restrictionEntry.getSelectedString();
                    break;
                case RestrictionEntry.TYPE_MULTI_SELECT:
                    value = restrictionEntry.getAllSelectedStrings();
                    break;
                case RestrictionEntry.TYPE_BUNDLE:
                    value = convertRestrictionsToBundle(Arrays.asList(getRestrictionEntries(restrictionEntry)));
                    break;
                case RestrictionEntry.TYPE_BUNDLE_ARRAY:
                    RestrictionEntry[] restrictionEntries = getRestrictionEntries(restrictionEntry);
                    Bundle[] bundles = new Bundle[restrictionEntries.length];
                    for (int i = 0; i < restrictionEntries.length; i++) {
                        bundles[i] = convertRestrictionsToBundle(Arrays.asList(getRestrictionEntries(restrictionEntries[i])));
                    }
                    value = bundles;
                    break;
            }
        }
        int[] supportType = (Utils.SDK_INT < Build.VERSION_CODES.M) ? SUPPORTED_TYPES_PRE_M : SUPPORTED_TYPES;
        KeyValuePairDialogFragment dialogFragment =
                KeyValuePairDialogFragment.newInstance(
                        type, true, key, value, supportType, getCurrentAppName());
        dialogFragment.setTargetFragment(MainFragment.this, RESULT_CODE_EDIT_DIALOG);
        dialogFragment.show(getFragmentManager(), "dialog");
    }

    private String getCurrentAppName() {
        return "OEMConfig";
    }

    @TargetApi(Build.VERSION_CODES.M)
    private RestrictionEntry[] getRestrictionEntries(RestrictionEntry restrictionEntry) {
        return restrictionEntry.getRestrictions();
    }

    private int getTypeIndexFromRestrictionType(int restrictionType) {
        switch (restrictionType) {
            case RestrictionEntry.TYPE_BOOLEAN:
                return KeyValuePairDialogFragment.DialogType.BOOL_TYPE;
            case RestrictionEntry.TYPE_INTEGER:
                return KeyValuePairDialogFragment.DialogType.INT_TYPE;
            case RestrictionEntry.TYPE_STRING:
                return KeyValuePairDialogFragment.DialogType.STRING_TYPE;
            case RestrictionEntry.TYPE_MULTI_SELECT:
                return KeyValuePairDialogFragment.DialogType.STRING_ARRAY_TYPE;
            case RestrictionEntry.TYPE_BUNDLE:
                return KeyValuePairDialogFragment.DialogType.BUNDLE_TYPE;
            case RestrictionEntry.TYPE_BUNDLE_ARRAY:
                return KeyValuePairDialogFragment.DialogType.BUNDLE_ARRAY_TYPE;
            default:
                throw new AssertionError("Unknown restriction type");
        }
    }

    private void showToast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
    }

    public class RestrictionEntryEditDeleteArrayAdapter
            extends EditDeleteArrayAdapter<RestrictionEntry> {

        public RestrictionEntryEditDeleteArrayAdapter(
                Context context,
                List<RestrictionEntry> entries,
                OnEditButtonClickListener<RestrictionEntry> onEditButtonClickListener,
                OnDeleteButtonClickListener<RestrictionEntry> onDeleteButtonClickListener) {
            super(context, entries, onEditButtonClickListener, onDeleteButtonClickListener);
        }

        @Override
        protected String getDisplayName(RestrictionEntry entry) {
            return entry.getKey();
        }
    }
}
