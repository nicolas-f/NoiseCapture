/*
 * 8-bit Galois Field
 *
 * Copyright 2015, Backblaze, Inc.  All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 *         of this software and associated documentation files (the "Software"), to deal
 *         in the Software without restriction, including without limitation the rights
 *         to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *         copies of the Software, and to permit persons to whom the Software is
 *         furnished to do so, subject to the following conditions:
 *
 *         The above copyright notice and this permission notice shall be included in all
 *         copies or substantial portions of the Software.
 *
 *         THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *         IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *         FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *         AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *         LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *         OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *         SOFTWARE.
 */

package org.orbisgis.sos.bandfilter;

public class BandFilter {
    static final int MAX_POLES = 2;
    static final double M_TWOPI = Math.PI * 2;

    double xv0, xv1, xv2;
    double yv0, yv1, yv2;

    double yc0;
    double yc1;
    double gain;

    public BandFilter(double sampleRate, double lowFreq, double highFreq) {

        Complex[] sPoles = new Complex[MAX_POLES];
        Complex[] sZeros = new Complex[MAX_POLES];
        Complex[] zPoles = new Complex[MAX_POLES];
        Complex[] zZeros = new Complex[MAX_POLES];
        Complex[] topCoefficients;
        Complex[] botCoefficients;

        double alpha1 = lowFreq / sampleRate;
        double alpha2 = highFreq / sampleRate;

        double warped_alpha1 = Math.tan(Math.PI * alpha1) / Math.PI;
        double warped_alpha2 = Math.tan(Math.PI * alpha2) / Math.PI;

        /* Calculate S poles */

        sPoles[0] = new Complex(-1.0, 0);

        /* Normalise S plane - 2 poles and 1 zero */

        double w1 = M_TWOPI * warped_alpha1;
        double w2 = M_TWOPI * warped_alpha2;

        double w0 = Math.sqrt(w1 * w2);
        double bw = w2 - w1;

        Complex hba = sPoles[0].mul(0.5 * bw);

        final Complex wo_hba = new Complex(w0, 0).div(hba);

        Complex temp = wo_hba.mul(wo_hba).negative().add(1.0).sqrt();

        sPoles[0] = hba.mul(temp.add(1.0));
        sPoles[1] = hba.mul(temp.negative().add(1.0));

        sZeros[0] = new Complex(0, 0);

        /* Calculate Z plane - 2 poles and 2 zeros */

        zPoles[0] = blt(sPoles[0]);
        zPoles[1] = blt(sPoles[1]);

        zZeros[0] = blt(sZeros[0]);
        zZeros[1] = new Complex(-1.0, 0);

        /* Calculate top and bottom coefficients */

        topCoefficients = expand(zZeros, 2);
        botCoefficients = expand(zPoles, 2);

        /* Calculate Y coefficients */

        yc0 = -((botCoefficients[0].r) / (botCoefficients[2].r));
        yc1 = -((botCoefficients[1].r) / (botCoefficients[2].r));

        /* Calculate gain */

        double theta = M_TWOPI * 0.5 * (alpha1 + alpha2);

        Complex expTheta = new Complex(Math.cos(theta), Math.sin(theta));

        Complex fc_gain = evaluate(topCoefficients, 2, botCoefficients, 2, expTheta);
        gain = 1.0 / Math.hypot(fc_gain.r, fc_gain.i);
    }


    double applyBandPassFilter(double sample) {
        xv0 = xv1;
        xv1 = xv2;
        xv2 = sample * gain;

        yv0 = yv1;
        yv1 = yv2;
        yv2 = xv2 - xv0 + yc0 * yv0 + yc1 * yv1;

        return yv2;
    }

    static Complex eval(Complex[] coefficient, int npz, Complex z) {
        Complex sum = new Complex(0, 0);
        for (int i = npz; i >= 0; i -= 1) {
            sum = sum.mul(z).add(coefficient[i]);
        }
        return sum;
    }

    static Complex evaluate(Complex[] topco, int nz, Complex[] botco, int np, Complex z) {
        return eval(topco, nz, z).div(eval(botco, np, z));
    }

    private static Complex blt(Complex pz) {
        return pz.add(2).div(pz.negative().add(2));
    }

    private Complex[] expand(Complex pz[], int npz) {
        Complex[] coefficients = new Complex[npz + 1];
        coefficients[0] = new Complex(1.0, 0);
        for (int i=0; i < npz; i += 1) {
            coefficients[i + 1] = new Complex(0, 0);
        }
        for (int i=0; i < npz; i += 1) {
            final Complex nw = pz[i].negative();
            for (int j = npz; j >= 1; j -= 1) {
                coefficients[j] = nw.mul(coefficients[j]).add(coefficients[j-1]);
            }
            coefficients[0] = nw.mul(coefficients[0]);
        }
        return coefficients;
    }
}
