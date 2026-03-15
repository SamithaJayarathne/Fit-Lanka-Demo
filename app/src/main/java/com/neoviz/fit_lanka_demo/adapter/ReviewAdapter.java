package com.neoviz.fit_lanka_demo.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.neoviz.fit_lanka_demo.R;
import com.neoviz.fit_lanka_demo.model.Feedback;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private final List<Feedback> reviewList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public ReviewAdapter(List<Feedback> reviewList) {
        this.reviewList = reviewList;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Feedback feedback = reviewList.get(position);

        if (feedback.getUserName() != null && !feedback.getUserName().isEmpty()) {
            holder.tvUserName.setText(feedback.getUserName());
        } else {
            holder.tvUserName.setText("Verified Buyer");
        }

        holder.ratingBar.setRating(feedback.getRating());

        holder.tvReviewText.setText(feedback.getReview());

        if (feedback.getTimestamp() != null) {
            String dateStr = dateFormat.format(feedback.getTimestamp().toDate());
            holder.tvReviewDate.setText(dateStr);
        }
    }

    @Override
    public int getItemCount() {
        return reviewList != null ? reviewList.size() : 0;
    }

    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvReviewText, tvReviewDate;
        RatingBar ratingBar;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tv_review_user_name);
            tvReviewText = itemView.findViewById(R.id.tv_review_text);
            tvReviewDate = itemView.findViewById(R.id.tv_review_date);
            ratingBar = itemView.findViewById(R.id.review_rating_bar);
        }
    }
}