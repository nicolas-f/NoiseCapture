package org.noise_planet.quiet;


public class Encoder {
    double gain;

    public Encoder() throws IllegalArgumentException {
        this.gain = gain;
        if(gain < 0 || gain > 0.5) {
            throw new IllegalArgumentException("Bad gain configuration should be (0-0.5]");
        }
        switch(encoding) {
            case OFDM_ENCODING:
                break;
            case MODEM_ENCODING:
                break;
            case GMSK_ENCODING:
                break;
        }
    }
}