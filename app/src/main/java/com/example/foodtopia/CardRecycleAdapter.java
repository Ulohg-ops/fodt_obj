package com.example.foodtopia;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CardRecycleAdapter extends RecyclerView.Adapter<CardRecycleAdapter.ViewHolder> {
    private Context context;
    private List<Member> memberList;

    CardRecycleAdapter(Context context, List<Member> memberList) {
        this.context = context;
        this.memberList = memberList;
    }


//當現有的ViewHolder不夠用時，要求Adapter產生一個新的
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.restaurant_cardview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Member member = memberList.get(position);
        holder.imageId.setImageResource(member.getImage());
        holder.textName.setText(member.getName());

        if (position % 6 == 0) {
            holder.linearBg.setBackgroundColor(ContextCompat.getColor(context, R.color.cv_c1));
        } else if (position % 6 == 1) {
            holder.linearBg.setBackgroundColor(ContextCompat.getColor(context, R.color.cv_c2));
        } else if (position % 6 == 2) {
            holder.linearBg.setBackgroundColor(ContextCompat.getColor(context, R.color.cv_c3));
        } else if (position % 6 == 3) {
            holder.linearBg.setBackgroundColor(ContextCompat.getColor(context, R.color.cv_c4));
        } else if(position % 6 == 4){
            holder.linearBg.setBackgroundColor(ContextCompat.getColor(context, R.color.cv_c5));
        }else if(position % 6 ==  5){
            holder.linearBg.setBackgroundColor(ContextCompat.getColor(context, R.color.cv_c6));
        }
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    //Adapter 需要一個 ViewHolder，只要實作它的 constructor 就好，保存起來的view會放在itemView裡面
    class ViewHolder extends RecyclerView.ViewHolder{
        ImageView imageId;
        LinearLayout linearBg;
        TextView textId, textName;
        ViewHolder(View itemView) {
            super(itemView);
            imageId = (ImageView) itemView.findViewById(R.id.imageId);
            textName = (TextView) itemView.findViewById(R.id.textName);
            linearBg=(LinearLayout) itemView.findViewById(R.id.bg);
        }
    }
}