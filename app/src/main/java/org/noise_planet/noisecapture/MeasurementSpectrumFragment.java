/*
 * This file is part of the NoiseCapture application and OnoMap system.
 *
 * The 'OnoMaP' system is led by Lab-STICC and Ifsttar and generates noise maps via
 * citizen-contributed noise data.
 *
 * This application is co-funded by the ENERGIC-OD Project (European Network for
 * Redistributing Geospatial Information to user Communities - Open Data). ENERGIC-OD
 * (http://www.energic-od.eu/) is partially funded under the ICT Policy Support Programme (ICT
 * PSP) as part of the Competitiveness and Innovation Framework Programme by the European
 * Community. The application work is also supported by the French geographic portal GEOPAL of the
 * Pays de la Loire region (http://www.geopal.org).
 *
 * Copyright (C) IFSTTAR - LAE and Lab-STICC – CNRS UMR 6285 Equipe DECIDE Vannes
 *
 * NoiseCapture is a free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of
 * the License, or(at your option) any later version. NoiseCapture is distributed in the hope that
 * it will be useful,but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.You should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation,Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301  USA or see For more information,  write to Ifsttar,
 * 14-20 Boulevard Newton Cite Descartes, Champs sur Marne F-77447 Marne la Vallee Cedex 2 FRANCE
 *  or write to scientific.computing@ifsttar.fr
 */

package org.noise_planet.noisecapture;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;


/**
 * Spectrum content on measurement activity
 */
public class MeasurementSpectrumFragment extends Fragment {

    private View view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(view == null) {
            // Inflate the layout for this fragment
            view = inflater.inflate(R.layout.fragment_measurement_spectrum, container, false);
            BarChart sChart = (BarChart) view.findViewById(R.id.spectrumChart);
            sChart.setDrawBarShadow(false);
            sChart.setDescription("");
            sChart.getLegend().setEnabled(false);
            sChart.setTouchEnabled(false);
            sChart.setPinchZoom(false);
            sChart.setDrawGridBackground(false);
            sChart.setMaxVisibleValueCount(0);
            sChart.setHorizontalScrollBarEnabled(false);
            sChart.setVerticalScrollBarEnabled(false);
            sChart.setNoDataTextDescription(getText(R.string.no_data_text_description).toString());
            // XAxis parameters:
            XAxis xls = sChart.getXAxis();
            xls.setPosition(XAxis.XAxisPosition.BOTTOM);
            xls.setDrawAxisLine(true);
            xls.setDrawGridLines(false);
            xls.setDrawLabels(true);
            xls.setTextColor(Color.WHITE);
            xls.setAvoidFirstLastClipping(false);
            // YAxis parameters (left): main axis for dB values representation
            YAxis yls = sChart.getAxisLeft();
            yls.setDrawAxisLine(true);
            yls.setDrawGridLines(true);
            yls.setAxisMaxValue(100.f);
            yls.setAxisMinValue(0f);
            yls.setTextColor(Color.WHITE);
            yls.setGridColor(Color.WHITE);
            yls.setSpaceBottom(0);
            yls.setSpaceTop(0);
            yls.setValueFormatter(new SPLValueFormatter());
            // YAxis parameters (right): no axis, hide all
            YAxis yrs = sChart.getAxisRight();
            yrs.setEnabled(false);
        }
        return view;
    }
}
