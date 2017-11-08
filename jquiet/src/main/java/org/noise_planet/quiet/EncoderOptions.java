package org.noise_planet.quiet;

/**
 * Created by nicolas on 08/11/17.
 */
public class EncoderOptions {
    enum ENCODING {
        // Encode/decode in OFDM mode
        OFDM_ENCODING,
        // Encode/decode in modem mode
        MODEM_ENCODING,
        /*
         * Encode/decode in gaussian minimum shift keying mode
         *
         * GMSK mode does not offer the modulation modes given by the other
         * encodings. It has a fairly limited bitrate, but the advantage of
         * GMSK is that its receiver does not need to compute any FFTs, making
         * it suitable for low-power receivers or situations with little
         * computational capacity.
         */
        GMSK_ENCODING,
    }


    private ENCODING encoding;
    // OFDM options, used only by OFDM mode
    private QuietOfdmOptions ofdmopt;

    // Interpolation filter and carrier frequency options
    private quiet_modulator_options modopt;

    // Resampler configuration (if specified frequency is not 44.1kHz)
    private quiet_resampler_options resampler;

    // Encoder mode, one of {ofdm_encoding, modem_encoding, gmsk_encoding}
    private quiet_encoding_t encoding;

    private quiet_checksum_scheme_t checksum_scheme;
    private quiet_error_correction_scheme_t inner_fec_scheme;
    private quiet_error_correction_scheme_t outer_fec_scheme;
    private quiet_modulation_scheme_t mod_scheme;

    /* Header schemes
     * These control the frame header properties
     * Only used if header_override_defaults = true
     */
    private bool header_override_defaults;
    private quiet_checksum_scheme_t header_checksum_scheme;
    private quiet_error_correction_scheme_t header_inner_fec_scheme;
    private quiet_error_correction_scheme_t header_outer_fec_scheme;
    private quiet_modulation_scheme_t header_mod_scheme;

    /* Maximum frame length
     *
     * This value controls the maximum length of the user-controlled
     * section of the frame. There is overhead in starting new frames,
     * and each frame performs its own CRC check which either accepts or
     * rejects the frame. A frame begins with a synchronization section
     * which the decoder uses to detect and lock on to the frame. Over time,
     * the synchronization will drift, which makes shorter frames easier to
     * decode than longer frames.
     */
    private size_t frame_len;

    /** Encoder options for OFDM
     *
     * These options configure the behavior of OFDM, orthogonal frequency division
     * multiplexing, as used by the encoder. OFDM places the modulated symbols on
     * to multiple orthogonal subcarriers. This can help the decoder estabilish
     * good equalization when used on a system with uneven filtering.
     */
    public static final class QuietOfdmOptions {
        // total number of subcarriers used, inlcuding guard bands and pilots
        int num_subcarriers;

        // number of cyclic prefix samples between symbols
        int cyclic_prefix_len;

        // number of taper window between symbols
        int taper_len;

        // number of extra guard subcarriers inserted on left (low freq)
        long left_band;

        // number of extra guard subcarriers inserted on right (high freq)
        long right_band;
    }

    public static final class QuietModulatorOptions {

    /* Numerical value for shape of interpolation filter
     *
     * These values correspond to those used by liquid DSP. In particular,
     *
     * 1: Nyquist Kaiser
     *
     * 2: Parks-McClellan
     *
     * 3: Raised Cosine
     *
     * 4: Flipped Exponential (Nyquist)
     *
     * 5: Flipped Hyperbolic Secant (Nyquist)
     *
     * 6: Flipped Arc-Hyperbolic Secant (Nyquist)
     *
     * 7: Root-Nyquist Kaiser (Approximate Optimum)
     *
     * 8: Root-Nyquist Kaiser (True Optimum)
     *
     * 9: Root Raised Cosine
     *
     * 10: Harris-Moerder-3
     *
     * 11: GMSK Transmit
     *
     * 12: GMSK Receive
     *
     * 13: Flipped Exponential (root-Nyquist)
     *
     * 14: Flipped Hyperbolic Secant (root-Nyquist)
     *
     * 15: Flipped Arc-Hyperbolic Secant (root-Nyquist)
     *
     * All other values invalid
     */
        int shape;

        // interpolation factor
        int samples_per_symbol;

        // interpolation filter delay
        int symbol_delay;

        // interpolation roll-off factor
        float excess_bw;

        // carrier frequency, [0, 2*pi)
        float center_rads;

        // gain, [0, 0.5]
        float gain;

        // dc blocker options
        quiet_dc_filter_options dc_filter_opt;
    }
}
