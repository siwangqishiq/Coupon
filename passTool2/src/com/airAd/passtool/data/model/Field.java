package com.airAd.passtool.data.model;

import java.io.Serializable;

/**
 * 单个区域内容
 * @author pengfan
 *
 */
public class Field implements Serializable {

    private static final long serialVersionUID = -7955587220319837078L;

    private String label;
    private String value;

    public Field(String label, String value) {
        super();
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
