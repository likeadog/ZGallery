package com.zhuang.library;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by zhuang on 2015/9/1.
 */
public class ImageModel implements Parcelable {

    private int id;
    private String path;//照片路径
    private Boolean checked = false;//是否被选中

    public ImageModel() {
    }

    public ImageModel(int id,String path) {
        this.id = id;
        this.path = path;
    }

    public ImageModel(int id, String path, Boolean checked) {
        this.id = id;
        this.path = path;
        this.checked = checked;
    }

    protected ImageModel(Parcel in) {
        id = in.readInt();
        path = in.readString();
        checked = in.readInt()==1?true:false;
    }

    public static final Creator<ImageModel> CREATOR = new Creator<ImageModel>() {
        @Override
        public ImageModel createFromParcel(Parcel in) {
            return new ImageModel(in);
        }

        @Override
        public ImageModel[] newArray(int size) {
            return new ImageModel[size];
        }
    };

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Boolean getChecked() {
        return checked;
    }

    public void setChecked(Boolean checked) {
        this.checked = checked;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(path);
        parcel.writeInt(checked?1:0);
    }
}
