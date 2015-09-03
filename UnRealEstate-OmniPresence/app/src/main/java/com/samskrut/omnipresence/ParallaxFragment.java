/**
 * This file is used for displaying the Instructions screens with a parallax effect.
 * It is from a Parallax Library 'ParallaxPagerTransformer'. It doesn't really have anything that I have modified.
 * It's just predefined code. I have commented the one place that I have modified.
 */
package com.samskrut.omnipresence;

import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class ParallaxFragment extends Fragment {

    private ParallaxAdapter mCatsAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_parallax, container, false);
        final ImageView image = (ImageView) v.findViewById(R.id.image);

        //'pos' contains the R.java resource code for the current instruction image (R.mipmap.something).
        int pos = getArguments().getInt("pos");
        image.setImageResource(pos);


        image.post(new Runnable() {
            @Override
            public void run() {
                Matrix matrix = new Matrix();
                matrix.reset();
                float wv = image.getWidth();
                float hv = image.getHeight();
                float wi = image.getDrawable().getIntrinsicWidth();
                float hi = image.getDrawable().getIntrinsicHeight();
                float width = wv;
                float height = hv;
                if (wi / wv > hi / hv) {
                    matrix.setScale(hv / hi, hv / hi);
                    width = wi * hv / hi;
                } else {
                    matrix.setScale(wv / wi, wv / wi);
                    height= hi * wv / wi;
                }
                matrix.preTranslate((wv - width) / 2, (hv - height) / 2);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setImageMatrix(matrix);
            }
        });
        return v;
    }

    public void setAdapter(ParallaxAdapter catsAdapter) {
        mCatsAdapter = catsAdapter;
    }
}