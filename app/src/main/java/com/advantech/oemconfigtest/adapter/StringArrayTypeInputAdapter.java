package com.advantech.oemconfigtest.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import com.advantech.oemconfigtest.R;
import java.util.ArrayList;
import java.util.List;

public class StringArrayTypeInputAdapter extends RecyclerView.Adapter<StringArrayTypeInputAdapter.ViewHolder> {

    private List<String> mStringList;

    public StringArrayTypeInputAdapter() {
        mStringList = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext()).inflate(R.layout.string_array_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.stringValue.setText(mStringList.get(position));
        if (holder.textWatcher != null) {
            holder.stringValue.removeTextChangedListener(holder.textWatcher);
        }
        holder.textWatcher = createEditTextTextWatcher(holder);
        holder.stringValue.addTextChangedListener(holder.textWatcher);
        holder.delete.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int adapterPosition = holder.getAdapterPosition();
                        // make sure the view is not removed yet.
                        if (adapterPosition != -1) {
                            mStringList.remove(adapterPosition);
                            notifyItemRemoved(adapterPosition);
                        }
                    }
                });
    }

    @Override
    public int getItemCount() {
        return mStringList.size();
    }

    public List<String> getStringList() {
        return mStringList;
    }

    public void setStringList(List<String> stringList) {
        mStringList = stringList;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public EditText stringValue;
        public ImageView delete;
        public TextWatcher textWatcher;

        public ViewHolder(View view) {
            super(view);
            stringValue = (EditText) view.findViewById(R.id.string_input);
            delete = (ImageView) view.findViewById(R.id.delete_row);
        }
    }

    private TextWatcher createEditTextTextWatcher(final ViewHolder viewHolder) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                mStringList.set(viewHolder.getAdapterPosition(), editable.toString());
            }
        };
    }
}
