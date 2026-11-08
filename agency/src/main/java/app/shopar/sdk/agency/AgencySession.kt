package app.shopar.sdk.agency

import app.shopar.sdk.proto.creator.v1.AgencyCreatorProfile
import app.shopar.sdk.proto.creator.v1.AgencyReferralDisclosure

/** Safe creator information returned by Agency SDK sign-in. Credentials are never exposed. */
data class AgencyCreatorSession(
    val fullName: String,
    val publicUsername: String,
    val profileImageUrl: String,
    val referralDisclosure: AgencyReferralDisclosure,
    val isNewAccount: Boolean,
)

internal fun AgencyCreatorProfile.toPublicSession(disclosure: AgencyReferralDisclosure, isNewAccount: Boolean) = AgencyCreatorSession(
    fullName = fullName,
    publicUsername = publicUsername,
    profileImageUrl = profileImageUrl,
    referralDisclosure = disclosure,
    isNewAccount = isNewAccount,
)
