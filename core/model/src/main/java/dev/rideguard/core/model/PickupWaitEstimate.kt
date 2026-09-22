package dev.rideguard.core.model

/** Historical mean from 416 Uber/Lyft rides in Denver; not a local prediction. */
object PickupWaitEstimate {
    const val MINUTES = 1.28
    const val STUDY_URL = "https://wp-cpr.s3.amazonaws.com/uploads/2019/06/cu-uber-lyft-study.pdf"
}
