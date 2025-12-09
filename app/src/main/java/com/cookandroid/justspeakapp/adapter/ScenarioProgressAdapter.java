package com.cookandroid.justspeakapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cookandroid.justspeakapp.R;
import com.cookandroid.justspeakapp.model.ScenarioProgress;

import java.util.List;

public class ScenarioProgressAdapter extends RecyclerView.Adapter<ScenarioProgressAdapter.ViewHolder> {
    private List<ScenarioProgress> scenarios;
    private OnScenarioClickListener listener;

    public interface OnScenarioClickListener {
        void onScenarioClick(ScenarioProgress scenario);
    }

    public ScenarioProgressAdapter(List<ScenarioProgress> scenarios) {
        this.scenarios = scenarios;
    }

    public void setOnScenarioClickListener(OnScenarioClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scenario_progress, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScenarioProgress scenario = scenarios.get(position);
        holder.bind(scenario);

        // 클릭 리스너 설정
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onScenarioClick(scenario);
            }
        });
    }

    @Override
    public int getItemCount() {
        return scenarios.size();
    }

    public void updateData(List<ScenarioProgress> newScenarios) {
        this.scenarios = newScenarios;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvScenarioName;
        TextView tvProgressText;
        TextView tvProgressPercent;
        ProgressBar progressScenario;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvScenarioName = itemView.findViewById(R.id.tv_scenario_name);
            tvProgressText = itemView.findViewById(R.id.tv_progress_text);
            tvProgressPercent = itemView.findViewById(R.id.tv_progress_percent);
            progressScenario = itemView.findViewById(R.id.progress_scenario);
        }

        void bind(ScenarioProgress scenario) {
            tvScenarioName.setText(scenario.getScenarioName());
            tvProgressText.setText(scenario.getCompletedLessons() + " / " + scenario.getTotalLessons());
            tvProgressPercent.setText(scenario.getProgressPercent() + "% 완료");
            progressScenario.setProgress(scenario.getProgressPercent());
        }
    }
}
