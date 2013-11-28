package com.airAd.passtool.ui.ticket;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.airAd.passtool.data.model.Ticket;

/**
 * 票据的父类
 * @author pengfan
 *
 */
public abstract class Appearance extends Fragment {

    protected static final String TICKET_FLAG = "ticket";
    protected Ticket ticket;

    public void setTicket(Ticket ticket) {
        final Bundle args = new Bundle();
        args.putSerializable(TICKET_FLAG, ticket);
        setArguments(args);
    }

    public Ticket getTicket() {
        return ticket;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ticket = (Ticket) (getArguments() != null ? getArguments().getSerializable(TICKET_FLAG) : null);
    }

    public abstract void setForgroundColor(int color);

    public abstract void setLabelColor(int color);
}
